package com.safecallkids.app

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.util.Log

/**
 * Helper class to manage contacts and phone number matching
 * Handles phone number normalization for different country formats
 */
class ContactsHelper(private val context: Context) {
    
    private val TAG = "ContactsHelper"
    
    /**
     * Check if a phone number exists in the user's contacts
     * Normalizes both input number and contact numbers for comparison
     */
    fun isNumberInContacts(phoneNumber: String): Boolean {
        try {
            // Normalize the input number for comparison
            val normalizedInput = normalizePhoneNumber(phoneNumber)
            
            // Query contacts for phone numbers
            val cursor = queryContactNumbers()
            cursor?.use { c ->
                while (c.moveToNext()) {
                    val contactNumber = c.getString(0)
                    val normalizedContact = normalizePhoneNumber(contactNumber ?: "")
                    
                    // Compare normalized numbers
                    if (comparePhoneNumbers(normalizedInput, normalizedContact)) {
                        Log.d(TAG, "Number $phoneNumber found in contacts as $contactNumber")
                        return true
                    }
                }
            }
            
            Log.d(TAG, "Number $phoneNumber not found in contacts")
            return false
            
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied to access contacts", e)
            return false // If no permission, assume not in contacts (safer)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking contacts", e)
            return false
        }
    }
    
    /**
     * Get the total count of contacts with phone numbers
     */
    fun getContactsCount(): Int {
        return try {
            val cursor = queryContactNumbers()
            cursor?.use { c ->
                c.count
            } ?: 0
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied to count contacts", e)
            0
        } catch (e: Exception) {
            Log.e(TAG, "Error counting contacts", e)
            0
        }
    }
    
    /**
     * Query the contacts database for phone numbers
     */
    private fun queryContactNumbers(): Cursor? {
        return try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to query contacts", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts", e)
            null
        }
    }
    
    /**
     * Normalize phone number for comparison
     * Removes spaces, dashes, parentheses and handles country codes
     */
    private fun normalizePhoneNumber(phoneNumber: String): String {
        if (phoneNumber.isBlank()) return ""
        
        // Remove all non-digit characters except +
        var normalized = phoneNumber.replace(Regex("[^+\\d]"), "")
        
        // Handle Brazilian phone numbers specifically
        normalized = when {
            // Remove +55 (Brazil country code)
            normalized.startsWith("+55") -> normalized.substring(3)
            // Remove just + if no country code
            normalized.startsWith("+") -> normalized.substring(1)
            else -> normalized
        }
        
        // Remove leading zeros (common in some formats)
        normalized = normalized.trimStart('0')
        
        // For Brazilian mobile numbers, handle 9th digit addition
        if (normalized.length == 11 && normalized.startsWith("1") && normalized[2] == '9') {
            // This is likely a mobile number with the 9th digit added
            // Also check for the version without the 9th digit
        }
        
        Log.v(TAG, "Normalized $phoneNumber to $normalized")
        return normalized
    }
    
    /**
     * Compare two normalized phone numbers
     * Handles different formats and lengths
     */
    @Suppress("DEPRECATION")
    private fun comparePhoneNumbers(number1: String, number2: String): Boolean {
        if (number1.isEmpty() || number2.isEmpty()) return false

        // Direct comparison
        if (number1 == number2) return true

        // For Brazilian numbers, check with/without 9th digit
        if (number1.length == 11 && number2.length == 10) {
            val withoutNinth = number1.substring(0, 2) + number1.substring(3)
            if (withoutNinth == number2) return true
        }
        if (number2.length == 11 && number1.length == 10) {
            val withoutNinth = number2.substring(0, 2) + number2.substring(3)
            if (withoutNinth == number1) return true
        }
        val suffix1 = number1.takeLast(8)
        val suffix2 = number2.takeLast(8)
        if (suffix1.length == 8 && suffix2.length == 8 && suffix1 == suffix2) {
            return true
        }
        // Use Android's built-in phone number comparison (API level 29+ uses areSamePhoneNumber)
        return try {
            val sdkInt = android.os.Build.VERSION.SDK_INT
            if (sdkInt >= 29) {
                val countryIso = try {
                    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
                    tm?.networkCountryIso?.uppercase() ?: java.util.Locale.getDefault().country
                } catch (e: Exception) {
                    java.util.Locale.getDefault().country
                }
                PhoneNumberUtils.areSamePhoneNumber(number1, number2, countryIso)
            } else {
                PhoneNumberUtils.compare(number1, number2)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error using PhoneNumberUtils.compare/areSamePhoneNumber", e)
            false
        }
    }
    
    /**
     * Get a sample of contact numbers for debugging
     */
    fun getContactSample(limit: Int = 5): List<String> {
        val samples = mutableListOf<String>()
        try {
            val cursor = queryContactNumbers()
            cursor?.use { c ->
                var count = 0
                while (c.moveToNext() && count < limit) {
                    val number = c.getString(0)
                    if (!number.isNullOrBlank()) {
                        samples.add(number)
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting contact samples", e)
        }
        return samples
    }
}
