package com.inci.campuscompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.inci.campuscompanion.events.*
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Retrofit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl("https://6ad70838f71f154ac4879fac.mockapi.io/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val api = retrofit.create(CampusApi::class.java)
        val repository = EventsRepository(api)

        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EventsViewModel(repository) as T
            }
        }
        val viewModel = ViewModelProvider(this, viewModelFactory)[EventsViewModel::class.java]

        setContent {
            CampusCompanionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        EventsFeedScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
