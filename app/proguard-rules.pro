# kotlinx.serialization ships its own consumer keep rules. Add rules here only
# when an R8 release build actually breaks something, and say what broke.
#
# Checked on the watch for #14 (store read, boot ring, edit, label, snooze): none
# needed. The one reflective lookup is Navigation 3's NavKeySerializer
# (Class.forName + serializer(KClass)) restoring the back stack after process
# death; the kotlinx consumer rules keep the Companion/INSTANCE serializers it uses.
