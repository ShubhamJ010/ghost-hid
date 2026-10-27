package com.sj010.ghid.domain.model

import com.sj010.ghid.ui.trackpad.TrackpadConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OsOverrideMode(val label: String, val description: String) {
    @SerialName("auto")
    AUTO("Auto-Detect", "Passively fingerprint host OS via LED handshake & Bluetooth OUI"),

    @SerialName("windows")
    WINDOWS("Force Windows", "Always map keys & gestures for Windows (Ctrl, Alt, Win shortcuts)"),

    @SerialName("macos")
    MACOS("Force macOS", "Always map keys & gestures for macOS (Cmd, Option, Mission Control)"),

    @SerialName("linux")
    LINUX("Force Linux", "Always map keys & gestures for Linux (Ctrl, Super, workspace navigation)");

    val targetOs: TargetOs?
        get() = when (this) {
            AUTO -> null
            WINDOWS -> TargetOs.WINDOWS
            MACOS -> TargetOs.MACOS
            LINUX -> TargetOs.LINUX
        }

    companion object {
        fun fromToken(token: String?): OsOverrideMode =
            entries.firstOrNull { it.name.equals(token, ignoreCase = true) } ?: AUTO
    }
}

@Serializable
enum class ThemeMode(val label: String) {
    @SerialName("system")
    SYSTEM("System Default"),

    @SerialName("dark")
    DARK("Force Dark"),

    @SerialName("light")
    LIGHT("Force Light");

    companion object {
        fun fromToken(token: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(token, ignoreCase = true) } ?: SYSTEM
    }
}

/**
 * Global configuration preferences for GhostHID.
 * Backed by persistent storage and reactive StateFlow.
 */
@Serializable
data class AppSettings(
    // 1. Trackpad & Pointer
    @SerialName("tp_sensitivity") val trackpadPointerSensitivity: Float = 1.5f,
    @SerialName("tp_scroll_step_dp") val trackpadScrollStepDp: Float = 30f,
    @SerialName("tp_scroll_notches") val trackpadScrollStepNotches: Int = 5,
    @SerialName("tp_tap_max_ms") val trackpadTapMaxDurationMs: Long = 320L,
    @SerialName("tp_drag_start_dp") val trackpadDragStartDp: Float = 4f,
    @SerialName("tp_tap_to_click") val trackpadTapToClick: Boolean = true,
    @SerialName("tp_invert_scroll") val trackpadInvertScroll: Boolean = false,
    @SerialName("tp_haptics") val trackpadHaptics: Boolean = true,

    // 2. Keyboard & HID Transmission Timing
    @SerialName("hid_typing_delay_ms") val typingDelayMs: Long = 25L,
    @SerialName("hid_key_hold_ms") val keyHoldDurationMs: Long = 40L,
    @SerialName("hid_raw_hold_ms") val rawHoldDurationMs: Long = 60L,
    @SerialName("hid_step_gap_ms") val actionStepGapMs: Long = 20L,

    // 3. Host Connection & OS Policy
    @SerialName("host_os_override") val osOverride: OsOverrideMode = OsOverrideMode.AUTO,
    @SerialName("host_auto_connect") val autoConnectOnLaunch: Boolean = true,
    @SerialName("host_led_window_ms") val ledHandshakeWindowMs: Long = 500L,

    // 4. Display & System Appearance
    @SerialName("ui_theme_mode") val themeMode: ThemeMode = ThemeMode.SYSTEM,
    @SerialName("ui_dynamic_color") val dynamicColor: Boolean = true,
    @SerialName("ui_keep_screen_awake") val keepScreenAwake: Boolean = true,
    @SerialName("ui_high_refresh_rate") val highRefreshRate: Boolean = true,

    // 5. Quick Send & Deck
    @SerialName("qs_auto_clear") val quickSendAutoClear: Boolean = false,
    @SerialName("qs_history_limit") val quickSendHistoryLimit: Int = 25,
    @SerialName("qs_haptics") val quickSendHaptics: Boolean = true
) {
    fun toTrackpadConfig(): TrackpadConfig = TrackpadConfig(
        pointerSensitivity = trackpadPointerSensitivity,
        tapMaxDurationMs = trackpadTapMaxDurationMs,
        scrollStepDp = trackpadScrollStepDp,
        scrollStepNotches = trackpadScrollStepNotches,
        dragStartDp = trackpadDragStartDp
    )
}
