package com.dragonpi.timegem.data.notes

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Keep this schema versioned: future migrations must preserve the user's notes.
private class NotesDatabase(context: Context, name: String) : SQLiteOpenHelper(context, name, null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE notes (id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, updated_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Missing notes migration from $oldVersion to $newVersion")
    }
}

data class Note(val id: String, val title: String = "", val body: String = "", val updatedAt: Long = 0L) {
    val displayTitle: String get() = title.ifBlank { "Untitled note" }
}

class NotesRepository(context: Context, databaseName: String = "time_gem_notes.db") : AutoCloseable {
    private val database = NotesDatabase(context.applicationContext, databaseName)

    suspend fun load(): List<Note> = withContext(Dispatchers.IO) {
        database.readableDatabase.query("notes", arrayOf("id", "title", "body", "updated_at"), null, null, null, null, "updated_at DESC, id ASC").use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(Note(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3)))
            }
        }
    }

    suspend fun save(note: Note) = withContext(Dispatchers.IO) {
        require(note.title.isNotBlank() || note.body.isNotBlank()) { "A note needs a title or some text." }
        val saved = note.copy(updatedAt = System.currentTimeMillis())
        val values = ContentValues().apply {
            put("id", note.id)
            put("title", note.title)
            put("body", note.body)
            put("updated_at", saved.updatedAt)
        }
        check(database.writableDatabase.insertWithOnConflict("notes", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L) {
            "Could not save note"
        }
        saved
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        database.writableDatabase.delete("notes", "id = ?", arrayOf(id))
        Unit
    }

    override fun close() = database.close()
}
