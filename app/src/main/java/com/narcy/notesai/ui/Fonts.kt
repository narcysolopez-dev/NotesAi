package com.narcy.notesai.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.narcy.notesai.R

class NoteFont(val label: String, val family: FontFamily?)

/**
 * Fuentes disponibles para el texto de las notas.
 * IMPORTANTE: en las notas se guarda la posición de la fuente en esta lista,
 * así que las nuevas fuentes se agregan al final y no se reordenan.
 */
val noteFonts: List<NoteFont> = listOf(
    NoteFont("Predeterminada", null),
    NoteFont("Serif", FontFamily.Serif),
    NoteFont("Monoespaciada", FontFamily.Monospace),
    NoteFont("Cursiva", FontFamily.Cursive),
    NoteFont("Alfa Slab", FontFamily(Font(R.font.alfa_slab_one))),
    NoteFont("Pacifico", FontFamily(Font(R.font.pacifico))),
    NoteFont("Lobster", FontFamily(Font(R.font.lobster))),
    NoteFont("Courgette", FontFamily(Font(R.font.courgette)))
)
