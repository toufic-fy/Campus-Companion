package com.inci.campuscompanion.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Starts the network work in viewModelScope (no GlobalScope) and turns
 * the result into visible UI state: Loading -> Content / Empty / Error.
 */
class EventsViewModel(
    private val repository: EventsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    /** Used for the first load, Refresh, and Retry. */
    fun loadEvents() {
        _uiState.value = EventsUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val events = repository.getEvents()
                if (events.isEmpty()) EventsUiState.Empty
                else EventsUiState.Content(events)
            } catch (e: IOException) {
                // No connection, timeout, unknown host
                EventsUiState.Error("No connection. Check your internet and try again.")
            } catch (e: HttpException) {
                if (e.code() in 500..599) {
                    // The server failed
                    EventsUiState.Error("The server is having problems. Please try again later.")
                } else {
                    // 4xx: the request was rejected
                    EventsUiState.Error("Events could not be found (error ${e.code()}).")
                }
            } catch (e: SerializationException) {
                // Data mismatch: log it, show a safe generic message
                Log.e(TAG, "Malformed events response", e)
                EventsUiState.Error("Something went wrong while reading the events.")
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
