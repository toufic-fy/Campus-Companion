package com.inci.campuscompanion.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.inci.campuscompanion.TaskScreen
import com.inci.campuscompanion.ui.EventsScreen
import com.inci.campuscompanion.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    mainViewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.TASKS
    ) {
        composable(Routes.TASKS) {
            val isDarkMode by mainViewModel.isDarkMode.collectAsStateWithLifecycle()
            val tasks by mainViewModel.tasks.collectAsStateWithLifecycle()

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                TaskScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { mainViewModel.toggleDarkMode(it) },
                    tasks = tasks,
                    onToggleTask = { mainViewModel.toggleTaskCompletion(it) },
                    onAddTask = { title, description ->
                        mainViewModel.upsertTask(title, description)
                    },
                    // navigate() pushes Events on top of Tasks in the back stack
                    onOpenEvents = { navController.navigate(Routes.EVENTS) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // The deep link lets a notification tap open Events directly.
        // NavHost builds Tasks underneath it, so Back still returns to Tasks.
        composable(
            route = Routes.EVENTS,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.EVENTS_DEEP_LINK })
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { Text("Events") },
                        navigationIcon = {
                            // Same result as the system Back button: pop Events off the stack
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Tasks"
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    EventsScreen()
                }
            }
        }
    }
}
