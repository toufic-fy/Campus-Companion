package com.inci.campuscompanion.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Asks for POST_NOTIFICATIONS in context (on the events screen), explains why, and
 * falls back to the system settings page once the user has permanently denied it.
 */
@Composable
fun NotificationPermissionCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var granted by rememberSaveable { mutableStateOf(EventNotifier.hasPermission(context)) }
    var dismissed by rememberSaveable { mutableStateOf(false) }
    var deniedOnce by rememberSaveable { mutableStateOf(false) }

    // The user may grant or revoke the permission in Settings while the app is in the background.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = EventNotifier.hasPermission(context)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        if (!isGranted) deniedOnce = true
    }

    if (granted || dismissed || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val activity = context.findActivity()
    val showRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.POST_NOTIFICATIONS
    )
    // After a denial with no rationale, the system dialog will not show again: send the user to Settings.
    val permanentlyDenied = deniedOnce && !showRationale

    Card(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Get notified about new events", style = MaterialTheme.typography.titleSmall)
            Text(
                text = when {
                    permanentlyDenied -> "Notifications are turned off. You can enable them in Settings."
                    showRationale -> "We only notify you when a background refresh finds new campus events."
                    else -> "Allow notifications to hear about new campus events after a background refresh."
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row {
                TextButton(onClick = {
                    if (permanentlyDenied) {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    } else {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) { Text(if (permanentlyDenied) "Open settings" else "Allow") }
                TextButton(onClick = { dismissed = true }) { Text("Not now") }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
