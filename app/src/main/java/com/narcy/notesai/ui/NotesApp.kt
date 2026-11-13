package com.narcy.notesai.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.Label as LabelIcon
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.narcy.notesai.NotesViewModel
import com.narcy.notesai.Section
import com.narcy.notesai.data.Label
import com.narcy.notesai.data.Note
import com.narcy.notesai.data.NoteType
import kotlinx.coroutines.launch

private enum class Page { Home, Settings, Help }

@Composable
fun NotesApp(viewModel: NotesViewModel) {
    val notes by viewModel.notes.collectAsState()
    val query by viewModel.query.collectAsState()
    val section by viewModel.section.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var editing by remember { mutableStateOf<Note?>(null) }
    var page by remember { mutableStateOf(Page.Home) }

    val current = editing
    when {
        current != null -> EditorScreen(
            initial = current,
            labels = labels,
            onSave = viewModel::save,
            onTrash = viewModel::trash,
            onRestore = viewModel::restore,
            onDeleteForever = viewModel::deleteForever,
            onClose = { editing = null }
        )

        page == Page.Settings -> SettingsScreen(
            settings = settings,
            onChange = viewModel::updateSettings,
            onBack = { page = Page.Home }
        )

        page == Page.Help -> HelpScreen(onBack = { page = Page.Home })

        else -> DrawerHome(
            viewModel = viewModel,
            notes = notes,
            query = query,
            section = section,
            labels = labels,
            gridMode = settings.grid,
            onOpen = { editing = it },
            onNew = { type ->
                editing = Note(
                    type = type.key,
                    labelId = (section as? Section.Label)?.id
                )
            },
            onSettings = { page = Page.Settings },
            onHelp = { page = Page.Help }
        )
    }
}

