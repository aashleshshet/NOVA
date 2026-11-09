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

fun backgroundColor(s: Settings): Color = when(s.theme) { ThemeMode.AMOLED -> Color.Black; ThemeMode.DARK -> Color(0xFF121212); ThemeMode.LIGHT -> Color(0xFFF4F3EF); ThemeMode.CUSTOM -> Color(s.background) }
fun foregroundColor(background: Color): Color = if(background.luminance() > 0.179f) Color(0xFF080A0C) else Color(0xFFF6F8F1)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable fun NovaTheme(settings: Settings, content: @Composable ()->Unit) {
    val bg=backgroundColor(settings)
    val fg=foregroundColor(bg)
    val light=bg.luminance()>0.179f
    val accent=Color(settings.accent)
    // A custom accent is used for filled actions; text links use a contrast-safe foreground.
    val scheme=(if(light) lightColorScheme(primary=if(settings.theme==ThemeMode.LIGHT) Color(0xFF9F211C) else fg,onPrimary=bg,primaryContainer=accent,onPrimaryContainer=foregroundColor(accent),background=bg,onBackground=fg,surface=bg,onSurface=fg,surfaceVariant=Color(0xFFE5E4E0),onSurfaceVariant=Color(0xFF444444),outline=Color(0xFF747A6D))
        else darkColorScheme(primary=fg,onPrimary=Color.Black,primaryContainer=accent,onPrimaryContainer=foregroundColor(accent),background=bg,onBackground=fg,surface=bg,onSurface=fg,surfaceVariant=Color(0xFF232323),onSurfaceVariant=Color(0xFFCCCCCC),outline=Color(0xFF858585)))
        .copy(secondary=fg,onSecondary=bg,secondaryContainer=if(light) Color(0xFFD5D5D5) else Color(0xFF444444),onSecondaryContainer=fg,tertiary=accent,onTertiary=foregroundColor(accent),tertiaryContainer=if(light) Color(0xFFE7E7E7) else Color(0xFF303030),onTertiaryContainer=fg,surfaceTint=Color.Transparent,surfaceContainer=if(light) Color(0xFFECEBE7) else Color(0xFF1D1D1D),surfaceContainerHigh=if(light) Color(0xFFE5E4E0) else Color(0xFF262626),surfaceContainerHighest=if(light) Color(0xFFDAD9D5) else Color(0xFF333333),surfaceContainerLow=if(light) Color(0xFFF0EFEB) else Color(0xFF191919),surfaceContainerLowest=bg)
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
        context.findActivity()?.window?.let { window ->
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

private tailrec fun Context.findActivity(): Activity? = when(this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
