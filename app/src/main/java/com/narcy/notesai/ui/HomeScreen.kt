package com.narcy.notesai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.narcy.notesai.Section
import com.narcy.notesai.data.Label
import com.narcy.notesai.data.Note
import com.narcy.notesai.data.NoteType
import com.narcy.notesai.data.noteType
import com.narcy.notesai.data.parseChecklist
import com.narcy.notesai.ui.theme.noteColor
import com.narcy.notesai.ui.theme.noteTextColor

@Composable
fun HomeScreen(
    notes: List<Note>,
    labels: List<Label>,
    query: String,
    section: Section,
    searchHint: String,
    gridMode: Boolean,
    onQueryChange: (String) -> Unit,
    onMenu: () -> Unit,
    selectedIds: Set<Long>,
    selection: SelectionActions,
    onNoteClick: (Note) -> Unit,
    onNoteLongClick: (Note) -> Unit,
    onNew: (NoteType) -> Unit,
    onEmptyTrash: () -> Unit
) {
    val labelNames = remember(labels) { labels.associate { it.id to it.name } }
    val selected = notes.filter { it.id in selectedIds }
    var menuOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = menuOpen) { menuOpen = false }

    Scaffold(
        floatingActionButton = {
            if ((section is Section.Notes || section is Section.Label) && selected.isEmpty()) {
                NewNoteMenu(
                    expanded = menuOpen,
                    onToggle = { menuOpen = !menuOpen },
                    onPick = { type ->
                        menuOpen = false
                        onNew(type)
                    }
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selected.isNotEmpty()) {
                SelectionBar(selected, section, labels, selection)
            } else {
                SearchBar(query, searchHint, onMenu, onQueryChange)
            }

            if (notes.isEmpty()) {
                val emptyText = if (query.isNotBlank()) {
                    "Sin resultados"
                } else {
                    when (section) {
                        Section.Notes -> "Aún no tienes notas"
                        Section.Archive -> "Tus notas archivadas aparecen aquí"
                        Section.Trash -> "La papelera está vacía"
                        is Section.Label -> "No hay notas con esta etiqueta"
                    }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val pinned = notes.filter { it.pinned }
                val others = notes.filter { !it.pinned }

                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(if (gridMode) 2 else 1),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (section is Section.Trash) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Las notas se eliminan definitivamente después de 7 días.",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = onEmptyTrash) { Text("Vaciar") }
                            }
                        }
                    }
                    if (pinned.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) { SectionHeader("Fijadas") }
                        items(pinned, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                labelName = note.labelId?.let { labelNames[it] },
                                selected = note.id in selectedIds,
                                onClick = { onNoteClick(note) },
                                onLongClick = { onNoteLongClick(note) }
                            )
                        }
                    }
                    if (others.isNotEmpty()) {
                        if (pinned.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) { SectionHeader("Otras") }
                        }
                        items(others, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                labelName = note.labelId?.let { labelNames[it] },
                                selected = note.id in selectedIds,
                                onClick = { onNoteClick(note) },
                                onLongClick = { onNoteLongClick(note) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewNoteMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    onPick: (NoteType) -> Unit
) {
    val container = MaterialTheme.colorScheme.primaryContainer
    val content = MaterialTheme.colorScheme.onPrimaryContainer

    if (!expanded) {
        ExtendedFloatingActionButton(
            onClick = onToggle,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nueva nota") }
        )
        return
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NoteType.menu.forEach { type ->
            ExtendedFloatingActionButton(
                onClick = { onPick(type) },
                icon = { Icon(type.icon(), contentDescription = null) },
                text = { Text(type.label) },
                containerColor = container,
                contentColor = content
            )
        }
        FloatingActionButton(
            onClick = onToggle,
            containerColor = container,
            contentColor = content
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Cerrar menú")
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    hint: String,
    onMenu: () -> Unit,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = CircleShape,
        tonalElevation = 3.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(hint) },
            leadingIcon = {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menú")
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: Note,
    labelName: String?,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val textColor = noteTextColor()
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = Modifier
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = noteColor(note.colorIndex)),
        border = if (selected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(Modifier.padding(14.dp)) {
            if (note.pinned) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = "Fijada",
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            if (note.mediaPath != null && (note.noteType == NoteType.Drawing || note.noteType == NoteType.Image)) {
                val bitmap by rememberNoteBitmap(note.mediaPath, 700)
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            }
            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (note.noteType == NoteType.Checklist) {
                val items = remember(note.content) { parseChecklist(note.content) }
                items.take(8).forEach { item ->
                    Text(
                        text = (if (item.checked) "☑  " else "☐  ") + item.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.checked) textColor.copy(alpha = 0.55f) else textColor,
                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (items.size > 8) {
                    Text(
                        text = "+ ${items.size - 8} más",
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = if (note.title.isNotBlank()) 6.dp else 0.dp)
                )
            }
            if (labelName != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = textColor.copy(alpha = 0.1f),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Text(
                        text = labelName,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
