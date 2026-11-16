package com.inci.campuscompanion.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.inci.campuscompanion.MainActivity
import com.inci.campuscompanion.R
import com.inci.campuscompanion.model.CampusEvent

class EventNotificationHelper(private val context: Context) {

    fun notifyEvent(event: CampusEvent, title: String) {
        post(event.id.hashCode(), event.id, title, event.title)
    }

    fun notifyTest() {
        post(TEST_ID.hashCode(), TEST_ID, "Test notification", "Event updates are working")
    }

    fun notifyNewEvents(events: List<CampusEvent>) {
        if (events.size > MAX_INDIVIDUAL) {
            post(
                SUMMARY_ID.hashCode(),
                SUMMARY_ID,
                "${events.size} new campus events",
                "Open Events to see what is new"
            )
        } else {
            events.forEach { notifyEvent(it, "New campus event") }
        }
    }

    fun notifyChangedEvents(events: List<CampusEvent>) {
        events.forEach { notifyEvent(it, "Campus event updated") }
    }

    @SuppressLint("MissingPermission")
    private fun post(notificationId: Int, eventId: String, title: String, text: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("campus://events/$eventId"),
            context,
            MainActivity::class.java
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, EVENT_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private companion object {
        const val MAX_INDIVIDUAL = 3
        const val SUMMARY_ID = "new_events_summary"
        const val TEST_ID = "test_notification"
    }
}
