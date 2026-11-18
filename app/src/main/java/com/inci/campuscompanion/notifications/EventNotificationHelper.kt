package com.inci.campuscompanion.notifications

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
import com.inci.campuscompanion.data.repository.CampusEvent

class EventNotificationHelper(private val context: Context) {

    fun show(event: CampusEvent) {
        // Safety net: the real policy check happens in the caller
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(
            Intent.ACTION_VIEW,
            "campus://events/${event.id}".toUri(),
            context,
            MainActivity::class.java
        )

        val eventPendingIntent = PendingIntent.getActivity(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context, EVENT_UPDATES_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New campus event")
            .setContentText(event.title)
            .setContentIntent(eventPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(event.id.hashCode(), notification)
    }
}