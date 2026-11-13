package com.narcy.notesai.ui

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.narcy.notesai.data.MediaFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Carga (reducida a [maxSize] px) la imagen guardada de una nota. */
@Composable
fun rememberNoteBitmap(name: String?, maxSize: Int): State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, name, maxSize) {
        value = if (name == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val path = MediaFiles.file(context, name).absolutePath
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(path, bounds)
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSize) {
                        sample *= 2
                    }
                    val options = BitmapFactory.Options().apply { inSampleSize = sample }
                    BitmapFactory.decodeFile(path, options)?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
}
