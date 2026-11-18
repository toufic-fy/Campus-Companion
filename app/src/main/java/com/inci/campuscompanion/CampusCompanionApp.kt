package com.inci.campuscompanion

import com.inci.campuscompanion.data.repository.EventsRepository
import android.app.Application
import androidx.work.Configuration
import com.inci.campuscompanion.data.network.CampusApi
import com.inci.campuscompanion.data.network.EventsRemoteDataSource
import com.inci.campuscompanion.notifications.NotificationPermissionChecker
import com.inci.campuscompanion.work.RefreshEventsWorkerFactory
import retrofit2.Retrofit
import com.inci.campuscompanion.notifications.createEventUpdatesChannel
import com.inci.campuscompanion.notifications.NotificationSettingsRepository
class CampusCompanionApp : Application(), Configuration.Provider {

    // the repository lives here so the Activity, ViewModels and the Worker all share one instance
    val eventsRepository: EventsRepository by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/event/")   // GUESS 1: copy from your existing Retrofit setup
            .addConverterFactory(RefreshEventsWorkerFactory.create())   // GUESS 2: use whatever converter you already use
            .build()
        val api = retrofit.create(CampusApi::class.java)
        EventsRepository(EventsRemoteDataSource(api))   // GUESS 3: match your real constructor
    }
    val notificationSettingsRepository by lazy {
        NotificationSettingsRepository(this)
    }
    val notificationPermissionChecker by lazy {
        NotificationPermissionChecker(this)
    }
    override fun onCreate() {
        super.onCreate()
        createEventUpdatesChannel(this)
    }
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(RefreshEventsWorkerFactory(eventsRepository))
            .build()
}