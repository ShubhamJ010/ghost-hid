package com.sj010.ghid.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Tracks how often each payload pill is dispatched from the landscape stream deck.
 *
 * Payloads are sorted most-used-first there, so this deliberately lives beside
 * [ComboUsageRepository] instead of polluting combo counts with payload names.
 */
class PayloadUsageRepository(context: Context) {

    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val lock = Any()
    private val _useCounts = MutableStateFlow(readUseCounts())
    val useCounts: StateFlow<Map<String, Int>> = _useCounts.asStateFlow()

    fun recordUse(name: String) {
        val normalizedName = normalizeName(name)
        if (normalizedName.isEmpty()) return

        synchronized(lock) {
            val nextCount = (_useCounts.value[normalizedName] ?: 0) + 1
            preferences.edit().putInt("$KEY_PREFIX$normalizedName", nextCount).apply()
            _useCounts.value = _useCounts.value + (normalizedName to nextCount)
        }
    }

    private fun readUseCounts(): Map<String, Int> = preferences.all
        .mapNotNull { (key, value) ->
            val name = key.removePrefix(KEY_PREFIX)
            if (key.startsWith(KEY_PREFIX) && value is Int) name to value else null
        }
        .toMap()

    private fun normalizeName(name: String): String = name.trim().lowercase(Locale.ROOT)

    private companion object {
        const val FILE_NAME = "payload_usage"
        const val KEY_PREFIX = "usage:"
    }
}
