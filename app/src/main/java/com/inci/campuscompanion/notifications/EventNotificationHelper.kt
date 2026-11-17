package com.inci.campuscompanion.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.inci.campuscompanion.MainActivity
import com.inci.campuscompanion.R
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.first

class EventNotificationHelper(
    private val context: Context,
    private val preferences: UserPreferencesRepository,
) {
    fun systemAllowsNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(EVENT_UPDATES_CHANNEL_ID)?.importance !=
            NotificationManager.IMPORTANCE_NONE
    }

    // Both test and real notifications pass through the same preference/permission gate.
    @SuppressLint("MissingPermission") // Checked by systemAllowsNotifications; revocation is caught below.
    suspend fun show(event: CampusEvent, isTest: Boolean = false): Boolean {
        if (!canNotify(preferences.eventUpdatesEnabled.first(), systemAllowsNotifications())) {
            return false
        }
        val intent = Intent(
            Intent.ACTION_VIEW,
            "campus://events/${android.net.Uri.encode(event.id)}".toUri(),
            context,
            MainActivity::class.java,
        )
        val pendingIntent = PendingIntent.getActivity(
            context, event.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, EVENT_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isTest) "Event updates test" else "New campus event")
            .setContentText(event.title)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        return try {
            // Permission can be revoked between the check above and posting.
            NotificationManagerCompat.from(context).notify(event.id.hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    suspend fun showTest(): Boolean = show(
        CampusEvent("notification-test", "Tap to open Campus Events", "", "", timeLabel = ""),
        isTest = true,
    )
}
