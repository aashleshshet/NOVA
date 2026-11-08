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
import androidx.compose.ui.unit.dp
import dev.nova.core.*
import kotlin.math.roundToInt

@Composable fun StudioScreen(state: LauncherState, apps: List<AppEntry>, model: LauncherViewModel, activity: MainActivity, onBack: ()->Unit) {
    val s=state.settings
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
            ChoiceRow("Icon shape",IconShape.entries,s.iconShape) { model.settings(s.copy(iconShape=it)) }
            ChoiceRow("Typeface",FontChoice.entries,s.font) { model.settings(s.copy(font=it)) }
            StudioHeading("Pages & folders")
            Text("Use Edit on home to drag shortcuts. Touch and hold an app for move and remove actions.")
            OutlinedButton(onClick={folder=true},modifier=Modifier.testTag("create-folder")) { Text("Create a folder") }
            state.pages.forEachIndexed { i,page -> Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Page ${i+1} · ${page.tiles.size} items",Modifier.weight(1f))
                TextButton(onClick={model.mutate { LayoutEditor.removePage(it,page.id) }},enabled=state.pages.size>1 && page.tiles.isEmpty() && state.widgets.none { it.pageId==page.id }) { Text("Delete") }
            } }
            TextButton(onClick={model.mutate(LauncherOps::addPage)},enabled=state.pages.size<12) { Text("Add page") }
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
    if(folder) FolderBuilder(apps,state,model) { folder=false }
}

@Composable fun StudioHeading(text: String) { Text(text,Modifier.padding(top=16.dp),style=MaterialTheme.typography.titleLarge) }
@Composable fun ToggleRow(title: String, description: String, checked: Boolean, onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=64.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end=12.dp)) { Text(title,style=MaterialTheme.typography.titleMedium); Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked,onChange)
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
