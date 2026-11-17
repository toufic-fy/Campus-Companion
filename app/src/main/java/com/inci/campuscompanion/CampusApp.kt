package com.inci.campuscompanion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inci.campuscompanion.ui.EventsScreen
import com.inci.campuscompanion.ui.EventsViewModel
import com.inci.campuscompanion.ui.MainViewModel

@Composable
fun CampusApp(tasksViewModel: MainViewModel, eventsViewModel: EventsViewModel) {
    val navController = rememberNavController()
    AppNavHost(navController, tasksViewModel, eventsViewModel)
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    tasksViewModel: MainViewModel,
    eventsViewModel: EventsViewModel
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        NavHost(navController, startDestination = "tasks", modifier = Modifier.padding(padding)) {
            composable("tasks") {
                val darkMode by tasksViewModel.isDarkMode.collectAsStateWithLifecycle()
                val tasks by tasksViewModel.tasks.collectAsStateWithLifecycle()
                TaskScreen(
                    isDarkMode = darkMode,
                    onToggleDarkMode = tasksViewModel::toggleDarkMode,
                    tasks = tasks,
                    onToggleTask = tasksViewModel::toggleTaskCompletion,
                    onAddTask = { title, description -> tasksViewModel.upsertTask(title, description) },
                    onOpenEvents = { navController.navigate("events") { launchSingleTop = true } }
                )
            }
            composable("events") {
                val state by eventsViewModel.uiState.collectAsStateWithLifecycle()
                EventsScreen(state, eventsViewModel::refresh, onBack = { navController.popBackStack() })
            }
        }
    }
}
