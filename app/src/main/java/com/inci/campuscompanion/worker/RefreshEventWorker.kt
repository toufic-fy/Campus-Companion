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
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.local.pref.dataStore
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.notification.EventNotificationHelper
import com.inci.campuscompanion.notification.EventUpdateNotifier
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

class RefreshEventWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = EventsRepository(
            EventsRemoteDataSource(RetrofitClient.campusApi),
            AppDatabase.getDatabase(applicationContext).eventDao(),
        )
        return try {
            val events = repository.refreshEvents()
            EventUpdateNotifier(
                UserPreferencesRepository(applicationContext.dataStore),
                EventNotificationHelper(applicationContext),
            ).onEventsRefreshed(events)
            Result.success()
        } catch (e: IOException) {
            Result.retry()
        } catch (e: HttpException) {
            if (e.code() >= 500) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "RefreshEventWorker"
        const val REFRESH_WORK_NAME = "refresh-campus-events"

        val constraints: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        private val networkConstraints: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun buildOneTimeWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<RefreshEventWorker>()
                .setConstraints(networkConstraints)
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
            workManager: WorkManager,
            policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
        ) {
            workManager.enqueueUniqueWork(
                REFRESH_WORK_NAME,
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
