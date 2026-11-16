package com.campus.events.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.events.notifications.NotificationPermissionChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The user may change permission or channel importance in system settings and come back.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshSystemState()
        onPauseOrDispose { }
    }

    // Workshop step 4: request permission in context, next to the choice that needs it.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    fun onToggle(enabled: Boolean) {
        viewModel.setEventUpdatesEnabled(enabled)
        if (enabled && state.requiresRuntimePermission && !state.permissionGranted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Workshop step 3: the preference toggle shows the current product choice.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Event updates", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Get notified when a new campus event is published. " +
                            "Routine refreshes with no changes never notify you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = state.eventUpdatesEnabled, onCheckedChange = ::onToggle)
            }

            if (state.blockedBySystem) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (!state.permissionGranted)
                                "Android notifications are off for Campus Events."
                            else
                                "The \"Event updates\" channel is turned off.",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "Your choice is saved, but nothing will be posted until you allow it " +
                                "in system settings. The rest of the app keeps working.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row {
                            if (!state.permissionGranted && state.requiresRuntimePermission) {
                                TextButton(onClick = {
                                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }) { Text("Ask again") }
                            }
                            TextButton(onClick = {
                                context.startActivity(
                                    if (state.channelBlocked && state.permissionGranted)
                                        NotificationPermissionChecker.eventChannelSettingsIntent(context)
                                    else
                                        NotificationPermissionChecker.appNotificationSettingsIntent(context)
                                )
                            }) { Text("Open system settings") }
                        }
                    }
                }
            }

            StatusRow("Preference (DataStore)", if (state.eventUpdatesEnabled) "On" else "Off")
            StatusRow("Android permission", if (state.permissionGranted) "Granted" else "Not granted")
            StatusRow("Event updates channel", if (state.channelBlocked) "Blocked" else "Enabled")
            StatusRow("Will notify?", if (state.canNotify) "Yes" else "No")

            OutlinedButton(
                onClick = {
                    context.startActivity(
                        NotificationPermissionChecker.eventChannelSettingsIntent(context)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Channel settings (importance, sound…)") }

            HorizontalDivider()
            Text("Workshop test tools", style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = viewModel::sendTestNotification,
                enabled = state.canNotify,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Send test notification") }

            Button(
                onClick = viewModel::simulateNewEventAndRefresh,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Publish a new event on the server + refresh") }

            OutlinedButton(
                onClick = viewModel::refreshNow,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Refresh with no server changes") }

            Text(
                "Last refresh: " + (state.lastRefreshOutcome ?: "not run yet"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
