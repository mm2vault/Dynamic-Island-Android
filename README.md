# Dynamic Island Android

An open-source Android overlay inspired by the Dynamic Island pill, built with Kotlin and Jetpack Compose.

## Features in the current starter
- Floating black capsule with a purple neon accent.
- Tap-to-expand/collapse animation.
- Live battery percentage and charging status.
- Explicit Android settings flow for overlay permission.
- No internet, SMS, contacts, microphone, location, or notification-listener permission.
- GitHub Actions build for a debug APK.

## Compatibility
Targets Android 8.0 (API 26) and newer. Xiaomi, Redmi, POCO and other manufacturers may apply background restrictions, so behavior can vary by device. The user must explicitly grant Display over other apps. Battery optimization and autostart settings may also affect persistence on some devices.

## Build
GitHub Actions builds the debug APK on pushes to main and can be started from Actions → Android CI → Run workflow. Download the artifact named dynamic-island-debug-apk, extract the ZIP, and install app-debug.apk.

For local builds with JDK 17 and Gradle 8.9:
~~~bash
gradle assembleDebug
~~~

## Permissions and privacy
The app does not request internet access and does not read or transmit notification contents. The overlay permission is required to draw the capsule above other apps. Android shows a persistent foreground-service notification while the island is running.

## Not implemented yet
Notification previews, music-session controls/artwork, call-state integration, a timer shortcut, and automatic adaptation to every device's camera/notch are not included in this starter build. These should be implemented as separate, explicitly permissioned features and tested on real devices before being advertised as available. Removing notification access from this build is intentional: it avoids requesting a sensitive permission that is not needed for the current battery/overlay features.
