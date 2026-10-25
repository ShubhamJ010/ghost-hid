package com.sj010.ghid

import com.sj010.ghid.domain.model.HidStep
import com.sj010.ghid.domain.parser.ActionSequenceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionSequenceParserTest {

    @Test
    fun testCanonicalSequenceParsing() {
        val sequence = "samxn81@gmail.com{Tab}{PASSWORD}{Enter}{500ms}{mod+s}{100ms}{WIN_SLEEP}"
        val steps = ActionSequenceParser.parse(sequence)

        assertEquals(8, steps.size)

        // 1. Text
        assertTrue(steps[0] is HidStep.TypeText)
        assertEquals("samxn81@gmail.com", (steps[0] as HidStep.TypeText).text)

        // 2. Tab
        assertTrue(steps[1] is HidStep.KeyboardChord)
        assertEquals(0x2B.toByte(), (steps[1] as HidStep.KeyboardChord).keyCode)

        // 3. Payload PASSWORD
        assertTrue(steps[2] is HidStep.PayloadReference)
        assertEquals("PASSWORD", (steps[2] as HidStep.PayloadReference).name)

        // 4. Enter
        assertTrue(steps[3] is HidStep.KeyboardChord)
        assertEquals(0x28.toByte(), (steps[3] as HidStep.KeyboardChord).keyCode)

        // 5. 500ms Delay
        assertTrue(steps[4] is HidStep.Delay)
        assertEquals(500L, (steps[4] as HidStep.Delay).millis)

        // 6. Chord mod+s
        assertTrue(steps[5] is HidStep.KeyboardChord)
        val chord = steps[5] as HidStep.KeyboardChord
        assertEquals("mod+s", chord.label)
        assertEquals(0x16.toByte(), chord.keyCode) // 's' is 0x16

        // 7. 100ms Delay
        assertTrue(steps[6] is HidStep.Delay)
        assertEquals(100L, (steps[6] as HidStep.Delay).millis)

        // 8. Payload WIN_SLEEP
        assertTrue(steps[7] is HidStep.PayloadReference)
        assertEquals("WIN_SLEEP", (steps[7] as HidStep.PayloadReference).name)
    }

    @Test
    fun testLiteralEscapedBraces() {
        val sequence = "Hello {{world}}"
        val steps = ActionSequenceParser.parse(sequence)

        assertEquals(1, steps.size)
        assertTrue(steps[0] is HidStep.TypeText)
        assertEquals("Hello {world}}", (steps[0] as HidStep.TypeText).text)
    }

    @Test
    fun testExtractPayloads() {
        val sequence = "samxn81@gmail.com{Tab}{PASSWORD}{Enter}{500ms}{mod+s}{100ms}{WIN_SLEEP}"
        val payloads = ActionSequenceParser.extractPayloads(sequence)

        assertEquals(listOf("PASSWORD", "WIN_SLEEP"), payloads)
    }
}
