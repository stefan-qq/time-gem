package com.dragonpi.timegem.feature.notes

import androidx.activity.compose.PredictiveBackHandler
import com.dragonpi.timegem.ui.rememberGentleHaptic
import kotlinx.coroutines.flow.collect
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragonpi.timegem.data.notes.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.flow.first
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
    initialAttachmentType: String? = null,
) {
    var title by rememberSaveable(note.id) { mutableStateOf(note.title) }
    var body by rememberSaveable(note.id) { mutableStateOf(note.body) }
    var leavePrompt by rememberSaveable { mutableStateOf(false) }
    var deletePrompt by rememberSaveable { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    var pinned by rememberSaveable(note.id) { mutableStateOf(note.pinned) }
    var color by rememberSaveable(note.id) { mutableStateOf(note.color) }
    var mediaJson by rememberSaveable(note.id) { mutableStateOf(encodeAttachments(note.attachments)) }
    val media = remember(mediaJson) { decodeAttachments(mediaJson) }
    var colorsOpen by remember { mutableStateOf(false) }
    var captureType by rememberSaveable { mutableStateOf<String?>(null) }
    var addOpen by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var initialActionDone by rememberSaveable(note.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val files = remember(context) { AttachmentStore(context) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val windowInfo = LocalWindowInfo.current
    val background = noteBackground(color)
    val draft = note.copy(title = title, body = body, pinned = pinned, color = color, attachments = media)
    val busy = saving || finishing || importing
    val haptic = rememberGentleHaptic()
    LaunchedEffect(error, saving) { if (error != null && !saving) finishing = false }
    val dirty = draft != note
    val hasContent = draft.hasContent
    fun save() {
        if (saving || finishing || importing || !hasContent) return
        finishing = true
        haptic()
        onSave(draft)
    }
    fun discard() {
        if (finishing) return
        finishing = true
        val draftFiles = media.filterNot { it in note.attachments }
        if (draftFiles.isEmpty()) onBack()
        else scope.launch {
            files.remove(note.id, draftFiles)
            onBack()
        }
    }
    fun leave() {
        if (saving || finishing || importing) return
        if (!dirty || !hasContent) discard()
        else if (askBeforeSaving) leavePrompt = true
        else save()
    }
    fun import(uri: Uri?, type: AttachmentType, cleanup: () -> Unit = {}) {
        if (uri == null) return
        if (media.size >= 20) { cleanup(); importError = "A note can hold up to 20 attachments."; return }
        importing = true
        scope.launch {
            try {
                val attachment = files.import(note.id, uri, type)
                mediaJson = encodeAttachments(decodeAttachments(mediaJson) + attachment)
                importError = null
                haptic()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { importError = failure.message ?: "Could not add this file. Try again." }
            finally { cleanup(); importing = false }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { import(it, AttachmentType.IMAGE) }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { import(it, AttachmentType.AUDIO) }
    LaunchedEffect(note.id) {
        if (isNew && !initialActionDone) {
            snapshotFlow { windowInfo.isWindowFocused }.first { it }
            withFrameNanos { }
            initialActionDone = true
            when (initialAttachmentType) {
                "IMAGE" -> captureType = "IMAGE"
                "AUDIO" -> captureType = "AUDIO"
                else -> { focus.requestFocus(); keyboard?.show() }
            }
        }
    }
    PredictiveBackHandler { progress ->
        progress.collect { }
        leave()
    }
    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background),
                navigationIcon = { IconButton(onClick = { leave() }, enabled = !busy) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    IconToggleButton(checked = pinned, enabled = !busy, onCheckedChange = { pinned = it; haptic() }) {
                        Icon(Icons.Rounded.PushPin, if (pinned) "Unpin note" else "Pin note", tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!isNew) IconButton(onClick = { deletePrompt = true }, enabled = !busy) { Icon(Icons.Rounded.Delete, "Delete note") }
                    TextButton(onClick = { save() }, enabled = hasContent && !busy, modifier = Modifier.widthIn(min = 72.dp)) { Text("Save") }
                },
            )
        },
        bottomBar = {
            Surface(color = background) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { keyboard?.hide(); addOpen = true; haptic() }, enabled = !busy) { Icon(Icons.Rounded.AddBox, "Add attachment") }
                    IconButton(onClick = { keyboard?.hide(); colorsOpen = true; haptic() }, enabled = !busy) { Icon(Icons.Rounded.Palette, "Note color") }
                    Spacer(Modifier.weight(1f))
                    Text(if (importing) "Adding attachment…" else if (pinned) "Pinned" else "", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 760.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                (error ?: importError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                BasicTextField(title, { title = it.take(200) }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Title" },
                    readOnly = busy, textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = { field ->
                        Box { if (title.isEmpty()) Text("Title", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); field() }
                    })
                BasicTextField(body, { body = it.take(100_000) }, modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp).focusRequester(focus).semantics { contentDescription = "Note" },
                    readOnly = busy, textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, letterSpacing = 0.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = { field ->
                        Box { if (body.isEmpty()) Text("Note", color = MaterialTheme.colorScheme.onSurfaceVariant); field() }
                    })
                media.forEach { attachment ->
                    key(attachment.fileName) {
                        OutlinedCard(colors = CardDefaults.outlinedCardColors(containerColor = background)) {
                            if (attachment.type == AttachmentType.IMAGE) NoteImage(note.id, attachment, Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 360.dp))
                            else AudioAttachment(note.id, attachment, !busy) {
                                mediaJson = encodeAttachments(media - attachment)
                                if (attachment !in note.attachments) scope.launch { files.remove(note.id, listOf(attachment)) }
                            }
                            if (attachment.type == AttachmentType.IMAGE) Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(attachment.name, Modifier.weight(1f), maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                IconButton(enabled = !busy, onClick = {
                                    mediaJson = encodeAttachments(media - attachment)
                                    if (attachment !in note.attachments) scope.launch { files.remove(note.id, listOf(attachment)) }
                                }) { Icon(Icons.Rounded.Close, "Remove ${attachment.name}") }
                            }
                        }
                    }
                }
                if (title.length == 200 || body.length == 100_000) Text("Title limit: 200 characters. Note limit: 100,000 characters.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (colorsOpen) ModalBottomSheet(onDismissRequest = { colorsOpen = false }) {
        Text("Note color", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.titleLarge)
        FlowRow(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoteColor.entries.forEach { option ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FilledIconToggleButton(checked = color == option, onCheckedChange = { color = option; haptic() },
                        modifier = Modifier.size(56.dp).semantics { contentDescription = option.label }, shape = CircleShape,
                        colors = IconButtonDefaults.filledIconToggleButtonColors(containerColor = noteBackground(option), checkedContainerColor = noteBackground(option))) {
                        if (color == option) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(option.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (addOpen) ModalBottomSheet(onDismissRequest = { addOpen = false }) {
        Text("Add to note", Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
        ListItem(headlineContent = { TextButton(onClick = { addOpen = false; captureType = "IMAGE" }) { Text("Image") } }, leadingContent = { Icon(Icons.Rounded.Image, null) })
        ListItem(headlineContent = { TextButton(onClick = { addOpen = false; captureType = "AUDIO" }) { Text("Audio") } }, leadingContent = { Icon(Icons.Rounded.AudioFile, null) })
        Spacer(Modifier.height(24.dp))
    }
    CaptureChoices(type = captureType, onDismiss = { captureType = null },
        onGallery = { captureType = null; imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onAudioFile = { captureType = null; audioPicker.launch(arrayOf("audio/*")) },
        onCaptured = { uri, type, cleanup -> captureType = null; import(uri, type, cleanup) })
    if (leavePrompt) AlertDialog(
        onDismissRequest = { leavePrompt = false },
        title = { Text("Save your changes?") },
        text = { Text("You have changes that have not been saved.") },
        confirmButton = { TextButton(onClick = { leavePrompt = false; save() }, enabled = hasContent && !busy) { Text("Save") } },
        dismissButton = {
            Row {
                TextButton(onClick = { leavePrompt = false }) { Text("Keep editing") }
                TextButton(onClick = { leavePrompt = false; discard() }) { Text("Discard") }
            }
        },
    )
    if (deletePrompt) AlertDialog(
        onDismissRequest = { deletePrompt = false },
        title = { Text("Delete this note?") },
        text = { Text("This permanently removes the note and its attachments from this device.") },
        confirmButton = { TextButton(onClick = { deletePrompt = false; finishing = true; haptic(); onDelete() }, enabled = !busy) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deletePrompt = false }) { Text("Cancel") } },
    )
}
