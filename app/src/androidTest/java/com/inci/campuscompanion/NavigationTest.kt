package com.inci.campuscompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun testNavigationTasksToEvents() {
        composeTestRule.setContent {
            CampusCompanionTheme {
                AppNavHost()
            }
        }

        // Verify start destination is Tasks Screen
        composeTestRule.onNodeWithText("Tasks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open Events").assertIsDisplayed()

        // Navigate to Events Screen
        composeTestRule.onNodeWithText("Open Events").performClick()

        // Verify Events placeholder destination is displayed
        composeTestRule.onNodeWithText("Events").assertIsDisplayed()
    }
}
