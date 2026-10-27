package com.inci.campuscompanion.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Content(val events: List<CampusEvent>) : EventsUiState
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
                onSuccess = {
                    Log.d(TAG, "Loaded ${it.size} events")
                    EventsUiState.Content(it)
                },
                onFailure = {
                    Log.e(TAG, "Failed to load events: ${it.javaClass.simpleName}: ${it.message}", it)
                    EventsUiState.Error(it.toUserMessage())
                }
            )
        }
    }

    /** Turns a technical exception into a message a user can act on. */
    private fun Throwable.toUserMessage(): String = when (this) {
        is IOException -> "No internet connection. Check your network and try again."
        is HttpException -> "The events server returned an error (HTTP ${code()}). Please try again."
        else -> "Something went wrong while loading events."
    }

    companion object {
        private const val TAG = "EventsViewModel"

        fun Factory(eventsRepository: EventsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EventsViewModel(eventsRepository) as T
                }
            }
    }
}
