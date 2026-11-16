package com.inci.campuscompanion.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.inci.campuscompanion.model.CampusEvent

// One cached event row. The Events screen reads from this table, so it still
// has something to show while offline or while a refresh is waiting.
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    val description: String
) {
    fun toCampusEvent() = CampusEvent(
        id = id,
        title = title,
        description = description
    )
}

fun CampusEvent.toEntity() = EventEntity(
    id = id,
    title = title,
    description = description
)
