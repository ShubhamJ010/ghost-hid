package com.sj010.ghid.data

import android.content.Context
import com.sj010.ghid.domain.model.Snippet
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

class SnippetRepository(
    private val context: Context,
    private val vault: AndroidKeystoreVault
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val file: File
        get() = File(context.getExternalFilesDir(null), "snippets.json")

    private val _snippets = MutableStateFlow<List<Snippet>>(emptyList())
    val snippets: StateFlow<List<Snippet>> = _snippets.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        load()
    }

    private fun isoNow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun load(): List<Snippet> {
        val f = file
        if (!f.exists() || f.length() == 0L) {
            _snippets.value = emptyList()
            return emptyList()
        }
        return try {
            val content = f.readText()
            val list = json.decodeFromString<List<Snippet>>(content)
            // Populate secure snippets from Android Keystore
            val populated = list.map { s ->
                if (s.secure) {
                    val secret = vault.get(s.name)
                    s.copy(value = secret ?: "")
                } else s
            }
            _snippets.value = populated
            populated
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(list: List<Snippet>) {
        _snippets.value = list
        scope.launch {
            try {
                // Strip secret values before writing to disk
                val toSave = list.map { s ->
                    if (s.secure) s.copy(value = "") else s
                }
                val content = json.encodeToString(toSave)
                file.writeText(content)
            } catch (ignored: Exception) {}
        }
    }

    fun ensureDefaults(): List<Snippet> {
        val current = load()
        val hasUser = current.any { it.name.equals("username", ignoreCase = true) }
        val hasPass = current.any { it.name.equals("password", ignoreCase = true) }

        if (!hasUser || !hasPass) {
            val list = current.toMutableList()
            val now = isoNow()
            if (!hasUser) {
                list.add(Snippet(name = "username", value = "", secure = false, createdAt = now, updatedAt = now))
            }
            if (!hasPass) {
                list.add(Snippet(name = "password", value = vault.get("password") ?: "", secure = true, createdAt = now, updatedAt = now))
            }
            save(list)
            return list
        }
        return current
    }

    fun set(name: String, value: String, secure: Boolean) {
        val cleanName = name.trim()
        val now = isoNow()
        if (secure) {
            vault.set(cleanName, value)
        } else {
            vault.delete(cleanName)
        }

        val list = _snippets.value.toMutableList()
        val idx = list.indexOfFirst { it.name.equals(cleanName, ignoreCase = true) }
        if (idx >= 0) {
            val existing = list[idx]
            list[idx] = existing.copy(
                value = value,
                secure = secure,
                updatedAt = now
            )
        } else {
            list.add(Snippet(
                name = cleanName,
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
        val list = _snippets.value.filterNot { it.name.equals(name, ignoreCase = true) }
        save(list)
    }

    fun resolve(name: String): String? {
        val sn = _snippets.value.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return null
        return if (sn.secure) {
            vault.get(sn.name) ?: sn.value
        } else {
            sn.value
        }
    }
}
