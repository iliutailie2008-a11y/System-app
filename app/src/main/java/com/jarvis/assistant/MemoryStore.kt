package com.jarvis.assistant

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Memorie persistentă locală:
 * - "memories": fapte scurte, durabile, despre utilizator (nișă, progres, preferințe)
 * - "notes": documente complete, salvate atunci când System face research amplu
 *   (ex: "research dropshipping", "research codul P0171") — nu se pierd la restart.
 */
class MemoryStore(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "jarvis_memory.db"
        private const val DB_VERSION = 2
        private const val MEMORIES_TABLE = "memories"
        private const val NOTES_TABLE = "notes"
    }

    data class Note(val id: Long, val title: String, val content: String, val createdAt: Long)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $MEMORIES_TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $NOTES_TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $MEMORIES_TABLE")
        db.execSQL("DROP TABLE IF EXISTS $NOTES_TABLE")
        onCreate(db)
    }

    // ---------- fapte scurte ----------

    fun addMemory(category: String, content: String) {
        if (content.isBlank()) return
        if (getAllMemories().any { it.second.equals(content, ignoreCase = true) }) return
        val values = ContentValues().apply {
            put("category", category.trim().lowercase())
            put("content", content.trim())
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insert(MEMORIES_TABLE, null, values)
    }

    fun getAllMemories(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val cursor = readableDatabase.query(
            MEMORIES_TABLE, arrayOf("category", "content"), null, null, null, null, "created_at DESC"
        )
        cursor.use {
            while (it.moveToNext()) list.add(it.getString(0) to it.getString(1))
        }
        return list
    }

    fun clearAll() {
        writableDatabase.delete(MEMORIES_TABLE, null, null)
    }

    fun buildMemoryContext(): String {
        val memories = getAllMemories()
        if (memories.isEmpty()) return ""
        val grouped = memories.groupBy({ it.first }, { it.second })
        val builder = StringBuilder("Fapte reținute deja despre utilizator (nu le repeți cu [REMEMBER]):\n")
        for ((category, items) in grouped) {
            builder.append("- [$category]: ").append(items.joinToString("; ")).append("\n")
        }
        return builder.toString()
    }

    // ---------- notițe / documente de research ----------

    fun addNote(title: String, content: String): Long {
        val values = ContentValues().apply {
            put("title", title.trim().ifEmpty { "Notă fără titlu" })
            put("content", content.trim())
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insert(NOTES_TABLE, null, values)
    }

    fun getAllNotes(): List<Note> {
        val list = mutableListOf<Note>()
        val cursor = readableDatabase.query(
            NOTES_TABLE, arrayOf("id", "title", "content", "created_at"),
            null, null, null, null, "created_at DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(Note(it.getLong(0), it.getString(1), it.getString(2), it.getLong(3)))
            }
        }
        return list
    }

    fun deleteNote(id: Long) {
        writableDatabase.delete(NOTES_TABLE, "id = ?", arrayOf(id.toString()))
    }
}
