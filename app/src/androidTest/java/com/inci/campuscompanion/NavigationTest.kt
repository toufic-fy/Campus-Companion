package com.inci.campuscompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.inci.campuscompanion.navigation.AppNavHost
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNavigationTasksToEventsAndBackToTasks() {
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            CampusCompanionTheme {
                AppNavHost(navController = navController)
            }
        }

        // Verify start destination is Tasks Screen
        composeTestRule.onNodeWithText("Tasks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open Events").assertIsDisplayed()

        // Navigate to Events Screen
        composeTestRule.onNodeWithText("Open Events").performClick()

        // Verify Events placeholder destination is displayed
        composeTestRule.onNodeWithText("Events").assertIsDisplayed()

        // Pop back stack to return to Tasks Screen
        composeTestRule.runOnIdle {
            navController.popBackStack()
        }

        // Assert Tasks Screen is displayed again
        composeTestRule.onNodeWithText("Tasks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open Events").assertIsDisplayed()
    }
}
