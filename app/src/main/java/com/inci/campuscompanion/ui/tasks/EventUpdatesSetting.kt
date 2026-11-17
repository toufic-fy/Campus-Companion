package com.inci.campuscompanion.ui.tasks

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import com.inci.campuscompanion.notification.EventNotificationHelper
import com.inci.campuscompanion.notification.NotificationPermission

@Composable
fun EventUpdatesSetting(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(NotificationPermission.isGranted(context)) }

    // The student may change the permission in system settings and come back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = NotificationPermission.isGranted(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted && NotificationPermission.isGranted(context)
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Event updates", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Notify me when new campus events are published",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    onEnabledChange(checked)
                    // Ask in context: only when the student turns the feature on, never at launch.
                    if (checked && !permissionGranted && NotificationPermission.needsRuntimeRequest) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }

        if (enabled && permissionGranted) {
            TextButton(onClick = { EventNotificationHelper.postTest(context) }) {
                Text("Send test notification")
            }
        }

        if (enabled && !permissionGranted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Notifications are blocked for this app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { context.startActivity(NotificationPermission.appSettingsIntent(context)) }) {
                    Text("Open settings")
                }
            }
        }
    }
}
