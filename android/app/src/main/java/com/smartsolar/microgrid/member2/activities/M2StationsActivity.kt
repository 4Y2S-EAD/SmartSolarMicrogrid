/*
 * Member 2 station entry point; Member 4 map integration.
 * Keeps existing dashboard routes and station-detail navigation while hosting the shared map UI.
 */
package com.smartsolar.microgrid.member2.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R

class M2StationsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Both mobile roles use the same Member 4 fragment and the existing station APIs.
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m2_activity_stations)
    }
}
