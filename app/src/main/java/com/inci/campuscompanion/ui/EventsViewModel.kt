package com.inci.campuscompanion.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Content(val events: List<CampusEvent>) : EventsUiState
    data object Empty : EventsUiState
    data class Error(val message: String) : EventsUiState
}

class EventsViewModel(private val repository: EventsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = EventsUiState.Loading
            _uiState.value = try {
                val events = repository.getEvents()
                if (events.isEmpty()) EventsUiState.Empty else EventsUiState.Content(events)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                EventsUiState.Error(
                    if (e.code() in 500..599) "The server is unavailable. Please retry later."
                    else "The request was rejected (HTTP ${e.code()}). Please retry."
                )
            } catch (e: IOException) {
                EventsUiState.Error("Unable to connect. Check your connection and retry.")
            } catch (e: Exception) {
                Log.e("EventsViewModel", "Unable to read events", e)
                EventsUiState.Error("Unable to read event data. Please retry.")
            }
        }
    }

    companion object {
        fun Factory(repository: EventsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EventsViewModel(repository) as T
            }
    }
}
