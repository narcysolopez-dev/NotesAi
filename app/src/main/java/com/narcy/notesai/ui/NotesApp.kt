package com.narcy.notesai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.narcy.notesai.NotesViewModel
import com.narcy.notesai.data.Note

@Composable
fun NotesApp(viewModel: NotesViewModel) {
    val notes by viewModel.notes.collectAsState()
    val query by viewModel.query.collectAsState()
    var editing by remember { mutableStateOf<Note?>(null) }

    val current = editing
    if (current != null) {
        EditorScreen(
            initial = current,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onClose = { editing = null }
        )
    } else {
        HomeScreen(
            notes = notes,
            query = query,
            onQueryChange = viewModel::setQuery,
            onOpen = { editing = it },
            onNew = { editing = Note() }
        )
    }
}
