package com.inci.campuscompanion.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.inci.campuscompanion.data.repository.EventsRepository

class RefreshEventsWorkerFactory(
    private val repository: EventsRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            RefreshEventsWorker::class.java.name ->
                RefreshEventsWorker(appContext, workerParameters, repository)

            else -> null
        }
    }
}
