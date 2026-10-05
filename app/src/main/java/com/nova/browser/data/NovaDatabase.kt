package com.nova.browser.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TabEntity::class,
        HistoryEntity::class,
        BookmarkEntity::class,
        DownloadEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun downloadDao(): DownloadDao
}