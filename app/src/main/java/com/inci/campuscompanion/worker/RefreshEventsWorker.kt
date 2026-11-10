package com.inci.campuscompanion.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository
import java.util.concurrent.TimeUnit

class RefreshEventsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = EventsRepository(EventsRemoteDataSource(RetrofitClient.campusApi))
            val events = repository.getEvents()
            Log.d(TAG, "Refreshed ${events.size} events")
            Result.success(workDataOf(KEY_EVENT_COUNT to events.size))
        } catch (e: Exception) {
            Log.w(TAG, "Refresh failed (attempt ${runAttemptCount + 1})", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "RefreshEventsWorker"
        private const val MAX_RETRIES = 3
        const val KEY_EVENT_COUNT = "event_count"

        // Two different unique names, so the button's one-time work
        // and the hourly periodic work never collide.
        const val MANUAL_WORK_NAME = "refresh_events_now"
        const val PERIODIC_WORK_NAME = "refresh_events_periodic"

        // Called by the Refresh button.
        // KEEP: if a refresh is already waiting or running, tapping again does nothing.
        fun refreshNow(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<RefreshEventsWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            workManager.enqueueUniqueWork(
                MANUAL_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        // Called once when the app starts.
        // KEEP: launching the app again does not create a second periodic job.
        fun schedulePeriodicRefresh(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<RefreshEventsWorker>(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}