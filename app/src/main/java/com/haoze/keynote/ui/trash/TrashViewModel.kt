package com.haoze.keynote.ui.trash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.BillEntity
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.data.db.entity.ScheduleEntity
import com.haoze.keynote.data.repository.BillRepository
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.data.repository.ScheduleRepository
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class TrashItem {
    abstract val deletedAt: Long

    data class TrashNote(val data: NoteWithTags) : TrashItem() {
        override val deletedAt: Long get() = data.note.deletedAt ?: data.note.updatedAt
    }

    data class TrashBill(val data: BillEntity) : TrashItem() {
        override val deletedAt: Long get() = data.deletedAt ?: 0
    }

    data class TrashSchedule(val data: ScheduleEntity) : TrashItem() {
        override val deletedAt: Long get() = data.deletedAt ?: 0
    }
}

class TrashViewModel(application: Application) : AndroidViewModel(application) {

    private val noteRepository: NoteRepository
    private val billRepository: BillRepository
    private val scheduleRepository: ScheduleRepository

    private val _trashItems = MutableStateFlow<List<TrashItem>>(emptyList())
    val trashItems: StateFlow<List<TrashItem>> = _trashItems.asStateFlow()

    init {
        val db = NoteDatabase.getDatabase(application)
        noteRepository = NoteRepository(db.noteDao(), db.tagDao(), PreferencesManager(application))
        billRepository = BillRepository(db.billDao(), db.categoryDao())
        scheduleRepository = ScheduleRepository(db.scheduleDao())

        viewModelScope.launch {
            combine(
                noteRepository.getAllDeletedNotes(),
                billRepository.getAllDeletedBills(),
                scheduleRepository.getAllDeletedSchedules()
            ) { notes, bills, schedules ->
                val items = mutableListOf<TrashItem>()
                notes.mapTo(items) { TrashItem.TrashNote(it) }
                bills.mapTo(items) { TrashItem.TrashBill(it) }
                schedules.mapTo(items) { TrashItem.TrashSchedule(it) }
                items.sortedByDescending { it.deletedAt }
            }.collect { _trashItems.value = it }
        }
    }

    fun restore(item: TrashItem) {
        viewModelScope.launch {
            when (item) {
                is TrashItem.TrashNote -> noteRepository.restoreNote(item.data.note)
                is TrashItem.TrashBill -> billRepository.restore(item.data)
                is TrashItem.TrashSchedule -> scheduleRepository.restore(item.data)
            }
        }
    }

    fun permanentlyDelete(item: TrashItem) {
        viewModelScope.launch {
            when (item) {
                is TrashItem.TrashNote -> noteRepository.permanentlyDeleteNote(item.data.note)
                is TrashItem.TrashBill -> billRepository.permanentlyDelete(item.data)
                is TrashItem.TrashSchedule -> scheduleRepository.permanentlyDelete(item.data)
            }
        }
    }
}
