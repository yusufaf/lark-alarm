package dev.yusufaf.lark.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmOrderTest {
    @Test
    fun sortsByTimeOfDayThenId() {
        val alarms = listOf(
            Alarm(id = 3, hour = 7, minute = 30),
            Alarm(id = 1, hour = 22, minute = 0),
            Alarm(id = 2, hour = 7, minute = 5),
            Alarm(id = 4, hour = 7, minute = 5),
        )
        assertEquals(listOf(2L, 4L, 3L, 1L), alarms.sortedWith(AlarmListOrder).map { it.id })
    }
}
