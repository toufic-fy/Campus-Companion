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
import com.inci.campuscompanion.data.repository.EventsRepository
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
            repository.refreshEvents()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
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
