package com.safecallkids.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.safecallkids.app.data.ProtectionPreferences
import com.safecallkids.app.domain.ProtectionStatusChecker

class CallReceiver : BroadcastReceiver() {
    private val TAG = "CallReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            // Gate early: only act if user explicitly activated protection and app is properly configured.
            if (!isProtectionActive(context)) {
                Log.d(TAG, "Protection not active; ignoring phone state broadcast")
                return
            }

            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            @Suppress("DEPRECATION")
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            Log.d(TAG, "Phone state changed: $state, Number: $phoneNumber")
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> handleIncomingCall(context, phoneNumber)
                TelephonyManager.EXTRA_STATE_IDLE -> Log.d(TAG, "Call ended or idle")
                TelephonyManager.EXTRA_STATE_OFFHOOK -> Log.d(TAG, "Call answered")
            }
        }
    }

    private fun isProtectionActive(context: Context): Boolean {
        return ProtectionStatusChecker(context).isActiveForLegacyReceiver()
    }

    private fun handleIncomingCall(context: Context, phoneNumber: String?) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            Log.w(TAG, "Private/Hidden call detected - blocking")
            val blocked = blockCall(context)
            if (blocked) {
                incrementBlockedCallsCount(context)
                Log.i(TAG, "Successfully blocked private/hidden call")
            } else {
                Log.w(TAG, "Failed to block private/hidden call")
            }
            return
        }

        Log.d(TAG, "Handling incoming call from: $phoneNumber")
        val contactsHelper = ContactsHelper(context)
        if (!contactsHelper.isNumberInContacts(phoneNumber)) {
            Log.d(TAG, "Number not in contacts, attempting to block: $phoneNumber")
            val blocked = blockCall(context)
            if (blocked) {
                incrementBlockedCallsCount(context)
                Log.i(TAG, "Successfully blocked call from: $phoneNumber")
            } else {
                Log.w(TAG, "Failed to block call from: $phoneNumber")
            }
        } else {
            Log.d(TAG, "Number is in contacts, allowing call: $phoneNumber")
        }
    }

    private fun blockCall(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ANSWER_PHONE_CALLS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    @Suppress("DEPRECATION")
                    telecomManager.endCall()
                    true
                } else {
                    Log.w(TAG, "Missing ANSWER_PHONE_CALLS permission, cannot block call")
                    false
                }
            } else {
                Log.w(TAG, "Call blocking limited on Android < 9")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking call", e)
            false
        }
    }

    private fun incrementBlockedCallsCount(context: Context) {
        val nextCount = ProtectionPreferences(context).incrementBlockedCallsCount()
        Log.d(TAG, "Blocked calls count updated: $nextCount")
    }
}
