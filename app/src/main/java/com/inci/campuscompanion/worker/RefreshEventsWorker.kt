package com.inci.campuscompanion.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.data.remote.CampusApi

class RefreshEventsWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val repository = EventsRepository(CampusApi.create())

    override suspend fun doWork(): Result {
        return try {
            val events = repository.getEvents(page = 1)
            if (events.isNotEmpty()) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
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
