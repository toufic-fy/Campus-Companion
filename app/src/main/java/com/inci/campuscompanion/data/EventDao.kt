package com.inci.campuscompanion.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY id")
    fun observeEvents(): Flow<List<EventEntity>>

    // Snapshot of the cache before a refresh, used to find new or changed events.
    @Query("SELECT * FROM events")
    suspend fun getAll(): List<EventEntity>

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun deleteAll()

    // Swap the whole cache in one transaction so the screen never sees it half-empty.
    @Transaction
    suspend fun replaceAll(events: List<EventEntity>) {
        deleteAll()
        upsertAll(events)
    }
}
