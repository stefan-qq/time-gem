package com.dragonpi.timegem.ui

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

val LocalHapticsEnabled = staticCompositionLocalOf { true }

@Composable
fun rememberGentleHaptic(): () -> Unit {
    val view = LocalView.current
    val enabled = LocalHapticsEnabled.current
    return { if (enabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
}
