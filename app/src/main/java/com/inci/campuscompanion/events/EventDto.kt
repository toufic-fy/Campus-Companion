package com.inci.campuscompanion.events

import kotlinx.serialization.Serializable

@Serializable
data class EventDto(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    val startingDate: String? = null,
    val avatar: String? = null,
    val createdAt: String? = null,
    val endingDate: String? = null,
    val address: String? = null
)
