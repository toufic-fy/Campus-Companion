package com.inci.campuscompanion.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.EventsRepository
import kotlinx.coroutines.CancellationException
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
 * Events are loaded one page at a time; loadMore() appends the next page.
 */
class EventsViewModel(
    private val repository: EventsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    // The last page that loaded successfully (0 = nothing loaded yet).
    private var currentPage = 0

    init {
        loadEvents()
    }

    /** First load, Refresh, and full-screen Retry: start again from page 1. */
    fun loadEvents() {
        _uiState.value = EventsUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val events = repository.getEvents(page = 1, limit = PAGE_SIZE)
                currentPage = 1
                if (events.isEmpty()) EventsUiState.Empty
                else EventsUiState.Content(
                    events = events,
                    endReached = events.size < PAGE_SIZE
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EventsUiState.Error(errorMessageFor(e))
            }
        }
    }

    /** Called when the user scrolls to the bottom, or taps Retry on the footer. */
    fun loadMore() {
        val state = _uiState.value
        // Only page forward from a list, and never twice at the same time.
        if (state !is EventsUiState.Content || state.isLoadingMore || state.endReached) return

        _uiState.value = state.copy(isLoadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            _uiState.value = try {
                val nextPage = currentPage + 1
                val newEvents = repository.getEvents(page = nextPage, limit = PAGE_SIZE)
                currentPage = nextPage

                // Skip anything we already have, so a repeated page can't duplicate rows.
                val knownIds = state.events.map { it.id }.toSet()
                val added = newEvents.filter { it.id !in knownIds }

                state.copy(
                    events = state.events + added,
                    isLoadingMore = false,
                    endReached = newEvents.size < PAGE_SIZE || added.isEmpty()
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep the events already shown; only the footer shows the error.
                state.copy(isLoadingMore = false, loadMoreError = errorMessageFor(e))
            }
        }
    }

    /** Classify the failure before choosing a message. */
    private fun errorMessageFor(e: Exception): String = when (e) {
        // No connection, timeout, unknown host
        is IOException -> "No connection. Check your internet and try again."
        // The server failed
        is HttpException -> if (e.code() in 500..599) {
            "The server is having problems. Please try again later."
        } else {
            // 4xx: the request was rejected
            "Events could not be found (error ${e.code()})."
        }
        // Data mismatch: log it, show a safe generic message
        is SerializationException -> {
            Log.e(TAG, "Malformed events response", e)
            "Something went wrong while reading the events."
        }
        else -> {
            Log.e(TAG, "Unexpected error while loading events", e)
            "Something went wrong. Please try again."
        }
    }

    companion object {
        private const val TAG = "EventsViewModel"
        private const val PAGE_SIZE = 10

        fun Factory(repository: EventsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EventsViewModel(repository) as T
                }
            }
    }
}
