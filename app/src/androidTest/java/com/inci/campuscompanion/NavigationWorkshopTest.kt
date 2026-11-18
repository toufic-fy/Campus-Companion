package com.inci.campuscompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import org.junit.Rule
import org.junit.Test

class NavigationWorkshopTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun tasksEventsToolbarAndSystemBack() {
        compose.onNodeWithText("Campus events").assertIsDisplayed().performClick()
        compose.onNodeWithText("Campus Events").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Campus events").assertIsDisplayed().performClick()
        compose.onNodeWithText("Campus Events").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithText("Campus events").assertIsDisplayed()
    }
}
