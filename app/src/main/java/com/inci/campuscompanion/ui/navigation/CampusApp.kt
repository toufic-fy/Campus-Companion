package com.inci.campuscompanion.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.ui.events.EventsScreen
import com.inci.campuscompanion.ui.tasks.TasksScreen
import com.inci.campuscompanion.ui.view_model.CampusEventViewModel
import com.inci.campuscompanion.ui.view_model.MainViewModel

/** Route names for the app's navigation graph. */
object CampusRoutes {
    const val TASKS = "tasks"
    const val EVENTS = "events"
}

/**
 * Root composable of the app. It owns the [NavHostController] and the [NavHost];
 * screens never touch the controller directly, they only receive callbacks.
 */
@Composable
fun CampusApp(
    mainViewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = CampusRoutes.TASKS
    ) {
        composable(CampusRoutes.TASKS) {
            val isDarkMode by mainViewModel.isDarkMode.collectAsStateWithLifecycle()
            val tasks by mainViewModel.tasks.collectAsStateWithLifecycle()

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                TasksScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { mainViewModel.toggleDarkMode(it) },
                    tasks = tasks,
                    onToggleTask = { mainViewModel.toggleTaskCompletion(it) },
                    onAddTask = { title, description -> mainViewModel.upsertTask(title, description) },
                    onOpenEvents = {
                        // launchSingleTop avoids stacking two Events entries on a double tap.
                        navController.navigate(CampusRoutes.EVENTS) { launchSingleTop = true }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        composable(CampusRoutes.EVENTS) {
            // Scoped to this back-stack entry: cleared when the user leaves Events.
            val eventsViewModel: CampusEventViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        CampusEventViewModel(
                            EventsRepository(EventsRemoteDataSource(RetrofitClient.campusApi))
                        )
                    }
                }
            )
            EventsScreen(
                viewModel = eventsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
