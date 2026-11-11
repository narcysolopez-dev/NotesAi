package com.narcy.notesai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.narcy.notesai.data.AppDatabase
import com.narcy.notesai.data.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).noteDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val notes: StateFlow<List<Note>> =
        combine(dao.observeAll(), _query) { list, q ->
            if (q.isBlank()) list
            else list.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.content.contains(q, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        _query.value = value
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

    fun delete(note: Note) {
        if (note.id == 0L) return
        viewModelScope.launch { dao.delete(note) }
    }
}
