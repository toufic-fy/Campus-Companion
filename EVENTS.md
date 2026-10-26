# Campus events

Open the **Events** tab to load the current campus events feed. The existing Tasks tab and dark-mode preference remain available. Use **Refresh** to request the feed again.

## API and data flow

The app makes `GET https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/event`. The response is a JSON array with `id`, `name`, and `startingDate`; `endingDate`, `description`, `address`, `avatar`, and `createdAt` are optional.

`CampusApi` calls the endpoint through Retrofit. `EventsRemoteDataSource` isolates the service, and `EventsRepository` validates and maps the response into `CampusEvent` values. `EventsViewModel` exposes Loading, Content, Empty, and Error states. Compose receives only the state and refresh callback.

Dates are displayed in the device timezone. Network, HTTP, and malformed-data failures show a safe message and Retry action. The feature does not add caching, authentication, or background synchronization.

## Checks

Run `gradlew.bat testDebugUnitTest assembleDebug`. Repository tests cover the request path, response mapping, sorting, empty responses, malformed dates, and server errors.

For a manual check, open Events with an internet connection, refresh after disconnecting to see the error and Retry action, then restore connectivity and retry. A successful empty response should show the empty state.
