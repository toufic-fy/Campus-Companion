package com.inci.campuscompanion

import android.app.Application
import com.inci.campuscompanion.notification.createEventUpdatesChannel

class CampusCompanionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // create the channel at startup
        createEventUpdatesChannel(this)
    }
}
