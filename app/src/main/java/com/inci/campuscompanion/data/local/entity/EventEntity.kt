package com.inci.campuscompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val startsAt: String,
    val endsAt: String?,
    val location: String?,
)

// A separate marker also distinguishes a confirmed empty feed from no refresh yet.
@Entity(tableName = "events_refresh_state")
data class EventsRefreshState(@PrimaryKey val id: Int = 1)
