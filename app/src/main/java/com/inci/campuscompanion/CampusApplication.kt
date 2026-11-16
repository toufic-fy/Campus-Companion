package com.inci.campuscompanion

import android.app.Application
import com.inci.campuscompanion.notification.NotificationChannels

class CampusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createEventUpdatesChannel(this)
    }
}