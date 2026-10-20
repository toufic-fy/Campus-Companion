package com.inci.campuscompanion.data.remote

import com.google.gson.annotations.SerializedName

// Mirrors the JSON exactly; fields are nullable because the server is not under our control.
data class EventDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("location") val location: String?,
    @SerializedName("starts_at") val startsAt: String?,
    @SerializedName("description") val description: String?
)
