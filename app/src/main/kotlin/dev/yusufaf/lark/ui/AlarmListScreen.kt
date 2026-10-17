package dev.yusufaf.lark.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.SplitSwitchButton
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextDefaults
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.yusufaf.lark.core.Alarm
import dev.yusufaf.lark.core.AlarmListOrder
import dev.yusufaf.lark.core.daysText
import dev.yusufaf.lark.core.listSubtitle
import dev.yusufaf.lark.core.timeText
import dev.yusufaf.lark.core.title

/** [alarms] is null until the store first loads, so a cold start doesn't flash the empty state. */
@Composable
fun AlarmListScreen(
    alarms: List<Alarm>?,
    notificationsBlocked: Boolean,
    onFixNotifications: () -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onOpen: (Alarm) -> Unit,
    onAdd: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val sorted = remember(alarms) { alarms.orEmpty().sortedWith(AlarmListOrder) }
    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = onAdd,
                modifier = Modifier.scrollable(
                    listState,
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                    overscrollEffect = rememberOverscrollEffect(),
                ),
            ) {
                Text("Add")
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
                    Text("Alarms")
                }
            }
            if (notificationsBlocked) {
                item {
                    Button(
                        onClick = onFixNotifications,
                        label = { Text("Notifications off") },
                        secondaryLabel = { Text("Open Lark to stop an alarm") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec)
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        transformation = SurfaceTransformation(spec),
                    )
                }
            }
            if (alarms != null && alarms.isEmpty()) {
                item {
                    Text(
                        text = "No alarms",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec)
                            .minimumVerticalContentPadding(top = 0.dp, bottom = TextDefaults.minimumBottomListContentPadding),
                    )
                }
            }
            items(sorted, key = { it.id }) { alarm ->
                SplitSwitchButton(
                    checked = alarm.enabled,
                    onCheckedChange = { onToggle(alarm, it) },
                    toggleContentDescription = "${alarm.timeText()} ${alarm.title()}, ${alarm.daysText()}",
                    onContainerClick = { onOpen(alarm) },
                    containerClickLabel = "Edit",
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        .animateItem(),
                    transformation = SurfaceTransformation(spec),
                    secondaryLabel = {
                        Text(alarm.listSubtitle(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    },
                ) {
                    Text(alarm.timeText(), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}
