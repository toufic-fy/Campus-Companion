
package com.inci.campuscompanion.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.worker.EventsRefreshWork
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface EventsUiState {
    data object Loading : EventsUiState

    data class Content(
        val events: List<CampusEvent>
    ) : EventsUiState

    data object Empty : EventsUiState

    data class Error(
        val message: String
    ) : EventsUiState
}

enum class RefreshUi {
    Idle,
    Queued,
    Refreshing,
    Current,
    Failed
}

class EventsViewModel(
    private val repository: EventsRepository,
    private val workManager: WorkManager
) : ViewModel() {

    val uiState: StateFlow<EventsUiState> =
        repository.observeEvents()
            .map { events: List<CampusEvent> ->
                if (events.isEmpty()) {
                    EventsUiState.Empty
                } else {
                    EventsUiState.Content(events)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                EventsUiState.Loading
            )

    val refreshStatus: StateFlow<RefreshUi> =
        workManager
            .getWorkInfosForUniqueWorkFlow(
                EventsRefreshWork.UNIQUE_NAME
            )
            .map { infos ->
                val info = infos.firstOrNull {
                    !it.state.isFinished
                } ?: infos.lastOrNull()

                when (info?.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.BLOCKED -> RefreshUi.Queued

                    WorkInfo.State.RUNNING -> RefreshUi.Refreshing

                    WorkInfo.State.SUCCEEDED -> RefreshUi.Current

                    WorkInfo.State.FAILED,
                    WorkInfo.State.CANCELLED -> RefreshUi.Failed

                    null -> RefreshUi.Idle
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                RefreshUi.Idle
            )

    init {
        refreshEvents()
    }

    fun refreshEvents() {
        EventsRefreshWork.enqueue(workManager)
    }
}
