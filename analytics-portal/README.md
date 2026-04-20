# Smart Campus Analytics Portal

Premium React + Recharts analytics UI with animated cards and a real-time update loop.

## Run

```bash
npm install
npm run dev
```

## Notes

- The dashboard first tries `/api/admin/dashboard/analytics`.
- It subscribes to the Spring SockJS/STOMP stream at `/ws` and listens on `/topic/analytics/live` and `/topic/analytics/feed`.
- If the websocket is unavailable, it keeps the last fetched backend snapshot and reconnects instead of simulating data.
- Set `VITE_API_BASE_URL` and `VITE_WS_URL` if you want to point it at a different backend.
