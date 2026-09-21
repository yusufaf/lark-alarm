@file:UseSerializers(DayOfWeekSerializer::class)

package dev.yusufaf.lark.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.DayOfWeek

/**
 * One alarm as the user configured it. [days] empty means one-shot; otherwise the
 * alarm repeats weekly on those days. Times are wall-clock in the device zone.
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
)
