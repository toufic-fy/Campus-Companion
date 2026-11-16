package com.inci.campuscompanion.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun EventUpdatesSetting(
    enabled: Boolean,
    permissionGranted: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    openNotificationSettingsIntent: () -> Intent,
    onSendTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onPermissionResult(granted)
    }
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Event updates", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Get notified when a new campus event is published",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    onEnabledChange(checked)
                    // permission only needed on android 13+
                    if (checked &&
                        !permissionGranted &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
        }

        when {
            enabled && !permissionGranted -> {
                Text(
                    "Notifications are blocked for this app, so you won't get updates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = { context.startActivity(openNotificationSettingsIntent()) }) {
                    Text("Open notification settings")
                }
            }
            enabled && permissionGranted -> {
                TextButton(onClick = onSendTest) {
                    Text("Send test notification")
                }
            }
        }
    }
}
