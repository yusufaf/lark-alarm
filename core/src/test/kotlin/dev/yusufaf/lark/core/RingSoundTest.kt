package dev.yusufaf.lark.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RingSoundTest {
    @Test
    fun soundOffIsOffWhateverTheHardware() {
        for (hasSpeaker in listOf(true, false)) {
            for (hasAlarmTone in listOf(true, false)) {
                assertEquals(RingSound.Off, ringSound(false, hasSpeaker, hasAlarmTone))
            }
        }
    }

    @Test
    fun soundOnWithSpeakerAndToneIsTone() {
        assertEquals(RingSound.Tone, ringSound(true, hasSpeaker = true, hasAlarmTone = true))
    }

    @Test
    fun noSpeakerVibratesOnly() {
        assertEquals(RingSound.NoSpeaker, ringSound(true, hasSpeaker = false, hasAlarmTone = true))
    }

    @Test
    fun noSpeakerWinsOverNoTone() {
        assertEquals(RingSound.NoSpeaker, ringSound(true, hasSpeaker = false, hasAlarmTone = false))
    }

    @Test
    fun noAlarmToneVibratesOnly() {
        assertEquals(RingSound.NoTone, ringSound(true, hasSpeaker = true, hasAlarmTone = false))
    }
}
