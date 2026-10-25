
package com.inci.campuscompanion.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EventsScreen(
    uiState: EventsUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Campus Events",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(onClick = onRetry) {
                Text("Refresh")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState) {

            EventsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            EventsUiState.Empty -> {
                Text("No events available.")
            }

            is EventsUiState.Error -> {
                Column {
                    Text("Error: ${uiState.message}")

                    Spacer(Modifier.height(12.dp))

                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }

            is EventsUiState.Content -> {
                LazyColumn {
                    items(
                        items = uiState.events,
                        key = { it.id }
                    ) { event ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(Modifier.height(8.dp))

                                Text("Date: ${event.timeLabel}")
                                Text("Location: ${event.locationLabel}")
                            }
                        }
                    }
                }
            }
        }
    }
}
