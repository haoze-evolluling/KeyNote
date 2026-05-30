package com.haoze.keynote.data.db.dao

import androidx.room.*
import com.haoze.keynote.data.db.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Insert
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Query("SELECT * FROM schedules ORDER BY date ASC")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): ScheduleEntity?

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)
}
