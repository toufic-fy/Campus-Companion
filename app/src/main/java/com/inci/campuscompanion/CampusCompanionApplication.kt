
package com.inci.campuscompanion

import android.app.Application
import android.app.NotificationManager
import android.util.Log
import com.inci.campuscompanion.notifications.EVENT_UPDATES_CHANNEL_ID
import com.inci.campuscompanion.notifications.createEventUpdatesChannel

class CampusCompanionApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        createEventUpdatesChannel(this)

        val manager = getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(EVENT_UPDATES_CHANNEL_ID)

        Log.d("NotificationTest", "Channel created: ${channel != null}")
    }
}
