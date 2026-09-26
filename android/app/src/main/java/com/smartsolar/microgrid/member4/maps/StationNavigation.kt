/* Member 4 | External navigation | Hand real station coordinates to Google Maps, then fall back to a Maps URL. */
package com.smartsolar.microgrid.member4.maps

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

object StationNavigation {
    fun open(destination: RoutePoint, launch: (Intent) -> Unit): Boolean {
        // Omit an origin so Google Maps uses the device's fresh current location.
        if (!destination.latitude.isFinite() || !destination.longitude.isFinite() ||
            destination.latitude !in -90.0..90.0 || destination.longitude !in -180.0..180.0) return false
        val coordinates = "${destination.latitude},${destination.longitude}"
        val native = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$coordinates&mode=d"))
            .setPackage("com.google.android.apps.maps")
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/").buildUpon()
            .appendQueryParameter("api", "1")
            .appendQueryParameter("destination", coordinates)
            .appendQueryParameter("travelmode", "driving")
            .appendQueryParameter("dir_action", "navigate")
            .build())
        for (intent in listOf(native, web)) {
            try {
                launch(intent)
                return true
            } catch (_: ActivityNotFoundException) {
                // A missing/disabled Maps app can still fall back to a browser.
            } catch (_: SecurityException) {
                // Device policy may block either handler; keep the in-app preview usable.
            }
        }
        return false
    }
}
