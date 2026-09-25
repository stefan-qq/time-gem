package com.dragonpi.timegem.feature.notes

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dragonpi.timegem.data.notes.AttachmentType
import java.io.File
import java.util.UUID

@Composable
fun CaptureChoices(
    type: String?, onDismiss: () -> Unit, onGallery: () -> Unit, onAudioFile: () -> Unit,
    onCaptured: (Uri, AttachmentType, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var cameraFile by rememberSaveable { mutableStateOf<String?>(null) }
    var recordingOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun captureFile(name: String) = File(context.cacheDir, "capture/$name").also { it.parentFile!!.mkdirs() }
    fun uri(file: File) = FileProvider.getUriForFile(context, "${context.packageName}.capture", file)
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        cameraFile?.let { name ->
            val file = captureFile(name)
            if (success && file.length() > 0) onCaptured(uri(file), AttachmentType.IMAGE) { file.delete() }
            else file.delete()
        }
        cameraFile = null
    }
    if (type != null && !recordingOpen) AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == "IMAGE") "Add image" else "Add audio") },
        text = {
            Column {
                if (type == "IMAGE") {
                    TextButton(onClick = {
                        val name = "photo-${UUID.randomUUID()}.jpg"
                        cameraFile = name
                        runCatching { camera.launch(uri(captureFile(name))) }.onFailure {
                            captureFile(name).delete(); cameraFile = null; error = "No camera app is available. Choose an image instead."
                        }
                    }) { Icon(Icons.Rounded.PhotoCamera, null); Spacer(Modifier.width(12.dp)); Text("Take photo") }
                    TextButton(onClick = onGallery) { Icon(Icons.Rounded.Image, null); Spacer(Modifier.width(12.dp)); Text("Choose image") }
                } else {
                    TextButton(onClick = { recordingOpen = true }) { Icon(Icons.Rounded.Mic, null); Spacer(Modifier.width(12.dp)); Text("Record audio") }
                    TextButton(onClick = onAudioFile) { Icon(Icons.Rounded.AudioFile, null); Spacer(Modifier.width(12.dp)); Text("Choose file") }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (recordingOpen) RecordingDialog(onDismiss = { recordingOpen = false }, onRecorded = { file ->
        recordingOpen = false
        onCaptured(uri(file), AttachmentType.AUDIO) { file.delete() }
    })
}

@Suppress("DEPRECATION")
@Composable
private fun RecordingDialog(onDismiss: () -> Unit, onRecorded: (File) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var output by remember { mutableStateOf<File?>(null) }
    val levels = remember { mutableStateListOf<Float>().apply { repeat(36) { add(0f) } } }
    var startedAt by remember { mutableLongStateOf(0L) }
    var seconds by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    fun cancel() {
        runCatching { recorder?.reset() }; recorder?.release(); recorder = null
        output?.delete(); output = null
    }
    fun finish() {
        val current = recorder ?: return
        val file = output
        val success = runCatching { current.stop() }.isSuccess
        current.release(); recorder = null; output = null
        if (success && file != null && file.length() > 0) onRecorded(file)
        else { file?.delete(); error = "Recording was too short. Try again." }
    }
    fun start() {
        if (recorder != null) return
        val file = File(context.cacheDir, "capture/recording-${UUID.randomUUID()}.m4a").also { it.parentFile!!.mkdirs() }
        val current = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        output = file; recorder = current
        runCatching {
            current.setAudioSource(MediaRecorder.AudioSource.MIC)
            current.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            current.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            current.setAudioEncodingBitRate(128000)
            current.setOutputFile(file.path)
            current.setMaxDuration(5 * 60 * 1000)
            current.setOnInfoListener { _, what, _ -> if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) finish() }
            current.setOnErrorListener { _, _, _ -> cancel(); error = "Recording stopped. Please try again." }
            current.prepare(); current.start(); startedAt = android.os.SystemClock.elapsedRealtime(); seconds = 0; error = null
        }.onFailure { cancel(); error = "The microphone is unavailable. Check permission and try again." }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else error = "Microphone permission is needed to record. You can still choose an audio file."
    }
    LaunchedEffect(recorder) {
        if (recorder != null) while (true) {
            kotlinx.coroutines.delay(100)
            seconds = ((android.os.SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()
            val peak = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
            levels.removeAt(0); levels.add(kotlin.math.sqrt(peak / 32767f).coerceIn(0f, 1f))
        }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && recorder != null) { cancel(); error = "Recording cancelled when the app left the screen." }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); cancel() }
    }
    AlertDialog(
        onDismissRequest = { cancel(); onDismiss() },
        title = { Text("Record audio") },
        text = { Column {
            Text(if (recorder == null) "Record up to 5 minutes. Audio stays on your device." else "Recording ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}")
            RecordingLevels(levels, recorder != null)
            if (recorder != null) LinearProgressIndicator(progress = { seconds / 300f }, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(onClick = {
            if (recorder != null) finish()
            else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) start()
            else permission.launch(Manifest.permission.RECORD_AUDIO)
        }) { Text(if (recorder == null) "Start recording" else "Stop and attach") } },
        dismissButton = { TextButton(onClick = { cancel(); onDismiss() }) { Text("Cancel") } },
    )
}
