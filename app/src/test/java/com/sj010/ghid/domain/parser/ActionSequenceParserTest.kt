package com.sj010.ghid.domain.parser

import com.sj010.ghid.domain.model.HidStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionSequenceParserTest {

    @Test
    fun testParseSimpleChord() {
        val steps = ActionSequenceParser.parse("win+l")
        assertEquals(1, steps.size)
        val step = steps[0] as HidStep.KeyboardChord
        assertEquals(0x08.toByte(), step.modifier)
        assertEquals(0x0F.toByte(), step.keyCode)
    }

    @Test
    fun testParseSequenceWithDelaysAndTyping() {
        val steps = ActionSequenceParser.parse("win+r ->{0.5} type:notepad ->{0.2} enter")
        assertEquals(5, steps.size)

        assertTrue(steps[0] is HidStep.KeyboardChord)
        val chord1 = steps[0] as HidStep.KeyboardChord
        assertEquals(0x08.toByte(), chord1.modifier)

        assertTrue(steps[1] is HidStep.Delay)
        assertEquals(500L, (steps[1] as HidStep.Delay).millis)

        assertTrue(steps[2] is HidStep.TypeText)
        assertEquals("notepad", (steps[2] as HidStep.TypeText).text)

        assertTrue(steps[3] is HidStep.Delay)
        assertEquals(200L, (steps[3] as HidStep.Delay).millis)

        assertTrue(steps[4] is HidStep.KeyboardChord)
        assertEquals(0x28.toByte(), (steps[4] as HidStep.KeyboardChord).keyCode)
    }

    @Test
    fun testParseVariablesAndDirectives() {
        val steps = ActionSequenceParser.parse("\$USERNAME -> tab -> \$PASSWORD -> enter -> !DISCONNECT")
        assertEquals(5, steps.size)

        assertTrue(steps[0] is HidStep.Variable)
        assertEquals("USERNAME", (steps[0] as HidStep.Variable).name)

        assertTrue(steps[2] is HidStep.Variable)
        assertEquals("PASSWORD", (steps[2] as HidStep.Variable).name)

        assertTrue(steps[4] is HidStep.Directive)
        assertEquals("DISCONNECT", (steps[4] as HidStep.Directive).name)
    }

    @Test
    fun testValidateMacAliases() {
        val result = ActionSequenceParser.validate("cmd+c")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("macOS key name") == true)
    }

    @Test
    fun testValidateValidGrammar() {
        val result = ActionSequenceParser.validate("ctrl+alt+del ->{1.0} esc ->{0.5} \$USERNAME")
        assertTrue(result.isSuccess)
    }
}
