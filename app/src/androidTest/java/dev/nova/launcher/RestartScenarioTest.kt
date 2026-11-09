package dev.nova.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.test.platform.app.InstrumentationRegistry
import dev.nova.core.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/** Also invoked one method at a time by scripts/restart-check.py across actual process death/reboot. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RestartScenarioTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    @Test fun aSeedPersistentLayout() = runBlocking {
        val apps=AppCatalog(context).discover().take(3)
        assertTrue(apps.size>=3)
        val host=AppWidgetHost(context,WidgetController.HOST_ID)
        val id=host.allocateAppWidgetId()
        instrumentation.uiAutomation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
        try { assertTrue(AppWidgetManager.getInstance(context).bindAppWidgetIdIfAllowed(id,ComponentName(instrumentation.context.packageName,FixtureWidget::class.java.name))) }
        finally { instrumentation.uiAutomation.dropShellPermissionIdentity() }
        val state=LauncherState(settings=Settings(theme=ThemeMode.DARK,columns=5,spacing=16),pages=listOf(Page(tiles=listOf(Tile("restart-folder",title="Persisted tools",children=apps.take(2).map { it.component }))),Page("second")),dock=listOf(Tile("restart-dock",apps[2].component)),widgets=listOf(WidgetPlacement(id,"second",ComponentName(instrumentation.context.packageName,FixtureWidget::class.java.name).flattenToString())))
        StateStore(context).replace(state)
        assertEquals(state,StateStore(context).state.first())
    }
    @Test fun bVerifyPersistentLayout() = runBlocking {
        val state=StateStore(context).state.first()
        assertEquals(ThemeMode.DARK,state.settings.theme)
        assertEquals(5,state.settings.columns)
        assertEquals("Persisted tools",state.pages.first().tiles.single().title)
        assertEquals(2,state.pages.size)
        assertEquals(1,state.dock.size)
        assertNotNull(AppWidgetManager.getInstance(context).getAppWidgetInfo(state.widgets.single().id))
    }
}
