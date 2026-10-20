# Mock events server

Serves `events` so the debug build's Events tab has data (`GET http://10.0.2.2:8080/events` from the emulator).

```bash
python -m http.server 8080 --directory mock-server
```

Stop the server to see the Error + Retry state. Events 4 and 5 have missing fields on purpose, to show the DTO mapping defaults.
