package dev.yusufaf.lark.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.yusufaf.lark.MainActivity
import dev.yusufaf.lark.data.AlarmRepository
import dev.yusufaf.lark.core.Alarm
import dev.yusufaf.lark.core.MISSED_GRACE
import dev.yusufaf.lark.core.RescheduleAction
import dev.yusufaf.lark.core.nextTrigger
import dev.yusufaf.lark.core.rescheduleAction
import dev.yusufaf.lark.ring.RingService
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Arms one exact alarm per [Alarm] with [AlarmManager.setAlarmClock]. Repeating alarms are
 * armed one occurrence at a time; whoever handles a ring re-arms the next one.
 */
class AlarmScheduler(private val context: Context, private val repository: AlarmRepository) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun schedule(alarm: Alarm, now: ZonedDateTime = ZonedDateTime.now()) {
        val trigger = alarm.nextTrigger(now)
        if (trigger == null) {
            cancel(alarm.id)
            return
        }
        val triggerMs = trigger.toInstant().toEpochMilli()
        // Persist first: if the process dies before setAlarmClock, a future armed-for
        // instant just gets re-armed, while a stale past one could be taken as missed.
        repository.setArmedFor(alarm.id, triggerMs)
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerMs, showIntent()),
            fireIntent(alarm.id),
        )
        Log.i(TAG, "scheduled alarm ${alarm.id} for $trigger")
    }

    suspend fun cancel(id: Long) {
        alarmManager.cancel(fireIntent(id))
        repository.setArmedFor(id, null)
        Log.i(TAG, "cancelled alarm $id")
    }

    /**
     * Re-arms every alarm from persisted state (boot, time change, app update). A trigger
     * that passed while nothing was armed (watch off, slow BOOT_COMPLETED, process killed
     * mid-ring) rings late within [MISSED_GRACE] and is otherwise missed, instead of
     * rolling a one-shot over to tomorrow.
     *
     * [alarmsLost] is true when AlarmManager may have dropped its alarms (boot, app update).
     * After a clock or zone change they are intact, so a trigger already in the past is
     * being delivered, or was moved by the clock jump, not missed: leave it to AlarmManager.
     */
    suspend fun rescheduleAll(alarmsLost: Boolean) {
        val now = ZonedDateTime.now()
        repository.snapshot().forEach { alarm ->
            // finishRing settles a ringing alarm. Its armedFor is still in the past, so
            // acting on it here would re-fire it or spend it mid-ring.
            if (alarm.id == RingService.ringingAlarmId.value) return@forEach
            // One alarm failing must not leave the ones after it un-armed.
            try {
                reschedule(alarm, now, alarmsLost)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "failed to reschedule alarm ${alarm.id}", e)
            }
        }
    }

    private suspend fun reschedule(alarm: Alarm, now: ZonedDateTime, alarmsLost: Boolean) {
        val action = alarm.rescheduleAction(now.toInstant())
        if (!alarmsLost && action != RescheduleAction.Arm) return
        when (action) {
            RescheduleAction.Arm -> schedule(alarm, now)
            RescheduleAction.RingNow -> ringLate(alarm, now)
            RescheduleAction.Disable -> {
                Log.i(TAG, "alarm ${alarm.id} missed (armed for ${armedFor(alarm, now)}); disabling")
                // TODO(#12): post the missed-alarm notification.
                repository.setEnabled(alarm.id, false)
                cancel(alarm.id)
            }
            RescheduleAction.SkipToNext -> {
                Log.i(TAG, "alarm ${alarm.id} missed (armed for ${armedFor(alarm, now)}); skipping to next")
                // TODO(#12): post the missed-alarm notification.
                schedule(alarm, now)
            }
        }
    }

    // Starting RingService from here is denied ("Background started FGS: Disallowed" on the
    // watch, #21): the exemption belongs to an exact alarm's own delivery, not to the
    // BOOT_COMPLETED / MY_PACKAGE_REPLACED receiver. So fire it through AlarmManager like
    // an on-time ring. armedFor stays at the original instant, which keeps MISSED_GRACE
    // bounding the lateness if the process dies before this alarm goes off; RingService's
    // finishRing then disables or re-arms the alarm as usual.
    private fun ringLate(alarm: Alarm, now: ZonedDateTime) {
        val nowMs = now.toInstant().toEpochMilli()
        val lateMs = nowMs - (alarm.armedForEpochMs ?: nowMs)
        Log.i(TAG, "alarm ${alarm.id} passed unseen ${lateMs / 1000}s ago (armed for ${armedFor(alarm, now)}); ringing late")
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(nowMs + LATE_RING_DELAY_MS, showIntent()),
            fireIntent(alarm.id),
        )
    }

    private fun armedFor(alarm: Alarm, now: ZonedDateTime) =
        alarm.armedForEpochMs?.let { Instant.ofEpochMilli(it).atZone(now.zone) }

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
        private const val LATE_RING_DELAY_MS = 1_000L
        const val ACTION_FIRE = "dev.yusufaf.lark.action.FIRE"
        const val EXTRA_ALARM_ID = "alarm_id"
    }
}
