package com.sj010.ghid.data

import android.content.Context
import com.sj010.ghid.domain.model.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    private fun loadSettings(): AppSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return AppSettings()
        return try {
            json.decodeFromString<AppSettings>(raw)
        } catch (_: Exception) {
            AppSettings()
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val current = _settings.value
        val updated = transform(current)
        if (updated == current) return
        _settings.value = updated
        persist(updated)
    }

    fun resetToDefaults() {
        val defaults = AppSettings()
        _settings.value = defaults
        persist(defaults)
    }

    private fun persist(settings: AppSettings) {
        scope.launch {
            try {
                val encoded = json.encodeToString(AppSettings.serializer(), settings)
                prefs.edit().putString(KEY_SETTINGS, encoded).apply()
            } catch (_: Exception) {
                // Ignore persistence errors gracefully
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "ghosthid_app_settings"
        private const val KEY_SETTINGS = "settings_payload_v1"
    }
}
