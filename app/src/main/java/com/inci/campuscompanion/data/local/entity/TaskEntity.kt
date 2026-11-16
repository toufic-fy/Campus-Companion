package com.inci.campuscompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.inci.campuscompanion.model.Task

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false
) {
    fun toDomain(): Task = Task(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted
    )
}

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted
)
