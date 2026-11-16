package com.inci.campuscompanion.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inci.campuscompanion.TaskScreen
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.notifications.NotificationPermissionCard
import com.inci.campuscompanion.work.EventsRefreshScheduler

@Composable
fun CampusApp(
    mainViewModel: MainViewModel,
    eventsRepository: EventsRepository,
    refreshScheduler: EventsRefreshScheduler,
    openEventsOnStart: Boolean = false
) {
    val navController = rememberNavController()
    AppNavHost(navController, mainViewModel, eventsRepository, refreshScheduler)

    // Opened from a "new events" notification: land on the events screen, with tasks below it.
    var handledDeepLink by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openEventsOnStart) {
        if (openEventsOnStart && !handledDeepLink) {
            handledDeepLink = true
            navController.navigate("events")
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    eventsRepository: EventsRepository,
    refreshScheduler: EventsRefreshScheduler
) {
    NavHost(navController = navController, startDestination = "tasks") {
        composable("tasks") {
            val darkMode by mainViewModel.isDarkMode.collectAsStateWithLifecycle()
            val tasks by mainViewModel.tasks.collectAsStateWithLifecycle()
            Scaffold { padding ->
                TaskScreen(
                    isDarkMode = darkMode,
                    onToggleDarkMode = mainViewModel::toggleDarkMode,
                    tasks = tasks,
                    onToggleTask = mainViewModel::toggleTaskCompletion,
                    onAddTask = { title, description -> mainViewModel.upsertTask(title, description) },
                    onOpenEvents = { navController.navigate("events") },
                    modifier = Modifier.padding(padding)
                )
            }
        }
        composable("events") {
            val eventsViewModel: EventsViewModel = viewModel(
                factory = EventsViewModel.Factory(eventsRepository, refreshScheduler)
            )
            val state by eventsViewModel.uiState.collectAsStateWithLifecycle()
            EventsScreen(
                state = state,
                onRefresh = eventsViewModel::refresh,
                onLoadMore = eventsViewModel::loadMore,
                onBack = { navController.popBackStack() },
                header = { NotificationPermissionCard() }
            )
        }
    }
}
