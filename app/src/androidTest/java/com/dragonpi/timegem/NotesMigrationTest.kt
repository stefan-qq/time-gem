package com.dragonpi.timegem

import androidx.test.platform.app.InstrumentationRegistry
import com.dragonpi.timegem.data.notes.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class NotesMigrationTest {
    @Test fun existingNotesSurviveMigrationAndNewFieldsSurviveReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "migration-${UUID.randomUUID()}.db"
        try {
            context.openOrCreateDatabase(databaseName, 0, null).use { db ->
                db.execSQL("CREATE TABLE notes (id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, updated_at INTEGER NOT NULL)")
                db.execSQL("INSERT INTO notes VALUES (?, ?, ?, ?)", arrayOf<Any>("old", "Existing note", "Keep my text", 12345L))
                db.version = 1
            }
            NotesRepository(context, databaseName).use { repo ->
                val original = repo.load().single()
                assertEquals("Keep my text", original.body)
                assertEquals(12345L, original.createdAt)
                assertFalse(original.pinned)
                assertEquals(NoteColor.DEFAULT, original.color)
                assertTrue(original.attachments.isEmpty())
                repo.save(original.copy(pinned = true, color = NoteColor.MINT,
                    attachments = listOf(Attachment("test-file", "A photo \"one\".png", AttachmentType.IMAGE))))
            }
            NotesRepository(context, databaseName).use { repo ->
                val saved = repo.load().single()
                assertEquals(12345L, saved.createdAt)
                assertTrue(saved.pinned)
                assertEquals(NoteColor.MINT, saved.color)
                assertEquals("A photo \"one\".png", saved.attachments.single().name)
                repo.save(saved.copy(title = "Renamed", createdAt = 0))
                assertEquals(12345L, repo.load().single().createdAt)
            }
        } finally { context.deleteDatabase(databaseName) }
    }

    @Test fun attachmentOnlyNotePersistsAndTrulyEmptyNoteIsRejected() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "media-${UUID.randomUUID()}.db"
        try {
            NotesRepository(context, databaseName).use { repo ->
                val media = Note("audio", attachments = listOf(Attachment("file", "Recording.wav", AttachmentType.AUDIO)))
                repo.save(media)
                assertEquals(media.attachments, repo.load().single().attachments)
                var rejected = false
                try { repo.save(Note("empty", title = "  ", pinned = true, color = NoteColor.ROSE)) }
                catch (_: IllegalArgumentException) { rejected = true }
                assertTrue(rejected)
                assertEquals(1, repo.load().size)
            }
        } finally { context.deleteDatabase(databaseName) }
    }

    @Test fun attachmentPathsCannotEscapeAppStorage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val files = AttachmentStore(context)
        assertThrows(IllegalArgumentException::class.java) { files.file("../escape", Attachment("file", "name", AttachmentType.IMAGE)) }
        assertThrows(IllegalArgumentException::class.java) { files.file("note", Attachment("../escape", "name", AttachmentType.IMAGE)) }
    }
}
