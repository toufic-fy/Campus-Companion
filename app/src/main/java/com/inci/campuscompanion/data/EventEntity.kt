
package com.inci.campuscompanion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val timeLabel: String,
    val locationLabel: String
)
