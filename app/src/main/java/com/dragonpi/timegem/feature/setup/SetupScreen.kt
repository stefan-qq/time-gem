package com.dragonpi.timegem.feature.setup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dragonpi.timegem.R
import com.dragonpi.timegem.data.preferences.*
import com.dragonpi.timegem.feature.settings.AppearanceControls
import com.dragonpi.timegem.feature.settings.SettingSwitch

@Composable
fun SetupScreen(
    appearance: Appearance,
    onAppearanceChange: (Appearance) -> Unit,
    saving: Boolean,
    onFinish: (SetupChoices) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var calendar by rememberSaveable { mutableStateOf(true) }
    var routines by rememberSaveable { mutableStateOf(true) }
    var wellbeing by rememberSaveable { mutableStateOf(true) }
    var reflections by rememberSaveable { mutableStateOf(true) }
    BackHandler(step > 0) { step = 0 }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
                    Row(
                        Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (step > 0) IconButton(onClick = { step = 0 }, enabled = !saving) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                        }
                        Button(
                            onClick = {
                                if (step == 0) step = 1
                                else onFinish(SetupChoices(calendar, routines, wellbeing, reflections))
                            },
                            enabled = !saving,
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        ) { Text(if (saving) "Saving…" else if (step == 0) "Continue" else "Start using Time Gem") }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "setup_page",
            ) { page ->
                Column(
                    Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(if (page == 0) "1 of 2 · Your space" else "2 of 2 · Appearance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(progress = { if (page == 0) 0.5f else 1f }, modifier = Modifier.fillMaxWidth())
                    if (page == 0) {
                        Image(painterResource(R.drawable.ic_time_gem_logo), contentDescription = null, modifier = Modifier.size(64.dp))
                        Text("Make Time Gem yours", style = MaterialTheme.typography.headlineMedium)
                        Text("Start with notes. Choose which other spaces you want to see.", style = MaterialTheme.typography.bodyLarge)
                        Text("The extra tools are still being built. You can change these choices in Settings.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column {
                            SettingSwitch("Calendar", "Events and important dates.", calendar) { calendar = it }
                            HorizontalDivider()
                            SettingSwitch("Routines", "Daily habits and goals.", routines) { routines = it }
                            HorizontalDivider()
                            SettingSwitch("Sleep & focus", "Make room for rest.", wellbeing) { wellbeing = it }
                            HorizontalDivider()
                            SettingSwitch("Reflections", "Look back on your day and week.", reflections) { reflections = it }
                        }
                        Text("Notes are always available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Find your colors", style = MaterialTheme.typography.headlineMedium)
                        Text("Preview your theme here. Change it anytime in Settings.", style = MaterialTheme.typography.bodyLarge)
                        AppearanceControls(appearance, onAppearanceChange)
                        HorizontalDivider()
                        Text("Your space, on your device", style = MaterialTheme.typography.titleMedium)
                        Text("No account needed. Notes and preferences are saved locally.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
