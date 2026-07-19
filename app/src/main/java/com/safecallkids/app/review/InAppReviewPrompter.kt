package com.safecallkids.app.review

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import com.safecallkids.app.data.ReviewPromptPreferences

class InAppReviewPrompter(
    private val activity: Activity,
    private val preferences: ReviewPromptPreferences
) {
    fun requestReviewIfEligible(isProtectionActive: Boolean) {
        if (!preferences.shouldRequestReview(isProtectionActive)) return

        preferences.markReviewRequested()

        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (!request.isSuccessful) {
                Log.w(TAG, "In-app review request was not available", request.exception)
                return@addOnCompleteListener
            }

            manager.launchReviewFlow(activity, request.result).addOnCompleteListener {
                Log.d(TAG, "In-app review flow completed")
            }
        }
    }

    companion object {
        private const val TAG = "InAppReviewPrompter"
    }
}
