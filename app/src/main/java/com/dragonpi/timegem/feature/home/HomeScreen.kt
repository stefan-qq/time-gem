package com.dragonpi.timegem.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.dragonpi.timegem.data.preferences.ColorSource
import com.dragonpi.timegem.ui.rememberGentleHaptic
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dragonpi.timegem.data.notes.Note
import com.dragonpi.timegem.feature.notes.NotesState
import com.dragonpi.timegem.R
import com.dragonpi.timegem.data.preferences.HomeOptions
import com.dragonpi.timegem.data.preferences.TimeGemPreferences

private enum class Workspace(val title: String, val icon: ImageVector) {
    Notes("Notes", Icons.AutoMirrored.Rounded.Notes),
    Today("Today", Icons.Rounded.Today),
    Calendar("Calendar", Icons.Rounded.CalendarMonth),
    Week("Week", Icons.Rounded.DateRange),
}

@Composable
fun HomeScreen(
    preferences: TimeGemPreferences,
    notesState: NotesState,
    onRetry: () -> Unit,
    onCreateNote: () -> Unit,
    onOpenNote: (Note) -> Unit,
    onSettingsClick: () -> Unit,
    onHomeOptionsChange: (HomeOptions) -> Unit,
    onRoutinesClick: () -> Unit,
    onWellbeingClick: () -> Unit,
) {
    val haptic = rememberGentleHaptic()
    val options = preferences.homeOptions
    val workspaces = Workspace.entries.filter {
        when (it) {
            Workspace.Notes -> true
            Workspace.Today -> options.showToday
            Workspace.Calendar -> options.showCalendar
            Workspace.Week -> options.showWeek
        }
    }
    var selectedName by rememberSaveable { mutableStateOf(Workspace.Notes.name) }
    val selected = workspaces.firstOrNull { it.name == selectedName } ?: Workspace.Notes
    LaunchedEffect(selected) { selectedName = selected.name }
    var query by rememberSaveable { mutableStateOf("") }
    var addMenuOpen by remember { mutableStateOf(false) }
    var creationInfo by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(selected) { addMenuOpen = false }
    val gridState = rememberLazyStaggeredGridState()
    val notes = (notesState as? NotesState.Ready)?.notes.orEmpty()
    val filteredNotes = notes.filter {
        it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
    }
    BackHandler(enabled = selected != Workspace.Notes || query.isNotEmpty()) {
        if (query.isNotEmpty()) query = "" else selectedName = Workspace.Notes.name
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        Scaffold(
            topBar = {
                HomeTopBar(
                    query = query,
                    onQueryChange = {
                        query = it
                        selectedName = Workspace.Notes.name
                    },
                    themedLogo = preferences.interactions.themedLogo && preferences.appearance.source != ColorSource.TIME_GEM,
                    gridLayout = options.gridLayout,
                    onToggleLayout = { onHomeOptionsChange(options.copy(gridLayout = !options.gridLayout)) },
                    onSettingsClick = onSettingsClick,
                )
            },
            bottomBar = {
                if (!useRail && workspaces.size > 1) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        workspaces.forEach { workspace ->
                            NavigationBarItem(
                                selected = selected == workspace,
                                onClick = { if (selected != workspace) haptic(); selectedName = workspace.name },
                                icon = { Icon(workspace.icon, contentDescription = null) },
                                label = { Text(workspace.title, maxLines = 1) },
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (selected == Workspace.Notes) {
                    Box {
                        val rotation by animateFloatAsState(if (addMenuOpen) 45f else 0f, tween(120), label = "Create icon")
                        FloatingActionButton(onClick = { haptic(); addMenuOpen = !addMenuOpen }) {
                            Icon(Icons.Rounded.Add, contentDescription = if (addMenuOpen) "Close create menu" else "Create", modifier = Modifier.rotate(rotation))
                        }
                        QuickCreateMenu(expanded = addMenuOpen, onDismiss = { addMenuOpen = false }) {
                            listOf(
                                "Text note" to Icons.Rounded.TextFields,
                                "Checklist" to Icons.Rounded.Checklist,
                                "Drawing" to Icons.Rounded.Draw,
                                "Reminder" to Icons.Rounded.Notifications,
                            ).forEach { (label, icon) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    leadingIcon = { Icon(icon, contentDescription = null) },
                                    onClick = {
                                        haptic()
                                        addMenuOpen = false
                                        if (label == "Text note") onCreateNote() else creationInfo = label
                                    },
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                if (useRail && workspaces.size > 1) {
                    NavigationRail(windowInsets = WindowInsets(0, 0, 0, 0)) {
                        workspaces.forEach { workspace ->
                            NavigationRailItem(
                                selected = selected == workspace,
                                onClick = { if (selected != workspace) haptic(); selectedName = workspace.name },
                                icon = { Icon(workspace.icon, contentDescription = null) },
                                label = { Text(workspace.title) },
                            )
                        }
                    }
                }
                when (selected) {
                    Workspace.Notes -> Column(Modifier.weight(1f)) {
                        Text(
                            "Notes",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                        if (notesState is NotesState.Loading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        } else if (notesState is NotesState.Failed) {
                            WorkspaceMessage("Could not load notes", "Your notes have not been changed. Try again.", Icons.Rounded.ErrorOutline) {
                                TextButton(onClick = onRetry) { Text("Retry") }
                            }
                        } else if (filteredNotes.isEmpty()) {
                            WorkspaceMessage(
                                title = if (query.isBlank()) "Room for your first idea" else "No matching notes",
                                message = if (query.isBlank()) "Tap + to create a note. Save it here and come back anytime." else "Try another word or clear your search.",
                                icon = Icons.Rounded.Search,
                            )
                        } else {
                            LazyVerticalStaggeredGrid(
                                columns = if (options.gridLayout) StaggeredGridCells.Adaptive(156.dp)
                                    else StaggeredGridCells.Fixed(1),
                                state = gridState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalItemSpacing = 10.dp,
                            ) {
                                items(filteredNotes, key = { it.id }) { note ->
                                    OutlinedCard(
                                        onClick = { onOpenNote(note) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.outlinedCardColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                        ),
                                    ) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(note.displayTitle, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                            if (note.body.isNotBlank()) Text(note.body, style = MaterialTheme.typography.bodyMedium, maxLines = 12, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Workspace.Today -> WorkspaceMessage(
                        title = "Today",
                        message = "A place for your reminders and daily routines. These features are coming next.",
                        icon = Workspace.Today.icon,
                    ) {
                        if (preferences.routinesEnabled) TextButton(onClick = onRoutinesClick) { Text("Routines") }
                        if (preferences.wellbeingEnabled) TextButton(onClick = onWellbeingClick) { Text("Sleep & focus") }
                    }
                    Workspace.Calendar -> WorkspaceMessage(
                        title = "Calendar",
                        message = "Your events, important dates and linked notes will live here. Calendar is not available yet.",
                        icon = Workspace.Calendar.icon,
                    )
                    Workspace.Week -> WorkspaceMessage(
                        title = "Your week",
                        message = "A view of your plans, routines and reflections for the week. Weekly planning is not available yet.",
                        icon = Workspace.Week.icon,
                    )
                }
            }
        }
    }
    creationInfo?.let { type ->
        AlertDialog(
            onDismissRequest = { creationInfo = null },
            title = { Text(type) },
            text = { Text("This type is still in development. Text notes are ready to use.") },
            confirmButton = { TextButton(onClick = { creationInfo = null }) { Text("Close") } },
        )
    }
}

@Composable
private fun HomeTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    themedLogo: Boolean,
    gridLayout: Boolean,
    onToggleLayout: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                Modifier.heightIn(min = 56.dp).padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (themedLogo) Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(painterResource(R.drawable.ic_launcher_monochrome), contentDescription = null,
                        modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                } else Image(painterResource(R.drawable.ic_time_gem_logo), contentDescription = null, modifier = Modifier.size(32.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Search Time Gem" },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { field ->
                        Box {
                            if (query.isEmpty()) Text(
                                "Search Time Gem",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            field()
                        }
                    },
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                } else {
                    IconButton(onClick = onToggleLayout) {
                        Icon(
                            if (gridLayout) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                            contentDescription = if (gridLayout) "Switch to list" else "Switch to grid",
                        )
                    }
                }
            }
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun WorkspaceMessage(
    title: String,
    message: String,
    icon: ImageVector,
    actions: @Composable () -> Unit = {},
) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 400.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            actions()
        }
    }
}

@Composable
private fun QuickCreateMenu(expanded: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = expanded
    val offset = with(LocalDensity.current) { 64.dp.roundToPx() }
    if (visibility.currentState || visibility.targetState) {
        Popup(alignment = Alignment.BottomEnd, offset = IntOffset(0, -offset),
            onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
            AnimatedVisibility(visibility,
                enter = fadeIn(tween(90)) + slideInVertically(tween(120)) { it / 12 },
                exit = fadeOut(tween(70))) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 3.dp) {
                    Column(Modifier.width(208.dp).padding(vertical = 8.dp), content = content)
                }
            }
        }
    }
}
