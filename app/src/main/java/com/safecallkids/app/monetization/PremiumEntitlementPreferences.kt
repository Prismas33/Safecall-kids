package com.safecallkids.app.monetization

import android.content.Context

class PremiumEntitlementPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val trialStartedAtMillis: Long
        get() = prefs.getLong(KEY_TRIAL_STARTED_AT, 0L)

    val isPlayPremiumPurchased: Boolean
        get() = prefs.getBoolean(KEY_PLAY_PREMIUM_PURCHASED, false)

    val isPurchasePending: Boolean
        get() = prefs.getBoolean(KEY_PURCHASE_PENDING, false)

    val playPrice: String?
        get() = prefs.getString(KEY_PLAY_PRICE, null)

    val billingErrorMessage: String?
        get() = prefs.getString(KEY_BILLING_ERROR, null)

    fun wasTrialExpiryNoticeShown(trialStartedAtMillis: Long): Boolean {
        return prefs.getLong(KEY_TRIAL_EXPIRY_NOTICE_START, 0L) == trialStartedAtMillis
    }

    fun markTrialExpiryNoticeShown(trialStartedAtMillis: Long) {
        prefs.edit().putLong(KEY_TRIAL_EXPIRY_NOTICE_START, trialStartedAtMillis).apply()
    }

    fun wasTrialReminderHandled(reminder: TrialReminder, trialStartedAtMillis: Long): Boolean {
        return prefs.getLong(reminderKey(reminder), 0L) == trialStartedAtMillis
    }

    fun markTrialReminderHandled(reminder: TrialReminder, trialStartedAtMillis: Long) {
        prefs.edit().putLong(reminderKey(reminder), trialStartedAtMillis).apply()
    }

    private fun reminderKey(reminder: TrialReminder): String = when (reminder) {
        TrialReminder.EXPIRED -> KEY_TRIAL_EXPIRY_NOTIFICATION_START
        TrialReminder.AFTER_THREE_DAYS -> KEY_TRIAL_REMINDER_THREE_DAYS_START
        TrialReminder.AFTER_SEVEN_DAYS -> KEY_TRIAL_REMINDER_SEVEN_DAYS_START
    }

    val wasNotificationPermissionRequested: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)

    fun markNotificationPermissionRequested() {
        prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true).apply()
    }

    fun ensureTrialStarted(nowMillis: Long): Long {
        val existing = trialStartedAtMillis
        if (existing > 0L) return existing

        prefs.edit()
            .putLong(KEY_TRIAL_STARTED_AT, nowMillis)
            .apply()
        return nowMillis
    }

    fun savePlayPrice(price: String) {
        prefs.edit()
            .putString(KEY_PLAY_PRICE, price)
            .remove(KEY_BILLING_ERROR)
            .apply()
    }

    fun markPurchasePending() {
        prefs.edit()
            .putBoolean(KEY_PURCHASE_PENDING, true)
            .remove(KEY_BILLING_ERROR)
            .apply()
    }

    fun markPremiumPurchased(purchaseToken: String, orderId: String?) {
        prefs.edit()
            .putBoolean(KEY_PLAY_PREMIUM_PURCHASED, true)
            .putBoolean(KEY_PURCHASE_PENDING, false)
            .putString(KEY_PURCHASE_TOKEN, purchaseToken)
            .putString(KEY_PURCHASE_ORDER_ID, orderId)
            .putString(KEY_PREMIUM_SOURCE, SOURCE_PLAY_BILLING)
            .remove(KEY_BILLING_ERROR)
            .apply()
    }

    fun markPlayPremiumNotPurchased() {
        prefs.edit()
            .putBoolean(KEY_PLAY_PREMIUM_PURCHASED, false)
            .putBoolean(KEY_PURCHASE_PENDING, false)
            .remove(KEY_PURCHASE_TOKEN)
            .remove(KEY_PURCHASE_ORDER_ID)
            .remove(KEY_PREMIUM_SOURCE)
            .apply()
    }

    fun clearPurchasePending() {
        prefs.edit()
            .putBoolean(KEY_PURCHASE_PENDING, false)
            .apply()
    }

    fun markBillingError(message: String) {
        prefs.edit()
            .putString(KEY_BILLING_ERROR, message)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "safecall_premium_prefs"
        private const val KEY_TRIAL_STARTED_AT = "trial_started_at"
        private const val KEY_PLAY_PREMIUM_PURCHASED = "play_premium_purchased"
        private const val KEY_PURCHASE_PENDING = "purchase_pending"
        private const val KEY_PLAY_PRICE = "play_price"
        private const val KEY_BILLING_ERROR = "billing_error"
        private const val KEY_TRIAL_EXPIRY_NOTICE_START = "trial_expiry_notice_start"
        private const val KEY_TRIAL_EXPIRY_NOTIFICATION_START = "trial_expiry_notification_start"
        private const val KEY_TRIAL_REMINDER_THREE_DAYS_START = "trial_reminder_three_days_start"
        private const val KEY_TRIAL_REMINDER_SEVEN_DAYS_START = "trial_reminder_seven_days_start"
        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
        private const val KEY_PURCHASE_TOKEN = "purchase_token"
        private const val KEY_PURCHASE_ORDER_ID = "purchase_order_id"
        private const val KEY_PREMIUM_SOURCE = "premium_source"
        private const val SOURCE_PLAY_BILLING = "google_play_billing"
    }
}
