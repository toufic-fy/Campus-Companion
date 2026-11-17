package com.inci.campuscompanion.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.inci.campuscompanion.MainActivity
import com.inci.campuscompanion.R
import com.inci.campuscompanion.model.CampusEvent

object EventNotificationHelper {
    const val EVENTS_DEEP_LINK = "campus://events"

    private const val NEW_EVENTS_ID = 1001
    private const val TEST_ID = 1002

    fun postNewEvents(context: Context, newEvents: List<CampusEvent>) {
        val title = if (newEvents.size == 1) "New campus event" else "${newEvents.size} new campus events"
        val text = newEvents.joinToString(limit = 3) { it.title }
        post(context, NEW_EVENTS_ID, title, text)
    }

    fun postTest(context: Context) {
        post(context, TEST_ID, "Event updates are on", "You will be notified when new campus events are published.")
    }

    // Callers have already checked the preference; the permission is checked again here
    // because it can be revoked at any time from system settings.
    @SuppressLint("MissingPermission")
    private fun post(context: Context, id: Int, title: String, text: String) {
        if (!NotificationPermission.isGranted(context)) return
        val notification = NotificationCompat.Builder(context, NotificationChannels.EVENT_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openEventsIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    /** Tapping the notification opens the Events destination, with Tasks underneath for Back. */
    private fun openEventsIntent(context: Context): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, EVENTS_DEEP_LINK.toUri(), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
