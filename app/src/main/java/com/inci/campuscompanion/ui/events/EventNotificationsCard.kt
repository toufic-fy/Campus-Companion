package com.inci.campuscompanion.ui.events

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.inci.campuscompanion.notifications.EventNotificationHelper
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme

/**
 * Stateful wrapper: owns the permission launcher and re-checks the system state
 * every time the screen resumes (e.g. after the student changes it in Settings).
 */
@Composable
fun EventNotificationsSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var systemAllows by remember { mutableStateOf(EventNotificationHelper.canPostNotifications(context)) }
    var permissionDenied by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        systemAllows = EventNotificationHelper.canPostNotifications(context)
    }

    // Step 4: asked only when the student turns the toggle on, never at app launch.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        systemAllows = EventNotificationHelper.canPostNotifications(context)
        permissionDenied = !granted
        // Denied: keep the preference off. The app keeps working without notifications.
        onEnabledChange(granted)
    }

    EventNotificationsCard(
        enabled = enabled,
        systemAllows = systemAllows,
        permissionDenied = permissionDenied,
        testResult = testResult,
        onToggle = { wantsOn ->
            testResult = null
            if (!wantsOn) {
                onEnabledChange(false)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                EventNotificationHelper.needsRuntimePermission(context)
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                permissionDenied = false
                onEnabledChange(true)
            }
        },
        onSendTest = {
            val posted = EventNotificationHelper.showTestNotification(context)
            testResult = if (posted) "Test notification sent." else
                "Notifications are blocked in system settings."
        },
        modifier = modifier
    )
}

/** Stateless UI so it can be previewed. */
@Composable
fun EventNotificationsCard(
    enabled: Boolean,
    systemAllows: Boolean,
    permissionDenied: Boolean,
    testResult: String?,
    onToggle: (Boolean) -> Unit,
    onSendTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Both gates must be open before anything can be posted.
    val active = enabled && systemAllows

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (active) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Event update notifications",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = statusText(enabled, systemAllows, permissionDenied),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }

            if (active) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onSendTest) {
                    Text("Send test notification")
                }
            }

            if (testResult != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = testResult,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun statusText(enabled: Boolean, systemAllows: Boolean, permissionDenied: Boolean): String =
    when {
        permissionDenied && !enabled ->
            "Permission denied. Notifications stay off; you can allow them later in system settings."
        !enabled -> "Off. Turn on to hear about new or changed events."
        !systemAllows -> "On, but blocked in system settings. Nothing will be posted."
        else -> "On. You'll be notified when events are added or changed."
    }

@Preview(showBackground = true)
@Composable
private fun EventNotificationsCardPreview() {
    CampusCompanionTheme {
        EventNotificationsCard(
            enabled = true,
            systemAllows = true,
            permissionDenied = false,
            testResult = null,
            onToggle = {},
            onSendTest = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
