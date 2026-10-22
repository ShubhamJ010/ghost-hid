package com.poc.hiddebug.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AndroidKeystoreVault(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "ghost_hid_secure_vault",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun vaultKey(name: String): String {
        return "ghost-hid:snippet:" + name.trim().lowercase()
    }

    fun get(name: String): String? {
        val key = vaultKey(name)
        val value = prefs.getString(key, null)
        if (value.isNullOrEmpty() && name.equals("password", ignoreCase = true)) {
            // fallback
            return prefs.getString("password", null)
        }
        return value
    }

    fun set(name: String, value: String) {
        prefs.edit().putString(vaultKey(name), value).apply()
    }

    fun delete(name: String) {
        prefs.edit().remove(vaultKey(name)).apply()
    }

    fun isAvailable(): Boolean = true
}
