package com.inci.campuscompanion.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val EVENT_UPDATES_ID = "event_updates"

    fun createEventUpdatesChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            EVENT_UPDATES_ID,
            "Event updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Alerts when new campus events are published" }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}