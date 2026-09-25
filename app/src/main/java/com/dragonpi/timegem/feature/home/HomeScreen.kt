package com.dragonpi.timegem.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.dragonpi.timegem.data.preferences.NoteSort
import com.dragonpi.timegem.data.notes.NoteColor
import com.dragonpi.timegem.data.notes.AttachmentType
import com.dragonpi.timegem.feature.notes.NoteImage
import com.dragonpi.timegem.feature.notes.noteBackground
import com.dragonpi.timegem.ui.theme.TimeGemWordmark
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    preferences: TimeGemPreferences,
    notesState: NotesState,
    onRetry: () -> Unit,
    onCreateNote: (String?) -> Unit,
    onOpenNote: (Note, Float, Float) -> Unit,
    onSettingsClick: () -> Unit,
    onHomeOptionsChange: (HomeOptions) -> Unit,
    onRoutinesClick: () -> Unit,
    onWellbeingClick: () -> Unit,
) {
    val hostView = LocalView.current
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
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("All") }
    var sortOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    var addMenuOpen by remember { mutableStateOf(false) }
    var creationInfo by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(selected) { addMenuOpen = false }
    val gridState = rememberLazyStaggeredGridState()
    val notes = (notesState as? NotesState.Ready)?.notes.orEmpty()
    val filteredNotes = com.dragonpi.timegem.data.notes.orderNotes(notes.filter { note ->
        (note.title.contains(query, ignoreCase = true) || note.body.contains(query, ignoreCase = true) || note.attachments.any { it.name.contains(query, ignoreCase = true) }) &&
            when (filter) {
                "Pinned" -> note.pinned
                "Images" -> note.attachments.any { it.type == AttachmentType.IMAGE }
                "Audio" -> note.attachments.any { it.type == AttachmentType.AUDIO }
                "Colored" -> note.color != NoteColor.DEFAULT
                else -> true
            }
    }, options.sort)
    var presentedIds by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val noteIds = filteredNotes.map { it.id }
    val notesById = filteredNotes.associateBy { it.id }
    val presentedNotes = presentedIds?.mapNotNull { notesById[it] } ?: filteredNotes
    LaunchedEffect(noteIds) {
        withFrameNanos { }
        withFrameNanos { }
        presentedIds = noteIds
    }
    BackHandler(enabled = selected != Workspace.Notes || searchActive || query.isNotEmpty()) {
        if (searchActive || query.isNotEmpty()) { query = ""; searchActive = false; filter = "All"; focusManager.clearFocus() }
        else selectedName = Workspace.Notes.name
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        val searchBar: @Composable () -> Unit = {
                HomeTopBar(
                    bottom = options.bottomSearch,
                    query = query,
                    onQueryChange = {
                        query = it
                        selectedName = Workspace.Notes.name
                    },
                    themedLogo = preferences.interactions.themedLogo && preferences.appearance.source != ColorSource.TIME_GEM,
                    searchActive = searchActive,
                    onSearchFocus = { if (it) searchActive = true },
                    onCloseSearch = { query = ""; searchActive = false; filter = "All"; focusManager.clearFocus() },
                    onSortClick = { focusManager.clearFocus(); sortOpen = true; haptic() },
                    gridLayout = options.gridLayout,
                    onToggleLayout = { onHomeOptionsChange(options.copy(gridLayout = !options.gridLayout)) },
                    onSettingsClick = onSettingsClick,
                )
            }
        Scaffold(
            topBar = { if (!options.bottomSearch) searchBar() },
            bottomBar = {
                Column(Modifier.imePadding().then(if (useRail || workspaces.size == 1) Modifier.navigationBarsPadding() else Modifier)) {
                if (options.bottomSearch) searchBar()
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
            }
            },
            floatingActionButton = {
                if (selected == Workspace.Notes) {
                    Box {
                        val rotation by animateFloatAsState(if (addMenuOpen) 45f else 0f, spring(dampingRatio = 0.6f, stiffness = 800f), label = "Create icon")
                        FloatingActionButton(onClick = { haptic(); addMenuOpen = !addMenuOpen }) {
                            Icon(Icons.Rounded.Add, contentDescription = if (addMenuOpen) "Close create menu" else "Create", modifier = Modifier.rotate(rotation))
                        }
                        QuickCreateMenu(expanded = addMenuOpen, onDismiss = { addMenuOpen = false }) {
                            listOf(
                                "Text note" to Icons.Rounded.TextFields,
                                "Image" to Icons.Rounded.Image,
                                "Audio" to Icons.Rounded.AudioFile,
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
                                        when (label) {
                                            "Text note" -> onCreateNote(null)
                                            "Image" -> onCreateNote("IMAGE")
                                            "Audio" -> onCreateNote("AUDIO")
                                            else -> creationInfo = label
                                        }
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
                        if (searchActive) FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("All", "Pinned", "Images", "Audio", "Colored").forEach { kind ->
                                FilterChip(selected = filter == kind, onClick = { filter = kind; haptic() }, label = { Text(kind) })
                            }
                        }
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
                                title = if (query.isBlank() && filter == "All") "Room for your first idea" else "No matching notes",
                                message = if (query.isBlank() && filter == "All") "Tap + to create a note. Save it here and come back anytime." else "Try another word or change your filters.",
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
                                items(presentedNotes, key = { it.id }) { note ->
                                    var origin by remember { mutableStateOf(TransformOrigin.Center) }
                                    val entrance = remember(note.id, note.updatedAt) { Animatable(if (System.currentTimeMillis() - note.updatedAt < 2500) 0.86f else 1f) }
                                    LaunchedEffect(note.id, note.updatedAt) { entrance.animateTo(1f, spring(0.78f, 500f)) }
                                    OutlinedCard(
                                        modifier = Modifier.animateItem(fadeInSpec = tween(260), placementSpec = spring(0.8f, 380f), fadeOutSpec = tween(200))
                                            .graphicsLayer { scaleX = entrance.value; scaleY = entrance.value; alpha = ((entrance.value - 0.86f) / 0.14f).coerceIn(0f, 1f) }
                                            .onGloballyPositioned { coordinates ->
                                                val center = coordinates.boundsInWindow().center
                                                origin = TransformOrigin((center.x / hostView.width.coerceAtLeast(1)).coerceIn(0f, 1f), (center.y / hostView.height.coerceAtLeast(1)).coerceIn(0f, 1f))
                                            },
                                        onClick = { onOpenNote(note, origin.pivotFractionX, origin.pivotFractionY) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.outlinedCardColors(
                                            containerColor = noteBackground(note.color),
                                        ),
                                    ) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            if (note.pinned) Icon(Icons.Rounded.PushPin, "Pinned", Modifier.size(18.dp))
                                            note.attachments.firstOrNull { it.type == AttachmentType.IMAGE }?.let { NoteImage(note.id, it, Modifier.fillMaxWidth().height(160.dp)) }
                                            Text(note.displayTitle, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                            val audioCount = note.attachments.count { it.type == AttachmentType.AUDIO }
                                            if (audioCount > 0) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.AudioFile, null, Modifier.size(20.dp)); Text(" $audioCount audio", style = MaterialTheme.typography.labelMedium) }
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
        AnimatedVisibility(addMenuOpen, enter = fadeIn(tween(180)), exit = fadeOut(tween(220))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)).clickable { addMenuOpen = false })
        }
    }
    if (sortOpen) ModalBottomSheet(onDismissRequest = { sortOpen = false }) {
        Text("Sort by", Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
        NoteSort.entries.forEach { order ->
            ListItem(modifier = Modifier.selectable(selected = options.sort == order, role = Role.RadioButton, onClick = {
                onHomeOptionsChange(options.copy(sort = order)); sortOpen = false; haptic()
            }), headlineContent = { Text(order.label) }, leadingContent = { RadioButton(selected = options.sort == order, onClick = null) })
        }
        Text("Pinned notes always appear first.", Modifier.padding(24.dp), style = MaterialTheme.typography.bodyMedium)
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
    bottom: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    themedLogo: Boolean,
    searchActive: Boolean,
    onSearchFocus: (Boolean) -> Unit,
    onCloseSearch: () -> Unit,
    onSortClick: () -> Unit,
    gridLayout: Boolean,
    onToggleLayout: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().then(if (bottom) Modifier else Modifier.windowInsetsPadding(WindowInsets.statusBars))
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
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (themedLogo) Icon(painterResource(R.drawable.ic_search_mascot), contentDescription = null,
                    modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                else Image(painterResource(R.drawable.ic_time_gem_logo), contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).onFocusChanged { onSearchFocus(it.isFocused) }.semantics { contentDescription = "Search Time Gem" },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { field ->
                        Box {
                            if (query.isEmpty()) SearchHint(searchActive)
                            field()
                        }
                    },
                )
                AnimatedContent(query.isNotEmpty() || searchActive, transitionSpec = {
                    (fadeIn(tween(220)) togetherWith fadeOut(tween(160))).using(SizeTransform { _, _ -> spring(0.8f, 380f) })
                }, label = "Search controls") { active ->
                Row {
                if (active) {
                    IconButton(onClick = { if (query.isNotEmpty()) onQueryChange("") else onCloseSearch() }) {
                        Icon(Icons.Rounded.Close, contentDescription = if (query.isNotEmpty()) "Clear search" else "Close search")
                    }
                } else {
                    IconButton(onClick = onToggleLayout) {
                        Icon(
                            if (gridLayout) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                            contentDescription = if (gridLayout) "Switch to list" else "Switch to grid",
                        )
                    }
                    IconButton(onClick = onSortClick) { Icon(Icons.Rounded.SwapVert, contentDescription = "Sort notes") }
                }
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
                enter = fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = 380f), initialScale = 0.92f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)),
                exit = fadeOut(tween(180)) + scaleOut(tween(220), targetScale = 0.9f, transformOrigin = TransformOrigin(1f, 1f))) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 3.dp) {
                    Column(Modifier.width(208.dp).padding(vertical = 8.dp), content = content)
                }
            }
        }
    }
}

