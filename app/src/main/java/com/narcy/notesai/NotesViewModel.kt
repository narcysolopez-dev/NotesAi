package com.narcy.notesai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.narcy.notesai.data.AppDatabase
import com.narcy.notesai.data.AppSettings
import com.narcy.notesai.data.Label
import com.narcy.notesai.data.Note
import com.narcy.notesai.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).noteDao()
    private val settingsStore = SettingsStore(app)

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
            dao.purgeTrashedBefore(System.currentTimeMillis() - TRASH_DAYS * DAY_MS)
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
            val blank = note.title.isBlank() && note.content.isBlank()
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
        viewModelScope.launch { dao.delete(note) }
    }

    fun emptyTrash() {
        viewModelScope.launch { dao.emptyTrash() }
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
