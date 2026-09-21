package dev.yusufaf.lark.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.yusufaf.lark.MainActivity
import dev.yusufaf.lark.data.AlarmRepository
import dev.yusufaf.lark.core.Alarm
import dev.yusufaf.lark.core.nextTrigger
import java.time.ZonedDateTime

/**
 * Arms one exact alarm per [Alarm] with [AlarmManager.setAlarmClock]. Repeating alarms are
 * armed one occurrence at a time; whoever handles a ring re-arms the next one.
 */
class AlarmScheduler(private val context: Context, private val repository: AlarmRepository) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(alarm: Alarm, now: ZonedDateTime = ZonedDateTime.now()) {
        val trigger = alarm.nextTrigger(now)
        if (trigger == null) {
            cancel(alarm.id)
            return
        }
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(trigger.toInstant().toEpochMilli(), showIntent()),
            fireIntent(alarm.id),
        )
        Log.i(TAG, "scheduled alarm ${alarm.id} for $trigger")
    }

    fun cancel(id: Long) {
        alarmManager.cancel(fireIntent(id))
        Log.i(TAG, "cancelled alarm $id")
    }

    /** Re-arms every enabled alarm from persisted state (boot, time change, app update). */
    suspend fun rescheduleAll() {
        val now = ZonedDateTime.now()
        repository.snapshot().forEach { schedule(it, now) }
    }

    private fun fireIntent(id: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        id.toInt(),
        Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_ALARM_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // What the system opens when the user taps the "upcoming alarm" affordance.
    private fun showIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val TAG = "Lark"
        const val ACTION_FIRE = "dev.yusufaf.lark.action.FIRE"
        const val EXTRA_ALARM_ID = "alarm_id"
    }
}
