
package com.inci.campuscompanion.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EventsScreen(viewModel: EventsViewModel) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Campus Events",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = { viewModel.refresh() }) {
            Text("Refresh")
        }

        Spacer(Modifier.height(16.dp))

        when (val current = state) {
            EventsUiState.Loading -> {
                CircularProgressIndicator()
            }

            is EventsUiState.Content -> {
                LazyColumn {
                    items(current.events) { event ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    event.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(event.timeLabel)
                                Text(event.locationLabel)
                            }
                        }
                    }
                }
            }

            EventsUiState.Empty -> {
                Text("No campus events available.")
            }

            is EventsUiState.Error -> {
                Text(current.message)

                Spacer(Modifier.height(8.dp))

                Button(onClick = { viewModel.refresh() }) {
                    Text("Retry")
                }
            }
        }
    }
}
