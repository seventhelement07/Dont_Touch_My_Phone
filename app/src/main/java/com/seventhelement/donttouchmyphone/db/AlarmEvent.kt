package com.seventhelement.donttouchmyphone.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_events")
data class AlarmEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val triggerType: String
)
