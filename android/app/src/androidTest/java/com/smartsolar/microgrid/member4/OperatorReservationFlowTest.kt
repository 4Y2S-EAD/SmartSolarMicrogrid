/* Member 4: live operator UI integration. Fixture created via existing POST; never approve an arbitrary reservation. */
package com.smartsolar.microgrid.member4

import android.graphics.Bitmap
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.member1.activities.GridOperatorDashboardActivity
import com.smartsolar.microgrid.member4.operator.OperatorReservationsActivity
import com.smartsolar.microgrid.network.TokenManager
import com.smartsolar.microgrid.network.ApiClient
import java.io.File
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OperatorReservationFlowTest {
    private fun fixture() = JSONObject(File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "operator-test.json").readText())
    private fun authenticated(action: () -> Unit) {
        // Preserve the user's device login while using short-lived verification credentials.
        val previous = TokenManager.getToken()
        TokenManager.saveToken(fixture().getString("token"))
        try { action() } finally { TokenManager.saveToken(previous.orEmpty()) }
    }
    private fun awaitHidden(id: Int) {
        // Observe bounded completion instead of assuming a fixed network delay.
        val deadline = System.currentTimeMillis() + 45000
        var done = false
        while (!done && System.currentTimeMillis() < deadline) {
            onView(withId(id)).check { view, error -> if (error != null) throw error; done = view.visibility != android.view.View.VISIBLE }
            if (!done) Thread.sleep(200)
        }
        assertTrue("Request completed", done)
    }
    private fun capture(name: String) {
        // Save actual UI evidence without credentials or network logs.
        android.os.SystemClock.sleep(250)
        val inst = InstrumentationRegistry.getInstrumentation()
        val folder = File(inst.targetContext.getExternalFilesDir(null), "operator-evidence").apply { mkdirs() }
        inst.uiAutomation.takeScreenshot()?.let { bitmap -> File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle() }
    }
    @Test fun dashboardAndExistingMapStillWork() = authenticated {
        ActivityScenario.launch(GridOperatorDashboardActivity::class.java).use {
            awaitHidden(R.id.m4SummaryLoading)
            val summary = runBlocking { ApiClient.apiService.getOperatorReservations(emptyMap()).body()!!.summary }
            onView(withId(R.id.m4ActiveCount)).check(matches(withText(summary.activeCount.toString())))
            onView(withId(R.id.m4CardActive)).perform(scrollTo())
            capture("android-dashboard")
            onView(withId(R.id.navOperatorStations)).perform(click())
            awaitHidden(R.id.m4Loading)
            onView(withId(R.id.m4MapContainer)).check(matches(isDisplayed()))
            awaitHidden(R.id.m4MapMessage)
            capture("android-existing-map")
            onView(withId(R.id.m4Back)).perform(click())
        }
    }
    @Test fun searchApproveAndRefresh() = authenticated {
        val id = fixture().getString("reservationId")
        ActivityScenario.launch(OperatorReservationsActivity::class.java).use { scenario ->
            awaitHidden(R.id.m4OperatorLoading)
            onView(withId(R.id.m4TabPending)).perform(click())
            awaitHidden(R.id.m4OperatorLoading)
            capture("android-pending")
            onView(withId(R.id.m4TabSearch)).perform(scrollTo(), click())
            awaitHidden(R.id.m4OperatorLoading)
            onView(withId(R.id.m4FilterId)).perform(scrollTo(), replaceText(id), closeSoftKeyboard())
            onView(withId(R.id.m4FilterSubmit)).perform(scrollTo(), click())
            awaitHidden(R.id.m4OperatorLoading)
            onView(allOf(withId(R.id.m4ReservationStatus), isDisplayed())).check(matches(withText("Pending")))
            onView(allOf(withId(R.id.m4ReservationApprove), isDisplayed())).perform(click())
            val deadline = System.currentTimeMillis()+30000
            var approved = false
            while (!approved && System.currentTimeMillis()<deadline) {
                approved = runBlocking { ApiClient.apiService.searchOperatorReservations(mapOf("reservationId" to id)).body()!!.items.single().status == "Approved" }
                if (!approved) Thread.sleep(200)
            }
            assertTrue(approved)
            onView(withId(R.id.m4OperatorRefresh)).perform(click())
            awaitHidden(R.id.m4OperatorLoading)
            onView(allOf(withId(R.id.m4ReservationStatus), isDisplayed())).check(matches(withText("Approved")))
            capture("android-approved-search")
            scenario.recreate()
            awaitHidden(R.id.m4OperatorLoading)
            onView(allOf(withId(R.id.m4ReservationStatus), isDisplayed())).check(matches(withText("Approved")))
            onView(withId(R.id.m4TabSearch)).perform(click())
            onView(withId(R.id.m4FilterId)).perform(scrollTo(), replaceText("000000000000000000000000"), closeSoftKeyboard())
            onView(withId(R.id.m4FilterSubmit)).perform(scrollTo(), click())
            awaitHidden(R.id.m4OperatorLoading)
            onView(withId(R.id.m4OperatorMessage)).check(matches(withText("No reservations found. Refresh or adjust your filters.")))
            onView(withId(R.id.m4TabHistory)).perform(scrollTo(), click())
            awaitHidden(R.id.m4OperatorLoading)
            capture("android-history")
        }
    }
}
