
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkManager
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.local.pref.dataStore
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.data.repository.TaskRepository
import com.inci.campuscompanion.ui.events.EventsScreen
import com.inci.campuscompanion.ui.tasks.TaskRoute
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme
import com.inci.campuscompanion.ui.view_model.CampusEventViewModel
import com.inci.campuscompanion.ui.view_model.TasksViewModel
import com.inci.campuscompanion.util.NotificationHelper
import com.inci.campuscompanion.worker.RefreshEventWorker
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val eventNavigationRequest = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.getBooleanExtra("navigate_to_events", false) == true) {
            eventNavigationRequest.value++
        }

        NotificationHelper.createNotificationChannel(this)

        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)

        val taskRepository = TaskRepository(database.taskDao())

        val preferencesRepository =
            UserPreferencesRepository(dataStore)

        val eventsRepository = EventsRepository(
            remoteDataSource = EventsRemoteDataSource(
                RetrofitClient.campusApi
            ),
            eventDao = database.eventDao()
        )

        val tasksViewModel: TasksViewModel by viewModels {
            TasksViewModel.Factory(
                preferencesRepository,
                taskRepository
            )
        }

        val eventsViewModel: CampusEventViewModel by viewModels {
            CampusEventViewModel.Factory(
                eventsRepository = eventsRepository,
                workManager = WorkManager.getInstance(applicationContext)
            )
        }

        RefreshEventWorker.enqueueUniquePeriodicWork(applicationContext)

        setContent {

            val isDarkMode by tasksViewModel.isDarkMode.collectAsStateWithLifecycle()

            val navigationRequest by
            eventNavigationRequest.collectAsStateWithLifecycle()

            CampusCompanionTheme(darkTheme = isDarkMode) {

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    CampusApp(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = tasksViewModel::toggleDarkMode,
                        tasksViewModel = tasksViewModel,
                        eventsViewModel = eventsViewModel,
                        navigationRequest = navigationRequest,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent.getBooleanExtra("navigate_to_events", false)) {
            eventNavigationRequest.value++
        }
    }
}

@Composable
fun CampusApp(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    tasksViewModel: TasksViewModel,
    eventsViewModel: CampusEventViewModel,
    navigationRequest: Int = 0,
    modifier: Modifier = Modifier
) {

    val navController = rememberNavController()

    AppNavHost(
        navController = navController,
        isDarkMode = isDarkMode,
        onToggleDarkMode = onToggleDarkMode,
        tasksViewModel = tasksViewModel,
        eventsViewModel = eventsViewModel,
        navigationRequest = navigationRequest,
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
    navigationRequest: Int = 0,
    modifier: Modifier = Modifier
) {

    LaunchedEffect(navigationRequest) {
        if (navigationRequest > 0) {
            navController.navigate("events") {
                launchSingleTop = true
            }
        }
    }

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
                onNavigateEvents = {
                    navController.navigate("events")
                }
            )
        }

        composable("events") {
            EventsScreen(
                viewModel = eventsViewModel
            )
        }
    }
}
