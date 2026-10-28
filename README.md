# Dynamic Island Android

An open-source Android overlay inspired by Dynamic Island, built with Kotlin. The project targets a broad range of Android devices using public Android APIs and graceful feature fallbacks.

## Features in this starter
- AMOLED-style floating pill with animated expand/collapse.
- Battery/charging status from system battery broadcasts.
- Optional notification preview through Android's user-enabled Notification Listener access.
- Overlay permission flow with clear in-app setup instructions.
- Timer shortcut opens the system clock/alarm app when available.
- GitHub Actions build workflow for a debug APK.

## Compatibility
Designed for Android 8.0 (API 26) and newer. Device makers may apply additional background restrictions, so no third-party overlay can guarantee identical behavior on every phone. Users must explicitly grant **Display over other apps** and, for notification previews, **Notification access**. Android may hide sensitive notification contents.

## Build
GitHub Actions runs on pushes and can be started manually from **Actions → Android CI → Run workflow**. The generated debug APK is attached to the workflow run as an artifact.

To build locally with Gradle 8.9 and JDK 17:
```bash
gradle assembleDebug
```

## Permissions & privacy
The app does not request internet access, contacts, SMS, microphone, or location. Notification access is optional and controlled by Android Settings. Notification text is used only to update the local island display; this starter does not upload it.

## Current status
This is the initial project foundation. Media-session artwork/transport controls and richer call-state integration are planned follow-up modules and must be implemented and tested on real devices before being advertised as complete.