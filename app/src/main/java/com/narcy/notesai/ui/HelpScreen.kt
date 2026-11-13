package com.narcy.notesai.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

private val tips = listOf(
    "Toca \"Nueva nota\" para crear una nota. Se guarda sola al volver.",
    "Usa el icono de la chincheta para fijar una nota arriba de las demás.",
    "Elige un color para cada nota con la barra inferior del editor.",
    "Organiza con etiquetas desde el menú ☰ y asígnalas con el icono de etiqueta.",
    "Archiva las notas que no uses a diario; siguen disponibles en \"Archivar\".",
    "Toca \"Nueva nota\" para elegir el tipo de nota que quieres crear.",
    "En una nota de imagen, elige una foto de tu galería y agrega una descripción debajo.",
    "En un dibujo, elige un color y dibuja con el dedo; usa deshacer o borrar todo si te equivocas.",
    "En una lista, pulsa Siguiente en el teclado para agregar otro elemento y marca los terminados con la casilla.",
    "Las notas de la papelera se eliminan definitivamente después de 7 días."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda y comentarios") },
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Consejos rápidos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )
            tips.forEach { tip ->
                Text(
                    text = "•  $tip",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Button(
                onClick = { uriHandler.openUri("https://github.com/narcy94/NotesAi/issues/new") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp)
            ) {
                Text("Enviar comentarios")
            }
        }
    }
}
