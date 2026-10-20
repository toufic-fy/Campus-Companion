# Campus Events workshop

Open the Events tab to fetch the read-only feed. Refresh starts a new request.

## Follow one refresh

1. EventsScreen reports Refresh or Retry to EventsViewModel.
2. The ViewModel sets Loading and calls EventsRepository in viewModelScope.
3. EventsRemoteDataSource calls CampusApi's suspend GET events/event operation.
4. Retrofit fetches the HTTPS response and decodes EventDto records.
5. The repository maps name, startingDate, and address to CampusEvent's title, timeLabel, and locationLabel. A missing address becomes TBA.
6. The ViewModel exposes Content, Empty, or Error. Compose renders that state.

Only the fields needed by the feed are decoded; extra API fields are ignored.
No cache, sync, authentication, pagination, or background refresh is added.

## Failure exercise

- With the app running, disconnect the emulator's network and tap Refresh. Expect a connection error and Retry, without a crash.
- Temporarily change CampusApi's base URL to an unreachable HTTPS host, rebuild, and refresh. Expect a retryable connection error.
- Using a local test response of [], expect No campus events yet. Do not delete events from the shared API to test this.
- Restore the original endpoint/network and tap Retry. Expect the event list to return.
- HTTP 4xx and 5xx have distinct error messages; malformed or invalid event data produces a safe error message and is logged.

The repository tests check mapping, missing location, empty content, connection failure propagation, and invalid data. The emulator exercises above remain manual checks.
