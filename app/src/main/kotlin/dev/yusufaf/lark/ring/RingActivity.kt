package dev.yusufaf.lark.ring

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import dev.yusufaf.lark.LarkApplication
import dev.yusufaf.lark.core.Alarm
import dev.yusufaf.lark.schedule.AlarmScheduler
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * Shown while an alarm rings, via the notification's content intent. Deliberately has no
 * swipe-to-dismiss: only the button (or the notification action) silences the alarm.
 */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val app = application as LarkApplication
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val alarm = runBlocking { app.repository.get(id) }

        setContent {
            MaterialTheme {
                RingScreen(alarm = alarm, onDismiss = ::dismiss)
            }
        }
    }

    private fun dismiss() {
        // The service is already in the foreground, so a plain start is allowed here.
        startService(RingService.dismissIntent(this))
    }

    companion object {
        fun intent(context: Context, id: Long): Intent =
            Intent(context, RingActivity::class.java).putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
    }
}

@Composable
private fun RingScreen(alarm: Alarm?, onDismiss: () -> Unit) {
    val ringing by RingService.ringingAlarmId.collectAsState()
    val activity = LocalActivity.current
    LaunchedEffect(ringing) {
        if (ringing == null) activity?.finish()
    }

    // Non-scrolling screen, so the EdgeButton sits in a Column like the EdgeButtonSample
    // rather than in ScreenScaffold's slot (that slot needs a scroll state).
    AppScaffold {
        ScreenScaffold {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val label = alarm?.label?.takeIf { it.isNotBlank() }
                    if (label != null) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        text = alarm?.let {
                            String.format(Locale.getDefault(), "%02d:%02d", it.hour, it.minute)
                        } ?: "Alarm",
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                EdgeButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}
