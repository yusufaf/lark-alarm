package dev.yusufaf.lark.core

import java.time.Duration
import java.time.Instant

/** What settling a finished ring does with the stored alarm. */
sealed interface AfterRing {
    /** Saved, toggled, switched off or re-armed while it rang: that change stands, snooze or not. */
    data object Keep : AfterRing

    /** Snoozed: ring again [Alarm.snoozeMinutes] from now. */
    data object Snooze : AfterRing

    /** A one-shot is spent. */
    data object Disable : AfterRing

    /** A weekly alarm moves to its next occurrence; any snooze is spent. */
    data object ArmNext : AfterRing
}

/**
 * Called on the alarm as stored when the ring ends. [armedAtRingStart] is [Alarm.armedForEpochMs]
 * as loaded when the ring started. An edit, toggle or save mid-ring changes it, and so does a
 * late ring of this same alarm that a reschedule armed before the ring was marked as ringing
 * (#25); either way the stored alarm stands.
 */
fun Alarm.afterRing(armedAtRingStart: Long?, snooze: Boolean): AfterRing = when {
    // Disabled covers a switch-off of a legacy alarm, whose armedForEpochMs was already null.
    !enabled || armedForEpochMs != armedAtRingStart -> AfterRing.Keep
    snooze -> AfterRing.Snooze
    days.isEmpty() -> AfterRing.Disable
    else -> AfterRing.ArmNext
}

/** Epoch ms [snoozeMinutes] after [now]. */
fun Alarm.snoozedUntil(now: Instant): Long = now.plus(Duration.ofMinutes(snoozeMinutes.toLong())).toEpochMilli()
