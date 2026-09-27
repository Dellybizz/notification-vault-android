# Notification Vault Android

Native Android/ChromeOS client for the Notification Vault server.

## Phase 1 status: COMPLETE in source

Phase 1 provides:

- Phone and Android-on-ChromeOS compatible native application shell.
- Enrollment with the one-time device token created in the admin dashboard.
- Device token encrypted at rest with Android Keystore (AES-256-GCM).
- Live `POST /api/device/profile` integration.
- Server-controlled Record permission display.
- Per-source View permission display.
- Clear revoked/invalid-token handling.
- Adaptive phone vs. large-screen/Chromebook layout.
- Basic settings: server endpoint, open admin dashboard, forget local token.
- GitHub Actions debug APK build artifact.

Phase 1 intentionally does **not** capture notifications yet. `NotificationListenerService`, local notification queue, and background upload are Phase 2.

## Backend

Default API base URL:

`https://notification-vault-6lthey.v2.appdeploy.ai`

Configured in `app/build.gradle.kts` as `BuildConfig.API_BASE_URL`.

## Build locally

Use Android Studio with JDK 17 and Android SDK 35, or install Gradle 8.11.1 and run:

```bash
gradle :app:assembleDebug
```

APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions

Every push to `main` builds a debug APK. Open **Actions → Build Phase 1 APK → latest run → Artifacts** and download `notification-vault-phase1-debug-apk`.

The workflow installs Android SDK 35 explicitly before compiling so the hosted build does not depend on the runner image already containing the required platform.

## Enrollment flow

1. Open the Notification Vault admin dashboard.
2. Add a Mobile or Chromebook device.
3. Copy the one-time device token.
4. Install/open this Android client.
5. Paste token and tap **Connect device**.
6. The app validates the token using `/api/device/profile` before storing it.
7. Current Record and View permissions appear in the app.

The device cannot grant itself permissions. All permission values come from the server/admin dashboard.
