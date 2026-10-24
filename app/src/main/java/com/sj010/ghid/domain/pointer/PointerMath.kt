package com.sj010.ghid.domain.pointer

import kotlin.math.abs

/**
 * Pure conversion of touch input into HID relative-pointer deltas.
 *
 * Everything here is side-effect free so it can be unit tested without a
 * device, a Bluetooth link, or a HID stack.
 */
object PointerMath {

    /** Largest signed value a single 8-bit HID axis field can carry. */
    const val MAX_AXIS_DELTA = 127

    /**
     * Splits [delta] into chunks that each fit a signed 8-bit relative field.
     *
     * The chunks sum exactly to [delta], so a fast flick is transmitted in
     * full instead of being clipped to the first 127 counts.
     */
    fun chunks(delta: Int, limit: Int = MAX_AXIS_DELTA): List<Int> {
        require(limit in 1..MAX_AXIS_DELTA) { "limit must be between 1 and $MAX_AXIS_DELTA" }
        if (delta == 0) return emptyList()

        val result = ArrayList<Int>(abs(delta) / limit + 1)
        var remaining = delta
        while (remaining != 0) {
            val step = remaining.coerceIn(-limit, limit)
            result.add(step)
            remaining -= step
        }
        return result
    }

    /**
     * Scales a touch delta by [sensitivity] while carrying the fractional
     * remainder forward. Without this, slow drags (a fraction of a count per
     * frame) would round to zero and the cursor would never move.
     */
    fun accumulate(remainder: Float, delta: Float, sensitivity: Float): MotionStep {
        val scaled = remainder + delta * sensitivity
        val whole = scaled.toInt()
        return MotionStep(delta = whole, remainder = scaled - whole)
    }
}

/** One axis of motion: whole report counts plus the fraction kept for later. */
data class MotionStep(val delta: Int, val remainder: Float)

/**
 * Converts a vertical drag into discrete wheel notches.
 *
 * Dragging up (negative dy) scrolls up, dragging down scrolls down. The
 * residual distance is kept so a slow drag still produces notches.
 */
class WheelAccumulator(private val stepPx: Float) {

    private var residualPx = 0f

    fun reset() {
        residualPx = 0f
    }

    fun feed(dragDy: Float): Int {
        if (stepPx <= 0f) return 0

        residualPx += dragDy
        var notches = 0
        while (residualPx <= -stepPx) {
            notches += 1
            residualPx += stepPx
        }
        while (residualPx >= stepPx) {
            notches -= 1
            residualPx -= stepPx
        }
        return notches
    }
}
