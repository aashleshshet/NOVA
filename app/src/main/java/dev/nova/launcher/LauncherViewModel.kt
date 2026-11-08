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
    val apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val message = MutableStateFlow<String?>(null)
    val loadingApps = MutableStateFlow(true)
    private var refreshJob: Job? = null
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_REMOVED && !intent.getBooleanExtra(Intent.EXTRA_REPLACING,false)) {
                intent.data?.schemeSpecificPart?.let { pkg -> mutate { LauncherOps.removePackage(it,pkg) } }
            }
            catalog.clearIcons(); refresh()
        }
    }
    init {
        viewModelScope.launch {
            store.state.catch { message.value = "Could not read your saved layout. Restore a backup in Studio; existing data has been kept." }.collect { state.value = it }
        }
        ContextCompat.registerReceiver(application,receiver,IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED); addAction(Intent.ACTION_PACKAGE_CHANGED); addAction(Intent.ACTION_PACKAGE_REPLACED); addDataScheme("package")
        },ContextCompat.RECEIVER_NOT_EXPORTED)
        refresh()
    }
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try { apps.value = catalog.discover() } catch (e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; message.value = "Cannot read apps. Try reopening NOVA." }
            finally { loadingApps.value = false }
        }
    }
    fun mutate(change: (LauncherState) -> LauncherState) { viewModelScope.launch {
        try { store.update(change) } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; message.value = e.message ?: "Could not save changes" }
    } }
    fun settings(settings: Settings) = mutate { it.copy(settings=settings) }
    fun pin(component: String, page: String?, dock: Boolean = false) = mutate { LauncherOps.pin(it,component,page,dock) }
    fun remove(id: String) = mutate { LauncherOps.remove(it,id) }
    fun launch(component: String) { catalog.launch(component).onFailure { message.value = "This app is unavailable. It may have been removed or disabled."; refresh() } }
    override fun onCleared() { getApplication<Application>().unregisterReceiver(receiver); super.onCleared() }
}
