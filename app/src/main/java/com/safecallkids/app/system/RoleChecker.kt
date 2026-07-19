package com.safecallkids.app.system

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.telecom.TelecomManager

class RoleChecker(private val context: Context) {
    fun hasCallScreeningRoleOrDefaultDialer(): Boolean {
        return hasCallScreeningRole() || isDefaultDialer()
    }

    fun hasCallScreeningRole(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                    roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isDefaultDialer(): Boolean {
        return try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecom.defaultDialerPackage == context.packageName
        } catch (_: Exception) {
            false
        }
    }
}
