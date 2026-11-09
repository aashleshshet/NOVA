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
- Android 8–9 and 11–14 compatibility across devices, foldables, work profiles, TalkBack navigation and RTL. Minimum SDK 26 is a build contract, not proof that every version has been exercised.
- Vendor widget configuration activities and commercial icon packs. Tested providers and packs are deliberately controlled instrumentation fixtures; dynamic calendars, masks and proprietary icon formats are outside the supported format.
- Documents from every external storage provider, complete installer/uninstaller confirmation flows and OEM restrictions. APK install and uninstall use Android-owned confirmation UI.
- User-owned release signing key: optional workflow is configured, but no signing secret has been supplied and no owner-signed release is claimed.

## Failures fixed during validation

Old Espresso used an unavailable Android 16 reflection API; upgraded AndroidX Test. A long-press child consumed editing drags; disabled child long-press while editing. Fixed widget-size assertion to include Android host padding and switched the external test widget provider to Java so its process does not depend on the test runner's Kotlin classpath. Corrected tests that assumed an English widget approval label and re-launched a task during ActivityScenario teardown. Explicit status-bar contrast assertion and delayed screenshot verify SystemUI updates.

## Local performance smoke profile

API 36.1 arm64, SwiftShader GPU, debug APK, five independently forced cold starts: 821 / 480 / 660 / 678 / 668 ms (`am start -W TotalTime`, median 668 ms). PSS after opening the drawer was 185,664 KB (~181 MB); RSS 306,364 KB. The small 19-frame sample recorded 8 janky frames (42.11%) and 42 ms median frame time. Sixteen swipe inputs were sent, but the installed-app catalog fits much of the viewport, so this is insufficient evidence for sustained scrolling performance. No 60/120 Hz or low-memory guarantee is claimed. A real-device release-build trace and a larger installed-app catalog remain necessary.

The first startup measurement produced zero because Android automatically restarted the selected HOME after force-stop. The profiling script now temporarily selects another actual HOME, asserts `LaunchState: COLD`, then restores the original default. The invalid zero measurements are excluded.

Optimized R8 build smoke: signature verified with a disposable debug certificate, installation/launch succeeded. PSS was 40,574 KB (~40 MB), RSS 137,740 KB. Cold starts 1607 / 2508 / 607 / 1514 / 2698 ms, median 1607 ms; 17 sampled frames, 10 janky (58.82%). This concurrent host/emulator run is too variable for a debug-versus-release speed comparison. Owner signing remains untested.

CI API 35 initially passed all 20 tests. API 29 exposed an Android framework accessibility null dereference while querying the UI with an unbound widget ID. The cancellation test now observes the foreground system Activity using test-shell state during that window and always cancels in `finally`, preventing a dangling test ID from affecting later cases. No production permission or API behavior was bypassed. Rerun results are recorded below when available.

## Final GitHub checks

[Android CI run 42612266837](https://github.com/aashleshshet/NOVA/actions/runs/42612266837): **PASS**, core tests/lint/build and uploaded `nova-debug` artifact, source commit `bda462e`.

[Emulator run 42612266835](https://github.com/aashleshshet/NOVA/actions/runs/42612266835): **PASS**, API 29 — 20 tests in 59.782 s; API 35 — 20 tests in 71.183 s. The corrected pending-widget cancellation test passes on both. These are x86_64 Google APIs Pixel 2 emulators on GitHub, independent of the local arm64 API 36.1 run.

Additional local visual checks: control centre at 200% font scaling and home/control centre in landscape remain usable, scrollable and unclipped. Display settings restored after capture. This is not a full TalkBack audit.

The optional release workflow initially referenced `runner.temp` in a job-level environment where that context is unavailable. It now resolves `$RUNNER_TEMP` inside its preparation step, exporting the path through `$GITHUB_ENV`; cleanup uses the runner variable directly. The signing workflow remains manual and has not used owner secrets.

Local delivered debug APK SHA-256: `4022d6433820da20ba45472de9165c9cf257b3d7d5b8005c1558f8f61080da51` (18,466,333 bytes). Its v2 APK signature verifies. CI's independent debug certificate/output can differ; use the artifact from the linked run for the CI-produced build.
