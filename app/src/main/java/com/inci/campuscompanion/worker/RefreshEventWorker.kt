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
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.local.pref.dataStore
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.notifications.EventNotifications
import com.inci.campuscompanion.notifications.EventSnapshotStore
import com.inci.campuscompanion.notifications.findMeaningfulChanges
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
            val freshEvents = repository.refreshEvents()
            notifyMeaningfulChanges(freshEvents)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    /**
     * Step 6: connect the refresh result to notifications.
     * Notify only when an event changed in a meaningful way AND the user turned
     * notifications on. Nothing is sent on the very first refresh (no "before" yet).
     */
    private suspend fun notifyMeaningfulChanges(freshEvents: List<CampusEvent>) {
        val store = EventSnapshotStore(applicationContext)
        val previous = store.load()
        store.save(freshEvents) // remember for next time

        if (previous == null) return // first refresh: just remember, don't notify

        val changes = findMeaningfulChanges(previous, freshEvents)
        if (changes.isEmpty()) return

        val preferences = UserPreferencesRepository(applicationContext.dataStore)
        if (!preferences.eventNotificationsEnabled.first()) return

        changes.forEach { change ->
            EventNotifications.showEventUpdate(
                context = applicationContext,
                title = "${change.title} updated",
                text = change.summary,
                // same event -> same id, so a newer update replaces the older one
                notificationId = EVENT_NOTIFICATION_BASE_ID + change.eventId.hashCode().mod(1000)
            )
        }
    }

    companion object {
        private const val EVENT_NOTIFICATION_BASE_ID = 2000
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
