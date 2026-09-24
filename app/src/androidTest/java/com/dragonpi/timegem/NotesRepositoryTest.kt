package com.dragonpi.timegem

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.dragonpi.timegem.data.notes.Note
import com.dragonpi.timegem.data.notes.NotesRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class NotesRepositoryTest {
    @Test fun notesSurviveReopenAndUpdateWithoutDuplicates() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "timegem-test-${UUID.randomUUID()}.db"
        var repository = NotesRepository(context, databaseName)
        try {
            val note = Note("one", "Școală 'notes'", "Line one\n第二行 🎉")
            repository.save(note)
            repository.save(Note("two", note.title, "A different note with the same title"))
            repository.close()
            repository = NotesRepository(context, databaseName)
            assertEquals(2, repository.load().size)
            assertEquals(note.body, repository.load().first { it.id == "one" }.body)
            repository.save(note.copy(body = "Edited"))
            assertEquals(2, repository.load().size)
            assertEquals("Edited", repository.load().first { it.id == "one" }.body)
            repository.delete("one")
            assertEquals(listOf("two"), repository.load().map { it.id })
        } finally {
            repository.close()
            context.deleteDatabase(databaseName)
        }
    }
}
