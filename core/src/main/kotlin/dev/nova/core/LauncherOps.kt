package dev.nova.core

import java.text.Collator
import java.util.UUID

object LauncherOps {
    fun pin(state: LauncherState, component: String, pageId: String?, dock: Boolean = false): LauncherState {
        require(StateCodec.validComponent(component))
        val tile = Tile(UUID.randomUUID().toString(), component)
        if (dock) {
            require(state.dock.size < state.settings.dockSize) { "Dock is full. Remove or move an app first." }
            if (state.dock.any { it.component == component }) return state
            return state.copy(dock = state.dock + tile)
        }
        val target = pageId ?: state.pages.first().id
        require(state.pages.any { it.id == target })
        return state.copy(pages = state.pages.map { if (it.id == target && it.tiles.none { t -> t.component == component }) it.copy(tiles = it.tiles + tile) else it })
    }
    fun remove(state: LauncherState, id: String): LauncherState = state.copy(
        pages = state.pages.map { it.copy(tiles = it.tiles.filterNot { t -> t.id == id }) },
        dock = state.dock.filterNot { it.id == id }
    )
    fun removePackage(state: LauncherState, packageName: String): LauncherState {
        fun clean(tiles: List<Tile>): List<Tile> = tiles.mapNotNull { tile ->
            if (tile.isFolder) {
                val children = tile.children.filterNot { it.substringBefore('/') == packageName }
                when (children.size) { 0 -> null; 1 -> tile.copy(component = children.first(), children = emptyList(), title = ""); else -> tile.copy(children = children) }
            } else tile.takeUnless { it.component.substringBefore('/') == packageName }
        }
        return state.copy(pages = state.pages.map { it.copy(tiles = clean(it.tiles)) }, dock = clean(state.dock),
            widgets = state.widgets.filterNot { it.provider.substringBefore('/') == packageName },
            history = state.history.filterKeys { it.substringBefore('/') != packageName })
    }
    fun addPage(state: LauncherState): LauncherState {
        require(state.pages.size < 12) { "Maximum 12 pages" }
        return state.copy(pages = state.pages + Page(UUID.randomUUID().toString()))
    }
    fun apps(apps: List<AppEntry>, query: String, order: SortOrder, history: Map<String, LaunchRecord> = emptyMap()): List<AppEntry> {
        val terms = query.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        val byName = Comparator<AppEntry> { a, b -> collator.compare(a.label, b.label).takeIf { it != 0 } ?: a.component.compareTo(b.component) }
        val comparator = when (order) {
            SortOrder.NAME -> byName
            SortOrder.RECENT -> compareByDescending<AppEntry> { it.installedAt }.then(byName)
            SortOrder.MOST_USED -> compareByDescending<AppEntry> { history[it.component]?.count ?: 0 }.then(byName)
        }
        return apps.filter { a -> terms.all { a.label.contains(it, true) || a.packageName.contains(it, true) } }.sortedWith(comparator)
    }
}
