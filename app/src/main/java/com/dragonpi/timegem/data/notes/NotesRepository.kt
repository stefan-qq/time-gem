package com.dragonpi.timegem.data.notes

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private class NotesDatabase(context: Context, name: String) : SQLiteOpenHelper(context, name, null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE notes (id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, updated_at INTEGER NOT NULL, created_at INTEGER NOT NULL, pinned INTEGER NOT NULL DEFAULT 0, color TEXT NOT NULL DEFAULT 'DEFAULT', attachments TEXT NOT NULL DEFAULT '[]')")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE notes ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE notes SET created_at = updated_at")
            db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE notes ADD COLUMN color TEXT NOT NULL DEFAULT 'DEFAULT'")
            db.execSQL("ALTER TABLE notes ADD COLUMN attachments TEXT NOT NULL DEFAULT '[]'")
        }
    }
}

enum class NoteColor(val label: String) { DEFAULT("Default"), CORAL("Coral"), PEACH("Peach"), SAND("Sand"), MINT("Mint"), SKY("Sky"), LAVENDER("Lavender"), ROSE("Rose") }
enum class AttachmentType { IMAGE, AUDIO }
data class Attachment(val fileName: String, val name: String, val type: AttachmentType)

fun encodeAttachments(items: List<Attachment>): String = JSONArray().apply {
    items.forEach { put(JSONObject().put("file", it.fileName).put("name", it.name).put("type", it.type.name)) }
}.toString()

fun decodeAttachments(value: String): List<Attachment> {
    val array = JSONArray(value)
    return List(array.length()) { index ->
        val item = array.getJSONObject(index)
        Attachment(item.getString("file"), item.getString("name"), AttachmentType.valueOf(item.getString("type")))
    }
}

data class Note(
    val id: String, val title: String = "", val body: String = "", val updatedAt: Long = 0L,
    val createdAt: Long = 0L, val pinned: Boolean = false, val color: NoteColor = NoteColor.DEFAULT,
    val attachments: List<Attachment> = emptyList(),
) {
    val displayTitle: String get() = title.ifBlank { "Untitled note" }
    val hasContent: Boolean get() = title.isNotBlank() || body.isNotBlank() || attachments.isNotEmpty()
}

class NotesRepository(context: Context, databaseName: String = "time_gem_notes.db") : AutoCloseable {
    private val database = NotesDatabase(context.applicationContext, databaseName)

    suspend fun load(): List<Note> = withContext(Dispatchers.IO) {
        database.readableDatabase.query("notes", arrayOf("id", "title", "body", "updated_at", "created_at", "pinned", "color", "attachments"), null, null, null, null, "updated_at DESC, id ASC").use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(Note(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3),
                    cursor.getLong(4), cursor.getInt(5) != 0,
                    NoteColor.entries.firstOrNull { it.name == cursor.getString(6) } ?: NoteColor.DEFAULT,
                    decodeAttachments(cursor.getString(7))))
            }
        }
    }

    suspend fun save(note: Note): Note = withContext(Dispatchers.IO) {
        require(note.hasContent) { "A note needs text or an attachment." }
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            val originalCreatedAt = db.query("notes", arrayOf("created_at"), "id = ?", arrayOf(note.id), null, null, null).use {
                if (it.moveToFirst()) it.getLong(0) else 0L
            }
            val now = System.currentTimeMillis()
            val saved = note.copy(updatedAt = now, createdAt = originalCreatedAt.takeIf { it > 0 } ?: now)
            val values = ContentValues().apply {
                put("id", saved.id); put("title", saved.title); put("body", saved.body)
                put("updated_at", saved.updatedAt); put("created_at", saved.createdAt)
                put("pinned", if (saved.pinned) 1 else 0); put("color", saved.color.name)
                put("attachments", encodeAttachments(saved.attachments))
            }
            check(db.insertWithOnConflict("notes", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L) { "Could not save note" }
            db.setTransactionSuccessful()
            saved
        } finally { db.endTransaction() }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        database.writableDatabase.delete("notes", "id = ?", arrayOf(id))
        Unit
    }
    override fun close() = database.close()
}
