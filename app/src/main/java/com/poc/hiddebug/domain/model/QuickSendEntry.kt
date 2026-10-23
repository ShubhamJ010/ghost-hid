package com.poc.hiddebug.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One dispatched quick-send scheme, kept so it can be rerun or saved as a combo. */
@Serializable
data class QuickSendEntry(
    @SerialName("scheme") val scheme: String,
    @SerialName("created_at") val createdAt: String = ""
)

/**
 * Pure history rules. Kept out of the repository so it can be tested without Android.
 */
object QuickSendHistoryPolicy {

    const val MAX_ENTRIES = 40

    fun record(
        current: List<QuickSendEntry>,
        scheme: String,
        createdAt: String
    ): List<QuickSendEntry> {
        val clean = scheme.trim()
        if (clean.isEmpty()) return current

        val withoutDuplicate = current.filterNot { it.scheme.trim() == clean }
        return (listOf(QuickSendEntry(scheme = clean, createdAt = createdAt)) + withoutDuplicate)
            .take(MAX_ENTRIES)
    }
}
