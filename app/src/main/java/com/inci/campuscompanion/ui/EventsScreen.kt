package com.inci.campuscompanion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EventsScreen(state: EventsUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Campus Events", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onRefresh, enabled = state !is EventsUiState.Loading) {
                Text("Refresh")
            }
        }
        when (state) {
            EventsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            EventsUiState.Empty -> Column(Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No campus events right now. Check back soon.")
                TextButton(onClick = onRefresh) { Text("Refresh events") }
            }
            is EventsUiState.Error -> Column(Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRefresh) { Text("Retry") }
            }
            is EventsUiState.Content -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.events) { event ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium)
                            Text(event.timeLabel, style = MaterialTheme.typography.bodyMedium)
                            Text(event.locationLabel, style = MaterialTheme.typography.bodyMedium)
                            if (event.description.isNotBlank()) Text(event.description)
                        }
                    }
                }
            }
        }
    }
}
