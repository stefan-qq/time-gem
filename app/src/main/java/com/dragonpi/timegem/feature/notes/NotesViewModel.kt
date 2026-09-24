package com.dragonpi.timegem.feature.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dragonpi.timegem.data.notes.Note
import com.dragonpi.timegem.data.notes.NotesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NotesState {
    data object Loading : NotesState
    data class Ready(val notes: List<Note>) : NotesState
    data object Failed : NotesState
}

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotesRepository(application)
    private val _state = MutableStateFlow<NotesState>(NotesState.Loading)
    val state = _state.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _state.value = NotesState.Loading
            try { _state.value = NotesState.Ready(repository.load()) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.value = NotesState.Failed }
        }
    }

    fun clearError() { _error.value = null }

    fun save(note: Note, onSaved: () -> Unit) = change("Could not save your note. Your text is still here. Try again.", onSaved) {
        val saved = repository.save(note)
        val current = (_state.value as? NotesState.Ready)?.notes.orEmpty()
        _state.value = NotesState.Ready((current.filterNot { it.id == saved.id } + saved)
            .sortedWith(compareByDescending<Note> { it.updatedAt }.thenBy { it.id }))
    }

    fun delete(id: String, onDeleted: () -> Unit) = change("Could not delete this note. Try again.", onDeleted) {
        repository.delete(id)
        val current = (_state.value as? NotesState.Ready)?.notes.orEmpty()
        _state.value = NotesState.Ready(current.filterNot { it.id == id })
    }

    private fun change(message: String, onSuccess: () -> Unit, action: suspend () -> Unit) {
        if (_saving.value) return
        _saving.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                action()
                onSuccess()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _error.value = message }
            finally { _saving.value = false }
        }
    }

    override fun onCleared() { repository.close() }
}
