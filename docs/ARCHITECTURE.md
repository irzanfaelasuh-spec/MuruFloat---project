# MuruFloat architecture

## App process

- `MainActivity` renders the splash, overview, installed-app browser, permissions checklist, and settings.
- `AppCatalog` queries launchable activities through `PackageManager` and returns their real labels/icons.
- `AppLauncher` performs a normal Activity launch or delegates a freeform/fullscreen request to `ShizukuShell`.

## Shizuku bridge

- The Shizuku provider is declared with the app-specific `${applicationId}.shizuku` authority.
- MuruFloat checks both the Shizuku binder and the app's Shizuku permission before attempting the shell operation.
- `ShizukuShell` binds a Shizuku `UserService` using `Shizuku.UserServiceArgs`.
- `ShellUserService` implements the narrow AIDL method `launchActivity`; it accepts validated package/activity identifiers and only windowing modes 1 (fullscreen) or 5 (freeform).
- The service invokes Android's Activity Manager command. It does not expose a general-purpose shell command API to the UI.

## Overlay process

- `FloatingService` is a user-started special-use foreground service.
- The edge handle is `TYPE_APPLICATION_OVERLAY`, fixed to the selected left/right edge, with Y-only dragging.
- The launcher panel and launch-progress chip are app-owned overlays.
- The app bubble is a draggable shortcut to the last successfully selected app. It is not a system task bubble and does not own the external app's task lifecycle.

## Platform boundaries

MuruFloat cannot put another app's live view inside its own view tree. It requests native Android freeform launch through Shizuku. The ROM must support freeform and the selected app must tolerate that windowing mode. Android owns the external task's title bar and resize/close controls.
