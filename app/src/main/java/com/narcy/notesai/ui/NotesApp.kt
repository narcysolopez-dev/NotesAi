package com.narcy.notesai.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
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
            onNew = {
                editing = Note(labelId = (section as? Section.Label)?.id)
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
    onNew: () -> Unit,
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

    fun select(target: Section) {
        viewModel.setSection(target)
        closeDrawer()
    }

    BackHandler(enabled = drawerState.isOpen) { closeDrawer() }
    BackHandler(enabled = !drawerState.isOpen && section != Section.Notes) {
        viewModel.setSection(Section.Notes)
    }

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
                    DrawerItem("Ayuda y comentarios", Icons.AutoMirrored.Outlined.HelpOutline, false) {
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
            onOpen = onOpen,
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
