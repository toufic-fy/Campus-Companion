package com.inci.campuscompanion.notification

import android.Manifest
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
import com.inci.campuscompanion.model.CampusEvent

const val EVENTS_DEEP_LINK = "campus://events"

class EventNotificationHelper(private val context: Context) {

    fun showNewEvent(event: CampusEvent) {
        val notification = NotificationCompat.Builder(context, EVENT_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New campus event")
            .setContentText(event.title)
            .setContentIntent(eventsPendingIntent("$EVENTS_DEEP_LINK/${event.id}", event.id.hashCode()))
            .setAutoCancel(true)
            .build()

        post(event.id.hashCode(), notification)
    }

    fun showNewEventsSummary(count: Int) {
        val notification = NotificationCompat.Builder(context, EVENT_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$count new campus events")
            .setContentText("Tap to see what's coming up")
            .setContentIntent(eventsPendingIntent(EVENTS_DEEP_LINK, SUMMARY_NOTIFICATION_ID))
            .setAutoCancel(true)
            .build()

        post(SUMMARY_NOTIFICATION_ID, notification)
    }

    private fun eventsPendingIntent(uri: String, requestCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, uri.toUri(), context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun post(id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val SUMMARY_NOTIFICATION_ID = 1001
    }
}
