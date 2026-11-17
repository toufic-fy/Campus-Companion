package com.inci.campuscompanion.ui.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.worker.RefreshEventWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import com.inci.campuscompanion.CampusApplication

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Success(val events: List<CampusEvent>) : EventsUiState
    data class Error(val message: String) : EventsUiState
}

class CampusEventViewModel(
    private val application: CampusApplication,
    private val workManager: WorkManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()
    val eventUpdatesEnabled = application.preferences.eventUpdatesEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false,
    )

    fun setEventUpdatesEnabled(enabled: Boolean) {
        viewModelScope.launch { application.preferences.setEventUpdatesEnabled(enabled) }
    }

    val workInfo: StateFlow<WorkInfo?> = workManager
        .getWorkInfosForUniqueWorkFlow(RefreshEventWorker.WORK_NAME)
        .map { list -> list.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    init {
        viewModelScope.launch {
            application.eventsRepository.events
                .catch { _uiState.value = EventsUiState.Error("Unable to read cached events") }
                .collect { _uiState.value = EventsUiState.Success(it) }
        }
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            if (_uiState.value !is EventsUiState.Success) {
                _uiState.value = EventsUiState.Loading
            }
            try {
                application.refreshEvents()
                _uiState.value = EventsUiState.Success(application.eventsRepository.getCachedEvents())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep cached events usable when the network is unavailable.
                val cached = (_uiState.value as? EventsUiState.Success)?.events
                if (cached.isNullOrEmpty()) {
                    _uiState.value = EventsUiState.Error(e.localizedMessage ?: "Failed to load events")
                }
            }
        }
    }

    companion object {
        fun Factory(
            application: CampusApplication,
            workManager: WorkManager,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CampusEventViewModel(application, workManager) as T
            }
        }
    }
}
