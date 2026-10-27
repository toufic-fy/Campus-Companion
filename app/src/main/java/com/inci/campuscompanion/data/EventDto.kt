package com.inci.campuscompanion.data

// Nullable wire fields let the repository validate missing/null required values.
data class EventDto(
    val id: String? = null,
    val name: String? = null,
    val startingDate: String? = null,
    val address: String? = null,
    val description: String? = null
)
