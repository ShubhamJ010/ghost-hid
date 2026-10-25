package com.sj010.ghid.data

import android.content.Context
import com.sj010.ghid.domain.model.Payload
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

class PayloadRepository(
    private val context: Context,
    private val vault: AndroidKeystoreVault
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val file: File
        get() = File(context.getExternalFilesDir(null), "payloads.json")

    private val _payloads = MutableStateFlow<List<Payload>>(emptyList())
    val payloads: StateFlow<List<Payload>> = _payloads.asStateFlow()

    /** Deprecated alias for smooth UI migration */
    val snippets: StateFlow<List<Payload>> get() = payloads

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        load()
    }

    private fun isoNow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun load(): List<Payload> {
        val f = file
        if (!f.exists() || f.length() == 0L) {
            _payloads.value = emptyList()
            return emptyList()
        }
        return try {
            val content = f.readText()
            val list = json.decodeFromString<List<Payload>>(content)
            // Populate secure payloads from Android Keystore
            val populated = list.map { p ->
                if (p.secure) {
                    val secret = vault.get(p.name)
                    p.copy(value = secret ?: "")
                } else p
            }
            _payloads.value = populated
            populated
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(list: List<Payload>) {
        _payloads.value = list
        scope.launch {
            try {
                // Strip secret values before writing to disk
                val toSave = list.map { p ->
                    if (p.secure) p.copy(value = "") else p
                }
                val content = json.encodeToString(toSave)
                file.writeText(content)
            } catch (ignored: Exception) {}
        }
    }

    fun ensureDefaults(): List<Payload> {
        val current = load()
        val hasUser = current.any { it.name.equals("username", ignoreCase = true) }
        val hasPass = current.any { it.name.equals("password", ignoreCase = true) }
        val hasEmail = current.any { it.name.equals("email", ignoreCase = true) }
        val hasSleep = current.any { it.name.equals("WIN_SLEEP", ignoreCase = true) || it.name.equals("sleep_windows", ignoreCase = true) || it.name.equals("sleep", ignoreCase = true) }
        val hasPlayPause = current.any { it.name.equals("PLAY_PAUSE", ignoreCase = true) || it.name.equals("play_pause", ignoreCase = true) || it.name.equals("playpause", ignoreCase = true) }

        if (!hasUser || !hasPass || !hasEmail || !hasSleep || !hasPlayPause) {
            val list = current.toMutableList()
            val now = isoNow()
            if (!hasUser) {
                list.add(Payload(name = "username", type = "type", value = "", secure = false, createdAt = now, updatedAt = now))
            }
            if (!hasPass) {
                list.add(Payload(name = "password", type = "type", value = vault.get("password") ?: "", secure = true, createdAt = now, updatedAt = now))
            }
            if (!hasEmail) {
                list.add(Payload(name = "email", type = "type", value = vault.get("email") ?: "", secure = true, createdAt = now, updatedAt = now))
            }
            if (!hasSleep) {
                list.add(Payload(name = "WIN_SLEEP", type = "raw", value = "02", secure = false, createdAt = now, updatedAt = now))
            }
            if (!hasPlayPause) {
                list.add(Payload(name = "PLAY_PAUSE", type = "raw", value = "cd00", secure = false, createdAt = now, updatedAt = now))
            }
            save(list)
            return list
        }
        return current
    }

    fun set(name: String, value: String, secure: Boolean, type: String = "type") {
        val cleanName = name.trim()
        val now = isoNow()
        val pType = if (type.equals("raw", ignoreCase = true)) "raw" else "type"
        if (secure) {
            vault.set(cleanName, value)
        } else {
            vault.delete(cleanName)
        }

        val list = _payloads.value.toMutableList()
        val idx = list.indexOfFirst { it.name.equals(cleanName, ignoreCase = true) }
        if (idx >= 0) {
            val existing = list[idx]
            list[idx] = existing.copy(
                type = pType,
                value = value,
                secure = secure,
                updatedAt = now
            )
        } else {
            list.add(Payload(
                name = cleanName,
                type = pType,
                value = value,
                secure = secure,
                createdAt = now,
                updatedAt = now
            ))
        }
        save(list)
    }

    fun delete(name: String) {
        vault.delete(name)
        val list = _payloads.value.filterNot { it.name.equals(name, ignoreCase = true) }
        save(list)
    }

    fun resolve(name: String): String? {
        val p = _payloads.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: (if (name.equals("sleep", ignoreCase = true) || name.equals("sleep_windows", ignoreCase = true)) {
                _payloads.value.firstOrNull { it.name.equals("WIN_SLEEP", ignoreCase = true) || it.name.equals("sleep_windows", ignoreCase = true) }
            } else if (name.equals("playpause", ignoreCase = true) || name.equals("play_pause", ignoreCase = true)) {
                _payloads.value.firstOrNull { it.name.equals("PLAY_PAUSE", ignoreCase = true) || it.name.equals("play_pause", ignoreCase = true) }
            } else null)
            ?: return null

        return if (p.secure) {
            vault.get(p.name) ?: p.value
        } else {
            p.value
        }
    }

    fun get(name: String): Payload? {
        return _payloads.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: (if (name.equals("sleep", ignoreCase = true) || name.equals("sleep_windows", ignoreCase = true)) {
                _payloads.value.firstOrNull { it.name.equals("WIN_SLEEP", ignoreCase = true) || it.name.equals("sleep_windows", ignoreCase = true) }
            } else if (name.equals("playpause", ignoreCase = true) || name.equals("play_pause", ignoreCase = true)) {
                _payloads.value.firstOrNull { it.name.equals("PLAY_PAUSE", ignoreCase = true) || it.name.equals("play_pause", ignoreCase = true) }
            } else null)
    }
}

/** Migration typealias */
typealias SnippetRepository = PayloadRepository
