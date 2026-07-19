package com.safecallkids.app.domain

import android.content.Context
import android.os.Build
import com.safecallkids.app.data.ProtectionPreferences
import com.safecallkids.app.monetization.PremiumAccessManager
import com.safecallkids.app.system.PermissionChecker
import com.safecallkids.app.system.RoleChecker

class ProtectionStatusChecker(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = ProtectionPreferences(appContext)
    private val permissionChecker = PermissionChecker(appContext)
    private val roleChecker = RoleChecker(appContext)
    private val premiumAccessManager = PremiumAccessManager(appContext)

    fun getMainStatus(): ProtectionStatus {
        return ProtectionStatus(
            hasSystemRequirements = hasMainSystemRequirements(),
            isUserEnabled = preferences.isUserProtectionEnabled,
            hasPremiumAccess = premiumAccessManager.canUsePremiumFeatures()
        )
    }

    fun hasMainSystemRequirements(): Boolean {
        return permissionChecker.hasAllBasicPermissions() &&
            permissionChecker.hasOverlayPermission() &&
            roleChecker.hasCallScreeningRoleOrDefaultDialer()
    }

    fun isActiveForCallScreeningService(): Boolean {
        if (!premiumAccessManager.canUsePremiumFeatures()) return false
        if (!preferences.isUserProtectionEnabled) return false
        if (!permissionChecker.hasReadContactsPermission()) return false
        if (!permissionChecker.hasAnswerPhoneCallsPermission()) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleChecker.hasCallScreeningRoleOrDefaultDialer()
        } else {
            true
        }
    }

    fun isActiveForLegacyReceiver(): Boolean {
        if (!premiumAccessManager.canUsePremiumFeatures()) return false
        if (!preferences.isUserProtectionEnabled) return false
        if (!permissionChecker.hasReadContactsPermission()) return false
        if (!permissionChecker.hasAnswerPhoneCallsPermission()) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleChecker.isDefaultDialer()
        } else {
            true
        }
    }
}
