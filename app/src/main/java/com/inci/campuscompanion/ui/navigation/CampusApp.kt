package com.inci.campuscompanion.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.ui.events.EventsScreen
import com.inci.campuscompanion.ui.tasks.TasksScreen
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme
import com.inci.campuscompanion.ui.view_model.CampusEventViewModel
import com.inci.campuscompanion.ui.view_model.MainViewModel

object CampusRoutes {
    const val TASKS = "tasks"
    const val EVENTS = "events"
}

@Composable
fun CampusApp(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val isDarkMode by mainViewModel.isDarkMode.collectAsStateWithLifecycle()
    val tasks by mainViewModel.tasks.collectAsStateWithLifecycle()

    CampusCompanionTheme(darkTheme = isDarkMode) {
        NavHost(
            navController = navController,
            startDestination = CampusRoutes.TASKS,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(CampusRoutes.TASKS) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TasksScreen(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { mainViewModel.toggleDarkMode(it) },
                        tasks = tasks,
                        onToggleTask = { mainViewModel.toggleTaskCompletion(it) },
                        onAddTask = { title, description -> mainViewModel.upsertTask(title, description) },
                        onNavigateToEvents = { navController.navigate(CampusRoutes.EVENTS) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
            composable(CampusRoutes.EVENTS) {
                // Scoped to this back stack entry, so leaving Events clears its ViewModel.
                val eventsViewModel: CampusEventViewModel = viewModel(factory = eventsViewModelFactory)
                EventsScreen(
                    viewModel = eventsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private val eventsViewModelFactory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = EventsRepository(EventsRemoteDataSource(RetrofitClient.campusApi))
        return CampusEventViewModel(repository) as T
    }
}
