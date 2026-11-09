# Create an owner-signed release

The debug APK is installable but uses a debug certificate. No production key is embedded in this repository. Keep the same owner signing key for future updates. See [Android's signing guide](https://developer.android.com/studio/publish/app-signing).

## GitHub Actions

Create a keystore locally with Android Studio's **Build → Generate Signed App Bundle or APK** wizard or JDK keytool. Keep an offline backup. Add these repository **Actions secrets** (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `NOVA_KEYSTORE_BASE64` | Base64 contents of your keystore file |
| `NOVA_KEYSTORE_PASSWORD` | Keystore password |
| `NOVA_KEY_ALIAS` | Alias of your signing key |
| `NOVA_KEY_PASSWORD` | Password for that key |

On macOS, `base64 -i /path/to/nova-release.jks | pbcopy` copies the encoded keystore for the secret form. Never commit the key or passwords.

Open **Actions → Signed release → Run workflow**, selecting a reviewed branch/tag. The manual workflow validates all secrets, runs core tests/lint, builds an R8-minified APK, verifies the signature, produces a SHA-256 checksum and uploads `nova-signed-release`. It deletes the temporary keystore even on failure. It does not publish to an app store or automatically create a public release.

## Local build

Set environment variables `NOVA_KEYSTORE_FILE` (absolute path), `NOVA_KEYSTORE_PASSWORD`, `NOVA_KEY_ALIAS`, `NOVA_KEY_PASSWORD` in your secure local shell/session, then run:

```sh
./gradlew :core:test :app:lintRelease :app:assembleRelease
"$ANDROID_HOME/build-tools/36.1.0/apksigner" verify --verbose app/build/outputs/apk/release/app-release.apk
```

Without `NOVA_KEYSTORE_FILE`, the release build intentionally produces `app-release-unsigned.apk`, which cannot be installed until signed. For a new version, increment `versionCode` and update `versionName` in app/build.gradle.kts, update release notes, and run validation before dispatching the release workflow.

Debug and release certificates differ. To switch, export a backup, select another default launcher, uninstall the debug app, install the signed release and restore. Re-add widgets and explicitly re-enable suggestions if wanted. Local and CI debug keys may also differ.

The optional owner-secret workflow has not been dispatched with your key; no owner-signed release is claimed.
