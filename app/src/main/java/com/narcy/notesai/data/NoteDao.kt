package com.narcy.notesai.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note): Long

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes WHERE trashed = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM notes WHERE trashed = 1 AND updatedAt < :before")
    suspend fun purgeTrashedBefore(before: Long)

    @Query("SELECT mediaPath FROM notes WHERE trashed = 1 AND mediaPath IS NOT NULL")
    suspend fun trashedMedia(): List<String>

    @Query("SELECT mediaPath FROM notes WHERE trashed = 1 AND updatedAt < :before AND mediaPath IS NOT NULL")
    suspend fun trashedMediaBefore(before: Long): List<String>

    @Query("SELECT * FROM notes WHERE cloudId = :cloudId LIMIT 1")
    suspend fun byCloudId(cloudId: String): Note?

    @Query("SELECT cloudId FROM notes WHERE id = :id")
    suspend fun cloudIdOf(id: Long): String?

    @Query("SELECT * FROM labels WHERE id = :id")
    suspend fun labelById(id: Long): Label?

    /** Coincidencia exacta (distingue mayúsculas) para que la sincronización de etiquetas sea estable. */
    @Query("SELECT * FROM labels WHERE name = :name LIMIT 1")
    suspend fun labelByName(name: String): Label?

    @Query("SELECT * FROM labels ORDER BY name COLLATE NOCASE")
    fun observeLabels(): Flow<List<Label>>

    @Insert
    suspend fun insertLabel(label: Label): Long

    @Delete
    suspend fun deleteLabel(label: Label)

    @Query("UPDATE notes SET labelId = NULL WHERE labelId = :labelId")
    suspend fun clearLabel(labelId: Long)
}
