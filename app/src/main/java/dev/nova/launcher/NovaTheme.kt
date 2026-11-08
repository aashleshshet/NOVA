package dev.nova.launcher

import android.app.Activity
import android.content.Context
import android.os.PowerManager
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dev.nova.core.FontChoice
import dev.nova.core.Settings
import dev.nova.core.ThemeMode

fun backgroundColor(s: Settings): Color = when(s.theme) { ThemeMode.AMOLED -> Color.Black; ThemeMode.DARK -> Color(0xFF101418); ThemeMode.LIGHT -> Color(0xFFF5F6F1); ThemeMode.CUSTOM -> Color(s.background) }
fun foregroundColor(background: Color): Color = if(background.luminance() > 0.179f) Color(0xFF080A0C) else Color(0xFFF6F8F1)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable fun NovaTheme(settings: Settings, content: @Composable ()->Unit) {
    val bg=backgroundColor(settings)
    val fg=foregroundColor(bg)
    val light=bg.luminance()>0.179f
    val accent=Color(settings.accent)
    // A custom accent is used for filled actions; text links use a contrast-safe foreground.
    val scheme=if(light) lightColorScheme(primary=if(settings.theme==ThemeMode.LIGHT) Color(0xFF365122) else fg,onPrimary=bg,primaryContainer=accent,onPrimaryContainer=foregroundColor(accent),background=bg,onBackground=fg,surface=bg,onSurface=fg,surfaceVariant=Color(0xFFE5E8DF),onSurfaceVariant=Color(0xFF41463C),outline=Color(0xFF747A6D))
        else darkColorScheme(primary=if(settings.theme==ThemeMode.CUSTOM) fg else accent,onPrimary=Color.Black,primaryContainer=accent,onPrimaryContainer=foregroundColor(accent),background=bg,onBackground=fg,surface=bg,onSurface=fg,surfaceVariant=Color(0xFF20262B),onSurfaceVariant=Color(0xFFC4CBC2),outline=Color(0xFF899186))
    val family=when(settings.font) { FontChoice.OUTFIT -> FontFamily(Font(R.font.outfit,variationSettings=FontVariation.Settings(FontVariation.weight(400)))); FontChoice.SYSTEM -> FontFamily.SansSerif; FontChoice.SERIF -> FontFamily.Serif }
    val base=Typography()
    val typography=remember(family) { Typography(
        displayLarge=base.displayLarge.copy(fontFamily=family,fontWeight=FontWeight.Light),displayMedium=base.displayMedium.copy(fontFamily=family,fontWeight=FontWeight.Light),displaySmall=base.displaySmall.copy(fontFamily=family),
        headlineLarge=base.headlineLarge.copy(fontFamily=family),headlineMedium=base.headlineMedium.copy(fontFamily=family),headlineSmall=base.headlineSmall.copy(fontFamily=family),
        titleLarge=base.titleLarge.copy(fontFamily=family),titleMedium=base.titleMedium.copy(fontFamily=family),titleSmall=base.titleSmall.copy(fontFamily=family),
        bodyLarge=base.bodyLarge.copy(fontFamily=family),bodyMedium=base.bodyMedium.copy(fontFamily=family),bodySmall=base.bodySmall.copy(fontFamily=family),
        labelLarge=base.labelLarge.copy(fontFamily=family),labelMedium=base.labelMedium.copy(fontFamily=family),labelSmall=base.labelSmall.copy(fontFamily=family)
    ) }
    val context=LocalContext.current
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window,window.decorView).apply { isAppearanceLightStatusBars=light; isAppearanceLightNavigationBars=light }
            if(settings.systemWallpaper) window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER) else window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        }
    }
    MaterialTheme(colorScheme=scheme,typography=typography,content=content)
}

@Composable fun GlassPanel(settings: Settings, modifier: Modifier = Modifier, content: @Composable ()->Unit) {
    val context=LocalContext.current
    val powerSave=context.getSystemService(PowerManager::class.java).isPowerSaveMode
    val glass=settings.glass && !powerSave
    val surface=MaterialTheme.colorScheme.surfaceVariant
    val shape=RoundedCornerShape(28.dp)
    Box(modifier.clip(shape).background(if(glass) Brush.verticalGradient(listOf(surface.copy(alpha=.96f),surface.copy(alpha=.84f))) else Brush.linearGradient(listOf(surface,surface)))
        .border(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=if(glass) .35f else .12f),shape)) { content() }
}
