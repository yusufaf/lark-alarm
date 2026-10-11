package dev.yusufaf.lark.core

import java.time.Duration
import java.time.Instant

/**
 * How long after its trigger an alarm that nobody saw ring still rings late rather than
 * counting as missed. Covers the reboot gap: on a Galaxy Watch 4, BOOT_COMPLETED reached
 * Lark up to about 4.5 minutes after `adb reboot` (#21).
 */
val MISSED_GRACE: Duration = Duration.ofMinutes(10)

/** What a reschedule (boot, clock change, app update) should do with one stored alarm. */
sealed interface RescheduleAction {
    /** Arm the next occurrence after now: nothing was missed, or we can't tell. */
    data object Arm : RescheduleAction

    /** The armed trigger passed unseen within the grace window: ring late, now. */
    data object RingNow : RescheduleAction

    /** A one-shot whose trigger passed unseen too long ago: disable it. */
    data object Disable : RescheduleAction

    /** A weekly alarm whose occurrence passed unseen too long ago: arm the next one. */
    data object SkipToNext : RescheduleAction
}

/**
 * Classifies this alarm against [now] using [Alarm.armedForEpochMs]. Disabled alarms and
 * alarms with no recorded trigger (stored before the field existed) take [RescheduleAction.Arm],
 * which re-arms or cancels them exactly as before. A trigger at or before [now] has passed;
 * one exactly [grace] old still rings.
 */
fun Alarm.rescheduleAction(now: Instant, grace: Duration = MISSED_GRACE): RescheduleAction {
    val armedFor = armedForEpochMs?.let(Instant::ofEpochMilli)
    if (!enabled || armedFor == null || armedFor.isAfter(now)) return RescheduleAction.Arm
    if (!armedFor.plus(grace).isBefore(now)) return RescheduleAction.RingNow
    return if (days.isEmpty()) RescheduleAction.Disable else RescheduleAction.SkipToNext
}
