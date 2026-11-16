
package com.inci.campuscompanion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import com.inci.campuscompanion.ui.MainViewModel
import com.inci.campuscompanion.ui.events.EventsScreen
import com.inci.campuscompanion.ui.events.EventsViewModel


import androidx.compose.foundation.layout.Column
import com.inci.campuscompanion.notifications.EventUpdatesToggle

import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.padding
import androidx.navigation.navDeepLink
import kotlinx.coroutines.launch
import com.inci.campuscompanion.notifications.showTestEventNotification
import androidx.compose.ui.unit.dp


@Composable
fun CampusApp(
    viewModel: MainViewModel,
    eventsViewModel: EventsViewModel
) {
    val navController = rememberNavController()

    AppNavHost(
        navController,
        viewModel,
        eventsViewModel

    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    eventsViewModel: EventsViewModel
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    val eventsState by eventsViewModel.uiState.collectAsStateWithLifecycle()

    val refreshStatus by eventsViewModel.refreshStatus.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    NavHost(
        navController = navController,
        startDestination = "tasks",
        modifier = Modifier.fillMaxSize().safeDrawingPadding()
    ) {
        composable("tasks") {
            TaskScreen(
                isDarkMode = isDarkMode,
                onToggleDarkMode = {
                    viewModel.toggleDarkMode(it)
                },
                tasks = tasks,
                onToggleTask = {
                    viewModel.toggleTaskCompletion(it)
                },
                onAddTask = { title, description ->
                    viewModel.upsertTask(title, description)
                },
                onOpenEvents = {
                    navController.navigate("events")
                }
            )
        }


        composable(
            route = "events",
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "campus://events"
                }
            )
        ) {

        Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Campus Events")
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    navController.popBackStack()
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                }

            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    EventUpdatesToggle()

                    Button(
                        onClick = {
                            scope.launch {
                                val sent = showTestEventNotification(context)

                                Toast.makeText(
                                    context,
                                    if (sent) {
                                        "Test notification sent!"
                                    } else {
                                        "Enable Event notifications and allow permission first"
                                    },
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text("Send test notification")
                    }


                    EventsScreen(
                        uiState = eventsState,
                        refreshStatus = refreshStatus,
                        onRetry = {
                            eventsViewModel.refreshEvents()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

        }
    }
}
