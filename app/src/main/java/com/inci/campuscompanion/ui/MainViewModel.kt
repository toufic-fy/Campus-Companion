package com.inci.campuscompanion.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.inci.campuscompanion.data.AppDatabase
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.data.TaskRepository
import com.inci.campuscompanion.data.UserPreferencesRepository
import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.model.Task
import com.inci.campuscompanion.util.EventNotificationHelper
import com.inci.campuscompanion.worker.RefreshEventsWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val taskRepository: TaskRepository,
    private val eventsRepository: EventsRepository? = null
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = preferencesRepository.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    val isEventNotificationsEnabled: StateFlow<Boolean> = preferencesRepository.isEventNotificationsEnabled.stateIn(
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

    fun toggleEventNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setEventNotificationsEnabled(enabled)
        }
    }

    fun sendTestNotification(context: Context) {
        val fakeEvent = CampusEvent(
            id = "fake_debug_${System.currentTimeMillis()}",
            title = "Fake Debug Campus Event",
            description = "Test notification for debugging event updates",
            date = "2026-10-20",
            location = "Campus Center"
        )
        Log.d("EventNotification", "Debug: Triggering sendTestNotification for fake event: ${fakeEvent.id}")
        EventNotificationHelper.show(
            context = context,
            event = fakeEvent,
            eventUpdatesEnabled = isEventNotificationsEnabled.value
        )
    }

    fun deleteOneCachedEvent(context: Context, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(context)
            val repo = eventsRepository ?: EventsRepository(CampusApi.create(), db.eventDao())
            val deleted = repo.deleteOneCachedEvent()
            Log.d("EventNotification", "Debug: deleteOneCachedEvent deleted row: $deleted")
            onResult(deleted)
        }
    }

    fun runRefreshNow(context: Context) {
        Log.d("EventNotification", "Debug: Enqueuing RefreshEventsWorker via runRefreshNow()")
        val workRequest = RefreshEventsWorker.buildWorkRequest()
        WorkManager.getInstance(context).enqueueUniqueWork(
            RefreshEventsWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

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
            taskRepository: TaskRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(preferencesRepository, taskRepository) as T
                }
            }
    }
}
