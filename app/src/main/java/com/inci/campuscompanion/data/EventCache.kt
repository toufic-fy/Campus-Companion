package com.inci.campuscompanion.data

import androidx.room.*
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val timeLabel: String,
    val locationLabel: String,
    val description: String
) {
    fun toDomain() = CampusEvent(id, title, timeLabel, locationLabel, description)
}

@Entity(tableName = "seen_events")
data class SeenEventEntity(@PrimaryKey val id: String)

@Entity(tableName = "event_sync")
data class EventSyncEntity(@PrimaryKey val id: Int = 1, val initialized: Boolean = true)

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY id")
    fun observeEvents(): Flow<List<EventEntity>>
    @Query("SELECT * FROM seen_events")
    suspend fun seenEvents(): List<SeenEventEntity>
    @Query("SELECT * FROM event_sync WHERE id = 1")
    suspend fun syncState(): EventSyncEntity?
    @Query("DELETE FROM events")
    suspend fun clearEvents()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSeenEvents(events: List<SeenEventEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSyncState(state: EventSyncEntity)

    // Cache, baseline and deduplication history advance together, including an empty refresh.
    @Transaction
    suspend fun replace(events: List<EventEntity>) {
        clearEvents()
        insertEvents(events)
        insertSeenEvents(events.map { SeenEventEntity(it.id) })
        saveSyncState(EventSyncEntity())
    }
}

interface EventsCache {
    val events: Flow<List<CampusEvent>>
    suspend fun initialized(): Boolean
    suspend fun seenIds(): Set<String>
    suspend fun replace(events: List<CampusEvent>)
}

class RoomEventsCache(private val dao: EventDao) : EventsCache {
    override val events = dao.observeEvents().map { rows -> rows.map { it.toDomain() } }
    override suspend fun initialized() = dao.syncState()?.initialized == true
    override suspend fun seenIds() = dao.seenEvents().map { it.id }.toSet()
    override suspend fun replace(events: List<CampusEvent>) = dao.replace(events.map {
        EventEntity(it.id, it.title, it.timeLabel, it.locationLabel, it.description)
    })
}
