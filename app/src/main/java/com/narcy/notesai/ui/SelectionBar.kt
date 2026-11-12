package com.narcy.notesai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label as LabelIcon
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.narcy.notesai.Section
import com.narcy.notesai.data.Label
import com.narcy.notesai.data.Note
import com.narcy.notesai.ui.theme.noteColor
import com.narcy.notesai.ui.theme.noteColorCount
import com.narcy.notesai.ui.theme.noteTextColor

class SelectionActions(
    val onClear: () -> Unit,
    val onTogglePin: () -> Unit,
    val onColor: (Int) -> Unit,
    val onLabel: (Long?) -> Unit,
    val onArchive: () -> Unit,
    val onTrash: () -> Unit,
    val onRestore: () -> Unit,
    val onDeleteForever: () -> Unit,
    val onDuplicate: () -> Unit,
    val onShare: () -> Unit
)

@Composable
fun SelectionBar(
    selected: List<Note>,
    section: Section,
    labels: List<Label>,
    actions: SelectionActions
) {
    val inTrash = section is Section.Trash
    val allPinned = selected.isNotEmpty() && selected.all { it.pinned }

    var menuOpen by remember { mutableStateOf(false) }
    var showColors by remember { mutableStateOf(false) }
    var showLabels by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = actions.onClear) {
                Icon(Icons.Filled.Close, contentDescription = "Cancelar selección")
            }
            Text(
                text = selected.size.toString(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )

            if (!inTrash) {
                IconButton(onClick = actions.onTogglePin) {
                    Icon(
                        if (allPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (allPinned) "Dejar de fijar" else "Fijar"
                    )
                }
                IconButton(onClick = { showColors = true }) {
                    Icon(Icons.Outlined.Palette, contentDescription = "Color")
                }
                IconButton(onClick = { showLabels = true }) {
                    Icon(Icons.AutoMirrored.Outlined.LabelIcon, contentDescription = "Etiqueta")
                }
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (inTrash) {
                        DropdownMenuItem(
                            text = { Text("Restaurar") },
                            onClick = {
                                menuOpen = false
                                actions.onRestore()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar definitivamente") },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(if (section is Section.Archive) "Desarchivar" else "Archivar") },
                            onClick = {
                                menuOpen = false
                                actions.onArchive()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Borrar") },
                            onClick = {
                                menuOpen = false
                                actions.onTrash()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Crear una copia") },
                            onClick = {
                                menuOpen = false
                                actions.onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Enviar") },
                            onClick = {
                                menuOpen = false
                                actions.onShare()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showColors) {
        val current = selected.map { it.colorIndex }.distinct().singleOrNull()
        AlertDialog(
            onDismissRequest = { showColors = false },
            title = { Text("Color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    List(noteColorCount) { it }.chunked(5).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { index ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(noteColor(index))
                                        .border(
                                            BorderStroke(
                                                if (index == current) 2.dp else 1.dp,
                                                MaterialTheme.colorScheme.outline
                                            ),
                                            CircleShape
                                        )
                                        .clickable {
                                            showColors = false
                                            actions.onColor(index)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (index == current) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Seleccionado",
                                            tint = noteTextColor()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showColors = false }) { Text("Cancelar") }
            }
        )
    }

    if (showLabels) {
        val currentLabel = selected.map { it.labelId }.distinct().singleOrNull()
        AlertDialog(
            onDismissRequest = { showLabels = false },
            title = { Text("Etiqueta") },
            text = {
                Column {
                    LabelRow("Sin etiqueta", currentLabel == null) {
                        showLabels = false
                        actions.onLabel(null)
                    }
                    labels.forEach { label ->
                        LabelRow(label.name, currentLabel == label.id) {
                            showLabels = false
                            actions.onLabel(label.id)
                        }
                    }
                    if (labels.isEmpty()) {
                        Text(
                            text = "Crea etiquetas desde el menú ☰.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLabels = false }) { Text("Cancelar") }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar definitivamente?") },
            text = { Text("Las notas seleccionadas se eliminarán y no se podrán recuperar.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.onDeleteForever()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun LabelRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(24.dp)) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = "Seleccionada")
        }
        Text(name, modifier = Modifier.padding(start = 12.dp))
    }
}
