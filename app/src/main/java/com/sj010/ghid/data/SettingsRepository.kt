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

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return AppSettings()
        val loaded = try {
            json.decodeFromString<AppSettings>(raw)
        } catch (_: Exception) {
            return AppSettings()
        }
        return migrate(loaded)
    }

    /**
     * The mapper row is back on its keyboard defaults, so installs that saved one of the
     * interim defaults revert with it. A customised row is never touched.
     */
    private fun migrate(loaded: AppSettings): AppSettings {
        if (loaded.interactiveMapperKeys !in STALE_MAPPER_DEFAULTS) return loaded
        return loaded.copy(interactiveMapperKeys = AppSettings.DEFAULT_INTERACTIVE_MAPPER_KEYS)
            .also(::persist)
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val current = _settings.value
        val updated = transform(current)
        if (updated == current) return
        _settings.value = updated
        persist(updated)
    }

    /** Writes one interactive mapper slot, padding the row out to its full width first. */
    fun updateInteractiveMapperSlot(index: Int, token: String) {
        if (index !in 0 until AppSettings.MAPPER_SLOTS) return
        update { settings ->
            val slots = settings.interactiveMapperKeys
                .toMutableList()
                .apply { while (size < AppSettings.MAPPER_SLOTS) add("") }
            slots[index] = token
            settings.copy(interactiveMapperKeys = slots)
        }
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
        /** Interim defaults, matched exactly so a customised row always survives. */
        private val STALE_MAPPER_DEFAULTS = listOf(
            listOf("lclick", "rclick", "mclick", ""), // crest mouse buttons
            listOf("esc", "tab", "shift+tab", "")     // pre-0.2.2 Stab row
        )
    }
}
