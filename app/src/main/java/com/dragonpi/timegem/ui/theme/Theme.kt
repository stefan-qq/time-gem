package com.dragonpi.timegem.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GemPrimaryDark,
    onPrimary = GemOnPrimaryDark,
    primaryContainer = GemPrimaryContainerDark,
    onPrimaryContainer = GemOnPrimaryContainerDark,
    secondary = GemSecondaryDark,
    secondaryContainer = GemSecondaryContainerDark,
    background = GemBackgroundDark,
    surface = GemSurfaceDark,
)

private val LightColorScheme = lightColorScheme(
    primary = GemPrimaryLight,
    onPrimary = GemOnPrimaryLight,
    primaryContainer = GemPrimaryContainerLight,
    onPrimaryContainer = GemOnPrimaryContainerLight,
    secondary = GemSecondaryLight,
    secondaryContainer = GemSecondaryContainerLight,
    background = GemBackgroundLight,
    surface = GemSurfaceLight,
)

@Composable
fun TimeGemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
