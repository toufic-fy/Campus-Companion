package com.inci.campuscompanion.ui.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Success(val events: List<CampusEvent>) : EventsUiState
    data class Error(val message: String) : EventsUiState
}

class CampusEventViewModel(
    private val eventsRepository: EventsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

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
}
