package com.inci.campuscompanion.ui.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.worker.RefreshEventWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Success(val events: List<CampusEvent>) : EventsUiState
    data class Error(val message: String) : EventsUiState
}

/** What the status line under the top bar tells the student about the last refresh request. */
enum class RefreshStatus(val label: String) {
    WaitingForNetwork("Waiting for network"),
    Refreshing("Refreshing…"),
    UpToDate("Up to date"),
    Failed("Refresh failed"),
}

fun refreshStatusOf(state: WorkInfo.State?): RefreshStatus? = when (state) {
    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> RefreshStatus.WaitingForNetwork
    WorkInfo.State.RUNNING -> RefreshStatus.Refreshing
    WorkInfo.State.SUCCEEDED -> RefreshStatus.UpToDate
    WorkInfo.State.FAILED -> RefreshStatus.Failed
    WorkInfo.State.CANCELLED, null -> null
}

/**
 * Cached events are always shown when there are any. An empty cache only means
 * "no events" once a refresh has finished; before that it means we are still loading.
 */
fun eventsUiStateOf(cached: List<CampusEvent>, status: RefreshStatus?): EventsUiState = when {
    cached.isNotEmpty() -> EventsUiState.Success(cached)
    status == RefreshStatus.Failed -> EventsUiState.Error("Could not refresh events. Please retry.")
    status == RefreshStatus.UpToDate -> EventsUiState.Success(emptyList())
    else -> EventsUiState.Loading
}

class CampusEventViewModel(
    eventsRepository: EventsRepository,
    private val workManager: WorkManager,
) : ViewModel() {

    val refreshStatus: StateFlow<RefreshStatus?> = workManager
        .getWorkInfosForUniqueWorkFlow(RefreshEventWorker.WORK_NAME)
        .map { list -> refreshStatusOf(list.firstOrNull()?.state) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val uiState: StateFlow<EventsUiState> = combine(
        eventsRepository.observeEvents(),
        refreshStatus,
        ::eventsUiStateOf,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EventsUiState.Loading,
    )

    init {
        refresh()
    }

    fun refresh() {
        workManager.enqueueUniqueWork(
            RefreshEventWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            RefreshEventWorker.buildOneTimeWorkRequest(),
        )
    }

    companion object {
        fun Factory(
            eventsRepository: EventsRepository,
            workManager: WorkManager,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CampusEventViewModel(eventsRepository, workManager) as T
            }
        }
    }
}
