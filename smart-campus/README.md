# Smart Campus Mobile (React Native + Expo)

## Setup

```bash
npx create-expo-app smart-campus
cd smart-campus
npm install
npm start
```

## Screens

- `screens/Dashboard.js`
- `screens/Cafeteria.js`
- `screens/Scanner.js`
- `screens/Profile.js`

## API base URL

Edit `services/api.js` and set your backend host.

For Android emulator use `http://10.0.2.2:8080`.
For physical device use your machine LAN IP.

## Notes

- Scanner posts to `/api/student/attendance/scan`
- Diet AI reads `/api/student/diet/suggestion`
- Map/location heartbeat can be extended to stream `/app/location` via WebSocket
