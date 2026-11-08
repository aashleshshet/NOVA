package dev.nova.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nova.core.*

@Composable fun FolderBuilder(apps: List<AppEntry>,state: LauncherState,model: LauncherViewModel,onClose:()->Unit) {
    var title by remember { mutableStateOf("Folder") }
    var chosen by remember { mutableStateOf(setOf<String>()) }
    var page by remember { mutableStateOf(state.pages.first().id) }
    AlertDialog(onDismissRequest=onClose,title={Text("Create folder")},text={ Column {
        OutlinedTextField(title,{title=it.take(80)},label={Text("Folder name")},singleLine=true)
        var pageMenu by remember { mutableStateOf(false) }
        Box { TextButton(onClick={pageMenu=true}) { Text("Page ${state.pages.indexOfFirst { it.id==page }+1}") }; DropdownMenu(pageMenu,{pageMenu=false}) { state.pages.forEachIndexed { i,p -> DropdownMenuItem(text={Text("Page ${i+1}")},onClick={page=p.id;pageMenu=false}) } } }
        Text("Choose at least two apps")
        LazyColumn(Modifier.heightIn(max=300.dp)) { items(apps,key={it.component}) { app -> Row { Checkbox(app.component in chosen,{checked->chosen=if(checked) chosen+app.component else chosen-app.component}); TextButton(onClick={chosen=if(app.component in chosen) chosen-app.component else chosen+app.component}) { Text(app.label) } } } }
    } },confirmButton={TextButton(onClick={model.mutate { LayoutEditor.folder(it,chosen.toList(),title,page) };onClose()},enabled=chosen.size>=2) { Text("Create") }},dismissButton={TextButton(onClick=onClose) { Text("Cancel") }})
}

@Composable fun FolderDialog(tile: Tile,state: LauncherState,apps: List<AppEntry>,model: LauncherViewModel,onClose:()->Unit) {
    var name by remember(tile.id) { mutableStateOf(tile.title) }
    var manage by remember { mutableStateOf(false) }
    val byId=remember(apps) { apps.associateBy { it.component } }
    AlertDialog(onDismissRequest=onClose,title={Text(tile.title)},text={Column {
        if(manage) {
            OutlinedTextField(name,{name=it.take(80)},label={Text("Folder name")},singleLine=true)
            TextButton(onClick={model.mutate { LayoutEditor.editFolder(it,tile.id,name,tile.children) }}) { Text("Rename") }
            LazyColumn(Modifier.heightIn(max=280.dp)) { items(apps,key={it.component}) { app -> Row {
                Checkbox(app.component in tile.children,{checked -> val children=if(checked) tile.children+app.component else tile.children-app.component; model.mutate { LayoutEditor.editFolder(it,tile.id,name,children) }; if(children.size<2) onClose() })
                Text(app.label,Modifier.padding(top=12.dp))
            } } }
        } else LazyVerticalGrid(GridCells.Fixed(3),Modifier.heightIn(max=320.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            items(tile.children,key={it}) { component -> val app=byId[component]; if(app!=null) AppTile(app,model,state.settings.copy(iconSize=44),{model.launch(component);onClose()},{manage=true}) }
        }
    }},confirmButton={TextButton(onClick={manage=!manage}) { Text(if(manage) "Done" else "Edit folder") }},dismissButton={TextButton(onClick=onClose) { Text("Close") }})
}
