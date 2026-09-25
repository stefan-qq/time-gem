package com.dragonpi.timegem.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissibleFeedback(host: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(host, modifier.widthIn(max = 560.dp)) { data ->
        key(data) {
            val entrance = remember { Animatable(0f) }
            LaunchedEffect(Unit) { entrance.animateTo(1f, spring(0.8f, 380f)) }
            val dismiss = rememberSwipeToDismissBoxState()
            LaunchedEffect(dismiss.currentValue) {
                if (dismiss.currentValue != SwipeToDismissBoxValue.Settled) data.dismiss()
            }
            SwipeToDismissBox(state = dismiss, backgroundContent = {}, modifier = Modifier.graphicsLayer {
                translationY = (1f - entrance.value) * 48.dp.toPx()
                alpha = entrance.value.coerceIn(0f, 1f)
            }) {
                Snackbar(shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    action = {
                        IconButton(onClick = { data.dismiss() }) {
                            Icon(Icons.Rounded.Close, "Dismiss message", tint = MaterialTheme.colorScheme.inverseOnSurface)
                        }
                    }) { Text(data.visuals.message) }
            }
        }
    }
}
