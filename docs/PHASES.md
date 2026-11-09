# Delivery log

## Phase 1 — Foundation
Created independent Compose application, pure Kotlin core, HOME manifest, DataStore design, wrapper and CI. Reference and license audit in REPOSITORY_AUDIT.md. Java/Gradle downloads required before validation. No build success claimed yet.

Phase 1 checks: `:core:test :app:assembleDebug :app:lintDebug` PASS (4 JVM tests, installable foundation APK, lint completed). Added instrumentation tests for HOME resolution and DataStore recreation; execution deferred to emulator validation. Important files: settings/build Gradle files, core/Models.kt and StateCodec.kt, StateStore.kt, AndroidManifest.xml, .github/workflows/android.yml. No implementation blocker remains; proceed to real launcher functionality.

## Phase 2 — Functional launcher
Implemented actual PackageManager catalog and explicit activity launch, searchable/name-or-install-date sorted drawer, persistent pages and dock, package-change handling, app details/uninstall, and document-picker APK installation through Android's installer. Empty home is intentional, with no fake apps. Important files: AppCatalog.kt, LauncherViewModel.kt, LauncherUi.kt, MainActivity.kt, LauncherOps.kt, manifest/FileProvider paths.

Checks: 8 JVM tests PASS; assembleDebug PASS; lintDebug PASS; 2 emulator instrumentation tests (HOME registration, persistence across repository instances) PASS on Android 16 API 36.1. APK installed and launched successfully. Visual capture inspected; status bar contrast needs the Phase 3 theme treatment. Physical device testing pending. Git CLI workflow upload was rejected for missing OAuth workflow scope; connected GitHub app successfully created the workflow instead.

## Phase 3 — Premium UI
Added NOVA Material color/type system, AMOLED/light/dark/custom schemes, Outfit and font/shape tokens, live localized clock/date, glass dock with PowerManager solid fallback, theme selection, system-bar contrast, and system-scaled Crossfade navigation. Added explicit exclusions for cloud backup and device transfer. Important files: NovaTheme.kt, HomeClock.kt, LauncherUi.kt, DESIGN.md, data_extraction_rules.xml.

Checks: 8 JVM tests remain PASS; assembleDebug and lintDebug PASS. Installed updated UI and captured AMOLED emulator screen. No physical performance claim; API 26/light/large-font coverage remains for Phase 7. Glass uses a lightweight translucent sheen, not expensive live backdrop blur.

## Phase 4 — Customization Studio
Implemented live previews using real icons, custom HEX color validation, grid/dock/icon/spacing controls, icon masks, typefaces, system wallpaper picker, persistent folders with rename/member editing, page deletion guards, drag reordering and page/dock drop targets, and accessible move actions. Reducing dock size moves overflow to page one. JSON backups use a size-limited document picker and strict version/schema validation; restore confirms replacement and drops consent/history/widget IDs. Corrupt persistence offers recovery without silently resetting user data. Important files: StudioScreen.kt, HomeScreen.kt, Folders.kt, LayoutEditor.kt, MainActivity.kt.

Checks: 12 JVM tests PASS including cross-container moves, folders, lossless dock reduction and deletion guards; assembleDebug/lintDebug PASS. Full gesture and document-provider UI checks remain for validation phase; physical-device tests pending.

## Phase 5 — Widgets and icon packs
Added AppWidgetHost with start/stop lifecycle, public bind/configure consent flows, durable pending ID, cancellation/orphan cleanup, actual RemoteViews, page placement, width/height controls respecting provider limits and ordered positioning. Added ADW/Nova static appfilter parsing (resource/assets), bounded caches, strict drawable names and per-app fallback. Important files: WidgetController.kt, WidgetUi.kt, IconPacks.kt, docs/ICON_PACKS.md, instrumentation-only FixtureWidget.java.

Checks: build/lint PASS; 12 core tests PASS; all 5 emulator tests PASS, including real provider bind/host/resize/delete and icon parser/fallback. First widget run failed because its assertion omitted Android host padding, and a Kotlin test provider couldn't load Kotlin in its separate test-APK process. Fixed assertion to account for platform padding and made fixture Java; rerun passed. Third-party vendor widgets/icon packs and physical-device compatibility remain pending. Work-profile widgets and dynamic icon-pack features are explicitly outside this version.

