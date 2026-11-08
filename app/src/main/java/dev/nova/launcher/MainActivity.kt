package dev.nova.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    val model: LauncherViewModel by viewModels()
    lateinit var widgets: WidgetController
        private set
    private val roleResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { model.refresh() }
    private val installPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (packageManager.canRequestPackageInstalls()) pickApk.launch(arrayOf("application/vnd.android.package-archive"))
        else model.message.value = "Installation permission was not granted. You can still install apps from your app store."
    }
    private val pickApk = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                val file = withContext(Dispatchers.IO) {
                    File(cacheDir,"install").apply { mkdirs() }.listFiles()?.forEach { it.delete() }
                    File(cacheDir,"install/selected.apk").also { file ->
                        contentResolver.openInputStream(uri).use { input -> requireNotNull(input).use { source -> file.outputStream().use { target ->
                            val buffer = ByteArray(8192); var total = 0L
                            while (true) { val n = source.read(buffer); if(n < 0) break; total += n; require(total <= 512L*1024*1024) { "APK exceeds 512 MB" }; target.write(buffer,0,n) }
                        } } }
                    }
                }
                startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(FileProvider.getUriForFile(this@MainActivity,"$packageName.files",file),"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }.onFailure { model.message.value = "Could not open APK: ${it.message}" }
        }
    }
    var pendingRestore = androidx.compose.runtime.mutableStateOf<dev.nova.core.LauncherState?>(null)
    private val saveBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val snapshot=model.state.value
        if(uri!=null && snapshot!=null) lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) { contentResolver.openOutputStream(uri,"wt").use { out -> requireNotNull(out).write(dev.nova.core.StateCodec.export(snapshot).toByteArray(Charsets.UTF_8)) } } }
                .onSuccess { model.message.value="Backup saved. Widget bindings and usage history were excluded." }
                .onFailure { model.message.value="Could not save backup: ${it.message}" }
        }
    }
    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri).use { stream ->
                    requireNotNull(stream)
                    val bytes=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
                    while(true) { val n=stream.read(buffer); if(n<0) break; require(bytes.size()+n<=dev.nova.core.StateCodec.MAX_BYTES) { "Backup exceeds 1 MB" }; bytes.write(buffer,0,n) }
                    dev.nova.core.StateCodec.restore(bytes.toString("UTF-8"))
                }
            } }.onSuccess { pendingRestore.value=it }.onFailure { model.message.value="Backup was not restored: ${it.message}" }
        }
    }
    fun exportBackup() = saveBackup.launch("nova-backup-v1.json")
    fun importBackup() = openBackup.launch(arrayOf("application/json","text/plain","application/octet-stream"))
    fun confirmRestore() {
        val restored=pendingRestore.value ?: return
        lifecycleScope.launch {
            runCatching { model.store.replace(restored) }.onSuccess { pendingRestore.value=null; model.reloadState(); model.message.value="Layout restored. Re-add widgets on this device." }.onFailure { model.message.value="Restore failed; your current layout has been kept." }
        }
    }
    var homeSignal = androidx.compose.runtime.mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        widgets=WidgetController(this,model)
        lifecycleScope.launch { model.state.collect { it?.let(widgets::reconcile) } }
        setContent { NovaApp(model,this,homeSignal.intValue) }
    }
    override fun onStart() { super.onStart(); widgets.start() }
    override fun onStop() { widgets.stop(); super.onStop() }
    @Deprecated("Platform widget host configuration callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==WidgetController.CONFIGURE_REQUEST) widgets.configurationResult(resultCode)
    }
    override fun onResume() { super.onResume(); model.refresh() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); homeSignal.intValue++ }
    fun chooseHome() {
        if (Build.VERSION.SDK_INT >= 29) {
            val role = getSystemService(RoleManager::class.java)
            if (role.isRoleAvailable(RoleManager.ROLE_HOME) && !role.isRoleHeld(RoleManager.ROLE_HOME)) {
                roleResult.launch(role.createRequestRoleIntent(RoleManager.ROLE_HOME)); return
            }
        }
        openSystem(Intent(Settings.ACTION_HOME_SETTINGS))
    }
    fun openSystem(intent: Intent) { runCatching { startActivity(intent) }.onFailure { model.message.value = "This action is not available on this device." } }
    fun uninstall(packageName: String) = openSystem(Intent(Intent.ACTION_DELETE,Uri.parse("package:$packageName")))
    fun appInfo(packageName: String) = openSystem(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))
    fun installApk() {
        if (packageManager.canRequestPackageInstalls()) pickApk.launch(arrayOf("application/vnd.android.package-archive"))
        else installPermission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:$packageName")))
    }
}
