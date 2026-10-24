package com.sj010.ghid.domain.model

import java.util.Locale

/**
 * OS scoping for the combo library.
 *
 * The connected host decides which combos are relevant, so the dashboard's
 * pinned and most-used sections show only combos authored for that OS. Before a
 * host is fingerprinted (UNKNOWN) nothing is hidden.
 */
object ComboOsPolicy {

    fun matches(combo: Combo, os: TargetOs): Boolean {
        if (!os.isKnown) return true
        return combo.targetOs == os
    }

    fun filter(combos: List<Combo>, os: TargetOs): List<Combo> {
        if (!os.isKnown) return combos
        return combos.filter { matches(it, os) }
    }

    fun favorites(combos: List<Combo>, os: TargetOs): List<Combo> =
        filter(PinnedFavoritesPolicy.favorites(combos), os)

    /** Case-insensitive name lookup shared by pins, usage counts and the UI. */
    fun normalize(name: String): String = name.trim().lowercase(Locale.ROOT)
}
