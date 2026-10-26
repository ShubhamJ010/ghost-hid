package com.sj010.ghid.domain.model

import java.util.Locale

/**
 * Pure rules for the dashboard's pin-a-favorite-shape collage.
 *
 * A pin maps a shape slot id to a combo name. The same favorite can only live
 * on one shape at a time: pinning it again moves it instead of duplicating it.
 */
object PinnedFavoritesPolicy {

    fun normalize(name: String): String = name.trim().lowercase(Locale.ROOT)

    /** Pins [comboName] to [slotId], removing it from any other shape first. */
    fun pin(pins: Map<Int, String>, slotId: Int, comboName: String): Map<Int, String> {
        if (slotId < 0) return pins
        val name = comboName.trim()
        if (name.isEmpty()) return pins

        val key = normalize(name)
        return pins
            .filterValues { normalize(it) != key }
            .plus(slotId to name)
    }

    /** Removes any pin on [slotId]. */
    fun unpin(pins: Map<Int, String>, slotId: Int): Map<Int, String> {
        if (!pins.containsKey(slotId)) return pins
        return pins.filterKeys { it != slotId }
    }

    /** Shape slot currently holding [comboName], or null when it is unpinned. */
    fun slotFor(pins: Map<Int, String>, comboName: String): Int? {
        val key = normalize(comboName)
        return pins.entries.firstOrNull { normalize(it.value) == key }?.key
    }

    /** Pins that still resolve to a combo, keyed by shape slot. Deleted combos drop out. */
    fun resolve(pins: Map<Int, String>, combos: List<Combo>): Map<Int, Combo> {
        if (pins.isEmpty()) return emptyMap()
        val byName = combos.associateBy { normalize(it.name) }
        return pins.mapNotNull { (slotId, name) ->
            byName[normalize(name)]?.let { slotId to it }
        }.toMap()
    }

    /**
     * Favorites offered for pinning, de-duplicated by case-insensitive name in
     * preference order. Only starred combos qualify.
     */
    fun favorites(combos: List<Combo>): List<Combo> =
        combos
            .filter { it.favorite }
            .distinctBy { normalize(it.name) }
}
