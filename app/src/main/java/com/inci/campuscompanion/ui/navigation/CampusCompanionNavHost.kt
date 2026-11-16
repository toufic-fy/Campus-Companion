package com.inci.campuscompanion.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inci.campuscompanion.ui.events.EventsPlaceholderScreen
import com.inci.campuscompanion.ui.tasks.TaskRoute
import com.inci.campuscompanion.ui.view_model.TasksViewModel

object AppRoutes {
    const val TASKS = "tasks"
    const val EVENTS = "events"
}

@Composable
fun CampusCompanionNavHost(
    viewModel: TasksViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.TASKS,
        modifier = modifier
    ) {
        composable(AppRoutes.TASKS) {
            TaskRoute(
                viewModel = viewModel,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                onNavigateEvents = { navController.navigate(AppRoutes.EVENTS) }
            )
        }

        composable(AppRoutes.EVENTS) {
            EventsPlaceholderScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}