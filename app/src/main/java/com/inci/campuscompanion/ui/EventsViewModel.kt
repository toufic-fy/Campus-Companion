package com.inci.campuscompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.EventsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class EventsViewModel(
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = EventsUiState.Loading
            _uiState.value = try {
                EventsUiState.Content(eventsRepository.getEvents())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EventsUiState.Error(e.toUserMessage())
            }
        }
    }

    private fun Exception.toUserMessage(): String = when (this) {
        is IOException -> "No internet connection. Check your network and try again."
        is HttpException -> when (code()) {
            in 400..499 -> "The events request was rejected (error ${code()})."
            in 500..599 -> "The events server is having problems (error ${code()}). Try again later."
            else -> "Unexpected server response (error ${code()})."
        }
        is SerializationException ->"Received event data in an unexpected format."
        else -> "Something went wrong while loading events."
    }

    companion object {
        fun Factory(
            eventsRepository: EventsRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EventsViewModel(eventsRepository) as T
                }
            }
    }
}
