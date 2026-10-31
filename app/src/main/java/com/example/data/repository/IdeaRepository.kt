package com.example.data.repository

import com.example.data.backup.AutoBackupManager
import com.example.data.local.IdeaDao
import com.example.data.local.SeedData
import com.example.data.local.SignalDao
import com.example.data.model.BusinessIdea
import com.example.data.model.BusinessModelCategory
import com.example.data.model.DevComplexity
import com.example.data.model.FounderSignal
import com.example.data.model.MarketType
import com.example.data.model.PipelineStage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class PortfolioStats(
    val totalIdeasCount: Int = 0,
    val immediatelyActionableCount: Int = 0,
    val totalProjectedARR: Double = 0.0,
    val totalProjectedMRR: Double = 0.0,
    val averageWeeksToMvp: Double = 0.0,
    val blueOceanCount: Int = 0,
    val redOceanCount: Int = 0,
    val validatingOrMvpCount: Int = 0,
    val averageOpportunityScore: Int = 0,
    val categoryDistribution: Map<BusinessModelCategory, Int> = emptyMap(),
    val marketTypeDistribution: Map<MarketType, Int> = emptyMap()
)

class IdeaRepository(
    private val ideaDao: IdeaDao,
    private val signalDao: SignalDao,
    val autoBackupManager: AutoBackupManager? = null
) {
    val allIdeas: Flow<List<BusinessIdea>> = ideaDao.getAllIdeas()
    val actionableIdeas: Flow<List<BusinessIdea>> = ideaDao.getImmediatelyActionableIdeas()
    val allSignals: Flow<List<FounderSignal>> = signalDao.getAllSignals()

    val portfolioStats: Flow<PortfolioStats> = allIdeas.map { ideas ->
        if (ideas.isEmpty()) {
            PortfolioStats()
        } else {
            val totalARR = ideas.sumOf { it.estimatedARR }
            val totalMRR = ideas.sumOf { it.estimatedMRR }
            val actionableCount = ideas.count { it.isImmediatelyActionable }
            val blueOceanCount = ideas.count { it.marketType == MarketType.BLUE_OCEAN }
            val redOceanCount = ideas.count { it.marketType == MarketType.RED_OCEAN }
            val avgWeeks = ideas.map { it.weeksToMvp }.average()
            val avgScore = ideas.map { it.opportunityScore }.average().toInt()
            val activeStages = ideas.count {
                it.stage == PipelineStage.VALIDATING ||
                it.stage == PipelineStage.PROTOTYPING ||
                it.stage == PipelineStage.MVP_READY ||
                it.stage == PipelineStage.LAUNCHED
            }

            val categoryMap = ideas.groupBy { it.category }.mapValues { it.value.size }
            val marketMap = ideas.groupBy { it.marketType }.mapValues { it.value.size }

            PortfolioStats(
                totalIdeasCount = ideas.size,
                immediatelyActionableCount = actionableCount,
                totalProjectedARR = totalARR,
                totalProjectedMRR = totalMRR,
                averageWeeksToMvp = if (avgWeeks.isNaN()) 0.0 else avgWeeks,
                blueOceanCount = blueOceanCount,
                redOceanCount = redOceanCount,
                validatingOrMvpCount = activeStages,
                averageOpportunityScore = avgScore,
                categoryDistribution = categoryMap,
                marketTypeDistribution = marketMap
            )
        }
    }

    suspend fun getIdeaByIdDirect(id: Long): BusinessIdea? = ideaDao.getIdeaByIdDirect(id)

    fun getIdeaById(id: Long): Flow<BusinessIdea?> = ideaDao.getIdeaById(id)

    suspend fun saveIdea(idea: BusinessIdea): Long {
        val calculatedScore = calculateOpportunityScore(idea)
        val calculatedMRR = idea.monthlyPricePerCustomer * (if (idea.targetCustomersYear1 > 0) idea.targetCustomersYear1 else 50)
        val calculatedARR = calculatedMRR * 12.0
        val preparedIdea = idea.copy(
            opportunityScore = calculatedScore,
            estimatedMRR = if (idea.estimatedMRR <= 0) calculatedMRR else idea.estimatedMRR,
            estimatedARR = if (idea.estimatedARR <= 0) calculatedARR else idea.estimatedARR,
            updatedAt = System.currentTimeMillis()
        )

        val id = if (preparedIdea.id == 0L) {
            ideaDao.insertIdea(preparedIdea)
        } else {
            ideaDao.updateIdea(preparedIdea)
            preparedIdea.id
        }

        triggerAutoBackup()
        return id
    }

    suspend fun insertMultipleIdeas(ideas: List<BusinessIdea>) {
        val preparedList = ideas.map { idea ->
            val calculatedScore = calculateOpportunityScore(idea)
            val calculatedMRR = idea.monthlyPricePerCustomer * (if (idea.targetCustomersYear1 > 0) idea.targetCustomersYear1 else 50)
            val calculatedARR = calculatedMRR * 12.0
            idea.copy(
                opportunityScore = calculatedScore,
                estimatedMRR = if (idea.estimatedMRR <= 0) calculatedMRR else idea.estimatedMRR,
                estimatedARR = if (idea.estimatedARR <= 0) calculatedARR else idea.estimatedARR,
                updatedAt = System.currentTimeMillis()
            )
        }
        ideaDao.insertIdeas(preparedList)
        triggerAutoBackup()
    }

    suspend fun deleteIdea(idea: BusinessIdea) {
        ideaDao.deleteIdea(idea)
        triggerAutoBackup()
    }

    suspend fun deleteIdeaById(id: Long) {
        ideaDao.deleteIdeaById(id)
        triggerAutoBackup()
    }

    suspend fun toggleActionable(id: Long) {
        val idea = ideaDao.getIdeaByIdDirect(id) ?: return
        val updated = idea.copy(
            isImmediatelyActionable = !idea.isImmediatelyActionable,
            updatedAt = System.currentTimeMillis()
        )
        ideaDao.updateIdea(updated)
        triggerAutoBackup()
    }

    suspend fun updateStage(id: Long, newStage: PipelineStage) {
        val idea = ideaDao.getIdeaByIdDirect(id) ?: return
        val updated = idea.copy(
            stage = newStage,
            updatedAt = System.currentTimeMillis()
        )
        ideaDao.updateIdea(updated)
        triggerAutoBackup()
    }

    suspend fun triggerAutoBackup(force: Boolean = false) {
        autoBackupManager?.let { manager ->
            try {
                val currentList = allIdeas.first()
                manager.performAutoBackup(currentList, force = force)
            } catch (_: Exception) {}
        }
    }

    suspend fun restoreIdeasFromList(restoredIdeas: List<BusinessIdea>) {
        if (restoredIdeas.isNotEmpty()) {
            ideaDao.insertIdeas(restoredIdeas)
            triggerAutoBackup(force = true)
        }
    }

    suspend fun ensureDefaultDataSeeded() {
        if (ideaDao.getCount() == 0) {
            ideaDao.insertIdeas(SeedData.getInitialIdeas())
        }
        if (signalDao.getCount() == 0) {
            signalDao.insertSignals(SeedData.getInitialSignals())
        }
    }

    suspend fun saveSignal(signal: FounderSignal): Long {
        return if (signal.id == 0L) {
            signalDao.insertSignal(signal)
        } else {
            signalDao.updateSignal(signal)
            signal.id
        }
    }

    suspend fun deleteSignal(signal: FounderSignal) {
        signalDao.deleteSignal(signal)
    }

    fun calculateOpportunityScore(idea: BusinessIdea): Int {
        var score = 40

        // Willingness to Pay factor (1-5 stars -> up to +30 points)
        score += (idea.willingnessToPayRating.coerceIn(1, 5) * 6)

        // Speed to MVP factor (<3 weeks = +15, 4-6 weeks = +5, >6 weeks = -5)
        score += when {
            idea.weeksToMvp <= 2 -> 15
            idea.weeksToMvp <= 4 -> 10
            idea.weeksToMvp <= 8 -> 0
            else -> -10
        }

        // Market type bonus
        if (idea.marketType == MarketType.BLUE_OCEAN) {
            score += 10
        }

        // Immediately actionable bonus
        if (idea.isImmediatelyActionable) {
            score += 10
        }

        // Real data source bonus
        if (idea.sourceQuote.isNotBlank()) {
            score += 8
        }

        // Revenue potential bonus (ARR > $100k)
        if (idea.estimatedARR >= 100000) {
            score += 10
        }

        return score.coerceIn(15, 99)
    }
}
