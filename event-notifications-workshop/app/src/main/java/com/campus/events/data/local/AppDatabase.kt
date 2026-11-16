package com.campus.events.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.campus.events.data.Event
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val location: String,
    val startsAt: String,
    val description: String,
)

fun EventEntity.toDomain() = Event(id, title, location, startsAt, description)
fun Event.toEntity() = EventEntity(id, title, location, startsAt, description)

@Dao
abstract class EventDao {
    @Query("SELECT * FROM events ORDER BY id")
    abstract fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    abstract fun observeById(id: String): Flow<EventEntity?>

    @Query("SELECT id FROM events")
    abstract suspend fun getAllIds(): List<String>

    @Query("SELECT * FROM events ORDER BY id LIMIT 1")
    abstract suspend fun getFirst(): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(events: List<EventEntity>)

    @Query("DELETE FROM events")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceAll(events: List<EventEntity>) {
        deleteAll()
        insertAll(events)
    }
}

@Database(entities = [EventEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "campus-events.db")
                .build()
                .also { instance = it }
        }
    }
}
