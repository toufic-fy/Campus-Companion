
package com.inci.campuscompanion.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.eventUpdatesDataStore by preferencesDataStore(
    name = "event_updates_preferences"
)

class EventUpdatesPreferences(
    private val context: Context
) {
    companion object {
        private val ENABLED_KEY =
            booleanPreferencesKey("event_updates_enabled")
    }

    val enabled: Flow<Boolean> =
        context.eventUpdatesDataStore.data.map { preferences ->
            preferences[ENABLED_KEY] ?: false
        }

    suspend fun setEnabled(enabled: Boolean) {
        context.eventUpdatesDataStore.edit { preferences ->
            preferences[ENABLED_KEY] = enabled
        }
    }
}

fun hasNotificationPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun EventUpdatesToggle() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val preferences = remember(context.applicationContext) {
        EventUpdatesPreferences(context.applicationContext)
    }

    val enabled by preferences.enabled
        .collectAsStateWithLifecycle(initialValue = false)

    var permissionGranted by remember {
        mutableStateOf(hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = hasNotificationPermission(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Event notifications",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Receive updates about campus events",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = { newValue ->
                    scope.launch {
                        preferences.setEnabled(newValue)
                    }

                    if (
                        newValue &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !hasNotificationPermission(context)
                    ) {
                        permissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                }
            )
        }

        if (enabled) {
            Spacer(modifier = Modifier.height(8.dp))

            if (permissionGranted) {
                Text(
                    "Notification permission granted",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    "Notifications are blocked by Android. " +
                            "You can enable them in system settings.",
                    style = MaterialTheme.typography.bodySmall
                )

                TextButton(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        ).apply {
                            putExtra(
                                Settings.EXTRA_APP_PACKAGE,
                                context.packageName
                            )
                        }

                        context.startActivity(intent)
                    }
                ) {
                    Text("Open notification settings")
                }
            }
        }
    }
}
