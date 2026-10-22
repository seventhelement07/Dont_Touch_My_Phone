package com.seventhelement.donttouchmyphone.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AlarmHistoryDao {
    @Insert
    suspend fun insertEvent(event: AlarmEvent): Long

    @Query("SELECT * FROM alarm_events ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<AlarmEvent>

    @Query("DELETE FROM alarm_events")
    suspend fun deleteAllEvents()
}
