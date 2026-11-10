
package com.inci.campuscompanion.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository
import java.io.IOException

class RefreshEventsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        val database = AppDatabase.getDatabase(
            applicationContext
        )

        val eventDao = database.eventDao()

        val remoteDataSource = EventsRemoteDataSource(
            RetrofitClient.campusApi
        )

        val repository = EventsRepository(
            remoteDataSource,
            eventDao
        )

        return try {
            repository.refreshEvents()
            Result.success()

        } catch (e: IOException) {
            Result.retry()

        } catch (e: Exception) {
            Result.failure()
        }
    }
}
