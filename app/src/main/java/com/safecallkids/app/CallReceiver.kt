package com.safecallkids.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

class CallReceiver : BroadcastReceiver() {
    private val TAG = "CallReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            @Suppress("DEPRECATION")
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            if (phoneNumber == null) {
                Log.w(TAG, "Número da chamada não disponível (pode ser restrição do Android)")
                return
            }
            Log.d(TAG, "Phone state changed: $state, Number: $phoneNumber")
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    handleIncomingCall(context, phoneNumber)
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    Log.d(TAG, "Call ended or idle")
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    Log.d(TAG, "Call answered")
                }
            }
        }
    }

    private fun handleIncomingCall(context: Context, phoneNumber: String) {
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ANSWER_PHONE_CALLS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
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
        val prefs = context.getSharedPreferences("safecall_prefs", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("blocked_calls_count", 0)
        prefs.edit().putInt("blocked_calls_count", currentCount + 1).apply()
        Log.d(TAG, "Blocked calls count updated: ${currentCount + 1}")
    }
}
