@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.nova.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
    val message by model.message.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf("home") }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<Pair<AppEntry,Tile?>?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); model.message.value = null } }
    LaunchedEffect(homeSignal) { screen="home"; selected=null; query="" }
    BackHandler(screen != "home" || selected != null) { selected=null; screen="home"; query="" }
    MaterialTheme(colorScheme=darkColorScheme()) {
        Scaffold(snackbarHost={ SnackbarHost(snackbar) }) { padding ->
            val s = state
            if(s == null) { Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
            Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
                when(screen) {
                    "drawer" -> {
                        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                            IconButton(onClick={screen="home"}) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back home") }
                            OutlinedTextField(query,{query=it},Modifier.weight(1f).testTag("app-search"),label={Text("Search apps")},singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)})
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text("${apps.size} apps",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                            var sortMenu by remember { mutableStateOf(false) }
                            Box { TextButton(onClick={sortMenu=true}) { Text(if(s.settings.sort==SortOrder.RECENT) "Recently installed" else "A–Z"); Icon(Icons.Default.Sort,null) }
                                DropdownMenu(sortMenu,{sortMenu=false}) { listOf(SortOrder.NAME,SortOrder.RECENT).forEach { order -> DropdownMenuItem(text={Text(if(order==SortOrder.NAME) "Name" else "Recently installed")},onClick={model.settings(s.settings.copy(sort=order));sortMenu=false}) } }
                            }
                        }
                        val filtered=remember(apps,query,s.settings.sort) { LauncherOps.apps(apps,query,s.settings.sort) }
                        if(filtered.isEmpty()) { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text(if(query.isBlank()) "No launchable apps found" else "No apps match “$query”") } }
                        LazyVerticalGrid(GridCells.Fixed(s.settings.columns),Modifier.fillMaxSize().testTag("app-drawer"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(s.settings.spacing.dp)) {
                            items(filtered,key={it.component}) { app -> AppTile(app,model,s.settings,{model.launch(app.component)},{selected=app to null}) }
                        }
                    }
                    "settings" -> {
                        Row(verticalAlignment=Alignment.CenterVertically) { IconButton(onClick={screen="home"}) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back home") }; Text("NOVA settings",style=MaterialTheme.typography.headlineMedium) }
                        Button(onClick=activity::chooseHome,modifier=Modifier.padding(16.dp)) { Text("Set as default launcher") }
                        OutlinedButton(onClick=activity::installApk,modifier=Modifier.padding(horizontal=16.dp)) { Text("Install an APK") }
                    }
                    else -> HomeScreen(s,apps,model,onDrawer={screen="drawer"},onStudio={screen="settings"},onSelect={app,tile->selected=app to tile})
                }
            }
        }
        selected?.let { (app,tile) ->
            ModalBottomSheet(onDismissRequest={selected=null}) {
                Column(Modifier.padding(24.dp).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(app.label,style=MaterialTheme.typography.headlineSmall)
                    TextButton(onClick={model.launch(app.component);selected=null}) { Text("Open app") }
                    state?.pages?.forEachIndexed { i,page -> TextButton(onClick={model.pin(app.component,page.id);selected=null}) { Text("Add to page ${i+1}") } }
                    TextButton(onClick={model.pin(app.component,null,true);selected=null}) { Text("Add to dock") }
                    if(tile!=null) TextButton(onClick={model.remove(tile.id);selected=null}) { Text("Remove shortcut") }
                    TextButton(onClick={activity.appInfo(app.packageName);selected=null}) { Text("App info") }
                    TextButton(onClick={activity.uninstall(app.packageName);selected=null}) { Text("Uninstall app") }
                }
            }
        }
    }
}

@Composable fun HomeScreen(state: LauncherState, apps: List<AppEntry>, model: LauncherViewModel, onDrawer: ()->Unit, onStudio: ()->Unit, onSelect: (AppEntry,Tile)->Unit) {
    val pager = rememberPagerState(pageCount={state.pages.size})
    val scope=rememberCoroutineScope()
    val byId=remember(apps) { apps.associateBy { it.component } }
    Column(Modifier.fillMaxSize().testTag("home-screen")) {
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("NOVA",Modifier.weight(1f),style=MaterialTheme.typography.headlineLarge)
            IconButton(onClick=onStudio) { Icon(Icons.Default.Tune,"Open customization studio") }
        }
        HorizontalPager(pager,Modifier.weight(1f)) { index ->
            val page=state.pages[index]
            if(page.tiles.isEmpty()) Column(Modifier.fillMaxSize().padding(32.dp),verticalArrangement=Arrangement.Center) {
                Text("Make room for what matters.",style=MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(16.dp)); Text("Open your apps. Touch and hold one to add it here.",style=MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp)); Button(onClick=onDrawer) { Text("Choose your apps") }
            } else LazyVerticalGrid(GridCells.Fixed(state.settings.columns),Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(state.settings.spacing.dp)) {
                items(page.tiles,key={it.id}) { tile -> val app=byId[tile.component] ?: AppEntry(tile.component,tile.component.substringBefore('/'),"Unavailable")
                    AppTile(app,model,state.settings,{model.launch(tile.component)},{onSelect(app,tile)})
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
            state.pages.forEachIndexed { i,_ -> TextButton(onClick={scope.launch { pager.animateScrollToPage(i) }}) { Text(if(pager.currentPage==i) "● ${i+1}" else "${i+1}") } }
            if(state.pages.size<12) IconButton(onClick={model.mutate(LauncherOps::addPage)}) { Icon(Icons.Default.Add,"Add home page") }
        }
        Surface(Modifier.padding(horizontal=16.dp).fillMaxWidth(),shape=RoundedCornerShape(28.dp),tonalElevation=3.dp) {
            Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
                if(state.dock.isEmpty()) TextButton(onClick=onDrawer) { Text("Add favorites to your dock") }
                state.dock.forEach { tile -> val app=byId[tile.component] ?: AppEntry(tile.component,tile.component.substringBefore('/'),"Unavailable")
                    Box(Modifier.weight(1f)) { AppTile(app,model,state.settings.copy(labels=false),{model.launch(tile.component)},{onSelect(app,tile)}) }
                }
            }
        }
        TextButton(onClick=onDrawer,modifier=Modifier.align(Alignment.CenterHorizontally).testTag("open-drawer")) { Icon(Icons.Default.Apps,null); Spacer(Modifier.width(8.dp)); Text("All apps") }
    }
}

@Composable fun AppTile(app: AppEntry, model: LauncherViewModel, settings: Settings, onClick: ()->Unit, onLongClick: ()->Unit) {
    val icon by produceState<android.graphics.Bitmap?>(null,app.component) { value=model.catalog.icon(app.component) }
    Column(Modifier.fillMaxWidth().heightIn(min=72.dp).clip(RoundedCornerShape(16.dp)).combinedClickable(onClick=onClick,onLongClick=onLongClick).padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        if(icon!=null) Image(icon!!.asImageBitmap(),app.label,Modifier.size(settings.iconSize.dp))
        else Icon(Icons.Default.Apps,app.label,Modifier.size(settings.iconSize.dp))
        if(settings.labels) { Spacer(Modifier.height(8.dp)); Text(app.label,style=MaterialTheme.typography.labelMedium,maxLines=2,overflow=TextOverflow.Ellipsis,textAlign=TextAlign.Center) }
    }
}
