package com.inci.campuscompanion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(state: EventsUiState, onRefresh: () -> Unit) {
 Scaffold(topBar = { TopAppBar(title = { Text("Campus Events") }, actions = {
  TextButton(onClick = onRefresh, enabled = state !is EventsUiState.Loading) { Text("Refresh") }
 }) }) { padding ->
  Box(Modifier.fillMaxSize().padding(padding)) {
   when (state) {
    EventsUiState.Loading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
     CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text("Loading events…")
    }
    EventsUiState.Empty -> StatusMessage("No events yet", "Check back later for upcoming campus events.", "Refresh", onRefresh)
    is EventsUiState.Error -> StatusMessage("Unable to load events", state.message, "Retry", onRefresh)
    is EventsUiState.Content -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
     item { Text("${state.events.size} events • Times shown in your device timezone", style = MaterialTheme.typography.labelLarge) }
     items(state.events, key = { it.id }) { event ->
      ElevatedCard(Modifier.fillMaxWidth()) {
       Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(event.title, style = MaterialTheme.typography.titleLarge)
        Text("Starts: ${event.timeLabel}", style = MaterialTheme.typography.bodyMedium)
        Text("Ends: ${event.endLabel}", style = MaterialTheme.typography.bodyMedium)
        Text(event.locationLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider()
        Text(event.description, style = MaterialTheme.typography.bodyMedium)
       }
      }
     }
    }
   }
  }
 }
}
@Composable
private fun StatusMessage(title: String, message: String, action: String, onAction: () -> Unit) {
 Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
  Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
   Text(title, style = MaterialTheme.typography.titleLarge)
   Text(message)
   Button(onClick = onAction) { Text(action) }
  }
 }
}
