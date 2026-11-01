# Build validation status

The project package was checked structurally before delivery:

- All Android XML resource and manifest files parse as XML.
- The GitHub Actions workflow parses as YAML.
- The manifest contains the launcher activity, overlay permission, special-use foreground-service declaration, and Shizuku provider.
- The AIDL interface and the two app icon resources are present.
- Kotlin source files were passed through the local Kotlin parser; no Kotlin syntax/parse diagnostics were found.

**A full Android APK compilation was not run in the authoring environment.** It does not have Android SDK platform 36, Android Build Tools, or Gradle installed. The included GitHub Actions workflow installs JDK 17, Gradle 8.13, Android SDK 36, and Build Tools 36.0.0, then runs `:app:assembleDebug`. The first workflow run is the definitive compile check; inspect its build log before installing the artifact.
