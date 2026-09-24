package com.dragonpi.timegem.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.dragonpi.timegem.ui.rememberGentleHaptic
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dragonpi.timegem.data.preferences.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: TimeGemPreferences,
    onAppearanceChange: (Appearance) -> Unit,
    onHomeOptionsChange: (HomeOptions) -> Unit,
    onFeaturesChange: (Boolean, Boolean, Boolean) -> Unit,
    onBack: () -> Unit,
    onInteractionsChange: (InteractionOptions) -> Unit,
) {
    val options = preferences.homeOptions
    val interactions = preferences.interactions
    val context = LocalContext.current
    var showFontLicense by remember { mutableStateOf(false) }
    if (showFontLicense) {
        val license = remember { context.assets.open("licenses/google_sans_flex_ofl.txt").bufferedReader().use { it.readText() } }
        AlertDialog(
            onDismissRequest = { showFontLicense = false },
            title = { Text("Google Sans Flex") },
            text = { Text(license, Modifier.verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall) },
            confirmButton = { TextButton(onClick = { showFontLicense = false }) { Text("Close") } },
        )
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        }) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Appearance", style = MaterialTheme.typography.headlineSmall)
                AppearanceControls(preferences.appearance, onAppearanceChange)
                SettingSwitch("Themed search logo", "Match the search logo to wallpaper and custom colors. Time Gem keeps its original logo.", interactions.themedLogo) {
                    onInteractionsChange(interactions.copy(themedLogo = it))
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Notes & feedback", style = MaterialTheme.typography.headlineSmall)
                SettingSwitch("Ask before saving", "Off: leaving a note saves your changes. Empty drafts are ignored; clearing a saved note keeps its previous version.", interactions.askBeforeSaving) {
                    onInteractionsChange(interactions.copy(askBeforeSaving = it))
                }
                SettingSwitch("Subtle haptics", "Gentle feedback for actions. Respects your phone's vibration settings.", interactions.haptics) {
                    onInteractionsChange(interactions.copy(haptics = it))
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Home", style = MaterialTheme.typography.headlineSmall)
                SettingSwitch("Note grid", "Turn off for a single list.", options.gridLayout) { onHomeOptionsChange(options.copy(gridLayout = it)) }
                Text("Workspaces", style = MaterialTheme.typography.titleMedium)
                Text("Notes always stays visible.", style = MaterialTheme.typography.bodyMedium)
                SettingSwitch("Today", "Reminders and daily routines.", options.showToday) { onHomeOptionsChange(options.copy(showToday = it)) }
                SettingSwitch("Calendar", "Events and important dates.", options.showCalendar) { onHomeOptionsChange(options.copy(showCalendar = it)) }
                SettingSwitch("Week", "Plans and reflections.", options.showWeek) { onHomeOptionsChange(options.copy(showWeek = it)) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Optional tools", style = MaterialTheme.typography.headlineSmall)
                Text("These tools are still in development.", style = MaterialTheme.typography.bodyMedium)
                SettingSwitch("Routines", "Daily habits and goals.", preferences.routinesEnabled) {
                    onFeaturesChange(it, preferences.wellbeingEnabled, preferences.reflectionsEnabled)
                }
                SettingSwitch("Sleep & focus", "Make room for rest.", preferences.wellbeingEnabled) {
                    onFeaturesChange(preferences.routinesEnabled, it, preferences.reflectionsEnabled)
                }
                SettingSwitch("Reflections", "Check in with yourself.", preferences.reflectionsEnabled) {
                    onFeaturesChange(preferences.routinesEnabled, preferences.wellbeingEnabled, it)
                }
                TextButton(onClick = { showFontLicense = true }) { Text("Google Sans Flex font license") }
            }
        }
    }
}

@Composable
fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val haptic = rememberGentleHaptic()
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = { haptic(); onChange(it) }).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
