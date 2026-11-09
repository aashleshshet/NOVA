package dev.nova.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import dev.nova.core.AppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppCatalog(private val context: Context) {
    val iconPacks = IconPacks(context)
    private val pm = context.packageManager
    private val cache = LruCache<String, Bitmap>(120)
    suspend fun discover(): List<AppEntry> = withContext(Dispatchers.IO) {
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.exported && it.activityInfo.enabled && it.activityInfo.packageName != context.packageName }
            .mapNotNull { info -> runCatching {
                val pkg = info.activityInfo.packageName
                AppEntry(ComponentName(pkg, info.activityInfo.name).flattenToString(),pkg,info.loadLabel(pm).toString(),pm.getPackageInfo(pkg,0).firstInstallTime)
            }.getOrNull() }.distinctBy { it.component }
    }
    suspend fun missingPackages(candidates: Set<String>): Set<String> = withContext(Dispatchers.IO) {
        candidates.filter { pkg ->
            try {
                // Retain disabled, archived, or temporarily unavailable packages with retained data.
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg,PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS)
                false
            } catch(_: PackageManager.NameNotFoundException) { true }
        }.toSet()
    }
    suspend fun icon(component: String, pack: String = ""): Bitmap? = withContext(Dispatchers.IO) {
        cache.get("$pack|$component") ?: runCatching {
            iconPacks.icon(pack,component)?.also { cache.put("$pack|$component",it) } ?:
            pm.getActivityIcon(ComponentName.unflattenFromString(component) ?: return@withContext null).toBitmap(144,144).also { cache.put("$pack|$component",it) }
        }.getOrNull()
    }
    fun clearIcons() { cache.evictAll(); iconPacks.clear() }
    fun launch(component: String): Result<Unit> = runCatching {
        val name = requireNotNull(ComponentName.unflattenFromString(component))
        val info = pm.getActivityInfo(name,0)
        check(info.exported && info.enabled) { "App is unavailable" }
        context.startActivity(Intent.makeMainActivity(name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
