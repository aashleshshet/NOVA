package dev.nova.launcher

import android.app.Activity
import android.appwidget.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import dev.nova.core.LauncherState
import dev.nova.core.WidgetPlacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Host IDs belong to this install. Never import them from backup or accept arbitrary result IDs. */
class WidgetController(private val activity: MainActivity, private val model: LauncherViewModel) {
    companion object { const val HOST_ID=0x4e4f5641; const val CONFIGURE_REQUEST=701 }
    val manager=AppWidgetManager.getInstance(activity)
    val host=object: AppWidgetHost(activity,HOST_ID) {
        override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo): AppWidgetHostView = object: AppWidgetHostView(context) {
            override fun getErrorView(): View = TextView(context).apply { text="Widget could not load. Remove and add it again."; setPadding(16,16,16,16) }
        }
    }
    private val pending=activity.getSharedPreferences("widget_binding",Context.MODE_PRIVATE)
    private val bindResult=activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode==Activity.RESULT_OK && manager.getAppWidgetInfo(pendingId())!=null) configureOrFinish() else cancel("Widget permission was not granted.")
    }
    fun pendingId(): Int=pending.getInt("id",AppWidgetManager.INVALID_APPWIDGET_ID)
    suspend fun providers(): List<AppWidgetProviderInfo> = withContext(Dispatchers.IO) {
        manager.getInstalledProvidersForProfile(Process.myUserHandle()).filter { it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 }.sortedBy { it.loadLabel(activity.packageManager).lowercase() }
    }
    fun start() { runCatching { host.startListening() }.onFailure { model.message.value="Widgets are temporarily unavailable. Reopen NOVA to retry." } }
    fun stop() { runCatching { host.stopListening() } }
    fun begin(info: AppWidgetProviderInfo,pageId: String) {
        if(pendingId()!=AppWidgetManager.INVALID_APPWIDGET_ID) cancel(null)
        runCatching {
            check(model.state.value?.widgets?.size?.let { it<48 } == true) { "Maximum 48 widgets" }
            val id=host.allocateAppWidgetId()
            if(!pending.edit().putInt("id",id).putString("page",pageId).commit()) { host.deleteAppWidgetId(id); error("Could not save widget binding") }
            val options=Bundle().apply { putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) }
            if(manager.bindAppWidgetIdIfAllowed(id,info.profile,info.provider,options)) configureOrFinish()
            else bindResult.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,info.provider).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE,info.profile).putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS,options))
        }.onFailure { cancel("Could not add widget: ${it.message}") }
    }
    private fun configureOrFinish() {
        val id=pendingId()
        val info=manager.getAppWidgetInfo(id) ?: return cancel("Widget provider is unavailable.")
        if(info.configure==null) finish() else runCatching { host.startAppWidgetConfigureActivityForResult(activity,id,0,CONFIGURE_REQUEST,null) }.onFailure { cancel("This widget's setup could not open.") }
    }
    fun configurationResult(resultCode: Int) { if(resultCode==Activity.RESULT_OK) finish() else cancel("Widget setup was cancelled.") }
    private fun finish() {
        val id=pendingId()
        val info=manager.getAppWidgetInfo(id) ?: return cancel("Widget provider is unavailable.")
        activity.lifecycleScope.launch {
            runCatching {
                model.store.update { state ->
                    val page=pending.getString("page",null)?.takeIf { p->state.pages.any { it.id==p } } ?: state.pages.first().id
                    val height=(info.minHeight/activity.resources.displayMetrics.density).toInt().coerceIn(100,600)
                    state.copy(widgets=state.widgets.filterNot { it.id==id } + WidgetPlacement(id,page,info.provider.flattenToString(),state.settings.columns,height))
                }
                pending.edit().clear().commit()
            }.onFailure { cancel("Could not save widget: ${it.message}") }
        }
    }
    private fun cancel(message: String?) {
        val id=pendingId()
        if(id!=AppWidgetManager.INVALID_APPWIDGET_ID) runCatching { host.deleteAppWidgetId(id) }
        pending.edit().clear().commit()
        if(message!=null) model.message.value=message
    }
    fun reconcile(state: LauncherState) {
        // Runs only after a successful state read. Keep in-flight binding IDs until result/cancellation.
        runCatching { host.appWidgetIds.filter { id->state.widgets.none { it.id==id } && id!=pendingId() }.forEach(host::deleteAppWidgetId) }
    }
    fun remove(id: Int) { activity.lifecycleScope.launch {
        runCatching { model.store.update { it.copy(widgets=it.widgets.filterNot { w->w.id==id }) }; host.deleteAppWidgetId(id) }.onFailure { model.message.value="Could not remove widget. Try again." }
    } }
}
