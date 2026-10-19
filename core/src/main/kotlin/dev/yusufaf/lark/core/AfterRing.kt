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
 * Called on the alarm as stored when the ring ends. [firedFor] is the armed-for instant the
 * ring's fire carried (see the [firedFor] function). A save, toggle or delete mid-ring changes
 * the stored one, and so, rarely, does a reschedule that acts on the alarm before RingService
 * marks the ring (#25); either way the stored alarm stands.
 */
fun Alarm.afterRing(firedFor: Long?, snooze: Boolean): AfterRing = when {
    // Disabled covers a switch-off of a legacy alarm, whose armedForEpochMs was already null.
    !enabled || armedForEpochMs != firedFor -> AfterRing.Keep
    snooze -> AfterRing.Snooze
    days.isEmpty() -> AfterRing.Disable
    else -> AfterRing.ArmNext
}

/** Epoch ms [snoozeMinutes] after [now]. */
fun Alarm.snoozedUntil(now: Instant): Long = now.plus(Duration.ofMinutes(snoozeMinutes.toLong())).toEpochMilli()
