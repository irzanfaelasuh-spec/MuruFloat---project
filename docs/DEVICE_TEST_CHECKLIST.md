# Device test checklist

Use this checklist after installing a GitHub Actions APK on the target phone.

## Permissions and connection

- [ ] Start Shizuku and confirm its service is running.
- [ ] Open MuruFloat and confirm the status changes from Offline to Connected.
- [ ] Request authorization and approve MuruFloat in Shizuku.
- [ ] Confirm MuruFloat reports Authorized.
- [ ] Stop/restart Shizuku and confirm MuruFloat reports the disconnected state after returning to the app.
- [ ] Grant Display over other apps.
- [ ] Start the overlay and confirm the foreground notification appears.

## Overlay behavior

- [ ] Confirm the left-edge handle appears.
- [ ] Drag the handle upward and downward; confirm X stays at the edge.
- [ ] Tap the handle and confirm the installed-app launcher opens.
- [ ] Search for an installed app and open it.
- [ ] Confirm the bubble uses the selected app's actual icon.
- [ ] Drag the bubble horizontally and vertically.
- [ ] Tap the bubble and confirm it reopens the last selected app.
- [ ] Long-press the bubble and confirm it disappears smoothly.
- [ ] Turn the overlay off and confirm handle/bubble disappear.

## Freeform compatibility

- [ ] In Developer options, look for an OEM-supported freeform/multi-window option (if present).
- [ ] If enabling freeform requires a reboot, reboot and restart Shizuku before retesting.
- [ ] Open a low-risk app such as Calculator or Browser from MuruFloat.
- [ ] Confirm whether it opens as a resizable window or full-screen.
- [ ] If it opens full-screen, record the device model, OS build, app name, and any Shizuku log/message.
- [ ] If native freeform appears, test the OS-provided resize, maximize, and close controls.

## Performance

- [ ] Test with one floating app first.
- [ ] Test with the intended game/app combination for a few minutes.
- [ ] Watch for battery-manager termination and notification behavior.
- [ ] Do not enable multiple heavy windows on a device that is already struggling with memory.

## Known expected result

The edge overlay and shortcut bubble should work when their Android permissions are granted. Real third-party freeform windows are a separate device capability and may not work on some OEM firmware.
