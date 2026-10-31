package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FounderSignal
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalDao {
    @Query("SELECT * FROM founder_signals ORDER BY verifiedAtTimestamp DESC")
    fun getAllSignals(): Flow<List<FounderSignal>>

    @Query("SELECT COUNT(*) FROM founder_signals")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: FounderSignal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignals(signals: List<FounderSignal>)

    @Update
    suspend fun updateSignal(signal: FounderSignal)

    @Delete
    suspend fun deleteSignal(signal: FounderSignal)

    @Query("DELETE FROM founder_signals WHERE id = :id")
    suspend fun deleteSignalById(id: Long)
}
