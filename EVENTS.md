# Campus events

Run Campus Companion and select **Events** above the Tasks screen. Refresh fetches the feed again. Tasks and the existing theme preference continue to work.

## API contract

GET https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/event

Response: JSON array. Required fields: id, name, startingDate. Optional fields: endingDate, description, address, avatar, createdAt. The app displays name, start/end dates, address and description. Portrait URLs are parsed but not displayed. Dates use the device timezone.

## Architecture

CampusApi (suspend Retrofit GET) -> EventsRemoteDataSource -> EventsRepository -> EventsViewModel StateFlow -> EventsScreen.

The repository validates required fields, dates and unique IDs, formats dates, supplies optional field fallbacks and sorts the feed. Compose renders state and sends callbacks; it does not create Retrofit or call endpoints.

Loading, Content, Empty and Error are distinct states. Connection/timeouts, HTTP rejection/server failures and malformed data have appropriate messages. Retry performs a fresh request. Cancellation is propagated and overlapping requests are ignored. No caching or authentication is added.

## Build and checks

Open the repository in Android Studio, sync Gradle and run app. Use SDK 37 and the Android Studio bundled JDK. The existing minimum API 24 is retained using core library desugaring for java.time.

Run `gradlew.bat testDebugUnitTest assembleDebug` in one command. EventsTest uses MockWebServer to test the actual GET path, JSON mapping, empty arrays, missing fields, invalid dates, HTTP 500, connection failure messages and ViewModel recovery after Retry.

Manual checks: open Events with internet enabled; disable connectivity and Refresh; restore connectivity and Retry. An empty JSON array should display No events yet. Automated fixtures cover empty/malformed responses without modifying the shared API.
