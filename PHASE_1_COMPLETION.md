# Phase 1 Completion Record

## Scope
Android foundation and device enrollment.

## Definition of Done

- [x] Standalone Android application project.
- [x] Package/application ID: `com.dellybizz.notificationvault`.
- [x] Minimum Android 8.0 / API 26; target/compile API 35.
- [x] Phone-compatible layout.
- [x] Large-screen Android/Chromebook adaptive layout.
- [x] Device token enrollment UI.
- [x] Token format validation before network request.
- [x] Server validation through `POST /api/device/profile`.
- [x] Invalid/revoked device token handling.
- [x] Token encrypted at rest with Android Keystore AES/GCM.
- [x] Raw token is not written to logs or plaintext preferences.
- [x] Device name and type shown from server profile.
- [x] Record permission shown from server profile.
- [x] Viewable source devices shown from server profile.
- [x] Device with no view grants gets an explicit no-access state.
- [x] Refresh pulls permissions from server again.
- [x] Basic settings screen/actions.
- [x] Open admin dashboard action.
- [x] Forget local device credential action.
- [x] HTTPS-only app networking configuration.
- [x] GitHub Actions workflow to build and upload debug APK.
- [x] README documents enrollment and build process.

## Deliberately deferred to Phase 2

- NotificationListenerService.
- Notification Access onboarding.
- SQLite notification queue/history.
- Background/batch sync to `/api/ingest`.
- Reboot/network recovery.

## Validation note

The source has been statically checked in the current workspace. GitHub Actions is the authoritative Android compile for Phase 1 and installs Android SDK 35 before building the debug APK.
