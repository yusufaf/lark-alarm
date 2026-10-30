# Lark

An open-source, standalone alarm clock for Wear OS: alarms ring and vibrate on the
watch itself, with no phone involved.

**Status: v0.1.0**, the first release you can sideload
([download](https://github.com/yusufaf/lark-alarm/releases/latest)). Tested on a Galaxy
Watch 4.

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

## What v0.1.0 does

- One-shot and weekly alarms (pick the weekdays), an optional label, and an on/off
  toggle per alarm. Add, edit and delete them on the watch.
- Always vibrates. Sound is optional per alarm and uses the watch's default alarm tone;
  it falls back to vibration only if the watch has no speaker or no tone set.
- Snooze from the ring screen or the notification, 5/10/15/30 minutes per alarm
  (default 10).
- Rings with the phone off or out of range, and in Samsung Sleep mode (tested on a
  Galaxy Watch 4). It turns the screen on and opens the ring screen (after the one-time
  grant in [Install](#install)).
- A ring stops after 5 minutes and leaves a missed-alarm notification; a one-shot alarm
  then switches off.
- Re-arms after a reboot, clock and time-zone changes, and app updates (if the watch has
  a screen lock, only after you unlock it once after a reboot; see
  [Known limits](#known-limits)). An alarm that passes while the watch is off or
  starting up rings late if Lark re-arms within 10 minutes of the alarm time; otherwise
  it's reported missed (a one-shot switches off, a weekly one moves to its next
  occurrence). Lark re-arms a few minutes after the watch starts, so turn the watch back
  on within about 5 minutes of an alarm for it to ring late.
- Standalone: no companion app, no Google Play Services, no account.

## Not in v0.1.0

- Tile ([#16](https://github.com/yusufaf/lark-alarm/issues/16)) and complication
  ([#17](https://github.com/yusufaf/lark-alarm/issues/17))
- Rotary dismiss/snooze ([#18](https://github.com/yusufaf/lark-alarm/issues/18))
- Escalating vibration ([#19](https://github.com/yusufaf/lark-alarm/issues/19))
- Skip the next occurrence ([#20](https://github.com/yusufaf/lark-alarm/issues/20))
- Seeing or cancelling a pending snooze
  ([#36](https://github.com/yusufaf/lark-alarm/issues/36))
- 12-hour time: Lark shows 24-hour time everywhere except the time picker, including
  the list, the edit screen and the notifications
  ([#33](https://github.com/yusufaf/lark-alarm/issues/33))
- Wake-up challenges, phone sync, and an ambient-mode ring screen

## Install

Each GitHub release has the APK attached. You need `adb` 30.0.0 or newer (from the
Android SDK Platform-Tools) on a computer on the same Wi-Fi as the watch.

1. Download `app-release.apk` from the
   [latest release](https://github.com/yusufaf/lark-alarm/releases/latest).
2. On the watch, enable developer options. On a Galaxy Watch: Settings > About watch >
   Software information, then tap Software version until it says developer mode is on.
   For other watches, see Android's
   [Debug Wear OS over Wi-Fi](https://developer.android.com/training/wearables/get-started/debug-wifi).
3. Connect the watch to the same Wi-Fi as the computer. On a Galaxy Watch, set Wi-Fi to
   on rather than auto, since auto keeps it off while the phone is connected. Keep the
   watch screen awake during the next steps; an idle watch can drop Wi-Fi, and adb then
   hangs.
4. Settings > Developer options: turn on ADB debugging and Wireless debugging, and allow
   the network.
5. Pair (once): Wireless debugging > Pair new device, then run this and enter the code:

   ```
   adb pair <ip>:<pairing port>
   ```

6. Connect, using the IP and port shown on the Wireless debugging screen itself (a
   different port from pairing):

   ```
   adb connect <ip>:<port>
   adb devices
   ```

   If `adb devices` lists more than one entry (a phone, or the watch twice under another
   name), add `-s <ip>:<port>` after `adb` in the next two steps.
7. Install. The `-r` also upgrades an earlier version and keeps your alarms:

   ```
   adb install -r app-release.apk
   ```

8. Let the ring screen open by itself (Wear has no setting for this). Without it, alarms
   still ring, and you dismiss them from the notification.

   ```
   adb shell appops set dev.yusufaf.lark SYSTEM_ALERT_WINDOW allow
   ```

9. Open Lark and allow notifications if it asks (the prompt appears on Wear OS 4 /
   API 33 and later). Turn Wireless debugging off when you're done; it also turns itself
   off after a reboot.

**Upgrading or switching builds.** Releases are signed with one fixed key, so a newer
release installs over an older one with `adb install -r`. An APK you build yourself is
signed with your own debug key. To switch between it and a release, run
`adb uninstall dev.yusufaf.lark` first. That deletes your alarms, the ring-screen grant
and the notification permission, so rerun steps 8 and 9 after installing.

## Known limits

- Screen: the alarm notification doesn't wake the watch on its own, so Lark turns the
  screen on itself. Samsung's wrist-down gesture can turn it off mid-ring; the alarm
  keeps vibrating. Tapping the notification opens the system's Dismiss / Snooze view,
  not Lark's ring screen. Ringing over a locked watch is untested.
- Without the step 8 grant the ring screen doesn't open by itself (Android blocks
  background activity starts, and One UI Watch has no setting to allow it). The alarm
  still works from the notification.
- Samsung Theater mode may silence alarms. Unverified (never tested); leave it off if
  you rely on Lark.
- Samsung can turn off Lark's notifications (seen once after enabling Sleep mode with
  the phone connected). The alarm still vibrates, but there's no ring notification, and
  missed-alarm notifications are hidden too. If you did step 8, the ring screen still
  opens by itself; if not, open Lark to reach Dismiss and Snooze. The alarm list then
  shows "Notifications off", which opens the setting to turn them back on.
- After a reboot, Lark re-arms only once Android starts it: on a Galaxy Watch 4 that was
  3m40s to 5 minutes after the restart. An alarm due in that window rings a few minutes
  late.
- If your watch has a screen lock, Lark can't re-arm alarms after a reboot until you
  unlock the watch once. An alarm due more than 10 minutes before that unlock is
  reported missed: a one-shot switches off, a weekly one moves to its next occurrence.
  Not tested.
- An alarm that comes due while another is ringing doesn't ring, before or after: Lark
  counts it as rung, so a one-shot switches off and a weekly one moves to its next
  occurrence ([#28](https://github.com/yusufaf/lark-alarm/issues/28)).
- Only tested on a Galaxy Watch 4 (Wear OS 6 / One UI 8 Watch). Wear OS 3 is the minimum
  but hasn't been tried.

## Building

Requires JDK 17+ and the Android SDK (platform 36).

```
./gradlew :core:test :app:assembleDebug
./gradlew :app:assembleRelease
```

The second line is the release build, minified with R8, at
`app/build/outputs/apk/release/app-release.apk`. Your build is signed with your own debug
key (see Upgrading or switching builds above).

Install on a watch or Wear OS emulator over ADB, then grant the ring screen as in
Install step 8:

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
