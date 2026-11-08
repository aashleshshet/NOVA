package dev.nova.core
import org.junit.Assert.*
import org.junit.Test

class StateCodecTest {
    @Test fun stateRoundTrip() {
        val state = LauncherState(pages = listOf(Page(tiles = listOf(Tile("one", "dev.test/.Main")))))
        assertEquals(state, StateCodec.decode(StateCodec.encode(state)))
    }
    @Test fun rejectsFutureVersion() { assertThrows(IllegalArgumentException::class.java) { StateCodec.decode("{\"version\":9}") } }
    @Test fun rejectsInvalidGrid() { assertThrows(IllegalArgumentException::class.java) { StateCodec.validate(LauncherState(settings = Settings(columns = 0))) } }
    @Test fun backupsDropDeviceIdsAndConsent() {
        val state = LauncherState(settings = Settings(usageConsent = true, suggestions = true), widgets = listOf(WidgetPlacement(8,"home","dev.test/.Widget")), history = mapOf("dev.test/.Main" to LaunchRecord(3,12)))
        val restored = StateCodec.restore(StateCodec.export(state))
        assertTrue(restored.widgets.isEmpty()); assertTrue(restored.history.isEmpty()); assertFalse(restored.settings.usageConsent)
    }
}
