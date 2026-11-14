package com.narcy.notesai.data

import android.content.Context
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Estado de la sincronización, para mostrarlo en la pantalla de cuenta. */
sealed interface SyncStatus {
    data object SignedOut : SyncStatus
    data object Connecting : SyncStatus
    data object Synced : SyncStatus

    /** Sin conexión o con cambios por enviar: Firestore los sincroniza solo al volver la red. */
    data object Pending : SyncStatus
    data class Error(val message: String) : SyncStatus
}

/** Una nota tal como se guarda en Firestore. Todos los números son Long, como los devuelve Firestore. */
private typealias Doc = Map<String, Any?>

private fun docOf(note: Note, labelName: String?): Doc = mapOf(
    "title" to note.title,
    "content" to note.content,
    "colorIndex" to note.colorIndex.toLong(),
    "pinned" to note.pinned,
    "updatedAt" to note.updatedAt,
    "archived" to note.archived,
    "trashed" to note.trashed,
    "label" to labelName,
    "type" to note.type,
    "spans" to note.spans
)

private fun remoteDoc(s: DocumentSnapshot): Doc = mapOf(
    "title" to (s.getString("title") ?: ""),
    "content" to (s.getString("content") ?: ""),
    "colorIndex" to (s.getLong("colorIndex") ?: 0L),
    "pinned" to (s.getBoolean("pinned") ?: false),
    "updatedAt" to (s.getLong("updatedAt") ?: 0L),
    "archived" to (s.getBoolean("archived") ?: false),
    "trashed" to (s.getBoolean("trashed") ?: false),
    "label" to s.getString("label"),
    "type" to (s.getString("type") ?: "text"),
    "spans" to s.getString("spans")
)

/**
 * Sincroniza las notas de Room con Cloud Firestore en users/{uid}/notes/{noteId}.
 *
 * - Room sigue siendo la base local: la app funciona igual sin sesión y sin internet.
 * - Con sesión, los cambios de Room se envían a Firestore y los de Firestore (de otros
 *   dispositivos) llegan en tiempo real a Room.
 * - Firestore guarda su propia copia local (persistencia offline): los cambios hechos sin
 *   conexión quedan en cola y se envían solos al reconectar.
 * - Si dos dispositivos cambian la misma nota, gana el cambio más reciente (updatedAt).
 * - Las etiquetas viajan por nombre; los archivos de imagen, dibujo y audio no se suben
 *   (Firestore no guarda archivos), así que esas notas sincronizan su texto y formato.
 */
