package com.safecallkids.app.data

import android.content.Context

class ReviewPromptPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(ProtectionPreferences.PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldRequestReview(isProtectionActive: Boolean): Boolean {
        return isProtectionActive && !prefs.getBoolean(KEY_REVIEW_PROMPT_REQUESTED, false)
    }

    fun markReviewRequested() {
        prefs.edit()
            .putBoolean(KEY_REVIEW_PROMPT_REQUESTED, true)
            .putLong(KEY_REVIEW_PROMPT_REQUESTED_AT, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val KEY_REVIEW_PROMPT_REQUESTED = "review_prompt_requested"
        private const val KEY_REVIEW_PROMPT_REQUESTED_AT = "review_prompt_requested_at"
    }
}
