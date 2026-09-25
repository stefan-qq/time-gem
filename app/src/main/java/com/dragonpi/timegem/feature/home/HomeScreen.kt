package com.dragonpi.timegem.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import com.dragonpi.timegem.data.preferences.NoteSort
import com.dragonpi.timegem.data.notes.NoteColor
import com.dragonpi.timegem.data.notes.AttachmentType
import com.dragonpi.timegem.feature.notes.NoteImage
import com.dragonpi.timegem.feature.notes.noteBackground
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    removedNote: Note? = null,
    onRemovalShown: () -> Unit = {},
) {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val systemNavigation = WindowInsets.navigationBars
    fun keyboardOffset() = (ime.getBottom(density) - systemNavigation.getBottom(density)).coerceAtLeast(0)
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
    var removingId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(removedNote?.id) {
        if (removedNote != null) {
            kotlinx.coroutines.delay(350)
            removingId = removedNote.id
            kotlinx.coroutines.delay(240)
            onRemovalShown()
            removingId = null
        }
    }
    val storedNotes = (notesState as? NotesState.Ready)?.notes.orEmpty()
    val notes = remember(storedNotes, removedNote) {
        if (removedNote != null && storedNotes.none { it.id == removedNote.id }) storedNotes + removedNote else storedNotes
    }
    val filteredNotes = remember(notes, query, filter, options.sort) { com.dragonpi.timegem.data.notes.orderNotes(notes.filter { note ->
        (note.title.contains(query, ignoreCase = true) || note.body.contains(query, ignoreCase = true) || note.attachments.any { it.name.contains(query, ignoreCase = true) }) &&
            when (filter) {
                "Pinned" -> note.pinned
                "Images" -> note.attachments.any { it.type == AttachmentType.IMAGE }
                "Audio" -> note.attachments.any { it.type == AttachmentType.AUDIO }
                "Colored" -> note.color != NoteColor.DEFAULT
                else -> true
            }
    }, options.sort) }
    var homeCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    var fabPosition by remember { mutableStateOf(IntOffset.Zero) }
    var presentedIds by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val noteIds = filteredNotes.map { it.id }
    val notesById = filteredNotes.associateBy { it.id }
    val presentedNotes = presentedIds?.mapNotNull { notesById[it] } ?: filteredNotes
    LaunchedEffect(noteIds) {
        withFrameNanos { }
        withFrameNanos { }
        presentedIds = noteIds
    }
    val imeVisible = WindowInsets.isImeVisible
    var searchHadKeyboard by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible, searchActive) {
        if (searchActive && imeVisible) searchHadKeyboard = true
        if (searchActive && searchHadKeyboard && !imeVisible) {
            query = ""; searchActive = false; filter = "All"; focusManager.clearFocus()
        }
        if (!searchActive) searchHadKeyboard = false
    }
    BackHandler(addMenuOpen) { addMenuOpen = false }
    BackHandler(enabled = !addMenuOpen && (selected != Workspace.Notes || searchActive || query.isNotEmpty())) {
        if (searchActive || query.isNotEmpty()) { query = ""; searchActive = false; filter = "All"; focusManager.clearFocus() }
        else selectedName = Workspace.Notes.name
    }

    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { homeCoordinates = it }) {
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
                Column(Modifier.offset { IntOffset(0, -keyboardOffset()) }.then(if (useRail || workspaces.size == 1) Modifier.navigationBarsPadding() else Modifier)) {
                if (options.bottomSearch) searchBar()
                if (!useRail && workspaces.size > 1) {
                    ShortNavigationBar(modifier = Modifier.padding(horizontal = 12.dp), containerColor = MaterialTheme.colorScheme.surface, arrangement = ShortNavigationBarArrangement.Centered) {
                        workspaces.forEach { workspace ->
                            ShortNavigationBarItem(
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
                if (selected == Workspace.Notes) Spacer(Modifier.size(56.dp).onGloballyPositioned {
                    val p = homeCoordinates?.localPositionOf(it, androidx.compose.ui.geometry.Offset.Zero) ?: androidx.compose.ui.geometry.Offset.Zero
                    fabPosition = IntOffset(p.x.toInt(), p.y.toInt())
                })
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
                AnimatedContent(selected, modifier = Modifier.weight(1f).fillMaxHeight().clipToBounds(), transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(tween(320)) { it * direction } togetherWith
                        slideOutHorizontally(tween(320)) { -it * direction }).using(SizeTransform(clip = true))
                }, label = "Workspace slide") { workspace ->
                when (workspace) {
                    Workspace.Notes -> Column(Modifier.fillMaxSize()) {
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
                        Box(Modifier.fillMaxWidth().weight(1f)) {
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
                                    val removal = animateFloatAsState(if (removingId == note.id) 0f else 1f, tween(220), label = "Delete note")
                                    OutlinedCard(
                                        modifier = Modifier.animateItem(fadeInSpec = tween(260), placementSpec = spring(0.8f, 380f), fadeOutSpec = tween(200))
                                            .graphicsLayer { scaleX = entrance.value * (0.82f + 0.18f * removal.value); scaleY = scaleX; alpha = ((entrance.value - 0.86f) / 0.14f).coerceIn(0f, 1f) * removal.value }
                                            .onGloballyPositioned { coordinates ->
                                                val center = coordinates.boundsInWindow().center
                                                origin = TransformOrigin((center.x / hostView.width.coerceAtLeast(1)).coerceIn(0f, 1f), (center.y / hostView.height.coerceAtLeast(1)).coerceIn(0f, 1f))
                                            },
                                        onClick = { if (note.id != removedNote?.id) onOpenNote(note, origin.pivotFractionX, origin.pivotFractionY) },
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
                                            if (note.body.isNotBlank()) Text(note.body, style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp), maxLines = 12, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(12.dp).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, Color.Transparent))))
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
        AnimatedVisibility(addMenuOpen, enter = fadeIn(tween(180)), exit = fadeOut(tween(220))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)).clickable { addMenuOpen = false })
        }
        if (selected == Workspace.Notes) {
            CreateOverlay(position = { fabPosition.copy(y = fabPosition.y - keyboardOffset()) }) {
                QuickCreateMenu(expanded = addMenuOpen) {
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
                Spacer(Modifier.height(8.dp))
                val rotation = animateFloatAsState(if (addMenuOpen) 45f else 0f, spring(0.72f, 650f), label = "Create icon")
                FloatingActionButton(onClick = { haptic(); addMenuOpen = !addMenuOpen }) {
                    Icon(Icons.Rounded.Add, if (addMenuOpen) "Close create menu" else "Create",
                        Modifier.graphicsLayer { rotationZ = rotation.value })
                }
            }
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
    var greeted by rememberSaveable { mutableStateOf(false) }
    val greeting = remember { greetingForHour(java.time.LocalTime.now().hour).random() }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(2800); greeted = true }
    LaunchedEffect(searchActive) { if (searchActive) greeted = true }
    val greetingVisible = !greeted && !searchActive
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
                AnimatedVisibility(!greetingVisible, enter = fadeIn(tween(240)) + expandHorizontally(tween(300)), exit = fadeOut(tween(160)) + shrinkHorizontally(tween(240))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                if (themedLogo) Icon(painterResource(R.drawable.ic_search_mascot), contentDescription = null,
                    modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                else Image(painterResource(R.drawable.ic_time_gem_logo), contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                }
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).onFocusChanged { onSearchFocus(it.isFocused) }.semantics { contentDescription = "Search Time Gem" },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { field ->
                        Box {
                            if (query.isEmpty()) SearchHint(greetingVisible, greeting)
                            field()
                        }
                    },
                )
                AnimatedVisibility(!greetingVisible, enter = fadeIn(tween(240)) + expandHorizontally(tween(300)), exit = fadeOut(tween(160)) + shrinkHorizontally(tween(240))) {
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
private fun CreateOverlay(position: () -> IntOffset, content: @Composable ColumnScope.() -> Unit) {
    Layout(content = { Column(horizontalAlignment = Alignment.End, content = content) }) { measurables, constraints ->
        val child = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val anchor = position()
            child.place(anchor.x + 56.dp.roundToPx() - child.width, anchor.y + 56.dp.roundToPx() - child.height)
        }
    }
}

@Composable
private fun QuickCreateMenu(expanded: Boolean, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(expanded,
        enter = fadeIn(tween(160)) + scaleIn(spring(0.82f, 450f), initialScale = 0.85f, transformOrigin = TransformOrigin(1f, 1f)),
        exit = fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 0.85f, transformOrigin = TransformOrigin(1f, 1f))) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 3.dp) {
            Column(Modifier.width(208.dp).padding(vertical = 8.dp), content = content)
        }
    }
}

