package com.inci.campuscompanion.data

import com.inci.campuscompanion.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getTasks()

    suspend fun upsertTask(task: Task) {
        taskDao.upsertTask(task)
    }

    suspend fun toggleTaskCompletion(task: Task) {
        taskDao.updateTaskCompletion(task.id, !task.isCompleted)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
}
