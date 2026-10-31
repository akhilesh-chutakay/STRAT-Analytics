package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BusinessIdea
import com.example.data.model.MarketType
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Query("SELECT * FROM business_ideas ORDER BY opportunityScore DESC, updatedAt DESC")
    fun getAllIdeas(): Flow<List<BusinessIdea>>

    @Query("SELECT * FROM business_ideas WHERE isImmediatelyActionable = 1 ORDER BY opportunityScore DESC, updatedAt DESC")
    fun getImmediatelyActionableIdeas(): Flow<List<BusinessIdea>>

    @Query("SELECT * FROM business_ideas WHERE marketType = :marketType ORDER BY opportunityScore DESC")
    fun getIdeasByMarketType(marketType: MarketType): Flow<List<BusinessIdea>>

    @Query("SELECT * FROM business_ideas WHERE id = :id")
    fun getIdeaById(id: Long): Flow<BusinessIdea?>

    @Query("SELECT * FROM business_ideas WHERE id = :id")
    suspend fun getIdeaByIdDirect(id: Long): BusinessIdea?

    @Query("SELECT COUNT(*) FROM business_ideas")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdea(idea: BusinessIdea): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdeas(ideas: List<BusinessIdea>)

    @Update
    suspend fun updateIdea(idea: BusinessIdea)

    @Delete
    suspend fun deleteIdea(idea: BusinessIdea)

    @Query("DELETE FROM business_ideas WHERE id = :id")
    suspend fun deleteIdeaById(id: Long)
}
