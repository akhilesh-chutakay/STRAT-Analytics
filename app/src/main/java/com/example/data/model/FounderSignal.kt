package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "founder_signals")
data class FounderSignal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val problemTitle: String,
    val rawProblemQuote: String,
    val founderOrCompanyName: String,
    val industryNiche: String,
    val sourceType: DataSourceType,
    val sourceLinkOrHandle: String,
    val estimatedWillingnessToPayMonthly: Double,
    val urgencyLevel: String = "High", // Critical, High, Moderate
    val suggestedToolIdea: String,
    val linkedIdeaId: Long? = null,
    val verifiedAtTimestamp: Long = System.currentTimeMillis()
)
