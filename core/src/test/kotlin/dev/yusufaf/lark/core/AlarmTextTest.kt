package dev.yusufaf.lark.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
import java.util.Locale

class AlarmTextTest {
    private fun alarm(
        hour: Int = 7,
        minute: Int = 5,
        label: String = "",
        days: Set<DayOfWeek> = emptySet(),
    ) = Alarm(id = 1, hour = hour, minute = minute, days = days, label = label)

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

    @Test
    fun daysTextOneShot() {
        assertEquals("Once", alarm().daysText(Locale.UK))
    }

    @Test
    fun daysTextEveryDay() {
        assertEquals("Every day", alarm(days = DayOfWeek.entries.toSet()).daysText(Locale.UK))
    }

    @Test
    fun daysTextMondayFirstLocale() {
        assertEquals("Mon, Wed, Fri", alarm(days = setOf(FRIDAY, MONDAY, WEDNESDAY)).daysText(Locale.UK))
    }

    @Test
    fun daysTextSundayFirstLocale() {
        val monSun = alarm(days = setOf(MONDAY, SUNDAY))
        assertEquals("Sun, Mon", monSun.daysText(Locale.US))
        assertEquals("Mon, Sun", monSun.daysText(Locale.UK))
    }

    @Test
    fun listSubtitleWithoutLabel() {
        assertEquals("Once", alarm(label = "").listSubtitle(Locale.UK))
        assertEquals("Once", alarm(label = "  ").listSubtitle(Locale.UK))
    }

    @Test
    fun listSubtitleWithLabel() {
        assertEquals("Gym · Mon", alarm(label = "Gym", days = setOf(MONDAY)).listSubtitle(Locale.UK))
    }
}
