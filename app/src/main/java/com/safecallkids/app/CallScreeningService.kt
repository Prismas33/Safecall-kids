package com.safecallkids.app

import android.app.role.RoleManager
import android.content.Context
import android.telecom.Call
import android.telecom.TelecomManager
import android.util.Log

/**
 * Call screening moderno para Android 10+.
 * O sistema chama este serviço para cada chamada recebida.
 */
class CallScreeningService : android.telecom.CallScreeningService() {

    private val TAG = "CallScreeningService"

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart
        Log.i(TAG, "=== SCREENING CALL ===")
        Log.i(TAG, "Call from: ${callDetails.handle} | Number: $phoneNumber")

        // Gate: só bloquear se o usuário ativou e o papel/role está OK
        if (!isProtectionActive()) {
            Log.w(TAG, "Protection not active; allowing call by default")
            val allow = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()
            respondToCall(callDetails, allow)
            return
        }

        Log.d(TAG, "Protection is ACTIVE - checking if should block...")

        if (phoneNumber.isNullOrBlank()) {
            Log.w(TAG, "Private/Hidden call detected - BLOCKING")

            val response = android.telecom.CallScreeningService.CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()

            respondToCall(callDetails, response)
            incrementBlockedCallsCount()
            updateServiceNotification()
            return
        }

        val shouldBlock = shouldBlockCall(phoneNumber)
        Log.i(TAG, "Decision for $phoneNumber: ${if (shouldBlock) "BLOCK" else "ALLOW"}")
        
        if (shouldBlock) {
            Log.i(TAG, "BLOCKING call from: $phoneNumber")

            val response = android.telecom.CallScreeningService.CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()

            respondToCall(callDetails, response)
            incrementBlockedCallsCount()
            updateServiceNotification()
        } else {
            Log.i(TAG, "ALLOWING call from: $phoneNumber")

            val response = android.telecom.CallScreeningService.CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()

            respondToCall(callDetails, response)
        }
    }

    private fun isProtectionActive(): Boolean {
        return try {
            Log.d(TAG, "=== CHECKING PROTECTION STATUS ===")
            
            // Verificar flag manual do utilizador primeiro
            val prefs = getSharedPreferences("safecall_prefs", Context.MODE_PRIVATE)
            val userEnabled = prefs.getBoolean("all_setup_completed", false)
            
            Log.d(TAG, "User manually enabled protection: $userEnabled")
            
            if (!userEnabled) {
                Log.w(TAG, "Protection not active: user hasn't manually activated")
                return false
            }
            
            // Verificar se temos as permissões básicas necessárias
            val hasReadContacts = checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasAnswerCalls = checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
            
            Log.d(TAG, "Permissions check:")
            Log.d(TAG, "  - READ_CONTACTS: $hasReadContacts")
            Log.d(TAG, "  - ANSWER_PHONE_CALLS: $hasAnswerCalls")
            
            if (!hasReadContacts || !hasAnswerCalls) {
                Log.w(TAG, "Protection not active: missing basic permissions")
                return false
            }

            // Em Android 10+, verificar se temos papel de call screening ou discador
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                Log.d(TAG, "Android 10+ detected, checking roles...")
                
                val roleManager = getSystemService(RoleManager::class.java)
                val holdsScreeningRole = try {
                    val available = roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                    val held = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                    Log.d(TAG, "Call screening role - available: $available, held: $held")
                    available && held
                } catch (e: Exception) { 
                    Log.w(TAG, "Error checking call screening role", e)
                    false 
                }

                if (holdsScreeningRole) {
                    Log.i(TAG, "Protection ACTIVE via Call Screening role")
                    return true
                }

                // Fallback: ser discador padrão também dá poderes equivalentes
                val telecom = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                val defaultDialer = telecom.defaultDialerPackage
                val isDefaultDialer = defaultDialer == packageName
                Log.d(TAG, "Default dialer: $defaultDialer | Is us: $isDefaultDialer")
                
                if (isDefaultDialer) {
                    Log.i(TAG, "Protection ACTIVE via Default Dialer")
                    return true
                } else {
                    Log.w(TAG, "Protection NOT ACTIVE: not call screening role and not default dialer")
                    return false
                }
            }
            
            Log.i(TAG, "Protection ACTIVE (Android < 10)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking protection active state", e)
            false
        }
    }

    private fun shouldBlockCall(phoneNumber: String): Boolean {
        return try {
            Log.d(TAG, "Checking if number $phoneNumber should be blocked...")
            
            // Verificar permissão de contatos primeiro
            val hasContactPermission = checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (!hasContactPermission) {
                Log.e(TAG, "Missing READ_CONTACTS permission - CANNOT BLOCK (safety first)")
                return false
            }
            
            Log.d(TAG, "Contacts permission OK, checking contacts...")
            val contactsHelper = ContactsHelper(this)
            val contactsCount = contactsHelper.getContactsCount()
            Log.d(TAG, "Total contacts: $contactsCount")
            
            val isContact = contactsHelper.isNumberInContacts(phoneNumber)
            Log.i(TAG, "Number $phoneNumber is contact: $isContact")

            // Bloquear se NÃO estiver nos contatos
            val shouldBlock = !isContact
            Log.i(TAG, "Final decision: ${if (shouldBlock) "BLOCK" else "ALLOW"}")
            
            return shouldBlock

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException checking contacts - NOT BLOCKING", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Exception checking contacts - NOT BLOCKING", e)
            return false
        }
    }

    private fun incrementBlockedCallsCount() {
        val prefs = getSharedPreferences("safecall_prefs", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("blocked_calls_count", 0)
        prefs.edit().putInt("blocked_calls_count", currentCount + 1).apply()

        Log.i(TAG, "Blocked calls count: ${currentCount + 1}")
    }

    private fun updateServiceNotification() {
        try {
            val intent = android.content.Intent("com.safecallkids.app.UPDATE_NOTIFICATION")
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification", e)
        }
    }
}
