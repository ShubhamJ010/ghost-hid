package com.sj010.ghid.domain.os

import com.sj010.ghid.domain.model.HostOsFingerprint
import com.sj010.ghid.domain.model.OsConfidence
import com.sj010.ghid.domain.model.TargetOs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostOsClassifierTest {

    @Test
    fun `apple OUI is conclusive macOS before the handshake window`() {
        val result = HostOsClassifier.classify(
            address = "A0:9A:8E:57:36:DA",
            name = "DESKTOP-TZD",
            serviceUuids = null,
            ledHandshakeReceived = false
        )

        assertEquals(TargetOs.MACOS, result.os)
        assertEquals(OsConfidence.HIGH, result.confidence)
        assertEquals(HostOsFingerprint.METHOD_APPLE_OUI, result.method)
    }

    @Test
    fun `apple OUI matching is case and separator insensitive`() {
        assertTrue(HostOsClassifier.isAppleOui("a0-9a-8e-57-36-da"))
        assertTrue(HostOsClassifier.isAppleOui("3c:22:fb:00:11:22"))
        assertFalse(HostOsClassifier.isAppleOui("38:FC:98:E8:E3:87"))
        assertFalse(HostOsClassifier.isAppleOui(null))
    }

    @Test
    fun `apple SDP services are conclusive macOS`() {
        val ams = HostOsClassifier.classify(
            address = "38:FC:98:E8:E3:87",
            name = null,
            serviceUuids = listOf("00001124-0000-1000-8000-00805F9B34FB", "89D3502B-0F36-433A-8EF4-C502AD55F8DC"),
            ledHandshakeReceived = true
        )
        assertEquals(TargetOs.MACOS, ams.os)
        assertEquals(HostOsFingerprint.METHOD_APPLE_SDP, ams.method)

        val continuity = HostOsClassifier.classify(
            address = null,
            name = null,
            serviceUuids = listOf("0000FE6A-0000-1000-8000-00805F9B34FB"),
            ledHandshakeReceived = false
        )
        assertEquals(TargetOs.MACOS, continuity.os)
        assertEquals(HostOsFingerprint.METHOD_APPLE_SDP, continuity.method)
    }

    @Test
    fun `LED handshake marks Windows with high confidence`() {
        val result = HostOsClassifier.classify(
            address = "38:FC:98:E8:E3:87",
            name = "LAPTOP-TZD",
            serviceUuids = listOf("00001124-0000-1000-8000-00805F9B34FB"),
            ledHandshakeReceived = true
        )

        assertEquals(TargetOs.WINDOWS, result.os)
        assertEquals(OsConfidence.HIGH, result.confidence)
        assertEquals(HostOsFingerprint.METHOD_LED_HANDSHAKE, result.method)
    }

    @Test
    fun `hostname hints classify when no handshake arrives`() {
        val windows = HostOsClassifier.classify("38:FC:98:E8:E3:87", "DESKTOP-4F2K", null, false)
        assertEquals(TargetOs.WINDOWS, windows.os)
        assertEquals(OsConfidence.MEDIUM, windows.confidence)
        assertEquals(HostOsFingerprint.METHOD_NAME, windows.method)

        val mac = HostOsClassifier.classify("AC:DE:48:00:11:22", null, null, false)
        assertEquals(TargetOs.MACOS, mac.os)

        val renamedMac = HostOsClassifier.classify("38:FC:98:E8:E3:87", "Shubham's MacBook Air", null, false)
        assertEquals(TargetOs.MACOS, renamedMac.os)
        assertEquals(OsConfidence.MEDIUM, renamedMac.confidence)

        val linux = HostOsClassifier.classify("38:FC:98:E8:E3:87", "ubuntu-workstation", null, false)
        assertEquals(TargetOs.LINUX, linux.os)
        assertEquals(OsConfidence.HIGH, linux.confidence)
    }

    @Test
    fun `non-apple host with no signals falls back to Linux medium`() {
        val result = HostOsClassifier.classify("38:FC:98:E8:E3:87", "unknown-host", null, false)

        assertEquals(TargetOs.LINUX, result.os)
        assertEquals(OsConfidence.MEDIUM, result.confidence)
        assertEquals(HostOsFingerprint.METHOD_FALLBACK, result.method)
    }

    @Test
    fun `empty signals never crash and still fall back`() {
        val result = HostOsClassifier.classify(null, null, null, false)
        assertEquals(TargetOs.LINUX, result.os)
        assertTrue(result.isKnown)
    }

    @Test
    fun `name hints return null when nothing matches`() {
        assertNull(HostOsClassifier.fromName("random-machine"))
        assertNull(HostOsClassifier.fromName(""))
        assertNull(HostOsClassifier.fromName(null))
    }

    @Test
    fun `combo tags normalize legacy values to windows`() {
        assertEquals(TargetOs.WINDOWS, TargetOs.fromComboTag(null))
        assertEquals(TargetOs.WINDOWS, TargetOs.fromComboTag(""))
        assertEquals(TargetOs.WINDOWS, TargetOs.fromComboTag("win"))
        assertEquals(TargetOs.MACOS, TargetOs.fromComboTag("Mac"))
        assertEquals(TargetOs.LINUX, TargetOs.fromComboTag("ubuntu"))
        assertEquals(TargetOs.UNKNOWN, TargetOs.fromToken("beos"))
    }
}
