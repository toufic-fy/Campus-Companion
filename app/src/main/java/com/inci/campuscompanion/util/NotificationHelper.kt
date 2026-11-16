package com.inci.campuscompanion.util

import android.content.Context
import com.inci.campuscompanion.model.CampusEvent

object NotificationHelper {
    const val CHANNEL_ID = EventNotificationHelper.CHANNEL_ID

    fun createNotificationChannel(context: Context) =
        EventNotificationHelper.createNotificationChannel(context)

    fun hasNotificationPermission(context: Context): Boolean =
        EventNotificationHelper.hasNotificationPermission(context)

    fun showEventUpdateNotification(
        context: Context,
        title: String,
        message: String,
        eventId: String,
        userEnabledPreference: Boolean
    ) {
        val fakeEvent = CampusEvent(
            id = eventId,
            title = title,
            description = message,
            date = "Today",
            location = "Campus"
        )
        EventNotificationHelper.show(context, fakeEvent, userEnabledPreference)
    }
}
