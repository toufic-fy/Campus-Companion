package com.inci.campuscompanion.notifications

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
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.local.pref.dataStore
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.first

object EventNotifications {
    const val CHANNEL_ID = "event_updates"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Event updates", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Updates about newly published campus events" }
            )
        }
    }

    fun systemAllows(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    suspend fun show(context: Context, events: List<CampusEvent>, test: Boolean = false): Boolean {
        if (events.isEmpty() || !UserPreferencesRepository(context.dataStore).eventUpdatesEnabled.first()
            || !systemAllows(context)) return false
        val event = events.first()
        val uri = Uri.Builder().scheme("campus").authority("events").appendPath(event.id).build()
        val intent = Intent(Intent.ACTION_VIEW, uri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(context, event.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (test) "Test campus event" else if (events.size == 1) "New campus event" else "${events.size} new campus events")
            .setContentText(event.title)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(event.id.hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false // Permission may be revoked between the check and the post.
        }
    }
}
