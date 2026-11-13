package com.narcy.notesai.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.narcy.notesai.data.Fmt

/** Color del texto sobre un resaltado (los resaltados son colores claros). */
private val TextOnHighlight = Color(0xFF202124)

fun spanStyleFor(fmt: Fmt): SpanStyle = SpanStyle(
    background = if (fmt.highlight != 0) Color(fmt.highlight) else Color.Unspecified,
    color = if (fmt.highlight != 0) TextOnHighlight else Color.Unspecified,
    textDecoration = if (fmt.underline) TextDecoration.Underline else null,
    fontFamily = noteFonts.getOrNull(fmt.font)?.family
)

fun buildRichText(text: String, styles: List<Fmt>): AnnotatedString = buildAnnotatedString {
    append(text)
    var i = 0
    while (i < text.length) {
        val fmt = styles.getOrNull(i) ?: Fmt()
        var j = i + 1
        while (j < text.length && (styles.getOrNull(j) ?: Fmt()) == fmt) j++
        if (!fmt.isDefault) addStyle(spanStyleFor(fmt), i, j)
        i = j
    }
}
