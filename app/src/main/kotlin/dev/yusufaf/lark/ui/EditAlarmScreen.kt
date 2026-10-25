package dev.yusufaf.lark.ui

import android.content.ActivityNotFoundException
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.Dialog
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.rememberPickerState
import dev.yusufaf.lark.core.Alarm
import dev.yusufaf.lark.core.SNOOZE_MINUTES
import dev.yusufaf.lark.core.snoozeOptionIndex
import dev.yusufaf.lark.core.timeText
import dev.yusufaf.lark.core.toggled
import dev.yusufaf.lark.core.weekdays
import dev.yusufaf.lark.schedule.AlarmScheduler
import java.time.LocalTime

/** [initial] is null while an existing alarm loads. */
@Composable
fun EditAlarmScreen(initial: Alarm?, isNew: Boolean, onSave: (Alarm) -> Unit, onDelete: () -> Unit) {
    if (initial == null) {
        ScreenScaffold {}
    } else {
        EditAlarmContent(initial, isNew, onSave, onDelete)
    }
}

@Composable
private fun EditAlarmContent(initial: Alarm, isNew: Boolean, onSave: (Alarm) -> Unit, onDelete: () -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    // New alarms open straight into the time picker; swiping it away still leaves a usable draft.
    var showTime by remember { mutableStateOf(isNew) }
    var showSnooze by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    // The screen closes after a save or delete, but a double tap lands before that.
    var done by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val labelInput = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        labelFromResult(result)?.let { draft = draft.copy(label = it) }
    }

    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = {
                    if (!done) {
                        done = true
                        onSave(draft)
                    }
                },
                modifier = Modifier.scrollable(
                    listState,
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                    overscrollEffect = rememberOverscrollEffect(),
                ),
            ) {
                Text("Save")
            }
        },
        scrollIndicator = { if (!LocalScrollCaptureInProgress.current) ScrollIndicator(listState) },
    ) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(spec),
                ) {
                    Text(if (isNew) "New alarm" else "Edit alarm")
                }
            }
            item {
                Button(
                    onClick = { showTime = true },
                    label = { Text(draft.timeText(), style = MaterialTheme.typography.titleLarge) },
                    secondaryLabel = { Text("Time") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(spec),
                )
            }
            item {
                Button(
                    onClick = {
                        try {
                            labelInput.launch(labelInputIntent())
                        } catch (e: ActivityNotFoundException) {
                            Log.w(AlarmScheduler.TAG, "no text input on this watch", e)
                        }
                    },
                    label = { Text("Label") },
                    secondaryLabel = {
                        Text(draft.label.ifBlank { "None" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(spec),
                )
            }
            // RemoteInput can't prefill or reliably submit empty text, so clearing gets its own row.
            if (draft.label.isNotBlank()) {
                item {
                    Button(
                        onClick = { draft = draft.copy(label = "") },
                        label = { Text("Clear label") },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec)
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        transformation = SurfaceTransformation(spec),
                    )
                }
            }
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) {
                    Text("Repeat")
                }
            }
            items(weekdays(locale)) { day ->
                CheckboxButton(
                    checked = day in draft.days,
                    onCheckedChange = { draft = draft.copy(days = draft.days.toggled(day)) },
                    label = { Text(day.getDisplayName(java.time.format.TextStyle.FULL, locale)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(spec),
                )
            }
            item {
                SwitchButton(
                    checked = draft.sound,
                    onCheckedChange = { draft = draft.copy(sound = it) },
                    label = { Text("Sound") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(spec),
                )
            }
            item {
                Button(
                    onClick = { showSnooze = true },
                    label = { Text("Snooze") },
                    secondaryLabel = { Text("${draft.snoozeMinutes} min") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(spec),
                )
            }
            if (!isNew) {
                item {
                    Button(
                        onClick = { showDelete = true },
                        label = { Text("Delete") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec)
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        transformation = SurfaceTransformation(spec),
                    )
                }
            }
        }

        // Dialogs rather than nav entries: swiping one away returns here with the draft intact.
        Dialog(visible = showTime, onDismissRequest = { showTime = false }) {
            TimePicker(
                initialTime = LocalTime.of(draft.hour, draft.minute),
                onTimePicked = {
                    draft = draft.copy(hour = it.hour, minute = it.minute)
                    showTime = false
                },
            )
        }
        Dialog(visible = showSnooze, onDismissRequest = { showSnooze = false }) {
            SnoozePicker(draft.snoozeMinutes) {
                draft = draft.copy(snoozeMinutes = it)
                showSnooze = false
            }
        }
        AlertDialog(
            visible = showDelete,
            onDismissRequest = { showDelete = false },
            title = { Text("Delete alarm?") },
            confirmButton = {
                AlertDialogDefaults.ConfirmButton(
                    onClick = {
                        showDelete = false
                        if (!done) {
                            done = true
                            onDelete()
                        }
                    },
                )
            },
        )
    }
}

// No ScreenScaffold: its edge-button overloads all need a scroll state.
@Composable
private fun SnoozePicker(minutes: Int, onPicked: (Int) -> Unit) {
    val state = rememberPickerState(SNOOZE_MINUTES.size, snoozeOptionIndex(minutes), shouldRepeatOptions = false)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Picker(
            state = state,
            contentDescription = { "${SNOOZE_MINUTES[state.selectedOptionIndex]} minutes" },
        ) {
            Text("${SNOOZE_MINUTES[it]} min", style = MaterialTheme.typography.displayMedium)
        }
        EdgeButton(
            onClick = { onPicked(SNOOZE_MINUTES[state.selectedOptionIndex]) },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Text("Done")
        }
    }
}
