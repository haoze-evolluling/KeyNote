package com.haoze.keynote.ui.trash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TrashViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository

    private val _deletedNotes = MutableStateFlow<List<NoteWithTags>>(emptyList())
    val deletedNotes: StateFlow<List<NoteWithTags>> = _deletedNotes.asStateFlow()

    init {
        val db = NoteDatabase.getDatabase(application)
        repository = NoteRepository(db.noteDao(), db.tagDao(), PreferencesManager(application))

        viewModelScope.launch {
            repository.getAllDeletedNotes().collect { notes ->
                _deletedNotes.value = notes
            }
        }
    }

    fun restoreNote(noteWithTags: NoteWithTags) {
        viewModelScope.launch { repository.restoreNote(noteWithTags.note) }
    }

    fun permanentlyDeleteNote(noteWithTags: NoteWithTags) {
        viewModelScope.launch { repository.permanentlyDeleteNote(noteWithTags.note) }
    }
}
