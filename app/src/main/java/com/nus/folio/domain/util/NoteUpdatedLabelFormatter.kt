package com.nus.folio.domain.util

import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Localized note updated/created timestamps for list and detail UI (AC1).
 *
 * Current year: `MMM d, HH:mm` (e.g. "Jun 12, 10:42")
 * Other years: `MMM d, yyyy` (e.g. "Jun 12, 2025")
 */
object NoteUpdatedLabelFormatter {

    fun format(
        instant: Instant = Instant.now(),
        clock: Clock = Clock.systemDefaultZone(),
        locale: Locale = Locale.getDefault(),
    ): String {
        val zone = clock.zone
        val zoned = instant.atZone(zone)
        val now = Instant.now(clock).atZone(zone)
        val pattern = if (zoned.year == now.year) {
            CURRENT_YEAR_PATTERN
        } else {
            OTHER_YEAR_PATTERN
        }
        return zoned.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    fun formatNow(
        clock: Clock = Clock.systemDefaultZone(),
        locale: Locale = Locale.getDefault(),
    ): String = format(instant = Instant.now(clock), clock = clock, locale = locale)

    private const val CURRENT_YEAR_PATTERN = "MMM d, HH:mm"
    private const val OTHER_YEAR_PATTERN = "MMM d, yyyy"
}
