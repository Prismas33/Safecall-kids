package com.safecallkids.app.monetization

import android.content.Context
import java.util.concurrent.TimeUnit

class PremiumAccessManager(context: Context) {
    private val preferences = PremiumEntitlementPreferences(context.applicationContext)

    fun getSnapshot(isLoading: Boolean = false, nowMillis: Long = System.currentTimeMillis()): MonetizationSnapshot {
        val trialStartedAt = preferences.ensureTrialStarted(nowMillis)
        val trialEndsAt = trialStartedAt + TRIAL_DURATION_MILLIS
        val trialRemainingMillis = (trialEndsAt - nowMillis).coerceAtLeast(0L)
        val trialDaysRemaining = calculateDisplayedTrialDays(trialRemainingMillis)
        val isTrialActive = nowMillis < trialEndsAt
        val hasPremium = preferences.isPlayPremiumPurchased

        val state = when {
            isLoading -> MonetizationState.LOADING
            hasPremium -> MonetizationState.PREMIUM
            preferences.isPurchasePending -> MonetizationState.PURCHASE_PENDING
            isTrialActive -> MonetizationState.TRIAL_ACTIVE
            preferences.billingErrorMessage != null -> MonetizationState.BILLING_ERROR
            else -> MonetizationState.TRIAL_EXPIRED
        }

        return MonetizationSnapshot(
            state = state,
            canUsePremiumFeatures = hasPremium || isTrialActive,
            trialDaysRemaining = trialDaysRemaining,
            playPrice = preferences.playPrice,
            billingErrorMessage = preferences.billingErrorMessage
        )
    }

    fun canUsePremiumFeatures(): Boolean {
        return getSnapshot().canUsePremiumFeatures
    }

    fun savePlayPrice(price: String) {
        preferences.savePlayPrice(price)
    }

    fun markPurchasePending() {
        preferences.markPurchasePending()
    }

    fun markPremiumPurchased(purchaseToken: String, orderId: String?) {
        preferences.markPremiumPurchased(purchaseToken, orderId)
    }

    fun markPlayPremiumNotPurchased() {
        preferences.markPlayPremiumNotPurchased()
    }

    fun clearPurchasePending() {
        preferences.clearPurchasePending()
    }

    fun markBillingError(message: String) {
        preferences.markBillingError(message)
    }

    private fun calculateDisplayedTrialDays(remainingMillis: Long): Int {
        if (remainingMillis <= 0L) return 0
        return TimeUnit.MILLISECONDS.toDays(remainingMillis).toInt() + 1
    }

    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_lifetime"
        const val TRIAL_DAYS = 7L
        val TRIAL_DURATION_MILLIS: Long = TimeUnit.DAYS.toMillis(TRIAL_DAYS)
    }
}
