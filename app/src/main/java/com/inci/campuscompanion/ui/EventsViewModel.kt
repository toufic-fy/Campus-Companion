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

class EventsViewModel(
    private val repository: EventsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    private var currentPage = 0

    init {
        loadEvents()
    }

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

    fun loadMore() {
        val state = _uiState.value
        if (state !is EventsUiState.Content || state.isLoadingMore || state.endReached) return

        _uiState.value = state.copy(isLoadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            _uiState.value = try {
                val nextPage = currentPage + 1
                val newEvents = repository.getEvents(page = nextPage, limit = PAGE_SIZE)
                currentPage = nextPage

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
                state.copy(isLoadingMore = false, loadMoreError = errorMessageFor(e))
            }
        }
    }

    private fun errorMessageFor(e: Exception): String = when (e) {
        is IOException -> "No connection. Check your internet and try again."
        is HttpException -> if (e.code() in 500..599) {
            "The server is having problems. Please try again later."
        } else {
            "Events could not be found (error ${e.code()})."
        }
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
