package com.inci.campuscompanion.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val EVENT_UPDATES = "event_updates"

    /** On Android 8+ a channel must exist before the app posts to it. Creating it twice is a no-op. */
    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            EVENT_UPDATES,
            "Event updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "New campus events found by the background refresh"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
