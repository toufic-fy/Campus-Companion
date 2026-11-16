package com.inci.campuscompanion.ui.events

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inci.campuscompanion.CampusApplication
import com.inci.campuscompanion.notifications.EVENT_UPDATES_CHANNEL_ID
import com.inci.campuscompanion.ui.view_model.CampusEventViewModel
import kotlinx.coroutines.launch

@Composable
fun EventNotificationSettings(viewModel: CampusEventViewModel) {
    val context = LocalContext.current
    val application = context.applicationContext as CampusApplication
    val enabled by viewModel.eventUpdatesEnabled.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var systemAllowed by remember {
        mutableStateOf(application.eventNotifications.systemAllowsNotifications())
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        systemAllowed = application.eventNotifications.systemAllowsNotifications()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Denial must never overwrite the user's separate product preference.
        systemAllowed = application.eventNotifications.systemAllowsNotifications()
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Event updates", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Notify me when a refresh finds a new campus event.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { selected ->
                    viewModel.setEventUpdatesEnabled(selected)
                    if (selected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !systemAllowed
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                modifier = Modifier.semantics { contentDescription = "Event updates" },
            )
        }
        if (enabled && !systemAllowed) {
            Text(
                "Event updates are selected, but Android is blocking notifications. " +
                    "Allow them in notification settings. You can still use the app.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (enabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = systemAllowed,
                    onClick = {
                        scope.launch {
                            val posted = application.eventNotifications.showTest()
                            Toast.makeText(context,
                                if (posted) "Test notification sent" else "Notifications are blocked",
                                Toast.LENGTH_SHORT).show()
                        }
                    },
                ) { Text("Send test notification") }
                TextButton(onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, EVENT_UPDATES_CHANNEL_ID)
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.parse("package:${context.packageName}"))
                    }
                    context.startActivity(intent)
                }) { Text("System settings") }
            }
        }
    }
}
