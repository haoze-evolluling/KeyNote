package com.haoze.keynote.ui.bill

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.BillEntity
import com.haoze.keynote.data.db.entity.BillWithCategoryRaw
import com.haoze.keynote.data.db.entity.CategoryEntity
import com.haoze.keynote.data.repository.BillRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BillViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BillRepository
    val bills: StateFlow<List<BillEntity>>
    val billsWithCategory: StateFlow<List<BillWithCategoryRaw>>
    val categories: StateFlow<List<CategoryEntity>>

    init {
        val db = NoteDatabase.getDatabase(application)
        repository = BillRepository(db.billDao(), db.categoryDao())
        bills = repository.getAllBills()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        billsWithCategory = repository.getBillsWithCategory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        categories = repository.getAllCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun updateBill(bill: BillEntity) {
        viewModelScope.launch { repository.updateBill(bill) }
    }

    fun deleteBill(bill: BillEntity) {
        viewModelScope.launch { repository.softDelete(bill) }
    }

    fun createBill(item: String, amount: Double, date: Long, categoryId: Long? = null) {
        viewModelScope.launch { repository.insertBill(item, amount, date, categoryId) }
    }

    fun addCategory(name: String) {
        viewModelScope.launch { repository.insertCategory(name) }
    }
}
