package com.sj010.ghid.domain.os

import com.sj010.ghid.domain.model.HostOsFingerprint
import com.sj010.ghid.domain.model.OsConfidence
import com.sj010.ghid.domain.model.TargetOs
import java.util.Locale

/**
 * Passive host OS fingerprinting (ADR-007).
 *
 * Signals are evaluated in order of trust:
 * 1. Apple hardware OUI — conclusive macOS, even before the SDP handshake.
 * 2. Apple proprietary SDP services (Continuity, AMS, ANCS) — conclusive macOS.
 * 3. HID LED handshake — Windows pushes the initial SET_REPORT on connect.
 * 4. Hostname heuristics — a hint, never the sole source of truth.
 * 5. Fallback — Linux with medium confidence.
 *
 * Pure and side-effect free so it stays fully unit-testable.
 */
object HostOsClassifier {

    /** Window Windows gets to push its initial LED output report. */
    const val LED_HANDSHAKE_WINDOW_MS = 500L

    /**
     * Apple-registered IEEE OUIs. Apple silicon and modern Intel Macs use these
     * prefixes for their public Bluetooth Classic address.
     */
    val APPLE_OUIS: Set<String> = setOf(
        "A0:9A:8E", "AC:DE:48", "F0:18:98", "BC:D1:D3", "3C:22:FB",
        "14:7D:DA", "8C:85:90", "18:65:90", "04:0C:CE", "28:CF:E9",
        "DC:A9:04", "90:B0:ED", "78:4F:43", "6C:19:C0", "88:66:A5",
        "84:FC:FE", "5C:F9:38", "7C:D1:C3", "9C:35:EB", "0C:30:21",
        "3C:AB:8E", "40:A6:D9", "60:F8:1D", "A4:83:E7", "D0:81:7A",
        "E0:AC:CB", "F4:5C:89"
    )

    /**
     * Apple-proprietary service UUIDs that never appear on Windows or BlueZ
     * hosts: Continuity/Handoff (0xFE6A-0xFE6C), Apple Media Service and the
     * Apple Notification Center Service.
     */
    val APPLE_SERVICE_UUIDS: Set<String> = setOf(
        "FE6A", "FE6B", "FE6C",
        "89D3502B-0F36-433A-8EF4-C502AD55F8DC",
        "7905F431-B5CE-4E99-A40F-4B1E122D00D0",
        "D06162F1-8B8E-4E9C-9C1C-3A2E5F0B7C44"
    )

    // Deliberately no bare "mac": it would match hostnames such as
    // "random-machine" and turn a Linux host into macOS.
    private val MAC_NAME_HINTS = listOf(
        "macbook", "mac book", "macmini", "mac mini", "imac", "mac pro",
        "macpro", "mac studio", "macstudio", "macintosh", "mac-os", "mac_os"
    )
    private val WINDOWS_NAME_HINTS = listOf(
        "desktop-", "laptop-", "lp-", "pc-", "win-", "windows"
    )
    private val LINUX_NAME_HINTS = listOf(
        "ubuntu", "arch", "fedora", "debian", "linux", "manjaro",
        "opensuse", "gentoo", "pop-os", "pop!_os", "elementary", "endeavour"
    )

    /** True when [address] is a public MAC using an Apple-registered OUI. */
    fun isAppleOui(address: String?): Boolean {
        val normalized = normalizeAddress(address) ?: return false
        return APPLE_OUIS.any { oui -> normalized.startsWith(oui) }
    }

    /** True when the SDP record exposes an Apple-proprietary service. */
    fun hasAppleService(uuids: List<String>?): Boolean {
        if (uuids.isNullOrEmpty()) return false
        return uuids.any { raw ->
            val uuid = raw.trim().uppercase(Locale.ROOT)
            if (uuid.isEmpty()) return@any false
            val short = uuid.removePrefix("0000").substringBefore('-')
            APPLE_SERVICE_UUIDS.any { apple -> uuid == apple || uuid.contains(apple) || short == apple }
        }
    }

    /**
     * Reads the host name for OS hints. Returns null when nothing matches.
     * Distro names are treated as strong Linux hints; the rest are medium.
     */
    fun fromName(name: String?): HostOsFingerprint? {
        val normalized = name?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (normalized.isEmpty()) return null

        if (LINUX_NAME_HINTS.any { normalized.contains(it) }) {
            return HostOsFingerprint(
                os = TargetOs.LINUX,
                confidence = OsConfidence.HIGH,
                method = HostOsFingerprint.METHOD_NAME
            )
        }
        if (WINDOWS_NAME_HINTS.any { normalized.contains(it) }) {
            return HostOsFingerprint(
                os = TargetOs.WINDOWS,
                confidence = OsConfidence.MEDIUM,
                method = HostOsFingerprint.METHOD_NAME
            )
        }
        // Checked after Windows hints so a Windows host renamed "My-Mac" does
        // not beat its own naming convention, but before the Linux fallback.
        if (MAC_NAME_HINTS.any { normalized.contains(it) }) {
            return HostOsFingerprint(
                os = TargetOs.MACOS,
                confidence = OsConfidence.MEDIUM,
                method = HostOsFingerprint.METHOD_NAME
            )
        }
        return null
    }

    /** Layer 1 + 2 only: conclusive macOS signals, or null. */
    fun appleSignal(address: String?, serviceUuids: List<String>?): HostOsFingerprint? {
        if (isAppleOui(address)) {
            return HostOsFingerprint(
                os = TargetOs.MACOS,
                confidence = OsConfidence.HIGH,
                method = HostOsFingerprint.METHOD_APPLE_OUI
            )
        }
        if (hasAppleService(serviceUuids)) {
            return HostOsFingerprint(
                os = TargetOs.MACOS,
                confidence = OsConfidence.HIGH,
                method = HostOsFingerprint.METHOD_APPLE_SDP
            )
        }
        return null
    }

    /**
     * Full classification once the LED handshake window has closed (or as soon
     * as a handshake arrives).
     */
    fun classify(
        address: String?,
        name: String?,
        serviceUuids: List<String>?,
        ledHandshakeReceived: Boolean
    ): HostOsFingerprint {
        appleSignal(address, serviceUuids)?.let { return it }

        if (ledHandshakeReceived) {
            return HostOsFingerprint(
                os = TargetOs.WINDOWS,
                confidence = OsConfidence.HIGH,
                method = HostOsFingerprint.METHOD_LED_HANDSHAKE
            )
        }

        fromName(name)?.let { return it }

        // Non-Apple host with no LED sync and no telling hostname: BlueZ.
        return HostOsFingerprint(
            os = TargetOs.LINUX,
            confidence = OsConfidence.MEDIUM,
            method = HostOsFingerprint.METHOD_FALLBACK
        )
    }

    private fun normalizeAddress(address: String?): String? {
        val raw = address?.trim()?.uppercase(Locale.ROOT) ?: return null
        if (raw.length < 8) return null
        return raw.replace('-', ':')
    }
}
