package com.inci.campuscompanion.work

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import java.util.concurrent.TimeUnit
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.map

enum class RefreshStatus { Idle, Queued, Refreshing, Current, Failed }

data class RefreshState(
    val status: RefreshStatus = RefreshStatus.Idle,
    val errorMessage: String? = null,
    val requestId: String? = null
)

class EventsRefreshScheduler(private val workManager: WorkManager) {
    val state = workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME).map { infos ->
        val info = infos.firstOrNull { !it.state.isFinished } ?: infos.firstOrNull()
        RefreshState(
            status = when (info?.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> RefreshStatus.Queued
                WorkInfo.State.RUNNING -> RefreshStatus.Refreshing
                WorkInfo.State.SUCCEEDED -> RefreshStatus.Current
                WorkInfo.State.FAILED -> RefreshStatus.Failed
                else -> RefreshStatus.Idle
            },
            errorMessage = info?.outputData?.getString(RefreshEventsWorker.ERROR_MESSAGE),
            requestId = info?.id?.toString()
        )
    }

    fun refresh() = workManager.enqueueUniqueWork(
        WORK_NAME,
        ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<RefreshEventsWorker>()
            .setConstraints(networkConstraint)
            .build()
    )

    /**
     * Refreshes events in the background so new-event notifications arrive while the app is closed.
     * KEEP leaves an existing schedule alone, so calling this on every launch is safe.
     * Kept under its own name so the on-screen refresh status only tracks manual refreshes.
     */
    fun schedulePeriodicRefresh() = workManager.enqueueUniquePeriodicWork(
        PERIODIC_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<RefreshEventsWorker>(PERIODIC_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .build()
    )

    private val networkConstraint
        get() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    companion object {
        const val WORK_NAME = "refresh-campus-events"
        const val PERIODIC_WORK_NAME = "periodic-refresh-campus-events"
        /** WorkManager's minimum periodic interval. */
        const val PERIODIC_INTERVAL_MINUTES = 15L
    }
}
