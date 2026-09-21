package dev.yusufaf.lark.ring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import dev.yusufaf.lark.R
import dev.yusufaf.lark.core.Alarm
import java.util.Locale

/**
 * The ringing notification is the alarm surface on Wear OS (full-screen intents are not
 * supported there): it carries the Dismiss action and opens [RingActivity] on tap.
 */
object AlarmNotifications {
    const val CHANNEL_ID = "alarm"
    const val RINGING_ID = 1

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Ringing alarms"
            // RingService drives the vibration itself; the channel must not add its own.
            setSound(null, null)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
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
        val time = alarm?.let { String.format(Locale.getDefault(), "%02d:%02d", it.hour, it.minute) }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(alarm?.label?.takeIf { it.isNotBlank() } ?: "Alarm")
            .setContentText(time ?: "")
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
