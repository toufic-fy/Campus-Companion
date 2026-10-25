package com.inci.campuscompanion.data.repository

import com.inci.campuscompanion.data.local.dao.TaskDao
import com.inci.campuscompanion.data.local.entity.toEntity
import com.inci.campuscompanion.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getTasks().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun upsertTask(task: Task) {
        taskDao.upsertTask(task.toEntity())
    }

    suspend fun toggleTaskCompletion(task: Task) {
        taskDao.updateTaskCompletion(task.id, !task.isCompleted)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }
}
