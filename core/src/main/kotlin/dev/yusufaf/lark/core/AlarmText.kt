package dev.yusufaf.lark.core

import java.util.Locale

// Shared by the ringing and missed notifications so both describe an alarm the same way.
// They live in :core because :app has no unit-test source set.

fun Alarm.timeText(locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%02d:%02d", hour, minute)

fun Alarm.title(): String = label.takeIf { it.isNotBlank() } ?: "Alarm"

fun Alarm.missedText(locale: Locale = Locale.getDefault()): String =
    if (label.isBlank()) timeText(locale) else "$label · ${timeText(locale)}"
