package com.inci.campuscompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.navigation.navDeepLink
import androidx.work.WorkManager
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.data.repository.TaskRepository
import com.inci.campuscompanion.ui.events.EventsScreen
import com.inci.campuscompanion.ui.tasks.TaskRoute
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme
import com.inci.campuscompanion.ui.view_model.CampusEventViewModel
import com.inci.campuscompanion.ui.view_model.TasksViewModel
import com.inci.campuscompanion.worker.RefreshEventWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val taskRepository = TaskRepository(database.taskDao())
        val campusApplication = application as CampusApplication
        val preferencesRepository = campusApplication.preferences

        val tasksViewModel: TasksViewModel by viewModels {
            TasksViewModel.Factory(preferencesRepository, taskRepository)
        }

        val eventsViewModel: CampusEventViewModel by viewModels {
            CampusEventViewModel.Factory(
                application = campusApplication,
                workManager = WorkManager.getInstance(applicationContext)
            )
        }

        RefreshEventWorker.enqueueUniquePeriodicWork(applicationContext)

        setContent {
            val isDarkMode by tasksViewModel.isDarkMode.collectAsStateWithLifecycle()

            CampusCompanionTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CampusApp(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = tasksViewModel::toggleDarkMode,
                        tasksViewModel = tasksViewModel,
                        eventsViewModel = eventsViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CampusApp(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    tasksViewModel: TasksViewModel,
    eventsViewModel: CampusEventViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    AppNavHost(
        navController = navController,
        isDarkMode = isDarkMode,
        onToggleDarkMode = onToggleDarkMode,
        tasksViewModel = tasksViewModel,
        eventsViewModel = eventsViewModel,
        modifier = modifier
    )
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    tasksViewModel: TasksViewModel,
    eventsViewModel: CampusEventViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "tasks",
        modifier = modifier
    ) {
        composable("tasks") {
            TaskRoute(
                viewModel = tasksViewModel,
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onNavigateEvents = { navController.navigate("events") }
            )
        }
        composable(
            "events",
            deepLinks = listOf(navDeepLink { uriPattern = "campus://events/{eventId}" }),
        ) {
            EventsScreen(
                viewModel = eventsViewModel
            )
        }
    }
}
