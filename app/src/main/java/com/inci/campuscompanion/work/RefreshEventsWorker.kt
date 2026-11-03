package com.inci.campuscompanion.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository

class RefreshEventsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = EventsRepository(EventsRemoteDataSource(RetrofitClient.campusApi))

    override suspend fun doWork(): Result {
        return try {
            repository.getEvents()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
