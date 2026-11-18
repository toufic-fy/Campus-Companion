package com.inci.campuscompanion

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.inci.campuscompanion.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DatabaseMigrationTest {
    @Test fun versionOneTasksSurviveMigrationToThree() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-workshop-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, isCompleted INTEGER NOT NULL)")
            db.execSQL("INSERT INTO tasks VALUES (1, 'Keep my homework', 'Saved before upgrade', 1)")
            db.version = 1
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
        try {
            val task = db.taskDao().getTasks().first().single()
            assertEquals("Keep my homework", task.title)
            assertTrue(task.isCompleted)
            assertTrue(db.eventDao().getEvents().first().isEmpty())
            assertFalse(db.eventDao().hasBaseline())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
