package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.drive.GoogleDriveService
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class AutoBackupSettings(
    val isAutoBackupEnabled: Boolean = true,
    val backupFrequency: String = "REALTIME", // REALTIME, HOURLY, DAILY
    val preferBlueOceanBias: Boolean = true,
    val highPainPointFilter: Boolean = true,
    val activeAiModel: String = "gemini-3.5-flash",
    val lastBackupTimestamp: Long = 0L,
    val lastBackupCount: Int = 0,
    val lastBackupFileName: String = "",
    val autoSyncToDrive: Boolean = true
)

data class BackupArchiveItem(
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val ideaCount: Int,
    val fileSizeFormatted: String
)

class AutoBackupManager(
    private val context: Context,
    private val driveService: GoogleDriveService? = null
) {
    private val prefs = context.getSharedPreferences("strat_analytics_autobackup_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AutoBackupSettings> = _settings.asStateFlow()

    private val backupDir: File by lazy {
        File(context.filesDir, "portfolio_backups").apply { mkdirs() }
    }

    private fun loadSettings(): AutoBackupSettings {
        return AutoBackupSettings(
            isAutoBackupEnabled = prefs.getBoolean("is_auto_backup_enabled", true),
            backupFrequency = prefs.getString("backup_frequency", "REALTIME") ?: "REALTIME",
            preferBlueOceanBias = prefs.getBoolean("prefer_blue_ocean_bias", true),
            highPainPointFilter = prefs.getBoolean("high_pain_point_filter", true),
            activeAiModel = prefs.getString("active_ai_model", "gemini-3.5-flash") ?: "gemini-3.5-flash",
            lastBackupTimestamp = prefs.getLong("last_backup_timestamp", 0L),
            lastBackupCount = prefs.getInt("last_backup_count", 0),
            lastBackupFileName = prefs.getString("last_backup_file_name", "") ?: "",
            autoSyncToDrive = prefs.getBoolean("auto_sync_to_drive", true)
        )
    }

    fun updateSettings(newSettings: AutoBackupSettings) {
        prefs.edit()
            .putBoolean("is_auto_backup_enabled", newSettings.isAutoBackupEnabled)
            .putString("backup_frequency", newSettings.backupFrequency)
            .putBoolean("prefer_blue_ocean_bias", newSettings.preferBlueOceanBias)
            .putBoolean("high_pain_point_filter", newSettings.highPainPointFilter)
            .putString("active_ai_model", newSettings.activeAiModel)
            .putLong("last_backup_timestamp", newSettings.lastBackupTimestamp)
            .putInt("last_backup_count", newSettings.lastBackupCount)
            .putString("last_backup_file_name", newSettings.lastBackupFileName)
            .putBoolean("auto_sync_to_drive", newSettings.autoSyncToDrive)
            .apply()
        _settings.value = newSettings
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        updateSettings(_settings.value.copy(isAutoBackupEnabled = enabled))
    }

    fun setPreferBlueOceanBias(prefer: Boolean) {
        updateSettings(_settings.value.copy(preferBlueOceanBias = prefer))
    }

    fun setActiveAiModel(model: String) {
        updateSettings(_settings.value.copy(activeAiModel = model))
    }

    /**
     * Automatically backs up the current list of ideas to persistent local storage
     * and triggers Drive sync if configured.
     */
    suspend fun performAutoBackup(ideas: List<BusinessIdea>, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val currentSettings = _settings.value
        if (!currentSettings.isAutoBackupEnabled && !force) {
            return@withContext false
        }

        try {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val now = System.currentTimeMillis()
            val timestampStr = dateFormat.format(Date(now))
            val backupFileName = "StratAnalytics_AutoBackup_$timestampStr.json"
            val backupFile = File(backupDir, backupFileName)

            val jsonArray = JSONArray()
            ideas.forEach { idea ->
                jsonArray.put(serializeIdea(idea))
            }

            val rootJson = JSONObject().apply {
                put("application", "Strat Analytics")
                put("version", "1.2")
                put("backupType", if (force) "MANUAL_TRIGGER" else "AUTOMATIC_PERSISTENCE")
                put("exportedAt", now)
                put("ideaCount", ideas.size)
                put("ideas", jsonArray)
            }

            // Write to latest snapshot file
            val latestFile = File(backupDir, "StratAnalytics_Latest_Vault.json")
            latestFile.writeText(rootJson.toString(2))

            // Write to timestamped historical archive
            backupFile.writeText(rootJson.toString(2))

            // Clean up old archives, keeping latest 8
            cleanOldArchives(maxKeep = 8)

            // Update state
            val updated = currentSettings.copy(
                lastBackupTimestamp = now,
                lastBackupCount = ideas.size,
                lastBackupFileName = backupFileName
            )
            withContext(Dispatchers.Main) {
                updateSettings(updated)
            }

            // Background trigger Google Drive backup if enabled
            if (currentSettings.autoSyncToDrive && driveService != null && driveService.isConnected()) {
                try {
                    driveService.savePortfolioBackupToDrive(ideas)
                } catch (_: Exception) {}
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun listBackupArchives(): List<BackupArchiveItem> {
        val files = backupDir.listFiles { file -> file.isFile && file.name.endsWith(".json") } ?: emptyArray()
        return files.sortedByDescending { it.lastModified() }.map { file ->
            val sizeKb = (file.length() / 1024.0).coerceAtLeast(0.1)
            val sizeFormatted = String.format(Locale.US, "%.1f KB", sizeKb)
            BackupArchiveItem(
                fileName = file.name,
                filePath = file.absolutePath,
                timestamp = file.lastModified(),
                ideaCount = countIdeasInFile(file),
                fileSizeFormatted = sizeFormatted
            )
        }
    }

    fun restoreIdeasFromBackupFile(file: File): List<BusinessIdea>? {
        return try {
            val content = file.readText()
            val root = JSONObject(content)
            val ideasArray = root.optJSONArray("ideas") ?: return null
            val restored = mutableListOf<BusinessIdea>()
            for (i in 0 until ideasArray.length()) {
                val obj = ideasArray.getJSONObject(i)
                restored.add(deserializeIdea(obj))
            }
            restored
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun cleanOldArchives(maxKeep: Int) {
        val files = backupDir.listFiles { file ->
            file.isFile && file.name.startsWith("StratAnalytics_AutoBackup_")
        } ?: return
        if (files.size > maxKeep) {
            val sorted = files.sortedBy { it.lastModified() }
            val toDelete = sorted.take(files.size - maxKeep)
            toDelete.forEach { it.delete() }
        }
    }

    private fun countIdeasInFile(file: File): Int {
        return try {
            val root = JSONObject(file.readText())
            root.optJSONArray("ideas")?.length() ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun serializeIdea(idea: BusinessIdea): JSONObject {
        return JSONObject().apply {
            put("id", idea.id)
            put("title", idea.title)
            put("tagline", idea.tagline)
            put("category", idea.category.name)
            put("marketType", idea.marketType.name)
            put("stage", idea.stage.name)
            put("isImmediatelyActionable", idea.isImmediatelyActionable)
            put("problemStatement", idea.problemStatement)
            put("solutionDescription", idea.solutionDescription)
            put("targetAudience", idea.targetAudience)
            put("whyNowTrigger", idea.whyNowTrigger)
            put("pricingModel", idea.pricingModel)
            put("monthlyPricePerCustomer", idea.monthlyPricePerCustomer)
            put("targetCustomersYear1", idea.targetCustomersYear1)
            put("estimatedMRR", idea.estimatedMRR)
            put("estimatedARR", idea.estimatedARR)
            put("projectedYear1Revenue", idea.projectedYear1Revenue)
            put("projectedYear3Revenue", idea.projectedYear3Revenue)
            put("pricingTiersSummary", idea.pricingTiersSummary)
            put("devComplexity", idea.devComplexity.name)
            put("weeksToMvp", idea.weeksToMvp)
            put("techStack", idea.techStack)
            put("estimatedMonthlyOperatingCost", idea.estimatedMonthlyOperatingCost)
            put("mvpFeatureScope", idea.mvpFeatureScope)
            put("competitionLevel", idea.competitionLevel)
            put("topCompetitors", idea.topCompetitors)
            put("competitorWeaknesses", idea.competitorWeaknesses)
            put("unfairAdvantageMoat", idea.unfairAdvantageMoat)
            put("dataSourceType", idea.dataSourceType.name)
            put("sourceUrlOrRef", idea.sourceUrlOrRef)
            put("sourceQuote", idea.sourceQuote)
            put("willingnessToPayRating", idea.willingnessToPayRating)
            put("readyBuyerProfiles", idea.readyBuyerProfiles)
            put("actionableNextSteps", idea.actionableNextSteps)
            put("opportunityScore", idea.opportunityScore)
            put("notes", idea.notes)
            put("updatedAt", idea.updatedAt)
            put("createdAt", idea.createdAt)
        }
    }

    private fun deserializeIdea(json: JSONObject): BusinessIdea {
        return BusinessIdea(
            id = json.optLong("id", 0L),
            title = json.optString("title", "Untitled Venture"),
            tagline = json.optString("tagline", ""),
            category = try { BusinessModelCategory.valueOf(json.optString("category", "B2B_SAAS")) } catch (_: Exception) { BusinessModelCategory.B2B_SAAS },
            marketType = try { MarketType.valueOf(json.optString("marketType", "BLUE_OCEAN")) } catch (_: Exception) { MarketType.BLUE_OCEAN },
            stage = try { PipelineStage.valueOf(json.optString("stage", "DISCOVERED")) } catch (_: Exception) { PipelineStage.DISCOVERED },
            isImmediatelyActionable = json.optBoolean("isImmediatelyActionable", false),
            problemStatement = json.optString("problemStatement", ""),
            solutionDescription = json.optString("solutionDescription", ""),
            targetAudience = json.optString("targetAudience", ""),
            whyNowTrigger = json.optString("whyNowTrigger", ""),
            pricingModel = json.optString("pricingModel", "Monthly Subscription"),
            monthlyPricePerCustomer = json.optDouble("monthlyPricePerCustomer", 99.0),
            targetCustomersYear1 = json.optInt("targetCustomersYear1", 50),
            estimatedMRR = json.optDouble("estimatedMRR", 4950.0),
            estimatedARR = json.optDouble("estimatedARR", 59400.0),
            projectedYear1Revenue = json.optDouble("projectedYear1Revenue", 50000.0),
            projectedYear3Revenue = json.optDouble("projectedYear3Revenue", 250000.0),
            pricingTiersSummary = json.optString("pricingTiersSummary", "Starter: $49/mo | Pro: $99/mo"),
            devComplexity = try { DevComplexity.valueOf(json.optString("devComplexity", "MEDIUM_3_6_WEEKS")) } catch (_: Exception) { DevComplexity.MEDIUM_3_6_WEEKS },
            weeksToMvp = json.optInt("weeksToMvp", 4),
            techStack = json.optString("techStack", "Kotlin, Jetpack Compose, Room, Gemini API"),
            estimatedMonthlyOperatingCost = json.optDouble("estimatedMonthlyOperatingCost", 80.0),
            mvpFeatureScope = json.optString("mvpFeatureScope", "1. Core feature 2. Dashboard 3. Export"),
            competitionLevel = json.optString("competitionLevel", "Low"),
            topCompetitors = json.optString("topCompetitors", ""),
            competitorWeaknesses = json.optString("competitorWeaknesses", ""),
            unfairAdvantageMoat = json.optString("unfairAdvantageMoat", ""),
            dataSourceType = try { DataSourceType.valueOf(json.optString("dataSourceType", "FOUNDER_INTERVIEW")) } catch (_: Exception) { DataSourceType.FOUNDER_INTERVIEW },
            sourceUrlOrRef = json.optString("sourceUrlOrRef", ""),
            sourceQuote = json.optString("sourceQuote", ""),
            willingnessToPayRating = json.optInt("willingnessToPayRating", 4),
            readyBuyerProfiles = json.optString("readyBuyerProfiles", ""),
            actionableNextSteps = json.optString("actionableNextSteps", "1. Validate with 3 buyers\n2. Ship prototype"),
            opportunityScore = json.optInt("opportunityScore", 80),
            notes = json.optString("notes", ""),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
