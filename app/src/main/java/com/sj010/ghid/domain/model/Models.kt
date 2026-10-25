package com.sj010.ghid.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Combo(
    @SerialName("name") val name: String,
    @SerialName("sequence") val sequence: String = "",
    @SerialName("os") val os: String = TargetOs.WINDOWS.token,
    @SerialName("emoji") val emoji: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("favorite") val favorite: Boolean = false,
    @SerialName("payloads") val payloads: Map<String, Payload>? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("keys") private val legacyKeys: String? = null
) {
    /** Effective sequence resolving legacy keys if sequence was omitted. */
    val effectiveSequence: String get() = if (sequence.isNotBlank()) sequence else (legacyKeys ?: "")

    /** Deprecated alias pointing to effectiveSequence for smooth migration. */
    val keys: String get() = effectiveSequence

    /** Persisted tag resolved to the enum, defaulting legacy entries to Windows. */
    val targetOs: TargetOs get() = TargetOs.fromComboTag(os)
}

@Serializable
data class PinnedFavorite(
    @SerialName("slot") val slotId: Int,
    @SerialName("combo") val comboName: String
)

@Serializable
data class Payload(
    @SerialName("name") val name: String,
    @SerialName("type") val type: String = "type",
    @SerialName("value") val value: String? = null,
    @SerialName("secure") val secure: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    val isRaw: Boolean
        get() = type.equals("raw", ignoreCase = true)

    val effectiveType: String
        get() = if (isRaw) "raw" else "type"

    fun maskedDisplay(): String {
        return if (secure) {
            if (!value.isNullOrEmpty()) "••••••••" else "(empty)"
        } else {
            value ?: "(empty)"
        }
    }
}

/** Migration typealias */
typealias Snippet = Payload

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    UNKNOWN;

    companion object {
        fun fromInt(state: Int): ConnectionStatus = when (state) {
            0 -> DISCONNECTED
            1 -> CONNECTING
            2 -> CONNECTED
            3 -> DISCONNECTING
            else -> UNKNOWN
        }
    }
}

data class LogEntry(
    val timestamp: String,
    val message: String
)

sealed class HidStep {
    data class KeyboardChord(val modifier: Byte, val keyCode: Byte, val label: String) : HidStep()
    data class TypeText(val text: String) : HidStep()
    data class PayloadReference(val name: String) : HidStep()
    data class MouseAction(val action: String) : HidStep()
    data class Delay(val millis: Long) : HidStep()
    data class RawReport(val bytes: ByteArray, val hex: String) : HidStep() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as RawReport
            return bytes.contentEquals(other.bytes) && hex == other.hex
        }
        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + hex.hashCode()
            return result
        }
    }

    /** Compatibility constructor/extractor for Variable -> PayloadReference */
    companion object {
        fun Variable(name: String): PayloadReference = PayloadReference(name)
    }
}
