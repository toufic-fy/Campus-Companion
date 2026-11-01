package com.inci.campuscompanion.ui

import com.inci.campuscompanion.model.CampusEvent

sealed interface EventsUiState {
    data object Loading : EventsUiState

    data class Content(
        val events: List<CampusEvent>,
        val isLoadingMore: Boolean = false,
        val loadMoreError: String? = null,
        val endReached: Boolean = false
    ) : EventsUiState

    data object Empty : EventsUiState
    data class Error(val message: String) : EventsUiState
}
