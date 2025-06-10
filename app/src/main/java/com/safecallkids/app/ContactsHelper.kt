package com.safecallkids.app

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log

class ContactsHelper(private val context: Context) {
    
    private val TAG = "ContactsHelper"
    
    fun getContactsCount(): Int {
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                null
            )
            val count = cursor?.count ?: 0
            cursor?.close()
            count
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied to read contacts", e)
            0
        }
    }
    
    fun getAllContactNumbers(): Set<String> {
        val contactNumbers = mutableSetOf<String>()
        
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )
            
            cursor?.use {
                val phoneNumberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                
                while (it.moveToNext()) {
                    val phoneNumber = it.getString(phoneNumberColumn)
                    if (phoneNumber != null) {
                        // Normalizar o número (remover espaços, hífens, parênteses)
                        val normalizedNumber = normalizePhoneNumber(phoneNumber)
                        contactNumbers.add(normalizedNumber)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied to read contacts", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading contacts", e)
        }
        
        Log.d(TAG, "Loaded ${contactNumbers.size} contact numbers")
        return contactNumbers
    }
    
    fun isNumberInContacts(phoneNumber: String): Boolean {
        val contactNumbers = getAllContactNumbers()
        val normalizedIncoming = normalizePhoneNumber(phoneNumber)
        
        // Verificar correspondência exata
        if (contactNumbers.contains(normalizedIncoming)) {
            return true
        }
        
        // Verificar correspondência parcial (últimos 8-10 dígitos)
        return contactNumbers.any { contactNumber ->
            val contactSuffix = contactNumber.takeLast(8)
            val incomingSuffix = normalizedIncoming.takeLast(8)
            contactSuffix == incomingSuffix && contactSuffix.length >= 8
        }
    }
    
    private fun normalizePhoneNumber(phoneNumber: String): String {
        // Remove todos os caracteres não numéricos
        val digits = phoneNumber.replace(Regex("[^0-9+]"), "")
        
        // Remove o símbolo + e códigos de país comuns
        return when {
            digits.startsWith("+55") -> digits.substring(3) // Brasil
            digits.startsWith("55") && digits.length > 10 -> digits.substring(2)
            digits.startsWith("0") -> digits.substring(1)
            else -> digits
        }
    }
}
