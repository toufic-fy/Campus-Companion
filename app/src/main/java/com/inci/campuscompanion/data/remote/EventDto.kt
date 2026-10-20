package com.inci.campuscompanion.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the JSON returned by GET events/event (the wire format).
 * It is NOT the object shown in the UI — see CampusEvent for that.
 */
@Serializable
data class EventDto(
    val id: String,
    val name: String,
    @SerialName("startingDate") val startsAt: String,
    @SerialName("endingDate") val endsAt: String? = null,
    val description: String? = null,
    val address: String? = null
)
