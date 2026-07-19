package com.safecallkids.app.monetization

enum class MonetizationState {
    LOADING,
    TRIAL_ACTIVE,
    TRIAL_EXPIRED,
    PREMIUM,
    PURCHASE_PENDING,
    BILLING_ERROR
}

data class MonetizationSnapshot(
    val state: MonetizationState,
    val canUsePremiumFeatures: Boolean,
    val trialDaysRemaining: Int,
    val playPrice: String?,
    val billingErrorMessage: String?
)
