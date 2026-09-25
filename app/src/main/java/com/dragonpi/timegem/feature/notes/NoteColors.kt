package com.dragonpi.timegem.feature.notes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.dragonpi.timegem.data.notes.NoteColor

@Composable
fun noteBackground(color: NoteColor): Color {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    return when (color) {
        NoteColor.DEFAULT -> scheme.surface
        NoteColor.CORAL -> if (dark) Color(0xFF51312E) else Color(0xFFFFDAD4)
        NoteColor.PEACH -> if (dark) Color(0xFF503A29) else Color(0xFFFFDFC3)
        NoteColor.SAND -> if (dark) Color(0xFF47412E) else Color(0xFFF1E6C7)
        NoteColor.MINT -> if (dark) Color(0xFF293F36) else Color(0xFFD2EADD)
        NoteColor.SKY -> if (dark) Color(0xFF2A3D4A) else Color(0xFFD3E7F5)
        NoteColor.LAVENDER -> if (dark) Color(0xFF3D354C) else Color(0xFFE9DDF6)
        NoteColor.ROSE -> if (dark) Color(0xFF4B3340) else Color(0xFFF6DBE8)
    }
}
