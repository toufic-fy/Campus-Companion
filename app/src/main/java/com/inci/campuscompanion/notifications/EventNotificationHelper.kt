
package com.inci.campuscompanion.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.inci.campuscompanion.MainActivity
import com.inci.campuscompanion.R
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.local.pref.dataStore
import kotlinx.coroutines.flow.first

object EventNotificationHelper {

    suspend fun showTestNotification(context: Context): Boolean {

        // 1. Check the user's saved preference
        val repository = UserPreferencesRepository(context.dataStore)
        val enabled = repository.eventUpdatesEnabled.first()

        if (!enabled) {
            return false
        }

        // 2. Check Android notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                return false
            }
        }

        // 3. Check whether notifications are blocked system-wide
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false
        }

        // 4. Open Campus Companion when the notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        // 5. Build the notification
        val notification = NotificationCompat.Builder(
            context,
            EVENT_UPDATES_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentTitle("New campus event")
            .setContentText("Android Development Workshop")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // 6. Display the notification
        NotificationManagerCompat.from(context).notify(
            1001,
            notification
        )

        return true
    }
}
