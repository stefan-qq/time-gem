package com.dragonpi.timegem.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dragonpi.timegem.data.preferences.AppPreferencesRepository
import com.dragonpi.timegem.data.preferences.Appearance
import com.dragonpi.timegem.feature.setup.SetupScreen
import com.dragonpi.timegem.feature.notes.NotesViewModel
import com.dragonpi.timegem.navigation.TimeGemNavigation
import com.dragonpi.timegem.ui.theme.TimeGemTheme
import com.dragonpi.timegem.ui.LocalHapticsEnabled

@Composable
fun TimeGemRoot(preferencesRepository: AppPreferencesRepository) {
    val viewModel: TimeGemAppViewModel = viewModel(factory = TimeGemAppViewModel.factory(preferencesRepository))
    val notesViewModel: NotesViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val preferences = (uiState as? TimeGemAppUiState.Ready)?.preferences
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }
    TimeGemTheme(appearance = preferences?.appearance ?: Appearance()) {
        CompositionLocalProvider(LocalHapticsEnabled provides (preferences?.interactions?.haptics ?: true)) {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                    if (preferences == null) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
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
                        )
                    }
                    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
                }
            }
        }
    }
}
