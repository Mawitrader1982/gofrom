package com.gofrom.app

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthTimeRangesTest {
    private val amsterdam = ZoneId.of("Europe/Amsterdam")

    @Test
    fun todayStartsAtLocalMidnight() {
        val now = Instant.parse("2026-09-03T08:15:00Z")

        val range = HealthTimeRanges.today(now, amsterdam)

        assertEquals(Instant.parse("2026-09-02T22:00:00Z"), range.start)
        assertEquals(now, range.end)
    }

    @Test
    fun lastNightEndsAtCurrentTimeBeforeNoon() {
        val now = Instant.parse("2026-09-03T04:30:00Z")

        val range = HealthTimeRanges.lastNight(now, amsterdam)

        assertEquals(Instant.parse("2026-09-02T16:00:00Z"), range.start)
        assertEquals(now, range.end)
    }

    @Test
    fun lastNightStopsAtNoonAfterNoon() {
        val now = Instant.parse("2026-09-03T18:00:00Z")

        val range = HealthTimeRanges.lastNight(now, amsterdam)

        assertEquals(Instant.parse("2026-09-02T16:00:00Z"), range.start)
        assertEquals(Instant.parse("2026-09-03T10:00:00Z"), range.end)
    }

    @Test
    fun todayUsesDstAwareLocalMidnight() {
        val now = Instant.parse("2026-10-25T13:00:00Z")

        val range = HealthTimeRanges.today(now, amsterdam)

        assertEquals(Instant.parse("2026-10-24T22:00:00Z"), range.start)
        assertEquals(now, range.end)
    }

    @Test
    fun yearOverviewContainsCurrentAndPreviousElevenCalendarMonths() {
        val now = Instant.parse("2026-09-03T08:15:00Z")

        val range = HealthTimeRanges.lastTwelveCalendarMonths(now, amsterdam)

        assertEquals("2025-10-01T00:00", range.start.toString())
        assertEquals("2026-09-03T10:15", range.end.toString())
    }
}
