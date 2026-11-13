package com.narcy.notesai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun ImageEditor(
    mediaPath: String?,
    readOnly: Boolean,
    onPick: () -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    textColor: Color,
    fieldColors: TextFieldColors
) {
    if (mediaPath != null) {
        val bitmap by rememberNoteBitmap(mediaPath, 1600)
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "Imagen de la nota",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
    } else {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Image, contentDescription = null, tint = textColor.copy(alpha = 0.4f))
        }
    }

    if (!readOnly) {
        OutlinedButton(
            onClick = onPick,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Outlined.Image, contentDescription = null)
            Text(
                text = if (mediaPath == null) "  Agregar imagen" else "  Cambiar imagen",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    TextField(
        value = content,
        onValueChange = onContentChange,
        readOnly = readOnly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        placeholder = { Text("Agrega una descripción") },
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = fieldColors
    )
}
