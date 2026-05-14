package com.gsshop.mobile.flutter.flutter_hawk

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.orhanobut.hawk.Hawk

// TODO: remove after next major release — migrates Hawk data that was stored without Conceal
// encryption. The intermediate B build (com.github.GundamD:conceal:v1.1.3-16kb-fixed-3) had no
// .so bundled, so Hawk fell back to NoEncryption.
//
// Hawk2 serialized format (HawkSerializer):
//   "<keyClassName>#<valueClassName>#<dataType>V@<cipherText>"
// NoEncryption cipherText = Base64(gsonJson.getBytes())   ← NOT plaintext
// SharedPreferences file name = "Hawk2"  (HawkBuilder.STORAGE_TAG_DO_NOT_CHANGE)
internal class ConcealMigrationHelper(context: Context) {

    private val hawkPrefs = context.getSharedPreferences("Hawk2", Context.MODE_PRIVATE)

    // Each attempted key stored as a boolean entry; presence = attempted (success or fail).
    private val attemptedPrefs = context.getSharedPreferences(
        "flutter_hawk_migration_v1", Context.MODE_PRIVATE
    )

    private val gson = Gson()

    /**
     * Reads [key] from B's NoEncryption-formatted Hawk storage and re-writes it to the current
     * Conceal-encrypted Hawk. Returns the migrated value on success, null otherwise.
     * Each key is attempted exactly once regardless of outcome.
     */
    fun tryMigrate(key: String): String? {
        if (attemptedPrefs.contains(key)) return null

        val rawValue = hawkPrefs.getString(key, null)
        if (rawValue == null) {
            markAttempted(key)
            return null
        }

        // Format: "<keyClass>#<valueClass>#<dataType>V@<cipherText>"
        // Split with limit=3 so the base64 payload is never split even if it contained '#'
        val parts = rawValue.split("#", limit = 3)
        if (parts.size < 3 || parts[0] != "java.lang.String") {
            markAttempted(key)
            return null
        }

        // parts[2] = "<dataType>V@<cipherText>", e.g. "0V@ImhlbGxvIg=="
        val typeAndCipher = parts[2]
        val atIdx = typeAndCipher.indexOf('@')
        if (atIdx < 0) {
            markAttempted(key)
            return null
        }
        val cipherText = typeAndCipher.substring(atIdx + 1)

        // NoEncryption: cipherText = Base64(gsonJson.getBytes())
        val jsonBytes = try {
            Base64.decode(cipherText, Base64.DEFAULT)
        } catch (e: Exception) {
            markAttempted(key)
            return null
        }
        val json = String(jsonBytes)

        val value = try {
            gson.fromJson(json, String::class.java)
        } catch (e: Exception) {
            markAttempted(key)
            return null
        } ?: run {
            markAttempted(key)
            return null
        }

        return try {
            Hawk.put(key, value)
            hawkPrefs.edit().remove(key).apply()
            markAttempted(key)
            value
        } catch (e: Exception) {
            markAttempted(key)
            null
        }
    }

    private fun markAttempted(key: String) {
        attemptedPrefs.edit().putBoolean(key, true).apply()
    }
}
