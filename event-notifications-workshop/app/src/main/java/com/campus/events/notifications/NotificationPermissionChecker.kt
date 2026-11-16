package com.campus.events.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat

/** Answers "may this app post ordinary notifications on this device?" — always asks the system. */
class NotificationPermissionChecker(private val context: Context) {

    /** Whether the runtime permission must be requested on this Android version. */
    val requiresRuntimePermission: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun isGranted(): Boolean {
        if (requiresRuntimePermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        // Below Android 13 there is no runtime permission, but the user can still block the app.
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Whether the user switched off the "Event updates" channel in system settings. */
    fun isEventChannelBlocked(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val channel = NotificationManagerCompat.from(context)
            .getNotificationChannel(EVENT_UPDATES_CHANNEL_ID) ?: return false
        return channel.importance == NotificationManagerCompat.IMPORTANCE_NONE
    }

    companion object {
        /** A clear path to the system screen where the user can change their mind later. */
        fun appNotificationSettingsIntent(context: Context): Intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
            }

        fun eventChannelSettingsIntent(context: Context): Intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, EVENT_UPDATES_CHANNEL_ID)
            } else {
                appNotificationSettingsIntent(context)
            }
    }
}
