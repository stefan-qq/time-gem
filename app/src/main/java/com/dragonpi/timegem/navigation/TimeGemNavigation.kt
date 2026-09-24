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

@Composable
fun TimeGemNavigation(
    preferences: TimeGemPreferences,
    onHomeOptionsChange: (HomeOptions) -> Unit,
    onAppearanceChange: (Appearance) -> Unit,
    onFeaturesChange: (Boolean, Boolean, Boolean) -> Unit,
    notesViewModel: NotesViewModel,
    onInteractionsChange: (InteractionOptions) -> Unit,
) {
    val backStack = rememberNavBackStack(HomeDestination)
    val notesState by notesViewModel.state.collectAsStateWithLifecycle()
    val saving by notesViewModel.saving.collectAsStateWithLifecycle()
    val noteError by notesViewModel.error.collectAsStateWithLifecycle()
    fun back() { if (backStack.size > 1) backStack.removeLastOrNull() }
    fun open(destination: NavKey) {
        if (backStack.lastOrNull() != destination) backStack.add(destination)
    }
    NavDisplay(
        backStack = backStack,
        onBack = { if (!saving) back() },
        transitionSpec = { (fadeIn(tween(130)) + slideInHorizontally(tween(160)) { it / 24 }) togetherWith ExitTransition.None },
        popTransitionSpec = { EnterTransition.None togetherWith (fadeOut(tween(110)) + slideOutHorizontally(tween(140)) { it / 24 }) },
        predictivePopTransitionSpec = { _ -> EnterTransition.None togetherWith (fadeOut(tween(140)) + slideOutHorizontally(tween(140)) { it / 24 }) },
        entryProvider = entryProvider {
            entry<HomeDestination> {
                HomeScreen(
                    preferences = preferences,
                    notesState = notesState,
                    onRetry = notesViewModel::reload,
                    onCreateNote = { notesViewModel.clearError(); open(NoteDestination(UUID.randomUUID().toString(), true)) },
                    onOpenNote = { notesViewModel.clearError(); open(NoteDestination(it.id)) },
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
                        saving = saving,
                        error = noteError,
                        askBeforeSaving = preferences.interactions.askBeforeSaving,
                        onSave = { notesViewModel.save(it, closeEditor) },
                        onDelete = { notesViewModel.delete(destination.id, closeEditor) },
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
}
