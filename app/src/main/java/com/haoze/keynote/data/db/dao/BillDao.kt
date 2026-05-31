package com.haoze.keynote.data.db.dao

import androidx.room.*
import com.haoze.keynote.data.db.entity.BillEntity
import com.haoze.keynote.data.db.entity.BillWithCategoryRaw
import com.haoze.keynote.data.db.entity.CategorySpending
import com.haoze.keynote.data.db.entity.DailySpending
import com.haoze.keynote.data.db.entity.MonthlySpending
import com.haoze.keynote.data.db.entity.WeeklySpending
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Insert
    suspend fun insertBill(bill: BillEntity): Long

    @Query("SELECT * FROM bills ORDER BY date DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: Long): BillEntity?

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    @Query("SELECT SUM(amount) FROM bills")
    fun getTotalSpending(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM bills WHERE date >= :startOfMonth")
    fun getMonthlySpending(startOfMonth: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM bills WHERE date >= :startOfYear")
    fun getYearlySpending(startOfYear: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM bills")
    fun getBillCount(): Flow<Int>

    @Query("""
        SELECT b.id as bill_id, b.item as bill_item, b.amount as bill_amount, b.date as bill_date, b.categoryId as bill_categoryId,
               c.id as cat_id, c.name as cat_name, c.isDefault as cat_isDefault
        FROM bills b LEFT JOIN categories c ON b.categoryId = c.id
        ORDER BY b.date DESC
    """)
    fun getBillsWithCategory(): Flow<List<BillWithCategoryRaw>>

    @Query("""
        SELECT categoryId,
               COALESCE(c.name, '未分类') as categoryName,
               SUM(b.amount) as total
        FROM bills b LEFT JOIN categories c ON b.categoryId = c.id
        WHERE b.date BETWEEN :start AND :end
        GROUP BY categoryId
    """)
    fun getSpendingByCategory(start: Long, end: Long): Flow<List<CategorySpending>>

    @Query("""
        SELECT strftime('%Y-%m', b.date / 1000, 'unixepoch', 'localtime') as month,
               SUM(b.amount) as total
        FROM bills b
        WHERE b.date BETWEEN :start AND :end
        GROUP BY month
        ORDER BY month
    """)
    fun getMonthlySpendingTrend(start: Long, end: Long): Flow<List<MonthlySpending>>

    @Query("SELECT SUM(amount) FROM bills WHERE date BETWEEN :start AND :end")
    fun getSpendingInRange(start: Long, end: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM bills WHERE date BETWEEN :start AND :end")
    fun getBillCountInRange(start: Long, end: Long): Flow<Int>

    @Query("SELECT * FROM bills WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getBillsByDateRange(start: Long, end: Long): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE date BETWEEN :start AND :end AND categoryId IN (:categoryIds) ORDER BY date DESC")
    fun getBillsByDateRangeAndCategory(start: Long, end: Long, categoryIds: List<Long>): Flow<List<BillEntity>>

    @Query("""
        SELECT strftime('%Y-%m-%d', b.date / 1000, 'unixepoch', 'localtime') as day,
               SUM(b.amount) as total
        FROM bills b
        WHERE b.date BETWEEN :start AND :end
        GROUP BY day
        ORDER BY day
    """)
    fun getDailySpending(start: Long, end: Long): Flow<List<DailySpending>>

    @Query("""
        SELECT strftime('%Y-W%W', b.date / 1000, 'unixepoch', 'localtime') as week,
               SUM(b.amount) as total
        FROM bills b
        WHERE b.date BETWEEN :start AND :end
        GROUP BY week
        ORDER BY week
    """)
    fun getWeeklySpending(start: Long, end: Long): Flow<List<WeeklySpending>>
}
