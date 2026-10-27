package com.inci.campuscompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.CancellationException
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

class EventsViewModel(
    private val repository: EventsRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<EventsUiState>(EventsUiState.Loading)

    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
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
            } catch (e: IOException) {
                _uiState.value = EventsUiState.Error(
                    "Network unavailable. Please retry."
                )
            } catch (e: HttpException) {
                _uiState.value = EventsUiState.Error(
                    "Server error ${e.code()}. Please retry."
                )
            } catch (e: Exception) {
                _uiState.value = EventsUiState.Error(
                    "Unable to load events. Please retry."
                )
            }
        }
    }
}
