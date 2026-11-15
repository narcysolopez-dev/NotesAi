package com.narcy.notesai.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

class InkStroke(val color: Color, val width: Float) {
    val points = mutableStateListOf<Offset>()
}

/** Estado del lienzo. Vive en el editor para poder guardar el dibujo al cerrar. */
@Stable
class DrawingState {
    /** Dibujo guardado anteriormente (si la nota ya existía). */
    var base by mutableStateOf<ImageBitmap?>(null)
    var baseVisible by mutableStateOf(true)
    var size by mutableStateOf(IntSize.Zero)
    val strokes = mutableStateListOf<InkStroke>()

    /** Hay algo distinto a lo ya guardado. */
    val changed: Boolean get() = strokes.isNotEmpty() || !baseVisible

    /** El lienzo quedó sin nada. */
    val blank: Boolean get() = strokes.isEmpty() && (base == null || !baseVisible)

    fun clear() {
        strokes.clear()
        baseVisible = false
    }

    fun undo() {
        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
    }

    /** Dibuja todo sobre una hoja blanca y lo guarda como PNG. */
    fun renderTo(file: File): Boolean {
        val w = size.width
        val h = size.height
        if (w <= 0 || h <= 0) return false
        return runCatching {
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = AndroidCanvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            val baseBitmap = base
            if (baseBitmap != null && baseVisible) {
                canvas.drawBitmap(baseBitmap.asAndroidBitmap(), null, Rect(0, 0, w, h), null)
            }
            strokes.forEach { stroke ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stroke.color.toArgb()
                    strokeWidth = stroke.width
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    style = Paint.Style.STROKE
                }
                val pts = stroke.points.toList()
                if (pts.size == 1) {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(pts[0].x, pts[0].y, stroke.width / 2f, paint)
                } else if (pts.size > 1) {
                    val path = android.graphics.Path()
                    path.moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) {
                        val mid = Offset((pts[i - 1].x + pts[i].x) / 2f, (pts[i - 1].y + pts[i].y) / 2f)
                        path.quadTo(pts[i - 1].x, pts[i - 1].y, mid.x, mid.y)
                    }
                    path.lineTo(pts.last().x, pts.last().y)
                    canvas.drawPath(path, paint)
                }
            }
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            true
        }.getOrDefault(false)
    }
}

private class InkColor(val name: String, val color: Color)

/** Paleta de tonos vivos y saturados, más negro y blanco (el blanco funciona como borrador sobre la hoja blanca). */
private val inkColors = listOf(
    InkColor("Negro", Color(0xFF000000)),
    InkColor("Blanco", Color(0xFFFFFFFF)),
    InkColor("Rojo intenso", Color(0xFFE60000)),
    InkColor("Naranja neón", Color(0xFFFF6A00)),
    InkColor("Amarillo brillante", Color(0xFFFFEB00)),
    InkColor("Verde esmeralda", Color(0xFF00C37A)),
    InkColor("Cian", Color(0xFF00E5FF)),
    InkColor("Azul eléctrico", Color(0xFF0057FF)),
    InkColor("Morado vibrante", Color(0xFF9D00FF)),
    InkColor("Rosa fucsia", Color(0xFFFF00A8))
)

private const val MIN_PEN_DP = 1f
private const val MAX_PEN_DP = 40f
private const val DEFAULT_PEN_DP = 4f

/**
 * [textColor] es el color de texto de la nota (el mismo que usan los demás iconos del editor):
 * siempre contrasta con el fondo de la nota, tanto en modo claro como oscuro.
 */
