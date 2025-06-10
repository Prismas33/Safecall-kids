package com.safecallkids.app

import android.content.Context
import android.content.SharedPreferences
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

/**
 * CallScreeningService é a forma moderna de bloquear chamadas no Android 10+
 * Este serviço é chamado pelo sistema para cada chamada recebida
 */
class CallScreeningService : CallScreeningService() {
    
    private val TAG = "CallScreeningService"
    
    override fun onScreenCall(callDetails: Call.Details) {
        Log.d(TAG, "Screening call from: ${callDetails.handle}")
        
        val phoneNumber = callDetails.handle?.schemeSpecificPart
        
        if (phoneNumber != null) {
            val shouldBlock = shouldBlockCall(phoneNumber)
              if (shouldBlock) {
                Log.i(TAG, "Blocking call from: $phoneNumber")
                
                // Bloquear e rejeitar a chamada
                val response = CallScreeningService.CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false) // Manter no log para estatísticas
                    .setSkipNotification(true) // Não mostrar notificação
                    .build()
                
                respondToCall(callDetails, response)
                
                // Incrementar contador de chamadas bloqueadas
                incrementBlockedCallsCount()
                
                // Atualizar notificação do serviço
                updateServiceNotification()
                  } else {
                Log.d(TAG, "Allowing call from: $phoneNumber")
                
                // Permitir a chamada
                val response = CallScreeningService.CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .build()
                
                respondToCall(callDetails, response)
            }        } else {
            Log.w(TAG, "Call with null phone number, allowing by default")
            
            // Se não conseguir obter o número, permitir por segurança
            val response = CallScreeningService.CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()
            
            respondToCall(callDetails, response)
        }
    }
    
    private fun shouldBlockCall(phoneNumber: String): Boolean {
        return try {
            val contactsHelper = ContactsHelper(this)
            val isContact = contactsHelper.isNumberInContacts(phoneNumber)
            
            Log.d(TAG, "Number $phoneNumber is contact: $isContact")
            
            // Bloquear se NÃO estiver nos contatos
            !isContact
            
        } catch (e: SecurityException) {
            Log.e(TAG, "No permission to read contacts", e)
            // Se não tiver permissão, não bloquear por segurança
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking contacts", e)
            // Em caso de erro, não bloquear por segurança
            false
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
            // Enviar broadcast para atualizar notificação
            val intent = android.content.Intent("com.safecallkids.app.UPDATE_NOTIFICATION")
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification", e)
        }
    }
}
