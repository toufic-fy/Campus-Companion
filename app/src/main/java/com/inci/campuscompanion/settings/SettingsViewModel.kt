package com.inci.campuscompanion.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.inci.campuscompanion.CampusCompanionApp
import com.inci.campuscompanion.notifications.NotificationPermissionChecker
import com.inci.campuscompanion.notifications.NotificationSettings
import com.inci.campuscompanion.notifications.NotificationSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
/*import com.inci.campuscompanion.notifications.NotificationSettingsRepository
import com.inci.campuscompanion.notifications.NotificationSettings
import com.inci.campuscompanion.CampusCompanionApp*/
class SettingsViewModel(
    private val repository: NotificationSettingsRepository,
    private val permissionChecker: NotificationPermissionChecker      // NEW
) : ViewModel() {

    val settings: StateFlow<NotificationSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationSettings(eventUpdatesEnabled = false)
        )

    // NEW: current system permission state
    private val _permissionGranted = MutableStateFlow(permissionChecker.isGranted())
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    // NEW: true after the user denied the dialog
    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    fun onEventUpdatesToggled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEventUpdatesEnabled(enabled)
        }
    }

    // NEW: called by the permission launcher
    fun onNotificationPermissionResult(granted: Boolean) {
        _permissionGranted.value = granted
        _permissionDenied.value = !granted
        if (granted) {
            onEventUpdatesToggled(true)
        }
        // If denied: do nothing else. The preference stays as it was.
    }

    // NEW: call when the screen returns to the foreground
    fun refreshPermissionState() {
        val granted = permissionChecker.isGranted()
        _permissionGranted.value = granted
        if (granted) _permissionDenied.value = false
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampusCompanionApp
                SettingsViewModel(
                    app.notificationSettingsRepository,
                    app.notificationPermissionChecker                 // NEW
                )
            }
        }
    }
}