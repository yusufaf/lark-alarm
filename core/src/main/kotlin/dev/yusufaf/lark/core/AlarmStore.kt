package dev.yusufaf.lark.core

import kotlinx.serialization.Serializable

/** Everything Lark persists: the alarm list plus the next id to hand out. */
@Serializable
data class AlarmStore(
    val alarms: List<Alarm> = emptyList(),
    val nextId: Long = 1,
)
