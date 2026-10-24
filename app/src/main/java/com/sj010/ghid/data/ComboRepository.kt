package com.sj010.ghid.data

import android.content.Context
import com.sj010.ghid.domain.model.Combo
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

class ComboRepository(private val context: Context) {

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

    /** Icons for the built-in combos; a combo name the user set keeps its own emoji. */
    private val builtInEmoji = mapOf(
        "Login" to "🔑",
        "Lock" to "🔒",
        "Sleep" to "😴",
        "MacSleep" to "🌙",
        "ForceQuit" to "⚡",
        "PlayPause" to "⏯️"
    )

    fun ensureDefaults(): List<Combo> {
        val loaded = load()
        val current = loaded.map { combo ->
            val emoji = builtInEmoji.entries
                .firstOrNull { it.key.equals(combo.name, ignoreCase = true) }
                ?.value
            if (combo.emoji.isNullOrBlank() && emoji != null) combo.copy(emoji = emoji) else combo
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
                    keys = "ctrl+alt+del ->{1.0} esc ->{0.8} \$USERNAME ->{0.8} tab ->{0.8} \$PASSWORD ->{0.8} enter",
                    os = TargetOs.WINDOWS.token,
                    emoji = "🔑",
                    description = "Windows unlock & credential injection",
                    favorite = true,
                    createdAt = isoNow()
                ),
                Combo(
                    name = "Lock",
                    keys = "win+l",
                    os = TargetOs.WINDOWS.token,
                    emoji = "🔒",
                    description = "Instant Windows lock screen (Win + L)",
                    favorite = true,
                    createdAt = isoNow()
                ),
                Combo(
                    name = "Sleep",
                    keys = "win+x ->{0.5} u ->{0.3} s ->{5.0} !DISCONNECT ->{0.5} !EXIT",
                    os = TargetOs.WINDOWS.token,
                    emoji = "😴",
                    description = "Windows sleep sequence + countdown exit",
                    favorite = true,
                    createdAt = isoNow()
                ),
                Combo(
                    name = "MacSleep",
                    keys = "cmd+ctrl+q ->{0.5} esc",
                    os = TargetOs.MACOS.token,
                    emoji = "🌙",
                    description = "Instant macOS lock and display sleep",
                    favorite = true,
                    createdAt = isoNow()
                ),
                Combo(
                    name = "ForceQuit",
                    keys = "cmd+opt+esc ->{0.4} enter ->{0.3} enter",
                    os = TargetOs.MACOS.token,
                    emoji = "⚡",
                    description = "Force quit frontmost unresponsive app",
                    favorite = true,
                    createdAt = isoNow()
                ),
                Combo(
                    name = "PlayPause",
                    keys = "raw:cd00 ->{0.06} raw:0000",
                    os = TargetOs.MACOS.token,
                    emoji = "⏯️",
                    description = "Consumer media play/pause toggle",
                    favorite = true,
                    createdAt = isoNow()
                )
            )
            save(defaults)
            return defaults
        }
        // Backfill icons for built-ins saved before emoji metadata existed.
        if (current != loaded) save(current)
        return current
    }

    fun addOrUpdate(combo: Combo) {
        val list = _combos.value.toMutableList()
        val idx = list.indexOfFirst { it.name.equals(combo.name, ignoreCase = true) }
        val normalized = combo.copy(os = TargetOs.fromComboTag(combo.os).token)
        val itemToSave = if (normalized.createdAt.isEmpty()) normalized.copy(createdAt = isoNow()) else normalized

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
