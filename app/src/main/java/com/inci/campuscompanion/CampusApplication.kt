package com.inci.campuscompanion

import android.app.Application
import com.inci.campuscompanion.notifications.EventNotifications

class CampusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EventNotifications.createChannel(this)
    }
}
