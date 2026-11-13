package com.narcy.notesai.data

/** Tipos de nota. [key] es lo que se guarda en la base de datos. */
enum class NoteType(val key: String, val label: String) {
    Audio("audio", "Audio"),
    Image("image", "Imagen"),
    Drawing("drawing", "Dibujo"),
    Checklist("list", "Lista"),
    Text("text", "Texto");

    companion object {
        fun fromKey(key: String): NoteType = entries.firstOrNull { it.key == key } ?: Text

        /** Opciones que aparecen en el menú "Nueva nota", de arriba hacia abajo. */
        val menu: List<NoteType> = listOf(Checklist, Text)
    }
}

val Note.noteType: NoteType get() = NoteType.fromKey(type)
