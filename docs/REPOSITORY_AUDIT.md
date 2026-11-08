# Reference audit — 2026-11-09
Reference: https://github.com/Gathin23/cleanyourtv at 294d3d59dc2514e69fed557f46c1ecdfb87a95ec.

The root contains TV setup/debloat tools, themes, device documentation, tests and a standalone `launcher/` Gradle project. Its launcher is Java/Android Views with RecyclerView and spring animation, SDK 34/minimum 26. It provides a dock, app library, wallpaper and theme controls, TV input integration, clock, remote key handling and optional accessibility helpers. MainActivity is landscape with HOME and LEANBACK filters. Setup tools may grant privileged permissions or change system packages; these do not belong in a smartphone launcher.

License: MIT, copyright 2026 Gathin. Outfit font is separately SIL OFL 1.1. No launcher source is reused. The bundled Outfit font and license are retained; Gradle wrapper scripts/JAR are standard Gradle distribution components. NOVA is an independent Compose project, preserving only the inspiration of sparse home, generous typography and translucent dock.

Architecture: `core/` holds serializable state, validated layout operations, backup schema and ranking rules with JVM tests; `app/` owns Android adapters, DataStore, lifecycle, widgets, icon packs and Compose UI. Runtime state is collected from a single repository. Package discovery uses MAIN/LAUNCHER visibility, explicit component IDs, public Android APIs. No INTERNET permission.
