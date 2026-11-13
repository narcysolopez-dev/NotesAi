package com.narcy.notesai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.narcy.notesai.data.Fmt

/** Colores de resaltado (ARGB), de tonos claros para que el texto se lea encima. */
val highlightColors = listOf(
    0xFFFFD54F.toInt(), // amarillo
    0xFFFFAB91.toInt(), // naranja
    0xFFF48FB1.toInt(), // rosa
    0xFFCE93D8.toInt(), // lila
    0xFF80DEEA.toInt(), // celeste
    0xFFA5D6A7.toInt()  // verde
)

@Composable
fun RichTextField(
    state: RichTextState,
    readOnly: Boolean,
    fieldColors: TextFieldColors,
    modifier: Modifier = Modifier
) {
    val styles = state.styles
    val transformation = remember(styles) {
        object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText =
                TransformedText(buildRichText(text.text, styles), OffsetMapping.Identity)
        }
    }
    TextField(
        value = state.value,
        onValueChange = state::onValueChange,
        readOnly = readOnly,
        modifier = modifier,
        placeholder = { Text("Escribe una nota") },
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = transformation,
        colors = fieldColors
    )
}

private enum class Panel { None, Highlight }

/** Barra de formato del editor de texto. */
@Composable
fun RichTextToolbar(state: RichTextState, modifier: Modifier = Modifier) {
    var panel by remember { mutableStateOf(Panel.None) }

    fun toggle(target: Panel) {
        panel = if (panel == target) Panel.None else target
    }

    Column(modifier.fillMaxWidth()) {
        when (panel) {
            Panel.Highlight -> ColorRow(
                colors = highlightColors,
                selected = state.current.highlight.takeIf { it != 0 },
                resetLabel = "Sin resaltado",
                onPick = { color -> state.applyFormat { it.copy(highlight = color ?: 0) } }
            )

            Panel.None -> Unit
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FormatButton(
                icon = Icons.Filled.Highlight,
                label = "Resaltar",
                active = panel == Panel.Highlight || state.current.highlight != 0,
                onClick = { toggle(Panel.Highlight) }
            )
            val underlined = state.all { it.underline }
            FormatButton(
                icon = Icons.Filled.FormatUnderlined,
                label = "Subrayar",
                active = underlined,
                onClick = { state.applyFormat { it.copy(underline = !underlined) } }
            )
        }
    }
}

@Composable
private fun FormatButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            contentColor = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(icon, contentDescription = label)
    }
}

/** Fila de colores. [onPick] recibe null cuando se elige "quitar". */
@Composable
private fun ColorRow(
    colors: List<Int>,
    selected: Int?,
    resetLabel: String,
    onPick: (Int?) -> Unit
) {
    val outline = MaterialTheme.colorScheme.outline
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(if (selected == null) 2.dp else 1.dp, outline), CircleShape)
                    .clickable { onPick(null) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.FormatColorReset, contentDescription = resetLabel)
            }
        }
        items(colors) { color ->
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        BorderStroke(
                            if (color == selected) 3.dp else 1.dp,
                            if (color == selected) MaterialTheme.colorScheme.primary else outline
                        ),
                        CircleShape
                    )
                    .clickable { onPick(color) }
            )
        }
    }
}
