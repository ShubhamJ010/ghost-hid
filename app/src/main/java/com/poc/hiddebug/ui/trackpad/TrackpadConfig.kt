package com.poc.hiddebug.ui.trackpad

/**
 * Tuning for the virtual trackpad.
 *
 * These values are intentionally code-level configuration, not in-app
 * controls: the trackpad should feel the same every time it opens, and the
 * screen stays free of a settings slider. A future settings surface can
 * construct a different instance and pass it to [TrackpadScreen].
 */
data class TrackpadConfig(
    /** Pointer counts emitted per touch pixel. 1.5 feels native on a phone. */
    val pointerSensitivity: Float = 1.5f,
    /** Longest press that still counts as a tap instead of a drag. */
    val tapMaxDurationMs: Long = 320L,
    /** Finger travel that turns a scroll-rail drag into one wheel notch. */
    val scrollStepDp: Float = 30f,
    /** Wheel notches emitted by one tap of a scroll step key. */
    val scrollStepNotches: Int = 5,
    /** Touch travel (dp) that turns a tap into a drag, on top of the system slop. */
    val dragStartDp: Float = 4f,
) {
    init {
        require(pointerSensitivity > 0f) { "pointerSensitivity must be positive" }
        require(tapMaxDurationMs > 0L) { "tapMaxDurationMs must be positive" }
        require(scrollStepDp > 0f) { "scrollStepDp must be positive" }
        require(scrollStepNotches > 0) { "scrollStepNotches must be positive" }
        require(dragStartDp >= 0f) { "dragStartDp must not be negative" }
    }
}

/** The shipped defaults. Sensitivity is fixed at 1.5×. */
val DefaultTrackpadConfig = TrackpadConfig()
