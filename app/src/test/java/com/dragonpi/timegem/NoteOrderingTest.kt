package com.dragonpi.timegem

import com.dragonpi.timegem.data.notes.Note
import com.dragonpi.timegem.data.notes.orderNotes
import com.dragonpi.timegem.data.preferences.NoteSort
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteOrderingTest {
    private val notes = listOf(
        Note("a", title = "Zebra", updatedAt = 30, createdAt = 10),
        Note("b", title = "alpha", updatedAt = 20, createdAt = 30),
        Note("c", title = "Pinned", updatedAt = 1, createdAt = 1, pinned = true),
    )

    @Test fun pinStaysFirstAcrossEverySortOrder() {
        assertEquals(listOf("c", "a", "b"), orderNotes(notes, NoteSort.MODIFIED).map { it.id })
        assertEquals(listOf("c", "b", "a"), orderNotes(notes, NoteSort.CREATED).map { it.id })
        assertEquals(listOf("c", "b", "a"), orderNotes(notes, NoteSort.TITLE).map { it.id })
    }

    @Test fun equalTimestampsHaveStableOrder() {
        val equal = listOf(Note("b", updatedAt = 5), Note("a", updatedAt = 5))
        assertEquals(listOf("a", "b"), orderNotes(equal, NoteSort.MODIFIED).map { it.id })
    }
}
