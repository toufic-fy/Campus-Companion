package com.inci.campuscompanion.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.inci.campuscompanion.MainActivity
import com.inci.campuscompanion.R
import com.inci.campuscompanion.model.CampusEvent

object EventNotificationHelper {

    const val EVENTS_DEEP_LINK = "campus://events"
    private const val EVENT_UPDATES_NOTIFICATION_ID = 1001

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showNewEvents(context: Context, newEvents: List<CampusEvent>) {
        if (newEvents.isEmpty()) return
        val title = if (newEvents.size == 1) "New campus event" else "${newEvents.size} new campus events"
        val text = if (newEvents.size == 1) {
            newEvents.first().title
        } else {
            "${newEvents.first().title} and ${newEvents.size - 1} more"
        }
        post(context, title, text)
    }

    fun showTestNotification(context: Context) {
        post(context, "Test notification", "Event updates are working.")
    }

    fun appNotificationSettingsIntent(context: Context): Intent {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @SuppressLint("MissingPermission") // checked by canPostNotifications() just below
    private fun post(context: Context, title: String, text: String) {
        if (!canPostNotifications(context)) return

        val intent = Intent(
            Intent.ACTION_VIEW,
            EVENTS_DEEP_LINK.toUri(),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, EVENT_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(EVENT_UPDATES_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
        }
    }
}