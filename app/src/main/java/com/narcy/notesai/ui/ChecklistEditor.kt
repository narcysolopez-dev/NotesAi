package com.narcy.notesai.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.narcy.notesai.data.CheckItem

/**
 * Editor de lista. Los cambios se hacen directamente sobre [items]; el editor
 * principal los serializa al guardar.
 */
@Composable
fun ChecklistEditor(
    items: SnapshotStateList<CheckItem>,
    readOnly: Boolean,
    textColor: Color,
    fieldColors: TextFieldColors,
    textStyle: TextStyle
) {
    var focusIndex by remember { mutableIntStateOf(-1) }

    items.forEachIndexed { index, item ->
        val requester = remember { FocusRequester() }
        LaunchedEffect(focusIndex) {
            if (focusIndex == index) {
                requester.requestFocus()
                focusIndex = -1
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.checked,
                enabled = !readOnly,
                onCheckedChange = { items[index] = item.copy(checked = it) }
            )
            TextField(
                value = item.text,
                onValueChange = { items[index] = item.copy(text = it.replace("\n", "")) },
                readOnly = readOnly,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(requester),
                placeholder = { Text("Elemento") },
                textStyle = textStyle.copy(
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.checked) textColor.copy(alpha = 0.55f) else textColor
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    items.add(index + 1, CheckItem(""))
                    focusIndex = index + 1
                }),
                colors = fieldColors
            )
            if (!readOnly) {
                IconButton(onClick = {
                    items.removeAt(index)
                    if (items.isEmpty()) items.add(CheckItem(""))
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Quitar elemento", tint = textColor)
                }
            }
        }
    }

    if (!readOnly) {
        TextButton(
            onClick = {
                items.add(CheckItem(""))
                focusIndex = items.lastIndex
            },
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = textColor)
            Text("  Agregar elemento", color = textColor)
        }
    }
}
