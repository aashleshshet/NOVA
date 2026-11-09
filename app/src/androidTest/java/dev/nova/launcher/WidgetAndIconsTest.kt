package dev.nova.launcher

import android.appwidget.*
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Xml
import android.widget.RemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetAndIconsTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    @Test fun widgetCanBindHostResizeAndRelease() {
        val automation=instrumentation.uiAutomation
        automation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
        val host=AppWidgetHost(context,4242)
        val manager=AppWidgetManager.getInstance(context)
        val id=host.allocateAppWidgetId()
        try {
            val provider=ComponentName(instrumentation.context.packageName,FixtureWidget::class.java.name)
            assertTrue("Fixture provider must bind",manager.bindAppWidgetIdIfAllowed(id,provider))
            val info=manager.getAppWidgetInfo(id)
            assertNotNull(info)
            instrumentation.runOnMainSync {
                host.startListening()
                val view=host.createView(context,id,info)
                view.updateAppWidgetSize(Bundle(),160,100,320,200)
                assertEquals(id,view.appWidgetId)
                host.stopListening()
            }
            val padding=AppWidgetHostView.getDefaultPaddingForWidget(context,provider,null)
            val horizontalPadding=((padding.left+padding.right)/context.resources.displayMetrics.density).toInt()
            assertEquals(160-horizontalPadding,manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH))
        } finally { host.deleteAppWidgetId(id);automation.dropShellPermissionIdentity() }
        assertNull(manager.getAppWidgetInfo(id))
    }
    @Test fun staticIconPackMappingNormalizesComponents() {
        val parser=Xml.newPullParser();parser.setInput("<resources><item component='ComponentInfo{dev.example/.Main}' drawable='sample'/><item component='bad' drawable='bad'/></resources>".reader())
        assertEquals(mapOf("dev.example/dev.example.Main" to "sample"),IconPacks.parse(parser))
    }
    @Test fun installedStaticIconPackLoadsActualDrawable() = runBlocking {
        val packs=IconPacks(context)
        assertTrue(packs.discover().any { it.packageName==instrumentation.context.packageName })
        val bitmap=packs.icon(instrumentation.context.packageName,"dev.nova.launcher/dev.nova.launcher.MainActivity")
        assertNotNull(bitmap)
        assertEquals(android.graphics.Color.GREEN,bitmap!!.getPixel(bitmap.width/2,bitmap.height/2))
    }
    @Test fun missingIconPackFallsBackToRealIcon() = runBlocking {
        val catalog=AppCatalog(context)
        val app=catalog.discover().first()
        assertNotNull(catalog.icon(app.component,"dev.missing.iconpack"))
    }
}
