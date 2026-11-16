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
import androidx.core.net.toUri
import com.inci.campuscompanion.MainActivity
import com.inci.campuscompanion.R

/**
 * Owns the "Event updates" channel and the single event-update notification.
 *
 * Posting is gated twice: the caller checks the user's in-app preference, and
 * [canPostNotifications] checks the system permission / app-level switch.
 */
object EventNotificationHelper {

    const val CHANNEL_ID = "event_updates"
    const val EVENTS_DEEP_LINK = "campuscompanion://events"

    // One fixed id: a newer update replaces the previous one instead of stacking.
    private const val EVENT_UPDATE_NOTIFICATION_ID = 1001

    /** Safe to call many times; Android ignores re-creating an existing channel. */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.event_updates_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.event_updates_channel_description)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /** True when the runtime permission is needed (Android 13+) but not granted yet. */
    fun needsRuntimePermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

    /** System side of the gate: permission granted and notifications not blocked in settings. */
    fun canPostNotifications(context: Context): Boolean =
        !needsRuntimePermission(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Tapping the notification opens the Events destination through its deep link. */
    private fun openEventsIntent(context: Context): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            EVENTS_DEEP_LINK.toUri(),
            context,
            MainActivity::class.java
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Posts the notification only if the system allows it.
     * Returns true when it was actually posted.
     */
    @SuppressLint("MissingPermission") // checked by canPostNotifications()
    fun showEventUpdate(context: Context, title: String, text: String): Boolean {
        if (!canPostNotifications(context)) return false
        createChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_event)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openEventsIntent(context))
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context)
                .notify(EVENT_UPDATE_NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false // permission revoked between the check and the post
        }
    }

    /** Step 5 of the workshop: one test notification to check title, icon and tap action. */
    fun showTestNotification(context: Context): Boolean =
        showEventUpdate(
            context,
            title = "Event updates are on",
            text = "This is a test. Tap to open Campus Events."
        )
}
