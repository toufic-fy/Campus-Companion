package com.inci.campuscompanion.ui.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.repository.TaskRepository
import com.inci.campuscompanion.model.Task
import com.inci.campuscompanion.notification.EventNotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val taskRepository: TaskRepository,
    private val notificationHelper: EventNotificationHelper
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = preferencesRepository.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    val eventUpdatesEnabled: StateFlow<Boolean> = preferencesRepository.eventUpdatesEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    val tasks: StateFlow<List<Task>> = taskRepository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkMode(enabled)
        }
    }

    fun setEventUpdatesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setEventUpdatesEnabled(enabled)
        }
    }

    fun hasNotificationPermission(): Boolean = notificationHelper.hasPermission()

    fun sendTestNotification() = notificationHelper.notifyTest()

    fun upsertTask(title: String, description: String, id: Int = 0) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.upsertTask(
                Task(
                    id = id,
                    title = title.trim(),
                    description = description.trim()
                )
            )
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    companion object {
        fun Factory(
            preferencesRepository: UserPreferencesRepository,
            taskRepository: TaskRepository,
            notificationHelper: EventNotificationHelper
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TasksViewModel(preferencesRepository, taskRepository, notificationHelper) as T
                }
            }
    }
}