package com.skywatch.cg.data.prefs

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(private val prefs: SharedPreferences) {

    companion object {
        private const val PREFS_NAME = "sky_watch_prefs"
        private const val KEY_ALERTS_ENABLED = "alerts_enabled"
        private const val KEY_ALERT_MINUTES_BEFORE = "alert_minutes_before"
        private const val KEY_ALERT_ALL_FLIGHTS = "alert_all_flights"

        fun getInstance(context: Context): AppPreferences {
            return AppPreferences(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
        }
    }

    var alertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALERTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ALERTS_ENABLED, value).apply()

    var alertMinutesBefore: Int
        get() = prefs.getInt(KEY_ALERT_MINUTES_BEFORE, 15)
        set(value) = prefs.edit().putInt(KEY_ALERT_MINUTES_BEFORE, value).apply()

    var alertAllFlights: Boolean
        get() = prefs.getBoolean(KEY_ALERT_ALL_FLIGHTS, false)
        set(value) = prefs.edit().putBoolean(KEY_ALERT_ALL_FLIGHTS, value).apply()
}