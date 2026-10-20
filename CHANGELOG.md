# Changelog

## 0.1.0 (2026-10-20)


### Features

* ring alarms from a foreground service and survive reboot ([#2](https://github.com/yusufaf/lark-alarm/issues/2)) ([25258e1](https://github.com/yusufaf/lark-alarm/commit/25258e14d3ccc90aee439994efbe4fa7b8ede3f3))
* **ring:** open the ring screen when an alarm fires ([#31](https://github.com/yusufaf/lark-alarm/issues/31)) ([961a363](https://github.com/yusufaf/lark-alarm/commit/961a363f9b6f97cfecb5c6a8613ae62a48bf976f))
* **ring:** report a ring that timed out as missed ([#26](https://github.com/yusufaf/lark-alarm/issues/26)) ([eb546a7](https://github.com/yusufaf/lark-alarm/commit/eb546a748a62655791931a83ebf07e9b175d2335)), closes [#12](https://github.com/yusufaf/lark-alarm/issues/12)
* **ring:** snooze from the notification and ring screen ([#35](https://github.com/yusufaf/lark-alarm/issues/35)) ([6da80dd](https://github.com/yusufaf/lark-alarm/commit/6da80dd58ddd3fb4086847a81424e5f39b4f0884))
* scaffold Lark, standalone Wear OS alarm clock ([#1](https://github.com/yusufaf/lark-alarm/issues/1)) ([664fcac](https://github.com/yusufaf/lark-alarm/commit/664fcac982600746c955f724fcb541fda2eba919))
* **ui:** alarm list screen with Nav3 and enable toggle ([#32](https://github.com/yusufaf/lark-alarm/issues/32)) ([91cc9af](https://github.com/yusufaf/lark-alarm/commit/91cc9af922deef33aae3dd293234c45f171b5663))
* **ui:** edit alarm screen (time, weekdays, sound, snooze, delete) ([#34](https://github.com/yusufaf/lark-alarm/issues/34)) ([3103d8c](https://github.com/yusufaf/lark-alarm/commit/3103d8c26367c249e5a56a28b44ab87c957792c5))


### Bug Fixes

* **ring:** reach Dismiss from Lark when notifications are blocked ([#30](https://github.com/yusufaf/lark-alarm/issues/30)) ([7cf6ac1](https://github.com/yusufaf/lark-alarm/commit/7cf6ac144c79cfefecb08a0f0b2c41871844ce3a)), closes [#22](https://github.com/yusufaf/lark-alarm/issues/22)
* **ring:** wake the screen when an alarm rings ([#23](https://github.com/yusufaf/lark-alarm/issues/23)) ([6727983](https://github.com/yusufaf/lark-alarm/commit/67279837ee860cad1dbe038c8ab4368b3ca4d38b)), closes [#6](https://github.com/yusufaf/lark-alarm/issues/6)
* **schedule:** close the start-of-ring gap and setAlarmClock failure ([#38](https://github.com/yusufaf/lark-alarm/issues/38)) ([27192b7](https://github.com/yusufaf/lark-alarm/commit/27192b7b27a00216f19a35717e9c6f1c12a7f4ef))
* **schedule:** ring or disable alarms that passed while nothing was armed ([#24](https://github.com/yusufaf/lark-alarm/issues/24)) ([c980d5d](https://github.com/yusufaf/lark-alarm/commit/c980d5dde9cc7475bc9b2225da10a2bc92159395))
