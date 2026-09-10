package com.dragonpi.timegem.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dragonpi.timegem.data.preferences.AppPreferencesRepository
import com.dragonpi.timegem.data.preferences.SetupChoices
import com.dragonpi.timegem.data.preferences.TimeGemPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TimeGemAppUiState {
    data object Loading : TimeGemAppUiState
    data class Ready(val preferences: TimeGemPreferences) : TimeGemAppUiState
}

class TimeGemAppViewModel(
    private val preferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<TimeGemAppUiState> = preferencesRepository.preferences
        .map<TimeGemPreferences, TimeGemAppUiState> { TimeGemAppUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimeGemAppUiState.Loading,
        )

    fun completeSetup(choices: SetupChoices) {
        viewModelScope.launch {
            preferencesRepository.completeSetup(choices)
        }
    }

    companion object {
        fun factory(repository: AppPreferencesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TimeGemAppViewModel(repository) as T
                }
            }
    }
}
