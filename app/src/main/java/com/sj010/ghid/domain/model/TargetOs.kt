package com.sj010.ghid.domain.model

import java.util.Locale

/**
 * Host operating system of the Bluetooth HID target.
 *
 * Combos are tagged with one of the three real values; UNKNOWN only exists for
 * detection results before a host is fingerprinted.
 */
enum class TargetOs(val token: String, val label: String) {
    WINDOWS("windows", "Windows"),
    MACOS("macos", "macOS"),
    LINUX("linux", "Linux"),
    UNKNOWN("unknown", "Unknown");

    val isKnown: Boolean get() = this != UNKNOWN

    companion object {
        /** Resolves a persisted combo tag, defaulting legacy combos to Windows. */
        fun fromComboTag(raw: String?): TargetOs {
            val os = fromToken(raw)
            return if (os == UNKNOWN) WINDOWS else os
        }

        /** Resolves any accepted alias, including "unknown". */
        fun fromToken(raw: String?): TargetOs {
            return when (raw?.trim()?.lowercase(Locale.ROOT)) {
                "windows", "win", "windows11", "windows-11" -> WINDOWS
                "macos", "mac", "osx", "darwin", "apple" -> MACOS
                "linux", "gnu/linux", "ubuntu", "arch", "fedora" -> LINUX
                else -> UNKNOWN
            }
        }
    }
}

enum class OsConfidence(val token: String) {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low"),
    NONE("none")
}

/** One passive fingerprint result, mirrored into status.json. */
data class HostOsFingerprint(
    val os: TargetOs = TargetOs.UNKNOWN,
    val confidence: OsConfidence = OsConfidence.NONE,
    val method: String = METHOD_NONE
) {
    val isKnown: Boolean get() = os.isKnown

    companion object {
        const val METHOD_NONE = "none"
        const val METHOD_APPLE_OUI = "apple_oui"
        const val METHOD_APPLE_SDP = "apple_sdp_uuid"
        const val METHOD_LED_HANDSHAKE = "hid_led_handshake"
        const val METHOD_NAME = "name_heuristic"
        const val METHOD_FALLBACK = "fallback"

        val Unknown = HostOsFingerprint()
    }
}
