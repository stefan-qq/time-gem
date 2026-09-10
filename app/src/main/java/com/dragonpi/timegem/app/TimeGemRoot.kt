package com.dragonpi.timegem.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dragonpi.timegem.data.preferences.AppPreferencesRepository
import com.dragonpi.timegem.feature.setup.SetupScreen
import com.dragonpi.timegem.navigation.TimeGemNavigation
import com.dragonpi.timegem.ui.theme.TimeGemTheme

@Composable
fun TimeGemRoot(
    preferencesRepository: AppPreferencesRepository,
) {
    val viewModel: TimeGemAppViewModel = viewModel(
        factory = TimeGemAppViewModel.factory(preferencesRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val dynamicColor = (uiState as? TimeGemAppUiState.Ready)
        ?.preferences
        ?.dynamicColorEnabled
        ?: false

    TimeGemTheme(dynamicColor = dynamicColor) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "app_state",
        ) { state ->
            when (state) {
                TimeGemAppUiState.Loading -> LoadingScreen()

                is TimeGemAppUiState.Ready -> {
                    if (state.preferences.setupCompleted) {
                        TimeGemNavigation(preferences = state.preferences)
                    } else {
                        SetupScreen(onFinish = viewModel::completeSetup)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
