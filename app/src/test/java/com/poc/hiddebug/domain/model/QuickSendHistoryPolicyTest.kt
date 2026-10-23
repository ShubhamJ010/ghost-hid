package com.poc.hiddebug.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class QuickSendHistoryPolicyTest {

    @Test
    fun blankSchemesAreIgnored() {
        val current = listOf(QuickSendEntry("win+l", "a"))
        assertEquals(current, QuickSendHistoryPolicy.record(current, "   ", "b"))
    }

    @Test
    fun newestEntryIsFirst() {
        val updated = QuickSendHistoryPolicy.record(
            current = listOf(QuickSendEntry("win+l", "a")),
            scheme = "type:hello",
            createdAt = "b"
        )
        assertEquals("type:hello", updated.first().scheme)
        assertEquals(2, updated.size)
    }

    @Test
    fun duplicateSchemeMovesToTopWithoutDuplicating() {
        val updated = QuickSendHistoryPolicy.record(
            current = listOf(
                QuickSendEntry("type:hello", "old"),
                QuickSendEntry("win+l", "a")
            ),
            scheme = " type:hello ",
            createdAt = "new"
        )
        assertEquals(2, updated.size)
        assertEquals("type:hello", updated.first().scheme)
        assertEquals("new", updated.first().createdAt)
    }

    @Test
    fun historyIsCapped() {
        val full = (1..QuickSendHistoryPolicy.MAX_ENTRIES).map {
            QuickSendEntry("scheme-$it", "t$it")
        }
        val updated = QuickSendHistoryPolicy.record(full, "fresh", "now")

        assertEquals(QuickSendHistoryPolicy.MAX_ENTRIES, updated.size)
        assertEquals("fresh", updated.first().scheme)
    }
}

class QuickSendTimeTest {

    @Test
    fun recentTimestampsUseRelativeLabels() {
        val now = 1_700_000_000_000L
        assertEquals("just now", QuickSendTime.relativeLabel(iso(now - 30_000), now))
        assertEquals("5m ago", QuickSendTime.relativeLabel(iso(now - 5 * 60_000), now))
        assertEquals("2h ago", QuickSendTime.relativeLabel(iso(now - 2 * 3_600_000), now))
        assertEquals("3d ago", QuickSendTime.relativeLabel(iso(now - 3 * 86_400_000), now))
    }

    @Test
    fun blankOrBrokenTimestampsFallBackGracefully() {
        assertEquals("just now", QuickSendTime.relativeLabel("", 1_700_000_000_000L))
        assertEquals("just now", QuickSendTime.relativeLabel("not-a-date", 1_700_000_000_000L))
    }

    private fun iso(millis: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date(millis))
    }
}
