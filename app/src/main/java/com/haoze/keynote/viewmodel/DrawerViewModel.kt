package com.haoze.keynote.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.TagEntity
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DrawerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository

    private val _tags = MutableStateFlow<List<TagEntity>>(emptyList())
    val tags: StateFlow<List<TagEntity>> = _tags.asStateFlow()

    init {
        val db = NoteDatabase.getDatabase(application)
        repository = NoteRepository(db.noteDao(), db.tagDao(), PreferencesManager(application))

        viewModelScope.launch {
            repository.getActiveTags().collect { tagList ->
                _tags.value = tagList
            }
        }
    }
}
