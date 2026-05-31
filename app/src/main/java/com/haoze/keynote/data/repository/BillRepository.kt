package com.haoze.keynote.data.repository

import com.haoze.keynote.data.db.dao.BillDao
import com.haoze.keynote.data.db.dao.CategoryDao
import com.haoze.keynote.data.db.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BillRepository(
    private val billDao: BillDao,
    private val categoryDao: CategoryDao
) {

    fun getAllBills(): Flow<List<BillEntity>> = billDao.getAllBills()

    fun getBillsWithCategory(): Flow<List<BillWithCategoryRaw>> = billDao.getBillsWithCategory()

    suspend fun getAllBillsList(): List<BillEntity> {
        var result = emptyList<BillEntity>()
        billDao.getAllBills().first().let { result = it }
        return result
    }

    suspend fun insertBill(item: String, amount: Double, date: Long, categoryId: Long? = null): Long {
        return billDao.insertBill(BillEntity(item = item, amount = amount, date = date, categoryId = categoryId))
    }

    suspend fun updateBill(bill: BillEntity) = billDao.updateBill(bill)

    suspend fun deleteBill(bill: BillEntity) = billDao.deleteBill(bill)

    suspend fun getBillsByDateRange(start: Long, end: Long): List<BillEntity> {
        var result = emptyList<BillEntity>()
        billDao.getBillsByDateRange(start, end).first().let { result = it }
        return result
    }

    suspend fun getBillsByDateRangeAndCategory(start: Long, end: Long, categoryIds: List<Long>): List<BillEntity> {
        var result = emptyList<BillEntity>()
        billDao.getBillsByDateRangeAndCategory(start, end, categoryIds).first().let { result = it }
        return result
    }

    fun getTotalSpending(): Flow<Double?> = billDao.getTotalSpending()
    fun getMonthlySpending(startOfMonth: Long): Flow<Double?> = billDao.getMonthlySpending(startOfMonth)
    fun getYearlySpending(startOfYear: Long): Flow<Double?> = billDao.getYearlySpending(startOfYear)
    fun getBillCount(): Flow<Int> = billDao.getBillCount()

    fun getSpendingInRange(start: Long, end: Long): Flow<Double?> = billDao.getSpendingInRange(start, end)
    fun getBillCountInRange(start: Long, end: Long): Flow<Int> = billDao.getBillCountInRange(start, end)
    fun getSpendingByCategory(start: Long, end: Long): Flow<List<CategorySpending>> = billDao.getSpendingByCategory(start, end)
    fun getMonthlySpendingTrend(start: Long, end: Long): Flow<List<MonthlySpending>> = billDao.getMonthlySpendingTrend(start, end)
    fun getDailySpending(start: Long, end: Long): Flow<List<DailySpending>> = billDao.getDailySpending(start, end)
    fun getWeeklySpending(start: Long, end: Long): Flow<List<WeeklySpending>> = billDao.getWeeklySpending(start, end)

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    suspend fun insertCategory(name: String): Long = categoryDao.insertCategory(CategoryEntity(name = name))
    suspend fun getCategoryById(id: Long): CategoryEntity? = categoryDao.getCategoryById(id)
}
