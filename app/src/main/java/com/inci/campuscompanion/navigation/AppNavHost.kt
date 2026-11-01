package com.inci.campuscompanion.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inci.campuscompanion.ui.EventsPlaceholderScreen
import com.inci.campuscompanion.ui.TasksScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.TASKS,
        modifier = modifier
    ) {
        composable(Routes.TASKS) {
            TasksScreen(
                onOpenEvents = { navController.navigate(Routes.EVENTS) }
            )
        }
        composable(Routes.EVENTS) {
            EventsPlaceholderScreen()
        }
    }
}
