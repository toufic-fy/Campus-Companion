
package com.inci.campuscompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.inci.campuscompanion.data.local.entity.EventEntity

@Dao
interface EventDao {

    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: String): EventEntity?

    @Upsert
    suspend fun upsertEvents(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()
}
