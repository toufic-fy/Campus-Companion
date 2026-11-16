package com.campus.events.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.campus.events.MainActivity
import com.campus.events.R
import com.campus.events.data.Event

const val EVENTS_DEEP_LINK = "campus://events"

/** Builds and posts Event update notifications. Callers must check the policy first. */
class EventNotificationHelper(private val context: Context) {

    /** One new event: the title says what changed, the tap opens that event. */
    fun show(event: Event) {
        val notification = NotificationCompat.Builder(context, EVENT_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New campus event")
            .setContentText(event.title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${event.title}\n${event.startsAt} · ${event.location}")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntentFor("$EVENTS_DEEP_LINK/${event.id}", event.id.hashCode()))
            .setAutoCancel(true)
            .build()

        // Stable id: the same event updates/replaces its notification instead of stacking.
        post(event.id.hashCode(), notification)
    }

    /** Several new events: one summary notification that opens the Events list. */
    fun showSummary(events: List<Event>) {
        val style = NotificationCompat.InboxStyle()
        events.take(5).forEach { style.addLine(it.title) }
        if (events.size > 5) style.setSummaryText("+${events.size - 5} more")

        val notification = NotificationCompat.Builder(context, EVENT_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${events.size} new campus events")
            .setContentText(events.joinToString { it.title })
            .setStyle(style)
            .setNumber(events.size)
            .setContentIntent(pendingIntentFor(EVENTS_DEEP_LINK, SUMMARY_NOTIFICATION_ID))
            .setAutoCancel(true)
            .build()

        post(SUMMARY_NOTIFICATION_ID, notification)
    }

    private fun pendingIntentFor(uri: String, requestCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, uri.toUri(), context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // The permission is checked by NotificationPermissionChecker before we get here; the
    // try/catch covers the race where the user revokes it in between.
    @SuppressLint("MissingPermission")
    private fun post(id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission was revoked: silently skip, the app keeps working.
        }
    }

    private companion object {
        const val SUMMARY_NOTIFICATION_ID = 1001
    }
}
