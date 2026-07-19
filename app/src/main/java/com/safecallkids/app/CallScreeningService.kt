package com.safecallkids.app

import android.telecom.Call
import android.util.Log
import com.safecallkids.app.data.ProtectionPreferences
import com.safecallkids.app.domain.ProtectionStatusChecker

/**
 * Call screening moderno para Android 10+.
 * O sistema chama este servico para cada chamada recebida.
 */
class CallScreeningService : android.telecom.CallScreeningService() {

    private val TAG = "CallScreeningService"

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart
        Log.i(TAG, "=== SCREENING CALL ===")
        Log.i(TAG, "Call from: ${callDetails.handle} | Number: $phoneNumber")

        // Gate: so bloquear se o usuario ativou e o papel/role esta OK.
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

            val response = CallResponse.Builder()
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

            val response = CallResponse.Builder()
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

            val response = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()

            respondToCall(callDetails, response)
        }
    }

    private fun isProtectionActive(): Boolean {
        return try {
            Log.d(TAG, "=== CHECKING PROTECTION STATUS ===")
            val active = ProtectionStatusChecker(this).isActiveForCallScreeningService()
            Log.i(TAG, "Protection active for call screening: $active")
            active
        } catch (e: Exception) {
            Log.e(TAG, "Error checking protection active state", e)
            false
        }
    }

    private fun shouldBlockCall(phoneNumber: String): Boolean {
        return try {
            Log.d(TAG, "Checking if number $phoneNumber should be blocked...")

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

            val shouldBlock = !isContact
            Log.i(TAG, "Final decision: ${if (shouldBlock) "BLOCK" else "ALLOW"}")

            shouldBlock
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException checking contacts - NOT BLOCKING", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception checking contacts - NOT BLOCKING", e)
            false
        }
    }

    private fun incrementBlockedCallsCount() {
        val nextCount = ProtectionPreferences(this).incrementBlockedCallsCount()
        Log.i(TAG, "Blocked calls count: $nextCount")
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
