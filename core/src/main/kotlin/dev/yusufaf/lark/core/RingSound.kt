package dev.yusufaf.lark.core

/** What a ring plays besides vibration, given the alarm's choice and what the watch can do. */
sealed interface RingSound {
    /** Sound is off for this alarm. */
    data object Off : RingSound

    /** Play the default alarm tone. */
    data object Tone : RingSound

    /** Sound is on, but the watch has no built-in speaker: vibrate only. */
    data object NoSpeaker : RingSound

    /** Sound is on, but no default alarm tone is set: vibrate only. */
    data object NoTone : RingSound
}

fun ringSound(sound: Boolean, hasSpeaker: Boolean, hasAlarmTone: Boolean): RingSound = when {
    !sound -> RingSound.Off
    !hasSpeaker -> RingSound.NoSpeaker
    !hasAlarmTone -> RingSound.NoTone
    else -> RingSound.Tone
}
