package com.inci.campuscompanion.ui

import com.inci.campuscompanion.model.CampusEvent

/**
 * Visible network states for the events feed.
 */
sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Content(val events: List<CampusEvent>) : EventsUiState
    data class Error(val message: String) : EventsUiState
}
