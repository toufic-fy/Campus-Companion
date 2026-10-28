
package com.inci.campuscompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EventsUiState {
    data object Loading : EventsUiState

    data class Content(
        val events: List<CampusEvent>
    ) : EventsUiState

    data class Error(
        val message: String
    ) : EventsUiState
}

class EventsViewModel(
    private val repository: EventsRepository = EventsRepository()
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<EventsUiState>(EventsUiState.Loading)

    val uiState: StateFlow<EventsUiState> =
        _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = EventsUiState.Loading

            try {
                val events = repository.getEvents()

                _uiState.value = if (events.isEmpty()) {
                    EventsUiState.Error(
                        "No campus events are available right now."
                    )
                } else {
                    EventsUiState.Content(events)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = EventsUiState.Error(
                    "Couldn't load campus events. Check your connection and try again."
                )
            }
        }
    }
}
