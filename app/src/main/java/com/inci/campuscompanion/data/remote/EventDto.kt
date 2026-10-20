package com.inci.campuscompanion.data.remote

import com.google.gson.annotations.SerializedName

data class EventDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("location") val location: String?,
    @SerializedName("starts_at") val startsAt: String?,
    @SerializedName("description") val description: String?
)
