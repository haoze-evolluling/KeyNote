package com.haoze.keynote.ui.bill

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.CategorySpending
import com.haoze.keynote.data.db.entity.DailySpending
import com.haoze.keynote.data.db.entity.MonthlySpending
import com.haoze.keynote.data.db.entity.WeeklySpending
import com.haoze.keynote.data.repository.BillRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class RangePreset { THIS_WEEK, THIS_MONTH, THIS_YEAR, ALL, CUSTOM }
enum class Granularity { DAY, WEEK, MONTH }

class BillStatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BillRepository

    private val _dateRange = MutableStateFlow<Pair<Long, Long>>(getDefaultMonthRange())
    val dateRange: StateFlow<Pair<Long, Long>> = _dateRange.asStateFlow()

    private val _totalSpending = MutableStateFlow(0.0)
    val totalSpending: StateFlow<Double> = _totalSpending.asStateFlow()

    private val _billCount = MutableStateFlow(0)
    val billCount: StateFlow<Int> = _billCount.asStateFlow()

    private val _categoryStats = MutableStateFlow<List<CategorySpending>>(emptyList())
    val categoryStats: StateFlow<List<CategorySpending>> = _categoryStats.asStateFlow()

    private val _monthlyTrend = MutableStateFlow<List<MonthlySpending>>(emptyList())
    val monthlyTrend: StateFlow<List<MonthlySpending>> = _monthlyTrend.asStateFlow()

    private val _granularity = MutableStateFlow(Granularity.MONTH)
    val granularity: StateFlow<Granularity> = _granularity.asStateFlow()

    private val _dailySpending = MutableStateFlow<List<DailySpending>>(emptyList())
    val dailySpending: StateFlow<List<DailySpending>> = _dailySpending.asStateFlow()

    private val _dailyTrend = MutableStateFlow<List<DailySpending>>(emptyList())
    val dailyTrend: StateFlow<List<DailySpending>> = _dailyTrend.asStateFlow()

    private val _weeklyTrend = MutableStateFlow<List<WeeklySpending>>(emptyList())
    val weeklyTrend: StateFlow<List<WeeklySpending>> = _weeklyTrend.asStateFlow()

    init {
        val db = NoteDatabase.getDatabase(application)
        repository = BillRepository(db.billDao(), db.categoryDao())
        collectStats()
    }

    fun setPresetRange(preset: RangePreset) {
        _dateRange.value = when (preset) {
            RangePreset.THIS_WEEK -> getWeekRange()
            RangePreset.THIS_MONTH -> getMonthRange()
            RangePreset.THIS_YEAR -> getYearRange()
            RangePreset.ALL -> Pair(0L, System.currentTimeMillis())
            RangePreset.CUSTOM -> _dateRange.value
        }
    }

    fun setCustomRange(start: Long, end: Long) {
        _dateRange.value = Pair(start, end)
    }

    fun setGranularity(g: Granularity) {
        _granularity.value = g
    }

    private fun collectStats() {
        viewModelScope.launch {
            _dateRange.collectLatest { (start, end) ->
                launch {
                    repository.getSpendingInRange(start, end).collect {
                        _totalSpending.value = it ?: 0.0
                    }
                }
                launch {
                    repository.getBillCountInRange(start, end).collect {
                        _billCount.value = it
                    }
                }
                launch {
                    repository.getSpendingByCategory(start, end).collect {
                        _categoryStats.value = it
                    }
                }
                launch {
                    repository.getMonthlySpendingTrend(start, end).collect {
                        _monthlyTrend.value = it
                    }
                }
                launch {
                    repository.getDailySpending(start, end).collect {
                        _dailySpending.value = it
                    }
                }
                launch {
                    repository.getDailySpending(start, end).collect {
                        _dailyTrend.value = it
                    }
                }
                launch {
                    repository.getWeeklySpending(start, end).collect {
                        _weeklyTrend.value = it
                    }
                }
            }
        }
    }

    private fun getWeekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return Pair(cal.timeInMillis, System.currentTimeMillis())
    }

    private fun getDefaultMonthRange() = getMonthRange()

    private fun getMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return Pair(cal.timeInMillis, System.currentTimeMillis())
    }

    private fun getYearRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return Pair(cal.timeInMillis, System.currentTimeMillis())
    }
}
