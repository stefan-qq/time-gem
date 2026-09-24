package com.dragonpi.timegem.feature.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dragonpi.timegem.data.preferences.*

@Composable
fun AppearanceControls(appearance: Appearance, onChange: (Appearance) -> Unit) {
    var palettePicker by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Brightness", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = appearance.mode == mode,
                    onClick = { onChange(appearance.copy(mode = mode)) },
                    label = { Text(if (mode == ThemeMode.SYSTEM) "Follow device" else mode.label) },
                    leadingIcon = if (appearance.mode == mode) { { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) } } else null,
                )
            }
        }
        Text("Colors", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) {
            ColorSourceRow("Time Gem", "Our yellow accent, with calm neutral surfaces.", appearance.source == ColorSource.TIME_GEM) {
                onChange(appearance.copy(source = ColorSource.TIME_GEM))
            }
            ColorSourceRow(
                "Material You", "Use your device wallpaper colors.",
                appearance.source == ColorSource.WALLPAPER,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            ) { onChange(appearance.copy(source = ColorSource.WALLPAPER)) }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Text("Wallpaper colors need Android 12 or later.", style = MaterialTheme.typography.bodySmall)
            }
            ColorSourceRow("Preset palette", "Choose a Material color family.", appearance.source == ColorSource.PRESET) {
                onChange(appearance.copy(source = ColorSource.PRESET))
            }
        }
        if (appearance.source == ColorSource.PRESET) {
            OutlinedButton(onClick = { palettePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(20.dp).background(Color(appearance.palette.seed), CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(appearance.palette.label)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Palette, contentDescription = null)
            }
        }
        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A little space to think", style = MaterialTheme.typography.titleMedium)
                Text("Your notes, your pace.", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary).forEach {
                        Box(Modifier.size(20.dp).background(it, CircleShape))
                    }
                }
            }
        }
    }
    if (palettePicker) {
        AlertDialog(
            onDismissRequest = { palettePicker = false },
            title = { Text("Color palette") },
            text = {
                LazyColumn(Modifier.selectableGroup()) {
                    items(Palette.entries, key = { it.name }) { palette ->
                        Row(
                            Modifier.fillMaxWidth().selectable(
                                selected = palette == appearance.palette, role = Role.RadioButton,
                                onClick = { onChange(appearance.copy(palette = palette, source = ColorSource.PRESET)); palettePicker = false },
                            ).padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(Modifier.size(28.dp).background(Color(palette.seed), CircleShape))
                            Text(palette.label, modifier = Modifier.weight(1f))
                            RadioButton(selected = palette == appearance.palette, onClick = null)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { palettePicker = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun ColorSourceRow(title: String, subtitle: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
