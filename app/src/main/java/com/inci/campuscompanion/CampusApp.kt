package com.inci.campuscompanion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.inci.campuscompanion.worker.RefreshEventsWorker
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
                val context = LocalContext.current
                val workFlow = remember(context) {
                    WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(RefreshEventsWorker.MANUAL_NAME)
                }
                val work by workFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                Column {
                    TextButton(onClick = { RefreshEventsWorker.refreshNow(context) }) {
                        Text("Refresh when online")
                    }
                    when (work.firstOrNull()?.state) {
                        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> Text("Refresh queued; waiting for network or retry.")
                        WorkInfo.State.RUNNING -> Text("Refreshing saved events…")
                        WorkInfo.State.SUCCEEDED -> Text("Saved events are up to date.")
                        WorkInfo.State.FAILED -> Text("Refresh failed. Try again when connected.")
                        else -> Unit
                    }
                    EventsScreen(state, eventsViewModel::refresh, modifier = Modifier.weight(1f),
                        onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
