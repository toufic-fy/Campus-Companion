package com.inci.campuscompanion.notifications

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
import com.inci.campuscompanion.R
import com.inci.campuscompanion.model.CampusEvent

/** Posts "new campus events" notifications on a dedicated channel. */
class EventNotifier(private val context: Context) {

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "New campus events",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Alerts when a background refresh finds new events." }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(): Boolean = hasPermission(context) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notifyNewEvents(newEvents: List<CampusEvent>) {
        if (newEvents.isEmpty() || !canNotify()) return
        createChannel()

        val openEvents = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_DESTINATION, DESTINATION_EVENTS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (newEvents.size == 1) "New campus event" else "${newEvents.size} new campus events"
        val inbox = NotificationCompat.InboxStyle().also { style ->
            newEvents.take(5).forEach { style.addLine("${it.title} · ${it.timeLabel}") }
            if (newEvents.size > 5) style.setSummaryText("+${newEvents.size - 5} more")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_event)
            .setContentTitle(title)
            .setContentText(newEvents.first().title)
            .setStyle(inbox)
            .setContentIntent(openEvents)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post; nothing to show.
        }
    }

    companion object {
        const val CHANNEL_ID = "new-campus-events"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_DESTINATION = "destination"
        const val DESTINATION_EVENTS = "events"

        /** POST_NOTIFICATIONS is a runtime permission only on Android 13 (API 33) and above. */
        fun hasPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }
}
