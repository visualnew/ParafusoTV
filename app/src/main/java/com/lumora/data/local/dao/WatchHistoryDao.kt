package com.lumora.data.local.dao

import androidx.room.*
import com.lumora.data.local.entity.WatchHistoryEntity

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY lastWatchedAt DESC LIMIT 50")
    suspend fun getRecentForProfile(profileId: Int): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY lastWatchedAt DESC")
    suspend fun getAllForProfile(profileId: Int): List<WatchHistoryEntity>

    /** Explicitly global: a device backup must round-trip every profile's history. */
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    suspend fun getAllForBackup(): List<WatchHistoryEntity>

    /** Explicitly global: used only to decide whether a backup restore needs confirmation. */
    @Query("SELECT COUNT(*) FROM watch_history")
    suspend fun countAllForBackup(): Int

    @Query("DELETE FROM watch_history WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WatchHistoryEntity)
}