## Phase 6 — Gestures and smart suggestions
Added configurable swipe up/down and double-tap actions in inset clock/empty home areas, preserving Android edge gestures and widget interactions. Added opt-in local launch ranking with expiry and bounded state, separate explicit consent for UsageEvents, permission-safe fallback, reset boundary, and disable/delete controls. Search gesture focuses the real app search. No network permission. Important files: HomeGestures.kt, UsageRanking.kt, Suggestions.kt, StudioScreen.kt, PRIVACY.md.

Checks: 16 JVM tests PASS, including privacy gates, reset/disable, retention and removed-app filtering; assembleDebug/lintDebug PASS. Android permission-denial and gesture UI scenarios proceed in Phase 7. No blocker.

## User-directed visual revision during Phase 7
Adopted Nothing OS-inspired monochrome design, dot-matrix clock, optional grayscale circular icons, restrained red state color, and a real control centre. Control centre provides local battery/network status, AudioManager volume, permission-gated CameraManager flashlight and system brightness, plus Android-owned Internet/Bluetooth/DND panels. Added CONTROL_CENTRE gesture and swipe-down default. No hidden APIs or notification-shade impersonation.

Initial expanded Compose suite found outdated Espresso incompatibility with Android 16, a child long-press handler consuming drag, and two test assumptions (widget dialog text and task-return teardown). Updated AndroidX test runtime, disabled child long-press while dragging, and corrected the system-dialog/task-return tests. Subsequent 18-test emulator suite passed. Light status-bar appearance now has an explicit WindowInsetsController assertion; snapshots wait for SystemUI rendering.

## Phase 7 — Validation
Final local suite: 16 core JVM tests and 20 Android/Compose tests PASS; debug build, instrumentation build and lint PASS. Actual force-stop and reboot preserve folders, dock, settings and bound widgets; NOVA remains default HOME. Uninstalling the test widget provider removes its persisted placement. Fixed folder drag interception and added a regression test. CPU/background persistence parsing and icon-pack negative caching reduce repeated work. Important files: ControlCentre.kt, HomeClock.kt, LauncherUiTest.kt, RestartScenarioTest.kt, WidgetAndIconsTest.kt, scripts/*-check.py, docs/VALIDATION.md and screenshots.

Only a disposable Android emulator was tested. Physical devices and vendor integrations remain pending; validation scope and limitations are explicit in VALIDATION.md. Performance measurements are debug/emulator smoke data, not a frame-rate guarantee. Proceed to delivery after all available functional checks passed.

## Phase 8 — APK delivery
Added optional environment-based owner signing and a manual GitHub Secrets release workflow, API 29/35 emulator CI, installation/default-HOME/rollback/troubleshooting guides, architecture notes and release notes. Debug and optimized release build/lint PASS (0 errors, 21 warnings); debug signature verified. Signing configuration was exercised using a disposable debug certificate for optimized emulator smoke only. No owner secret was supplied and no owner-signed release is claimed. Important files: app/build.gradle.kts, .github/workflows/{android,emulator,release}.yml, README.md, docs/{INSTALL,SIGNING,RELEASE_NOTES,VALIDATION}.md. Local APK: artifacts/NOVA-0.1.0-debug.apk.

GitHub API 35 passed 20 instrumentation tests; the first API 29 run found a framework accessibility null dereference during pending widget binding. Corrected test observation and cleanup; rerunning before final publication. Physical-device and vendor integration checks remain pending.

Final delivery checks: GitHub Android CI PASS (42612266837); API 29 and 35 emulator jobs both PASS, 20 tests each (42612266835). Debug APK signature verified and checksum recorded in VALIDATION.md. Optional release workflow context issue corrected; owner secrets remain user-supplied. 200% text and landscape visual checks passed. Source publication to main preserves the dedicated development branch. Physical-device/performance/vendor checks remain documented follow-up work, not claimed as complete.
