package com.inci.campuscompanion.notification

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.inci.campuscompanion.MainActivity
import com.inci.campuscompanion.R
import com.inci.campuscompanion.data.UserPreferencesRepository
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.first

const val EVENT_UPDATES_CHANNEL_ID = "event_updates"

fun createEventUpdatesChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(EVENT_UPDATES_CHANNEL_ID, "Event updates", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "New campus events" }
        )
    }
}

class EventNotifications(private val context: Context, private val preferences: UserPreferencesRepository) {
    fun hasRuntimePermission(): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun systemAllowsNotifications(): Boolean {
        if (!hasRuntimePermission() || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(EVENT_UPDATES_CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    suspend fun showNewEvents(events: List<CampusEvent>): Boolean {
        if (events.isEmpty() || !preferences.eventUpdatesEnabled.first() || !systemAllowsNotifications()) return false
        val event = events.first()
        return post(
            event.id,
            if (events.size == 1) "New campus event" else "${events.size} new campus events",
            event.title
        )
    }

    suspend fun showTest(): Boolean {
        if (!preferences.eventUpdatesEnabled.first() || !systemAllowsNotifications()) return false
        return post(null, "Event updates are enabled", "Tap to open Campus Events.")
    }

    @SuppressLint("MissingPermission") // Rechecked immediately before notify; revocation is also caught.
    private fun post(eventId: String?, title: String, text: String): Boolean {
        val uri = Uri.Builder().scheme("campus").authority("events").apply {
            if (eventId != null) appendPath(eventId)
        }.build()
        val intent = Intent(Intent.ACTION_VIEW, uri, context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val notificationId = eventId?.hashCode() ?: 0
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, EVENT_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentText(text)
            .setContentIntent(pendingIntent).setAutoCancel(true).build()
        return try {
            // Permission can be revoked between the check and this call.
            if (!systemAllowsNotifications()) return false
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
