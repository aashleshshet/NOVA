package dev.nova.launcher

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.nova.core.LauncherState
import dev.nova.core.StateCodec
import kotlinx.coroutines.flow.map

private val Context.novaDataStore by preferencesDataStore("nova")
class StateStore(context: Context) {
    private val dataStore = context.applicationContext.novaDataStore
    private val key = stringPreferencesKey("state_v1")
    val state = dataStore.data.map { prefs -> prefs[key]?.let(StateCodec::decode) ?: LauncherState() }
    suspend fun update(transform: (LauncherState) -> LauncherState) {
        dataStore.edit { prefs ->
            val current = prefs[key]?.let(StateCodec::decode) ?: LauncherState()
            prefs[key] = StateCodec.encode(transform(current))
        }
    }
    suspend fun replace(state: LauncherState) { dataStore.edit { it[key] = StateCodec.encode(state) } }
}
