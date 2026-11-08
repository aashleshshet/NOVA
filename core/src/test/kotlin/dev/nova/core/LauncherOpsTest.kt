package dev.nova.core
import org.junit.Assert.*
import org.junit.Test
class LauncherOpsTest {
    @Test fun pinsPersistWithoutDuplicates() {
        val s = LauncherOps.pin(LauncherState(), "dev.test/.Main", "home")
        assertEquals(s, LauncherOps.pin(s, "dev.test/.Main", "home"))
        assertEquals(s, StateCodec.decode(StateCodec.encode(s)))
    }
    @Test fun fullDockRejectsWithoutLosingApps() {
        val s = (1..4).fold(LauncherState()) { s, i -> LauncherOps.pin(s,"dev.app$i/.Main",null,true) }
        assertThrows(IllegalArgumentException::class.java) { LauncherOps.pin(s,"dev.app5/.Main",null,true) }
    }
    @Test fun removalPrunesFoldersAndHistory() {
        val s = LauncherState(pages = listOf(Page(tiles=listOf(Tile("f",children=listOf("dev.one/.Main","dev.two/.Main"))))),history=mapOf("dev.one/.Main" to LaunchRecord(1,3)))
        val result = LauncherOps.removePackage(s,"dev.one")
        assertEquals("dev.two/.Main",result.pages[0].tiles[0].component); assertTrue(result.history.isEmpty())
    }
    @Test fun searchMatchesAllTermsAndSortsActualApps() {
        val apps=listOf(AppEntry("dev.z/.Main","dev.z","Zebra",1),AppEntry("dev.a/.Main","dev.a","Alpha Maps",3))
        assertEquals("Alpha Maps",LauncherOps.apps(apps,"  MAPS alpha  ",SortOrder.NAME).single().label)
        assertEquals("Alpha Maps",LauncherOps.apps(apps,"",SortOrder.RECENT).first().label)
        assertTrue(LauncherOps.apps(apps,"not installed",SortOrder.NAME).isEmpty())
    }
}
