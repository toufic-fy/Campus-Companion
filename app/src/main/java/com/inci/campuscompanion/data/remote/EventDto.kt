package com.inci.campuscompanion.data.remote

/**
 * Exact shape of one item returned by the events endpoint.
 * Fields are nullable because we do not control the server.
 */
data class EventDto(
    val id: String?,
    val name: String?,
    val description: String?,
    val address: String?,
    val avatar: String?,
    val startingDate: String?,
    val endingDate: String?,
    val createdAt: String?
)
