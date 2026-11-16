package com.inci.campuscompanion.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.inci.campuscompanion.notifications.EventNotificationHelper

@Composable
fun EventUpdatesSetting(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onSimulateNewEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var systemAllows by remember { mutableStateOf(EventNotificationHelper.canPostNotifications(context)) }

    // Re-check when coming back from the system settings screen.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        systemAllows = EventNotificationHelper.canPostNotifications(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        systemAllows = EventNotificationHelper.canPostNotifications(context)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Event updates", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Get a notification when new campus events are published.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    onEnabledChange(checked)
                    if (checked && !systemAllows && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }

        if (enabled && !systemAllows) {
            Text(
                "Notifications are blocked for this app, so you won't get event updates. You can allow them in system settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = {
                context.startActivity(EventNotificationHelper.appNotificationSettingsIntent(context))
            }) {
                Text("Open settings")
            }
        }

        if (enabled && systemAllows) {
            Row {
                TextButton(onClick = { EventNotificationHelper.showTestNotification(context) }) {
                    Text("Send test notification")
                }
                TextButton(onClick = onSimulateNewEvent) {
                    Text("Simulate new event")
                }
            }
        }
    }
}