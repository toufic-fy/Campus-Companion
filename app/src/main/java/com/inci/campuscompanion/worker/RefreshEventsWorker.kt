package com.inci.campuscompanion.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.inci.campuscompanion.data.AppDatabase
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.data.UserPreferencesRepository
import com.inci.campuscompanion.data.dataStore
import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.util.EventNotificationHelper
import kotlinx.coroutines.flow.first

class RefreshEventsWorker(
    private val appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val TAG = "EventNotification"
    private val database = AppDatabase.getDatabase(appContext)
    private val repository = EventsRepository(CampusApi.create(), database.eventDao())
    private val userPreferencesRepository = UserPreferencesRepository(appContext.dataStore)

    override suspend fun doWork(): Result {
        return try {
            val userEnabledNotifications = userPreferencesRepository.isEventNotificationsEnabled.first()
            val hasSystemPermission = EventNotificationHelper.hasNotificationPermission(appContext)

            Log.d(
                TAG,
                "RefreshEventsWorker.doWork() -> preference state (userEnabledNotifications): $userEnabledNotifications, permission state (hasSystemPermission): $hasSystemPermission"
            )

            val updatedEvents = repository.refreshAndDetectUpdates(page = 1, limit = 10)
            Log.d(TAG, "RefreshEventsWorker.doWork() -> number of new events found: ${updatedEvents.size}")

            for (event in updatedEvents) {
                EventNotificationHelper.show(
                    context = appContext,
                    event = event,
                    eventUpdatesEnabled = userEnabledNotifications
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "RefreshEventsWorker failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "RefreshEventsWork"

        fun buildWorkRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<RefreshEventsWorker>()
                .setConstraints(constraints)
                .build()
        }
    }
}
