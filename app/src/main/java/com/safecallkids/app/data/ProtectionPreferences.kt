package com.safecallkids.app.data

import android.content.Context

class ProtectionPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isUserProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALL_SETUP_COMPLETED, false)

    val blockedCallsCount: Int
        get() = prefs.getInt(KEY_BLOCKED_CALLS_COUNT, 0)

    fun setProtectionEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ALL_SETUP_COMPLETED, enabled)
            .putBoolean(KEY_CALL_SCREENING_CONFIGURED, enabled)
            .apply()
    }

    fun incrementBlockedCallsCount(): Int {
        val nextCount = blockedCallsCount + 1
        prefs.edit().putInt(KEY_BLOCKED_CALLS_COUNT, nextCount).apply()
        return nextCount
    }

    companion object {
        const val PREFS_NAME = "safecall_prefs"
        const val KEY_ALL_SETUP_COMPLETED = "all_setup_completed"
        const val KEY_CALL_SCREENING_CONFIGURED = "call_screening_configured"
        const val KEY_BLOCKED_CALLS_COUNT = "blocked_calls_count"
    }
}
