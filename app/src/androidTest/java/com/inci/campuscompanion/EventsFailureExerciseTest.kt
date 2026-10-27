package com.inci.campuscompanion

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.inci.campuscompanion.data.*
import com.inci.campuscompanion.ui.*
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/** Requires an emulator with internet. Temporarily disables its radios, restoring them in finally. */
class EventsFailureExerciseTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).readBytes()
        }
    }

    private fun evidence(name: String, state: EventsUiState) {
        compose.waitForIdle()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "exercise")
        directory.mkdirs()
        // Capture the actual Compose app window separately from Android system overlays.
        when (state) {
            is EventsUiState.Error -> {
                compose.onNodeWithText(state.message).assertIsDisplayed()
                compose.onNodeWithText("Retry").assertIsDisplayed()
            }
            EventsUiState.Empty -> compose.onNodeWithText("No campus events yet. Refresh to check again.").assertIsDisplayed()
            is EventsUiState.Content -> compose.onNodeWithText(state.events.first().title).assertIsDisplayed()
            else -> Unit
        }
        val appBitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "${name}_app.png").outputStream().use { appBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        requireNotNull(bitmap)
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val result = when (state) {
            is EventsUiState.Content -> "Content(${state.events.size} events)"
            is EventsUiState.Error -> "Error(${state.message})"
            else -> state.toString()
        }
        File(directory, "observations.txt").appendText("$name: $result\n")
        Log.i("CampusExercise", "$name: $result")
    }

    @Test fun fourFailureScenariosAndRecovery() {
        val live = CampusApi.create()
        var delegate = live
        val routingApi = object : CampusApi {
            override suspend fun getEvents(): List<EventDto> = delegate.getEvents()
        }
        lateinit var vm: EventsViewModel
        compose.runOnUiThread { vm = EventsViewModel(EventsRepository(EventsRemoteDataSource(routingApi))) }
        compose.setContent {
            val state by vm.state.collectAsState()
            CampusCompanionTheme(darkTheme = false) { EventsScreen(state, vm::refresh) }
        }
        fun awaitContent() = compose.waitUntil(60_000) { vm.state.value is EventsUiState.Content }
        fun awaitError() = compose.waitUntil(30_000) { vm.state.value is EventsUiState.Error }
        try {
            awaitContent()
            evidence("01_live_content", vm.state.value)
            shell("svc wifi disable")
            shell("svc data disable")
            // Wait for Android to apply the radio change before starting a new call.
            Thread.sleep(2000)
            compose.onNodeWithText("Refresh").performClick()
            awaitError()
            evidence("02_disconnected_network", vm.state.value)
            shell("svc wifi enable")
            shell("svc data enable")
            Thread.sleep(3000)
            compose.onNodeWithText("Retry").performClick()
            awaitContent()
            evidence("03_network_restored_retry", vm.state.value)

            delegate = CampusApi.create("https://campus-events.invalid/")
            compose.onNodeWithText("Refresh").performClick()
            awaitError()
            evidence("04_broken_base_url", vm.state.value)

            // Controlled HTTP response exercises real Retrofit/Gson parsing without editing the shared API.
            val emptyClient = OkHttpClient.Builder().addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body("[]".toResponseBody("application/json".toMediaType())).build()
            }.callTimeout(20, TimeUnit.SECONDS).build()
            delegate = CampusApi.create(client = emptyClient)
            compose.onNodeWithText("Retry").performClick()
            compose.waitUntil(30_000) { vm.state.value is EventsUiState.Empty }
            evidence("05_empty_json", vm.state.value)

            delegate = CampusApi.create("https://campus-events.invalid/")
            compose.onNodeWithText("Refresh").performClick()
            awaitError()
            delegate = live
            compose.onNodeWithText("Retry").performClick()
            awaitContent()
            evidence("06_valid_endpoint_restored_retry", vm.state.value)
        } finally {
            delegate = live
            shell("svc wifi enable")
            shell("svc data enable")
        }
    }
}
