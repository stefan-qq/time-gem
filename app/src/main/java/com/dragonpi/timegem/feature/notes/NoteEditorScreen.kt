package com.dragonpi.timegem.feature.notes

import androidx.activity.compose.PredictiveBackHandler
import com.dragonpi.timegem.ui.rememberGentleHaptic
import kotlinx.coroutines.flow.collect
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dragonpi.timegem.data.notes.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: Note,
    isNew: Boolean,
    saving: Boolean,
    error: String?,
    onSave: (Note) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    askBeforeSaving: Boolean = false,
) {
    var title by rememberSaveable(note.id) { mutableStateOf(note.title) }
    var body by rememberSaveable(note.id) { mutableStateOf(note.body) }
    var leavePrompt by rememberSaveable { mutableStateOf(false) }
    var deletePrompt by rememberSaveable { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    val busy = saving || finishing
    val haptic = rememberGentleHaptic()
    LaunchedEffect(error, saving) { if (error != null && !saving) finishing = false }
    val dirty = title != note.title || body != note.body
    val hasContent = title.isNotBlank() || body.isNotBlank()
    fun save() {
        if (saving || finishing || !hasContent) return
        finishing = true
        haptic()
        onSave(note.copy(title = title, body = body))
    }
    fun leave() {
        if (saving || finishing) return
        if (!dirty || !hasContent) { finishing = true; onBack() }
        else if (askBeforeSaving) leavePrompt = true
        else save()
    }
    PredictiveBackHandler { progress ->
        progress.collect { }
        leave()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New note" else "Edit note") },
                navigationIcon = { IconButton(onClick = { leave() }, enabled = !busy) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    if (!isNew) IconButton(onClick = { deletePrompt = true }, enabled = !busy) { Icon(Icons.Rounded.Delete, "Delete note") }
                    TextButton(onClick = { save() }, enabled = hasContent && !busy, modifier = Modifier.widthIn(min = 72.dp)) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 760.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                OutlinedTextField(
                    value = title, onValueChange = { title = it.take(200) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Title") },
                    singleLine = true, readOnly = busy,
                )
                OutlinedTextField(
                    value = body, onValueChange = { body = it.take(100_000) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp), label = { Text("Note") },
                    readOnly = busy,
                )
                Text(if (askBeforeSaving) "You’ll be asked to save when you leave." else "Saved on this device when you leave. Empty drafts aren’t saved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (title.length == 200 || body.length == 100_000) Text("Title limit: 200 characters. Note limit: 100,000 characters.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (leavePrompt) AlertDialog(
        onDismissRequest = { leavePrompt = false },
        title = { Text("Save your changes?") },
        text = { Text("You have changes that have not been saved.") },
        confirmButton = { TextButton(onClick = { leavePrompt = false; save() }, enabled = hasContent && !busy) { Text("Save") } },
        dismissButton = {
            Row {
                TextButton(onClick = { leavePrompt = false }) { Text("Keep editing") }
                TextButton(onClick = { leavePrompt = false; finishing = true; onBack() }) { Text("Discard") }
            }
        },
    )
    if (deletePrompt) AlertDialog(
        onDismissRequest = { deletePrompt = false },
        title = { Text("Delete this note?") },
        text = { Text("This permanently removes the note from this device.") },
        confirmButton = { TextButton(onClick = { deletePrompt = false; finishing = true; haptic(); onDelete() }, enabled = !busy) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deletePrompt = false }) { Text("Cancel") } },
    )
}
