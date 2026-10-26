package com.inci.campuscompanion.data

import com.inci.campuscompanion.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDto: TaskDto) {
    val allTasks: Flow<List<Task>> = taskDto.getTasks()

    suspend fun upsertTask(task: Task) {
        taskDto.upsertTask(task)
    }

    suspend fun toggleTaskCompletion(task: Task) {
        taskDto.updateTaskCompletion(task.id, !task.isCompleted)
    }

    suspend fun deleteTask(task: Task) {
        taskDto.deleteTask(task)
    }
}
