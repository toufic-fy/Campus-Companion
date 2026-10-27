package com.inci.campuscompanion.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EventsUiState {
    object Loading : EventsUiState()
    data class Content(val events: List<CampusEvent>) : EventsUiState()
    data class Error(val message: String) : EventsUiState()
}

class EventsViewModel(private val repository: EventsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        fetchEvents()
    }

    fun fetchEvents() {
        _uiState.value = EventsUiState.Loading
        viewModelScope.launch {
            try {
                val events = repository.getEvents()
                if (events.isEmpty()) {
                    _uiState.value = EventsUiState.Error("No events found.")
                } else {
                    _uiState.value = EventsUiState.Content(events)
                }
            } catch (e: retrofit2.HttpException) {
                _uiState.value = EventsUiState.Error("Failed to fetch events: HTTP ${e.code()}")
            } catch (e: Exception) {
                _uiState.value = EventsUiState.Error("Failed to fetch events: ${e.message}")
            }
        }
    }
}
