package com.inci.campuscompanion.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inci.campuscompanion.AppGraph
import kotlinx.coroutines.launch

@Composable
fun NotificationSettings(graph: AppGraph) {
    val enabled by graph.preferences.eventUpdatesEnabled.collectAsStateWithLifecycle(initialValue = false)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    var allowed by remember { mutableStateOf(graph.notifications.systemAllowsNotifications()) }
    var message by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = graph.notifications.systemAllowsNotifications()
        message = if (allowed) "Notifications allowed." else "Notifications blocked. You can allow them in system settings."
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = graph.notifications.systemAllowsNotifications()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Event updates")
            Switch(checked = enabled, onCheckedChange = { wantUpdates ->
                scope.launch {
                    graph.preferences.setEventUpdatesEnabled(wantUpdates)
                    if (wantUpdates && Build.VERSION.SDK_INT >= 33 && !graph.notifications.hasRuntimePermission()) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            })
        }
        Text(when {
            !enabled -> "Enable to hear about newly published campus events."
            !allowed -> "Updates are enabled here, but blocked by Android. Open notification settings to allow them."
            else -> "New events may notify you after a refresh."
        }, style = MaterialTheme.typography.bodySmall)
        Row {
            TextButton(modifier = Modifier.weight(1f), onClick = {
                val intent = if (Build.VERSION.SDK_INT >= 26) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null))
                }
                context.startActivity(intent)
            }) { Text("Notification settings") }
            TextButton(modifier = Modifier.weight(1f), enabled = enabled && allowed, onClick = {
                scope.launch {
                    message = if (graph.notifications.showTest()) "Test notification sent." else "Notification was blocked."
                }
            }) { Text("Send test") }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
