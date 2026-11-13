package com.narcy.notesai.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
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

    /**
     * Copia una imagen elegida por la persona a la carpeta de la app, reducida
     * a un máximo de 2048 px y con la rotación de la cámara corregida.
     * Devuelve el nombre del archivo, o null si no se pudo leer.
     */
    fun importImage(context: Context, uri: Uri): String? = runCatching {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_IMAGE_SIZE) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("No se pudo leer la imagen")

        val orientation = runCatching {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        val result = if (matrix.isIdentity) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        val name = newName("jpg")
        FileOutputStream(file(context, name)).use { result.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        name
    }.getOrNull()

    private const val MAX_IMAGE_SIZE = 2048
}
