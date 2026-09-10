package com.dragonpi.timegem.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dragonpi.timegem.data.preferences.TimeGemPreferences

private data class DemoNote(
    val title: String,
    val body: String,
)

@Composable
fun HomeScreen(
    preferences: TimeGemPreferences,
    onCalendarClick: () -> Unit,
    onRoutinesClick: () -> Unit,
    onWellbeingClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    var addMenuExpanded by remember { mutableStateOf(false) }

    val demoNotes = remember {
        listOf(
            DemoNote("Welcome to Time Gem", "This home is now a real app shell instead of a setup placeholder."),
            DemoNote("The plan", "Notes first. Then persistence, calendar links, routines, reminders, drawing and the weekly view."),
        )
    }

    Scaffold(
        topBar = {
            TimeGemTopBar(onSettingsClick = onSettingsClick)
        },
        floatingActionButton = {
            AddMenu(
                expanded = addMenuExpanded,
                onToggle = { addMenuExpanded = !addMenuExpanded },
                onDismiss = { addMenuExpanded = false },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Your space",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
            }

            item {
                QuickActions(
                    preferences = preferences,
                    onCalendarClick = onCalendarClick,
                    onRoutinesClick = onRoutinesClick,
                    onWellbeingClick = onWellbeingClick,
                )
            }

            items(demoNotes) { note ->
                NoteCard(note)
            }
        }
    }
}

@Composable
private fun TimeGemTopBar(
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            onClick = { /* Search screen arrives with the notes milestone. */ },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null)
                Text(
                    text = "Search Time Gem",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
            }
        }
    }
}

@Composable
private fun QuickActions(
    preferences: TimeGemPreferences,
    onCalendarClick: () -> Unit,
    onRoutinesClick: () -> Unit,
    onWellbeingClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (preferences.calendarEnabled) {
            QuickAction(
                label = "Calendar",
                icon = Icons.Rounded.CalendarMonth,
                onClick = onCalendarClick,
                modifier = Modifier.weight(1f),
            )
        }
        if (preferences.routinesEnabled) {
            QuickAction(
                label = "Routines",
                icon = Icons.Rounded.Repeat,
                onClick = onRoutinesClick,
                modifier = Modifier.weight(1f),
            )
        }
        if (preferences.wellbeingEnabled) {
            QuickAction(
                label = "Focus",
                icon = Icons.Rounded.Bedtime,
                onClick = onWellbeingClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NoteCard(note: DemoNote) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Note options")
                }
            }
            Text(
                text = note.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AddMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AddAction("Reminder", Icons.Rounded.Notifications, onDismiss)
                AddAction("Drawing", Icons.Rounded.Draw, onDismiss)
                AddAction("Text note", Icons.Rounded.TextFields, onDismiss)
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            shape = CircleShape,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add")
        }
    }
}

@Composable
private fun AddAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        text = { Text(text) },
    )
}
