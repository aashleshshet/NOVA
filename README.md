# NOVA

Independent Kotlin / Jetpack Compose smartphone launcher inspired by CleanYourTV's minimalist philosophy. Android 8.0+ (API 26), target/compile API 36. Development is on `development/nova-launcher`.

## Build
Use JDK 17 and Android SDK 36 with Build Tools 36.1.0. Set `ANDROID_HOME` or untracked `local.properties` (`sdk.dir=...`).

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions uploads `nova-debug`. No cloud service, root or ADB is needed to use the launcher.

See [reference audit](docs/REPOSITORY_AUDIT.md), [phase log](docs/PHASES.md) and [design system](DESIGN.md).
