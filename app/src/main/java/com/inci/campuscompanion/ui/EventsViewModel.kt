package com.inci.campuscompanion.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Content(val events: List<CampusEvent>) : EventsUiState
    data object Empty : EventsUiState
    data class Error(val message: String) : EventsUiState
}

class EventsViewModel(private val repository: EventsRepository) : ViewModel() {
    private val _state = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val state = _state.asStateFlow()
    private var request: Job? = null

    init { refresh() }

    fun refresh() {
        if (request?.isActive == true) return
        _state.value = EventsUiState.Loading
        request = viewModelScope.launch {
            try {
                val events = repository.getEvents()
                _state.value = if (events.isEmpty()) EventsUiState.Empty else EventsUiState.Content(events)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("CampusEvents", "Events request failed: ${error.javaClass.simpleName}")
                _state.value = EventsUiState.Error(errorMessage(error))
            }
        }
    }

    companion object {
        fun errorMessage(error: Exception): String = when (error) {
            is HttpException -> if (error.code() >= 500)
                "The events server is unavailable. Please retry later."
            else "The events request failed (HTTP ${error.code()}). Please retry."
            is JsonParseException, is com.google.gson.stream.MalformedJsonException,
            is IllegalArgumentException, is IllegalStateException -> "The server returned invalid event data. Please retry."
            is IOException -> "Cannot connect. Check your internet connection and retry."
            else -> "Could not load events. Please retry."
        }

        fun factory(repository: EventsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(EventsViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return EventsViewModel(repository) as T
                }
            }
    }
}
