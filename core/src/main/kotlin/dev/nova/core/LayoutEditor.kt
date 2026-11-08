package dev.nova.core

import java.util.UUID

object LayoutEditor {
    fun move(state: LauncherState, id: String, pageId: String?, index: Int = Int.MAX_VALUE): LauncherState {
        val tile=(state.pages.flatMap { it.tiles }+state.dock).find { it.id==id } ?: return state
        if(pageId==null) require(state.dock.size < state.settings.dockSize || state.dock.any { it.id==id }) { "Dock is full" }
        else require(state.pages.any { it.id==pageId }) { "Page no longer exists" }
        val removed=LauncherOps.remove(state,id)
        fun insert(list: List<Tile>) = list.toMutableList().apply { add(index.coerceIn(0,size),tile) }.toList()
        return if(pageId==null) removed.copy(dock=insert(removed.dock)) else removed.copy(pages=removed.pages.map { if(it.id==pageId) it.copy(tiles=insert(it.tiles)) else it })
    }
    fun folder(state: LauncherState, components: List<String>, title: String, pageId: String): LauncherState {
        require(components.distinct().size in 2..60) { "Select at least two apps" }
        require(components.all(StateCodec::validComponent))
        require(state.pages.any { it.id==pageId })
        return state.copy(pages=state.pages.map { p -> if(p.id!=pageId) p else p.copy(tiles=p.tiles.filterNot { it.component in components } + Tile(UUID.randomUUID().toString(),title=title.trim().take(80).ifBlank { "Folder" },children=components.distinct())) })
    }
    fun editFolder(state: LauncherState, id: String, title: String, children: List<String>): LauncherState {
        require(children.distinct().size<=60 && children.all(StateCodec::validComponent))
        fun edit(tiles: List<Tile>) = tiles.mapNotNull { t -> if(t.id!=id) t else when(children.size) {
            0 -> null
            1 -> t.copy(component=children.first(),title="",children=emptyList())
            else -> t.copy(title=title.trim().take(80).ifBlank { "Folder" },children=children.distinct())
        } }
        return state.copy(pages=state.pages.map { it.copy(tiles=edit(it.tiles)) },dock=edit(state.dock))
    }
    fun removePage(state: LauncherState, id: String): LauncherState {
        require(state.pages.size>1) { "Keep at least one page" }
        val p=state.pages.first { it.id==id }
        require(p.tiles.isEmpty() && state.widgets.none { it.pageId==id }) { "Move apps and widgets before deleting this page" }
        return state.copy(pages=state.pages.filterNot { it.id==id })
    }
    fun settings(state: LauncherState, settings: Settings): LauncherState {
        // Reducing dock capacity never loses hidden shortcuts: overflow moves to the first page.
        val overflow=state.dock.drop(settings.dockSize)
        return state.copy(settings=settings,dock=state.dock.take(settings.dockSize),pages=state.pages.mapIndexed { index,p -> if(index==0) p.copy(tiles=p.tiles+overflow) else p })
    }
}
