package com.inci.campuscompanion.ui.view_model

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.notification.EventNotificationHelper
import com.inci.campuscompanion.notification.NotificationPermissionChecker
import com.inci.campuscompanion.worker.RefreshEventWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Success(val events: List<CampusEvent>) : EventsUiState
    data class Error(val message: String) : EventsUiState
}

class CampusEventViewModel(
    private val eventsRepository: EventsRepository,
    private val workManager: WorkManager,
    private val preferencesRepository: UserPreferencesRepository,
    private val permissionChecker: NotificationPermissionChecker,
    private val notificationHelper: EventNotificationHelper,
) : ViewModel() {
    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    val workInfo: StateFlow<WorkInfo?> = workManager
        .getWorkInfosForUniqueWorkFlow(RefreshEventWorker.WORK_NAME)
        .map { list -> list.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val eventUpdatesEnabled: StateFlow<Boolean> = preferencesRepository.eventUpdatesEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    private val _notificationPermissionGranted = MutableStateFlow(permissionChecker.isGranted())
    val notificationPermissionGranted: StateFlow<Boolean> = _notificationPermissionGranted.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = EventsUiState.Loading
            try {
                val events = eventsRepository.getEvents()
                _uiState.value = EventsUiState.Success(events)
            } catch (e: Exception) {
                _uiState.value = EventsUiState.Error(e.localizedMessage ?: "Failed to load events")
            }
        }
    }

    fun setEventUpdatesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setEventUpdatesEnabled(enabled)
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _notificationPermissionGranted.value = granted
    }

    fun refreshPermissionState() {
        _notificationPermissionGranted.value = permissionChecker.isGranted()
    }

    fun notificationSettingsIntent(): Intent = permissionChecker.appNotificationSettingsIntent()

    fun sendTestNotification() {
        val event = (uiState.value as? EventsUiState.Success)?.events?.firstOrNull()
            ?: CampusEvent(
                id = "test",
                title = "Test event",
                description = "",
                startsAt = "",
                timeLabel = "",
            )
        if (eventUpdatesEnabled.value && permissionChecker.isGranted()) {
            notificationHelper.showNewEvent(event)
        }
    }

    companion object {
        fun Factory(
            eventsRepository: EventsRepository,
            workManager: WorkManager,
            preferencesRepository: UserPreferencesRepository,
            permissionChecker: NotificationPermissionChecker,
            notificationHelper: EventNotificationHelper,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CampusEventViewModel(
                    eventsRepository,
                    workManager,
                    preferencesRepository,
                    permissionChecker,
                    notificationHelper,
                ) as T
            }
        }
    }
}
