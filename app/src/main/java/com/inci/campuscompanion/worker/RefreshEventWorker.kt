package com.inci.campuscompanion.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.local.pref.dataStore
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.notifications.EventNotificationHelper
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class RefreshEventWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = EventsRepository(
                EventsRemoteDataSource(RetrofitClient.campusApi),
            )
            val events = repository.refreshEvents()

            val preferences = UserPreferencesRepository(applicationContext.dataStore)
            var knownIds = preferences.knownEventIds.first()

            // Test only: pretend the first event was never seen before.
            if (inputData.getBoolean(KEY_SIMULATE_NEW_EVENT, false) && events.isNotEmpty()) {
                knownIds = events.map { it.id }.toSet() - events.first().id
            }

            // First refresh ever: just remember the events, don't notify about all of them.
            val newEvents = if (knownIds == null) emptyList() else events.filter { it.id !in knownIds }
            preferences.saveKnownEventIds(events.map { it.id }.toSet())

            // Notify only for a meaningful change, and only if BOTH
            // the user preference and the system permission allow it.
            val userWantsUpdates = preferences.eventUpdatesEnabled.first()
            if (newEvents.isNotEmpty() &&
                userWantsUpdates &&
                EventNotificationHelper.canPostNotifications(applicationContext)
            ) {
                EventNotificationHelper.showNewEvents(applicationContext, newEvents)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "RefreshEventWorker"
        private const val SIMULATION_WORK_NAME = "RefreshEventWorkerSimulation"
        private const val KEY_SIMULATE_NEW_EVENT = "simulate_new_event"

        val constraints: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        fun buildOneTimeWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<RefreshEventWorker>()
                .setConstraints(constraints)
                .build()
        }

        fun buildPeriodicWorkRequest(
            repeatInterval: Long = 1,
            timeUnit: TimeUnit = TimeUnit.HOURS,
        ): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<RefreshEventWorker>(repeatInterval, timeUnit)
                .setConstraints(constraints)
                .build()
        }

        fun enqueueUniqueWork(
            context: Context,
            policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
        ) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                policy,
                buildOneTimeWorkRequest(),
            )
        }

        fun enqueueUniquePeriodicWork(
            context: Context,
            repeatInterval: Long = 1,
            timeUnit: TimeUnit = TimeUnit.HOURS,
            policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
        ) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                policy,
                buildPeriodicWorkRequest(repeatInterval, timeUnit),
            )
        }

        // Test button: runs one refresh that treats one event as new.
        fun enqueueSimulatedNewEvent(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshEventWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(workDataOf(KEY_SIMULATE_NEW_EVENT to true))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                SIMULATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}