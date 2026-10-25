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
import kotlin.coroutines.cancellation.CancellationException

class EventsViewModel(
    private val repository: EventsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = EventsUiState.Loading

            _uiState.value = try {
                val events = repository.getEvents()
                if (events.isEmpty()) {
                    EventsUiState.Empty
                } else {
                    EventsUiState.Content(events)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                EventsUiState.Error("Can't reach the server. Check your connection and try again.")
            } catch (e: HttpException) {
                if (e.code() in 500..599) {
                    EventsUiState.Error("The server had a problem. Please try again later.")
                } else {
                    EventsUiState.Error("The request was rejected (error ${e.code()}).")
                }
            } catch (e: SerializationException) {
                Log.e(TAG, "Server sent data we couldn't read", e)
                EventsUiState.Error("Received unexpected data from the server.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading events", e)
                EventsUiState.Error("Something went wrong. Please try again.")
            }
        }
    }

    class Factory(
        private val repository: EventsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EventsViewModel(repository) as T
    }

    private companion object {
        const val TAG = "EventsViewModel"
    }
}