internal fun greetingForHour(hour: Int): List<String> = when (hour) {
    in 5..11 -> listOf("Good morning", "A fresh page awaits", "Hello, new day", "Start with one idea", "Make room for today", "Morning, take your time", "What's on your mind?", "A little morning clarity", "Your day, your pace", "One thing at a time")
    in 12..16 -> listOf("Good afternoon", "How's your day going?", "Room for another idea", "A moment to regroup", "Keep your ideas close", "A little progress counts", "Pick up where you left off", "Pause, then carry on", "Make space to think", "Hello again")
    in 17..21 -> listOf("Good evening", "How did today go?", "Save a thought for later", "A little time for you", "Ease into the evening", "Tomorrow can wait", "Catch those last ideas", "Take a quiet moment", "Let the day settle", "One less thing to remember")
    else -> listOf("Hello, night owl", "A quiet space to think", "Save it for tomorrow", "Rest when you're ready", "A thought before sleep", "Keep it for the morning", "The ideas can wait here", "Take it easy tonight", "A little peace and quiet", "Leave tomorrow a note")
}

@Composable
private fun SearchHint(showGreeting: Boolean, greeting: String) {
    BoxWithConstraints {
        val measurer = androidx.compose.ui.text.rememberTextMeasurer()
        val pixels = with(LocalDensity.current) { maxWidth.toPx() }
        val base = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        val brandStyle = remember(base) { com.dragonpi.timegem.ui.theme.TimeGemWordmark.copy(color = base.color, textMotion = androidx.compose.ui.text.style.TextMotion.Animated) }
        val prefixWidth = remember(base, measurer) { measurer.measure("Search ", base, maxLines = 1).size.width.toFloat() }
        val brandWidth = remember(brandStyle, measurer) { measurer.measure("Time Gem", brandStyle, maxLines = 1).size.width.toFloat() }
        val targetScale = ((pixels - prefixWidth) / brandWidth).coerceIn(0.5f, 1f)
        val brandScale = animateFloatAsState(targetScale, tween(280), label = "Wordmark width")
        AnimatedContent(showGreeting, transitionSpec = {
            (fadeIn(tween(280)) + slideInVertically(tween(300)) { it / 3 }) togetherWith
                (fadeOut(tween(180)) + slideOutVertically(tween(240)) { -it / 3 })
        }, label = "Search greeting") { showing ->
            if (showing) Text(greeting, style = base, maxLines = 1, overflow = TextOverflow.Ellipsis)
            else Row {
                Text("Search ", style = base, maxLines = 1, softWrap = false)
                Text("Time Gem", style = brandStyle, maxLines = 1, softWrap = false,
                    modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true).graphicsLayer {
                        scaleX = brandScale.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    })
            }
        }
    }
}
