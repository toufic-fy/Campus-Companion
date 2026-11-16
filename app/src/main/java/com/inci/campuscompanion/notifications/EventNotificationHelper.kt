package com.inci.campuscompanion.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.inci.campuscompanion.MainActivity
import com.inci.campuscompanion.R
import com.inci.campuscompanion.model.CampusEvent

class EventNotificationHelper(private val context: Context) {

    fun showNewEvents(events: List<CampusEvent>) {
        if (events.isEmpty()) return

        val title = if (events.size == 1) {
            "New campus event"
        } else {
            "${events.size} new campus events"
        }
        val text = events.joinToString(separator = ", ") { it.title }

        post(
            notificationId = NEW_EVENTS_NOTIFICATION_ID,
            title = title,
            text = text
        )
    }

    fun showTestNotification() {
        post(
            notificationId = TEST_NOTIFICATION_ID,
            title = "Event updates are on",
            text = "You will get a notification when new campus events are published."
        )
    }

    private fun post(notificationId: Int, title: String, text: String) {
        if (!NotificationPermissionChecker.isGranted(context)) return

        val notification = NotificationCompat.Builder(context, EVENT_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(eventsPendingIntent(notificationId))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun eventsPendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            EVENTS_DEEP_LINK.toUri(),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EVENTS_DEEP_LINK = "campus://events"
        private const val TEST_NOTIFICATION_ID = 1001
        private const val NEW_EVENTS_NOTIFICATION_ID = 1002
    }
}
