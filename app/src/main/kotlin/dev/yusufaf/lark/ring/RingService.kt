package dev.yusufaf.lark.ring

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.yusufaf.lark.LarkApplication
import dev.yusufaf.lark.core.Alarm
import dev.yusufaf.lark.schedule.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

/**
 * Keeps the watch vibrating while an alarm rings. Runs as a `systemExempted` foreground
 * service, which an exact alarm's delivery may start from the background even after boot.
 * Other receivers (BOOT_COMPLETED, MY_PACKAGE_REPLACED) may not, so go through AlarmManager.
 */
class RingService : Service() {
    private val app get() = application as LarkApplication
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { finishRing(RingEnd.TimedOut) }

    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var screenWakeLock: PowerManager.WakeLock? = null
    private var alarm: Alarm? = null

    // The system cancels app vibrations when the screen is turned off by the user, and
    // Samsung's wrist-down gesture counts as that, so a ring the screen wake-up made
    // visible would go silent a few seconds later. Only Dismiss, Snooze or the timeout may stop it.
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i(AlarmScheduler.TAG, "screen off mid-ring; restarting vibration")
            vibrator?.let(::vibrate)
        }
    }
    private var screenOffRegistered = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> startRinging(intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1))
            ACTION_DISMISS -> finishRing(RingEnd.Dismissed)
            ACTION_SNOOZE -> finishRing(RingEnd.Snoozed)
            // Restarted by the system with no intent: nothing to ring, go away quietly.
            else -> promoteAndStop()
        }
        return START_NOT_STICKY
    }

    private fun startRinging(id: Long) {
        // Promote before touching storage: startForeground() must run within seconds of
        // the receiver's startForegroundService() call, whatever the file read does.
        promote(alarm)
        val loaded = runBlocking { app.repository.get(id) }
        if (alarm != null) {
            // A second alarm fired mid-ring. The user is already being woken, so treat it
            // as rung rather than dropping it silently and leaving it un-armed.
            Log.i(AlarmScheduler.TAG, "alarm $id fired while ${alarm?.id} rings; re-arming it")
            loaded?.let { settle(it, snooze = false) }
            return
        }
        promote(loaded)
        if (loaded == null) {
            Log.w(AlarmScheduler.TAG, "alarm $id fired but is not in the store")
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        alarm = loaded
        Log.i(AlarmScheduler.TAG, "ringing alarm $id")

        val power = getSystemService(PowerManager::class.java)
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Lark:ring").also {
            it.acquire(RING_TIMEOUT_MS + 60_000)
        }
        // The alarm notification does not wake the screen on its own (verified on a Galaxy
        // Watch 4, #6), and a full-screen intent is blocked from opening anything (#29), so
        // turn it on ourselves.
        @Suppress("DEPRECATION")
        screenWakeLock = power.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Lark:screen",
        ).also { it.acquire(RING_TIMEOUT_MS) }
        vibrator = getSystemService(Vibrator::class.java).also { vibrate(it) }
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        screenOffRegistered = true
        _ringingAlarmId.value = id
        handler.postDelayed(autoStop, RING_TIMEOUT_MS)
        // After ringingAlarmId is set: RingActivity closes as soon as it sees no ring.
        openRingScreen(id)

        // Diagnostic only: a failure here must not take down a ring that is already going.
        val blocked = runCatching { AlarmNotifications.blocked(this, AlarmNotifications.CHANNEL_ID) }
            .getOrDefault(false)
        if (blocked) {
            Log.w(AlarmScheduler.TAG, "notifications blocked; alarm $id rings with no notification, dismiss from the ring screen or open Lark")
        }
    }

    // Starts from a service are background activity starts, foreground service or not, and
    // are blocked unless Lark holds SYSTEM_ALERT_WINDOW (#29, measured on a Galaxy Watch 4;
    // an activity PendingIntent fired by AlarmManager is blocked too). Without it the ring
    // notification and opening Lark stay the ways to Dismiss or Snooze.
    private fun openRingScreen(id: Long) {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(AlarmScheduler.TAG, "no SYSTEM_ALERT_WINDOW; ring screen not opened for alarm $id")
            return
        }
        try {
            startActivity(RingActivity.intent(this, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Log.e(AlarmScheduler.TAG, "failed to open ring screen for alarm $id", e)
        }
    }

    private fun vibrate(vibrator: Vibrator) {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
    }

    private fun finishRing(end: RingEnd) {
        val ringing = alarm ?: return promoteAndStop()
        Log.i(AlarmScheduler.TAG, "alarm ${ringing.id} stopped: ${end.reason}")
        handler.removeCallbacks(autoStop)
        stopRingEffects()
        alarm = null
        // Before settling: its store writes block, and if the process dies during them the
        // user should still see the miss. A later boot reclassification re-posts under the
        // same (tag, id), so it does not duplicate.
        if (end == RingEnd.TimedOut) AlarmNotifications.postMissed(this, ringing)
        settle(ringing, snooze = end == RingEnd.Snoozed)
        // Only after settling: while the id is set, rescheduleAll skips this alarm, so it can't
        // arm a late ring of it that would fire before settle's cancel (#25).
        _ringingAlarmId.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // The scheduler settles the ring under its edit lock, from a fresh read, so an edit that
    // landed mid-ring stands (AfterRing). The writes are awaited because the process may be
    // killed right after stopSelf(), and a lost one would leave the past armed-for instant
    // behind, so the next reschedule would ring it again.
    private fun settle(rung: Alarm, snooze: Boolean) = runBlocking {
        app.scheduler.settleRing(rung.id, rung.armedForEpochMs, snooze)
    }

    // Every start via startForegroundService() must be followed by startForeground(),
    // even when there is nothing to do, or the system kills the app.
    private fun promoteAndStop() {
        promote(null)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun promote(alarm: Alarm?) {
        ServiceCompat.startForeground(
            this,
            AlarmNotifications.RINGING_ID,
            AlarmNotifications.ringing(this, alarm),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED,
        )
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        stopRingEffects()
        _ringingAlarmId.value = null
        super.onDestroy()
    }

    private fun stopRingEffects() {
        if (screenOffRegistered) {
            unregisterReceiver(screenOffReceiver)
            screenOffRegistered = false
        }
        vibrator?.cancel()
        screenWakeLock?.takeIf { it.isHeld }?.release()
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    private enum class RingEnd(val reason: String) {
        Dismissed("dismissed"),
        Snoozed("snoozed"),
        TimedOut("timeout"),
    }

    companion object {
        const val ACTION_RING = "dev.yusufaf.lark.action.RING"
        const val ACTION_DISMISS = "dev.yusufaf.lark.action.DISMISS"
        const val ACTION_SNOOZE = "dev.yusufaf.lark.action.SNOOZE"
        const val RING_TIMEOUT_MS = 5 * 60_000L

        private val _ringingAlarmId = MutableStateFlow<Long?>(null)

        /**
         * Id of the alarm currently ringing, or null; [RingActivity] closes when it clears,
         * and [dev.yusufaf.lark.MainActivity] opens RingActivity while it is set.
         */
        val ringingAlarmId: StateFlow<Long?> = _ringingAlarmId

        fun ringIntent(context: Context, id: Long): Intent =
            Intent(context, RingService::class.java)
                .setAction(ACTION_RING)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)

        fun dismissIntent(context: Context): Intent =
            Intent(context, RingService::class.java).setAction(ACTION_DISMISS)

        fun snoozeIntent(context: Context): Intent =
            Intent(context, RingService::class.java).setAction(ACTION_SNOOZE)
    }
}
