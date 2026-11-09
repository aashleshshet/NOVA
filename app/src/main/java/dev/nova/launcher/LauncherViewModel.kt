package dev.nova.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nova.core.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val store = (application as NovaApplication).store
    val catalog = AppCatalog(application)
    val state = MutableStateFlow<LauncherState?>(null)
    val iconRevision = MutableStateFlow(0)
    val apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val message = MutableStateFlow<String?>(null)
    val usageRanking = UsageRanking(application)
    val usage = MutableStateFlow<Map<String,Long>>(emptyMap())
    val loadingApps = MutableStateFlow(true)
    private var refreshJob: Job? = null
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_REMOVED && !intent.getBooleanExtra(Intent.EXTRA_REPLACING,false)) {
                intent.data?.schemeSpecificPart?.let { pkg -> mutate { LauncherOps.removePackage(it,pkg) } }
            }
            catalog.clearIcons(); iconRevision.value++; refresh()
        }
    }
    val loadFailed = MutableStateFlow(false)
    private var stateJob: Job? = null
    fun reloadState() {
        stateJob?.cancel(); loadFailed.value=false
        stateJob=viewModelScope.launch { store.state.catch { loadFailed.value=true; message.value="Could not read your saved layout. Restore a backup; existing data has been kept." }.collect { val firstLoad=state.value==null; state.value=it; if(firstLoad) refresh()
            val cutoff=System.currentTimeMillis()-Suggestions.RETENTION_MS
            if(it.history.values.any { record->record.lastLaunch<cutoff }) store.update { current->current.copy(history=current.history.filterValues { record->record.lastLaunch>=cutoff }) }
        } }
    }
    init {
        reloadState()
        ContextCompat.registerReceiver(application,receiver,IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED); addAction(Intent.ACTION_PACKAGE_CHANGED); addAction(Intent.ACTION_PACKAGE_REPLACED); addDataScheme("package")
        },ContextCompat.RECEIVER_NOT_EXPORTED)
        refresh()
    }
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                apps.value = catalog.discover()
                state.value?.let { current ->
                    val components=current.pages.flatMap { it.tiles }.plus(current.dock).flatMap { if(it.isFolder) it.children else listOf(it.component) }
                    val known=(components+current.widgets.map { it.provider }+current.history.keys).map { it.substringBefore('/') }.toSet()
                    val installed=apps.value.map { it.packageName }.toSet()
                    val missing=catalog.missingPackages(known-installed)
                    if(missing.isNotEmpty()) store.update { previous->missing.fold(previous) { state,pkg->LauncherOps.removePackage(state,pkg) } }
                }
            } catch (e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; message.value = "Cannot read apps. Try reopening NOVA." }
            finally { loadingApps.value = false }
            state.value?.settings?.let { settings -> usage.value=runCatching { usageRanking.read(settings) }.getOrDefault(emptyMap()) }
        }
    }
    fun mutate(change: (LauncherState) -> LauncherState) { viewModelScope.launch {
        try { store.update(change) } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; message.value = e.message ?: "Could not save changes" }
    } }
    fun resetSuggestions() { usage.value=emptyMap(); mutate { Suggestions.reset(it,System.currentTimeMillis()) } }
    fun disableSuggestions() { usage.value=emptyMap(); mutate(Suggestions::disable) }
    fun settings(settings: Settings) = mutate { LayoutEditor.settings(it,settings) }
    fun pin(component: String, page: String?, dock: Boolean = false) = mutate { LauncherOps.pin(it,component,page,dock) }
    fun remove(id: String) = mutate { LauncherOps.remove(it,id) }
    fun launch(component: String) { catalog.launch(component).onSuccess { mutate { Suggestions.record(it,component,System.currentTimeMillis()) } }.onFailure { message.value = "This app is unavailable. It may have been removed or disabled."; refresh() } }
    override fun onCleared() { getApplication<Application>().unregisterReceiver(receiver); super.onCleared() }
}
