package com.inci.campuscompanion

import android.app.Application
import com.inci.campuscompanion.util.EventNotificationHelper

class CampusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EventNotificationHelper.createNotificationChannel(this)
    }
}
