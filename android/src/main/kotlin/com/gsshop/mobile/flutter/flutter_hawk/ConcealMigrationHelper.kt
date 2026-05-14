package com.gsshop.mobile.flutter.flutter_hawk

import android.content.Context
import com.google.gson.Gson
import com.orhanobut.hawk.Hawk

// TODO: remove after next major release — migrates Hawk data that was stored without Conceal
// encryption. The intermediate B build (com.github.GundamD:conceal:v1.1.3-16kb-fixed-3) had no
// .so bundled, so Hawk fell back to NoEncryption and stored values as "java.lang.String@@\"val\"".
internal class ConcealMigrationHelper(context: Context) {

    // Hawk's default SharedPreferences file name (SharedPreferencesStorage.HAWK)
    private val hawkPrefs = context.getSharedPreferences("HAWK", Context.MODE_PRIVATE)

    // Each attempted key is stored as a boolean entry; presence = attempted (success or fail).
    private val attemptedPrefs = context.getSharedPreferences(
        "flutter_hawk_migration_v1", Context.MODE_PRIVATE
    )

    private val gson = Gson()

    /**
     * Tries to read [key] from B's NoEncryption storage and re-write it to the current
     * Conceal-encrypted Hawk. Returns the migrated value on success, null otherwise.
     * After the first attempt (success or fail), the key is recorded so it is never retried.
     */
    fun tryMigrate(key: String): String? {
        if (attemptedPrefs.contains(key)) return null

        val rawValue = hawkPrefs.getString(key, null)
        if (rawValue == null) {
            markAttempted(key)
            return null
        }

        // NoEncryption format written by Hawk's DataUtil: "<className>@@<gsonJson>"
        val sepIdx = rawValue.indexOf("@@")
        if (sepIdx < 0 || rawValue.substring(0, sepIdx) != "java.lang.String") {
            markAttempted(key)
            return null
        }

        val json = rawValue.substring(sepIdx + 2)
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
