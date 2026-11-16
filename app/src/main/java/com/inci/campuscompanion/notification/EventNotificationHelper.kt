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

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun notifyNewEvents(events: List<CampusEvent>) {
        if (events.isEmpty() || !hasPermission()) return
        val title = if (events.size == 1) "New campus event" else "${events.size} new campus events"
        show(NEW_EVENTS_ID, title, events.joinToString(", ") { it.title })
    }

    fun notifyTest() {
        if (!hasPermission()) return
        show(TEST_ID, "Event updates are on", "Tap to open campus events")
    }

    private fun show(id: Int, title: String, text: String) {
        val intent = Intent(Intent.ACTION_VIEW, EVENTS_DEEP_LINK.toUri(), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.EVENT_UPDATES_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private companion object {
        const val NEW_EVENTS_ID = 1001
        const val TEST_ID = 1002
    }
}