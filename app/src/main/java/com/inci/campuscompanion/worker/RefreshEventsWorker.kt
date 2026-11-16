
package com.inci.campuscompanion.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.inci.campuscompanion.data.AppDatabase
import com.inci.campuscompanion.data.remote.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

class RefreshEventsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        val database = AppDatabase.getDatabase(applicationContext)

        val repository = EventsRepository(
            EventsRemoteDataSource(RetrofitClient.api),
            database.eventDao()
        )

        return try {
            repository.refreshEvents()
            Result.success()

        } catch (error: CancellationException) {
            throw error

        } catch (error: IOException) {
            Result.retry()

        } catch (error: HttpException) {
            if (error.code() == 429 || error.code() >= 500) {
                Result.retry()
            } else {
                Result.failure()
            }

        } catch (error: Exception) {
            Result.failure()
        }
    }
}

object EventsRefreshWork {

    const val UNIQUE_NAME = "refresh-campus-events"

    fun enqueue(workManager: WorkManager) {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<RefreshEventsWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
