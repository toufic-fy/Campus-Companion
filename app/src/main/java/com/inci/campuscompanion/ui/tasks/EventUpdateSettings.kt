package com.inci.campuscompanion.ui.tasks

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.local.pref.dataStore
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.notifications.EventNotifications
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun EventUpdateSettings() {
    val context = LocalContext.current
    val preferences = remember { UserPreferencesRepository(context.applicationContext.dataStore) }
    val enabled by preferences.eventUpdatesEnabled.collectAsStateWithLifecycle(initialValue = false)
    var allowed by remember { mutableStateOf(EventNotifications.systemAllows(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = EventNotifications.systemAllows(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = EventNotifications.systemAllows(context)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Event updates", style = MaterialTheme.typography.titleMedium)
                Text("Notify me when new campus events are published.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = enabled, modifier = Modifier.semantics { contentDescription = "Enable event updates" },
                onCheckedChange = { value ->
                    scope.launch {
                        preferences.setEventUpdatesEnabled(value)
                        message = null
                        if (value && Build.VERSION.SDK_INT >= 33 &&
                            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                })
        }
        if (enabled && !allowed) {
            Text("Event updates are enabled in the app, but Android notifications are blocked. You can allow them in system settings.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = {
                val settingsIntent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                context.startActivity(settingsIntent)
            }) { Text("Open notification settings") }
        }
        if (enabled && allowed) {
            TextButton(onClick = {
                scope.launch {
                    val event = AppDatabase.getDatabase(context).eventDao().getEvents().first().firstOrNull()?.toDomain()
                        ?: CampusEvent("workshop-test", "Tap to open Campus Events", "A test notification", "", null, "", null)
                    message = if (EventNotifications.show(context, listOf(event), test = true))
                        "Test notification sent. Open the notification shade and tap it." else "Notification blocked. Check your settings."
                    allowed = EventNotifications.systemAllows(context)
                }
            }) { Text("Send test notification") }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
