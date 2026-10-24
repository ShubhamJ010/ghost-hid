package com.sj010.ghid.domain.pointer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PointerMathTest {

    @Test
    fun `chunks keep the exact total for fast flicks`() {
        val chunks = PointerMath.chunks(400)

        assertEquals(400, chunks.sum())
        assertTrue(chunks.all { it in -127..127 })
        assertEquals(listOf(127, 127, 127, 19), chunks)
    }

    @Test
    fun `chunks preserve negative direction`() {
        val chunks = PointerMath.chunks(-260)

        assertEquals(-260, chunks.sum())
        assertEquals(listOf(-127, -127, -6), chunks)
    }

    @Test
    fun `chunks are empty for no motion`() {
        assertTrue(PointerMath.chunks(0).isEmpty())
    }

    @Test
    fun `sensitivity carries the fractional remainder`() {
        var remainder = 0f
        var emitted = 0

        repeat(10) {
            val step = PointerMath.accumulate(remainder, delta = 1f, sensitivity = 1.5f)
            remainder = step.remainder
            emitted += step.delta
        }

        // 10 one-pixel frames at 1.5x must produce exactly 15 counts.
        assertEquals(15, emitted)
        assertEquals(0f, remainder, 0.0001f)
    }

    @Test
    fun `sensitivity never loses slow motion below one count`() {
        val step = PointerMath.accumulate(remainder = 0f, delta = 0.2f, sensitivity = 1.5f)

        assertEquals(0, step.delta)
        assertEquals(0.3f, step.remainder, 0.0001f)
    }

    @Test
    fun `wheel accumulator emits one notch per step`() {
        val accumulator = WheelAccumulator(stepPx = 30f)

        assertEquals(0, accumulator.feed(20f))
        assertEquals(-1, accumulator.feed(20f))
        assertEquals(0, accumulator.feed(10f))
        assertEquals(1, accumulator.feed(-50f))
    }

    @Test
    fun `wheel accumulator handles several notches in one event`() {
        val accumulator = WheelAccumulator(stepPx = 10f)

        assertEquals(-3, accumulator.feed(30f))
    }

    @Test
    fun `wheel accumulator reset drops pending travel`() {
        val accumulator = WheelAccumulator(stepPx = 30f)
        accumulator.feed(25f)
        accumulator.reset()

        assertEquals(0, accumulator.feed(20f))
    }

    @Test
    fun `mouse button token vocabulary stays shared with the cli`() {
        assertEquals(MouseButton.LEFT, MouseButton.fromToken("click-left"))
        assertEquals(MouseButton.LEFT, MouseButton.fromToken("lclick"))
        assertEquals(MouseButton.RIGHT, MouseButton.fromToken("right-click"))
        assertEquals(MouseButton.MIDDLE, MouseButton.fromToken("middleclick"))
        assertEquals(MouseButton.LEFT, MouseButton.fromToken("double-click"))
        assertNull(MouseButton.fromToken("scroll-up"))
    }

    @Test
    fun `button masks match the hid descriptor`() {
        assertEquals(0x01.toByte(), MouseButton.LEFT.mask)
        assertEquals(0x02.toByte(), MouseButton.RIGHT.mask)
        assertEquals(0x04.toByte(), MouseButton.MIDDLE.mask)
    }
}
