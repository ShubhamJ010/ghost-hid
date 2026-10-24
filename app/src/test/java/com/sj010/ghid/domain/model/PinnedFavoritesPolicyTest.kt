package com.sj010.ghid.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PinnedFavoritesPolicyTest {

    @Test
    fun pinAddsComboToEmptySlot() {
        val result = PinnedFavoritesPolicy.pin(emptyMap(), slotId = 2, comboName = "Lock")

        assertEquals(mapOf(2 to "Lock"), result)
    }

    @Test
    fun pinMovesComboOffItsPreviousShapeCaseInsensitively() {
        val pins = mapOf(0 to "Lock", 1 to "Login")

        val result = PinnedFavoritesPolicy.pin(pins, slotId = 3, comboName = " lock ")

        assertEquals(mapOf(1 to "Login", 3 to "lock"), result)
    }

    @Test
    fun pinReplacesWhateverWasOnTheSlot() {
        val pins = mapOf(0 to "Lock")

        val result = PinnedFavoritesPolicy.pin(pins, slotId = 0, comboName = "Login")

        assertEquals(mapOf(0 to "Login"), result)
    }

    @Test
    fun pinIgnoresBlankNamesAndNegativeSlots() {
        val pins = mapOf(0 to "Lock")

        assertEquals(pins, PinnedFavoritesPolicy.pin(pins, slotId = 1, comboName = "   "))
        assertEquals(pins, PinnedFavoritesPolicy.pin(pins, slotId = -1, comboName = "Login"))
    }

    @Test
    fun slotForFindsShapeHoldingCombo() {
        val pins = mapOf(0 to "Lock", 4 to "Sleep")

        assertEquals(4, PinnedFavoritesPolicy.slotFor(pins, "sleep"))
        assertNull(PinnedFavoritesPolicy.slotFor(pins, "Login"))
    }

    @Test
    fun resolveDropsDeletedCombosAndMatchesNamesCaseInsensitively() {
        val pins = mapOf(0 to "lock", 1 to "Deleted", 2 to "Login")
        val combos = listOf(combo("Lock"), combo("Login"))

        val result = PinnedFavoritesPolicy.resolve(pins, combos)

        assertEquals(mapOf(0 to "Lock", 2 to "Login"), result.mapValues { it.value.name })
    }

    @Test
    fun favoritesKeepsStarredCombosOnceInPreferenceOrder() {
        val combos = listOf(
            combo("Login", favorite = true),
            combo("Lock", favorite = false),
            combo("LOGIN", favorite = true),
            combo("Sleep", favorite = true)
        )

        val result = PinnedFavoritesPolicy.favorites(combos)

        assertEquals(listOf("Login", "Sleep"), result.map { it.name })
    }

    private fun combo(name: String, favorite: Boolean = false) =
        Combo(name = name, keys = "win+l", favorite = favorite)
}
