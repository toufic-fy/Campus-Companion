package com.inci.campuscompanion.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.inci.campuscompanion.MainActivity
import com.inci.campuscompanion.R
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.navigation.Routes

class EventNotificationHelper(context: Context) {

    private val context = context.applicationContext

    // Creating a channel that already exists does nothing, so this is safe to call often.
    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Event updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "New or changed campus events"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    // True only when the system lets this app post: the Android 13+ permission is
    // granted and notifications aren't blocked in system settings.
    fun hasSystemPermission(): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showTestNotification() {
        post(
            id = TEST_NOTIFICATION_ID,
            title = "Event updates are on",
            text = "You'll be notified when campus events are added or changed."
        )
    }

    fun showEventUpdates(changedEvents: List<CampusEvent>) {
        if (changedEvents.isEmpty()) return
        val text = if (changedEvents.size == 1) {
            changedEvents.first().title
        } else {
            "${changedEvents.size} events were added or changed"
        }
        post(id = UPDATES_NOTIFICATION_ID, title = "Campus events updated", text = text)
    }

    @SuppressLint("MissingPermission") // checked by hasSystemPermission() just below
    private fun post(id: Int, title: String, text: String) {
        if (!hasSystemPermission()) return
        createChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openEventsIntent())
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    // Tapping the notification opens Events through the NavHost deep link.
    // TaskStackBuilder gives a back stack of Tasks -> Events, so Back returns to Tasks.
    private fun openEventsIntent(): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Routes.EVENTS_DEEP_LINK.toUri(),
            context,
            MainActivity::class.java
        )
        return TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )!!
    }

    companion object {
        const val CHANNEL_ID = "event_updates"
        private const val TEST_NOTIFICATION_ID = 1
        private const val UPDATES_NOTIFICATION_ID = 2
    }
}
