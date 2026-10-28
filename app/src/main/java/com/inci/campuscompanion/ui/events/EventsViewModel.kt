package com.inci.campuscompanion.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.repository.CampusEvent
import com.inci.campuscompanion.data.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import retrofit2.HttpException

sealed interface EventsUiState {
    object Loading : EventsUiState
    data class Content(val events: List<CampusEvent>) : EventsUiState
    object Empty : EventsUiState
    data class Error(val message: String, val isRetryable: Boolean) : EventsUiState
}

class EventsViewModel(private val repository: EventsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState

    init {
        fetchEvents()
    }

    fun fetchEvents() {
        viewModelScope.launch {
            _uiState.value = EventsUiState.Loading
            try {
                val events = repository.getEvents()
                _uiState.value = if (events.isEmpty()) {
                    EventsUiState.Empty
                } else {
                    EventsUiState.Content(events)
                }
            } catch (_: IOException) {
                _uiState.value = EventsUiState.Error("Network error. Check connection.", isRetryable = true)
            } catch (e: HttpException) {
                _uiState.value = EventsUiState.Error("Server error: ${e.code()}", isRetryable = true)
            } catch (_: Exception) {
                _uiState.value = EventsUiState.Error("Safe generic error.", isRetryable = false)
            }
        }
    }

    companion object {
        fun Factory(
            eventsRepository: EventsRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EventsViewModel(eventsRepository) as T
                }
            }
    }
}