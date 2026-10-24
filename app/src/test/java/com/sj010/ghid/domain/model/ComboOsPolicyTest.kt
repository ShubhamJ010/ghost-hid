package com.sj010.ghid.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComboOsPolicyTest {

    private fun combo(name: String, os: String, favorite: Boolean = false) =
        Combo(name = name, keys = "win+r", os = os, favorite = favorite)

    private val windows = combo("Open Run", TargetOs.WINDOWS.token, favorite = true)
    private val mac = combo("Spotlight", TargetOs.MACOS.token, favorite = true)
    private val linux = combo("Terminal", TargetOs.LINUX.token)
    private val legacy = combo("Old", "")

    @Test
    fun `unknown host does not hide anything`() {
        val all = listOf(windows, mac, linux, legacy)
        assertEquals(all, ComboOsPolicy.filter(all, TargetOs.UNKNOWN))
        assertTrue(all.all { ComboOsPolicy.matches(it, TargetOs.UNKNOWN) })
    }

    @Test
    fun `connected host filters to its tagged combos`() {
        val all = listOf(windows, mac, linux, legacy)
        assertEquals(listOf(mac), ComboOsPolicy.filter(all, TargetOs.MACOS))
        assertEquals(listOf(windows, legacy), ComboOsPolicy.filter(all, TargetOs.WINDOWS))
        assertEquals(listOf(linux), ComboOsPolicy.filter(all, TargetOs.LINUX))
        assertFalse(ComboOsPolicy.matches(mac, TargetOs.WINDOWS))
    }

    @Test
    fun `favorites are scoped to the connected host`() {
        val all = listOf(windows, mac, linux, legacy)
        assertEquals(listOf(mac), ComboOsPolicy.favorites(all, TargetOs.MACOS))
        assertEquals(listOf(windows), ComboOsPolicy.favorites(all, TargetOs.WINDOWS))
        assertEquals(2, ComboOsPolicy.favorites(all, TargetOs.UNKNOWN).size)
    }

    @Test
    fun `most used only counts combos for the connected host`() {
        val counts = mapOf(
            ComboOsPolicy.normalize("Open Run") to 9,
            ComboOsPolicy.normalize("Spotlight") to 5,
            ComboOsPolicy.normalize("Terminal") to 7
        )
        val all = listOf(windows, mac, linux)

        val macTop = ComboUsagePolicy.mostUsed(all, counts, os = TargetOs.MACOS)
        assertEquals(listOf("Spotlight"), macTop.map { it.name })

        val linuxTop = ComboUsagePolicy.mostUsed(all, counts, os = TargetOs.LINUX)
        assertEquals(listOf("Terminal"), linuxTop.map { it.name })

        val allTop = ComboUsagePolicy.mostUsed(all, counts, os = TargetOs.UNKNOWN)
        assertEquals(listOf("Open Run", "Terminal", "Spotlight"), allTop.map { it.name })
    }
}
