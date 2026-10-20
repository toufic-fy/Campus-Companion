package com.inci.campuscompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.*
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface EventsUiState {
 data object Loading : EventsUiState
 data object Empty : EventsUiState
 data class Content(val events: List<CampusEvent>) : EventsUiState
 data class Error(val message: String) : EventsUiState
}
fun errorMessage(error: Exception): String = when (error) {
 is HttpException -> when (error.code()) {
  in 500..599 -> "The server is unavailable. Please try again later."
  404 -> "The events feed could not be found. Please try again later."
  in 400..499 -> "The server rejected the request. Please try again later."
  else -> "An unexpected server response occurred. Please retry."
 }
 is InvalidEventData, is JsonDataException, is JsonEncodingException -> "The server returned invalid event data. Please try again later."
 is IOException -> "Could not connect. Check your internet connection and retry."
 else -> "Could not load events. Please retry."
}
class EventsViewModel(private val repository: EventsRepository) : ViewModel() {
 private val _state = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
 val state = _state.asStateFlow()
 private var request: Job? = null
 init { refresh() }
 fun refresh() {
  if (request?.isActive == true) return
  request = viewModelScope.launch {
   _state.value = EventsUiState.Loading
   try {
    val events = repository.getEvents()
    _state.value = if (events.isEmpty()) EventsUiState.Empty else EventsUiState.Content(events)
   } catch (cancelled: CancellationException) { throw cancelled
   } catch (error: Exception) { _state.value = EventsUiState.Error(errorMessage(error)) }
  }
 }
 companion object {
  val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
   @Suppress("UNCHECKED_CAST")
   override fun <T : ViewModel> create(modelClass: Class<T>): T {
    require(modelClass.isAssignableFrom(EventsViewModel::class.java))
    return EventsViewModel(AppContainer.repository) as T
   }
  }
 }
}
