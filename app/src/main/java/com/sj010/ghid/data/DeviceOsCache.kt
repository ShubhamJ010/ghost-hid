package com.sj010.ghid.data

import android.content.Context
import com.sj010.ghid.domain.model.HostOsFingerprint
import com.sj010.ghid.domain.model.OsConfidence
import com.sj010.ghid.domain.model.TargetOs
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

interface DeviceOsStorage {
    fun loadAll(): Map<String, HostOsFingerprint>
    fun save(address: String, fingerprint: HostOsFingerprint)
    fun remove(address: String)
    fun clear()
}

class SharedPreferencesDeviceOsStorage(context: Context) : DeviceOsStorage {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun loadAll(): Map<String, HostOsFingerprint> {
        val result = mutableMapOf<String, HostOsFingerprint>()
        prefs.all.forEach { (rawKey, rawValue) ->
            val valueStr = rawValue as? String ?: return@forEach
            DeviceOsCache.parseEntry(valueStr)?.let { fingerprint ->
                DeviceOsCache.normalize(rawKey)?.let { key ->
                    result[key] = fingerprint
                }
            }
        }
        return result
    }

    override fun save(address: String, fingerprint: HostOsFingerprint) {
        prefs.edit().putString(address, DeviceOsCache.formatEntry(fingerprint)).apply()
    }

    override fun remove(address: String) {
        prefs.edit().remove(address).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "device_os_cache"
    }
}

/**
 * Caches detected host OS fingerprints by Bluetooth MAC address so subsequent
 * connections do not re-query or delay with handshake windows.
 */
class DeviceOsCache(private val storage: DeviceOsStorage) {

    constructor(context: Context) : this(SharedPreferencesDeviceOsStorage(context))

    private val cache = ConcurrentHashMap<String, HostOsFingerprint>()

    init {
        storage.loadAll().forEach { (address, fingerprint) ->
            normalize(address)?.let { key ->
                if (fingerprint.isKnown) {
                    cache[key] = fingerprint
                }
            }
        }
    }

    fun get(address: String?): HostOsFingerprint? {
        val key = normalize(address) ?: return null
        return cache[key]
    }

    fun put(address: String?, fingerprint: HostOsFingerprint) {
        val key = normalize(address) ?: return
        if (!fingerprint.isKnown) return
        cache[key] = fingerprint
        storage.save(key, fingerprint)
    }

    fun remove(address: String?) {
        val key = normalize(address) ?: return
        cache.remove(key)
        storage.remove(key)
    }

    fun clear() {
        cache.clear()
        storage.clear()
    }

    companion object {
        fun normalize(address: String?): String? {
            val raw = address?.trim()?.uppercase(Locale.ROOT) ?: return null
            if (raw.length < 8) return null
            return raw.replace('-', ':')
        }

        fun formatEntry(fingerprint: HostOsFingerprint): String {
            return "${fingerprint.os.token}|${fingerprint.confidence.token}|${fingerprint.method}"
        }

        fun parseEntry(value: String): HostOsFingerprint? {
            val parts = value.split("|")
            if (parts.size < 3) return null
            val os = TargetOs.fromToken(parts[0])
            if (!os.isKnown) return null
            val confidence = OsConfidence.entries.firstOrNull { it.token == parts[1] } ?: OsConfidence.HIGH
            val method = parts[2]
            return HostOsFingerprint(os = os, confidence = confidence, method = method)
        }
    }
}
