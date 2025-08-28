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

class CallReceiver : BroadcastReceiver() {
    private val TAG = "CallReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            // Gate early: only act if user explicitly activated protection and app is properly configured
            if (!isProtectionActive(context)) {
                Log.d(TAG, "Protection not active; ignoring phone state broadcast")
                return
            }

            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            @Suppress("DEPRECATION")
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

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

    private fun isProtectionActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences("safecall_prefs", Context.MODE_PRIVATE)
        val userEnabled = prefs.getBoolean("all_setup_completed", false)
        if (!userEnabled) return false

        // Require basic permissions
        val hasAnswer = ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
        val hasReadContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!hasAnswer || !hasReadContacts) return false

        // On Android 10+ ensure we're default dialer (or call screening role implies it)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return try {
                val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                val isDefault = telecom.defaultDialerPackage == context.packageName
                if (!isDefault) Log.w(TAG, "Not default dialer on Q+; will not block via receiver")
                isDefault
            } catch (e: Exception) {
                Log.e(TAG, "Error checking default dialer", e)
                false
            }
        }
        return true
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
        val prefs = context.getSharedPreferences("safecall_prefs", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("blocked_calls_count", 0)
        prefs.edit().putInt("blocked_calls_count", currentCount + 1).apply()
        Log.d(TAG, "Blocked calls count updated: ${currentCount + 1}")
    }
}
