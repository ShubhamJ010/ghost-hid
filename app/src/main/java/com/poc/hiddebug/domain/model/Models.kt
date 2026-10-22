package com.poc.hiddebug.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Combo(
    @SerialName("name") val name: String,
    @SerialName("keys") val keys: String,
    @SerialName("emoji") val emoji: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("favorite") val favorite: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class Snippet(
    @SerialName("name") val name: String,
    @SerialName("value") val value: String? = null,
    @SerialName("secure") val secure: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    fun maskedDisplay(): String {
        return if (secure) {
            if (!value.isNullOrEmpty()) "••••••••" else "(empty)"
        } else {
            value ?: "(empty)"
        }
    }
}

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
    data class Variable(val name: String) : HidStep()
    data class MouseAction(val action: String) : HidStep()
    data class Delay(val millis: Long) : HidStep()
    data class Directive(val name: String) : HidStep()
}
