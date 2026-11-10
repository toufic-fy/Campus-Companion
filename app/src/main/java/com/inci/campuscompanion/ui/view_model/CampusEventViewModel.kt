
package com.inci.campuscompanion.ui.view_model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.worker.EventsWorkScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EventsUiState {
    data object Loading : EventsUiState

    data class Success(
        val events: List<CampusEvent>
    ) : EventsUiState

    data class Error(
        val message: String
    ) : EventsUiState
}

class CampusEventViewModel(
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<EventsUiState>(EventsUiState.Loading)

    val uiState: StateFlow<EventsUiState> =
        _uiState.asStateFlow()

    private val _refreshStatus = MutableStateFlow("Idle")

    val refreshStatus: StateFlow<String> =
        _refreshStatus.asStateFlow()

    private var refreshObservationJob: Job? = null

    private var hasCachedEvents = false

    init {
        observeCachedEvents()
        loadEvents()
    }

    // Observe Events saved in Room
    private fun observeCachedEvents() {
        viewModelScope.launch {
            eventsRepository.observeEvents().collect { events ->

                if (events.isNotEmpty()) {
                    hasCachedEvents = true
                    _uiState.value = EventsUiState.Success(events)

                } else if (!hasCachedEvents &&
                    _uiState.value is EventsUiState.Success
                ) {
                    _uiState.value = EventsUiState.Success(events)
                }
            }
        }
    }

    // Refresh Events from the API and save them in Room
    fun loadEvents() {
        viewModelScope.launch {

            if (!hasCachedEvents) {
                _uiState.value = EventsUiState.Loading
            }

            try {
                eventsRepository.refreshEvents()

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                if (!hasCachedEvents) {
                    _uiState.value = EventsUiState.Error(
                        e.localizedMessage ?: "Failed to load events"
                    )
                }
            }
        }
    }

    // Schedule durable background refresh
    fun scheduleBackgroundRefresh(context: Context) {
        val appContext = context.applicationContext

        EventsWorkScheduler.scheduleRefresh(appContext)

        observeRefreshStatus(appContext)
    }

    // Observe WorkManager status
    fun observeRefreshStatus(context: Context) {
        if (refreshObservationJob?.isActive == true) return

        val workManager = WorkManager.getInstance(
            context.applicationContext
        )

        refreshObservationJob = viewModelScope.launch {

            workManager.getWorkInfosForUniqueWorkFlow(
                EventsWorkScheduler.WORK_NAME
            ).collect { workInfos ->

                val info = workInfos.firstOrNull {
                    !it.state.isFinished
                } ?: workInfos.firstOrNull()

                _refreshStatus.value = when (info?.state) {
                    WorkInfo.State.ENQUEUED -> "Queued"
                    WorkInfo.State.RUNNING -> "Refreshing"
                    WorkInfo.State.SUCCEEDED -> "Current"
                    WorkInfo.State.FAILED -> "Failed"
                    WorkInfo.State.CANCELLED -> "Cancelled"
                    else -> "Idle"
                }
            }
        }
    }

    companion object{
        fun Factory(
            eventsRepository: EventsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CampusEventViewModel(eventsRepository) as T
            }
        }
    }
}
