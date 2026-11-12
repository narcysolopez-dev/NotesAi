package com.narcy.notesai

import com.narcy.notesai.data.Note

sealed interface Section {
    fun matches(note: Note): Boolean

    data object Notes : Section {
        override fun matches(note: Note) = !note.trashed && !note.archived
    }

    data object Archive : Section {
        override fun matches(note: Note) = !note.trashed && note.archived
    }

    data object Trash : Section {
        override fun matches(note: Note) = note.trashed
    }

    data class Label(val id: Long) : Section {
        override fun matches(note: Note) = !note.trashed && !note.archived && note.labelId == id
    }
}
