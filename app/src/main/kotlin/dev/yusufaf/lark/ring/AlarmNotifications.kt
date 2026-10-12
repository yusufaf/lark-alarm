package dev.yusufaf.lark.ring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.yusufaf.lark.MainActivity
import dev.yusufaf.lark.R
import dev.yusufaf.lark.core.Alarm
import dev.yusufaf.lark.core.missedText
import dev.yusufaf.lark.core.timeText
import dev.yusufaf.lark.core.title
import dev.yusufaf.lark.schedule.AlarmScheduler

/**
 * The ringing notification is the alarm surface on Wear OS (full-screen intents are not
 * supported there): it carries the Dismiss action and opens [RingActivity] on tap.
 * The missed notification is the record left when an alarm went unanswered.
 */
object AlarmNotifications {
    const val CHANNEL_ID = "alarm"
    const val MISSED_CHANNEL_ID = "missed"
    const val RINGING_ID = 1
    private const val MISSED_TAG = "missed"

    fun ensureChannels(context: Context) {
        val ring = NotificationChannel(
            CHANNEL_ID,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Ringing alarms"
            // RingService drives the vibration itself; the channel must not add its own.
            setSound(null, null)
            enableVibration(false)
        }
        // Below the ring channel, but still alerts once so an alarm found missed after a
        // reboot gets noticed. Importance is fixed once created; the user can lower it.
        val missed = NotificationChannel(
            MISSED_CHANNEL_ID,
            "Missed alarms",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Alarms that rang out unanswered or passed while the watch was off"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(ring, missed))
    }

    /**
     * One notification per alarm, keyed by (tag, id): a repeat miss replaces the old one and
     * it cannot collide with [RINGING_ID]. No category, so it is not let through DND as an
     * alarm: it records a past event.
     */
    fun postMissed(context: Context, alarm: Alarm) {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, MISSED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Missed alarm")
            .setContentText(alarm.missedText())
            .setWhen(alarm.armedForEpochMs ?: System.currentTimeMillis())
            .setShowWhen(true)
            .setAutoCancel(true)
            // Standalone by design: never bridge to a paired phone.
            .setLocalOnly(true)
            .setContentIntent(open)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java)
        // Posting without POST_NOTIFICATIONS is dropped silently (#22); leave a trace.
        if (!manager.areNotificationsEnabled()) {
            Log.w(AlarmScheduler.TAG, "notifications blocked; missed alarm ${alarm.id} not shown")
            return
        }
        manager.notify(MISSED_TAG, alarm.id.toInt(), notification)
    }

    fun ringing(context: Context, alarm: Alarm?): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(
            context,
            0,
            RingActivity.intent(context, alarm?.id ?: -1),
            flags,
        )
        // Wear notification actions must target an Activity or Service, not a receiver.
        val dismiss = PendingIntent.getForegroundService(
            context,
            0,
            RingService.dismissIntent(context),
            flags,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(alarm?.title() ?: "Alarm")
            .setContentText(alarm?.timeText() ?: "")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            // Standalone by design: never bridge the ring to a paired phone.
            .setLocalOnly(true)
            .setContentIntent(open)
            .addAction(0, "Dismiss", dismiss)
            .build()
    }
}
