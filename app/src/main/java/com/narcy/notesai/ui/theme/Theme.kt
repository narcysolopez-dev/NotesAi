package com.narcy.notesai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun NotesAiTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private val lightNoteColors = listOf(
    Color(0xFFFFFFFF), Color(0xFFFAAFA8), Color(0xFFF39F76),
    Color(0xFFFFF8B8), Color(0xFFE2F6D3), Color(0xFFB4DDD3),
    Color(0xFFD4E4ED), Color(0xFFD3BFDB), Color(0xFFF6E2DD)
)

private val darkNoteColors = listOf(
    Color(0xFF202124), Color(0xFF77172E), Color(0xFF692B17),
    Color(0xFF7C4A03), Color(0xFF264D3B), Color(0xFF0C625D),
    Color(0xFF256377), Color(0xFF472E5B), Color(0xFF6C394F)
)

val noteColorCount: Int get() = lightNoteColors.size

@Composable
fun noteColor(index: Int): Color {
    val colors = if (isSystemInDarkTheme()) darkNoteColors else lightNoteColors
    return colors[index.coerceIn(0, colors.lastIndex)]
}

@Composable
fun noteTextColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFFE8EAED) else Color(0xFF202124)
