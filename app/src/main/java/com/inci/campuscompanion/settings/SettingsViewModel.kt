package com.inci.campuscompanion.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.inci.campuscompanion.CampusCompanionApp
import com.inci.campuscompanion.notifications.NotificationSettings
import com.inci.campuscompanion.notifications.NotificationSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
/*import com.inci.campuscompanion.notifications.NotificationSettingsRepository
import com.inci.campuscompanion.notifications.NotificationSettings
import com.inci.campuscompanion.CampusCompanionApp*/
class SettingsViewModel(
    private val repository: NotificationSettingsRepository
) : ViewModel() {

    val settings: StateFlow<NotificationSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationSettings(eventUpdatesEnabled = false)
        )

    fun onEventUpdatesToggled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEventUpdatesEnabled(enabled)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampusCompanionApp
                SettingsViewModel(app.notificationSettingsRepository)
            }
        }
    }
}