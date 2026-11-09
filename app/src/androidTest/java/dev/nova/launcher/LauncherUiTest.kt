package dev.nova.launcher

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import dev.nova.core.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File

class LauncherUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private val device get()=UiDevice.getInstance(instrumentation)
    private lateinit var original: LauncherState
    @Before fun reset() {
        runBlocking { original=StateStore(context).state.first();StateStore(context).replace(LauncherState()) }
        compose.waitUntil(10_000) { compose.activity.model.state.value==LauncherState() && compose.activity.model.apps.value.isNotEmpty() }
        compose.onNodeWithTag("home-screen").assertExists()
    }
    @After fun restore() { runBlocking { StateStore(context).replace(original) } }
    @Test fun drawerSearchAndPinRealApp() {
        val app=compose.activity.model.apps.value.first()
        compose.onNodeWithTag("open-drawer").performClick()
        compose.onNodeWithTag("app-search").performTextInput(app.label)
        compose.onAllNodesWithContentDescription(app.label).onFirst().assertExists().performTouchInput { longClick() }
        compose.onNodeWithText("Add to page 1").performClick()
        compose.waitUntil(5000) { compose.activity.model.state.value?.pages?.first()?.tiles?.any { it.component==app.component }==true }
        compose.onNodeWithTag("app-search").performTextReplacement("__no_installed_app_matches__")
        compose.onNodeWithText("No apps match “__no_installed_app_matches__”").assertExists()
    }
    @Test fun launchesActualInstalledActivity() {
        val app=compose.activity.model.apps.value.firstOrNull { it.packageName=="com.android.settings" } ?: compose.activity.model.apps.value.first()
        compose.runOnIdle { compose.activity.model.launch(app.component) }
        assertTrue("Expected ${app.packageName} to open",device.wait(Until.hasObject(By.pkg(app.packageName).depth(0)),5000))
        device.pressBack()
        device.wait(Until.hasObject(By.pkg(context.packageName).depth(0)),5000)
    }
    @Test fun persistedThemeAndFolderSurviveActivityRecreation() {
        val apps=compose.activity.model.apps.value.take(2)
        val saved=LayoutEditor.folder(LauncherState(settings=Settings(theme=ThemeMode.LIGHT)),apps.map { it.component },"Daily","home")
        runBlocking { StateStore(context).replace(saved) }
        compose.waitUntil(5000) { compose.activity.model.state.value==saved }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Daily").assertExists().performClick()
        compose.onNodeWithText("Edit folder").assertExists()
        assertEquals(saved,runBlocking { StateStore(context).state.first() })
    }
    @Test fun dragReordersPersistentHomeTiles() {
        val apps=compose.activity.model.apps.value.take(2)
        val saved=LauncherState(pages=listOf(Page(tiles=apps.mapIndexed { i,a->Tile("tile$i",a.component) })))
        runBlocking { StateStore(context).replace(saved) }
        compose.waitUntil(5000) { compose.activity.model.state.value==saved }
        compose.onNodeWithContentDescription("Edit home").performClick()
        val start=compose.onNodeWithTag("tile-tile0").fetchSemanticsNode().boundsInRoot
        val end=compose.onNodeWithTag("tile-tile1").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("tile-tile0").performTouchInput {
            down(center);advanceEventTime(700)
            moveTo(end.center-start.topLeft,300);up()
        }
        compose.waitUntil(5000) { compose.activity.model.state.value?.pages?.first()?.tiles?.first()?.id=="tile1" }
    }
    @Test fun dragMovesFolderWithoutOpeningIt() {
        val apps=compose.activity.model.apps.value.take(3)
        val saved=LauncherState(pages=listOf(Page(tiles=listOf(Tile("folder",title="Tools",children=apps.take(2).map { it.component }),Tile("app",apps[2].component)))))
        runBlocking { StateStore(context).replace(saved) }
        compose.waitUntil(5000) { compose.activity.model.state.value==saved }
        compose.onNodeWithContentDescription("Edit home").performClick()
        val start=compose.onNodeWithTag("tile-folder").fetchSemanticsNode().boundsInRoot
        val end=compose.onNodeWithTag("tile-app").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("tile-folder").performTouchInput {
            down(Offset(center.x,20f));advanceEventTime(700)
            moveTo(end.center-start.topLeft,300);up()
        }
        compose.waitUntil(5000) { compose.activity.model.state.value?.pages?.first()?.tiles?.first()?.id=="app" }
        compose.onNodeWithText("Edit folder").assertDoesNotExist()
    }
    @Test fun swipeUpOpensDrawerFromClock() {
        compose.onNodeWithTag("gesture-clock").performTouchInput { swipeUp(durationMillis=350) }
        compose.onNodeWithTag("app-search").assertExists()
    }
    @Test fun studioPreviewAndConsentAreRealControls() {
        compose.onNodeWithContentDescription("Open customization studio").performClick()
        compose.onNodeWithTag("live-preview").assertExists()
        compose.onNodeWithText("Light",useUnmergedTree=true).performClick()
        compose.waitUntil(5000) { compose.activity.model.state.value?.settings?.theme==ThemeMode.LIGHT }
        compose.onNodeWithText("Local suggestions").performScrollTo()
        assertFalse(compose.activity.model.state.value!!.settings.suggestions)
        compose.onNodeWithContentDescription("Local suggestions").performClick()
        compose.onNodeWithText("Enable local suggestions?").assertExists()
        assertFalse(compose.activity.model.state.value!!.settings.suggestions)
        compose.onNodeWithText("Not now").performClick()
        compose.onNodeWithContentDescription("Local suggestions").performClick()
        compose.onNodeWithText("Enable",useUnmergedTree=true).performClick()
        compose.waitUntil(5000) { compose.activity.model.state.value!!.settings.suggestions }
        compose.onNodeWithContentDescription("Local suggestions").performClick()
        compose.waitUntil(5000) { !compose.activity.model.state.value!!.settings.suggestions && compose.activity.model.state.value!!.history.isEmpty() }
    }
    @Test fun usagePermissionDenialReturnsEmptyRanking() = runBlocking {
        val command=instrumentation.uiAutomation.executeShellCommand("appops set dev.nova.launcher GET_USAGE_STATS deny");command.close()
        try { assertTrue(UsageRanking(context).read(Settings(suggestions=true,usageConsent=true)).isEmpty()) }
        finally { instrumentation.uiAutomation.executeShellCommand("appops set dev.nova.launcher GET_USAGE_STATS default").close() }
    }
    @Test fun widgetBindingCancellationReleasesId() {
        val controller=compose.activity.widgets
        val provider=runBlocking { controller.providers().first { it.provider.className==FixtureWidget::class.java.name } }
        val before=controller.host.appWidgetIds.toSet()
        compose.runOnIdle { controller.begin(provider,"home") }
        try {
            // Android 10's accessibility service dereferences an unbound widget provider.
            // Observe the foreground system Activity through test-shell state instead of
            // querying accessibility nodes while a host ID is waiting for user consent.
            var shown=false
            val deadline=SystemClock.uptimeMillis()+5000
            while(!shown && SystemClock.uptimeMillis()<deadline) {
                val dump=android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("dumpsys activity activities")).bufferedReader().use { it.readText() }
                shown=dump.lineSequence().any { (it.contains("mResumedActivity") || it.contains("topResumedActivity")) && it.contains("com.android.settings") }
                if(!shown) SystemClock.sleep(100)
            }
            assertTrue("Android widget consent Activity did not open",shown)
        } finally {
            // Always close consent, including failed assertions, so later tests have no unbound IDs.
            device.pressBack()
            compose.waitUntil(5000) { controller.pendingId()==android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID }
        }
        assertEquals(before,controller.host.appWidgetIds.toSet())
    }
    @Test fun controlCentreAndVolumeUseRealSystemState() {
        val audio=context.getSystemService(android.media.AudioManager::class.java)
        val previous=audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        try {
            compose.onNodeWithContentDescription("Open control centre").performClick()
            compose.onNodeWithTag("control-centre").assertExists()
            compose.onNodeWithText("Internet").assertExists()
            compose.onNodeWithTag("media-volume").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(0f) }
            if(!audio.isVolumeFixed) compose.runOnIdle { compose.activity.controls.setVolume(0);assertEquals(0,audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)) }
            compose.onNodeWithText("Allow brightness control").performScrollTo().assertExists()
        } finally { audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC,previous,0) }
    }
    @Test fun swipeDownOpensControlCentre() {
        compose.onNodeWithTag("gesture-clock").performTouchInput { swipeDown(durationMillis=350) }
        compose.onNodeWithTag("control-centre").assertExists()
    }
    @Test fun captureThemeScreens() {
        val apps=compose.activity.model.apps.value.take(8)
        val saved=LauncherState(pages=listOf(Page(tiles=apps.drop(4).mapIndexed { i,a->Tile("home$i",a.component) })),dock=apps.take(4).mapIndexed { i,a->Tile("dock$i",a.component) })
        for(theme in listOf(ThemeMode.AMOLED,ThemeMode.LIGHT,ThemeMode.DARK)) {
            val themed=saved.copy(settings=saved.settings.copy(theme=theme))
            runBlocking { StateStore(context).replace(themed) }
            compose.waitUntil(5000) { compose.activity.model.state.value==themed }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(theme==ThemeMode.LIGHT,androidx.core.view.WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView).isAppearanceLightStatusBars) }
            SystemClock.sleep(300) // SystemUI renders status-bar appearance outside Compose's idling clock.
            val output=File(context.getExternalFilesDir(null),"qa-${theme.name.lowercase()}.png")
            instrumentation.uiAutomation.takeScreenshot().also { bitmap->output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle() }
        }
        compose.onNodeWithContentDescription("Open control centre").performClick()
        compose.waitForIdle()
        File(context.getExternalFilesDir(null),"qa-control-centre.png").outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it) }
        compose.onNodeWithContentDescription("Back home").performClick()
        compose.onNodeWithTag("open-drawer").performClick()
        compose.waitForIdle()
        File(context.getExternalFilesDir(null),"qa-drawer.png").outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
