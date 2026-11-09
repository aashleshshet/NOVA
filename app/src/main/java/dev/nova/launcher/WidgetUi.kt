@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.nova.launcher

import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import dev.nova.core.*
import kotlin.math.ceil

@Composable fun WidgetPicker(state: LauncherState,controller: WidgetController,model: LauncherViewModel,onClose:()->Unit) {
    var providers by remember { mutableStateOf<List<AppWidgetProviderInfo>?>(null) }
    var page by remember { mutableStateOf(state.pages.first().id) }
    val context=LocalContext.current
    LaunchedEffect(Unit) { runCatching { controller.providers() }.onSuccess { providers=it }.onFailure { providers=emptyList();model.message.value="Could not load widget providers." } }
    ModalBottomSheet(onDismissRequest=onClose) {
        Column(Modifier.padding(24.dp)) {
            Text("Add a widget",style=MaterialTheme.typography.headlineMedium)
            var menu by remember { mutableStateOf(false) }
            Box { TextButton(onClick={menu=true}) { Text("Place on page ${state.pages.indexOfFirst { it.id==page }+1}") }; DropdownMenu(menu,{menu=false}) { state.pages.forEachIndexed { i,p -> DropdownMenuItem(text={Text("Page ${i+1}")},onClick={page=p.id;menu=false}) } } }
            if(providers==null) CircularProgressIndicator()
            else if(providers!!.isEmpty()) Text("No home-screen widgets are available. Install an app that provides widgets, then return here.")
            LazyColumn(Modifier.heightIn(max=480.dp)) { items(providers.orEmpty(),key={it.provider.flattenToString()}) { info ->
                TextButton(onClick={controller.begin(info,page);onClose()},modifier=Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth()) { Text(info.loadLabel(context.packageManager),style=MaterialTheme.typography.titleMedium); Text(info.provider.packageName,style=MaterialTheme.typography.bodySmall) } }
            } }
        }
    }
}

@Composable fun HostedWidget(widget: WidgetPlacement,controller: WidgetController,editing: Boolean,onEdit:()->Unit) {
    val info=remember(widget.id,widget.provider) { controller.manager.getAppWidgetInfo(widget.id) }
    Column {
        if(editing) TextButton(onClick=onEdit) { Text("Resize / move widget") }
        BoxWithConstraints(Modifier.fillMaxWidth().height(widget.height.dp)) {
            val width=maxWidth.value.toInt().coerceAtLeast(1)
            if(info==null) Text("Widget unavailable. Use Edit to remove it.")
            else AndroidView(factory={context->controller.host.createView(context,widget.id,info)},modifier=Modifier.fillMaxSize(),update={ view ->
                if(android.os.Build.VERSION.SDK_INT>=31) view.updateAppWidgetSize(Bundle(),listOf(android.util.SizeF(width.toFloat(),widget.height.toFloat())))
                else { @Suppress("DEPRECATION") view.updateAppWidgetSize(Bundle(),width,widget.height,width,widget.height) }
            })
        }
    }
}

@Composable fun WidgetEditor(widget: WidgetPlacement,state: LauncherState,controller: WidgetController,model: LauncherViewModel,onClose:()->Unit) {
    val info=controller.manager.getAppWidgetInfo(widget.id)
    val density=LocalDensity.current.density
    val context=LocalContext.current
    val widthDp=context.resources.displayMetrics.widthPixels/density-32
    val cellWidth=widthDp/state.settings.columns
    val minSpan=ceil((info?.minResizeWidth?.takeIf { it>0 } ?: info?.minWidth ?: 0)/density/cellWidth).toInt().coerceIn(1,state.settings.columns)
    val minHeight=((info?.minResizeHeight?.takeIf { it>0 } ?: info?.minHeight ?: 80)/density).toInt().coerceIn(80,600)
    ModalBottomSheet(onDismissRequest=onClose) { Column(Modifier.padding(24.dp)) {
        Text("Widget layout",style=MaterialTheme.typography.headlineMedium)
        if(info!=null && info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL!=0 && minSpan<state.settings.columns) StepSlider("Columns",widget.span.coerceIn(minSpan,state.settings.columns),minSpan..state.settings.columns) { n->model.mutate { it.copy(widgets=it.widgets.map { w->if(w.id==widget.id) w.copy(span=n) else w }) } }
        if(info!=null && info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL!=0 && minHeight<600) StepSlider("Height (dp)",widget.height.coerceIn(minHeight,600),minHeight..600) { n->model.mutate { it.copy(widgets=it.widgets.map { w->if(w.id==widget.id) w.copy(height=n) else w }) } }
        state.pages.forEachIndexed { i,p->TextButton(onClick={model.mutate { it.copy(widgets=it.widgets.map { w->if(w.id==widget.id) w.copy(pageId=p.id) else w }) };onClose()}) { Text("Move to page ${i+1}") } }
        TextButton(onClick={model.mutate { val list=it.widgets.toMutableList();val i=list.indexOfFirst { w->w.id==widget.id };if(i>0) { val previous=list[i-1];list[i-1]=list[i];list[i]=previous };it.copy(widgets=list) }}) { Text("Move earlier") }
        TextButton(onClick={controller.remove(widget.id);onClose()}) { Text("Remove widget") }
    } }
}
