package com.inci.campuscompanion.ui

import com.inci.campuscompanion.model.CampusEvent

/**
 * Visible network states. Empty and Error are different things:
 * an empty list never stands in for a failure.
 */
sealed interface EventsUiState {
    data object Loading : EventsUiState

    /**
     * The events loaded so far, plus the state of the "next page" request.
     * A failed next page keeps the list on screen and shows a small retry instead.
     */
    data class Content(
        val events: List<CampusEvent>,
        val isLoadingMore: Boolean = false,
        val loadMoreError: String? = null,
        val endReached: Boolean = false
    ) : EventsUiState

    data object Empty : EventsUiState
    data class Error(val message: String) : EventsUiState
}