@Composable
private fun DrawerHome(
    viewModel: NotesViewModel,
    notes: List<Note>,
    query: String,
    section: Section,
    labels: List<Label>,
    gridMode: Boolean,
    onOpen: (Note) -> Unit,
    onNew: (NoteType) -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showNewLabel by remember { mutableStateOf(false) }
    var labelToDelete by remember { mutableStateOf<Label?>(null) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }

    fun closeDrawer() {
        scope.launch { drawerState.close() }
    }

    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val selected = notes.filter { it.id in selectedIds }
    val context = LocalContext.current

    fun clearSelection() {
        selectedIds = emptySet()
    }

    fun toggle(note: Note) {
        selectedIds = if (note.id in selectedIds) selectedIds - note.id else selectedIds + note.id
    }

    fun select(target: Section) {
        clearSelection()
        viewModel.setSection(target)
        closeDrawer()
    }

    val selection = SelectionActions(
        onClear = { clearSelection() },
        onTogglePin = {
            val pin = !selected.all { it.pinned }
            viewModel.update(selected) {
                it.copy(pinned = pin, archived = if (pin) false else it.archived)
            }
            clearSelection()
        },
        onColor = { color ->
            viewModel.update(selected) { it.copy(colorIndex = color) }
            clearSelection()
        },
        onLabel = { labelId ->
            viewModel.update(selected) { it.copy(labelId = labelId) }
            clearSelection()
        },
        onArchive = {
            val archive = section != Section.Archive
            viewModel.update(selected) {
                it.copy(archived = archive, pinned = if (archive) false else it.pinned)
            }
            clearSelection()
        },
        onTrash = {
            viewModel.update(selected) { it.copy(trashed = true, pinned = false) }
            clearSelection()
        },
        onRestore = {
            viewModel.update(selected) { it.copy(trashed = false) }
            clearSelection()
        },
        onDeleteForever = {
            viewModel.deleteAllForever(selected)
            clearSelection()
        },
        onDuplicate = {
            viewModel.duplicate(selected)
            clearSelection()
        },
        onShare = {
            shareNotes(context, selected)
            clearSelection()
        }
    )

    BackHandler(enabled = drawerState.isOpen) { closeDrawer() }
    BackHandler(enabled = !drawerState.isOpen && section != Section.Notes) {
        viewModel.setSection(Section.Notes)
    }
    BackHandler(enabled = selected.isNotEmpty()) { clearSelection() }

    val searchHint = when (section) {
        Section.Notes -> "Buscar en tus notas"
        Section.Archive -> "Buscar en el archivo"
        Section.Trash -> "Buscar en la papelera"
        is Section.Label -> labels.firstOrNull { it.id == section.id }?.name ?: "Etiqueta"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "NotesAi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
                    )

                    DrawerItem("Notas", Icons.Outlined.Lightbulb, section == Section.Notes) {
                        select(Section.Notes)
                    }

                    labels.forEach { label ->
                        NavigationDrawerItem(
                            label = { Text(label.name) },
                            icon = { Icon(Icons.AutoMirrored.Outlined.LabelIcon, contentDescription = null) },
                            selected = section == Section.Label(label.id),
                            onClick = { select(Section.Label(label.id)) },
                            badge = {
                                IconButton(onClick = { labelToDelete = label }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar etiqueta")
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    DrawerItem("Crear etiqueta nueva", Icons.Filled.Add, false) {
                        showNewLabel = true
                    }

                    DrawerItem("Archivar", Icons.Outlined.Archive, section == Section.Archive) {
                        select(Section.Archive)
                    }
                    DrawerItem("Papelera", Icons.Outlined.Delete, section == Section.Trash) {
                        select(Section.Trash)
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 28.dp))

                    DrawerItem("Configuración", Icons.Outlined.Settings, false) {
                        closeDrawer()
                        onSettings()
                    }
                    DrawerItem("Ayuda y comentarios", Icons.Outlined.Info, false) {
                        closeDrawer()
                        onHelp()
                    }
                }
            }
        }
    ) {
        HomeScreen(
            notes = notes,
            labels = labels,
            query = query,
            section = section,
            searchHint = searchHint,
            gridMode = gridMode,
            onQueryChange = viewModel::setQuery,
            onMenu = { scope.launch { drawerState.open() } },
            selectedIds = selectedIds,
            selection = selection,
            onNoteClick = { note -> if (selected.isNotEmpty()) toggle(note) else onOpen(note) },
            onNoteLongClick = { note -> toggle(note) },
            onNew = onNew,
            onEmptyTrash = { confirmEmptyTrash = true }
        )
    }

    if (showNewLabel) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewLabel = false },
            title = { Text("Crear etiqueta nueva") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("Nombre de la etiqueta") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        viewModel.addLabel(name)
                        showNewLabel = false
                    }
                ) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showNewLabel = false }) { Text("Cancelar") }
            }
        )
    }

    labelToDelete?.let { label ->
        AlertDialog(
            onDismissRequest = { labelToDelete = null },
            title = { Text("¿Eliminar etiqueta?") },
            text = { Text("Se eliminará \"${label.name}\". Las notas no se borrarán.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLabel(label)
                    labelToDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { labelToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    if (confirmEmptyTrash) {
        AlertDialog(
            onDismissRequest = { confirmEmptyTrash = false },
            title = { Text("¿Vaciar la papelera?") },
            text = { Text("Todas las notas de la papelera se eliminarán definitivamente.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    confirmEmptyTrash = false
                }) { Text("Vaciar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmptyTrash = false }) { Text("Cancelar") }
            }
        )
    }
}

private fun shareNotes(context: Context, notes: List<Note>) {
    if (notes.isEmpty()) return
    val text = notes.joinToString("\n\n") { note ->
        listOf(note.title, note.content).filter { it.isNotBlank() }.joinToString("\n")
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        notes.singleOrNull()?.title?.takeIf { it.isNotBlank() }?.let {
            putExtra(Intent.EXTRA_SUBJECT, it)
        }
    }
    context.startActivity(Intent.createChooser(send, "Enviar nota"))
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
