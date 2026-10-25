package com.sj010.ghid

import com.sj010.ghid.domain.model.HidStep
import com.sj010.ghid.domain.parser.ActionSequenceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionSequenceParserTest {

    @Test
    fun testSleepWindowsComboParsing() {
        val keys = "\$SLEEP_WINDOWS ->{0.06} raw:00"
        val steps = ActionSequenceParser.parse(keys)

        assertEquals(3, steps.size)
        assertTrue(steps[0] is HidStep.Variable)
        assertEquals("SLEEP_WINDOWS", (steps[0] as HidStep.Variable).name)

        assertTrue(steps[1] is HidStep.Delay)
        assertEquals(60L, (steps[1] as HidStep.Delay).millis)

        assertTrue(steps[2] is HidStep.RawReport)
        val rawStep = steps[2] as HidStep.RawReport
        assertEquals(1, rawStep.bytes.size)
        assertEquals(0x00.toByte(), rawStep.bytes[0])
    }

    @Test
    fun testPlayPauseComboParsing() {
        val keys = "\$PLAY_PAUSE ->{0.06} raw:0000"
        val steps = ActionSequenceParser.parse(keys)

        assertEquals(3, steps.size)
        assertTrue(steps[0] is HidStep.Variable)
        assertEquals("PLAY_PAUSE", (steps[0] as HidStep.Variable).name)

        assertTrue(steps[1] is HidStep.Delay)
        assertEquals(60L, (steps[1] as HidStep.Delay).millis)

        assertTrue(steps[2] is HidStep.RawReport)
        val rawStep = steps[2] as HidStep.RawReport
        assertEquals(2, rawStep.bytes.size)
        assertEquals(0x00.toByte(), rawStep.bytes[0])
        assertEquals(0x00.toByte(), rawStep.bytes[1])
    }
}
