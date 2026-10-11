@file:UseSerializers(DayOfWeekSerializer::class)

package dev.yusufaf.lark.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.DayOfWeek

/**
 * One alarm as the user configured it. [days] empty means one-shot; otherwise the
 * alarm repeats weekly on those days. Times are wall-clock in the device zone.
 *
 * [armedForEpochMs] is the instant AlarmManager was last armed for, or null when nothing
 * is armed. It survives reboots and process death, which AlarmManager's state does not,
 * so a reschedule can tell a trigger that passed unseen from one still to come.
 */
@Serializable
data class Alarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val days: Set<DayOfWeek> = emptySet(),
    val label: String = "",
    val enabled: Boolean = true,
    val sound: Boolean = false,
    val snoozeMinutes: Int = 10,
    val snoozedUntilEpochMs: Long? = null,
    val armedForEpochMs: Long? = null,
)
