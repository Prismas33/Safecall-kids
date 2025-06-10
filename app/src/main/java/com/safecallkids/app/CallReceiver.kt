package com.safecallkids.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import java.lang.reflect.Method

class CallReceiver : BroadcastReceiver() {
    
    private val TAG = "CallReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            // EXTRA_INCOMING_NUMBER está depreciado, mas ainda é necessário para interceptar chamadas em versões antigas.
            // Em Android 10+ e como discador padrão, use APIs modernas.
            @Suppress("DEPRECATION") // Necessário para compatibilidade com versões antigas do Android
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
            
            // Tentar bloquear a chamada
            val blocked = blockCall(context)
            
            if (blocked) {
                // Incrementar contador de chamadas bloqueadas
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
            // Método 1: Tentar usar TelecomManager (Android 9+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                if (androidx.core.content.ContextCompat.checkSelfPermission(
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
                // Método 2: Usar reflexão para acessar ITelephony (Android mais antigo)
                blockCallUsingReflection()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking call", e)
            false
        }
    }
    
    private fun blockCallUsingReflection(): Boolean {
        return try {
            val telephonyManager = android.telephony.TelephonyManager::class.java
            val getITelephony: Method = telephonyManager.getDeclaredMethod("getITelephony")
            getITelephony.isAccessible = true
            
            val telephonyService = getITelephony.invoke(telephonyManager)
            val endCall: Method = telephonyService.javaClass.getDeclaredMethod("endCall")
            endCall.invoke(telephonyService)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to block call using reflection", e)
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
