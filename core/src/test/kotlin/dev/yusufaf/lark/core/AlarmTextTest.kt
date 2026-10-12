package dev.yusufaf.lark.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AlarmTextTest {
    private fun alarm(hour: Int = 7, minute: Int = 5, label: String = "") =
        Alarm(id = 1, hour = hour, minute = minute, label = label)

    @Test
    fun timeTextZeroPads() {
        assertEquals("07:05", alarm().timeText(Locale.ROOT))
    }

    @Test
    fun titleFallsBackWhenLabelBlank() {
        assertEquals("Alarm", alarm(label = "").title())
        assertEquals("Alarm", alarm(label = "  ").title())
        assertEquals("Gym", alarm(label = "Gym").title())
    }

    @Test
    fun missedTextWithLabel() {
        assertEquals("Gym · 07:05", alarm(label = "Gym").missedText(Locale.ROOT))
    }

    @Test
    fun missedTextWithoutLabel() {
        assertEquals("07:05", alarm().missedText(Locale.ROOT))
    }

    @Test
    fun missedTextBlankLabel() {
        assertEquals("07:05", alarm(label = "  ").missedText(Locale.ROOT))
    }
}
