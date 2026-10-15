package com.safecallkids.app.monetization

import java.util.concurrent.TimeUnit

enum class TrialReminder(
    val daysAfterExpiry: Long,
    val notificationId: Int,
    val uniqueWorkName: String
) {
    EXPIRED(0, 2, "trial_expiry_notification"),
    AFTER_THREE_DAYS(3, 3, "trial_expiry_reminder_3_days"),
    AFTER_SEVEN_DAYS(7, 4, "trial_expiry_reminder_7_days");

    fun dueAtMillis(trialStartedAtMillis: Long): Long {
        return trialStartedAtMillis + PremiumAccessManager.TRIAL_DURATION_MILLIS +
            TimeUnit.DAYS.toMillis(daysAfterExpiry)
    }

    companion object {
        fun fromWorkInput(value: String?): TrialReminder {
            return entries.firstOrNull { it.name == value } ?: EXPIRED
        }
    }
}
