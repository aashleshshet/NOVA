package dev.nova.core

import kotlin.math.exp
import kotlin.math.ln

object Suggestions {
    const val RETENTION_MS=30L*24*60*60*1000
    fun record(state: LauncherState,component: String,now: Long): LauncherState {
        if(!state.settings.suggestions) return state
        val history=state.history.filterValues { it.lastLaunch>=now-RETENTION_MS }.toMutableMap()
        val old=history[component] ?: LaunchRecord()
        history[component]=LaunchRecord((old.count.toLong()+1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),now)
        return state.copy(history=history.entries.sortedByDescending { it.value.lastLaunch }.take(2000).associate { it.key to it.value })
    }
    fun ranked(apps: List<AppEntry>,state: LauncherState,now: Long,usage: Map<String,Long> = emptyMap()): List<AppEntry> {
        if(!state.settings.suggestions) return emptyList()
        fun score(app: AppEntry): Double {
            val history=state.history[app.component]
            val local=if(history==null || history.lastLaunch<now-RETENTION_MS) 0.0 else ln(1.0+history.count)*exp(-(now-history.lastLaunch).coerceAtLeast(0).toDouble()/(7*24*60*60*1000.0))
            val external=if(state.settings.usageConsent) ln(1.0+(usage[app.packageName] ?: 0).coerceAtLeast(0)/60_000.0)*.3 else 0.0
            return local+external
        }
        return apps.map { it to score(it) }.filter { it.second>0 }.sortedWith(compareByDescending<Pair<AppEntry,Double>> { it.second }.thenBy { it.first.label.lowercase() }).map { it.first }
    }
    fun disable(state: LauncherState): LauncherState = state.copy(history=emptyMap(),settings=state.settings.copy(suggestions=false,usageConsent=false,sort=SortOrder.NAME,usageSince=0))
    fun reset(state: LauncherState,now: Long): LauncherState = state.copy(history=emptyMap(),settings=state.settings.copy(usageSince=now))
}
