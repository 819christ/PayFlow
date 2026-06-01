package com.aurel.payflow.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history_entries WHERE status = 'PENDING'")
    suspend fun getPending(): List<HistoryEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry)

    @Update
    suspend fun update(entry: HistoryEntry)

    @Query("DELETE FROM history_entries")
    suspend fun clearAll()

    @Query("DELETE FROM history_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM history_entries WHERE timestamp = :timestamp AND sender = :sender)")
    suspend fun exists(timestamp: Long, sender: String): Boolean
}
