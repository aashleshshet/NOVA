# Architecture

`core` is an Android-independent Kotlin/JVM module. Immutable serializable models feed pure layout operations, strict backup validation and bounded, opt-in ranking. Unit tests cover data invariants and destructive-edge behavior.

`app` uses one HOME Activity, Compose screens and a lifecycle ViewModel. DataStore serializes all mutations atomically on IO; decoding runs off the UI thread. The ViewModel exposes observable state, handles package broadcasts and reconciles confirmed removals on cold start. PackageManager discovers actual activities; icons decode off-thread into a bounded cache.

Android boundaries are explicit: AppCatalog (apps), WidgetController (host IDs/consent/lifecycle), IconPacks (bounded static mapping parser), UsageRanking (consent + special-access checks), ControlCentreController (public controls). ActivityResult contracts own user-controlled document, install, permission and bind flows. Widget IDs are persisted separately while binding is pending and reconciled only after a valid layout loads.

Compose home pages use lazy grids/pager. App drawer filters real entries, keyed by component; icons are loaded asynchronously. Glass is a cheap gradient/translucent surface with a solid power-save fallback. There is no per-frame blur or network dependency. Navigation animation follows platform animation scaling. Drag targets use Compose layout bounds; equivalent action-sheet controls support users who do not drag.

Data limits: 12 pages, 120 tiles/page, 6 dock tiles, 60 apps/folder, 48 widgets, 2,000 suggestion records, 1 MB backup, 120 cached 144px icons. Restores drop device-bound IDs and personal ranking data. See PRIVACY.md and ICON_PACKS.md for compatibility boundaries.
