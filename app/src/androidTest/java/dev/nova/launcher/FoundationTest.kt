package dev.nova.launcher

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nova.core.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun registeredAsHome() {
        val homes = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(context.packageName), 0)
        assertTrue(homes.any { it.activityInfo.name == MainActivity::class.java.name })
    }
    @Test fun statePersistsAcrossRepositoryInstances() = runBlocking {
        val store = StateStore(context)
        val original = store.state.first()
        try {
            store.update { it.copy(settings = it.settings.copy(theme = ThemeMode.LIGHT)) }
            assertEquals(ThemeMode.LIGHT, StateStore(context).state.first().settings.theme)
        } finally { store.replace(original) }
    }
}
