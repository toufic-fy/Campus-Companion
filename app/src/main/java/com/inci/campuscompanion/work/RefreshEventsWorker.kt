package com.inci.campuscompanion.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.notifications.EventNotificationHelper
import com.inci.campuscompanion.notifications.NotificationPermissionChecker
import com.inci.campuscompanion.notifications.NotificationPolicy
import com.inci.campuscompanion.notifications.NotificationSettingsRepository
import kotlinx.coroutines.flow.first
import java.io.IOException

class RefreshEventsWorker(
    appContext: Context,
    params: WorkerParameters,
    private val repository: EventsRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Get cached events before fetching new ones
            val cachedEvents = repository.events.value

            // 2. Fetch fresh events from remote
            val remoteEvents = repository.refreshEvents()

            // 3. Read settings and permission state
            val settingsRepo = NotificationSettingsRepository(applicationContext)
            val settings = settingsRepo.settings.first()
            val permissionChecker = NotificationPermissionChecker(applicationContext)
            val permissionGranted = permissionChecker.isGranted()

            // 4. Determine if there are new events
            val newEvents = NotificationPolicy.findNewEvents(cachedEvents, remoteEvents)

            // 5. Post notifications if policy criteria are met
            if (NotificationPolicy.shouldNotify(settings, permissionGranted, newEvents)) {
                val helper = EventNotificationHelper(applicationContext)
                newEvents.forEach { event ->
                    helper.show(event)
                }
            }

            Result.success()
        } catch (error: IOException) {
            Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
