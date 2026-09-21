package dev.yusufaf.lark.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import dev.yusufaf.lark.ring.RingService

/** Target of the exact alarm; hands off to [RingService] immediately. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        Log.i(AlarmScheduler.TAG, "alarm $id fired")
        // Exact alarms are exempt from the background foreground-service-start
        // restriction, so this is allowed even with the screen off.
        ContextCompat.startForegroundService(context, RingService.ringIntent(context, id))
    }
}
