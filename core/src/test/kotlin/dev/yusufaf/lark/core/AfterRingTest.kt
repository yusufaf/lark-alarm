package dev.yusufaf.lark.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class AfterRingTest {
    private val london = ZoneId.of("Europe/London")

    // Sunday.
    private val now = ZonedDateTime.of(2026, 9, 20, 10, 0, 0, 0, london)
    private val armed = now.toInstant().toEpochMilli()

    private fun alarm(
        days: Set<DayOfWeek> = emptySet(),
        enabled: Boolean = true,
        armedFor: Long? = armed,
        snoozedUntil: Long? = null,
    ) = Alarm(
        id = 1,
        hour = 10,
        minute = 0,
        days = days,
        enabled = enabled,
        snoozedUntilEpochMs = snoozedUntil,
        armedForEpochMs = armedFor,
    )

    @Test
    fun oneShotIsDisabled() {
        assertEquals(AfterRing.Disable, alarm().afterRing(armed, snooze = false))
    }

    @Test
    fun weeklyArmsNext() {
        assertEquals(AfterRing.ArmNext, alarm(days = setOf(DayOfWeek.MONDAY)).afterRing(armed, snooze = false))
    }

    @Test
    fun snoozedRingSettlesLikeAnyOther() {
        val snoozedUntil = armed - 60_000
        assertEquals(AfterRing.Disable, alarm(snoozedUntil = snoozedUntil).afterRing(armed, snooze = false))
        assertEquals(
            AfterRing.ArmNext,
            alarm(days = setOf(DayOfWeek.MONDAY), snoozedUntil = snoozedUntil).afterRing(armed, snooze = false),
        )
    }

    @Test
    fun snoozePressSnoozes() {
        assertEquals(AfterRing.Snooze, alarm().afterRing(armed, snooze = true))
        assertEquals(AfterRing.Snooze, alarm(days = setOf(DayOfWeek.MONDAY)).afterRing(armed, snooze = true))
    }

    @Test
    fun reArmedMidRingIsKept() {
        val reArmed = alarm(armedFor = now.plusDays(1).toInstant().toEpochMilli())
        assertEquals(AfterRing.Keep, reArmed.afterRing(armed, snooze = false))
        assertEquals(AfterRing.Keep, reArmed.afterRing(armed, snooze = true))
    }

    @Test
    fun switchedOffMidRingIsKept() {
        val off = alarm(enabled = false, armedFor = null)
        assertEquals(AfterRing.Keep, off.afterRing(armed, snooze = false))
        assertEquals(AfterRing.Keep, off.afterRing(armed, snooze = true))
    }

    @Test
    fun switchedOffLegacyAlarmIsKept() {
        assertEquals(AfterRing.Keep, alarm(enabled = false, armedFor = null).afterRing(null, snooze = false))
    }

    @Test
    fun legacyNullArmedForSettlesNormally() {
        assertEquals(AfterRing.Disable, alarm(armedFor = null).afterRing(null, snooze = false))
    }

    @Test
    fun snoozedUntilAddsSnoozeMinutes() {
        val alarm = alarm().copy(snoozeMinutes = 15)
        assertEquals(now.toInstant().toEpochMilli() + 15 * 60_000, alarm.snoozedUntil(now.toInstant()))
    }

    @Test
    fun snoozeIsNextTrigger() {
        val weekly = Alarm(id = 1, hour = 7, minute = 0, days = setOf(DayOfWeek.MONDAY))
        val snoozed = weekly.copy(snoozedUntilEpochMs = weekly.snoozedUntil(now.toInstant()))
        assertEquals(now.plusMinutes(10), snoozed.nextTrigger(now))
    }

    @Test
    fun rebootBeforeSnoozeRearmsIt() {
        val weekly = Alarm(id = 1, hour = 7, minute = 0, days = setOf(DayOfWeek.MONDAY))
        val s = weekly.snoozedUntil(now.toInstant())
        val snoozed = weekly.copy(snoozedUntilEpochMs = s, armedForEpochMs = s)
        val later = now.plusMinutes(3)
        assertEquals(RescheduleAction.Arm, snoozed.rescheduleAction(later.toInstant()))
        assertEquals(s, snoozed.nextTrigger(later)?.toInstant()?.toEpochMilli())
    }
}
