package dev.yusufaf.lark.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant

class RescheduleActionTest {
    private val now = Instant.parse("2026-10-03T14:30:00Z")

    private fun alarm(
        armedFor: Instant?,
        days: Set<DayOfWeek> = emptySet(),
        enabled: Boolean = true,
    ) = Alarm(
        id = 1,
        hour = 14,
        minute = 25,
        days = days,
        enabled = enabled,
        armedForEpochMs = armedFor?.toEpochMilli(),
    )

    @Test
    fun futureTriggerIsArmed() {
        assertEquals(RescheduleAction.Arm, alarm(now.plusSeconds(60)).rescheduleAction(now))
    }

    @Test
    fun justPassedRingsNow() {
        // #6 (a): armed for 14:25, BootReceiver only ran at 14:25:02.
        assertEquals(
            RescheduleAction.RingNow,
            alarm(now.minusSeconds(2)).rescheduleAction(now),
        )
    }

    @Test
    fun triggerExactlyNowRingsNow() {
        assertEquals(RescheduleAction.RingNow, alarm(now).rescheduleAction(now))
    }

    @Test
    fun triggerExactlyGraceAgoStillRings() {
        assertEquals(
            RescheduleAction.RingNow,
            alarm(now.minus(MISSED_GRACE)).rescheduleAction(now),
        )
    }

    @Test
    fun oneShotLongPassedIsDisabled() {
        assertEquals(
            RescheduleAction.Disable,
            alarm(now.minus(MISSED_GRACE).minusMillis(1)).rescheduleAction(now),
        )
    }

    @Test
    fun weeklyJustPassedRingsNow() {
        assertEquals(
            RescheduleAction.RingNow,
            alarm(now.minusSeconds(60), days = setOf(DayOfWeek.SATURDAY)).rescheduleAction(now),
        )
    }

    @Test
    fun weeklyLongPassedSkipsToNext() {
        assertEquals(
            RescheduleAction.SkipToNext,
            alarm(now.minus(Duration.ofHours(8)), days = setOf(DayOfWeek.SATURDAY))
                .rescheduleAction(now),
        )
    }

    @Test
    fun legacyAlarmWithoutArmedForIsArmed() {
        assertEquals(RescheduleAction.Arm, alarm(armedFor = null).rescheduleAction(now))
    }

    @Test
    fun disabledIsArmedSoTheSchedulerCancelsIt() {
        assertEquals(
            RescheduleAction.Arm,
            alarm(now.minusSeconds(60), enabled = false).rescheduleAction(now),
        )
    }

    @Test
    fun customGraceIsHonoured() {
        assertEquals(
            RescheduleAction.Disable,
            alarm(now.minusSeconds(61)).rescheduleAction(now, grace = Duration.ofMinutes(1)),
        )
    }
}
