# Icon-pack compatibility
NOVA supports static ADW/Nova-style `<item component="ComponentInfo{package/activity}" drawable="resource_name"/>` mappings in `res/xml/appfilter.xml` or `assets/appfilter.xml`. Relative component names are normalized. Packages advertise `org.adw.launcher.THEMES` or `com.novalauncher.THEME`. Resource drawables are loaded without executing pack code. Malformed XML, missing mappings/resources, uninstalled packs and unsupported features fall back to the app's original icon. Bitmap cache is bounded to 120 icons at 144px; metadata to 40,000 mappings / 2 MB assets.

Dynamic calendar/clock icons, iconback/iconmask composition, proprietary encrypted formats and launcher-specific plugins are not supported in 0.1. No blanket package-visibility permission is used.

Format reference: https://github.com/jahirfiquitiva/Blueprint/wiki/Setting-up-icon-pack-%28Part-1%29
Widget lifecycle reference: https://developer.android.com/develop/ui/views/appwidgets/host

Widgets are selected for the current Android profile, bound through Android's consent screen if needed, configured through AppWidgetHost, and rendered as actual RemoteViews. Resize honors provider resize modes/minimums. ID cleanup happens after persistent deletion; abandoned IDs are reconciled, while an in-flight binding ID is retained across process recreation. Backup deliberately omits host IDs; add widgets again after restore. Work-profile widgets and arbitrary overlap/freeform placement are outside this version; widgets use ordered, resizable grid cells on selected pages.
