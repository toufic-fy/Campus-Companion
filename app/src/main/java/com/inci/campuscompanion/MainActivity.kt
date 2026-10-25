package com.inci.campuscompanion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.local.pref.dataStore
import com.inci.campuscompanion.data.repository.TaskRepository
import com.inci.campuscompanion.ui.tasks.TasksScreen
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme
import com.inci.campuscompanion.ui.view_model.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val taskRepository = TaskRepository(database.taskDao())
        val preferencesRepository = UserPreferencesRepository(dataStore)

        val viewModel: MainViewModel by viewModels {
            MainViewModel.Factory(preferencesRepository, taskRepository)
        }

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val tasks by viewModel.tasks.collectAsStateWithLifecycle()

            CampusCompanionTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TasksScreen(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                        tasks = tasks,
                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                        onAddTask = { title, description -> viewModel.upsertTask(title, description) },
                        onNavigateToEvents = {
                            startActivity(Intent(this, EventActivity::class.java))
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
