package com.smartsolar.microgrid.network

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private const val PREFS_NAME = "SmartSolarPrefs"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs?.edit()?.putString("jwt_token", token)?.apply()
    }

    fun getToken(): String? {
        return prefs?.getString("jwt_token", null)
    }

    fun saveNic(nic: String) {
        prefs?.edit()?.putString("user_nic", nic)?.apply()
    }

    fun getNic(): String? {
        return prefs?.getString("user_nic", null)
    }

    fun saveRole(role: String) {
        prefs?.edit()?.putString("user_role", role)?.apply()
    }

    fun getRole(): String? {
        return prefs?.getString("user_role", null)
    }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}
