package com.example.navigationworkshop

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result

class RefreshEventsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = EventsRepository()

            repository.refreshEvents()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}