package com.narcy.notesai.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.narcy.notesai.data.CloudUser
import com.narcy.notesai.data.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/** Descarga la foto de perfil (solo https). Devuelve null mientras carga o si falla. */
@Composable
private fun rememberRemoteImage(url: String?): State<ImageBitmap?> =
    produceState<ImageBitmap?>(initialValue = null, url) {
        value = if (url == null || !url.startsWith("https://")) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val connection = URL(url).openConnection().apply {
                        connectTimeout = 8_000
                        readTimeout = 8_000
                    }
                    connection.getInputStream().use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                }.getOrNull()
            }
        }
    }

/** Foto de perfil, o la inicial del nombre si no hay foto; un icono genérico si no hay sesión. */
@Composable
fun AccountAvatar(user: CloudUser?, size: Dp, modifier: Modifier = Modifier) {
    if (user == null) {
        Icon(
            Icons.Filled.AccountCircle,
            contentDescription = "Cuenta de Google",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(size)
        )
        return
    }
    val photo by rememberRemoteImage(user.photoUrl)
    val bitmap = photo
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val initial = (user.name ?: user.email ?: "?").trim().firstOrNull()?.uppercase() ?: "?"
            Text(
                text = initial,
                style = if (size >= 56.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** Diálogo de cuenta: iniciar sesión con Google, o ver el correo conectado y cerrar sesión. */
@Composable
fun AccountDialog(
    user: CloudUser?,
    busy: Boolean,
    error: String?,
    syncStatus: SyncStatus,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cuenta de Google") },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (user != null) {
                    AccountAvatar(user, 72.dp)
                    Spacer(Modifier.height(4.dp))
                    if (!user.name.isNullOrBlank()) {
                        Text(
                            user.name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        user.email ?: "Sin correo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    val (statusText, isError) = when (syncStatus) {
                        SyncStatus.SignedOut, SyncStatus.Connecting -> "Conectando con la nube…" to false
                        SyncStatus.Synced -> "Notas sincronizadas" to false
                        SyncStatus.Pending -> "Sin conexión o con cambios pendientes: se sincronizarán solos" to false
                        is SyncStatus.Error -> syncStatus.message to true
                    }
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(
                        "Inicia sesión con tu cuenta personal de Google.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    if (busy) {
                        CircularProgressIndicator(Modifier.padding(top = 8.dp).size(28.dp))
                    }
                }
                if (error != null) {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (user == null) {
                Button(onClick = onSignIn, enabled = !busy) { Text("Iniciar sesión con Google") }
            } else {
                OutlinedButton(onClick = onSignOut) { Text("Cerrar sesión") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
