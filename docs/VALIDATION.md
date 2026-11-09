# Validation record — NOVA 0.1.0

Development validation on 9 November 2026. Physical devices were **not available**. This is a functional preview, not a claim of production certification.

## Local checks

- `:core:test`: 16 JVM tests pass (serialization, backup limits, layout operations, search/sort, folder cleanup, suggestion consent/retention/reset).
- `:app:lintDebug`: passes; dependency-update and compatibility/deprecation warnings are not suppressed as errors.
- `:app:assembleDebug` and `:app:assembleDebugAndroidTest`: pass.
- 20 Android/Compose tests pass (29.750 s) on Android 16/API 36.1 arm64 emulator: HOME registration, real installed-app discovery/launch/search/pin, persistent folders/themes, actual touch drag, swipe gestures, Studio controls, explicit consent/disable, usage permission denial, real AppWidgetHost binding/resize/delete and cancelled binding, static installed icon-pack resource and fallback, real media volume, and theme/control-centre screenshots.
- External force-stop plus HOME relaunch: persisted layout, dock, theme and bound widget pass.
- Actual emulator reboot: persisted layout and widget binding pass; NOVA still resolves as default HOME.
- Real uninstall of the instrumentation widget-provider package removes its saved widget placement; test package reinstalled afterward.
- Screenshots reviewed for AMOLED, dark, light, drawer and control centre. Test fixtures live only in the instrumentation APK; production discovery contains no fake apps.

Run `scripts/emulator-tests.sh` after building both APKs. `scripts/restart-check.py` force-stops and reboots only a serial beginning `emulator-`; do not run it on personal devices. `scripts/profile-emulator.py` records debug cold starts, scrolling frame statistics and PSS. Reports are in ignored `artifacts/` locally and CI report artifacts remotely.

## Known verification gaps

- Physical phones, OEM launchers/gesture navigation integrations, battery consumption, thermal behavior and hardware flashlight output.
- Android 8–14 compatibility across devices, foldables, work profiles, TalkBack navigation and RTL. Minimum SDK 26 is a build contract, not proof that every version has been exercised.
- Vendor widget configuration activities and commercial icon packs. Tested providers and packs are deliberately controlled instrumentation fixtures; dynamic calendars, masks and proprietary icon formats are outside the supported format.
- Documents from every external storage provider, complete installer/uninstaller confirmation flows and OEM restrictions. APK install and uninstall use Android-owned confirmation UI.
- User-owned release signing key: optional workflow is configured, but no signing secret has been supplied and no owner-signed release is claimed.

## Failures fixed during validation

Old Espresso used an unavailable Android 16 reflection API; upgraded AndroidX Test. A long-press child consumed editing drags; disabled child long-press while editing. Fixed widget-size assertion to include Android host padding and switched the external test widget provider to Java so its process does not depend on the test runner's Kotlin classpath. Corrected tests that assumed an English widget approval label and re-launched a task during ActivityScenario teardown. Explicit status-bar contrast assertion and delayed screenshot verify SystemUI updates.
