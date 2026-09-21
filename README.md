# Lark

An open-source, standalone alarm clock for Wear OS: alarms ring and vibrate on the
watch itself, with no phone involved.

**Status: early development.** The current build is a scaffold; the first milestone
is proving that an alarm set on the watch fires and vibrates after a reboot with the
phone switched off.

## Why

Every Wear OS watch already ships a standalone alarm app. Samsung's Alarm and Google's
Clock both ring on the wrist with the phone off, repeat by weekday, snooze, and let you
pick vibration or sound. Lark does not exist because that is missing. It exists because
none of it is open: the stock apps are closed-source, the open-source Wear "alarm"
projects that do exist ([AlarmClockXtreme](https://github.com/SysAdminDoc/AlarmClockXtreme),
[WakeIQ](https://github.com/rajeshsub/WakeIQ)) ring on the phone and only mirror to the
watch, and the one true standalone one
([Smart Alarm for Wear OS](https://github.com/fridgecow/smartalarm)) is GPL-3.0 and has
been unmaintained since 2019. Lark is a small, Apache-2.0, phone-free alarm clock you
can read, build, and sideload on any Wear OS 3+ watch — and a place to put the things
the stock apps won't, without a vendor account or a companion app.

## Planned v1 scope

- One-shot and weekly repeating alarms (per-weekday toggles), optional label, enable toggle
- Vibration always; optional sound per alarm using the system alarm tone
- Snooze with a per-alarm interval (5/10/15/30 min); ringing auto-stops after 5 minutes
  and the alarm is marked missed
- Alarms survive reboot, time and time-zone changes, and app updates
- Fully standalone: no companion app, no Google Play Services

## Out of scope for v1

Tile and complication, rotary dismiss/snooze, escalating vibration, skip-next,
wake-up challenges, phone sync, and an ambient-mode ring screen.

## Target hardware

Primary test device is a Samsung Galaxy Watch 4 (Wear OS 6 / One UI 8 Watch,
450x450 round). Minimum supported is Wear OS 3 (`minSdk 30`).

## Building

Requires JDK 17+ and the Android SDK (platform 36).

```
./gradlew :core:test :app:assembleDebug
```

Install on a watch or Wear OS emulator over ADB:

```
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Tech

- Kotlin, Compose for Wear OS Material 3 (`androidx.wear.compose.material3`)
- `:core` is a pure Kotlin module holding the alarm model and next-trigger math, unit
  tested on the JVM; `:app` is the Wear OS app (`android.hardware.type.watch`,
  `com.google.android.wearable.standalone`)
- Exact alarms via `AlarmManager.setAlarmClock`, ringing from a foreground service

## License

[Apache-2.0](LICENSE)
