package dev.yusufaf.lark

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import dev.yusufaf.lark.core.Alarm
import dev.yusufaf.lark.core.nextTrigger
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as LarkApplication
        setContent {
            MaterialTheme {
                LarkApp(app)
            }
        }
    }
}

@Composable
private fun LarkApp(app: LarkApplication) {
    NotificationPermissionRequest()
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

    AppScaffold {
        ScreenScaffold { contentPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                Text(text = "Lark", style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = { armIn(2) },
                    label = { Text("Alarm in 2 min") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { armIn(5) },
                    label = { Text("Alarm in 5 min") },
                    modifier = Modifier.fillMaxWidth(),
                )
                next?.let {
                    Text(
                        text = "Next: " + it.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/** Notifications silently fail without this on Wear OS 4+, and the ring surface is one. */
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
