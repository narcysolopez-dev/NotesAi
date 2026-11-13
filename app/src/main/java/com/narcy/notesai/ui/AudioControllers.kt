package com.narcy.notesai.ui

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.narcy.notesai.data.MediaFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Graba audio del micrófono a un archivo .m4a en la carpeta de la app. */
class AudioRecorder(private val context: Context) {
    var recording by mutableStateOf(false)
        private set
    var elapsedMs by mutableLongStateOf(0L)
        private set

    private var recorder: MediaRecorder? = null
    private var currentName: String? = null
    private var startedAt = 0L

    fun start(): Boolean {
        if (recording) return true
        val name = MediaFiles.newName("m4a")
        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(MediaFiles.file(context, name).absolutePath)
            r.prepare()
            r.start()
            recorder = r
            currentName = name
            startedAt = SystemClock.elapsedRealtime()
            elapsedMs = 0L
            recording = true
            true
        } catch (e: Exception) {
            runCatching { r.release() }
            MediaFiles.delete(context, name)
            false
        }
    }

    fun tick() {
        if (recording) elapsedMs = SystemClock.elapsedRealtime() - startedAt
    }

    /** Detiene la grabación y devuelve el nombre del archivo, o null si no se pudo guardar. */
    fun stop(): String? {
        val r = recorder ?: return null
        val name = currentName
        recorder = null
        currentName = null
        recording = false
        val ok = try {
            r.stop()
            true
        } catch (e: RuntimeException) {
            // Pasa si se detiene casi al instante: no hay audio que guardar.
            false
        }
        runCatching { r.release() }
        if (ok && name != null) return name
        MediaFiles.delete(context, name)
        return null
    }

    /** Se usa al salir de la pantalla: si quedó grabando, se descarta. */
    fun release() {
        if (recording) stop()?.let { MediaFiles.delete(context, it) }
    }
}

/** Reproduce el audio de una nota. */
class AudioPlayer(private val context: Context) {
    var playing by mutableStateOf(false)
        private set
    var positionMs by mutableIntStateOf(0)
        private set
    var durationMs by mutableIntStateOf(0)
        private set

    private var player: MediaPlayer? = null
    private var loaded: String? = null

    fun toggle(name: String) {
        if (playing) pause() else play(name)
    }

    private fun play(name: String) {
        if (loaded != name) release()
        val current = player ?: runCatching {
            MediaPlayer().apply {
                setDataSource(MediaFiles.file(context, name).absolutePath)
                prepare()
                setOnCompletionListener {
                    this@AudioPlayer.playing = false
                    this@AudioPlayer.positionMs = 0
                }
            }
        }.getOrNull() ?: return
        player = current
        loaded = name
        durationMs = current.duration
        current.start()
        playing = true
    }

    private fun pause() {
        player?.pause()
        playing = false
    }

    fun tick() {
        val p = player ?: return
        if (playing) positionMs = runCatching { p.currentPosition }.getOrDefault(0)
    }

    fun stop() {
        release()
    }

    fun release() {
        runCatching { player?.release() }
        player = null
        loaded = null
        playing = false
        positionMs = 0
    }
}

/** Duración (en ms) de un audio guardado. */
@Composable
fun rememberAudioDuration(name: String?): State<Int> {
    val context = LocalContext.current
    return produceState(initialValue = 0, name) {
        value = if (name == null) {
            0
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(MediaFiles.file(context, name).absolutePath)
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toIntOrNull() ?: 0
                    } finally {
                        retriever.release()
                    }
                }.getOrDefault(0)
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
