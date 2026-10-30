# kotlinx.serialization ships its own consumer keep rules. Add rules here only
# when an R8 release build actually breaks something, and say what broke.
#
# None needed as of #14. The release APK was run on the watch: it read a store the
# debug build wrote, rang after a reboot, and add, edit, label, snooze, dismiss from
# the notification, toggle and delete all worked. The editor was also restored after
# `am kill`, which runs the one reflective lookup: Navigation 3's NavKeySerializer
# (Class.forName + serializer(KClass)), served by the kotlinx consumer rules. Not
# run: a ring with sound on (platform Ringtone calls only, no reflection). Re-check
# on a watch when adding reflection or a library that uses it.
