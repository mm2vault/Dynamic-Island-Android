# Dynamic Island Android

An open-source Android overlay inspired by the Dynamic Island pill, built with Kotlin and Jetpack Compose.

## Features
- Canvas-drawn floating capsule with purple rim light and a soft glow, centred on the top display cutout when Android reports one.
- Tap to expand: battery ring, percentage and charging / full / discharging status, all from real Android battery broadcasts.
- Premium dark-purple home screen with a clearly labelled sample preview, permission status and start / stop control.
- Settings (DataStore): capsule width, height, vertical offset, animation intensity and glow. Changes apply to the running capsule immediately; reset restores defaults.
- Turkish UI, all text in string resources.
- No internet, SMS, contacts, microphone, location, or notification-listener permission.

## Compatibility
Targets Android 8.0 (API 26) and newer. Xiaomi, Redmi, POCO and other manufacturers may apply background restrictions, so behavior can vary by device. The user must explicitly grant Display over other apps. Battery optimization and autostart settings may also affect persistence on some devices.

## Build
GitHub Actions runs the unit tests and builds the debug APK on pushes to main. Each successful build is published as a GitHub Release with a direct `Dynamic-Island.apk` asset and a separate `SHA256SUMS.txt`; no ZIP extraction is needed.

For local builds with JDK 17 and Gradle 8.9:
~~~bash
gradle testDebugUnitTest assembleDebug
~~~

## Permissions and privacy
The app does not request internet access and does not read or transmit notification contents. The overlay permission is required to draw the capsule above other apps. Android shows a persistent foreground-service notification while the island is running.

## Not implemented yet
Notification previews, music-session controls/artwork, call-state integration, a timer shortcut, and automatic adaptation to every device's camera/notch are not included in this starter build. These should be implemented as separate, explicitly permissioned features and tested on real devices before being advertised as available. Removing notification access from this build is intentional: it avoids requesting a sensitive permission that is not needed for the current battery/overlay features.
