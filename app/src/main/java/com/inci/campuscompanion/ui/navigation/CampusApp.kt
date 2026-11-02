package com.inci.campuscompanion.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.inci.campuscompanion.ui.view_model.CampusEventViewModel
import com.inci.campuscompanion.ui.view_model.MainViewModel

@Composable
fun CampusApp(
    mainViewModel: MainViewModel,
    eventViewModel: CampusEventViewModel
) {
    val navController = rememberNavController()
    AppNavHost(
        navController = navController,
        mainViewModel = mainViewModel,
        eventViewModel = eventViewModel
    )
}
