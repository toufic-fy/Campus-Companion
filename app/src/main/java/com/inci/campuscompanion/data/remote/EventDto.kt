package com.inci.campuscompanion.data.remote

import kotlinx.serialization.Serializable

// Mirrors the JSON returned by GET events/event. Optional fields have defaults
// so a missing value does not break the whole response.
@Serializable
data class EventDto(
    val id: String,
    val name: String = "",
    val startingDate: String? = null,
    val endingDate: String? = null,
    val description: String? = null,
    val address: String? = null,
    val createdAt: String? = null,
    val avatar: String? = null
)
