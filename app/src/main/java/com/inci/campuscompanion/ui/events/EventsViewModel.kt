package com.inci.campuscompanion.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import com.inci.campuscompanion.data.repository.EventsRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class EventsViewModel(
    private val repository: EventsRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<EventsUiState>(EventsUiState.Loading)

    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadEvents()
    }

    fun loadEvents() {
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            _uiState.value = EventsUiState.Loading

            try {
                val events = repository.getEvents()

                _uiState.value = if (events.isEmpty()) {
                    EventsUiState.Empty
                } else {
                    EventsUiState.Content(events)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                val message = if (e.code() >= 500) {
                    "The server is unavailable. Please try again later."
                } else {
                    "The request was rejected (HTTP ${e.code()})."
                }

                _uiState.value = EventsUiState.Error(message)
            } catch (e: JsonParseException) {
                _uiState.value = EventsUiState.Error(
                    "The server returned invalid event data."
                )
            } catch (e: IOException) {
                _uiState.value = EventsUiState.Error(
                    "Could not connect. Check your internet and try again."
                )
            } catch (e: Exception) {
                _uiState.value = EventsUiState.Error(
                    "Could not load events. Please try again."
                )
            }
        }
    }
}