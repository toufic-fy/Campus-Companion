package com.inci.campuscompanion

import android.util.Log
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.data.remote.*
import com.inci.campuscompanion.ui.*
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Runs only on the disposable CI emulator; saves actual screens and logcat evidence. */
class FullFailureExerciseTest {
    private val VALID_URL = "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/"
    private fun createApi(baseUrl: String, client: OkHttpClient): CampusApi =
        retrofit2.Retrofit.Builder().baseUrl(baseUrl).client(client)
            .addConverterFactory(kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .asConverterFactory("application/json".toMediaType()))
            .build().create(CampusApi::class.java)

    @get:Rule val compose = createComposeRule()
    private val automation get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    private fun shell(command: String): String = automation.executeShellCommand(command).use { fd ->
        FileInputStream(fd.fileDescriptor).bufferedReader().use { it.readText() }
    }

    private fun capture(name: String, result: String) {
        compose.waitForIdle()
        shell("mkdir -p /sdcard/Download/charbel-events-evidence")
        shell("screencap -p /sdcard/Download/charbel-events-evidence/$name.png")
        Log.i("CampusExercise", "$name: $result")
    }

    private fun restoreConnectivity() {
        shell("cmd connectivity airplane-mode disable")
        shell("svc wifi enable")
        shell("svc data enable")
    }

    @Test fun fourFailureScenariosAndRecovery() {
        var baseUrl = VALID_URL
        var emptyResponse = false
        val client = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                if (emptyResponse) Response.Builder()
                    .request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK")
                    .body("[]".toResponseBody("application/json".toMediaType())).build()
                else chain.proceed(chain.request())
            }.build()
        val api = object : CampusApi {
            override suspend fun getEvents() = createApi(baseUrl, client).getEvents()
        }
        lateinit var vm: EventsViewModel
        compose.runOnUiThread {
            vm = EventsViewModel(EventsRepository(EventsRemoteDataSource(api)))
        }
        compose.setContent {
            val state by vm.uiState.collectAsState()
            CampusCompanionTheme(darkTheme = false) {
                Scaffold { padding -> EventsScreen(state, vm::refresh, Modifier.padding(padding)) }
            }
        }
        fun awaitResult() {
            compose.waitUntil(timeoutMillis = 40_000) { vm.uiState.value !is EventsUiState.Loading }
        }
        fun requireContent() {
            awaitResult()
            assertTrue("Expected Content, got ${vm.uiState.value}", vm.uiState.value is EventsUiState.Content)
        }
        try {
            requireContent()
            capture("00-baseline", "Valid endpoint -> Content")

            // Disable real emulator connectivity. No exception is injected for this scenario.
            shell("cmd connectivity airplane-mode enable")
            shell("svc wifi disable")
            shell("svc data disable")
            Thread.sleep(3_000)
            compose.onNodeWithText("Refresh").performClick()
            awaitResult()
            assertTrue("Expected offline Error, got ${vm.uiState.value}", vm.uiState.value is EventsUiState.Error)
            compose.onNodeWithText("Retry").assertIsDisplayed()
            capture("01-disconnected", "Wi-Fi/data disabled -> ${vm.uiState.value}; Retry visible; app still running")

            restoreConnectivity()
            Thread.sleep(8_000)
            compose.onNodeWithText("Retry").performClick()
            requireContent()
            capture("01-reconnected", "Connectivity restored; Retry -> Content")

            baseUrl = "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/missing/"
            compose.onNodeWithText("Refresh").performClick()
            awaitResult()
            assertTrue(vm.uiState.value is EventsUiState.Error)
            compose.onNodeWithText("Retry").assertIsDisplayed()
            capture("02-invalid-base-url", "Temporary base URL /api/v1/missing/ -> ${vm.uiState.value}; Retry visible")

            baseUrl = VALID_URL
            emptyResponse = true
            compose.onNodeWithText("Retry").performClick()
            awaitResult()
            assertTrue(vm.uiState.value is EventsUiState.Empty)
            compose.onNodeWithText("No campus events yet.").assertIsDisplayed()
            capture("03-empty-array", "Test-only HTTP 200 [] response -> Empty; deliberate message visible")

            emptyResponse = false
            baseUrl = "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/missing/"
            compose.onNodeWithText("Refresh").performClick()
            awaitResult()
            assertTrue(vm.uiState.value is EventsUiState.Error)
            baseUrl = VALID_URL
            compose.onNodeWithText("Retry").performClick()
            requireContent()
            capture("04-restored-retry", "Valid endpoint restored; selected Retry -> Content from live API")
        } finally {
            emptyResponse = false
            baseUrl = VALID_URL
            restoreConnectivity()
        }
    }
}
