package dev.yusufaf.lark

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.yusufaf.lark.core.Alarm
import dev.yusufaf.lark.core.nextTrigger
import dev.yusufaf.lark.ring.AlarmNotifications
import dev.yusufaf.lark.ring.RingActivity
import dev.yusufaf.lark.ring.RingService
import dev.yusufaf.lark.schedule.AlarmScheduler
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private var notificationsBlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as LarkApplication
        setContent {
            MaterialTheme {
                LarkApp(app, notificationsBlocked, onFixNotifications = ::openNotificationSettings)
            }
        }
        // The ring notification is normally the way to RingActivity, but it is hidden when
        // notifications are blocked (#22), so opening Lark mid-ring must lead there too.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                RingService.ringingAlarmId.filterNotNull().collect { id ->
                    startActivity(RingActivity.intent(this@MainActivity, id))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-checked on every return, so the warning follows the permission prompt and Settings.
        notificationsBlocked = AlarmNotifications.blocked(this, AlarmNotifications.CHANNEL_ID)
    }

    private fun openNotificationSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
                )
            } catch (e: ActivityNotFoundException) {
                Log.w(AlarmScheduler.TAG, "no settings screen for Lark's notifications", e)
            }
        }
    }
}

@Composable
private fun LarkApp(app: LarkApplication, notificationsBlocked: Boolean, onFixNotifications: () -> Unit) {
    // Not mid-ring: the system prompt would race RingActivity for the foreground. It shows
    // on return after Dismiss.
    val ringing by RingService.ringingAlarmId.collectAsState()
    if (ringing == null) NotificationPermissionRequest()
    val scope = rememberCoroutineScope()
    var next by remember { mutableStateOf<ZonedDateTime?>(null) }

    // Temporary M1 test harness: replaced by the alarm list in M2. Two offsets because a
    // watch reboot takes about 90 seconds, so 2 minutes alone is too tight to test it.
    fun armIn(minutes: Long) {
        scope.launch {
            val at = ZonedDateTime.now().plusMinutes(minutes)
            val alarm = app.repository.add(Alarm(id = 0, hour = at.hour, minute = at.minute))
            app.scheduler.schedule(alarm)
            next = alarm.nextTrigger(ZonedDateTime.now())
        }
    }

    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    AppScaffold {
        // Scrolls because the warning button does not fit a fixed column on a round screen.
        ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Text(text = "Lark", style = MaterialTheme.typography.titleLarge) }
                if (notificationsBlocked) {
                    item {
                        Button(
                            onClick = onFixNotifications,
                            label = { Text("Notifications off") },
                            secondaryLabel = { Text("Open Lark to stop an alarm") },
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                            transformation = SurfaceTransformation(spec),
                        )
                    }
                }
                item {
                    Button(
                        onClick = { armIn(2) },
                        label = { Text("Alarm in 2 min") },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                    )
                }
                item {
                    Button(
                        onClick = { armIn(5) },
                        label = { Text("Alarm in 5 min") },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                    )
                }
                next?.let {
                    item {
                        Text(
                            text = "Next: " + it.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Notifications silently fail without this on Wear OS 4+, and the ring surface is one.
 * Not requested mid-ring (see [LarkApp]).
 */
@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
