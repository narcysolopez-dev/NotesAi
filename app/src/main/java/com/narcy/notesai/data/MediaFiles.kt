package com.narcy.notesai.data

import android.content.Context
import java.io.File
import java.util.UUID

/** Archivos (dibujos, imágenes, audios) de las notas. Se guardan en filesDir/media. */
object MediaFiles {
    private fun dir(context: Context): File =
        File(context.applicationContext.filesDir, "media").apply { mkdirs() }

    /** Solo se usa el nombre del archivo, nunca rutas, para no salir de la carpeta. */
    fun file(context: Context, name: String): File = File(dir(context), File(name).name)

    fun newName(extension: String): String = "${UUID.randomUUID()}.$extension"

    fun delete(context: Context, name: String?) {
        if (name.isNullOrBlank()) return
        runCatching { file(context, name).delete() }
    }

    /** Copia el archivo para que una nota duplicada tenga el suyo propio. */
    fun copy(context: Context, name: String?): String? {
        if (name.isNullOrBlank()) return null
        return runCatching {
            val source = file(context, name)
            if (!source.exists()) return null
            val newName = newName(source.extension.ifBlank { "bin" })
            source.copyTo(file(context, newName))
            newName
        }.getOrNull()
    }
}
