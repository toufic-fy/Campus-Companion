package com.inci.campuscompanion.ui.navigation

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
import com.inci.campuscompanion.ui.events.EventsScreen
import com.inci.campuscompanion.ui.tasks.TasksScreen
import com.inci.campuscompanion.ui.view_model.CampusEventViewModel
import com.inci.campuscompanion.ui.view_model.MainViewModel


@Composable
fun CampusApp(
    viewModel: MainViewModel,
    eventsViewModel: CampusEventViewModel
) {
    val navController = rememberNavController()
    AppNavHost(navController, viewModel, eventsViewModel)
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    eventsViewModel: CampusEventViewModel
) {
    NavHost(navController = navController, startDestination = "tasks") {
        composable("tasks") {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val tasks by viewModel.tasks.collectAsStateWithLifecycle()
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                TasksScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                    tasks = tasks,
                    onToggleTask = { viewModel.toggleTaskCompletion(it) },
                    onAddTask = { title, description -> viewModel.upsertTask(title, description) },
                    onNavigateToEvents = { navController.navigate("events") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        composable("events") {
            EventsScreen(
                viewModel = eventsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}