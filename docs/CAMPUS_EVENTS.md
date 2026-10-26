# Campus Events feed

## API contract and architecture

The supplied service returns an array at:
`https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/event`.

The real JSON uses `name`, `startingDate`, and `address`, whereas the slides use illustrative names. `EventDto` mirrors the real response. Unknown fields are ignored, required fields are validated, and optional locations fall back to `Location TBA`.

Refresh (and Retry) follows this path:

1. `EventsScreen` invokes its `onRefresh` callback.
2. `EventsViewModel.refresh()` cancels any previous request, sets `Loading`, and launches work in `viewModelScope`.
3. `EventsRepository.getEvents()` calls `EventsRemoteDataSource`.
4. The remote source invokes the suspend Retrofit `CampusApi.getEvents()` method. Retrofit/OkHttp sends `GET event` relative to the configured HTTPS base URL and deserializes the JSON into DTOs.
5. The repository maps each DTO into `CampusEvent`, formatting the UTC start time for the device timezone and supplying a location fallback.
6. The ViewModel publishes `Content`, `Empty`, or a retryable `Error`. Coroutine cancellation is rethrown rather than displayed as an error.
7. `MainActivity` collects state with lifecycle awareness. Compose renders the list, progress indicator, empty message, or error and Retry button.

The network operation does not block the UI thread. The existing Tasks tab remains available. No offline cache or synchronization was added.

## Verification

Run with JDK 17 or newer and the repository's Android SDK requirements:

```powershell
./gradlew.bat testDebugUnitTest assembleDebug
```

`EventsFeedTest` covers DTO mapping through Retrofit, an empty JSON array, an unavailable local socket, HTTP 404 with endpoint restoration and retry, malformed JSON, an invalid date, and HTTP 503.

The local socket test is an automated approximation of a disconnected network; it does not prove that the emulator's network toggle or Compose UI behaves correctly. On-device checks are documented separately in the failure report.

## Manual failure exercise

Capture a screenshot or relevant log after each action. Record the actual result rather than copying these expected results.

1. Load Events with internet available. Disable both Wi-Fi and mobile data on the test device, then select Refresh. Expect a connection error and Retry (the call timeout is 20 seconds).
2. Temporarily change `CampusApiFactory.BASE_URL` to the same HTTPS host with `/api/v1/missing/` as its path. Rebuild, install and open Events. Expect an HTTP error and Retry.
3. In a temporary local test build, make `EventsRemoteDataSource.getEvents()` return `emptyList()` or use a controlled mock endpoint returning HTTP 200 with `[]`. Expect the explicit empty-content message. Restore the remote source immediately afterwards. Do not delete records from the shared class API.
4. Restore the valid base URL and original source, rebuild and install. Disable device connectivity and Refresh to enter Error; restore connectivity and select Retry. Expect Content to return.

Before submission, verify that the valid HTTPS URL and actual remote source are restored, commit the latest code, push the feature branch, and include the PR link in the failure report.
