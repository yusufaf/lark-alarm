package dev.yusufaf.lark.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class NextTriggerTest {
    private val london = ZoneId.of("Europe/London")
    private val newYork = ZoneId.of("America/New_York")

    // Sunday.
    private val now = ZonedDateTime.of(2026, 9, 20, 10, 0, 0, 0, london)

    private fun alarm(
        hour: Int,
        minute: Int,
        days: Set<DayOfWeek> = emptySet(),
        enabled: Boolean = true,
        snoozedUntil: ZonedDateTime? = null,
    ) = Alarm(
        id = 1,
        hour = hour,
        minute = minute,
        days = days,
        enabled = enabled,
        snoozedUntilEpochMs = snoozedUntil?.toInstant()?.toEpochMilli(),
    )

    @Test
    fun oneShotLaterToday() {
        assertEquals(now.withHour(10).withMinute(30), alarm(10, 30).nextTrigger(now))
    }

    @Test
    fun oneShotPassedToday() {
        assertEquals(now.plusDays(1).withHour(9).withMinute(30), alarm(9, 30).nextTrigger(now))
    }

    @Test
    fun oneShotExactlyNowIsTomorrow() {
        assertEquals(now.plusDays(1), alarm(10, 0).nextTrigger(now))
    }

    @Test
    fun weeklyWrapsToNextDay() {
        assertEquals(
            ZonedDateTime.of(2026, 9, 21, 9, 0, 0, 0, london),
            alarm(9, 0, days = setOf(DayOfWeek.MONDAY)).nextTrigger(now),
        )
    }

    @Test
    fun weeklySameDayPassedGoesNextWeek() {
        assertEquals(
            ZonedDateTime.of(2026, 9, 27, 9, 0, 0, 0, london),
            alarm(9, 0, days = setOf(DayOfWeek.SUNDAY)).nextTrigger(now),
        )
    }

    @Test
    fun weeklyPicksNearestOfSeveral() {
        assertEquals(
            ZonedDateTime.of(2026, 9, 23, 7, 0, 0, 0, london),
            alarm(7, 0, days = setOf(DayOfWeek.SATURDAY, DayOfWeek.WEDNESDAY)).nextTrigger(now),
        )
    }

    @Test
    fun dstGapShiftsForward() {
        // 2026-03-08 02:30 does not exist in New York; clocks jump 02:00 -> 03:00.
        val before = ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, newYork)
        assertEquals(
            ZonedDateTime.parse("2026-03-08T03:30-04:00[America/New_York]"),
            alarm(2, 30).nextTrigger(before),
        )
    }

    @Test
    fun dstOverlapUsesEarlierOffset() {
        // 2026-11-01 01:30 happens twice in New York; take the first (EDT, -04:00).
        val before = ZonedDateTime.of(2026, 10, 31, 12, 0, 0, 0, newYork)
        assertEquals(
            ZonedDateTime.parse("2026-11-01T01:30-04:00[America/New_York]"),
            alarm(1, 30).nextTrigger(before),
        )
    }

    @Test
    fun snoozeInFutureWins() {
        val snoozed = now.plusMinutes(5)
        assertEquals(snoozed, alarm(23, 0, snoozedUntil = snoozed).nextTrigger(now))
    }

    @Test
    fun snoozeInPastIgnored() {
        assertEquals(
            now.withHour(10).withMinute(30),
            alarm(10, 30, snoozedUntil = now.minusMinutes(5)).nextTrigger(now),
        )
    }

    @Test
    fun disabledIsNull() {
        assertNull(alarm(10, 30, enabled = false).nextTrigger(now))
        assertNull(alarm(10, 30, enabled = false, snoozedUntil = now.plusMinutes(5)).nextTrigger(now))
    }
}
