package com.campus.events

import android.app.Application
import android.content.Context
import com.campus.events.data.EventsRepository
import com.campus.events.data.NotificationSettingsRepository
import com.campus.events.data.local.AppDatabase
import com.campus.events.data.remote.FakeEventsApi
import com.campus.events.notifications.EventNotificationHelper
import com.campus.events.notifications.NotificationPermissionChecker
import com.campus.events.notifications.createEventUpdatesChannel
import com.campus.events.work.RefreshEventsWorker

class CampusEventsApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Workshop step 2: create the channel once, at startup, before anything is posted.
        // Re-creating it is safe: Android keeps the user's existing channel settings.
        createEventUpdatesChannel(this)

        // Existing refresh flow: deferrable periodic refresh (not an exact reminder clock).
        RefreshEventsWorker.schedulePeriodic(this)
    }
}

/** Minimal manual dependency container shared by the UI and the worker. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val eventsApi = FakeEventsApi(appContext)
    private val database = AppDatabase.get(appContext)
    val eventsRepository = EventsRepository(eventsApi, database.eventDao())
    val notificationSettings = NotificationSettingsRepository(appContext)
    val permissionChecker = NotificationPermissionChecker(appContext)
    val notificationHelper = EventNotificationHelper(appContext)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as CampusEventsApp).container
