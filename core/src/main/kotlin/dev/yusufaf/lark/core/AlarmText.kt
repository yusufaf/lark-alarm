package dev.yusufaf.lark.core

import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

// Shared by the ringing and missed notifications and the alarm list so all describe an alarm the same way.
// They live in :core because :app has no unit-test source set.

fun Alarm.timeText(locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%02d:%02d", hour, minute)

fun Alarm.title(): String = label.takeIf { it.isNotBlank() } ?: "Alarm"

fun Alarm.missedText(locale: Locale = Locale.getDefault()): String =
    if (label.isBlank()) timeText(locale) else "$label · ${timeText(locale)}"

/** Repeat days as short names, ordered from the locale's first day of the week. */
fun Alarm.daysText(locale: Locale = Locale.getDefault()): String = when (days.size) {
    0 -> "Once"
    7 -> "Every day"
    else -> generateSequence(WeekFields.of(locale).firstDayOfWeek) { it.plus(1) }
        .take(7)
        .filter { it in days }
        .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
}

fun Alarm.listSubtitle(locale: Locale = Locale.getDefault()): String =
    if (label.isBlank()) daysText(locale) else "$label · ${daysText(locale)}"
