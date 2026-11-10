package com.inci.campuscompanion.ui.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.worker.RefreshEventsWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Success(val events: List<CampusEvent>) : EventsUiState
    data class Error(val message: String) : EventsUiState
}

enum class RefreshStatus { IDLE, WAITING, RUNNING, FAILED }

class CampusEventViewModel(
    private val eventsRepository: EventsRepository,
    private val workManager: WorkManager
) : ViewModel() {
    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    // True only after the user taps Refresh, so an old finished job
    // from a previous session does not trigger a reload.
    private var reloadWhenWorkSucceeds = false

    val refreshStatus: StateFlow<RefreshStatus> = workManager
        .getWorkInfosForUniqueWorkFlow(RefreshEventsWorker.MANUAL_WORK_NAME)
        .map { infos -> infos.firstOrNull() }
        .onEach { info ->
            if (reloadWhenWorkSucceeds && info?.state == WorkInfo.State.SUCCEEDED) {
                reloadWhenWorkSucceeds = false
                loadEvents()
            }
        }
        .map { info -> info.toRefreshStatus() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = RefreshStatus.IDLE
        )

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = EventsUiState.Loading
            try {
                val events = eventsRepository.getEvents()
                _uiState.value = EventsUiState.Success(events)
            } catch (e: Exception) {
                _uiState.value = EventsUiState.Error(e.localizedMessage ?: "Failed to load events")
            }
        }
    }

    fun refreshInBackground() {
        reloadWhenWorkSucceeds = true
        RefreshEventsWorker.refreshNow(workManager)
    }

    companion object {
        fun Factory(
            eventsRepository: EventsRepository,
            workManager: WorkManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CampusEventViewModel(eventsRepository, workManager) as T
            }
        }
    }
}

private fun WorkInfo?.toRefreshStatus(): RefreshStatus = when (this?.state) {
    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> RefreshStatus.WAITING
    WorkInfo.State.RUNNING -> RefreshStatus.RUNNING
    WorkInfo.State.FAILED -> RefreshStatus.FAILED
    else -> RefreshStatus.IDLE
}