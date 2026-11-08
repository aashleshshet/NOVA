package dev.nova.core

import kotlinx.serialization.Serializable

@Serializable enum class ThemeMode { AMOLED, DARK, LIGHT, CUSTOM }
@Serializable enum class SortOrder { NAME, RECENT, MOST_USED }
@Serializable enum class GestureAction { NONE, DRAWER, SEARCH, STUDIO, SYSTEM_SETTINGS }
@Serializable enum class IconShape { ORIGINAL, CIRCLE, ROUNDED }
@Serializable enum class FontChoice { OUTFIT, SYSTEM, SERIF }

@Serializable data class Settings(
    val theme: ThemeMode = ThemeMode.AMOLED,
    val accent: Long = 0xFFD6F4A0,
    val background: Long = 0xFF12171D,
    val glass: Boolean = true,
    val columns: Int = 4,
    val dockSize: Int = 4,
    val iconSize: Int = 52,
    val spacing: Int = 12,
    val labels: Boolean = true,
    val font: FontChoice = FontChoice.OUTFIT,
    val iconShape: IconShape = IconShape.ROUNDED,
    val iconPack: String = "",
    val systemWallpaper: Boolean = false,
    val sort: SortOrder = SortOrder.NAME,
    val swipeUp: GestureAction = GestureAction.DRAWER,
    val swipeDown: GestureAction = GestureAction.SEARCH,
    val doubleTap: GestureAction = GestureAction.NONE,
    val suggestions: Boolean = false,
    val usageConsent: Boolean = false
)

@Serializable data class Tile(
    val id: String,
    val component: String = "",
    val title: String = "",
    val children: List<String> = emptyList()
) { val isFolder: Boolean get() = children.isNotEmpty() }

@Serializable data class Page(val id: String = "home", val tiles: List<Tile> = emptyList())
@Serializable data class WidgetPlacement(val id: Int, val pageId: String, val provider: String, val span: Int = 4, val height: Int = 180)
@Serializable data class LaunchRecord(val count: Int = 0, val lastLaunch: Long = 0)
@Serializable data class LauncherState(
    val version: Int = 1,
    val settings: Settings = Settings(),
    val pages: List<Page> = listOf(Page()),
    val dock: List<Tile> = emptyList(),
    val widgets: List<WidgetPlacement> = emptyList(),
    val history: Map<String, LaunchRecord> = emptyMap()
)

data class AppEntry(val component: String, val packageName: String, val label: String, val installedAt: Long = 0)
