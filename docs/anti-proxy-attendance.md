## 🛡️ Robust Anti-Proxy Attendance System

This project enforces a **server-side, multi-signal pipeline** for attendance marking. The client is treated as untrusted: all critical checks are performed on the backend, and the database enforces key invariants to prevent race-condition bypasses.

### Endpoints

- `POST /api/student/attendance/mark`: Mark attendance with QR + face + geo + anti-cheat engine.
- `POST /api/student/attendance/register-face`: Register face embedding + liveness.
- `POST /api/student/attendance/geofence/check`: Validate geofence independently.

### 8-Step Security Pipeline (Enforced)

Flow implemented in `StudentAttendanceController` and `AttendanceService`:

1. **QR JWT validation**: signature + token type + claims parsing (`AttendanceQRTokenService`).
2. **Expiry validation**: rejects expired QR.
3. **Token hashing**: hashes QR token for replay and audit (`AttendanceQRTokenService#hashToken`).
4. **Face verification + liveness**: required; failure logs a security violation (`FaceVerificationService`).
5. **Block list**: blocks student when recent violations exceed threshold (`AntiCheatingService#isStudentBlocked`).
6. **Rate limit**: blocks rapid repeated attempts (`AntiCheatingService#detectRapidAttempts`).
7. **Geofence**: must be inside an active campus zone (`GeolocationService` + `CampusLocationRepository`).
8. **VPN / proxy & spoofing signals**:
   - **VPN/proxy mismatch** (best-effort): compares GPS against IP geolocation when enabled (`IpGeolocationService`).
   - **Impossible movement**: detects teleporting speeds for same device fingerprint (`AntiCheatingService#detectImpossibleMovement`).
   - **Device sharing**: flags device fingerprint used by multiple students in short window (`AntiCheatingService#detectDeviceSharing`).
   - Composite scoring and decisioning (`FraudDetectionService`).

### Database Guarantees (Impossible to Bypass)

The `attendance` table has a unique constraint preventing duplicate attendance records:

- Unique: `(student_id, subject_id, attendance_date, tenant_id)`

This guarantees **duplicate guard** even under concurrent requests.

### Token Reuse Guard

The QR token hash is stored as `qr_token_used` and checked before insert:

- Rejects reuse of the same token for the same student/day/tenant (`AttendanceService` + `AttendanceRepository` query).

### VPN / IP Geo Configuration

IP geolocation is optional and must be explicitly enabled:

```properties
app.attendance.ipgeo.enabled=true
app.attendance.ipgeo.base-url=https://ipapi.co
```

If the provider is unavailable, the system fails open (it won’t block solely on missing IP geo).

