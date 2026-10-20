package com.inci.campuscompanion.ui

import com.inci.campuscompanion.model.CampusEvent

/**
 * Visible network states. Empty and Error are different things:
 * an empty list never stands in for a failure.
 */
sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Content(val events: List<CampusEvent>) : EventsUiState
    data object Empty : EventsUiState
    data class Error(val message: String) : EventsUiState
}
