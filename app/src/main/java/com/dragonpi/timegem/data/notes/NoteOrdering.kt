package com.dragonpi.timegem.data.notes

import com.dragonpi.timegem.data.preferences.NoteSort
import java.util.Locale

fun orderNotes(notes: List<Note>, sort: NoteSort): List<Note> {
    val comparator = when (sort) {
        NoteSort.MODIFIED -> compareByDescending<Note> { it.updatedAt }
        NoteSort.CREATED -> compareByDescending<Note> { it.createdAt }
        NoteSort.TITLE -> compareBy<Note> { it.displayTitle.lowercase(Locale.ROOT) }
    }
    return notes.sortedWith(compareByDescending<Note> { it.pinned }.then(comparator).thenBy { it.id })
}
