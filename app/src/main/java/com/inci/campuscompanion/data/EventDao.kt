package com.inci.campuscompanion.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY id ASC")
    fun getEventsFlow(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: String): EventEntity?

    @Upsert
    suspend fun upsertEvents(events: List<EventEntity>)

    @Upsert
    suspend fun upsertEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id IN (SELECT id FROM events LIMIT 1)")
    suspend fun deleteOneEvent(): Int
}
