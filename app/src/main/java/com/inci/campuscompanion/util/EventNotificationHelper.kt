package com.inci.campuscompanion.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.inci.campuscompanion.MainActivity
import com.inci.campuscompanion.R
import com.inci.campuscompanion.model.CampusEvent

object EventNotificationHelper {

    const val CHANNEL_ID = "event_updates"
    private const val CHANNEL_NAME = "Event updates"
    private const val CHANNEL_DESC = "Notifications when campus events are added or updated"
    private const val TAG = "EventNotification"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Small policy function: returns true ONLY when BOTH:
     * 1. User eventUpdatesEnabled preference is true.
     * 2. Android system notification permission is granted.
     */
    fun canPostNotification(context: Context, eventUpdatesEnabled: Boolean): Boolean {
        val systemPermissionGranted = hasNotificationPermission(context)
        Log.d(
            TAG,
            "canPostNotification check -> preference (eventUpdatesEnabled): $eventUpdatesEnabled, system permission: $systemPermissionGranted"
        )
        return eventUpdatesEnabled && systemPermissionGranted
    }

    /**
     * Builds and displays a notification for a new/updated event if policy check passes.
     */
    fun show(context: Context, event: CampusEvent, eventUpdatesEnabled: Boolean) {
        val canPost = canPostNotification(context, eventUpdatesEnabled)
        Log.d(TAG, "show() called for event '${event.title}' (ID: ${event.id}). Can post: $canPost")

        if (!canPost) {
            return
        }

        createNotificationChannel(context)

        // Deep link intent targeting campus://events/{id}
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("campus://events/${event.id}"),
            context,
            MainActivity::class.java
        ).apply {
            putExtra("destination", "events")
            putExtra("eventId", event.id)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            event.id.hashCode(),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New campus event")
            .setContentText(event.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${event.title}\n${event.description ?: ""}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(event.id.hashCode(), builder.build())
            Log.d(TAG, "Successfully posted notification for event ID: ${event.id}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while posting notification: ${e.message}", e)
        }
    }
}
