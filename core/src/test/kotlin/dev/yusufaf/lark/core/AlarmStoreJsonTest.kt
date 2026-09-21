package dev.yusufaf.lark.core

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class AlarmStoreJsonTest {
    private val json = Json

    @Test
    fun roundTripsWeeklyAlarm() {
        val store = AlarmStore(
            alarms = listOf(
                Alarm(
                    id = 3,
                    hour = 6,
                    minute = 45,
                    days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                    label = "Gym",
                    enabled = false,
                    sound = true,
                    snoozeMinutes = 15,
                    snoozedUntilEpochMs = 1_700_000_000_000,
                ),
            ),
            nextId = 4,
        )

        val encoded = json.encodeToString(AlarmStore.serializer(), store)

        assertTrue(encoded, encoded.contains("\"days\":[1,5]"))
        assertEquals(store, json.decodeFromString(AlarmStore.serializer(), encoded))
    }

    @Test
    fun omittedFieldsTakeDefaults() {
        val decoded = json.decodeFromString(
            AlarmStore.serializer(),
            """{"alarms":[{"id":1,"hour":7,"minute":0}],"nextId":2}""",
        )

        assertEquals(
            AlarmStore(alarms = listOf(Alarm(id = 1, hour = 7, minute = 0)), nextId = 2),
            decoded,
        )
        val alarm = decoded.alarms.single()
        assertEquals(emptySet<DayOfWeek>(), alarm.days)
        assertEquals("", alarm.label)
        assertTrue(alarm.enabled)
        assertEquals(false, alarm.sound)
        assertEquals(10, alarm.snoozeMinutes)
        assertEquals(null, alarm.snoozedUntilEpochMs)
    }
}
