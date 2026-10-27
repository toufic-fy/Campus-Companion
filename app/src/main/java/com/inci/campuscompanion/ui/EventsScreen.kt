package com.inci.campuscompanion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag

@Composable
fun EventsScreen(state: EventsUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Campus Events", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onRefresh, enabled = state !is EventsUiState.Loading) {
                Text("Refresh")
            }
        }
        when (state) {
            EventsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            EventsUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No campus events yet.")
            }
            is EventsUiState.Error -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.message)
                Button(onClick = onRefresh, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
            }
            is EventsUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("events-content"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(state.events, key = { it.id }) { event ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
