package com.campus.events.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.campus.events.notifications.EVENTS_DEEP_LINK
import com.campus.events.ui.events.EventDetailScreen
import com.campus.events.ui.events.EventsScreen
import com.campus.events.ui.settings.SettingsScreen

object Routes {
    const val EVENTS = "events"
    const val EVENT_DETAIL = "events/{eventId}"
    const val SETTINGS = "settings"
    fun eventDetail(id: String) = "events/$id"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.EVENTS) {
        composable(
            route = Routes.EVENTS,
            deepLinks = listOf(navDeepLink { uriPattern = EVENTS_DEEP_LINK }),
        ) {
            EventsScreen(
                onEventClick = { navController.navigate(Routes.eventDetail(it)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.EVENT_DETAIL,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
            // Tapping a notification lands here; Back returns to the Events list.
            deepLinks = listOf(navDeepLink { uriPattern = "$EVENTS_DEEP_LINK/{eventId}" }),
        ) { entry ->
            EventDetailScreen(
                eventId = entry.arguments?.getString("eventId").orEmpty(),
                onBack = {
                    if (!navController.popBackStack()) navController.navigate(Routes.EVENTS)
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
