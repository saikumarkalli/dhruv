package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM calculation_history WHERE isInRecycleBin = 0 ORDER BY timestamp DESC")
    fun getActiveHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM calculation_history WHERE isInRecycleBin = 1 ORDER BY deletedTimestamp DESC")
    fun getRecycleBinHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Update
    suspend fun updateHistory(history: HistoryEntity)

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM calculation_history WHERE id IN (:ids)")
    suspend fun deleteMultipleHistoryByIds(ids: List<Long>)

    @Query("UPDATE calculation_history SET isInRecycleBin = 1, deletedTimestamp = :deletedTime WHERE id = :id")
    suspend fun moveToRecycleBin(id: Long, deletedTime: Long)

    @Query("UPDATE calculation_history SET isInRecycleBin = 1, deletedTimestamp = :deletedTime WHERE id IN (:ids)")
    suspend fun moveMultipleToRecycleBin(ids: List<Long>, deletedTime: Long)

    @Query("UPDATE calculation_history SET isInRecycleBin = 0 WHERE id = :id")
    suspend fun restoreFromRecycleBin(id: Long)

    @Query("DELETE FROM calculation_history WHERE isInRecycleBin = 1")
    suspend fun emptyRecycleBin()

    @Query("DELETE FROM calculation_history WHERE isInRecycleBin = 1 AND deletedTimestamp < :beforeTime")
    suspend fun autoRemoveRecycleBinOlderThan(beforeTime: Long)

    @Query("DELETE FROM calculation_history WHERE isInRecycleBin = 0")
    suspend fun clearActiveHistory()

    @Query("DELETE FROM calculation_history")
    suspend fun clearAllHistory()
}
