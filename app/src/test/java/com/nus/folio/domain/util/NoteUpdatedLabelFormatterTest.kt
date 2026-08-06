package com.nus.folio.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class NoteUpdatedLabelFormatterTest {

    private val zone = ZoneOffset.UTC
    private val locale = Locale.US

    @Test
    fun `current year uses month day and time`() {
        val clock = Clock.fixed(Instant.parse("2026-08-05T10:42:00Z"), zone)
        val label = NoteUpdatedLabelFormatter.format(
            instant = Instant.parse("2026-06-12T10:42:00Z"),
            clock = clock,
            locale = locale,
        )
        assertEquals("Jun 12, 10:42", label)
    }

    @Test
    fun `different year uses month day and year`() {
        val clock = Clock.fixed(Instant.parse("2026-08-05T10:42:00Z"), zone)
        val label = NoteUpdatedLabelFormatter.format(
            instant = Instant.parse("2025-06-12T10:42:00Z"),
            clock = clock,
            locale = locale,
        )
        assertEquals("Jun 12, 2025", label)
    }
}
