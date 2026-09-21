package dev.yusufaf.lark.ring

import android.app.Service
import android.content.Context
import android.content.Intent
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
import android.util.Log
import androidx.core.app.ServiceCompat
import dev.yusufaf.lark.LarkApplication
import dev.yusufaf.lark.core.Alarm
import dev.yusufaf.lark.schedule.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Keeps the watch vibrating while an alarm rings. Runs as a `systemExempted` foreground
 * service, which exact-alarm apps may start from the background even after boot.
 */
class RingService : Service() {
    private val app get() = application as LarkApplication
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { finishRing(reason = "timeout") }

    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarm: Alarm? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> startRinging(intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1))
            ACTION_DISMISS -> finishRing(reason = "dismissed")
            // Restarted by the system with no intent: nothing to ring, go away quietly.
            else -> promoteAndStop()
        }
        return START_NOT_STICKY
    }

    private fun startRinging(id: Long) {
        if (alarm != null) return // already ringing; ignore a second alarm until this one ends
        // Blocking read of a tiny file: startForeground() must run within seconds of
        // the receiver's startForegroundService() call.
        val loaded = runBlocking { app.repository.get(id) }
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
        vibrator = getSystemService(Vibrator::class.java).also { vibrate(it) }
        _ringingAlarmId.value = id
        handler.postDelayed(autoStop, RING_TIMEOUT_MS)
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

    private fun finishRing(reason: String) {
        val ringing = alarm ?: return promoteAndStop()
        Log.i(AlarmScheduler.TAG, "alarm ${ringing.id} stopped: $reason")
        handler.removeCallbacks(autoStop)
        vibrator?.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        alarm = null
        _ringingAlarmId.value = null

        if (ringing.days.isEmpty()) {
            app.scope.launch { app.repository.setEnabled(ringing.id, false) }
            app.scheduler.cancel(ringing.id)
        } else {
            app.scheduler.schedule(ringing)
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        vibrator?.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        _ringingAlarmId.value = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_RING = "dev.yusufaf.lark.action.RING"
        const val ACTION_DISMISS = "dev.yusufaf.lark.action.DISMISS"
        const val RING_TIMEOUT_MS = 5 * 60_000L

        private val _ringingAlarmId = MutableStateFlow<Long?>(null)

        /** Id of the alarm currently ringing, or null; [RingActivity] closes when it clears. */
        val ringingAlarmId: StateFlow<Long?> = _ringingAlarmId

        fun ringIntent(context: Context, id: Long): Intent =
            Intent(context, RingService::class.java)
                .setAction(ACTION_RING)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)

        fun dismissIntent(context: Context): Intent =
            Intent(context, RingService::class.java).setAction(ACTION_DISMISS)
    }
}
