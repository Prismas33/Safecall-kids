package com.safecallkids.app.domain

import android.content.Context
import android.os.Build
import com.safecallkids.app.data.ProtectionPreferences
import com.safecallkids.app.system.PermissionChecker
import com.safecallkids.app.system.RoleChecker

class ProtectionStatusChecker(context: Context) {
    private val preferences = ProtectionPreferences(context)
    private val permissionChecker = PermissionChecker(context)
    private val roleChecker = RoleChecker(context)

    fun getMainStatus(): ProtectionStatus {
        return ProtectionStatus(
            hasSystemRequirements = hasMainSystemRequirements(),
            isUserEnabled = preferences.isUserProtectionEnabled
        )
    }

    fun hasMainSystemRequirements(): Boolean {
        return permissionChecker.hasAllBasicPermissions() &&
            permissionChecker.hasOverlayPermission() &&
            roleChecker.hasCallScreeningRoleOrDefaultDialer()
    }

    fun isActiveForCallScreeningService(): Boolean {
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
