package com.safecallkids.app.domain

data class ProtectionStatus(
    val hasSystemRequirements: Boolean,
    val isUserEnabled: Boolean,
    val hasPremiumAccess: Boolean
) {
    val isReallyActive: Boolean
        get() = hasSystemRequirements && isUserEnabled && hasPremiumAccess
}
