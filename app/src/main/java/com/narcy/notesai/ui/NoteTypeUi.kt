package com.narcy.notesai.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.Image
import androidx.compose.ui.graphics.vector.ImageVector
import com.narcy.notesai.data.NoteType

fun NoteType.icon(): ImageVector = when (this) {
    NoteType.Audio -> Icons.Filled.Mic
    NoteType.Image -> Icons.Outlined.Image
    NoteType.Drawing -> Icons.Filled.Brush
    NoteType.Checklist -> Icons.Filled.CheckBox
    NoteType.Text -> Icons.Filled.TextFields
}
