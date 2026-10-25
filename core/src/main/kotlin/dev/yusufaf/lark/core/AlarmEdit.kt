package dev.yusufaf.lark.core

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.temporal.WeekFields
import java.util.Locale

// What the editor offers and what Save writes. In :core because :app has no unit-test source set.

val SNOOZE_MINUTES: List<Int> = listOf(5, 10, 15, 30)

/** Index of [minutes] in [SNOOZE_MINUTES]; a value not offered falls back to the [Alarm] default. */
fun snoozeOptionIndex(minutes: Int): Int =
    SNOOZE_MINUTES.indexOf(minutes).takeIf { it >= 0 } ?: SNOOZE_MINUTES.indexOf(DEFAULT_SNOOZE_MINUTES)

/** The seven days in the locale's week order. */
fun weekdays(locale: Locale = Locale.getDefault()): List<DayOfWeek> =
    generateSequence(WeekFields.of(locale).firstDayOfWeek) { it.plus(1) }.take(7).toList()

fun Set<DayOfWeek>.toggled(day: DayOfWeek): Set<DayOfWeek> = if (day in this) this - day else this + day

/** What Save persists: an edited alarm is switched on, and a pending snooze would otherwise ring before the edited time. */
fun Alarm.forSave(): Alarm = copy(enabled = true, snoozedUntilEpochMs = null)

/** Draft for a new alarm at [time] (seconds dropped); the repository assigns its id. */
fun newAlarm(time: LocalTime): Alarm = Alarm(id = 0, hour = time.hour, minute = time.minute)

/**
 * What a label from text input becomes: trimmed, with line breaks and other whitespace runs folded to one space.
 * Blank clears it. Uses [Char.isWhitespace], not regex `\s`, which on the JVM misses Unicode spaces.
 */
fun normalizeLabel(input: CharSequence): String = buildString {
    for (c in input.trim()) {
        if (!c.isWhitespace()) append(c) else if (last() != ' ') append(' ')
    }
}

/** Removes that alarm and keeps [AlarmStore.nextId], so its id is never handed out again. */
fun AlarmStore.without(id: Long): AlarmStore = copy(alarms = alarms.filterNot { it.id == id })

private val DEFAULT_SNOOZE_MINUTES = Alarm(id = 0, hour = 0, minute = 0).snoozeMinutes
