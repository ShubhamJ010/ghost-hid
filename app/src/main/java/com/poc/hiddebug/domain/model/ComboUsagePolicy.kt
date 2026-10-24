package com.poc.hiddebug.domain.model

import java.util.Locale

object ComboUsagePolicy {

    fun mostUsed(
        combos: List<Combo>,
        useCounts: Map<String, Int>,
        limit: Int = 4
    ): List<Combo> {
        if (limit <= 0) return emptyList()

        return combos
            .distinctBy { it.name.trim().lowercase(Locale.ROOT) }
            .filter { combo ->
                (useCounts[combo.name.trim().lowercase(Locale.ROOT)] ?: 0) > 0
            }
            .sortedByDescending { combo ->
                useCounts[combo.name.trim().lowercase(Locale.ROOT)] ?: 0
            }
            .take(limit)
    }
}