class CloudSync(
    context: Context,
    private val dao: NoteDao,
    user: StateFlow<CloudUser?>,
    scope: CoroutineScope
) {
    private companion object {
        const val TRASH_MS = 7L * 24 * 60 * 60 * 1000
    }

    private val appContext = context.applicationContext
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance().also { configure(it) }

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.SignedOut)
    val status: StateFlow<SyncStatus> = _status

    init {
        scope.launch(Dispatchers.Default) {
            user.map { it?.uid }.distinctUntilChanged().collectLatest { uid ->
                if (uid == null) {
                    _status.value = SyncStatus.SignedOut
                } else {
                    runSession(uid)
                }
            }
        }
    }

    private fun configure(db: FirebaseFirestore) {
        try {
            // Persistencia offline nativa de Firestore (caché en disco).
            db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
        } catch (e: IllegalStateException) {
            // Firestore ya estaba en uso con su configuración; se deja como está.
        }
    }

    private fun snapshots(col: CollectionReference): Flow<QuerySnapshot> = callbackFlow {
        val registration = col.addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
            if (error != null) close(error) else if (snap != null) trySend(snap)
        }
        awaitClose { registration.remove() }
        // Cada snapshot trae sus cambios respecto al anterior: no se puede descartar ninguno.
    }.buffer(Channel.UNLIMITED)

    private fun describe(e: Throwable): String =
        if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            "Firestore rechazó el acceso. Publica las reglas de seguridad (firestore.rules) en Firebase Console."
        } else {
            "No se pudo sincronizar: ${e.message ?: "error desconocido"}"
        }

    /** Una sesión dura mientras la misma cuenta siga conectada. Se cancela al cerrar sesión. */
    private suspend fun runSession(uid: String) {
        _status.value = SyncStatus.Connecting
        val col = firestore.collection("users").document(uid).collection("notes")

        // Última versión de cada nota que se sabe igual en la nube (enviada o recibida).
        val synced = ConcurrentHashMap<String, Doc>()
        val mutex = Mutex()
        val firstSnapshot = CompletableDeferred<Unit>()

        coroutineScope {
            // Nube -> Room
            launch {
                try {
                    snapshots(col).collect { snap ->
                        mutex.withLock { applyRemote(col, snap, synced) }
                        firstSnapshot.complete(Unit)
                        _status.value =
                            if (snap.metadata.isFromCache || snap.metadata.hasPendingWrites()) {
                                SyncStatus.Pending
                            } else {
                                SyncStatus.Synced
                            }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _status.value = SyncStatus.Error(describe(e))
                }
            }

            // Room -> nube. Espera al primer estado de la nube para no pisar cambios más nuevos.
            launch {
                firstSnapshot.await()
                try {
                    combine(dao.observeAll(), dao.observeLabels()) { notes, labels -> notes to labels }
                        .collect { (notes, labels) ->
                            mutex.withLock { pushLocal(col, notes, labels, synced) }
                        }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _status.value = SyncStatus.Error(describe(e))
                }
            }
        }
    }

    private suspend fun labelNameOf(labelId: Long?): String? =
        labelId?.let { dao.labelById(it)?.name }

    private suspend fun applyRemote(
        col: CollectionReference,
        snap: QuerySnapshot,
        synced: MutableMap<String, Doc>
    ) {
        for (change in snap.documentChanges) {
            val doc = change.document
            // Los cambios que aún esperan confirmación son nuestros: ya están en Room.
            if (doc.metadata.hasPendingWrites()) continue
            val cloudId = doc.id

            if (change.type == DocumentChange.Type.REMOVED) {
                synced.remove(cloudId)
                val local = dao.byCloudId(cloudId)
                if (local != null) {
                    dao.delete(local)
                    withContext(Dispatchers.IO) { MediaFiles.delete(appContext, local.mediaPath) }
                }
                continue
            }

            val remote = remoteDoc(doc)
            val local = dao.byCloudId(cloudId)
            if (local == null) {
                // Si la conocíamos y ya no está, se borró aquí y el borrado se enviará.
                if (cloudId in synced) continue
                // La papelera se vacía a los 7 días: una nota vencida se borra también de la nube
                // en vez de volver a aparecer aquí.
                val expired = remote["trashed"] == true &&
                    (remote["updatedAt"] as Long) < System.currentTimeMillis() - TRASH_MS
                if (expired) {
                    col.document(cloudId).delete()
                    continue
                }
                saveFromRemote(cloudId, remote, null)
                synced[cloudId] = remote
            } else if (docOf(local, labelNameOf(local.labelId)) == remote) {
                synced[cloudId] = remote
            } else if ((remote["updatedAt"] as Long) >= local.updatedAt) {
                saveFromRemote(cloudId, remote, local)
                synced[cloudId] = remote
            }
            // Si lo local es más nuevo, no se toca: pushLocal lo enviará.
        }
    }

    private suspend fun saveFromRemote(cloudId: String, remote: Doc, local: Note?) {
        val labelName = (remote["label"] as String?)?.takeIf { it.isNotBlank() }
        val labelId = labelName?.let { name ->
            dao.labelByName(name)?.id ?: dao.insertLabel(Label(name = name))
        }
        dao.upsert(
            Note(
                id = local?.id ?: 0,
                title = remote["title"] as String,
                content = remote["content"] as String,
                colorIndex = (remote["colorIndex"] as Long).toInt(),
                pinned = remote["pinned"] as Boolean,
                updatedAt = remote["updatedAt"] as Long,
                archived = remote["archived"] as Boolean,
                trashed = remote["trashed"] as Boolean,
                labelId = labelId,
                type = remote["type"] as String,
                // Los archivos no viajan: si esta copia ya tenía el suyo, se conserva.
                mediaPath = local?.mediaPath,
                spans = remote["spans"] as String?,
                cloudId = cloudId
            )
        )
    }

    private fun pushLocal(
        col: CollectionReference,
        notes: List<Note>,
        labels: List<Label>,
        synced: MutableMap<String, Doc>
    ) {
        val labelNames = labels.associate { it.id to it.name }
        val present = HashSet<String>()

        for (note in notes) {
            val cloudId = note.cloudId ?: continue
            present.add(cloudId)
            val doc = docOf(note, note.labelId?.let { labelNames[it] })
            if (synced[cloudId] != doc) {
                synced[cloudId] = doc
                col.document(cloudId).set(doc).addOnFailureListener { e ->
                    synced.remove(cloudId) // se reintenta con el próximo cambio
                    _status.value = SyncStatus.Error(describe(e))
                }
            }
        }

        // Lo que estaba sincronizado y ya no existe en Room se borró aquí.
        for (cloudId in synced.keys.toList()) {
            if (cloudId !in present) {
                synced.remove(cloudId)
                col.document(cloudId).delete().addOnFailureListener { e ->
                    _status.value = SyncStatus.Error(describe(e))
                }
            }
        }
    }
}
