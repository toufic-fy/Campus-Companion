package com.inci.campuscompanion.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val EVENT_UPDATES_CHANNEL_ID = "event_updates"

fun createEventUpdatesChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        EVENT_UPDATES_CHANNEL_ID,
        "Event updates",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Updates about campus events"
    }
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)
}
