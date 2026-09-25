package com.dragonpi.timegem.feature.notes

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dragonpi.timegem.data.notes.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NoteImage(noteId: String, attachment: Attachment, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, noteId, attachment.fileName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = AttachmentStore(context).file(noteId, attachment)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > 1200 || bounds.outHeight / inSampleSize > 1200) inSampleSize *= 2
                }
                BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
            }.getOrNull()
        }
    }
    if (bitmap != null) Image(bitmap!!, attachment.name, modifier, contentScale = ContentScale.Fit)
    else Box(modifier, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Image, "Image preview unavailable") }
}

@Composable
fun AudioAttachment(noteId: String, attachment: Attachment, enabled: Boolean, onRemove: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var playing by remember { mutableStateOf(false) }
    var preparing by remember { mutableStateOf(false) }
    var prepared by remember { mutableStateOf(false) }
    var position by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    var scrubbing by remember { mutableStateOf(false) }
    LaunchedEffect(noteId, attachment.fileName) {
        duration = withContext(Dispatchers.IO) {
            val metadata = android.media.MediaMetadataRetriever()
            try {
                metadata.setDataSource(AttachmentStore(context).file(noteId, attachment).path)
                metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull() ?: 0
            } catch (_: Exception) { 0 } finally { metadata.release() }
        }
    }
    var message by remember { mutableStateOf<String?>(null) }
    val player = remember(attachment.fileName) { MediaPlayer() }
    val audioManager = remember(context) { context.getSystemService(AudioManager::class.java) }
    val audioAttributes = remember { AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build() }
    val focusRequest = remember(player) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { change ->
                if (change < 0 && playing) { player.pause(); playing = false }
            }.build()
    }
    LaunchedEffect(playing) {
        while (playing) {
            if (!scrubbing) position = runCatching { player.currentPosition }.getOrDefault(position)
            kotlinx.coroutines.delay(100)
        }
    }
    fun play() {
        if (audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            player.start()
            playing = true
        } else message = "Audio playback is unavailable right now."
    }
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (playing) player.pause()
                playing = false
                audioManager.abandonAudioFocusRequest(focusRequest)
                if (preparing) { player.reset(); preparing = false; prepared = false }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); audioManager.abandonAudioFocusRequest(focusRequest); player.release() }
    }
    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(enabled = enabled && !preparing, onClick = {
            if (playing) { player.pause(); playing = false; audioManager.abandonAudioFocusRequest(focusRequest) }
            else if (prepared) { play() }
            else {
                message = null
                runCatching {
                    player.reset()
                    player.setAudioAttributes(audioAttributes)
                    player.setDataSource(AttachmentStore(context).file(noteId, attachment).path)
                    player.setOnPreparedListener { preparing = false; prepared = true; duration = player.duration; play() }
                    player.setOnCompletionListener { position = duration; playing = false; audioManager.abandonAudioFocusRequest(focusRequest) }
                    player.setOnErrorListener { _, _, _ -> preparing = false; prepared = false; playing = false; message = "This audio could not be played."; true }
                    preparing = true
                    player.prepareAsync()
                }.onFailure { preparing = false; message = "This audio could not be opened." }
            }
        }) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pause audio" else "Play audio") }
        Column(Modifier.weight(1f)) {
            Text(attachment.name, style = MaterialTheme.typography.bodyMedium)
            if (message != null) Text(message!!, color = MaterialTheme.colorScheme.error)
            else if (preparing) Text("Opening audio…", style = MaterialTheme.typography.bodySmall)
        }
        IconButton(enabled = enabled, onClick = onRemove) { Icon(Icons.Rounded.Close, "Remove ${attachment.name}") }
    }
    AudioProgress(position, duration, playing, prepared && enabled,
        onSeek = { scrubbing = true; position = it },
        onSeekFinished = { if (prepared) player.seekTo(position); scrubbing = false })
    }
}
