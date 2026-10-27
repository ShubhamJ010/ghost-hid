package com.sj010.ghid.data

import android.content.Context
import com.sj010.ghid.domain.model.Combo
import com.sj010.ghid.domain.model.Payload
import com.sj010.ghid.domain.model.TargetOs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ComboRepository(
    private val context: Context,
    private val payloadRepository: PayloadRepository? = null
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val file: File
        get() = File(context.getExternalFilesDir(null), "combos.json")

    private val _combos = MutableStateFlow<List<Combo>>(emptyList())
    val combos: StateFlow<List<Combo>> = _combos.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        load()
    }

    private fun isoNow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun load(): List<Combo> {
        val f = file
        if (!f.exists() || f.length() == 0L) {
            _combos.value = emptyList()
            return emptyList()
        }
        return try {
            val content = f.readText()
            val list = json.decodeFromString<List<Combo>>(content)
            _combos.value = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(list: List<Combo>) {
        _combos.value = list
        scope.launch {
            try {
                val content = json.encodeToString(list)
                file.writeText(content)
            } catch (ignored: Exception) {}
        }
    }

    private val builtInEmoji = mapOf(
        "Login" to "🔑",
        "Lock" to "🔒",
        "Sleep" to "💤",
        "MacSleep" to "🌙",
        "ForceQuit" to "⚡",
        "PlayPause" to "⏯️"
    )

    fun ensureDefaults(): List<Combo> {
        val loaded = load()
        var migrated = false
        val current = loaded.map { combo ->
            var updated = combo
            val emoji = builtInEmoji.entries
                .firstOrNull { it.key.equals(combo.name, ignoreCase = true) }
                ?.value
            if (updated.emoji.isNullOrBlank() && emoji != null) {
                updated = updated.copy(emoji = emoji)
                migrated = true
            }
            if (updated.name.equals("Sleep", ignoreCase = true) && (updated.effectiveSequence.contains("win+x") || updated.effectiveSequence.contains("->") || updated.effectiveSequence.startsWith("$"))) {
                updated = updated.copy(
                    sequence = "{WIN_SLEEP}",
                    payloads = mapOf("WIN_SLEEP" to Payload(name = "WIN_SLEEP", type = "raw", value = "02")),
                    description = "Hardware ACPI sleep sequence"
                )
                migrated = true
            }
            if (updated.name.equals("PlayPause", ignoreCase = true) && (updated.effectiveSequence.contains("->") || updated.effectiveSequence.startsWith("$"))) {
                updated = updated.copy(
                    sequence = "{PLAY_PAUSE}",
                    payloads = mapOf("PLAY_PAUSE" to Payload(name = "PLAY_PAUSE", type = "raw", value = "cd00")),
                    description = "Consumer media play/pause toggle"
                )
                migrated = true
            }
            if (updated.name.equals("Login", ignoreCase = true) && updated.effectiveSequence.contains("->")) {
                updated = updated.copy(
                    sequence = "{ctrl+alt+del}{1s}{Escape}{800ms}{USERNAME}{800ms}{Tab}{800ms}{PASSWORD}{800ms}{Enter}"
                )
                migrated = true
            }
            if (updated.name.equals("Lock", ignoreCase = true) && (updated.effectiveSequence.contains("mod+l") || updated.sequence.contains("mod+l") || !updated.effectiveSequence.contains("{win+l}"))) {
                updated = updated.copy(
                    sequence = "{win+l}",
                    description = "Instant Windows lock screen (Win + L)"
                )
                migrated = true
            }
            if (updated.name.equals("MacSleep", ignoreCase = true) && (updated.effectiveSequence.contains("->") || updated.sequence.contains("mod+ctrl+q"))) {
                updated = updated.copy(
                    sequence = "{cmd+ctrl+q}{500ms}{Escape}",
                    description = "Instant macOS lock and display sleep"
                )
                migrated = true
            }
            if (updated.name.equals("ForceQuit", ignoreCase = true) && (updated.effectiveSequence.contains("->") || updated.sequence.contains("mod+alt+esc"))) {
                updated = updated.copy(
                    sequence = "{cmd+alt+esc}{400ms}{Enter}{300ms}{Enter}",
                    description = "Force quit frontmost unresponsive app"
                )
                migrated = true
            }
            updated
        }

        val hasExactBuiltIns = current.size >= 6 &&
                current.any { it.name.equals("Login", ignoreCase = true) } &&
                current.any { it.name.equals("Lock", ignoreCase = true) } &&
                current.any { it.name.equals("Sleep", ignoreCase = true) } &&
                current.any { it.name.equals("MacSleep", ignoreCase = true) } &&
                current.any { it.name.equals("ForceQuit", ignoreCase = true) } &&
                current.any { it.name.equals("PlayPause", ignoreCase = true) }

        if (!hasExactBuiltIns) {
            val defaults = listOf(
                Combo(
                    name = "Login",
                    sequence = "{ctrl+alt+del}{1s}{Escape}{800ms}{USERNAME}{800ms}{Tab}{800ms}{PASSWORD}{800ms}{Enter}",
                    os = TargetOs.WINDOWS.token,
                    emoji = "🔑",
                    description = "Windows unlock & credential injection",
                    favorite = true,
                    createdAt = isoNow()
                ),
                Combo(
                    name = "Lock",
                    sequence = "{win+l}",
                    os = TargetOs.WINDOWS.token,
                    emoji = "🔒",
                    description = "Instant Windows lock screen (Win + L)",
                    favorite = true,
                    createdAt = isoNow()
                ),
                Combo(
                    name = "Sleep",
                    sequence = "{WIN_SLEEP}",
                    os = TargetOs.WINDOWS.token,
                    emoji = "💤",
                    description = "Hardware ACPI sleep sequence",
                    favorite = true,
                    payloads = mapOf("WIN_SLEEP" to Payload(name = "WIN_SLEEP", type = "raw", value = "02")),
                    createdAt = isoNow()
                ),
                Combo(
                    name = "MacSleep",
                    sequence = "{cmd+ctrl+q}{500ms}{Escape}",
                    os = TargetOs.MACOS.token,
                    emoji = "🌙",
                    description = "Instant macOS lock and display sleep",
                    favorite = true,
                    createdAt = isoNow()
                ),
                Combo(
                    name = "ForceQuit",
                    sequence = "{cmd+alt+esc}{400ms}{Enter}{300ms}{Enter}",
                    os = TargetOs.MACOS.token,
                    emoji = "⚡",
                    description = "Force quit frontmost unresponsive app",
                    favorite = true,
                    createdAt = isoNow()
                ),
                Combo(
                    name = "PlayPause",
                    sequence = "{PLAY_PAUSE}",
                    os = TargetOs.MACOS.token,
                    emoji = "⏯️",
                    description = "Consumer media play/pause toggle",
                    favorite = true,
                    payloads = mapOf("PLAY_PAUSE" to Payload(name = "PLAY_PAUSE", type = "raw", value = "cd00")),
                    createdAt = isoNow()
                )
            )
            defaults.forEach { autoProvision(it) }
            save(defaults)
            return defaults
        }

        if (migrated || current != loaded) {
            current.forEach { autoProvision(it) }
            save(current)
        }
        return current
    }

    private fun autoProvision(combo: Combo) {
        if (payloadRepository == null || combo.payloads.isNullOrEmpty()) return
        combo.payloads.forEach { (name, payload) ->
            if (payloadRepository.get(name) == null) {
                payloadRepository.set(payload.name, payload.value ?: "", payload.secure, payload.type)
            }
        }
    }

    fun addOrUpdate(combo: Combo) {
        val list = _combos.value.toMutableList()
        val idx = list.indexOfFirst { it.name.equals(combo.name, ignoreCase = true) }
        val normalized = combo.copy(os = TargetOs.fromComboTag(combo.os).token)
        val itemToSave = if (normalized.createdAt.isEmpty()) normalized.copy(createdAt = isoNow()) else normalized

        autoProvision(itemToSave)

        if (idx >= 0) {
            list[idx] = itemToSave
        } else {
            list.add(itemToSave)
        }
        save(list)
    }

    fun toggleFavorite(name: String) {
        val list = _combos.value.map {
            if (it.name.equals(name, ignoreCase = true)) {
                it.copy(favorite = !it.favorite)
            } else it
        }
        save(list)
    }

    fun delete(name: String) {
        val list = _combos.value.filterNot { it.name.equals(name, ignoreCase = true) }
        save(list)
    }

    fun find(name: String): Combo? {
        return _combos.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}
