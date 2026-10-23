package com.poc.hiddebug.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Small reusable JSON-list file store. One concern: turn a file into a typed list and back.
 * Repositories own state and business rules; this owns serialization and disk IO.
 */
class JsonListStore<T>(
    private val file: File,
    serializer: KSerializer<T>
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    private val listSerializer = ListSerializer(serializer)

    fun read(): List<T> {
        if (!file.exists() || file.length() == 0L) return emptyList()
        return try {
            json.decodeFromString(listSerializer, file.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun write(items: List<T>) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(listSerializer, items))
        } catch (_: Exception) {
            // A failed disk write must never crash the HID pipeline.
        }
    }
}
