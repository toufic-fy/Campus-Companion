
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
import com.inci.campuscompanion.MainActivity
import com.inci.campuscompanion.R
import com.inci.campuscompanion.model.CampusEvent

object NotificationHelper {

    const val CHANNEL_ID = "event_updates"
    private const val CHANNEL_NAME = "Event updates"
    private const val CHANNEL_DESCRIPTION =
        "Notifications about important campus event updates"

    private const val TEST_NOTIFICATION_ID = 1001
    private const val EVENT_NOTIFICATION_ID = 1002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showTestNotification(context: Context) {
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Campus Companion")
            .setContentText("You have a new campus event!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(createPendingIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(TEST_NOTIFICATION_ID, notification)
    }

    fun showEventUpdatesNotification(
        context: Context,
        changedEvents: List<CampusEvent>
    ) {
        if (!hasNotificationPermission(context)) return
        if (changedEvents.isEmpty()) return

        val title = if (changedEvents.size == 1) {
            "Campus event updated"
        } else {
            "${changedEvents.size} campus events updated"
        }

        val message = if (changedEvents.size == 1) {
            changedEvents.first().title
        } else {
            changedEvents.joinToString(", ") { it.title }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(createPendingIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(EVENT_NOTIFICATION_ID, notification)
    }
}
