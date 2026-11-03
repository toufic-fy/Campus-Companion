package com.inci.campuscompanion.ui.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.work.RefreshEventsWorker
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

enum class RefreshUi {
    Idle,
    Queued,
    Refreshing,
    Current,
    Failed
}

class CampusEventViewModel(
    eventsRepository: EventsRepository,
    private val workManager: WorkManager
) : ViewModel() {

    val refreshStatus: StateFlow<RefreshUi> = workManager
        .getWorkInfosForUniqueWorkFlow(REFRESH_WORK_NAME)
        .map { infos -> infos.firstOrNull()?.state.toRefreshUi() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RefreshUi.Idle)

    val uiState: StateFlow<EventsUiState> = combine(
        eventsRepository.cachedEvents,
        refreshStatus
    ) { events, status ->
        when {
            events.isNotEmpty() -> EventsUiState.Success(events)
            status == RefreshUi.Failed -> EventsUiState.Error("The events could not be refreshed.")
            status == RefreshUi.Current -> EventsUiState.Success(emptyList())
            else -> EventsUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsUiState.Loading)

    init {
        refreshEvents()
    }

    fun refreshEvents() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<RefreshEventsWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            REFRESH_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun WorkInfo.State?.toRefreshUi(): RefreshUi = when (this) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> RefreshUi.Queued
        WorkInfo.State.RUNNING -> RefreshUi.Refreshing
        WorkInfo.State.SUCCEEDED -> RefreshUi.Current
        WorkInfo.State.FAILED -> RefreshUi.Failed
        WorkInfo.State.CANCELLED, null -> RefreshUi.Idle
    }

    companion object {
        const val REFRESH_WORK_NAME = "refresh-campus-events"
    }
}
