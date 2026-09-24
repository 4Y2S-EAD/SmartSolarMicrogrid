/* Member 4 | Maps integration checks | Exercise both role routes against the configured live API. */
package com.smartsolar.microgrid.member4

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.member1.activities.GridOperatorDashboardActivity
import com.smartsolar.microgrid.member1.activities.HomeActivity
import com.smartsolar.microgrid.network.ApiClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.Matchers.allOf
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import com.smartsolar.microgrid.member2.activities.M2StationsActivity

@RunWith(AndroidJUnit4::class)
class StationMapFlowTest {
    private fun capture(name: String) {
        // Save actual emulator UI evidence, without recording keys, tokens or application logs.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "maps-evidence")
        directory.mkdirs()
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun awaitStationData() {
        // Poll observable UI state while real requests run; fail on API errors or a bounded timeout.
        val deadline = System.currentTimeMillis() + 30000
        var loaded = false
        while (!loaded && System.currentTimeMillis() < deadline) {
            onView(withId(R.id.m4Loading)).check { view, error ->
                if (error != null) throw error
                loaded = view.visibility != android.view.View.VISIBLE
            }
            if (!loaded) Thread.sleep(200)
        }
        assertTrue("Station request completed", loaded)
        onView(withId(R.id.m4DataMessage)).check { view, error ->
            if (error != null) throw error
            val text = (view as android.widget.TextView).text.toString()
            assertTrue("No API error: $text", !text.contains("Could not refresh"))
        }
    }

    @Test fun prosumerCanSearchAndSelectRealStation() {
        // Read the fixture name from the real API instead of embedding production station names.
        val station = runBlocking { ApiClient.apiService.getMapStations().body()!!.stations.first() }
        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.btnFindStations)).perform(scrollTo(), click())
            awaitStationData()
            capture("prosumer-map")
            onView(withId(R.id.m4Search)).perform(replaceText(station.stationName), closeSoftKeyboard())
            onView(withId(R.id.m4SearchSubmit)).perform(click())
            awaitStationData()
            onView(allOf(withId(R.id.m4StationName), withText(station.stationName), isDisplayed())).perform(click())
            onView(withId(R.id.m4DetailName)).check(matches(withText(station.stationName)))
            capture("prosumer-station-details")
            onView(withId(R.id.m4DetailClose)).perform(click())
            onView(withId(R.id.m4Search)).perform(replaceText("no-match-map-test-928"), closeSoftKeyboard())
            onView(withId(R.id.m4SearchSubmit)).perform(click())
            awaitStationData()
            onView(withId(R.id.m4DataMessage)).check(matches(withText(R.string.m4_no_matches)))
            onView(withId(R.id.m4Back)).perform(click())
        }
    }

    @Test fun operatorStationsNavigationUsesSharedMap() {
        // Verify the operator's existing station tab opens the exact same native map/list screen.
        ActivityScenario.launch(GridOperatorDashboardActivity::class.java).use {
            onView(withId(R.id.navOperatorStations)).perform(click())
            awaitStationData()
            onView(withId(R.id.m4MapContainer)).check(matches(isDisplayed()))
            onView(withId(R.id.m4Stations)).check(matches(isDisplayed()))
            capture("operator-map")
            onView(withId(R.id.m4Back)).perform(click())
            onView(withId(R.id.navOperatorStations)).check(matches(isDisplayed()))
        }
    }

    @Test fun deniedLocationStillAllowsSearchAndRecreation() {
        // Exercise the real permission denial, then recreate the screen with its existing search state.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertTrue("Start this check with location permission denied",
            ContextCompat.checkSelfPermission(instrumentation.targetContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_DENIED)
        val station = runBlocking { ApiClient.apiService.getMapStations().body()!!.stations.first() }
        ActivityScenario.launch(M2StationsActivity::class.java).use { scenario ->
            awaitStationData()
            onView(withId(R.id.m4Near)).perform(click())
            val deadline = System.currentTimeMillis() + 10000
            var denied = false
            while (!denied && System.currentTimeMillis() < deadline) {
                val button = instrumentation.uiAutomation.rootInActiveWindow
                    ?.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/permission_deny_button")
                    ?.firstOrNull()
                if (button != null) denied = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                else Thread.sleep(200)
            }
            assertTrue("Location permission dialog was denied", denied)
            awaitStationData()
            onView(withId(R.id.m4LocationMessage)).check(matches(withText(R.string.m4_location_denied)))
            onView(withId(R.id.m4Search)).perform(replaceText(station.stationName), closeSoftKeyboard())
            onView(withId(R.id.m4SearchSubmit)).perform(click())
            awaitStationData()
            scenario.recreate()
            awaitStationData()
            onView(withId(R.id.m4Search)).check(matches(withText(station.stationName)))
            onView(allOf(withId(R.id.m4StationName), withText(station.stationName), isDisplayed())).perform(click())
            onView(withId(R.id.m4DetailName)).check(matches(withText(station.stationName)))
            capture("permission-denied-recreated-details")
            onView(withId(R.id.m4DetailClose)).perform(click())
        }
    }

    @Test fun googleMapLoadsTilesWithLiveStations() {
        // Independently verify Google tile readiness instead of treating a visible map container as success.
        ActivityScenario.launch(M2StationsActivity::class.java).use {
            awaitStationData()
            val deadline = System.currentTimeMillis() + 45000
            var ready = false
            while (!ready && System.currentTimeMillis() < deadline) {
                onView(withId(R.id.m4MapMessage)).check { view, error ->
                    if (error != null) throw error
                    ready = view.visibility != android.view.View.VISIBLE
                }
                if (!ready) Thread.sleep(250)
            }
            capture("map-tile-readiness")
            assertTrue("Google map tiles finished loading", ready)
        }
    }
}
