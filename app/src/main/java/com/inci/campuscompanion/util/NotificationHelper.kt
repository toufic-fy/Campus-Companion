
package com.inci.campuscompanion.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.inci.campuscompanion.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "event_updates"

    // Create the notification channel
    fun createNotificationChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Event Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about campus event updates"
            }

            val manager = context.getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }

    // Check whether notifications are allowed
    private fun canPostNotifications(context: Context): Boolean {

        // Check user's preference
        if (!NotificationPreferences.isEnabled(context)) {
            return false
        }

        // Check Android 13+ runtime permission
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        // Check whether notifications are enabled in system settings
        return NotificationManagerCompat
            .from(context)
            .areNotificationsEnabled()
    }

    // Display an event update notification
    fun showEventUpdateNotification(
        context: Context,
        eventId: String,
        eventTitle: String,
        message: String
    ) {

        if (!canPostNotifications(context)) {
            return
        }

        createNotificationChannel(context)

        val notificationId = eventId.hashCode()

        // Intent opened when notification is tapped
        val intent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            putExtra("navigate_to_events", true)
            putExtra("event_id", eventId)

            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(eventTitle)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat
                .from(context)
                .notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission may have been revoked before posting.
        }
    }

    // Temporary test notification
    fun showTestNotification(context: Context) {

        showEventUpdateNotification(
            context = context,
            eventId = "test_event",
            eventTitle = "Campus Event Update",
            message = "There is a new update in campus events!"
        )
    }
}
