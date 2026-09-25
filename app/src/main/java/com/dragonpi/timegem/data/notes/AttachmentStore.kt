package com.dragonpi.timegem.data.notes

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class AttachmentStore(private val context: Context) {
    fun file(noteId: String, attachment: Attachment): File {
        require(noteId.matches(Regex("[a-zA-Z0-9_-]{1,100}")))
        require(attachment.fileName.matches(Regex("[a-zA-Z0-9-]{1,100}")))
        return File(File(context.filesDir, "attachments/$noteId"), attachment.fileName)
    }

    suspend fun import(noteId: String, uri: Uri, type: AttachmentType): Attachment = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty()
        require(mime.startsWith(if (type == AttachmentType.IMAGE) "image/" else "audio/")) { "Choose a supported ${type.name.lowercase()} file." }
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }?.take(200) ?: if (type == AttachmentType.IMAGE) "Image" else "Audio"
        val attachment = Attachment(UUID.randomUUID().toString(), name, type)
        val target = file(noteId, attachment)
        target.parentFile!!.mkdirs()
        try {
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 50L * 1024 * 1024) { "Choose a file smaller than 50 MB." }
                        output.write(buffer, 0, count)
                    }
                    require(total > 0) { "This file is empty." }
                }
            } ?: error("This file could not be opened.")
            if (type == AttachmentType.IMAGE) {
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeFile(target.path, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "This image could not be opened." }
            }
            attachment
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    suspend fun remove(noteId: String, items: List<Attachment>) = withContext(Dispatchers.IO) {
        items.forEach { file(noteId, it).delete() }
    }

    suspend fun removeNote(noteId: String) = withContext(Dispatchers.IO) {
        require(noteId.matches(Regex("[a-zA-Z0-9_-]{1,100}")))
        File(context.filesDir, "attachments/$noteId").deleteRecursively()
        Unit
    }
}
