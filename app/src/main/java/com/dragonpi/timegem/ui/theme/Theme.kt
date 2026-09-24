package com.dragonpi.timegem.ui.theme

import android.os.Build
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import com.dragonpi.timegem.data.preferences.Appearance
import com.dragonpi.timegem.data.preferences.ThemeMode
import com.dragonpi.timegem.data.preferences.ColorSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GemPrimaryDark,
    onPrimary = GemOnPrimaryDark,
    primaryContainer = GemPrimaryContainerDark,
    onPrimaryContainer = GemOnPrimaryContainerDark,
    secondary = GemSecondaryDark,
    secondaryContainer = GemSecondaryContainerDark,
    onSecondary = Color(0xFF353021),
    onSecondaryContainer = Color(0xFFEAE2D1),
    background = Color(0xFF141413),
    onBackground = Color(0xFFE5E2DD),
    surface = Color(0xFF141413),
    onSurface = Color(0xFFE5E2DD),
    onSurfaceVariant = Color(0xFFCAC6BD),
    surfaceDim = Color(0xFF141413),
    surfaceBright = Color(0xFF3A3936),
    surfaceContainerLowest = Color(0xFF0F0F0E),
    surfaceContainerLow = Color(0xFF1C1C1A),
    surfaceContainer = Color(0xFF222220),
    surfaceContainerHigh = Color(0xFF2C2C29),
    surfaceContainerHighest = Color(0xFF373733),
    outline = Color(0xFF939087),
    outlineVariant = Color(0xFF494740),
)

private val LightColorScheme = lightColorScheme(
    primary = GemPrimaryLight,
    onPrimary = GemOnPrimaryLight,
    primaryContainer = GemPrimaryContainerLight,
    onPrimaryContainer = GemOnPrimaryContainerLight,
    secondary = GemSecondaryLight,
    secondaryContainer = GemSecondaryContainerLight,
    onSecondary = Color(0xFFFFFFFF),
    onSecondaryContainer = Color(0xFF211D10),
    background = Color(0xFFFAF9F6),
    onBackground = Color(0xFF1C1C19),
    surface = Color(0xFFFAF9F6),
    onSurface = Color(0xFF1C1C19),
    onSurfaceVariant = Color(0xFF494740),
    surfaceDim = Color(0xFFDBDAD5),
    surfaceBright = Color(0xFFFAF9F6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F3EF),
    surfaceContainer = Color(0xFFEEEDE8),
    surfaceContainerHigh = Color(0xFFE8E7E2),
    surfaceContainerHighest = Color(0xFFE2E1DC),
    outline = Color(0xFF7B786F),
    outlineVariant = Color(0xFFCBC7BD),
)

@Composable
fun TimeGemTheme(
    appearance: Appearance = Appearance(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = when (appearance.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = context.findActivity() as? ComponentActivity
            activity?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { darkTheme },
                navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { darkTheme },
            )
        }
    }

    val colorScheme = when {
        appearance.source == ColorSource.WALLPAPER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        appearance.source == ColorSource.PRESET -> presetColorScheme(appearance.palette, darkTheme)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
