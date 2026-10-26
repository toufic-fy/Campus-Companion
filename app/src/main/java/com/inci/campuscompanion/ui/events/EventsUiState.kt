package com.inci.campuscompanion.ui.events

import com.inci.campuscompanion.data.model.CampusEvent

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