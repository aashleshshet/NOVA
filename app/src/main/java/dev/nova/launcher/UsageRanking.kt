package dev.nova.launcher

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import dev.nova.core.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageRanking(private val context: Context) {
    @Suppress("DEPRECATION")
    fun hasAccess(): Boolean = context.getSystemService(AppOpsManager::class.java).checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),context.packageName)==AppOpsManager.MODE_ALLOWED
    suspend fun read(settings: Settings): Map<String,Long> = withContext(Dispatchers.IO) {
        if(!settings.suggestions || !settings.usageConsent || !hasAccess()) return@withContext emptyMap()
        val now=System.currentTimeMillis()
        val start=maxOf(now-7L*24*60*60*1000,settings.usageSince)
        // Events allow reset to exclude earlier activity precisely, unlike bucketed aggregate stats.
        val events=context.getSystemService(UsageStatsManager::class.java).queryEvents(start,now)
        val event=android.app.usage.UsageEvents.Event()
        val began=mutableMapOf<String,Long>(); val totals=mutableMapOf<String,Long>(); var count=0
        while(events.hasNextEvent() && count++<100_000) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION")
            when(event.eventType) {
                android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND -> began[event.packageName]=event.timeStamp
                android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND -> began.remove(event.packageName)?.let { since -> totals[event.packageName]=(totals[event.packageName] ?: 0)+(event.timeStamp-since).coerceIn(0,24*60*60*1000L) }
            }
        }
        totals
    }
}
