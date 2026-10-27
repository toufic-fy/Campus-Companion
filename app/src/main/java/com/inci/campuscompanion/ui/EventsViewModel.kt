package com.inci.campuscompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.EventsSource
import com.inci.campuscompanion.data.InvalidEventDataException
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.util.logging.Logger

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Content(val events: List<CampusEvent>) : EventsUiState
    data object Empty : EventsUiState
    data class Error(val message: String) : EventsUiState
}

class EventsViewModel(
    private val repository: EventsSource,
    private val logger: Logger = Logger.getLogger("CampusEvents")
) : ViewModel() {
    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { refresh() }

    fun refresh() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.value = EventsUiState.Loading
            try {
                val events = repository.getEvents()
                _uiState.value = if (events.isEmpty()) EventsUiState.Empty else EventsUiState.Content(events)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Log the failure type, never response bodies, URLs, or personal data.
                logger.warning("Events request failed: ${error.javaClass.simpleName}")
                _uiState.value = EventsUiState.Error(errorMessage(error))
            }
        }
    }

    companion object {
        fun Factory(repository: EventsSource): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(EventsViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return EventsViewModel(repository) as T
                }
            }
    }
}

internal fun errorMessage(error: Exception): String = when (error) {
    is HttpException -> if (error.code() >= 500) {
        "The server is unavailable. Please try again later."
    } else {
        "The events request was rejected (HTTP ${error.code()}). Please try again."
    }
    is SerializationException, is InvalidEventDataException ->
        "The server returned invalid event data. Please try again later."
    is IOException -> "Unable to connect. Check your internet connection and retry."
    else -> "Unable to load events. Please try again."
}
