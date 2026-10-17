package dev.yusufaf.lark.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object AlarmList : NavKey

/** [id] null means a new alarm. */
@Serializable
data class EditAlarm(val id: Long? = null) : NavKey
