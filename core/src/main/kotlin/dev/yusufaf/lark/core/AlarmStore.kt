package dev.yusufaf.lark.core

import kotlinx.serialization.Serializable

/** Everything Lark persists: the alarm list plus the next id to hand out. */
@Serializable
data class AlarmStore(
    val alarms: List<Alarm> = emptyList(),
    val nextId: Long = 1,
)

/**
 * The store to keep when the credential-encrypted store from before #43 turns up. [legacy] is
 * null when it couldn't be read. Only a never-used store takes it: nothing can write the store
 * before the first unlock, so one still equal to AlarmStore() hasn't been used. Any other store
 * (even one emptied later) was edited after the move, so the leftover is stale.
 */
fun AlarmStore.withLegacy(legacy: AlarmStore?): AlarmStore =
    if (legacy == null || this != AlarmStore()) this else legacy
