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
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Content(val events: List<CampusEvent>) : EventsUiState
    data object Empty : EventsUiState
    data class Error(val message: String) : EventsUiState
}

class EventsViewModel(private val repository: EventsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() {
        _uiState.value = EventsUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val events = repository.getEvents()
                if (events.isEmpty()) EventsUiState.Empty else EventsUiState.Content(events)
            } catch (e: IOException) {
                EventsUiState.Error("No connection. Check your network and try again.")
            } catch (e: HttpException) {
                if (e.code() in 500..599) {
                    EventsUiState.Error("The server is having trouble. Please try again later.")
                } else {
                    EventsUiState.Error("The request was rejected (error ${e.code()}).")
                }
            } catch (e: SerializationException) {
                Log.e(TAG, "Malformed events response", e)
                EventsUiState.Error("Something went wrong loading events.")
            }
        }
    }

    companion object {
        private const val TAG = "EventsViewModel"

        fun Factory(repository: EventsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EventsViewModel(repository) as T
                }
            }
    }
}
