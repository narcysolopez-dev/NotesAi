package com.narcy.notesai.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val colorIndex: Int = 0,
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
    @ColumnInfo(defaultValue = "0") val trashed: Boolean = false,
    val labelId: Long? = null,
    val type: String = "text",
    /** Nombre del archivo (en filesDir/media) de la imagen, dibujo o audio de la nota. */
    val mediaPath: String? = null,
    /** Formato del texto (resaltado, subrayado, fuente, color) en JSON. Ver RichText.kt. */
    val spans: String? = null
)
