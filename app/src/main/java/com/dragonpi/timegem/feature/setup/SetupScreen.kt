package com.dragonpi.timegem.feature.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dragonpi.timegem.R
import com.dragonpi.timegem.data.preferences.SetupChoices

@Composable
fun SetupScreen(
    onFinish: (SetupChoices) -> Unit,
) {
    var calendarEnabled by rememberSaveable { mutableStateOf(true) }
    var routinesEnabled by rememberSaveable { mutableStateOf(true) }
    var wellbeingEnabled by rememberSaveable { mutableStateOf(true) }
    var reflectionsEnabled by rememberSaveable { mutableStateOf(true) }
    var dynamicColorEnabled by rememberSaveable { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_time_gem_logo),
                contentDescription = "Time Gem logo",
                modifier = Modifier.size(112.dp),
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Make Time Gem yours",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Notes are always at the center. Pick the extra tools you want around them. You can change everything later.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            FeatureChoice(
                icon = Icons.Rounded.CalendarMonth,
                title = "Calendar & events",
                description = "Plan days, birthdays, holidays and reminders.",
                checked = calendarEnabled,
                onCheckedChange = { calendarEnabled = it },
            )
            FeatureChoice(
                icon = Icons.Rounded.Repeat,
                title = "Routines & goals",
                description = "Build repeatable student and daily-life workflows.",
                checked = routinesEnabled,
                onCheckedChange = { routinesEnabled = it },
            )
            FeatureChoice(
                icon = Icons.Rounded.Bedtime,
                title = "Sleep & focus",
                description = "Use intentional nudges to stop doom-scrolling and wind down.",
                checked = wellbeingEnabled,
                onCheckedChange = { wellbeingEnabled = it },
            )
            FeatureChoice(
                icon = Icons.Rounded.CheckCircle,
                title = "Daily & weekly reflections",
                description = "Track mood, progress and what actually worked.",
                checked = reflectionsEnabled,
                onCheckedChange = { reflectionsEnabled = it },
            )
            FeatureChoice(
                icon = Icons.Rounded.ColorLens,
                title = "Use Material You colors",
                description = "Off keeps Time Gem's yellow identity. On follows your wallpaper colors.",
                checked = dynamicColorEnabled,
                onCheckedChange = { dynamicColorEnabled = it },
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(
                    text = "Private by default: these choices are stored locally on your device. Time Gem does not need an account to become useful.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(18.dp),
                )
            }

            Button(
                onClick = {
                    onFinish(
                        SetupChoices(
                            calendarEnabled = calendarEnabled,
                            routinesEnabled = routinesEnabled,
                            wellbeingEnabled = wellbeingEnabled,
                            reflectionsEnabled = reflectionsEnabled,
                            dynamicColorEnabled = dynamicColorEnabled,
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Text("Start using Time Gem")
            }

            Text(
                text = "You can change these choices later in Settings.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun FeatureChoice(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}
