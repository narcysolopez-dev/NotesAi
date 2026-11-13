package com.narcy.notesai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AudioEditor(
    recorder: AudioRecorder,
    player: AudioPlayer,
    mediaPath: String?,
    readOnly: Boolean,
    micDenied: Boolean,
    onRecord: () -> Unit,
    onStop: () -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    textColor: Color,
    fieldColors: TextFieldColors
) {
    LaunchedEffect(recorder.recording) {
        while (recorder.recording) {
            recorder.tick()
            delay(200)
        }
    }
    LaunchedEffect(player.playing) {
        while (player.playing) {
            player.tick()
            delay(200)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(textColor.copy(alpha = 0.08f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            recorder.recording -> {
                Text(
                    text = formatDuration(recorder.elapsedMs),
                    style = MaterialTheme.typography.displaySmall,
                    color = textColor
                )
                Text("Grabando…", style = MaterialTheme.typography.bodyMedium, color = textColor)
                FilledIconButton(onClick = onStop, modifier = Modifier.size(64.dp), shape = CircleShape) {
                    Icon(Icons.Filled.Stop, contentDescription = "Detener grabación")
                }
            }

            mediaPath != null -> {
                val storedDuration by rememberAudioDuration(mediaPath)
                val duration = if (player.durationMs > 0) player.durationMs else storedDuration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { player.toggle(mediaPath) }) {
                        Icon(
                            if (player.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (player.playing) "Pausar" else "Reproducir",
                            tint = textColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        LinearProgressIndicator(
                            progress = {
                                if (duration > 0) (player.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${formatDuration(player.positionMs.toLong())} / ${formatDuration(duration.toLong())}",
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                if (!readOnly) {
                    OutlinedButton(onClick = onRecord) {
                        Icon(Icons.Filled.Mic, contentDescription = null)
                        Text("  Grabar de nuevo")
                    }
                }
            }

            !readOnly -> {
                Text(
                    text = "Toca el micrófono para grabar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                FilledIconButton(onClick = onRecord, modifier = Modifier.size(64.dp), shape = CircleShape) {
                    Icon(Icons.Filled.Mic, contentDescription = "Grabar audio")
                }
            }
        }
        if (micDenied) {
            Text(
                text = "Para grabar necesitas permitir el uso del micrófono en los ajustes de la app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    TextField(
        value = content,
        onValueChange = onContentChange,
        readOnly = readOnly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        placeholder = { Text("Agrega una descripción") },
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = fieldColors
    )
}
