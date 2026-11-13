package com.narcy.notesai.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.narcy.notesai.data.Fmt
import com.narcy.notesai.data.parseSpans
import com.narcy.notesai.data.serializeSpans

/**
 * Texto con formato por carácter. [styles] siempre tiene la misma longitud que el texto.
 * Al escribir, los caracteres nuevos reciben [typing]; al mover el cursor, [typing]
 * pasa a ser el formato del carácter anterior.
 */
@Stable
class RichTextState(text: String, spans: String?) {
    var value by mutableStateOf(TextFieldValue(text))
        private set
    var styles by mutableStateOf(parseSpans(spans, text.length))
        private set
    var typing by mutableStateOf(Fmt())
        private set

    fun spansJson(): String? = serializeSpans(styles)

    fun onValueChange(new: TextFieldValue) {
        val old = value
        if (new.text != old.text) {
            applyTextChange(old, new)
        } else if (new.selection != old.selection && new.selection.collapsed) {
            typing = styles.getOrNull((new.selection.start - 1).coerceAtLeast(0)) ?: Fmt()
        }
        value = new
    }

    private fun applyTextChange(old: TextFieldValue, new: TextFieldValue) {
        val oldText = old.text
        val newText = new.text
        var start: Int
        var removed: Int
        var inserted: Int

        // Primero se deduce el cambio a partir del cursor/selección; si no cuadra, se compara el texto.
        val sel = old.selection
        if (!sel.collapsed) {
            start = sel.min
            removed = sel.length
            inserted = newText.length - (oldText.length - removed)
        } else if (newText.length >= oldText.length) {
            start = sel.start
            removed = 0
            inserted = newText.length - oldText.length
        } else {
            removed = oldText.length - newText.length
            start = new.selection.start
            inserted = 0
        }
        val consistent = start >= 0 && removed >= 0 && inserted >= 0 &&
            start + removed <= oldText.length && start + inserted <= newText.length &&
            oldText.regionMatches(0, newText, 0, start) &&
            oldText.regionMatches(start + removed, newText, start + inserted, oldText.length - start - removed)
        if (!consistent) {
            var p = 0
            val limit = minOf(oldText.length, newText.length)
            while (p < limit && oldText[p] == newText[p]) p++
            var s = 0
            while (s < limit - p && oldText[oldText.length - 1 - s] == newText[newText.length - 1 - s]) s++
            start = p
            removed = oldText.length - p - s
            inserted = newText.length - p - s
        }

        val list = styles.toMutableList()
        if (removed > 0) list.subList(start, start + removed).clear()
        if (inserted > 0) list.addAll(start, List(inserted) { typing })
        styles = list
        if (inserted == 0) typing = list.getOrNull((start - 1).coerceAtLeast(0)) ?: typing
    }

    /** Aplica [transform] al texto seleccionado o, si solo hay cursor, al texto que se escriba a continuación. */
    fun applyFormat(transform: (Fmt) -> Fmt) {
        val sel = value.selection
        if (sel.collapsed) {
            typing = transform(typing)
        } else {
            styles = styles.mapIndexed { i, f -> if (i >= sel.min && i < sel.max) transform(f) else f }
        }
    }

    /** Formato "actual" para mostrar el estado de la barra de herramientas. */
    val current: Fmt
        get() {
            val sel = value.selection
            return if (sel.collapsed) typing else styles.getOrNull(sel.min) ?: Fmt()
        }

    /** Verdadero si todo el texto seleccionado (o lo que se escriba) cumple [predicate]. */
    fun all(predicate: (Fmt) -> Boolean): Boolean {
        val sel = value.selection
        if (sel.collapsed) return predicate(typing)
        return (sel.min until sel.max).all { predicate(styles.getOrNull(it) ?: Fmt()) }
    }
}
