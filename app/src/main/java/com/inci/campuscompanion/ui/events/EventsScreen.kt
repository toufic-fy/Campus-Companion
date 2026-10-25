package com.inci.campuscompanion.ui.events

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme


@Composable
fun EventsScreen(modifier: Modifier = Modifier){
    CampusCompanionTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            EventsScreen(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CampusCompanionTheme {
        Greeting("Android")
    }
}