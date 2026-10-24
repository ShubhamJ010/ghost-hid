package com.sj010.ghid.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ComboUsagePolicyTest {

    @Test
    fun mostUsedSortsByCountAndSkipsUnusedCombos() {
        val combos = listOf(
            combo("Lock"),
            combo("Sleep"),
            combo("Login"),
            combo("Unused")
        )

        val result = ComboUsagePolicy.mostUsed(
            combos = combos,
            useCounts = mapOf("lock" to 2, "sleep" to 5, "login" to 3)
        )

        assertEquals(listOf("Sleep", "Login", "Lock"), result.map { it.name })
    }

    @Test
    fun mostUsedRespectsLimitAndCaseInsensitiveNames() {
        val combos = listOf(combo("Login"), combo("LOGIN"), combo("Lock"), combo("Sleep"))

        val result = ComboUsagePolicy.mostUsed(
            combos = combos,
            useCounts = mapOf("login" to 4, "lock" to 3, "sleep" to 2),
            limit = 2
        )

        assertEquals(listOf("Login", "Lock"), result.map { it.name })
    }

    private fun combo(name: String) = Combo(name = name, keys = "win+l")
}
