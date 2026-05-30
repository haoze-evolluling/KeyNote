package com.haoze.keynote.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var repository: NoteRepository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _notes = MutableStateFlow<List<NoteWithTags>>(emptyList())
    val notes: StateFlow<List<NoteWithTags>> = _notes.asStateFlow()

    init {
        val db = NoteDatabase.getDatabase(application)
        repository = NoteRepository(db.noteDao(), db.tagDao(), PreferencesManager(application))
        viewModelScope.launch {
            _searchQuery.flatMapLatest { query ->
                if (query.isBlank()) {
                    repository.getAllActiveNotesWithTags()
                } else {
                    repository.searchNotesWithTags(query)
                }
            }.collect { result ->
                _notes.value = result
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            val noteWithTags = repository.getNoteWithTagsById(noteId).first()
            noteWithTags?.let { repository.softDeleteNote(it.note) }
        }
    }

    fun addTagToNote(noteId: Long, tagName: String) {
        viewModelScope.launch { repository.addTagToNote(noteId, tagName) }
    }

    fun removeTagFromNote(noteId: Long, tagId: Long) {
        viewModelScope.launch { repository.removeTagFromNote(noteId, tagId) }
    }

    suspend fun createEmptyNote(): Long {
        return repository.createNote("", "")
    }

    fun aiGenerateTags(noteId: Long, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.generateTagsForNote(noteId)
                result.onSuccess { onComplete(true) }
                    .onFailure { onComplete(false) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                onComplete(false)
                throw e
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun aiGenerateTitle(noteId: Long, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.generateTitle(noteId)
                result.onSuccess {
                    onComplete(true)
                }.onFailure { onComplete(false) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                onComplete(false)
                throw e
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun summarizeNote(noteId: Long, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.summarizeNote(noteId)
                result.onSuccess { onComplete(true) }
                    .onFailure { onComplete(false) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                onComplete(false)
                throw e
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}
