package com.inci.campuscompanion.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inci.campuscompanion.model.CampusEvent

@Composable
fun EventsScreen(
    eventsViewModel: EventsViewModel = viewModel()
) {
    val events by eventsViewModel.events.collectAsStateWithLifecycle()
    val status by eventsViewModel.refreshStatus.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Campus Events",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Discover what's happening on campus.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Status of the background refresh (queued / running / done / failed).
        if (status != RefreshStatus.Idle) {
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (status == RefreshStatus.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        EventNotificationSettings(eventsViewModel = eventsViewModel)

        Button(
            onClick = { eventsViewModel.refresh() },
            enabled = status != RefreshStatus.Running,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text("Refresh events")
        }

        if (events.isEmpty()) {
            // Nothing cached yet: explain what's happening instead of a blank screen.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (status) {
                    RefreshStatus.Running, RefreshStatus.Idle -> {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading events...",
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    RefreshStatus.Waiting -> Text(
                        "No saved events yet. They will load once you're online."
                    )
                    RefreshStatus.Failed -> Text(
                        "Couldn't load campus events. Tap Refresh to try again."
                    )
                    RefreshStatus.UpToDate -> Text(
                        "No campus events are available right now."
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = events,
                    key = { event -> event.id }
                ) { event ->
                    EventCard(event = event)
                }
            }
        }
    }
}

@Composable
private fun EventNotificationSettings(eventsViewModel: EventsViewModel) {
    val savedChoice by eventsViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    var hasPermission by remember { mutableStateOf(eventsViewModel.hasNotificationPermission()) }
    var showBlockedHint by remember { mutableStateOf(false) }

    // Asked only when the user turns the switch on (contextual request).
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            eventsViewModel.setNotificationsEnabled(true)
        }
        // Denied: the preference stays off and the app keeps working.
        showBlockedHint = !granted
    }

    // On only if the user chose it AND the system allows it.
    val isOn = savedChoice && hasPermission

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Event update notifications",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isOn,
                    onCheckedChange = { turnOn ->
                        showBlockedHint = false
                        when {
                            !turnOn -> eventsViewModel.setNotificationsEnabled(false)
                            eventsViewModel.hasNotificationPermission() -> {
                                hasPermission = true
                                eventsViewModel.setNotificationsEnabled(true)
                            }
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else -> showBlockedHint = true
                        }
                    }
                )
            }

            if (showBlockedHint) {
                Text(
                    text = "Notifications are blocked. You can allow them in system settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            TextButton(
                onClick = { eventsViewModel.sendTestNotification() },
                enabled = isOn
            ) {
                Text("Send test notification")
            }
        }
    }
}

@Composable
private fun EventCard(event: CampusEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
