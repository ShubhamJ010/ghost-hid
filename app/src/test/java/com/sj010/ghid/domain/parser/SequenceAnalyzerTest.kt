package com.sj010.ghid.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SequenceAnalyzerTest {

    @Test
    fun blankSchemeIsEmptyButNotAnError() {
        val analysis = SequenceAnalyzer.analyze("   ")

        assertTrue(analysis.isBlank)
        assertFalse(analysis.isInvalid)
        assertFalse(analysis.canSend)
        assertTrue(analysis.steps.isEmpty())
    }

    @Test
    fun validSchemeExposesStepsAndTotalDelay() {
        val analysis = SequenceAnalyzer.analyze("win+r ->{0.5} type:notepad ->{0.25} enter")

        assertFalse(analysis.isInvalid)
        assertTrue(analysis.canSend)
        assertEquals(5, analysis.steps.size)
        assertEquals(750L, analysis.totalDelayMillis)
    }

    @Test
    fun macAliasIsReportedAsError() {
        val analysis = SequenceAnalyzer.analyze("cmd+c")

        assertTrue(analysis.isInvalid)
        assertFalse(analysis.canSend)
        assertTrue(analysis.error.orEmpty().contains("macOS key name"))
    }

    @Test
    fun macAliasAllowedWhenTargetOsIsMac() {
        val analysis = SequenceAnalyzer.analyze("cmd+c", com.sj010.ghid.domain.model.TargetOs.MACOS)

        assertFalse(analysis.isInvalid)
        assertTrue(analysis.canSend)
    }

    @Test
    fun nonAsciiTypedTextIsReportedAsError() {
        val analysis = SequenceAnalyzer.analyze("type:héllo")

        assertTrue(analysis.isInvalid)
        assertTrue(analysis.error.orEmpty().contains("printable ASCII"))
    }
}
