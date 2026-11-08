package dev.nova.core
import org.junit.Assert.*
import org.junit.Test
class LayoutEditorTest {
    private val base=LauncherState(pages=listOf(Page(tiles=listOf(Tile("a","dev.a/.Main"),Tile("b","dev.b/.Main"))),Page("second")))
    @Test fun movingBetweenPagesAndDockIsLossless() {
        val moved=LayoutEditor.move(base,"a","second")
        assertEquals(listOf("b"),moved.pages[0].tiles.map { it.id })
        assertEquals("a",moved.pages[1].tiles.single().id)
        assertEquals("a",LayoutEditor.move(moved,"a",null).dock.single().id)
    }
    @Test fun reorderAndFolderRoundTrip() {
        assertEquals(listOf("b","a"),LayoutEditor.move(base,"a","home",1).pages[0].tiles.map { it.id })
        val f=LayoutEditor.folder(base,listOf("dev.a/.Main","dev.b/.Main"),"Tools","home")
        assertTrue(f.pages[0].tiles.single().isFolder)
        assertEquals(f,StateCodec.decode(StateCodec.encode(f)))
        assertEquals("dev.a/.Main",LayoutEditor.editFolder(f,f.pages[0].tiles[0].id,"Tools",listOf("dev.a/.Main")).pages[0].tiles.single().component)
    }
    @Test fun dockReductionMovesOverflowToHome() {
        val s=base.copy(dock=(1..6).map { Tile("d$it","dev.d$it/.Main") },settings=Settings(dockSize=6))
        val resized=LayoutEditor.settings(s,Settings(dockSize=3))
        assertEquals(3,resized.dock.size); assertEquals(5,resized.pages[0].tiles.size)
        StateCodec.validate(resized)
    }
    @Test fun populatedOrLastPageCannotBeRemoved() {
        assertThrows(IllegalArgumentException::class.java) { LayoutEditor.removePage(base,"home") }
        assertThrows(IllegalArgumentException::class.java) { LayoutEditor.removePage(LauncherState(),"home") }
        assertEquals(1,LayoutEditor.removePage(base,"second").pages.size)
    }
}
