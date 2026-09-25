package com.dragonpi.timegem.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dragonpi.timegem.data.preferences.AppPreferencesRepository
import com.dragonpi.timegem.data.preferences.Appearance
import com.dragonpi.timegem.feature.setup.SetupScreen
import com.dragonpi.timegem.feature.notes.NotesViewModel
import com.dragonpi.timegem.feature.notes.NotesState
import com.dragonpi.timegem.navigation.TimeGemNavigation
import com.dragonpi.timegem.ui.theme.TimeGemTheme
import com.dragonpi.timegem.ui.LocalHapticsEnabled

@Composable
fun TimeGemRoot(preferencesRepository: AppPreferencesRepository, onReady: () -> Unit = {}, newNoteRequest: String? = null, onNewNoteHandled: () -> Unit = {}) {
    val viewModel: TimeGemAppViewModel = viewModel(factory = TimeGemAppViewModel.factory(preferencesRepository))
    val notesViewModel: NotesViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notesState by notesViewModel.state.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val preferences = (uiState as? TimeGemAppUiState.Ready)?.preferences
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }
    var hasStarted by remember { mutableStateOf(false) }
    if (!hasStarted && (uiState is TimeGemAppUiState.Loading || (preferences?.setupCompleted == true && notesState is NotesState.Loading))) return
    SideEffect { hasStarted = true; onReady() }
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(280)) }
    TimeGemTheme(appearance = preferences?.appearance ?: Appearance()) {
        CompositionLocalProvider(LocalHapticsEnabled provides (preferences?.interactions?.haptics ?: true)) {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = entrance.value }) {
                    if (preferences == null) {
                        Text("Could not load your settings. Close Time Gem and try again.", Modifier.align(Alignment.Center).padding(24.dp))
                    } else if (!preferences.setupCompleted) {
                        SetupScreen(
                            appearance = preferences.appearance,
                            onAppearanceChange = viewModel::updateAppearance,
                            saving = saving,
                            onFinish = viewModel::completeSetup,
                        )
                    } else {
                        TimeGemNavigation(
                            preferences = preferences,
                            onHomeOptionsChange = viewModel::updateHomeOptions,
                            onAppearanceChange = viewModel::updateAppearance,
                            onFeaturesChange = viewModel::updateFeatures,
                            onInteractionsChange = viewModel::updateInteractions,
                            notesViewModel = notesViewModel,
                            newNoteRequest = newNoteRequest,
                            onNewNoteHandled = onNewNoteHandled,
                        )
                    }
                    com.dragonpi.timegem.ui.DismissibleFeedback(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
                }
            }
        }
    }
}
