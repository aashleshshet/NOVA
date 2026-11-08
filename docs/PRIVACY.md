# Privacy
The production manifest has no INTERNET permission. There is no analytics, account, cloud API or remote ranking. Installed launchable activities, icon resources, home layouts and widget bindings are used locally to perform launcher functions. APK installation/removal, HOME selection, wallpaper changes and widget permission are delegated to Android system UI.

Suggestions are off by default. Explicit opt-in records only launches made through NOVA (component ID, count, last-opened time), capped at 2,000 entries. Entries older than 30 days are removed on state observation; ranking decays over seven days. A separate opt-in and Android Usage Access grant allow temporary in-memory ranking from foreground events, starting at the most recent consent/reset and limited to seven days. Denial or revocation falls back to NOVA-only ranking. Reset clears counts and advances the usage start boundary. Disable deletes counts and turns off both kinds of ranking; revoking Android's system grant is available in its settings.

Backups exclude usage history, consent and device-specific widget IDs. Android cloud backup and device transfer are disabled; a deliberate Storage Access Framework JSON export is the only NOVA backup. The user's chosen document provider controls where that export is stored.

Gestures apply only within the inset home clock/empty space. NOVA requests no AccessibilityService, device-admin privileges, root, notification access or gesture exclusions.
