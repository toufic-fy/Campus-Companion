package com.inci.campuscompanion.data.local.dao

import com.inci.campuscompanion.data.local.entity.EventSyncState
import com.inci.campuscompanion.data.local.entity.SeenEvent
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.inci.campuscompanion.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startsAt")
    fun getEvents(): Flow<List<EventEntity>>

    @Query("SELECT id FROM events")
    suspend fun cachedIds(): List<String>

    @Query("SELECT id FROM seen_events")
    suspend fun seenIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM event_sync_state WHERE id = 1)")
    suspend fun hasBaseline(): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM event_sync_state WHERE id = 1)")
    fun observeBaseline(): Flow<Boolean>

    @Upsert
    suspend fun rememberIds(ids: List<SeenEvent>)

    @Upsert
    suspend fun markSynced(state: EventSyncState)

    @Upsert
    suspend fun upsertEvents(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()

    @Transaction
    suspend fun replaceEvents(events: List<EventEntity>) {
        deleteAllEvents()
        upsertEvents(events)
    }
}
