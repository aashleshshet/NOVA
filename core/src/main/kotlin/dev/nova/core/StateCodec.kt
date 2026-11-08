package dev.nova.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object StateCodec {
    const val MAX_BYTES = 1_048_576
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun encode(state: LauncherState): String = json.encodeToString(validate(state))
    fun decode(raw: String): LauncherState {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds 1 MB" }
        return validate(json.decodeFromString<LauncherState>(raw))
    }
    fun validate(state: LauncherState): LauncherState {
        require(state.version == 1) { "Unsupported backup version ${state.version}" }
        require(state.pages.size in 1..12) { "Choose between 1 and 12 pages" }
        require(state.pages.map { it.id }.distinct().size == state.pages.size) { "Duplicate page IDs" }
        val s = state.settings
        require(s.columns in 3..6 && s.dockSize in 3..6 && s.iconSize in 36..72 && s.spacing in 4..24) { "Invalid layout settings" }
        require(s.accent in 0xFF000000..0xFFFFFFFF && s.background in 0xFF000000..0xFFFFFFFF) { "Colors must be opaque" }
        require(s.iconPack.length <= 255)
        val tiles = state.pages.flatMap { it.tiles } + state.dock
        require(state.pages.all { it.tiles.size <= 120 } && state.dock.size <= 6) { "Too many shortcuts" }
        require(tiles.map { it.id }.distinct().size == tiles.size) { "Duplicate tile IDs" }
        tiles.forEach {
            require(it.id.length in 1..100 && it.title.length <= 80)
            require(it.children.size <= 60 && it.children.distinct().size == it.children.size)
            require(if (it.isFolder) it.component.isEmpty() && it.children.all(::validComponent) else validComponent(it.component)) { "Invalid shortcut" }
        }
        require(state.widgets.size <= 48 && state.widgets.map { it.id }.distinct().size == state.widgets.size)
        require(state.widgets.all { it.id > 0 && state.pages.any { p -> p.id == it.pageId } && it.span in 1..6 && it.height in 80..600 && validComponent(it.provider) }) { "Invalid widget placement" }
        require(state.history.size <= 2000 && state.history.all { validComponent(it.key) && it.value.count >= 0 && it.value.lastLaunch >= 0 })
        return state
    }
    fun validComponent(value: String): Boolean = value.length in 3..512 && value.count { it == '/' } == 1 && value.none { it.isWhitespace() } && value.substringBefore('/').contains('.') && value.substringAfter('/').isNotEmpty()
    /** Host IDs and consent are device-specific; history is deliberately excluded. */
    fun export(state: LauncherState): String = encode(state.copy(widgets = emptyList(), history = emptyMap(), settings = state.settings.copy(usageConsent = false, suggestions = false)))
    fun restore(raw: String): LauncherState = decode(raw).let { it.copy(widgets = emptyList(), history = emptyMap(), settings = it.settings.copy(usageConsent = false, suggestions = false)) }
}
