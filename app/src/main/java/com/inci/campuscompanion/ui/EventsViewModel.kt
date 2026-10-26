package com.inci.campuscompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import java.io.IOException
import java.util.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Content(val events: List<CampusEvent>) : EventsUiState
    data object Empty : EventsUiState
    data class Error(val message: String) : EventsUiState
}

class EventsViewModel(private val repository: EventsRepository) : ViewModel() {
    private val _state = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val state = _state.asStateFlow()
    private var refreshJob: Job? = null

    init { refresh() }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.value = EventsUiState.Loading
            try {
                val events = repository.getEvents()
                _state.value = if (events.isEmpty()) EventsUiState.Empty else EventsUiState.Content(events)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Log the category only; never log response bodies or credentials.
                Logger.getLogger("CampusEvents").warning("Events request failed: ${error.javaClass.simpleName}")
                _state.value = EventsUiState.Error(when (error) {
                    is HttpException -> if (error.code() >= 500)
                        "The events server is unavailable. Please try again later."
                    else "The events request failed (HTTP ${error.code()}). Please retry."
                    is SerializationException, is IllegalArgumentException ->
                        "The server returned invalid event data. Please retry."
                    is IOException -> "Unable to connect. Check your connection and retry."
                    else -> "Unable to load events. Please retry."
                })
            }
        }
    }

    class Factory(private val repository: EventsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(EventsViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return EventsViewModel(repository) as T
        }
    }
}
