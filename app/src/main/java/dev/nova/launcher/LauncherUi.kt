@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.nova.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nova.core.*
import kotlinx.coroutines.launch

@Composable fun NovaApp(model: LauncherViewModel, activity: MainActivity, homeSignal: Int) {
    val state by model.state.collectAsStateWithLifecycle()
    val apps by model.apps.collectAsStateWithLifecycle()
    val usage by model.usage.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val loadFailed by model.loadFailed.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf("home") }
    var searchFocus by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<Pair<AppEntry,Tile?>?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); model.message.value = null } }
    LaunchedEffect(homeSignal) { screen="home"; selected=null; query="" }
    BackHandler(screen != "home" || selected != null) { selected=null; screen="home"; query="" }
    NovaTheme(state?.settings ?: Settings()) {
        Scaffold(containerColor=if(state?.settings?.systemWallpaper==true && screen=="home") MaterialTheme.colorScheme.background.copy(alpha=.82f) else MaterialTheme.colorScheme.background,snackbarHost={ SnackbarHost(snackbar) }) { padding ->
            val s = state
            if(s == null) { Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.Center) {
                if(loadFailed) Column(Modifier.padding(24.dp)) { Text("Your saved layout could not be read."); TextButton(onClick=activity::importBackup) { Text("Restore backup") }; TextButton(onClick=activity::chooseHome) { Text("Choose another launcher") } } else CircularProgressIndicator()
            }; return@Scaffold }
            Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
                Crossfade(screen,label="launcher navigation",modifier=Modifier.weight(1f)) { destination ->
                when(destination) {
                    "drawer" -> {
                        val focus=remember { FocusRequester() }
                        val keyboard=LocalSoftwareKeyboardController.current
                        LaunchedEffect(Unit) { if(searchFocus) { focus.requestFocus();keyboard?.show() } }
                        Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                            IconButton(onClick={screen="home"}) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back home") }
                            OutlinedTextField(query,{query=it},Modifier.weight(1f).focusRequester(focus).testTag("app-search"),label={Text("Search apps")},singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)})
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text("${apps.size} apps",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                            var sortMenu by remember { mutableStateOf(false) }
                            Box { TextButton(onClick={sortMenu=true}) { Text(when(s.settings.sort) { SortOrder.NAME->"A–Z";SortOrder.RECENT->"Recently installed";SortOrder.MOST_USED->"Most used" }); Icon(Icons.AutoMirrored.Filled.Sort,null) }
                                DropdownMenu(sortMenu,{sortMenu=false}) { (if(s.settings.suggestions) SortOrder.entries else listOf(SortOrder.NAME,SortOrder.RECENT)).forEach { order -> DropdownMenuItem(text={Text(when(order) { SortOrder.NAME->"Name";SortOrder.RECENT->"Recently installed";SortOrder.MOST_USED->"Most used in NOVA" })},onClick={model.settings(s.settings.copy(sort=order));sortMenu=false}) } }
                            }
                        }
                        val filtered=remember(apps,query,s.settings.sort,s.history) { LauncherOps.apps(apps,query,s.settings.sort,s.history) }
                        if(filtered.isEmpty()) { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text(if(query.isBlank()) "No launchable apps found" else "No apps match “$query”") } }
                        LazyVerticalGrid(GridCells.Fixed(s.settings.columns),Modifier.fillMaxSize().testTag("app-drawer"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(s.settings.spacing.dp)) {
                            items(filtered,key={it.component}) { app -> AppTile(app,model,s.settings,{model.launch(app.component)},{selected=app to null}) }
                        }
                        }
                    }
                    "settings" -> StudioScreen(s,apps,model,activity) { screen="home" }
                    else -> HomeScreen(s,apps,model,activity.widgets,onDrawer={searchFocus=false;screen="drawer"},onStudio={screen="settings"},onSelect={app,tile->selected=app to tile},suggestions=Suggestions.ranked(apps,s,System.currentTimeMillis(),usage).take(4),onGesture={action->when(action) {
                        GestureAction.DRAWER -> {screen="drawer";query=""}
                        GestureAction.SEARCH -> {searchFocus=true;screen="drawer";query=""}
                        GestureAction.STUDIO -> screen="settings"
                        GestureAction.SYSTEM_SETTINGS -> activity.openSystem(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
                        GestureAction.NONE -> Unit
                    }})
                }
                }
            }
        }
        activity.pendingRestore.value?.let { restored ->
            AlertDialog(onDismissRequest={activity.pendingRestore.value=null},title={Text("Replace your layout?")},text={Text("Restore ${restored.pages.size} pages and ${restored.dock.size} dock items. Current widget bindings will be removed. Usage consent will be off.")},confirmButton={TextButton(onClick=activity::confirmRestore) { Text("Restore") }},dismissButton={TextButton(onClick={activity.pendingRestore.value=null}) { Text("Cancel") }})
        }
        selected?.let { (app,tile) ->
            ModalBottomSheet(onDismissRequest={selected=null}) {
                Column(Modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(app.label,style=MaterialTheme.typography.headlineSmall)
                    TextButton(onClick={model.launch(app.component);selected=null}) { Text("Open app") }
                    state?.pages?.forEachIndexed { i,page -> TextButton(onClick={model.pin(app.component,page.id);selected=null}) { Text("Add to page ${i+1}") } }
                    TextButton(onClick={model.pin(app.component,null,true);selected=null}) { Text("Add to dock") }
                    if(tile!=null) {
                        state?.pages?.forEachIndexed { i,page -> TextButton(onClick={model.mutate { LayoutEditor.move(it,tile.id,page.id) };selected=null}) { Text("Move to page ${i+1}") } }
                        TextButton(onClick={model.mutate { LayoutEditor.move(it,tile.id,null) };selected=null}) { Text("Move to dock") }
                        TextButton(onClick={model.remove(tile.id);selected=null}) { Text("Remove shortcut") }
                    }
                    TextButton(onClick={activity.appInfo(app.packageName);selected=null}) { Text("App info") }
                    TextButton(onClick={activity.uninstall(app.packageName);selected=null}) { Text("Uninstall app") }
                }
            }
        }
    }
}

@Composable fun AppTile(app: AppEntry, model: LauncherViewModel, settings: Settings, onClick: ()->Unit, onLongClick: ()->Unit) {
    val icon by produceState<android.graphics.Bitmap?>(null,app.component,settings.iconPack) { value=model.catalog.icon(app.component,settings.iconPack) }
    Column(Modifier.fillMaxWidth().heightIn(min=72.dp).clip(RoundedCornerShape(16.dp)).combinedClickable(onClick=onClick,onLongClick=onLongClick).padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        if(icon!=null) Image(icon!!.asImageBitmap(),app.label,Modifier.size(settings.iconSize.dp).then(when(settings.iconShape) { IconShape.CIRCLE -> Modifier.clip(CircleShape); IconShape.ROUNDED -> Modifier.clip(RoundedCornerShape(16.dp)); IconShape.ORIGINAL -> Modifier }))
        else Icon(Icons.Default.Apps,app.label,Modifier.size(settings.iconSize.dp))
        if(settings.labels) { Spacer(Modifier.height(8.dp)); Text(app.label,style=MaterialTheme.typography.labelMedium,maxLines=2,overflow=TextOverflow.Ellipsis,textAlign=TextAlign.Center) }
    }
}
