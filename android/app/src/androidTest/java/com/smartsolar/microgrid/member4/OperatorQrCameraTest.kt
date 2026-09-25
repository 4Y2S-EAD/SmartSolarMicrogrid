// Member 4: these tests use the REAL camera. The physical scan test requires the generated QR on another display/paper.
package com.smartsolar.microgrid.member4

import android.graphics.Bitmap
import android.view.View
import android.widget.TextView
import androidx.camera.view.PreviewView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.member1.activities.GridOperatorDashboardActivity
import com.smartsolar.microgrid.member4.qr.OperatorQrScannerActivity
import com.smartsolar.microgrid.network.ApiClient
import com.smartsolar.microgrid.network.TokenManager
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OperatorQrCameraTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun awaitPreview(allowAlreadyCaptured: Boolean = false) {
        val deadline = System.currentTimeMillis() + 20000
        var streaming = false
        while (!streaming && System.currentTimeMillis() < deadline) {
            onView(withId(R.id.m4QrPreview)).check { view, error ->
                if (error != null) throw error
                streaming = (view as PreviewView).previewStreamState.value == PreviewView.StreamState.STREAMING
                if (allowAlreadyCaptured && !(view.parent as View).isShown) streaming = true
            }
            if (!streaming) Thread.sleep(200)
        }
        assertTrue("Real CameraX preview streams", streaming)
    }

    private fun awaitTitle(expected: String, timeout: Long = 45000) {
        val deadline = System.currentTimeMillis() + timeout
        var title = ""
        while (title != expected && System.currentTimeMillis() < deadline) {
            onView(withId(R.id.m4QrResultTitle)).check { view, error ->
                if (error != null) throw error
                title = (view as TextView).text.toString()
            }
            if (title != expected) Thread.sleep(300)
        }
        assertEquals(expected, title)
    }

    private fun captureResult(name: String) {
        // Capture result screens only: no credentials or camera images of physical QR codes in evidence.
        Thread.sleep(250)
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "qr-evidence").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun liveCameraSurvivesRecreationAndBackground() {
        ActivityScenario.launch(OperatorQrScannerActivity::class.java).use { scenario ->
            awaitPreview()
            scenario.recreate()
            awaitPreview()
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            awaitPreview()
            onView(withId(R.id.m4QrScanIndicator)).check(matches(withText(R.string.m4_qr_scanning)))
        }
    }

    @Test fun deniedCameraOffersRecoveryWithoutCompletion() {
        // Run on an emulator with camera permission revoked and user-set/user-fixed flags cleared before instrumentation.
        instrumentation.uiAutomation.serviceInfo = instrumentation.uiAutomation.serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        instrumentation.targetContext.getSharedPreferences("m4_camera_permission", 0).edit().clear().commit()
        ActivityScenario.launch(OperatorQrScannerActivity::class.java).use {
            fun denyDialog() {
                val deadline = System.currentTimeMillis() + 10000
                var denied = false
                while (!denied && System.currentTimeMillis() < deadline) {
                    val root = instrumentation.uiAutomation.rootInActiveWindow
                    val buttons = listOf("com.android.permissioncontroller", "com.google.android.permissioncontroller")
                        .flatMap { pkg -> listOf("permission_deny_button", "permission_deny_and_dont_ask_again_button")
                            .flatMap { id -> root?.findAccessibilityNodeInfosByViewId("$pkg:id/$id").orEmpty() } } +
                        root?.findAccessibilityNodeInfosByText("Don't allow").orEmpty()
                    denied = buttons.firstOrNull()?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true
                    if (!denied) Thread.sleep(200)
                }
                assertTrue("Android camera permission dialog appeared", denied)
            }
            denyDialog()
            onView(withId(R.id.m4QrPrimary)).check(matches(withText(R.string.m4_qr_allow)))
            onView(withId(R.id.m4QrDetailsCard)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            captureResult("permission-denied")
            onView(withId(R.id.m4QrPrimary)).perform(click())
            denyDialog()
            onView(withId(R.id.m4QrPrimary)).check(matches(withText(R.string.m4_qr_settings)))
            captureResult("permission-settings")
        }
    }

    @Test fun physicalQrVerifiesCompletesAndRejectsReplay() {
        // Caller must build with apiBaseUrl=http://127.0.0.1:5236/api/ and adb reverse tcp:5236 tcp:5236.
        val fixture = JSONObject(File(instrumentation.targetContext.getExternalFilesDir(null), "qr-device.json").readText())
        val previous = TokenManager.getToken()
        TokenManager.saveToken(fixture.getString("token"))
        try {
            ActivityScenario.launch(GridOperatorDashboardActivity::class.java).use {
                onView(withId(R.id.btnQrApproval)).perform(scrollTo(), click())
                awaitPreview(allowAlreadyCaptured = true)
                // A person presents the actual generated QR to the phone; no direct verify() call or fake camera frame.
                awaitTitle("QR verified", 180000)
                onView(withId(R.id.m4QrPrimary)).perform(scrollTo())
                captureResult("verified")
                val id = fixture.getString("reservationId")
                assertEquals("Approved", runBlocking { ApiClient.apiService.getReservationById(id).body()!!.status })
                onView(withId(R.id.m4QrPrimary)).perform(click())
                onView(withText(R.string.m4_qr_confirm_action)).perform(click())
                awaitTitle("Transfer completed")
                captureResult("completed")
                assertEquals("Completed", runBlocking { ApiClient.apiService.getReservationById(id).body()!!.status })
                onView(withId(R.id.m4QrAgain)).perform(scrollTo(), click())
                awaitPreview(allowAlreadyCaptured = true)
                // Hold the same physical code in view: the server must now reject it.
                awaitTitle("Verification rejected", 180000)
                onView(withId(R.id.m4QrPrimary)).check(matches(withEffectiveVisibility(Visibility.GONE)))
                captureResult("rejected-completed")
            }
        } finally { TokenManager.saveToken(previous.orEmpty()) }
    }
}
