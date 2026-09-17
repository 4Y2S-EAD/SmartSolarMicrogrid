package com.smartsolar.microgrid

import android.app.Application
import com.smartsolar.microgrid.network.TokenManager

class SmartSolarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.init(this)
    }
}
