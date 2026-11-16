
package com.inci.campuscompanion.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.inci.campuscompanion.MainActivity
import kotlinx.coroutines.flow.first

private const val TEST_NOTIFICATION_ID = 1001

@SuppressLint("MissingPermission")
suspend fun showTestEventNotification(
    context: Context
): Boolean {

    val appContext = context.applicationContext

    // Check the user's saved preference
    val enabled = EventUpdatesPreferences(appContext)
        .enabled.first()

    if (!enabled) return false

    // Check Android notification permission
    if (!hasNotificationPermission(appContext)) {
        return false
    }

    val manager = NotificationManagerCompat.from(appContext)

    if (!manager.areNotificationsEnabled()) {
        return false
    }

    // Open Campus Events when notification is tapped
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("campus://events"),
        appContext,
        MainActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pendingIntent = PendingIntent.getActivity(
        appContext,
        TEST_NOTIFICATION_ID,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(
        appContext,
        EVENT_UPDATES_CHANNEL_ID
    )
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Campus Events")
        .setContentText("Test event notification")
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    manager.notify(TEST_NOTIFICATION_ID, notification)

    return true
}

@SuppressLint("MissingPermission")
suspend fun showNewEventsNotification(
    context: Context,
    count: Int,
    eventTitle: String
): Boolean {

    val appContext = context.applicationContext

    if (count <= 0) return false

    val enabled = EventUpdatesPreferences(appContext)
        .enabled.first()

    if (!enabled || !hasNotificationPermission(appContext)) {
        return false
    }

    val manager = NotificationManagerCompat.from(appContext)

    if (!manager.areNotificationsEnabled()) {
        return false
    }

    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("campus://events"),
        appContext,
        MainActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pendingIntent = PendingIntent.getActivity(
        appContext,
        2002,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(
        appContext,
        EVENT_UPDATES_CHANNEL_ID
    )
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(
            if (count == 1) "New campus event"
            else "$count new campus events"
        )
        .setContentText(
            if (count == 1) eventTitle
            else "Open Campus Events to see what's new."
        )
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    manager.notify(2002, notification)

    return true
}

