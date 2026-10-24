package com.sj010.ghid.ui.trackpad

import com.sj010.ghid.domain.parser.KeyTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackpadGestureTest {

    @Test
    fun registryContainsExactlyEightGestures() {
        assertEquals(8, TrackpadGestureRegistry.ALL_GESTURES.size)
    }

    @Test
    fun registryHasFourThreeFingerGestures() {
        val threeFingers = TrackpadGestureRegistry.ALL_GESTURES.filter { it.fingers == FingerCount.THREE }
        assertEquals(4, threeFingers.size)
        val directions = threeFingers.map { it.direction }.toSet()
        assertEquals(
            setOf(GestureDirection.UP, GestureDirection.DOWN, GestureDirection.LEFT, GestureDirection.RIGHT),
            directions
        )
    }

    @Test
    fun registryHasFourFourFingerGestures() {
        val fourFingers = TrackpadGestureRegistry.ALL_GESTURES.filter { it.fingers == FingerCount.FOUR }
        assertEquals(4, fourFingers.size)
        val directions = fourFingers.map { it.direction }.toSet()
        assertEquals(
            setOf(GestureDirection.UP, GestureDirection.DOWN, GestureDirection.LEFT, GestureDirection.RIGHT),
            directions
        )
    }

    @Test
    fun findResolvesEveryCombination() {
        for (fingers in FingerCount.values()) {
            for (direction in GestureDirection.values()) {
                val gesture = TrackpadGestureRegistry.find(fingers, direction)
                assertNotNull("Missing gesture for $fingers + $direction", gesture)
                assertEquals(fingers, gesture?.fingers)
                assertEquals(direction, gesture?.direction)
            }
        }
    }

    @Test
    fun allWindowsAndMacCombosAreValidKeyTokens() {
        for (gesture in TrackpadGestureRegistry.ALL_GESTURES) {
            val winChord = KeyTokens.parseChord(gesture.windowsCombo)
            assertNotNull("Invalid Windows combo: ${gesture.windowsCombo}", winChord)

            val macChord = KeyTokens.parseChord(gesture.macCombo)
            assertNotNull("Invalid Mac combo: ${gesture.macCombo}", macChord)

            assertTrue(gesture.title.isNotBlank())
            assertTrue(gesture.actionName.isNotBlank())
            assertTrue(gesture.description.isNotBlank())
            assertTrue(gesture.shortcutFor(TargetOs.WINDOWS).isNotBlank())
            assertTrue(gesture.shortcutFor(TargetOs.MACOS).isNotBlank())
        }
    }
}
