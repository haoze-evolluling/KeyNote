package com.haoze.keynote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: Long,
    val endDate: Long? = null,
    val location: String? = null,
    val description: String? = null,
    val noteId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
