package dev.yusufaf.lark.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalTime
import java.util.Locale

class AlarmEditTest {
    private fun alarm(id: Long, days: Set<DayOfWeek> = emptySet()) =
        Alarm(id = id, hour = 7, minute = 0, days = days)

    @Test
    fun weekdaysFollowLocaleFirstDay() {
        val us = weekdays(Locale.US)
        val uk = weekdays(Locale.UK)
        assertEquals(SUNDAY, us.first())
        assertEquals(MONDAY, uk.first())
        assertEquals(DayOfWeek.entries.toSet(), us.toSet())
        assertEquals(DayOfWeek.entries.toSet(), uk.toSet())
        assertEquals(7, us.size)
        assertEquals(7, uk.size)
    }

    @Test
    fun toggledAddsMissingDay() {
        assertEquals(setOf(MONDAY, FRIDAY), setOf(MONDAY).toggled(FRIDAY))
    }

    @Test
    fun toggledRemovesPresentDay() {
        assertEquals(setOf(MONDAY), setOf(MONDAY, FRIDAY).toggled(FRIDAY))
    }

    @Test
    fun snoozeOptionIndexFindsOffered() {
        assertEquals(0, snoozeOptionIndex(5))
        assertEquals(3, snoozeOptionIndex(30))
    }

    @Test
    fun snoozeOptionIndexFallsBackToDefault() {
        assertEquals(1, snoozeOptionIndex(7))
    }

    @Test
    fun forSaveEnablesAndClearsSnooze() {
        val edited = Alarm(
            id = 3,
            hour = 6,
            minute = 15,
            days = setOf(MONDAY),
            label = "Gym",
            enabled = false,
            sound = true,
            snoozeMinutes = 15,
            snoozedUntilEpochMs = 1_000L,
            armedForEpochMs = 2_000L,
        )
        assertEquals(edited.copy(enabled = true, snoozedUntilEpochMs = null), edited.forSave())
    }

    @Test
    fun newAlarmDropsSeconds() {
        val alarm = newAlarm(LocalTime.of(6, 30, 45))
        assertEquals(6, alarm.hour)
        assertEquals(30, alarm.minute)
        assertTrue(alarm.days.isEmpty())
        assertTrue(alarm.enabled)
        assertEquals(false, alarm.sound)
        assertEquals(10, alarm.snoozeMinutes)
        assertNull(alarm.snoozedUntilEpochMs)
        assertNull(alarm.armedForEpochMs)
    }

    @Test
    fun withoutRemovesOnlyThatAlarmAndKeepsNextId() {
        val store = AlarmStore(alarms = listOf(alarm(1), alarm(2), alarm(3)), nextId = 4)
        assertEquals(AlarmStore(alarms = listOf(alarm(1), alarm(3)), nextId = 4), store.without(2))
    }

    @Test
    fun withoutUnknownIdIsNoOp() {
        val store = AlarmStore(alarms = listOf(alarm(1)), nextId = 2)
        assertEquals(store, store.without(9))
    }
}
