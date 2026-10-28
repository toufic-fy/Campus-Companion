package com.inci.campuscompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Content(val events: List<CampusEvent>) : EventsUiState
    data object Empty : EventsUiState
    data class Error(val message: String) : EventsUiState
}

class EventsViewModel(
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = EventsUiState.Loading
        viewModelScope.launch {
            _uiState.value = eventsRepository.getEvents().fold(
                onSuccess = { if (it.isEmpty()) EventsUiState.Empty else EventsUiState.Content(it) },
                onFailure = { EventsUiState.Error(it.toUserMessage()) }
            )
        }
    }

    // Maps a failure category to a message the user can act on.
    private fun Throwable.toUserMessage(): String = when (this) {
        is JsonParseException, is MalformedJsonException, is EOFException, is IllegalStateException ->
            "The events data was in an unexpected format."
        is SocketTimeoutException -> "The server took too long to respond. Please try again."
        is IOException -> "Can't reach the events server. Check your connection and try again."
        is HttpException -> when (code()) {
            in 400..499 -> "The events feed could not be found (HTTP ${code()})."
            in 500..599 -> "The events server is having problems (HTTP ${code()}). Please try again later."
            else -> "Unexpected server response (HTTP ${code()})."
        }
        else -> "Could not load events."
    }

    companion object {
        fun Factory(eventsRepository: EventsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EventsViewModel(eventsRepository) as T
                }
            }
    }
}
