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
import com.inci.campuscompanion.TaskScreen
import com.inci.campuscompanion.ui.EventsScreen
import com.inci.campuscompanion.ui.EventsViewModel
import com.inci.campuscompanion.ui.MainViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    eventsViewModel: EventsViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "tasks"
    ) {
        composable("tasks") {
            val isDarkMode by mainViewModel.isDarkMode.collectAsStateWithLifecycle()
            val tasks by mainViewModel.tasks.collectAsStateWithLifecycle()

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                TaskScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { mainViewModel.toggleDarkMode(it) },
                    tasks = tasks,
                    onToggleTask = { mainViewModel.toggleTaskCompletion(it) },
                    onAddTask = { title, description -> mainViewModel.upsertTask(title, description) },
                    onOpenEvents = { navController.navigate("events") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        composable("events") {
            val eventsUiState by eventsViewModel.uiState.collectAsStateWithLifecycle()

            EventsScreen(
                uiState = eventsUiState,
                onRefresh = { eventsViewModel.loadEvents() },
                onLoadMore = { eventsViewModel.loadMore() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
