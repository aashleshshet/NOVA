@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package dev.nova.launcher

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nova.core.*
import kotlinx.coroutines.launch

private data class DropSpot(val rect: Rect,val page: String?,val index: Int)

@Composable fun HomeScreen(state: LauncherState, apps: List<AppEntry>, model: LauncherViewModel, widgets: WidgetController, onDrawer: ()->Unit, onStudio: ()->Unit, onControls: ()->Unit, onSelect: (AppEntry,Tile?)->Unit, suggestions: List<AppEntry>, onGesture:(GestureAction)->Unit) {
    val compact=LocalConfiguration.current.screenHeightDp<600
    val columns=minOf(state.settings.columns,((LocalConfiguration.current.screenWidthDp-32)/56).coerceAtLeast(3))
    val pager=rememberPagerState(pageCount={state.pages.size})
    val scope=rememberCoroutineScope()
    val byId=remember(apps) { apps.associateBy { it.component } }
    var editing by remember { mutableStateOf(false) }
    var widgetId by remember { mutableStateOf<Int?>(null) }
    var folderId by remember { mutableStateOf<String?>(null) }
    val spots=remember { mutableMapOf<String,DropSpot>() }
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    fun finishDrag() {
        val id=dragging ?: return
        val target=spots.entries.filter { it.key!=id && it.value.rect.contains(dragPosition) }.minByOrNull { it.value.rect.width*it.value.rect.height }?.value
        if(target!=null) model.mutate { LayoutEditor.move(it,id,target.page,target.index) }
        dragging=null
    }
    @Composable fun tileView(tile: Tile, page: String?, index: Int, dock: Boolean=false) {
        DisposableEffect(tile.id) { onDispose { spots.remove(tile.id) } }
        val gesture=if(editing) Modifier.pointerInput(tile.id,editing) { detectDragGesturesAfterLongPress(
            onDragStart={ offset -> dragging=tile.id; dragPosition=(spots[tile.id]?.rect?.topLeft ?: Offset.Zero)+offset },
            onDrag={change,delta->change.consume();dragPosition+=delta},onDragEnd={finishDrag()},onDragCancel={dragging=null}) } else Modifier
        Box(Modifier.testTag("tile-${tile.id}").onGloballyPositioned { spots[tile.id]=DropSpot(it.boundsInRoot(),page,index) }.then(gesture)
            .alpha(if(dragging==tile.id) .45f else 1f)
            .then(if(editing) Modifier.border(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.4f),RoundedCornerShape(16.dp)) else Modifier)) {
            if(tile.isFolder) Column(Modifier.fillMaxWidth().heightIn(min=72.dp).combinedClickable(onClick={folderId=tile.id},onLongClick=if(editing) null else {{folderId=tile.id}}).padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(Icons.Default.Folder,tile.title,Modifier.size(state.settings.iconSize.dp),tint=MaterialTheme.colorScheme.primary)
                if(!dock) Text(tile.title,style=MaterialTheme.typography.labelMedium,textAlign=TextAlign.Center)
                if(editing) TextButton(onClick={model.remove(tile.id)}) { Text("Remove") }
            } else {
                val app=byId[tile.component] ?: AppEntry(tile.component,tile.component.substringBefore('/'),"Unavailable")
                AppTile(app,model,state.settings.copy(labels=state.settings.labels && !dock),{if(editing) onSelect(app,tile) else model.launch(tile.component)},if(editing) null else { {onSelect(app,tile)} })
            }
        }
    }
    Column(Modifier.fillMaxSize().testTag("home-screen")) {
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("NOVA",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            IconButton(onClick={editing=!editing}) { Icon(if(editing) Icons.Default.Check else Icons.Default.Edit,if(editing) "Finish editing" else "Edit home") }
            IconButton(onClick=onControls) { Icon(Icons.Default.Dashboard,"Open control centre") }
            IconButton(onClick=onStudio) { Icon(Icons.Default.Tune,"Open customization studio") }
        }
        if(editing) Text(if(dragging!=null) "Drop on a position, page or dock" else "Hold and drag to move. Tap for more actions.",Modifier.padding(horizontal=24.dp),style=MaterialTheme.typography.bodySmall)
        if(!compact) Box(Modifier.testTag("gesture-clock").padding(horizontal=24.dp).homeGestures(state.settings,onGesture)) { HomeClock() }
        if(suggestions.isNotEmpty() && !editing && !compact) Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { app->Box(Modifier.weight(1f)) { AppTile(app,model,state.settings.copy(iconSize=40),{model.launch(app.component)},{onSelect(app,null)}) } }
        }
        HorizontalPager(pager,Modifier.weight(1f),userScrollEnabled=dragging==null) { index ->
            val page=state.pages[index]
            if(page.tiles.isEmpty() && state.widgets.none { it.pageId==page.id }) Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp).homeGestures(state.settings,onGesture).onGloballyPositioned { spots["empty-${page.id}"]=DropSpot(it.boundsInRoot(),page.id,0) },verticalArrangement=Arrangement.Center) {
                Text("Make room for what matters.",style=MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp)); Text("Open your apps. Touch and hold one to add it here.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp)); Button(onClick=onDrawer) { Text("Choose your apps") }
            } else LazyVerticalGrid(GridCells.Fixed(columns),Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(state.settings.spacing.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                items(state.widgets.filter { it.pageId==page.id },key={"widget-${it.id}"},span={GridItemSpan(it.span.coerceAtMost(columns))}) { widget -> HostedWidget(widget,widgets,editing) { widgetId=widget.id } }
                itemsIndexed(page.tiles,key={_,t->t.id}) { i,t -> tileView(t,page.id,i) }
            }
        }
        LazyRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically,contentPadding=PaddingValues(horizontal=12.dp)) {
            items(state.pages,key={it.id}) { p -> val i=state.pages.indexOf(p)
                TextButton(onClick={scope.launch { pager.animateScrollToPage(i) }},modifier=Modifier.onGloballyPositioned { spots["page-${p.id}"]=DropSpot(it.boundsInRoot(),p.id,Int.MAX_VALUE) }) { Text(if(pager.currentPage==i) "Page ${i+1}" else "${i+1}") }
            }
            item { if(state.pages.size<12) IconButton(onClick={model.mutate(LauncherOps::addPage)}) { Icon(Icons.Default.Add,"Add home page") } }
        }
        GlassPanel(state.settings,Modifier.padding(horizontal=16.dp).fillMaxWidth().onGloballyPositioned { spots["dock"]=DropSpot(it.boundsInRoot(),null,Int.MAX_VALUE) }) {
            Row(Modifier.padding(12.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
                if(state.dock.isEmpty()) TextButton(onClick=onDrawer,modifier=Modifier.fillMaxWidth()) { Text("Add favorites to your dock") }
                state.dock.forEachIndexed { i,tile -> Box(Modifier.widthIn(min=48.dp).width(((LocalConfiguration.current.screenWidthDp-56)/state.dock.size.coerceAtLeast(1)).coerceAtLeast(48).dp)) { tileView(tile,null,i,true) } }
            }
        }
        TextButton(onClick=onDrawer,modifier=Modifier.align(Alignment.CenterHorizontally).testTag("open-drawer")) { Icon(Icons.Default.Apps,null); Spacer(Modifier.width(8.dp)); Text("All apps") }
    }
    state.widgets.find { it.id==widgetId }?.let { WidgetEditor(it,state,widgets,model) { widgetId=null } }
    val folder=(state.pages.flatMap { it.tiles }+state.dock).find { it.id==folderId && it.isFolder }
    if(folder!=null) FolderDialog(folder,state,apps,model) { folderId=null }
}
