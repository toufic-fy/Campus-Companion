package com.campus.events.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val EVENT_UPDATES_CHANNEL_ID = "event_updates"

/**
 * Creates the single "Event updates" channel used in this workshop.
 * Call once from Application.onCreate(), not from every composable.
 * On Android 8+ a channel must exist before the app posts to it.
 */
fun createEventUpdatesChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            EVENT_UPDATES_CHANNEL_ID,
            "Event updates",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Updates about campus events"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
