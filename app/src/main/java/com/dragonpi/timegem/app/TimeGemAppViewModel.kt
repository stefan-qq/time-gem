package com.dragonpi.timegem.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dragonpi.timegem.data.preferences.AppPreferencesRepository
import com.dragonpi.timegem.data.preferences.HomeOptions
import com.dragonpi.timegem.data.preferences.Appearance
import com.dragonpi.timegem.data.preferences.InteractionOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.dragonpi.timegem.data.preferences.SetupChoices
import com.dragonpi.timegem.data.preferences.TimeGemPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TimeGemAppUiState {
    data object Loading : TimeGemAppUiState
    data object Failed : TimeGemAppUiState
    data class Ready(val preferences: TimeGemPreferences) : TimeGemAppUiState
}

class TimeGemAppViewModel(
    private val preferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<TimeGemAppUiState> = preferencesRepository.preferences
        .map<TimeGemPreferences, TimeGemAppUiState> { TimeGemAppUiState.Ready(it) }
        .catch { emit(TimeGemAppUiState.Failed) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimeGemAppUiState.Loading,
        )

    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private var pendingWrites = 0

    fun clearError() { _error.value = null }

    fun completeSetup(choices: SetupChoices) = update { preferencesRepository.completeSetup(choices) }
    fun updateHomeOptions(options: HomeOptions) = update { preferencesRepository.updateHomeOptions(options) }
    fun updateInteractions(options: InteractionOptions) = update { preferencesRepository.updateInteractions(options) }
    fun updateAppearance(appearance: Appearance) = update { preferencesRepository.updateAppearance(appearance) }
    fun updateFeatures(routines: Boolean, wellbeing: Boolean, reflections: Boolean) = update {
        preferencesRepository.updateFeatures(routines, wellbeing, reflections)
    }

    private fun update(action: suspend () -> Unit) {
        pendingWrites++
        _saving.value = true
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _error.value = "Could not save your settings. Please try again." }
            finally { pendingWrites--; _saving.value = pendingWrites > 0 }
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
