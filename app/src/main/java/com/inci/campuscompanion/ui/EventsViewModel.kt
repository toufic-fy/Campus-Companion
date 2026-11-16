package com.inci.campuscompanion.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.inci.campuscompanion.data.AppDatabase
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.worker.RefreshEventsWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// What the refresh work is doing right now, shown as a line on the Events screen.
enum class RefreshStatus(val label: String) {
    Idle(""),
    Waiting("Refresh queued, waiting for a network connection…"),
    Running("Refreshing events…"),
    UpToDate("Events are up to date"),
    Failed("Couldn't refresh events. Showing saved events.")
}

class EventsViewModel(application: Application) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)

    private val repository = EventsRepository(
        eventDao = AppDatabase.getDatabase(application).eventDao()
    )

    // Cached events from Room: available offline and while the work waits.
    val events: StateFlow<List<CampusEvent>> = repository.cachedEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Map the unique work's state to a simple status for the UI.
    val refreshStatus: StateFlow<RefreshStatus> = workManager
        .getWorkInfosForUniqueWorkFlow(RefreshEventsWorker.UNIQUE_WORK_NAME)
        .map { infos -> infos.firstOrNull()?.state.toRefreshStatus() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RefreshStatus.Idle
        )

    init {
        refresh()
    }

    fun refresh() {
        val request = OneTimeWorkRequestBuilder<RefreshEventsWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        // KEEP: if a refresh is already queued or running, tapping Refresh again
        // does not add a duplicate.
        workManager.enqueueUniqueWork(
            RefreshEventsWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun WorkInfo.State?.toRefreshStatus(): RefreshStatus = when (this) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> RefreshStatus.Waiting
        WorkInfo.State.RUNNING -> RefreshStatus.Running
        WorkInfo.State.SUCCEEDED -> RefreshStatus.UpToDate
        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> RefreshStatus.Failed
        null -> RefreshStatus.Idle
    }
}
