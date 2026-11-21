package com.inci.campuscompanion.notifications

import android.Manifest
import android.annotation.SuppressLint
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
import com.inci.campuscompanion.R

/**
 * Everything related to "Event updates" notifications lives here.
 */
object EventNotifications {

    // The ID never changes once shipped: Android remembers the user's
    // settings for the channel under this ID.
    const val CHANNEL_ID = "event_updates"

    // Fixed ID for the test notification: sending it again replaces the old one.
    const val TEST_NOTIFICATION_ID = 1001

    /**
     * Step 2: create the "Event updates" channel.
     * Safe to call many times: if the channel already exists, Android ignores the call.
     */
    fun createChannel(context: Context) {
        // Channels only exist on Android 8.0 (API 26) and newer. Our minSdk is 24.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_event_updates_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_event_updates_description)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /** True if Android will actually let us show a notification right now. */
    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Step 5: build and show one "Event updates" notification.
     * - Title + text: what changed
     * - Small icon: ic_notification_event (must be a simple white shape)
     * - Tap action: opens MainActivity, then the notification disappears
     *
     * Returns false if notifications are blocked, so the caller can tell the user.
     */
    @SuppressLint("MissingPermission") // checked in canPostNotifications()
    fun showEventUpdate(
        context: Context,
        title: String,
        text: String,
        notificationId: Int
    ): Boolean {
        if (!canPostNotifications(context)) return false

        // Make sure the channel exists (e.g. when called from a background worker).
        createChannel(context)

        // Tap action: open the app's main screen.
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_event)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // used on Android 7.x (no channels)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // remove it when tapped
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return true
    }
}
