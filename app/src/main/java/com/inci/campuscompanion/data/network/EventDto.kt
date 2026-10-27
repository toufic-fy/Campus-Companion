package com.inci.campuscompanion.data.network

import kotlinx.serialization.Serializable

// Mirrors the JSON returned by GET /events. Optional fields default to null
// so a missing value does not fail the whole response.
@Serializable
data class EventDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val address: String? = null,
    val avatar: String? = null,
    val startingDate: String? = null,
    val endingDate: String? = null,
    val createdAt: String? = null
)
