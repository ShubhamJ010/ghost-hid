package com.sj010.ghid.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Human friendly labels for quick-send history timestamps. */
object QuickSendTime {

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    fun relativeLabel(isoTimestamp: String, nowMillis: Long = System.currentTimeMillis()): String {
        val millis = parseIso(isoTimestamp) ?: return "just now"
        val elapsed = (nowMillis - millis).coerceAtLeast(0L)

        return when {
            elapsed < MINUTE -> "just now"
            elapsed < HOUR -> "${elapsed / MINUTE}m ago"
            elapsed < DAY -> "${elapsed / HOUR}h ago"
            elapsed < 7 * DAY -> "${elapsed / DAY}d ago"
            else -> SimpleDateFormat("MMM d", Locale.US).format(Date(millis))
        }
    }

    fun isoNow(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }

    private fun parseIso(value: String): Long? {
        if (value.isBlank()) return null
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(value)?.time
        } catch (_: Exception) {
            null
        }
    }
}