@Composable
fun DrawingEditor(
    state: DrawingState,
    readOnly: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    var ink by remember { mutableStateOf(inkColors[0].color) }
    var penDp by remember { mutableFloatStateOf(DEFAULT_PEN_DP) }
    val strokeWidth = with(LocalDensity.current) { penDp.dp.toPx() }
    val outline = MaterialTheme.colorScheme.outline

    Box(modifier.padding(horizontal = 12.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, outline), RoundedCornerShape(16.dp))
                .onSizeChanged { state.size = it }
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (readOnly) {
                            Modifier
                        } else {
                            Modifier
                                .pointerInput(ink, strokeWidth) {
                                    detectTapGestures { offset ->
                                        val stroke = InkStroke(ink, strokeWidth)
                                        stroke.points.add(offset)
                                        state.strokes.add(stroke)
                                    }
                                }
                                .pointerInput(ink, strokeWidth) {
                                    var current: InkStroke? = null
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            current = InkStroke(ink, strokeWidth).also {
                                                it.points.add(offset)
                                                state.strokes.add(it)
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            current?.points?.add(change.position)
                                        },
                                        onDragEnd = { current = null },
                                        onDragCancel = { current = null }
                                    )
                                }
                        }
                    )
            ) {
                val base = state.base
                if (base != null && state.baseVisible) {
                    drawImage(
                        image = base,
                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                    )
                }
                state.strokes.forEach { stroke ->
                    val pts = stroke.points
                    if (pts.size == 1) {
                        drawCircle(stroke.color, radius = stroke.width / 2f, center = pts[0])
                    } else if (pts.size > 1) {
                        val path = Path()
                        path.moveTo(pts[0].x, pts[0].y)
                        for (i in 1 until pts.size) {
                            val mid = Offset((pts[i - 1].x + pts[i].x) / 2f, (pts[i - 1].y + pts[i].y) / 2f)
                            path.quadraticTo(pts[i - 1].x, pts[i - 1].y, mid.x, mid.y)
                        }
                        path.lineTo(pts.last().x, pts.last().y)
                        drawPath(
                            path,
                            color = stroke.color,
                            style = Stroke(
                                width = stroke.width,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }
    }

    if (!readOnly) {
        val buttonColors = IconButtonDefaults.iconButtonColors(
            contentColor = textColor,
            disabledContentColor = textColor.copy(alpha = 0.5f)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrushPreview(color = ink, widthDp = penDp, textColor = textColor)
                Slider(
                    value = penDp,
                    onValueChange = { penDp = it },
                    valueRange = MIN_PEN_DP..MAX_PEN_DP,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Grosor del lápiz" },
                    colors = SliderDefaults.colors(
                        thumbColor = textColor,
                        activeTrackColor = textColor,
                        inactiveTrackColor = textColor.copy(alpha = 0.25f)
                    )
                )
                IconButton(
                    onClick = { state.undo() },
                    enabled = state.strokes.isNotEmpty(),
                    colors = buttonColors
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer")
                }
                IconButton(
                    onClick = { state.clear() },
                    enabled = !state.blank,
                    colors = buttonColors
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Borrar todo")
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                inkColors.forEach { item ->
                    ColorSwatch(
                        item = item,
                        selected = item.color == ink,
                        textColor = textColor,
                        onClick = { ink = item.color }
                    )
                }
            }
        }
    }
}

/** Muestra el punto del pincel con el color y el grosor elegidos, antes de dibujar. */
@Composable
private fun BrushPreview(color: Color, widthDp: Float, textColor: Color) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(52.dp)
            .clip(shape)
            .background(Color.White)
            .border(BorderStroke(1.dp, textColor.copy(alpha = 0.35f)), shape)
            .semantics { contentDescription = "Vista previa del pincel" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = widthDp.dp.toPx() / 2f
            drawCircle(color, radius = radius)
            // Contorno fino para que también se vea el punto blanco.
            drawCircle(Color.Gray.copy(alpha = 0.55f), radius = radius, style = Stroke(width = 1.dp.toPx()))
        }
    }
}

@Composable
private fun ColorSwatch(item: InkColor, selected: Boolean, textColor: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(item.color)
            .border(
                BorderStroke(
                    if (selected) 3.dp else 1.dp,
                    if (selected) textColor else textColor.copy(alpha = 0.35f)
                ),
                CircleShape
            )
            .semantics { contentDescription = item.name }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = if (item.color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
