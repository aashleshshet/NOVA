# Delivery log

## Phase 1 — Foundation (in progress)
Created independent Compose application, pure Kotlin core, HOME manifest, DataStore design, wrapper and CI. Reference and license audit in REPOSITORY_AUDIT.md. Java/Gradle downloads required before validation. No build success claimed yet.

Phase 1 checks: `:core:test :app:assembleDebug :app:lintDebug` PASS (4 JVM tests, installable foundation APK, lint completed). Added instrumentation tests for HOME resolution and DataStore recreation; execution deferred to emulator validation. Important files: settings/build Gradle files, core/Models.kt and StateCodec.kt, StateStore.kt, AndroidManifest.xml, .github/workflows/android.yml. No implementation blocker remains; proceed to real launcher functionality.

## Phase 2 — Functional launcher
Implemented actual PackageManager catalog and explicit activity launch, searchable/name-or-install-date sorted drawer, persistent pages and dock, package-change handling, app details/uninstall, and document-picker APK installation through Android's installer. Empty home is intentional, with no fake apps. Important files: AppCatalog.kt, LauncherViewModel.kt, LauncherUi.kt, MainActivity.kt, LauncherOps.kt, manifest/FileProvider paths.

Checks: 8 JVM tests PASS; assembleDebug PASS; lintDebug PASS; 2 emulator instrumentation tests (HOME registration, persistence across repository instances) PASS on Android 16 API 36.1. APK installed and launched successfully. Visual capture inspected; status bar contrast needs the Phase 3 theme treatment. Physical device testing pending. Git CLI workflow upload was rejected for missing OAuth workflow scope; connected GitHub app successfully created the workflow instead.

## Phase 3 — Premium UI
Added NOVA Material color/type system, AMOLED/light/dark/custom schemes, Outfit and font/shape tokens, live localized clock/date, glass dock with PowerManager solid fallback, theme selection, system-bar contrast, and system-scaled Crossfade navigation. Added explicit exclusions for cloud backup and device transfer. Important files: NovaTheme.kt, HomeClock.kt, LauncherUi.kt, DESIGN.md, data_extraction_rules.xml.

Checks: 8 JVM tests remain PASS; assembleDebug and lintDebug PASS. Installed updated UI and captured AMOLED emulator screen. No physical performance claim; API 26/light/large-font coverage remains for Phase 7. Glass uses a lightweight translucent sheen, not expensive live backdrop blur.
