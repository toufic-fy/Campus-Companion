package com.inci.campuscompanion.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inci.campuscompanion.data.repository.CampusEvent
import com.inci.campuscompanion.notifications.EventNotificationHelper

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissionGranted by viewModel.permissionGranted.collectAsStateWithLifecycle()
    val permissionDenied by viewModel.permissionDenied.collectAsStateWithLifecycle()

    // Re-check the system permission whenever the screen returns to the foreground
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissionState()
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    val showBlockedHint =
        !permissionGranted && (permissionDenied || settings.eventUpdatesEnabled)

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Event updates", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Get notified when new campus events are published",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Switch(
                checked = settings.eventUpdatesEnabled,
                onCheckedChange = { wantsOn ->
                    if (wantsOn && !permissionGranted) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.onEventUpdatesToggled(true)
                        }
                    } else {
                        viewModel.onEventUpdatesToggled(wantsOn)
                    }
                }
            )
        }

        if (showBlockedHint) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Notifications are blocked. You can enable them in system settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
                context.startActivity(intent)
            }) {
                Text("Open notification settings")
            }
        }

        // TEMPORARY: remove after step 6
        Button(onClick = {
            if (settings.eventUpdatesEnabled && permissionGranted) {
                EventNotificationHelper(context).show(
                    CampusEvent(
                        id = "test-1",
                        title = "Test campus event",
                        "e32d32",
                        locationLabel = "Test location"
                    )  // adapt to your Event class
                )
            }
        }) {
            Text("Send test notification")
        }
    }
}
