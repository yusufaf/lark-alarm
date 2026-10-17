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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import dev.yusufaf.lark.ring.AlarmNotifications
import dev.yusufaf.lark.ring.RingActivity
import dev.yusufaf.lark.ring.RingService
import dev.yusufaf.lark.schedule.AlarmScheduler
import dev.yusufaf.lark.ui.AlarmList
import dev.yusufaf.lark.ui.AlarmListScreen
import dev.yusufaf.lark.ui.EditAlarm
import dev.yusufaf.lark.ui.EditAlarmScreen
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

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
    val backStack = rememberNavBackStack(AlarmList)
    // NavDisplay keeps entries until the back stack changes, so a plain parameter read
    // inside one would stay at its first value.
    val blocked by rememberUpdatedState(notificationsBlocked)

    AppScaffold {
        NavDisplay(
            backStack = backStack,
            sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
            entryProvider = entryProvider {
                entry<AlarmList> {
                    val alarms by app.repository.alarms.collectAsState(initial = null)
                    AlarmListScreen(
                        alarms = alarms,
                        notificationsBlocked = blocked,
                        onFixNotifications = onFixNotifications,
                        onToggle = { alarm, on ->
                            // Process scope, so leaving the list can't cancel between persist and
                            // arm. Undispatched, so rapid taps enter toggleLock in tap order.
                            app.scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                app.scheduler.setEnabled(alarm.id, on)
                            }
                        },
                        onOpen = { backStack.add(EditAlarm(it.id)) },
                        onAdd = { backStack.add(EditAlarm()) },
                    )
                }
                entry<EditAlarm> { key -> EditAlarmScreen(key.id) }
            },
        )
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
