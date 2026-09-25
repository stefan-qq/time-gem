package com.dragonpi.timegem.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.dragonpi.timegem.data.preferences.*
import com.dragonpi.timegem.data.notes.Note
import com.dragonpi.timegem.feature.home.HomeScreen
import com.dragonpi.timegem.feature.placeholder.PlaceholderFeatureScreen
import com.dragonpi.timegem.feature.settings.SettingsScreen
import com.dragonpi.timegem.feature.notes.*
import java.util.UUID
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

@Composable
fun TimeGemNavigation(
    preferences: TimeGemPreferences,
    onHomeOptionsChange: (HomeOptions) -> Unit,
    onAppearanceChange: (Appearance) -> Unit,
    onFeaturesChange: (Boolean, Boolean, Boolean) -> Unit,
    notesViewModel: NotesViewModel,
    onInteractionsChange: (InteractionOptions) -> Unit,
    newNoteRequest: String? = null,
    onNewNoteHandled: () -> Unit = {},
) {
    var noteOrigin by remember { mutableStateOf(androidx.compose.ui.graphics.TransformOrigin.Center) }
    var removedNote by remember { mutableStateOf<Note?>(null) }
    var noteTransition by remember { mutableStateOf(false) }
    val backStack = rememberNavBackStack(HomeDestination)
    val notesState by notesViewModel.state.collectAsStateWithLifecycle()
    val saving by notesViewModel.saving.collectAsStateWithLifecycle()
    val noteError by notesViewModel.error.collectAsStateWithLifecycle()
    fun back() { if (backStack.size > 1) backStack.removeLastOrNull() }
    fun open(destination: NavKey) {
        if (backStack.lastOrNull() != destination) {
            noteTransition = destination is NoteDestination
            backStack.add(destination)
        }
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(notesViewModel) { notesViewModel.messages.collectLatest { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(it) } }
    LaunchedEffect(newNoteRequest, saving) {
        if (newNoteRequest != null && !saving) {
            val parts = newNoteRequest.split(":")
            if (parts.first() == "NOTE" && parts.size >= 2) open(NoteDestination(parts[1]))
            else open(NoteDestination(UUID.randomUUID().toString(), true, parts.first().takeIf { it == "IMAGE" || it == "AUDIO" }))
            onNewNoteHandled()
        }
    }
    Box(Modifier.fillMaxSize()) {
    NavDisplay(
        backStack = backStack,
        onBack = { if (!saving) back() },
        transitionSpec = {
            if (noteTransition) (fadeIn(tween(220)) + scaleIn(tween(340), initialScale = 0.8f, transformOrigin = noteOrigin)) togetherWith ExitTransition.None
            else slideInHorizontally(tween(300)) { it } togetherWith slideOutHorizontally(tween(300)) { -it / 4 }
        },
        popTransitionSpec = {
            if (noteTransition) (fadeIn(tween(340)) togetherWith (fadeOut(tween(240, delayMillis = 80)) + scaleOut(tween(340), targetScale = 0.8f, transformOrigin = noteOrigin))).apply { targetContentZIndex = -1f }
            else slideInHorizontally(tween(300)) { -it / 4 } togetherWith slideOutHorizontally(tween(300)) { it }
        },
        predictivePopTransitionSpec = { _ ->
            if (noteTransition) (fadeIn(tween(340)) togetherWith (fadeOut(tween(240, delayMillis = 80)) + scaleOut(tween(340), targetScale = 0.8f, transformOrigin = noteOrigin))).apply { targetContentZIndex = -1f }
            else slideInHorizontally(tween(300)) { -it / 4 } togetherWith slideOutHorizontally(tween(300)) { it }
        },
        entryProvider = entryProvider {
            entry<HomeDestination> {
                HomeScreen(
                    preferences = preferences,
                    notesState = notesState,
                    removedNote = removedNote,
                    onRemovalShown = { removedNote = null },
                    onRetry = notesViewModel::reload,
                    onCreateNote = { type -> noteOrigin = androidx.compose.ui.graphics.TransformOrigin(0.9f, 0.9f); notesViewModel.clearError(); open(NoteDestination(UUID.randomUUID().toString(), true, type)) },
                    onOpenNote = { note, x, y -> noteOrigin = androidx.compose.ui.graphics.TransformOrigin(x, y); notesViewModel.clearError(); open(NoteDestination(note.id)) },
                    onSettingsClick = { open(SettingsDestination) },
                    onHomeOptionsChange = onHomeOptionsChange,
                    onRoutinesClick = { open(RoutinesDestination) },
                    onWellbeingClick = { open(WellbeingDestination) },
                )
            }
            entry<SettingsDestination> {
                SettingsScreen(preferences, onAppearanceChange, onHomeOptionsChange, onFeaturesChange, ::back, onInteractionsChange)
            }
            entry<NoteDestination> { destination ->
                val loaded = if (destination.isNew) Note(destination.id)
                    else (notesState as? NotesState.Ready)?.notes?.firstOrNull { it.id == destination.id }
                var snapshot by remember(destination.id) { mutableStateOf(loaded) }
                if (snapshot == null && loaded != null) snapshot = loaded
                val note = snapshot
                val closeEditor = { if (backStack.lastOrNull() == destination) back() }
                if (note != null) {
                    NoteEditorScreen(
                        note = note,
                        isNew = destination.isNew,
                        initialAttachmentType = destination.attachmentType,
                        saving = saving,
                        error = noteError,
                        askBeforeSaving = preferences.interactions.askBeforeSaving,
                        onSave = { notesViewModel.save(it, closeEditor) },
                        onDelete = { notesViewModel.delete(destination.id) { removedNote = note; closeEditor() } },
                        onBack = closeEditor,
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (notesState is NotesState.Loading) CircularProgressIndicator()
                        else Column {
                            Text(if (notesState is NotesState.Failed) "Could not load this note." else "This note is no longer available.")
                            if (notesState is NotesState.Failed) TextButton(onClick = notesViewModel::reload) { Text("Retry") }
                            TextButton(onClick = ::back) { Text("Back to notes") }
                        }
                    }
                }
            }
            entry<CalendarDestination> {
                PlaceholderFeatureScreen("Calendar", "Events and linked notes are coming next.", ::back)
            }
            entry<RoutinesDestination> {
                PlaceholderFeatureScreen("Routines", "Daily routines and goals are still in development.", ::back)
            }
            entry<WellbeingDestination> {
                PlaceholderFeatureScreen("Sleep & focus", "Sleep reminders and focus tools are still in development.", ::back)
            }
        },
    )
    com.dragonpi.timegem.ui.DismissibleFeedback(snackbar,
        Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(16.dp))
    }
}
