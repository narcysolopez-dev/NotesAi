package com.narcy.notesai.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.narcy.notesai.data.AppSettings
import com.narcy.notesai.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Tema",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
            )

            val options = listOf(
                ThemeMode.System to "Predeterminado del sistema",
                ThemeMode.Light to "Claro",
                ThemeMode.Dark to "Oscuro"
            )
            options.forEach { (mode, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = settings.theme == mode,
                            onClick = { onChange { s -> s.copy(theme = mode) } },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = settings.theme == mode, onClick = null)
                    Text(label, modifier = Modifier.padding(start = 16.dp))
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SwitchRow(
                title = "Vista de cuadrícula",
                subtitle = "Muestra las notas en dos columnas",
                checked = settings.grid,
                onCheckedChange = { checked -> onChange { s -> s.copy(grid = checked) } }
            )

            val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            SwitchRow(
                title = "Colores dinámicos",
                subtitle = if (dynamicSupported) {
                    "Usa los colores de tu fondo de pantalla"
                } else {
                    "Requiere Android 12 o superior"
                },
                checked = settings.dynamicColor && dynamicSupported,
                enabled = dynamicSupported,
                onCheckedChange = { checked -> onChange { s -> s.copy(dynamicColor = checked) } }
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
