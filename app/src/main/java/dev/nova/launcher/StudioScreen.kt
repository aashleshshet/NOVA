@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.nova.launcher

import android.app.WallpaperManager
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import dev.nova.core.*
import kotlin.math.roundToInt

@Composable fun StudioScreen(state: LauncherState, apps: List<AppEntry>, model: LauncherViewModel, activity: MainActivity, onBack: ()->Unit) {
    val s=state.settings
    var consent by remember { mutableStateOf<String?>(null) }
    var widgetPicker by remember { mutableStateOf(false) }
    var packs by remember { mutableStateOf<List<IconPack>>(emptyList()) }
    LaunchedEffect(apps) { packs=model.catalog.iconPacks.discover() }
    var folder by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back home") }
            Text("Studio",style=MaterialTheme.typography.headlineMedium)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("A home that feels like you.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            GlassPanel(s,Modifier.fillMaxWidth().testTag("live-preview")) {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("Live preview",style=MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        apps.take(s.columns.coerceAtMost(4)).forEach { app -> Box(Modifier.weight(1f)) { AppTile(app,model,s.copy(iconSize=36),{model.launch(app.component)},{}) } }
                    }
                    Text("${s.columns} columns · ${s.dockSize} dock spaces",style=MaterialTheme.typography.labelMedium)
                }
            }
            StudioHeading("Appearance")
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { ThemeMode.entries.forEach { mode -> FilterChip(s.theme==mode,{model.settings(s.copy(theme=mode))},label={Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })}) } }
            if(s.theme==ThemeMode.CUSTOM) HexColorField("Background",s.background) { model.settings(s.copy(background=it)) }
            HexColorField("Accent",s.accent) { model.settings(s.copy(accent=it)) }
            ToggleRow("Glass surfaces","Lightweight sheen; solid in battery saver.",s.glass) { model.settings(s.copy(glass=it)) }
            ToggleRow("System wallpaper","Show your wallpaper behind a readable scrim.",s.systemWallpaper) { model.settings(s.copy(systemWallpaper=it)) }
            OutlinedButton(onClick={activity.openSystem(Intent(Intent.ACTION_SET_WALLPAPER))}) { Text("Choose wallpaper") }
            StudioHeading("Layout")
            StepSlider("Grid columns",s.columns,3..6) { model.settings(s.copy(columns=it)) }
            StepSlider("Dock spaces",s.dockSize,3..6) { model.settings(s.copy(dockSize=it)) }
            StepSlider("Icon size",s.iconSize,36..72) { model.settings(s.copy(iconSize=it)) }
            StepSlider("Spacing",s.spacing,4..24) { model.settings(s.copy(spacing=it)) }
            ToggleRow("App labels","Show names below home and drawer icons.",s.labels) { model.settings(s.copy(labels=it)) }
            ToggleRow("Monochrome icons","Keep a quiet home, or restore original app colors.",s.monochromeIcons) { model.settings(s.copy(monochromeIcons=it)) }
            ChoiceRow("Icon shape",IconShape.entries,s.iconShape) { model.settings(s.copy(iconShape=it)) }
            ChoiceRow("Typeface",FontChoice.entries,s.font) { model.settings(s.copy(font=it)) }
            StudioHeading("Widgets & icons")
            OutlinedButton(onClick={widgetPicker=true},modifier=Modifier.testTag("add-widget")) { Text("Add a widget") }
            Text("Use Edit on home to resize, move or remove widgets.",style=MaterialTheme.typography.bodySmall)
            var packMenu by remember { mutableStateOf(false) }
            Box { OutlinedButton(onClick={packMenu=true}) { Text(packs.find { it.packageName==s.iconPack }?.label ?: "System icons") }
                DropdownMenu(packMenu,{packMenu=false}) {
                    DropdownMenuItem(text={Text("System icons")},onClick={model.settings(s.copy(iconPack=""));packMenu=false})
                    packs.forEach { pack->DropdownMenuItem(text={Text(pack.label)},onClick={model.settings(s.copy(iconPack=pack.packageName));packMenu=false}) }
                }
            }
            if(packs.isEmpty()) Text("Install an ADW-compatible icon pack to customize icons. Unmapped apps keep their original icon.",style=MaterialTheme.typography.bodySmall)
            StudioHeading("Pages & folders")
            Text("Use Edit on home to drag shortcuts. Touch and hold an app for move and remove actions.")
            OutlinedButton(onClick={folder=true},modifier=Modifier.testTag("create-folder")) { Text("Create a folder") }
            state.pages.forEachIndexed { i,page -> Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Page ${i+1} · ${page.tiles.size} items",Modifier.weight(1f))
                TextButton(onClick={model.mutate { LayoutEditor.removePage(it,page.id) }},enabled=state.pages.size>1 && page.tiles.isEmpty() && state.widgets.none { it.pageId==page.id }) { Text("Delete") }
            } }
            TextButton(onClick={model.mutate(LauncherOps::addPage)},enabled=state.pages.size<12) { Text("Add page") }
            StudioHeading("Gestures")
            Text("Swipe or double-tap the clock or empty home area. Android's edge navigation and widget touches stay available.",style=MaterialTheme.typography.bodyMedium)
            GestureChoice("Swipe up",s.swipeUp) { model.settings(s.copy(swipeUp=it)) }
            GestureChoice("Swipe down",s.swipeDown) { model.settings(s.copy(swipeDown=it)) }
            GestureChoice("Double tap",s.doubleTap) { model.settings(s.copy(doubleTap=it)) }
            StudioHeading("Suggestions & privacy")
            ToggleRow("Local suggestions","Learn from apps you open through NOVA. Off by default.",s.suggestions) { if(it) consent="local" else model.disableSuggestions() }
            if(s.suggestions) {
                ToggleRow("Use Android usage access","Optional ranking using activity since your consent or last reset.",s.usageConsent) { if(it) consent="usage" else { model.usage.value=emptyMap();model.settings(s.copy(usageConsent=false)) } }
                if(s.usageConsent) {
                    Text(if(model.usageRanking.hasAccess()) "Android usage access is enabled." else "Usage access is not granted. Only NOVA launches are used.",style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick={activity.openSystem(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))}) { Text("Manage usage access") }
                }
                OutlinedButton(onClick=model::resetSuggestions) { Text("Reset suggestion history") }
            }
            StudioHeading("Backup & restore")
            Text("Versioned JSON saves appearance, pages, dock and folders. Widget bindings, usage history and consent stay on this device. Restoring replaces the current layout.",style=MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { OutlinedButton(onClick=activity::exportBackup) { Text("Save backup") }; OutlinedButton(onClick=activity::importBackup) { Text("Restore") } }
            StudioHeading("System")
            Button(onClick=activity::chooseHome,modifier=Modifier.fillMaxWidth()) { Text("Set as default launcher") }
            OutlinedButton(onClick=activity::installApk,modifier=Modifier.fillMaxWidth()) { Text("Install an APK") }
            Text("NOVA ${BuildConfig.VERSION_NAME} · Offline by design",style=MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(24.dp))
        }
    }
    consent?.let { kind ->
        AlertDialog(onDismissRequest={consent=null},title={Text(if(kind=="local") "Enable local suggestions?" else "Use Android usage activity?")},text={Text(if(kind=="local") "NOVA will keep launch counts and last-opened times on this device to suggest apps. No data leaves your device. You can reset or disable this at any time; disabling deletes NOVA's history." else "NOVA can read app foreground activity through Android Usage Access. It will rank apps locally using activity after consent or reset, up to seven days. This data is never exported or sent to a server. Android will ask you to grant access separately.")},confirmButton={TextButton(onClick={
            if(kind=="local") model.settings(s.copy(suggestions=true))
            else { model.usage.value=emptyMap();model.settings(s.copy(usageConsent=true,usageSince=System.currentTimeMillis()));activity.openSystem(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            consent=null
        }) { Text("Enable") }},dismissButton={TextButton(onClick={consent=null}) { Text("Not now") }})
    }
    if(widgetPicker) WidgetPicker(state,activity.widgets,model) { widgetPicker=false }
    if(folder) FolderBuilder(apps,state,model) { folder=false }
}

@Composable fun StudioHeading(text: String) { Text(text,Modifier.padding(top=16.dp),style=MaterialTheme.typography.titleLarge) }
@Composable fun ToggleRow(title: String, description: String, checked: Boolean, onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=64.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end=12.dp)) { Text(title,style=MaterialTheme.typography.titleMedium); Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked,onChange,modifier=Modifier.semantics { contentDescription=title })
    }
}
@Composable fun StepSlider(title: String, value: Int, range: IntRange, onCommit: (Int)->Unit) {
    var draft by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column { Text("$title · ${draft.roundToInt()}",style=MaterialTheme.typography.titleMedium); Slider(draft,{draft=it},valueRange=range.first.toFloat()..range.last.toFloat(),steps=range.last-range.first-1,onValueChangeFinished={onCommit(draft.roundToInt())}) }
}
@Composable fun <T:Enum<T>> ChoiceRow(title: String, options: List<T>, selected: T, onSelect:(T)->Unit) {
    Column { Text(title,style=MaterialTheme.typography.titleMedium); FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { options.forEach { item -> FilterChip(item==selected,{onSelect(item)},label={Text(item.name.lowercase().replace('_',' ').replaceFirstChar { it.uppercase() })}) } } }
}
@Composable fun HexColorField(title: String,value: Long,onApply: (Long)->Unit) {
    var hex by remember(value) { mutableStateOf("%06X".format(value and 0xFFFFFF)) }
    val valid=hex.matches(Regex("[0-9a-fA-F]{6}"))
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(hex,{hex=it.take(6)},Modifier.weight(1f),label={Text("$title · HEX")},prefix={Text("#")},singleLine=true,isError=!valid)
        Button(onClick={onApply(0xFF000000 or hex.toLong(16))},enabled=valid) { Text("Apply") }
    }
}

@Composable fun GestureChoice(title: String,value: GestureAction,onSelect:(GestureAction)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
        Box { TextButton(onClick={expanded=true}) { Text(value.name.lowercase().replace('_',' ').replaceFirstChar { it.uppercase() }) }; DropdownMenu(expanded,{expanded=false}) { GestureAction.entries.forEach { action->DropdownMenuItem(text={Text(action.name.lowercase().replace('_',' ').replaceFirstChar { it.uppercase() })},onClick={onSelect(action);expanded=false}) } } }
    }
}
