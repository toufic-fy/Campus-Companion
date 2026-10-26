package com.inci.campuscompanion.data.remote

import com.google.gson.annotations.SerializedName

data class EventDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("startingDate") val startingDate: String?,
    @SerializedName("endingDate") val endingDate: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("createdAt") val createdAt: String?
)
