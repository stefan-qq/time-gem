package com.dragonpi.timegem.feature.notes

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private fun audioTime(milliseconds: Int): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

@Composable
fun AudioProgress(position: Int, duration: Int, playing: Boolean, enabled: Boolean,
    onSeek: (Int) -> Unit, onSeekFinished: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val phase = if (playing) {
        val transition = rememberInfiniteTransition(label = "Audio wave")
        val value by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "Wave phase")
        value
    } else 0f
    Box(Modifier.fillMaxWidth().height(44.dp)) {
        Canvas(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
            val path = Path()
            for (step in 0..160) {
                val x = size.width * step / 160f
                val y = size.height / 2 + sin(x / 18.dp.toPx() * (2 * PI).toFloat() - phase) * 3.dp.toPx()
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, track, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            clipRect(right = size.width * progress) { drawPath(path, primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)) }
        }
        Slider(value = progress, onValueChange = { onSeek((it * duration).toInt()) }, onValueChangeFinished = onSeekFinished,
            enabled = enabled && duration > 0, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Audio position" },
            colors = SliderDefaults.colors(activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent,
                disabledActiveTrackColor = Color.Transparent, disabledInactiveTrackColor = Color.Transparent))
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(audioTime(position), style = MaterialTheme.typography.labelSmall)
        Text(if (duration > 0) audioTime(duration) else "--:--", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun RecordingLevels(levels: List<Float>, recording: Boolean) {
    val color = if (recording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Canvas(Modifier.fillMaxWidth().height(76.dp).padding(vertical = 12.dp)
        .semantics { contentDescription = if (recording) "Live microphone levels" else "Microphone idle" }) {
        val spacing = size.width / levels.size.coerceAtLeast(1)
        levels.forEachIndexed { index, amplitude ->
            val height = (if (recording) amplitude else 0f) * (size.height - 4.dp.toPx()) + 4.dp.toPx()
            val x = spacing * (index + 0.5f)
            drawLine(color, Offset(x, (size.height - height) / 2), Offset(x, (size.height + height) / 2),
                strokeWidth = (spacing * 0.55f).coerceAtLeast(1f), cap = StrokeCap.Round)
        }
    }
}
