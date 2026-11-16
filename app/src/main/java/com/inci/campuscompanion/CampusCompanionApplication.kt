
package com.inci.campuscompanion

import android.app.Application
import com.inci.campuscompanion.notifications.createEventUpdatesChannel

class CampusCompanionApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        createEventUpdatesChannel(this)
    }
}
