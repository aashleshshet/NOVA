# Install and use NOVA

1. Download `nova-debug` from a successful GitHub Actions **Android CI** run and extract `app-debug.apk`. Artifacts may require GitHub sign-in and expire under GitHub's retention policy.
2. Open the APK on the phone. If asked, allow installation from the browser/file manager you used. Android presents the install confirmation. Revoke that install-source access afterward if desired.
3. Open NOVA, then Studio (sliders icon) → set default home. Choose NOVA in Android's chooser. Alternatively open Android Settings → Apps → Default apps → Home app.
4. Open **All apps**. Touch and hold a real app → add to page or dock. Tap the pencil to edit, then hold and drag. Page buttons and dock accept drops. The app action sheet offers move controls too.
5. Swipe down on the clock/empty home area or tap the dashboard icon for control centre. Swipe up opens apps. Change gestures in Studio. Screen edges retain Android navigation gestures.

Studio controls themes, glass, wallpaper, grid, icons, fonts, folders, pages, widgets and icon packs. Widgets have Android binding/configuration consent. Their size/page/order controls are available in editing mode. Install a compatible icon pack normally and select it in Studio.

Control centre reads real battery/connectivity state. Media volume changes the music stream. Flashlight asks for camera permission only when used. Brightness asks for optional special write-settings access and changes to manual brightness when adjusted. Denying these permissions keeps the rest of NOVA working. Internet, Bluetooth and DND open Android-owned controls; NOVA does not replace the notification shade.

## Backup and rollback

Studio → Export backup saves a versioned JSON document to a location you choose. Restore validates the document and asks before replacing the layout. Widgets must be added again because their host IDs belong to an installation; ranking history and consent are not transferred.

To stop using NOVA, open Android Settings → Apps → Default apps → Home app and choose your previous launcher. Your previous launcher and its layout are not removed. Export a NOVA backup before uninstalling; uninstall removes NOVA's private layout. If NOVA cannot open, use the system notification shade's Settings shortcut to change Home app.

## Troubleshooting

- **App not installed / conflicting package:** local and CI debug builds can have different signing certificates. Export backup, uninstall the old build, then install the new one and restore. An owner-signed release uses a different certificate from debug.
- **Missing app:** only enabled, exported launchable activities in the current profile appear. Private/work-profile apps are outside this preview's support. Reopen NOVA after changing installed apps.
- **Widget missing or setup cancelled:** add it again through Studio. A removed provider removes its saved placement. After restore, re-add widgets manually.
- **Icon pack incomplete:** unsupported/missing mappings use original icons. Disable monochrome icons to see pack colors. See ICON_PACKS.md.
- **Search returns nothing:** search matches all words against installed app labels/package names. Clear the query to show all apps.
- **Permission denied:** use Android app settings to grant the optional permission again; there is no requirement to grant usage access to launch apps.
- **Corrupt saved state:** NOVA offers recovery without silently discarding it. Restore a valid backup; reset only if you intend to discard the layout.

Normal installation/use needs no ADB, root or cloud service. Test scripts use ADB solely for development validation on disposable emulators.
