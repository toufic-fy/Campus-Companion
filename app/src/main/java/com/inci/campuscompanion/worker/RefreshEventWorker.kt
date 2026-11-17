package com.inci.campuscompanion.worker

import android.content.Context
import android.util.Log
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
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository
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
            repository.refreshEvents()
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Events refresh failed (attempt ${runAttemptCount + 1})", e)
            resultFor(e)
        }
    }

    companion object {
        private const val TAG = "RefreshEventWorker"

        /** One-time refresh requested from the Events screen. */
        const val WORK_NAME = "refresh-campus-events"

        /** Background refresh scheduled when the app starts. */
        const val PERIODIC_WORK_NAME = "refresh-campus-events-periodic"

        /** Temporary problems are retried with backoff; anything else will not fix itself. */
        fun resultFor(error: Exception): Result = when {
            error is IOException -> Result.retry()
            error is HttpException && error.code() >= 500 -> Result.retry()
            else -> Result.failure()
        }

        private val networkConstraints: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        private val periodicConstraints: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
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
                .setConstraints(periodicConstraints)
                .build()
        }

        // KEEP: if a refresh is already pending or running, extra taps do not queue duplicates.
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
                PERIODIC_WORK_NAME,
                policy,
                buildPeriodicWorkRequest(repeatInterval, timeUnit),
            )
        }
    }
}
