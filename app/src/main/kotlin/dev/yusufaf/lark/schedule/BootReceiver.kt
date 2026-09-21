package dev.yusufaf.lark.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.yusufaf.lark.LarkApplication
import kotlinx.coroutines.launch

/**
 * AlarmManager forgets everything on reboot and exact alarms drift when the clock or
 * zone changes, so re-arm from persisted state. Exported only for protected system
 * broadcasts (see the manifest filter).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        Log.i(AlarmScheduler.TAG, "rescheduling after ${intent.action}")
        val app = context.applicationContext as LarkApplication
        val pending = goAsync()
        app.scope.launch {
            try {
                app.scheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
