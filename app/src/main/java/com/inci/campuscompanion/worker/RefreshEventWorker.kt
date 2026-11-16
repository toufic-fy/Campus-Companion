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
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.local.pref.dataStore
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.notification.EventNotificationHelper
import com.inci.campuscompanion.notification.NotificationPolicy
import com.inci.campuscompanion.notification.diffEvents
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
            notifyAboutChanges(events)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun notifyAboutChanges(events: List<CampusEvent>) {
        val preferences = UserPreferencesRepository(applicationContext.dataStore)
        val previous = preferences.getEventSnapshot()
        val diff = diffEvents(previous, events)
        preferences.saveEventSnapshot(diff.snapshot)

        if (previous == null) return
        val policy = NotificationPolicy(applicationContext)
        if (!policy.canNotify(preferences.eventUpdatesEnabled.first())) return

        val helper = EventNotificationHelper(applicationContext)
        helper.notifyNewEvents(diff.newEvents)
        helper.notifyChangedEvents(diff.changedEvents)
    }

    companion object {
        const val WORK_NAME = "RefreshEventWorker"

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
    }
}
