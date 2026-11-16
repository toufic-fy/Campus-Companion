package com.inci.campuscompanion.ui.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.worker.RefreshEventWorker
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Success(val events: List<CampusEvent>) : EventsUiState
    data class Error(val message: String) : EventsUiState
}

sealed interface RefreshUi {
    data object Idle : RefreshUi
    data object Queued : RefreshUi
    data object Refreshing : RefreshUi
    data object Current : RefreshUi
    data object Failed : RefreshUi
}

class CampusEventViewModel(
    private val eventsRepository: EventsRepository,
    private val workManager: WorkManager,
) : ViewModel() {
    val refreshStatus: StateFlow<RefreshUi> = workManager
        .getWorkInfosForUniqueWorkFlow(RefreshEventWorker.REFRESH_WORK_NAME)
        .map { list ->
            when (list.firstOrNull()?.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> RefreshUi.Queued
                WorkInfo.State.RUNNING -> RefreshUi.Refreshing
                WorkInfo.State.SUCCEEDED -> RefreshUi.Current
                WorkInfo.State.FAILED -> RefreshUi.Failed
                else -> RefreshUi.Idle
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RefreshUi.Idle,
        )

    val uiState: StateFlow<EventsUiState> = combine(
        eventsRepository.cachedEvents,
        refreshStatus
    ) { events, status ->
        when {
            events.isNotEmpty() -> EventsUiState.Success(events)
            status == RefreshUi.Current -> EventsUiState.Success(emptyList())
            status == RefreshUi.Failed -> EventsUiState.Error("Could not refresh events")
            else -> EventsUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EventsUiState.Loading,
    )

    init {
        refreshEvents()
    }

    fun refreshEvents() {
        RefreshEventWorker.enqueueUniqueWork(workManager)
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
