package com.inci.campuscompanion

import android.app.Application
import com.inci.campuscompanion.notification.createEventUpdatesChannel

class CampusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createEventUpdatesChannel(this)
    }
}
