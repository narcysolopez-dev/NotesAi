package com.narcy.notesai.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label as LabelIcon
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.narcy.notesai.data.CheckItem
import com.narcy.notesai.data.Label
import com.narcy.notesai.data.MediaFiles
import com.narcy.notesai.data.Note
import com.narcy.notesai.data.NoteType
import com.narcy.notesai.data.noteType
import com.narcy.notesai.data.parseChecklist
import com.narcy.notesai.data.serializeChecklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.narcy.notesai.ui.theme.noteColor
import com.narcy.notesai.ui.theme.noteColorCount
import com.narcy.notesai.ui.theme.noteTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initial: Note,
    labels: List<Label>,
    onSave: (Note) -> Unit,
    onTrash: (Note) -> Unit,
    onRestore: (Note) -> Unit,
    onDeleteForever: (Note) -> Unit,
    onClose: () -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var content by remember { mutableStateOf(initial.content) }
    var colorIndex by remember { mutableIntStateOf(initial.colorIndex) }
    var pinned by remember { mutableStateOf(initial.pinned) }
    var archived by remember { mutableStateOf(initial.archived) }
    var labelId by remember { mutableStateOf(initial.labelId) }
    var labelMenu by remember { mutableStateOf(false) }
    var mediaPath by remember { mutableStateOf(initial.mediaPath) }

    val readOnly = initial.trashed
    val type = initial.noteType

    val checklist = remember {
        mutableStateListOf<CheckItem>().apply {
            addAll(parseChecklist(initial.content))
            if (isEmpty()) add(CheckItem(""))
        }
    }

    fun current(): Note = initial.copy(
        title = title,
        content = if (type == NoteType.Checklist) serializeChecklist(checklist) else content,
        colorIndex = colorIndex,
        pinned = pinned,
        archived = archived,
        labelId = labelId,
        mediaPath = mediaPath
    )

    val context = LocalContext.current

    val drawing = remember { DrawingState() }
    if (type == NoteType.Drawing) {
        val base by rememberNoteBitmap(initial.mediaPath, 2048)
        SideEffect { if (drawing.base == null) drawing.base = base }
    }

    val scope = rememberCoroutineScope()

    /** Reemplaza el archivo de imagen/audio de la nota, borrando el que había creado en esta sesión. */
    fun replaceMedia(name: String) {
        val previous = mediaPath
        if (previous != null && previous != initial.mediaPath) MediaFiles.delete(context, previous)
        mediaPath = name
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val name = withContext(Dispatchers.IO) { MediaFiles.importImage(context, uri) }
                if (name != null) replaceMedia(name)
            }
        }
    }
    fun pickImage() {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    if (type == NoteType.Image) {
        LaunchedEffect(Unit) {
            if (initial.mediaPath == null && !readOnly) pickImage()
        }
    }

    val recorder = remember { AudioRecorder(context) }
    val player = remember { AudioPlayer(context) }
    var micDenied by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            recorder.release()
            player.release()
        }
    }

    fun startRecording() {
        player.stop()
        micDenied = !recorder.start()
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else micDenied = true
    }
    fun requestRecording() {
        micDenied = false
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    fun stopRecording() {
        recorder.stop()?.let { replaceMedia(it) }
    }

    /** Guarda en disco lo que haya en el lienzo y devuelve la nota lista para guardar. */
    fun persisted(): Note {
        if (recorder.recording) stopRecording()
        val note = current()
        if (!readOnly && (type == NoteType.Image || type == NoteType.Audio) && mediaPath != initial.mediaPath) {
            MediaFiles.delete(context, initial.mediaPath)
        }
        if (readOnly || type != NoteType.Drawing || !drawing.changed) return note
        if (drawing.blank) {
            MediaFiles.delete(context, initial.mediaPath)
            return note.copy(mediaPath = null)
        }
        val name = MediaFiles.newName("png")
        return if (drawing.renderTo(MediaFiles.file(context, name))) {
            MediaFiles.delete(context, initial.mediaPath)
            note.copy(mediaPath = name)
        } else {
            note
        }
    }

    fun saveAndClose() {
        if (!readOnly) onSave(persisted())
        onClose()
    }

    BackHandler { saveAndClose() }

    val background = noteColor(colorIndex)
    val textColor = noteTextColor()

    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        cursorColor = textColor
    )

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = { saveAndClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = textColor)
                    }
                },
                actions = {
                    if (readOnly) {
                        IconButton(onClick = {
                            onRestore(initial)
                            onClose()
                        }) {
                            Icon(Icons.Filled.RestoreFromTrash, contentDescription = "Restaurar", tint = textColor)
                        }
                        IconButton(onClick = {
                            onDeleteForever(initial)
                            onClose()
                        }) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = "Eliminar definitivamente", tint = textColor)
                        }
                    } else {
                        IconButton(onClick = {
                            pinned = !pinned
                            if (pinned) archived = false
                        }) {
                            Icon(
                                if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Fijar",
                                tint = textColor
                            )
                        }
                        IconButton(onClick = {
                            val nowArchived = !archived
                            onSave(persisted().copy(archived = nowArchived, pinned = if (nowArchived) false else pinned))
                            onClose()
                        }) {
                            Icon(
                                if (archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                contentDescription = if (archived) "Desarchivar" else "Archivar",
                                tint = textColor
                            )
                        }
                        Box {
                            IconButton(onClick = { labelMenu = true }) {
                                Icon(Icons.AutoMirrored.Outlined.LabelIcon, contentDescription = "Etiqueta", tint = textColor)
                            }
                            DropdownMenu(expanded = labelMenu, onDismissRequest = { labelMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Sin etiqueta") },
                                    leadingIcon = { if (labelId == null) Icon(Icons.Filled.Check, contentDescription = null) },
                                    onClick = {
                                        labelId = null
                                        labelMenu = false
                                    }
                                )
                                labels.forEach { label ->
                                    DropdownMenuItem(
                                        text = { Text(label.name) },
                                        leadingIcon = { if (labelId == label.id) Icon(Icons.Filled.Check, contentDescription = null) },
                                        onClick = {
                                            labelId = label.id
                                            labelMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = {
                            onTrash(persisted())
                            onClose()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Enviar a la papelera", tint = textColor)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!readOnly) {
                ColorPicker(
                    selected = colorIndex,
                    onSelect = { colorIndex = it },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .let { if (type == NoteType.Drawing) it else it.verticalScroll(rememberScrollState()) }
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                readOnly = readOnly,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Título", style = MaterialTheme.typography.titleLarge) },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                colors = fieldColors
            )
            when (type) {
                NoteType.Drawing -> DrawingEditor(
                    state = drawing,
                    readOnly = readOnly,
                    modifier = Modifier.weight(1f)
                )

                NoteType.Audio -> AudioEditor(
                    recorder = recorder,
                    player = player,
                    mediaPath = mediaPath,
                    readOnly = readOnly,
                    micDenied = micDenied,
                    onRecord = { requestRecording() },
                    onStop = { stopRecording() },
                    content = content,
                    onContentChange = { content = it },
                    textColor = textColor,
                    fieldColors = fieldColors
                )

                NoteType.Image -> ImageEditor(
                    mediaPath = mediaPath,
                    readOnly = readOnly,
                    onPick = { pickImage() },
                    content = content,
                    onContentChange = { content = it },
                    textColor = textColor,
                    fieldColors = fieldColors
                )

                NoteType.Checklist -> ChecklistEditor(
                    items = checklist,
                    readOnly = readOnly,
                    textColor = textColor,
                    fieldColors = fieldColors,
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                else -> TextField(
                    value = content,
                    onValueChange = { content = it },
                    readOnly = readOnly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    placeholder = { Text("Escribe una nota") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = fieldColors
                )
            }
        }
    }
}

@Composable
private fun ColorPicker(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outline
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(List(noteColorCount) { it }) { _, index ->
            val color = noteColor(index)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        BorderStroke(if (index == selected) 2.dp else 1.dp, outline),
                        CircleShape
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                if (index == selected) {
                    Icon(Icons.Filled.Check, contentDescription = "Seleccionado", tint = noteTextColor())
                }
            }
        }
    }
}
