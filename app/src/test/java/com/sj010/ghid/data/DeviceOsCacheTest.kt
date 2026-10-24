package com.sj010.ghid.data

import com.sj010.ghid.domain.model.HostOsFingerprint
import com.sj010.ghid.domain.model.OsConfidence
import com.sj010.ghid.domain.model.TargetOs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DeviceOsCacheTest {

    private class InMemoryStorage : DeviceOsStorage {
        val map = mutableMapOf<String, HostOsFingerprint>()

        override fun loadAll(): Map<String, HostOsFingerprint> = map.toMap()

        override fun save(address: String, fingerprint: HostOsFingerprint) {
            map[address] = fingerprint
        }

        override fun remove(address: String) {
            map.remove(address)
        }

        override fun clear() {
            map.clear()
        }
    }

    private lateinit var storage: InMemoryStorage
    private lateinit var cache: DeviceOsCache

    @Before
    fun setUp() {
        storage = InMemoryStorage()
        cache = DeviceOsCache(storage)
    }

    @Test
    fun getReturnsNullWhenUncached() {
        assertNull(cache.get("AA:BB:CC:DD:EE:FF"))
        assertNull(cache.get(null))
        assertNull(cache.get(""))
    }

    @Test
    fun putAndGetCachesByNormalizedAddress() {
        val win = HostOsFingerprint(
            os = TargetOs.WINDOWS,
            confidence = OsConfidence.HIGH,
            method = HostOsFingerprint.METHOD_LED_HANDSHAKE
        )
        // Store with lowercase and hyphens
        cache.put("aa-bb-cc-dd-ee-ff", win)

        // Query with uppercase and colons
        val cached = cache.get("AA:BB:CC:DD:EE:FF")
        assertNotNull(cached)
        assertEquals(TargetOs.WINDOWS, cached?.os)
        assertEquals(OsConfidence.HIGH, cached?.confidence)
        assertEquals(HostOsFingerprint.METHOD_LED_HANDSHAKE, cached?.method)

        // Storage was updated with normalized key
        assertEquals(win, storage.map["AA:BB:CC:DD:EE:FF"])
    }

    @Test
    fun ignoresUnknownOs() {
        cache.put("AA:BB:CC:DD:EE:FF", HostOsFingerprint.Unknown)
        assertNull(cache.get("AA:BB:CC:DD:EE:FF"))
        assertNull(storage.map["AA:BB:CC:DD:EE:FF"])
    }

    @Test
    fun loadsInitialDataFromStorage() {
        val mac = HostOsFingerprint(
            os = TargetOs.MACOS,
            confidence = OsConfidence.HIGH,
            method = HostOsFingerprint.METHOD_APPLE_OUI
        )
        storage.save("11:22:33:44:55:66", mac)

        val freshCache = DeviceOsCache(storage)
        val loaded = freshCache.get("11:22:33:44:55:66")
        assertEquals(mac, loaded)
    }

    @Test
    fun removeAndClearEvictFromCacheAndStorage() {
        val linux = HostOsFingerprint(
            os = TargetOs.LINUX,
            confidence = OsConfidence.MEDIUM,
            method = HostOsFingerprint.METHOD_FALLBACK
        )
        cache.put("AA:11:BB:22:CC:33", linux)
        assertEquals(linux, cache.get("AA:11:BB:22:CC:33"))

        cache.remove("AA:11:BB:22:CC:33")
        assertNull(cache.get("AA:11:BB:22:CC:33"))
        assertNull(storage.map["AA:11:BB:22:CC:33"])

        cache.put("AA:11:BB:22:CC:33", linux)
        cache.clear()
        assertNull(cache.get("AA:11:BB:22:CC:33"))
        assertEquals(0, storage.map.size)
    }

    @Test
    fun serializationRoundTrip() {
        val original = HostOsFingerprint(
            os = TargetOs.WINDOWS,
            confidence = OsConfidence.HIGH,
            method = HostOsFingerprint.METHOD_LED_HANDSHAKE
        )
        val formatted = DeviceOsCache.formatEntry(original)
        assertEquals("windows|high|hid_led_handshake", formatted)

        val parsed = DeviceOsCache.parseEntry(formatted)
        assertEquals(original, parsed)
    }

    @Test
    fun parseEntryHandlesInvalidInputs() {
        assertNull(DeviceOsCache.parseEntry(""))
        assertNull(DeviceOsCache.parseEntry("invalid"))
        assertNull(DeviceOsCache.parseEntry("unknown|high|fallback"))
    }
}
