package com.nova.browser.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TabEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun historyDao(): HistoryDao
}