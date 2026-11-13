package com.narcy.notesai.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Formato de un carácter del texto de una nota.
 * Los valores 0 / false significan "sin formato".
 */
data class Fmt(
    /** Color de resaltado (ARGB). 0 = sin resaltado. */
    val highlight: Int = 0,
    val underline: Boolean = false,
    /** Posición en la lista de fuentes (ui/Fonts.kt). 0 = predeterminada. */
    val font: Int = 0,
    /** Color del texto (ARGB). 0 = el color normal de la nota. */
    val color: Int = 0
) {
    val isDefault: Boolean get() = this == Fmt()
}

/** Lee el formato guardado en la nota. Siempre devuelve una lista de [length] elementos. */
fun parseSpans(json: String?, length: Int): List<Fmt> {
    val result = MutableList(length) { Fmt() }
    if (json.isNullOrBlank() || length == 0) return result
    runCatching {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val start = o.getInt("s").coerceIn(0, length)
            val end = o.getInt("e").coerceIn(start, length)
            val fmt = Fmt(
                highlight = o.optInt("h", 0),
                underline = o.optInt("u", 0) == 1,
                font = o.optInt("f", 0),
                color = o.optInt("c", 0)
            )
            for (k in start until end) result[k] = fmt
        }
    }
    return result
}

/** Guarda el formato como tramos consecutivos. Devuelve null si no hay formato alguno. */
fun serializeSpans(styles: List<Fmt>): String? {
    val array = JSONArray()
    var i = 0
    while (i < styles.size) {
        val fmt = styles[i]
        var j = i + 1
        while (j < styles.size && styles[j] == fmt) j++
        if (!fmt.isDefault) {
            array.put(
                JSONObject().apply {
                    put("s", i)
                    put("e", j)
                    if (fmt.highlight != 0) put("h", fmt.highlight)
                    if (fmt.underline) put("u", 1)
                    if (fmt.font != 0) put("f", fmt.font)
                    if (fmt.color != 0) put("c", fmt.color)
                }
            )
        }
        i = j
    }
    return if (array.length() == 0) null else array.toString()
}
