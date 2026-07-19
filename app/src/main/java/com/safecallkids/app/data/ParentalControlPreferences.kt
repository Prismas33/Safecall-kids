package com.safecallkids.app.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

class ParentalControlPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(ProtectionPreferences.PREFS_NAME, Context.MODE_PRIVATE)

    val isPinConfigured: Boolean
        get() = !prefs.getString(KEY_PARENTAL_PIN_HASH, null).isNullOrBlank() &&
            !prefs.getString(KEY_PARENTAL_PIN_SALT, null).isNullOrBlank()

    fun setPin(pin: String) {
        val salt = ByteArray(SALT_SIZE_BYTES)
        SecureRandom().nextBytes(salt)
        prefs.edit()
            .putString(KEY_PARENTAL_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PARENTAL_PIN_HASH, hashPin(pin, salt))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltText = prefs.getString(KEY_PARENTAL_PIN_SALT, null) ?: return false
        val expectedHash = prefs.getString(KEY_PARENTAL_PIN_HASH, null) ?: return false
        val salt = Base64.decode(saltText, Base64.NO_WRAP)
        return hashPin(pin, salt) == expectedHash
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PARENTAL_PIN_SALT)
            .remove(KEY_PARENTAL_PIN_HASH)
            .apply()
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    companion object {
        private const val KEY_PARENTAL_PIN_HASH = "parental_pin_hash"
        private const val KEY_PARENTAL_PIN_SALT = "parental_pin_salt"
        private const val SALT_SIZE_BYTES = 16
    }
}
