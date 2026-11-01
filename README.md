# MuruFloat

**MuruFloat — a compact Android floating-app launcher.**

A native Kotlin Android project with an edge-anchored launcher handle, installed-app discovery, draggable app shortcut bubble, Shizuku permission flow, and a GitHub Actions APK build.

> This is an early, testable foundation—not a promise that every Android ROM can force every app into a resizable window. Freeform windowing is controlled by Android and the device manufacturer.

## What's included

- Native Android project (Kotlin, Android Views, Gradle).
- MuruFloat launcher icon and a short animated loading screen.
- Installed launcher-app discovery using the phone's package manager. App names and icons come from the device.
- Game filtering using Android app category plus common name/package hints. It is a best-effort filter, not a perfect game classifier.
- Edge handle overlay. It is fixed to the left edge and can only be dragged vertically; it cannot be moved into the middle of the display.
- Floating app launcher with search and category filters.
- Shizuku binder detection, permission request, and status refresh when Shizuku disconnects/restarts.
- Freeform launch attempt through Shizuku shell: `am start --windowingMode 5`.
- Normal full-screen fallback when freeform is rejected (configurable).
- App shortcut bubble using the last selected app's real icon. The bubble can be dragged anywhere; tap it to reopen that app; long-press it to dismiss it.
- Settings for overlay, bubble, game filtering, freeform requests, fallback, animations, haptics, compact list, left/right edge placement, and handle opacity.
- A setup checklist for Shizuku authorization, overlay permission, notification permission, and battery-management guidance.
- A small launch-progress indicator while the Shizuku launch request is being processed.
- Foreground service notification while floating controls are active.
- GitHub Actions workflow that builds a debug APK and uploads it as an artifact.

## Important Android behavior / honest limitations

1. **Overlay is not an embedded app window.** MuruFloat can place its own handle, launcher panel, and shortcut bubble above other apps. Android does not let an ordinary app simply place another app's live UI inside its own view hierarchy.
2. **Freeform is ROM-dependent.** Shizuku can request shell-level operations, but it cannot add freeform support if the firmware disables or removes it. A successful `am start --windowingMode 5` request still needs to be checked on the actual device. Some OEM builds may ignore the request or open the app full-screen.
3. **Window chrome belongs to Android.** The initial build does not draw a custom `X` or minimize button over another app's title bar, and it cannot reliably resize/close an arbitrary third-party task. When freeform is supported, use the system's own window controls. The MuruFloat bubble is a draggable shortcut to the last selected app, not a claim that the foreign task was minimized into Android's system bubble stack.
4. **Full-screen action.** In the app list, tap an app to request a freeform window; long-press an app to request a normal/full-screen launch. The app list is populated from launcher activities currently installed on the device, not from a hard-coded social-media catalogue.
5. **Shizuku is not root.** ADB/shell permissions vary by Android release. MuruFloat checks the Shizuku binder and authorization before using the shell launch request. The app does not bypass Android's permission model.
6. **Background survival is not guaranteed.** Android and OEM battery managers can stop services. The persistent foreground-service notification makes the active state visible, but it cannot guarantee permanent survival.
7. **Package inventory is sensitive.** `QUERY_ALL_PACKAGES` is included because the core purpose is to list/launch installed apps. Google Play restricts broad package visibility; this project is intended for personal sideload/testing unless the permission use is separately approved for distribution.

## Build APK with GitHub (phone-friendly)

1. Create a new GitHub repository named `MuruFloat`.
2. Upload **the contents of this folder** to the repository root (not an extra nested folder).
3. Open the repository's **Actions** tab.
4. Select **Build MuruFloat APK** and press **Run workflow**, or push a commit to `main`/`master`.
5. Open the completed workflow run.
6. Download the `MuruFloat-debug-apk` artifact and extract it. The installable file is `app-debug.apk`.
7. Install the APK on the phone. Android may ask you to allow installation from the file manager/browser you used.

The workflow uses JDK 17, Gradle 8.13, Android SDK platform 36, and Build Tools 36.0.0. The APK is a debug-signed installable build; no keystore or secret is needed.

## First-run setup

1. Install and start Shizuku separately. On a non-rooted Android 11+ phone, Shizuku is normally started through Wireless debugging and needs to be started again after a reboot.
2. Open MuruFloat and tap the Shizuku row in **Settings → Setup checklist**. Start Shizuku, return, then tap the row again to request MuruFloat authorization.
3. Grant **Display over other apps** when Android opens its permission page.
4. Allow notifications so Android can show the foreground-service status. The overlay may still run if notification permission is denied, but its visibility is reduced.
5. Enable the floating overlay from Overview. A narrow handle appears on the left or right edge; drag it only vertically.
6. Tap the handle to see installed apps. Tap an app to request freeform; long-press an app to open it full-screen.
7. For freeform testing, open Android Developer options and look for an OEM setting such as **Enable freeform windows** if the phone firmware provides it. Reboot if that setting asks you to. Availability and wording vary by ROM.

Official Shizuku download and developer documentation:
- https://shizuku.rikka.app/download/
- https://github.com/RikkaApps/Shizuku-API

## Project configuration

- Application ID: `app.murufloat.mobile` (debug variant: `app.murufloat.mobile.debug`)
- Minimum Android: API 26 (Android 8.0), because `TYPE_APPLICATION_OVERLAY` is used.
- Compile SDK: 36
- Target SDK: 35
- Shizuku API: 13.1.5
- Gradle: 8.13
- Android Gradle Plugin: 8.11.1
- Kotlin Gradle Plugin: 2.2.20

## Local build

With JDK 17 and Android SDK 36 installed:

```bash
gradle --no-daemon :app:assembleDebug
```

On the GitHub Actions runner, Gradle 8.13 is installed by `gradle/actions/setup-gradle`, so the workflow does not depend on a checked-in wrapper JAR. The included `gradlew` helper downloads Gradle 8.13 on a machine that has `curl`/`wget` and `unzip`; use the GitHub workflow if building from a phone.

## License

The MuruFloat project source is released under the MIT License. Shizuku API remains under its own upstream license; see the Shizuku-API repository.
