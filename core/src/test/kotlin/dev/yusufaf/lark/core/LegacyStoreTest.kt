package dev.yusufaf.lark.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyStoreTest {
    private fun alarm(id: Long) = Alarm(id = id, hour = 7, minute = 0)

    @Test
    fun emptyStoreTakesLegacy() {
        val legacy = AlarmStore(listOf(alarm(1), alarm(2)), nextId = 3)
        assertEquals(legacy, AlarmStore().withLegacy(legacy))
    }

    @Test
    fun storeWithAlarmsKeepsItsOwn() {
        val current = AlarmStore(listOf(alarm(5)), nextId = 6)
        assertEquals(current, current.withLegacy(AlarmStore(listOf(alarm(1)), nextId = 2)))
    }

    @Test
    fun unreadableLegacyKeepsStore() {
        assertEquals(AlarmStore(), AlarmStore().withLegacy(null))
        val current = AlarmStore(listOf(alarm(5)), nextId = 6)
        assertEquals(current, current.withLegacy(null))
    }

    // An emptied store means the legacy file is a leftover; taking it would bring deleted alarms back.
    @Test
    fun usedEmptyStoreKeepsItsOwn() {
        val current = AlarmStore(nextId = 7)
        assertEquals(current, current.withLegacy(AlarmStore(listOf(alarm(1)), nextId = 2)))
    }
}
