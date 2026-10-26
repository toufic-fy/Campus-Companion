
package com.inci.campuscompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.data.remote.CampusApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventsViewModel : ViewModel() {

    private val repository = EventsRepository(
        CampusApi.create()
    )

    private val pageSize = 5

    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = EventsUiState.Loading

            try {
                val events = repository.getEvents(page = 1, limit = pageSize)

                _uiState.value = EventsUiState.Content(
                    events = events,
                    page = 1,
                    hasReachedEnd = events.size < pageSize
                )
            } catch (e: Exception) {
                _uiState.value = EventsUiState.Error(
                    e.message ?: "Unable to load events."
                )
            }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value as? EventsUiState.Content ?: return
        if (currentState.isLoadingMore || currentState.hasReachedEnd) return

        _uiState.value = currentState.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                val nextPage = currentState.page + 1
                val newEvents = repository.getEvents(page = nextPage, limit = pageSize)

                if (newEvents.isEmpty()) {
                    _uiState.value = currentState.copy(
                        isLoadingMore = false,
                        hasReachedEnd = true
                    )
                } else {
                    _uiState.value = currentState.copy(
                        events = currentState.events + newEvents,
                        page = nextPage,
                        isLoadingMore = false,
                        hasReachedEnd = newEvents.size < pageSize
                    )
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(isLoadingMore = false)
            }
        }
    }
}
