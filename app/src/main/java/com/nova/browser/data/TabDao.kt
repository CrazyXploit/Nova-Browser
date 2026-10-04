package com.nova.browser.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs ORDER BY lastActive DESC")
    fun observeAll(): Flow<List<TabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun delete(id: String)
}