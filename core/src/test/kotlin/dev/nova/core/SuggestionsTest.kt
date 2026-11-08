package dev.nova.core
import org.junit.Assert.*
import org.junit.Test
class SuggestionsTest {
    private val app=AppEntry("dev.a/.Main","dev.a","Alpha")
    @Test fun disabledMeansNoRecordingOrRanking() {
        assertEquals(LauncherState(),Suggestions.record(LauncherState(),app.component,10))
        assertTrue(Suggestions.ranked(listOf(app),LauncherState(),10,mapOf("dev.a" to 10000)).isEmpty())
    }
    @Test fun externalUsageNeedsSeparateConsent() {
        val state=LauncherState(settings=Settings(suggestions=true))
        assertTrue(Suggestions.ranked(listOf(app),state,10,mapOf("dev.a" to 999999)).isEmpty())
        assertEquals(app,Suggestions.ranked(listOf(app),state.copy(settings=state.settings.copy(usageConsent=true)),10,mapOf("dev.a" to 999999)).single())
    }
    @Test fun resetAndDisableEraseHistory() {
        val state=Suggestions.record(LauncherState(settings=Settings(suggestions=true)),app.component,10)
        assertEquals(1,state.history.size)
        assertTrue(Suggestions.reset(state,20).history.isEmpty())
        assertEquals(20,Suggestions.reset(state,20).settings.usageSince)
        assertFalse(Suggestions.disable(state).settings.suggestions)
        assertTrue(Suggestions.disable(state).history.isEmpty())
    }
    @Test fun expiryAndRemovedAppsNeverSuggest() {
        val state=Suggestions.record(LauncherState(settings=Settings(suggestions=true)),app.component,10)
        assertTrue(Suggestions.ranked(listOf(app),state,Suggestions.RETENTION_MS+11).isEmpty())
        assertTrue(Suggestions.ranked(emptyList(),state,11).isEmpty())
    }
}
