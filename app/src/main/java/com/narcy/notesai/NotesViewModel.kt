package com.narcy.notesai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.narcy.notesai.data.AppDatabase
import com.narcy.notesai.data.AppSettings
import com.narcy.notesai.data.Label
import com.narcy.notesai.data.MediaFiles
import com.narcy.notesai.data.Note
import com.narcy.notesai.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val appContext: Application = app
    private val dao = AppDatabase.get(app).noteDao()
    private val settingsStore = SettingsStore(app)

    private fun deleteMedia(names: List<String?>) {
        names.forEach { MediaFiles.delete(appContext, it) }
    }

    val settings: StateFlow<AppSettings> = settingsStore.state

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _section = MutableStateFlow<Section>(Section.Notes)
    val section: StateFlow<Section> = _section

    val labels: StateFlow<List<Label>> =
        dao.observeLabels()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<Note>> =
        combine(dao.observeAll(), _query, _section) { list, q, section ->
            list.filter { section.matches(it) }
                .filter {
                    q.isBlank() ||
                        it.title.contains(q, ignoreCase = true) ||
                        it.content.contains(q, ignoreCase = true)
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Las notas en la papelera se eliminan después de 7 días.
        viewModelScope.launch {
            val before = System.currentTimeMillis() - TRASH_DAYS * DAY_MS
            val media = withContext(Dispatchers.IO) { dao.trashedMediaBefore(before) }
            dao.purgeTrashedBefore(before)
            withContext(Dispatchers.IO) { deleteMedia(media) }
        }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setSection(value: Section) {
        _section.value = value
        _query.value = ""
    }

    fun save(note: Note) {
        viewModelScope.launch {
            val blank = note.title.isBlank() && note.content.isBlank() && note.mediaPath == null
            if (blank) {
                if (note.id != 0L) dao.delete(note)
            } else {
                dao.upsert(note.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun trash(note: Note) {
        if (note.id == 0L) return
        viewModelScope.launch {
            dao.upsert(
                note.copy(trashed = true, pinned = false, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun restore(note: Note) {
        viewModelScope.launch {
            dao.upsert(note.copy(trashed = false, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteForever(note: Note) {
        if (note.id == 0L) return
        viewModelScope.launch {
            dao.delete(note)
            withContext(Dispatchers.IO) { deleteMedia(listOf(note.mediaPath)) }
        }
    }

    fun update(selected: List<Note>, transform: (Note) -> Note) {
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            selected.forEach { dao.upsert(transform(it).copy(updatedAt = now)) }
        }
    }

    fun duplicate(selected: List<Note>) {
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            selected.forEach {
                val mediaCopy = withContext(Dispatchers.IO) { MediaFiles.copy(appContext, it.mediaPath) }
                dao.upsert(
                    it.copy(
                        id = 0,
                        mediaPath = mediaCopy,
                        title = if (it.title.isBlank()) "" else "Copia de ${it.title}",
                        pinned = false,
                        updatedAt = now
                    )
                )
            }
        }
    }

    fun deleteAllForever(selected: List<Note>) {
        if (selected.isEmpty()) return
        viewModelScope.launch {
            selected.forEach { dao.delete(it) }
            withContext(Dispatchers.IO) { deleteMedia(selected.map { it.mediaPath }) }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val media = withContext(Dispatchers.IO) { dao.trashedMedia() }
            dao.emptyTrash()
            withContext(Dispatchers.IO) { deleteMedia(media) }
        }
    }

    fun addLabel(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { dao.insertLabel(Label(name = clean)) }
    }

    fun deleteLabel(label: Label) {
        if ((_section.value as? Section.Label)?.id == label.id) {
            _section.value = Section.Notes
        }
        viewModelScope.launch {
            dao.clearLabel(label.id)
            dao.deleteLabel(label)
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsStore.update(transform)
    }

    private companion object {
        const val TRASH_DAYS = 7L
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
