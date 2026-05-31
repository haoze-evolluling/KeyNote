package com.haoze.keynote.data.repository

import com.haoze.keynote.data.db.dao.ScheduleDao
import com.haoze.keynote.data.db.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ScheduleRepository(private val scheduleDao: ScheduleDao) {

    fun getAllSchedules(): Flow<List<ScheduleEntity>> = scheduleDao.getAllSchedules()

    suspend fun getScheduleById(id: Long): ScheduleEntity? = scheduleDao.getScheduleById(id)

    suspend fun insertSchedule(title: String, date: Long, endDate: Long? = null, location: String? = null, description: String? = null, noteId: Long? = null): Long {
        return scheduleDao.insertSchedule(ScheduleEntity(title = title, date = date, endDate = endDate, location = location, description = description, noteId = noteId))
    }

    suspend fun updateSchedule(schedule: ScheduleEntity) = scheduleDao.updateSchedule(schedule)

    suspend fun deleteSchedule(schedule: ScheduleEntity) = scheduleDao.deleteSchedule(schedule)

    fun getSchedulesByDateRange(start: Long, end: Long): Flow<List<ScheduleEntity>> =
        scheduleDao.getSchedulesByDateRange(start, end)

    suspend fun getAllSchedulesList(): List<ScheduleEntity> {
        var result = emptyList<ScheduleEntity>()
        scheduleDao.getAllSchedules().first().let { result = it }
        return result
    }

    suspend fun getSchedulesByDateRangeList(start: Long, end: Long): List<ScheduleEntity> {
        var result = emptyList<ScheduleEntity>()
        scheduleDao.getSchedulesByDateRange(start, end).first().let { result = it }
        return result
    }
}
