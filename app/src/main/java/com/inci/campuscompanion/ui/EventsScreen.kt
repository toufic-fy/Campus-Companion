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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Campus Events", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onRefresh, enabled = state !is EventsUiState.Loading) { Text("Refresh") }
        }
        when (state) {
            EventsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Loading events…", Modifier.padding(top = 12.dp))
                }
            }
            EventsUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No campus events yet. Refresh to check again.")
            }
            is EventsUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRefresh, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
                }
            }
            is EventsUiState.Content -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // No server-controlled keys: duplicate IDs must not crash the list.
                items(state.events) { event ->
                    Card(Modifier.fillMaxWidth()) {
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
