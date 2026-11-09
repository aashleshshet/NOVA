# NOVA 0.1.0 — functional preview

First smartphone release with a Nothing OS-inspired visual direction: monochrome circular icons, dot-matrix clock, AMOLED/light/dark/custom themes, and a swipe-down control centre.

Includes real HOME registration, installed-app discovery/search/launch, persistent pages/dock/folders, drag editing, live customization Studio, widget hosting and resizing, static icon-pack support, configurable home gestures, local opt-in suggestions and versioned backup/restore. Control centre includes real volume, battery/network status, permission-gated flashlight/brightness and Android connectivity/focus panels.

Verified locally: 16 core tests, 20 Android/Compose tests on API 36.1, lint, debug build, actual force-stop/reboot persistence and test-provider uninstall cleanup. CI independently builds APKs and runs API 29/35 instrumentation; consult the linked workflow results. Physical devices, vendor widgets/icon packs and hardware performance remain pending.

Known boundaries: widgets occupy ordered grid cells, current Android profile only; static icon mappings only; glass is a lightweight sheen, not real-time blur. System notification shade, lock-screen gestures and system-wide gesture interception are not replaced. Owner release signing requires your own secrets; the delivered debug build is for evaluation.
