package dev.yusufaf.lark.core

import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * The next instant this alarm should ring strictly after [now], or null when it never
 * will (disabled). A pending snooze wins over the schedule. Wall-clock times that fall
 * into a DST gap are pushed forward by the gap; times in a DST overlap use the earlier
 * offset (both are what [ZonedDateTime.of] does).
 */
fun Alarm.nextTrigger(now: ZonedDateTime): ZonedDateTime? {
    if (!enabled) return null

    snoozedUntilEpochMs?.let { snoozedUntil ->
        val snoozed = Instant.ofEpochMilli(snoozedUntil).atZone(now.zone)
        if (snoozed.isAfter(now)) return snoozed
    }

    val time = LocalTime.of(hour, minute)
    // Day 0 is today; day 7 covers a weekly alarm whose only day is today but has
    // already passed.
    for (offset in 0L..7L) {
        val date = now.toLocalDate().plusDays(offset)
        if (days.isNotEmpty() && date.dayOfWeek !in days) continue
        val candidate = ZonedDateTime.of(date, time, now.zone)
        if (candidate.isAfter(now)) return candidate
    }
    return null
}
