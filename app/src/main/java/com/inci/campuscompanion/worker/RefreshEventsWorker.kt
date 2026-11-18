package com.inci.campuscompanion.worker

import android.content.Context
import androidx.work.*
import com.inci.campuscompanion.CampusApplication
import com.inci.campuscompanion.data.InvalidEventDataException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

class RefreshEventsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            (applicationContext as CampusApplication).graph.events.getEvents()
            Result.success()
        } catch (_: IOException) {
            retryOrFail()
        } catch (error: HttpException) {
            if (error.code() == 429 || error.code() >= 500) retryOrFail() else Result.failure()
        } catch (_: SerializationException) {
            Result.failure()
        } catch (_: InvalidEventDataException) {
            Result.failure()
        }
    }

    private fun retryOrFail() = if (runAttemptCount < 4) Result.retry() else Result.failure()

    companion object {
        const val PERIODIC_NAME = "periodic-events-refresh"
        const val MANUAL_NAME = "manual-events-refresh"
        private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshEventsWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun refreshNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                MANUAL_NAME, ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<RefreshEventsWorker>().setConstraints(network)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
            )
        }
    }
}