@Composable
private fun SearchHint(searchActive: Boolean) {
    var greeted by rememberSaveable { mutableStateOf(false) }
    val greeting = remember {
        val choices = when (java.time.LocalTime.now().hour) {
            in 5..11 -> listOf("Good morning", "A fresh start", "Morning, hello")
            in 12..16 -> listOf("Good afternoon", "Hello there", "A little progress")
            in 17..21 -> listOf("Good evening", "Time to unwind", "Evening, hello")
            else -> listOf("Hello, night owl", "A quiet moment", "Take it easy")
        }
        choices.random()
    }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(2200); greeted = true }
    BoxWithConstraints {
        val measurer = androidx.compose.ui.text.rememberTextMeasurer()
        val pixels = with(LocalDensity.current) { maxWidth.toPx() }
        val base = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        val candidates = listOf(78f, 65f, 50f).map { width ->
            buildAnnotatedString {
                append("Search ")
                withStyle(com.dragonpi.timegem.ui.theme.timeGemWordmark(width).toSpanStyle()) { append("Time Gem") }
            }
        }
        val label = candidates.firstOrNull { measurer.measure(it, base, maxLines = 1).size.width <= pixels }
            ?: buildAnnotatedString { withStyle(com.dragonpi.timegem.ui.theme.timeGemWordmark(50f).toSpanStyle()) { append("Time Gem") } }
        LaunchedEffect(searchActive) { if (searchActive) greeted = true }
        val showGreeting = !greeted && !searchActive && measurer.measure(greeting, base, maxLines = 1).size.width <= pixels
        AnimatedContent(showGreeting, transitionSpec = {
            (fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 3 }) togetherWith
                (fadeOut(tween(220)) + slideOutVertically(tween(280)) { -it / 3 })
        }, label = "Search greeting") { showing ->
            if (showing) Text(greeting, style = base, maxLines = 1)
            else Text(label, style = base, maxLines = 1, softWrap = false)
        }
    }
}
