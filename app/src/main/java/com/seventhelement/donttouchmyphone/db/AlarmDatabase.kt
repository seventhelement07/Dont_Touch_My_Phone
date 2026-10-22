package com.seventhelement.donttouchmyphone.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AlarmEvent::class], version = 2, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmHistoryDao(): AlarmHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        fun getInstance(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_database"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
