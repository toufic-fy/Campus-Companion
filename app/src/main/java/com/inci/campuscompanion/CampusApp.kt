package com.inci.campuscompanion

import android.net.Uri
import androidx.navigation.navDeepLink
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavType
import com.inci.campuscompanion.ui.tasks.EventUpdateSettings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.inci.campuscompanion.worker.RefreshEventWorker

@Composable
fun CampusApp(notificationUri: Uri? = null, onLinkConsumed: () -> Unit = {}) {
    val context = LocalContext.current.applicationContext
    val database = remember { AppDatabase.getDatabase(context) }
    val preferences = remember { UserPreferencesRepository(context.dataStore) }
    val tasks: TasksViewModel = viewModel(factory = TasksViewModel.Factory(
        preferences, TaskRepository(database.taskDao())))
    val events = remember { EventsRepository(EventsRemoteDataSource(RetrofitClient.campusApi), database) }
    val workManager = remember { WorkManager.getInstance(context) }
    val isDarkMode by tasks.isDarkMode.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    LaunchedEffect(Unit) { RefreshEventWorker.enqueueUniquePeriodicWork(context) }
    CampusCompanionTheme(darkTheme = isDarkMode) {
        Scaffold(Modifier.fillMaxSize()) { padding ->
            AppNavHost(navController, tasks, events, workManager, isDarkMode, Modifier.padding(padding))
            LaunchedEffect(notificationUri) {
                notificationUri?.let {
                    navController.navigate(
                        request = NavDeepLinkRequest.Builder.fromUri(it).build(),
                        navOptions = navOptions {
                            popUpTo("tasks") { inclusive = false }
                            launchSingleTop = true
                        }
                    )
                    onLinkConsumed()
                }
            }
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    tasks: TasksViewModel,
    eventsRepository: EventsRepository,
    workManager: WorkManager,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    NavHost(navController, startDestination = "tasks", modifier = modifier) {
        composable("tasks") {
            TaskRoute(
                viewModel = tasks,
                isDarkMode = isDarkMode,
                onToggleDarkMode = tasks::toggleDarkMode,
                onOpenEvents = { navController.navigate("events") { launchSingleTop = true } },
                notificationSettings = { EventUpdateSettings() }
            )
        }
        composable(
            route = "events?eventId={eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            deepLinks = listOf(navDeepLink { uriPattern = "campus://events/{eventId}" })
        ) { entry ->
            val events: CampusEventViewModel = viewModel(factory =
                CampusEventViewModel.Factory(eventsRepository, workManager))
            EventsScreen(viewModel = events, onBack = { navController.popBackStack() }, selectedEventId = entry.arguments?.getString("eventId"))
        }
    }
}
