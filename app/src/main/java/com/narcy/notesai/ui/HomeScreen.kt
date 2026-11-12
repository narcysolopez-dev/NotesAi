package com.narcy.notesai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.narcy.notesai.Section
import com.narcy.notesai.data.Label
import com.narcy.notesai.data.Note
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
    onOpen: (Note) -> Unit,
    onNew: () -> Unit,
    onEmptyTrash: () -> Unit
) {
    val labelNames = remember(labels) { labels.associate { it.id to it.name } }

    Scaffold(
        floatingActionButton = {
            if (section is Section.Notes || section is Section.Label) {
                ExtendedFloatingActionButton(
                    onClick = onNew,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nueva nota") }
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SearchBar(query, searchHint, onMenu, onQueryChange)

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
                        items(pinned, key = { it.id }) {
                            NoteCard(it, labelNames[it.labelId]) { onOpen(it) }
                        }
                    }
                    if (others.isNotEmpty()) {
                        if (pinned.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) { SectionHeader("Otras") }
                        }
                        items(others, key = { it.id }) {
                            NoteCard(it, labelNames[it.labelId]) { onOpen(it) }
                        }
                    }
                }
            }
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

@Composable
private fun NoteCard(note: Note, labelName: String?, onClick: () -> Unit) {
    val textColor = noteTextColor()
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = noteColor(note.colorIndex)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
            if (note.content.isNotBlank()) {
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
