package com.narcy.notesai.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.narcy.notesai.data.Note
import com.narcy.notesai.ui.theme.noteColor
import com.narcy.notesai.ui.theme.noteColorCount
import com.narcy.notesai.ui.theme.noteTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initial: Note,
    onSave: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onClose: () -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var content by remember { mutableStateOf(initial.content) }
    var colorIndex by remember { mutableIntStateOf(initial.colorIndex) }
    var pinned by remember { mutableStateOf(initial.pinned) }

    fun saveAndClose() {
        onSave(initial.copy(title = title, content = content, colorIndex = colorIndex, pinned = pinned))
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
                    IconButton(onClick = { pinned = !pinned }) {
                        Icon(
                            if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Fijar",
                            tint = textColor
                        )
                    }
                    IconButton(onClick = {
                        onDelete(initial)
                        onClose()
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = textColor)
                    }
                }
            )
        },
        bottomBar = {
            ColorPicker(
                selected = colorIndex,
                onSelect = { colorIndex = it },
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Título", style = MaterialTheme.typography.titleLarge) },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                colors = fieldColors
            )
            TextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weightedMinHeight(),
                placeholder = { Text("Escribe una nota") },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = fieldColors
            )
        }
    }
}

private fun Modifier.weightedMinHeight(): Modifier =
    this.then(Modifier.padding(bottom = 24.dp))

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
