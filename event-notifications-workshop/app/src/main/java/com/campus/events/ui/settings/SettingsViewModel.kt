package com.campus.events.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.campus.events.appContainer
import com.campus.events.data.Event
import com.campus.events.work.RefreshEventsWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SystemNotificationState(
    val permissionGranted: Boolean,
    val channelBlocked: Boolean,
)

data class SettingsUiState(
    /** Product choice, stored in DataStore. */
    val eventUpdatesEnabled: Boolean = false,
    /** System state, always queried from Android. */
    val permissionGranted: Boolean = false,
    val channelBlocked: Boolean = false,
    val requiresRuntimePermission: Boolean = false,
    val lastRefreshOutcome: String? = null,
) {
    /** Checkpoint: both the preference and the system must allow it. */
    val canNotify: Boolean get() = eventUpdatesEnabled && permissionGranted && !channelBlocked
    val blockedBySystem: Boolean get() = eventUpdatesEnabled && (!permissionGranted || channelBlocked)
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app.appContainer
    private val checker = container.permissionChecker

    private val systemState = MutableStateFlow(readSystemState())

    val uiState: StateFlow<SettingsUiState> = combine(
        container.notificationSettings.settings,
        container.notificationSettings.lastRefreshOutcome,
        systemState,
    ) { settings, outcome, system ->
        SettingsUiState(
            eventUpdatesEnabled = settings.eventUpdatesEnabled,
            permissionGranted = system.permissionGranted,
            channelBlocked = system.channelBlocked,
            requiresRuntimePermission = checker.requiresRuntimePermission,
            lastRefreshOutcome = outcome,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** Re-read the system state, e.g. when the user comes back from system settings. */
    fun refreshSystemState() {
        systemState.value = readSystemState()
    }

    fun setEventUpdatesEnabled(enabled: Boolean) {
        viewModelScope.launch { container.notificationSettings.setEventUpdatesEnabled(enabled) }
    }

    /**
     * The preference is kept even when permission is denied: the person said they want
     * updates; Android currently says the app may not post. The UI explains how to fix it.
     */
    fun onNotificationPermissionResult(@Suppress("UNUSED_PARAMETER") granted: Boolean) {
        refreshSystemState()
    }

    /** Workshop step 5: one test notification to verify title, icon and tap action. */
    fun sendTestNotification() {
        if (!uiState.value.canNotify) return
        viewModelScope.launch {
            val event = container.eventsRepository.anyCachedEvent() ?: Event(
                id = "evt-1",
                title = "Welcome week fair",
                location = "Main quad",
                startsAt = "Mon 10:00",
                description = "Meet clubs and societies.",
            )
            container.notificationHelper.show(event)
        }
    }

    /** Demo for step 6: make the fake server publish a new event, then refresh. */
    fun simulateNewEventAndRefresh() {
        container.eventsApi.publishNewEvent()
        RefreshEventsWorker.refreshNow(getApplication())
    }

    /** Demo for step 6: refresh with unchanged server data (should never notify). */
    fun refreshNow() {
        RefreshEventsWorker.refreshNow(getApplication())
    }

    private fun readSystemState() = SystemNotificationState(
        permissionGranted = checker.isGranted(),
        channelBlocked = checker.isEventChannelBlocked(),
    )
}
