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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(state: EventsUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Campus Events") },
            navigationIcon = {
                if (onBack != null) IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                TextButton(onClick = onRefresh, enabled = state !is EventsUiState.Loading) { Text("Refresh") }
            },
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
        )
        if (state is EventsUiState.Content && state.refreshMessage != null) {
            Text(state.refreshMessage, modifier = Modifier.padding(16.dp))
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
