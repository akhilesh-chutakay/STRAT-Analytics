package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.AutoBackupManager
import com.example.data.backup.AutoBackupSettings
import com.example.data.backup.BackupArchiveItem
import com.example.data.drive.DriveFileItem
import com.example.data.drive.DriveSyncResult
import com.example.data.drive.GoogleDriveService
import com.example.data.model.*
import com.example.data.remote.GeminiApiClient
import com.example.data.repository.IdeaRepository
import com.example.data.repository.PortfolioStats
import com.example.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class IdeaSortOption(val displayName: String) {
    OPPORTUNITY_SCORE("Highest Opportunity"),
    PROJECTED_ARR("Highest ARR"),
    FASTEST_MVP("Fastest to MVP"),
    BUYER_WILLINGNESS("Highest Buyer Demand"),
    RECENTLY_UPDATED("Recently Updated")
}

sealed class DriveSyncUiState {
    object Idle : DriveSyncUiState()
    data class Syncing(val message: String = "Saving locally and backing up to Google Drive...") : DriveSyncUiState()
    data class Success(val result: DriveSyncResult) : DriveSyncUiState()
    data class Error(val message: String) : DriveSyncUiState()
}

data class AiAnalysisState(
    val isLoading: Boolean = false,
    val resultText: String? = null,
    val errorMessage: String? = null,
    val currentMode: String = "COMPETITOR_TEARDOWN"
)

data class AiExtractionState(
    val isLoading: Boolean = false,
    val extractedBlueprint: String? = null,
    val errorMessage: String? = null
)

data class MarketUpdateState(
    val isScanning: Boolean = false,
    val newlyAddedIdeas: List<BusinessIdea> = emptyList(),
    val errorMessage: String? = null,
    val completedMessage: String? = null,
    val scanMode: String = "BLUE_OCEAN", // "BLUE_OCEAN", "ZERO_INVESTMENT", or "CATEGORY_RESEARCH"
    val targetCategoryName: String? = null
)

data class FilterCriteria(
    val query: String = "",
    val market: MarketType? = null,
    val category: BusinessModelCategory? = null,
    val stage: PipelineStage? = null,
    val sort: IdeaSortOption = IdeaSortOption.OPPORTUNITY_SCORE,
    val zeroInvestmentOnly: Boolean = false
)

class IdeaViewModel(
    private val repository: IdeaRepository,
    val driveService: GoogleDriveService? = null,
    private val userProfileRepository: UserProfileRepository? = null,
    val autoBackupManager: AutoBackupManager? = null
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.ensureDefaultDataSeeded()
            refreshDriveFiles()
            // Initial auto-backup sync
            repository.triggerAutoBackup(force = false)
        }
    }

    // Auto-Backup Settings
    val backupSettings: StateFlow<AutoBackupSettings> = autoBackupManager?.settings
        ?: MutableStateFlow(AutoBackupSettings()).asStateFlow()

    // Market Analysis Update State
    val marketUpdateState = MutableStateFlow(MarketUpdateState())

    // User Profile State
    val userProfile: StateFlow<UserProfile> = userProfileRepository?.userProfile
        ?: MutableStateFlow(UserProfile()).asStateFlow()

    fun updateUserProfile(profile: UserProfile) {
        userProfileRepository?.updateProfile(profile)
    }

    fun updateAvatar(avatarUrl: String) {
        userProfileRepository?.updateAvatar(avatarUrl)
    }

    fun uploadAvatar(uri: android.net.Uri, onComplete: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val savedPath = userProfileRepository?.saveUploadedImageFromUri(uri)
            onComplete(savedPath)
        }
    }

    // Google Drive & Local Sync States
    val driveSyncState = MutableStateFlow<DriveSyncUiState>(DriveSyncUiState.Idle)
    val driveFiles = MutableStateFlow<List<DriveFileItem>>(emptyList())
    val isDriveConnected = MutableStateFlow(driveService?.isConnected() ?: true)

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedMarketFilter = MutableStateFlow<MarketType?>(null)
    val selectedCategoryFilter = MutableStateFlow<BusinessModelCategory?>(null)
    val selectedStageFilter = MutableStateFlow<PipelineStage?>(null)
    val selectedZeroInvestmentFilter = MutableStateFlow(false)
    val sortOption = MutableStateFlow(IdeaSortOption.OPPORTUNITY_SCORE)

    val portfolioStats: StateFlow<PortfolioStats> = repository.portfolioStats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PortfolioStats()
        )

    val allSignals: StateFlow<List<FounderSignal>> = repository.allSignals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val immediatelyActionableIdeas: StateFlow<List<BusinessIdea>> = repository.actionableIdeas
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val zeroCostIdeas: StateFlow<List<BusinessIdea>> = repository.allIdeas
        .map { list ->
            list.filter { it.isZeroInvestment || it.devComplexity == DevComplexity.ZERO_CODE_NO_CODE || it.estimatedMonthlyOperatingCost <= 20.0 }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val filterCriteria: Flow<FilterCriteria> = combine(
        combine(searchQuery, selectedMarketFilter, selectedCategoryFilter) { q, m, c -> Triple(q, m, c) },
        combine(selectedStageFilter, selectedZeroInvestmentFilter, sortOption) { s, z, so -> Triple(s, z, so) }
    ) { (query, market, category), (stage, zeroInvest, sort) ->
        FilterCriteria(query, market, category, stage, sort, zeroInvest)
    }

    val filteredIdeas: StateFlow<List<BusinessIdea>> = combine(
        repository.allIdeas,
        filterCriteria
    ) { ideas, criteria ->
        ideas.filter { idea ->
            val matchesQuery = criteria.query.isBlank() ||
                idea.title.contains(criteria.query, ignoreCase = true) ||
                idea.tagline.contains(criteria.query, ignoreCase = true) ||
                idea.problemStatement.contains(criteria.query, ignoreCase = true) ||
                idea.targetAudience.contains(criteria.query, ignoreCase = true) ||
                idea.sourceQuote.contains(criteria.query, ignoreCase = true) ||
                idea.zeroCostPlaybook.contains(criteria.query, ignoreCase = true) ||
                idea.zeroCostStack.contains(criteria.query, ignoreCase = true)

            val matchesMarket = criteria.market == null || idea.marketType == criteria.market
            val matchesCategory = criteria.category == null || idea.category == criteria.category
            val matchesStage = criteria.stage == null || idea.stage == criteria.stage
            val matchesZeroInvest = !criteria.zeroInvestmentOnly ||
                (idea.isZeroInvestment || idea.devComplexity == DevComplexity.ZERO_CODE_NO_CODE || idea.estimatedMonthlyOperatingCost <= 20.0)

            matchesQuery && matchesMarket && matchesCategory && matchesStage && matchesZeroInvest
        }.let { filtered ->
            when (criteria.sort) {
                IdeaSortOption.OPPORTUNITY_SCORE -> filtered.sortedByDescending { it.opportunityScore }
                IdeaSortOption.PROJECTED_ARR -> filtered.sortedByDescending { it.estimatedARR }
                IdeaSortOption.FASTEST_MVP -> filtered.sortedBy { it.weeksToMvp }
                IdeaSortOption.BUYER_WILLINGNESS -> filtered.sortedByDescending { it.willingnessToPayRating }
                IdeaSortOption.RECENTLY_UPDATED -> filtered.sortedByDescending { it.updatedAt }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // AI Analysis State
    val aiAnalysisState = MutableStateFlow(AiAnalysisState())
    val aiExtractionState = MutableStateFlow(AiExtractionState())

    fun selectMarketFilter(market: MarketType?) {
        selectedMarketFilter.value = market
    }

    fun selectCategoryFilter(category: BusinessModelCategory?) {
        selectedCategoryFilter.value = category
    }

    fun selectStageFilter(stage: PipelineStage?) {
        selectedStageFilter.value = stage
    }

    fun selectZeroInvestmentFilter(enabled: Boolean) {
        selectedZeroInvestmentFilter.value = enabled
    }

    fun setSort(sort: IdeaSortOption) {
        sortOption.value = sort
    }

    fun setSearch(query: String) {
        searchQuery.value = query
    }

    fun resetFilters() {
        searchQuery.value = ""
        selectedMarketFilter.value = null
        selectedCategoryFilter.value = null
        selectedStageFilter.value = null
        selectedZeroInvestmentFilter.value = false
        sortOption.value = IdeaSortOption.OPPORTUNITY_SCORE
    }

    fun toggleActionable(id: Long) {
        viewModelScope.launch {
            repository.toggleActionable(id)
        }
    }

    fun updateStage(id: Long, newStage: PipelineStage) {
        viewModelScope.launch {
            repository.updateStage(id, newStage)
        }
    }

    fun saveIdea(idea: BusinessIdea, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveIdea(idea)
            onComplete(id)
        }
    }

    fun deleteIdea(idea: BusinessIdea) {
        viewModelScope.launch {
            repository.deleteIdea(idea)
        }
    }

    fun deleteIdeaById(id: Long) {
        viewModelScope.launch {
            repository.deleteIdeaById(id)
        }
    }

    fun saveSignal(signal: FounderSignal, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveSignal(signal)
            onComplete(id)
        }
    }

    fun deleteSignal(signal: FounderSignal) {
        viewModelScope.launch {
            repository.deleteSignal(signal)
        }
    }

    // Convert Founder Signal into a new Idea draft
    fun convertSignalToIdea(signal: FounderSignal, onIdeaCreated: (BusinessIdea) -> Unit) {
        val newIdea = BusinessIdea(
            title = signal.suggestedToolIdea.ifBlank { "Tool for ${signal.problemTitle}" },
            tagline = "Directly solving: ${signal.problemTitle}",
            category = BusinessModelCategory.B2B_SAAS,
            marketType = MarketType.BLUE_OCEAN,
            stage = PipelineStage.VALIDATING,
            isImmediatelyActionable = signal.urgencyLevel == "Critical" || signal.urgencyLevel == "High",
            problemStatement = signal.rawProblemQuote,
            solutionDescription = signal.suggestedToolIdea,
            targetAudience = "${signal.founderOrCompanyName} (${signal.industryNiche})",
            whyNowTrigger = "Verified unaddressed founder pain point with immediate budget intent.",
            monthlyPricePerCustomer = if (signal.estimatedWillingnessToPayMonthly > 0) signal.estimatedWillingnessToPayMonthly else 99.0,
            targetCustomersYear1 = 60,
            estimatedMRR = (if (signal.estimatedWillingnessToPayMonthly > 0) signal.estimatedWillingnessToPayMonthly else 99.0) * 60,
            estimatedARR = (if (signal.estimatedWillingnessToPayMonthly > 0) signal.estimatedWillingnessToPayMonthly else 99.0) * 60 * 12,
            dataSourceType = signal.sourceType,
            sourceUrlOrRef = signal.sourceLinkOrHandle,
            sourceQuote = signal.rawProblemQuote,
            willingnessToPayRating = if (signal.urgencyLevel == "Critical") 5 else 4,
            readyBuyerProfiles = "${signal.industryNiche} with verified willingness to pay ~$${signal.estimatedWillingnessToPayMonthly.toInt()}/mo"
        )
        onIdeaCreated(newIdea)
    }

    fun runGeminiAnalysis(idea: BusinessIdea, mode: String) {
        aiAnalysisState.value = AiAnalysisState(isLoading = true, currentMode = mode)
        viewModelScope.launch {
            val result = GeminiApiClient.analyzeBusinessIdea(
                title = idea.title,
                problem = idea.problemStatement,
                solution = idea.solutionDescription,
                category = idea.category.displayName,
                marketType = idea.marketType.displayName,
                analysisMode = mode
            )
            result.fold(
                onSuccess = { responseText ->
                    aiAnalysisState.value = AiAnalysisState(
                        isLoading = false,
                        resultText = responseText,
                        currentMode = mode
                    )
                },
                onFailure = { error ->
                    aiAnalysisState.value = AiAnalysisState(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to generate analysis.",
                        currentMode = mode
                    )
                }
            )
        }
    }

    fun clearAiAnalysis() {
        aiAnalysisState.value = AiAnalysisState()
    }

    fun extractIdeaWithAi(rawQuote: String, source: String) {
        aiExtractionState.value = AiExtractionState(isLoading = true)
        viewModelScope.launch {
            val result = GeminiApiClient.extractIdeaFromProblemQuote(rawQuote, source)
            result.fold(
                onSuccess = { blueprint ->
                    aiExtractionState.value = AiExtractionState(
                        isLoading = false,
                        extractedBlueprint = blueprint
                    )
                },
                onFailure = { error ->
                    aiExtractionState.value = AiExtractionState(
                        isLoading = false,
                        errorMessage = error.message ?: "Could not extract idea."
                    )
                }
            )
        }
    }

    fun clearAiExtraction() {
        aiExtractionState.value = AiExtractionState()
    }

    // ==========================================
    // GOOGLE DRIVE & LOCAL STORAGE IDEA SAVER
    // ==========================================

    fun saveIdeaLocallyAndToDrive(
        idea: BusinessIdea,
        asMarkdown: Boolean = false,
        onComplete: (Long, DriveSyncResult) -> Unit = { _, _ -> }
    ) {
        driveSyncState.value = DriveSyncUiState.Syncing("Persisting to local database & uploading to Google Drive...")
        viewModelScope.launch {
            try {
                // 1. Save locally to Room database first
                val savedId = repository.saveIdea(idea)
                val directIdea = repository.getIdeaByIdDirect(savedId) ?: idea.copy(id = savedId)

                // 2. Save & sync to Google Drive
                val syncResult = if (driveService != null) {
                    driveService.saveIdeaToDrive(directIdea, asMarkdown)
                } else {
                    DriveSyncResult(
                        success = true,
                        fileId = "local_stored_${savedId}",
                        fileName = "iAStrat_Idea_${savedId}.json",
                        webViewLink = "https://drive.google.com",
                        message = "Saved to local on-device database",
                        isLocalFallback = true
                    )
                }

                // 3. Update idea in Room with Drive Sync status
                if (syncResult.success) {
                    val updatedWithDrive = directIdea.copy(
                        isDriveSynced = true,
                        driveFileId = syncResult.fileId,
                        lastDriveSyncTimestamp = syncResult.timestamp
                    )
                    repository.saveIdea(updatedWithDrive)
                }

                driveSyncState.value = DriveSyncUiState.Success(syncResult)
                refreshDriveFiles()
                onComplete(savedId, syncResult)
            } catch (e: Exception) {
                driveSyncState.value = DriveSyncUiState.Error(e.message ?: "Failed to save idea to Google Drive.")
            }
        }
    }

    fun backupPortfolioToDrive(onComplete: (DriveSyncResult) -> Unit = {}) {
        driveSyncState.value = DriveSyncUiState.Syncing("Backing up entire venture portfolio to Google Drive...")
        viewModelScope.launch {
            try {
                val allList = repository.allIdeas.first()
                val result = if (driveService != null) {
                    driveService.savePortfolioBackupToDrive(allList)
                } else {
                    DriveSyncResult(
                        success = true,
                        fileId = "portfolio_local_${System.currentTimeMillis()}",
                        fileName = "iAStrat_Portfolio_Backup.json",
                        webViewLink = "https://drive.google.com",
                        message = "Portfolio saved locally",
                        isLocalFallback = true
                    )
                }
                driveSyncState.value = DriveSyncUiState.Success(result)
                refreshDriveFiles()
                onComplete(result)
            } catch (e: Exception) {
                driveSyncState.value = DriveSyncUiState.Error(e.message ?: "Failed to backup portfolio.")
            }
        }
    }

    fun refreshDriveFiles() {
        if (driveService == null) return
        viewModelScope.launch {
            val list = driveService.listDriveFiles()
            driveFiles.value = list
        }
    }

    fun clearDriveSyncState() {
        driveSyncState.value = DriveSyncUiState.Idle
    }

    // ==========================================
    // MARKET RE-ANALYSIS & BLUE OCEAN DISCOVERY
    // ==========================================

    /**
     * Re-analyzes the market landscape focusing on Blue Ocean opportunities
     * and real founder/operator in-need bottlenecks.
     * Appends ONLY new ideas, leaving all existing ideas intact.
     */
    fun updateMarketIdeas(onComplete: (List<BusinessIdea>) -> Unit = {}) {
        marketUpdateState.value = MarketUpdateState(isScanning = true, scanMode = "BLUE_OCEAN")
        viewModelScope.launch {
            try {
                val existingList = repository.allIdeas.first()
                val existingTitles = existingList.map { it.title }
                val preferBlueOcean = backupSettings.value.preferBlueOceanBias
                val activeModel = backupSettings.value.activeAiModel

                val result = GeminiApiClient.discoverBlueOceanMarketIdeas(
                    existingIdeaTitles = existingTitles,
                    preferBlueOcean = preferBlueOcean,
                    activeModel = activeModel,
                    count = 3
                )

                result.fold(
                    onSuccess = { newIdeas ->
                        if (newIdeas.isNotEmpty()) {
                            // Insert only new ideas; existing ones remain intact
                            repository.insertMultipleIdeas(newIdeas)
                            marketUpdateState.value = MarketUpdateState(
                                isScanning = false,
                                newlyAddedIdeas = newIdeas,
                                completedMessage = "Synthesized & added ${newIdeas.size} new Blue Ocean venture opportunities to your portfolio.",
                                scanMode = "BLUE_OCEAN"
                            )
                            onComplete(newIdeas)
                        } else {
                            marketUpdateState.value = MarketUpdateState(
                                isScanning = false,
                                errorMessage = "No new unique ideas identified in this market scan.",
                                scanMode = "BLUE_OCEAN"
                            )
                        }
                    },
                    onFailure = { error ->
                        marketUpdateState.value = MarketUpdateState(
                            isScanning = false,
                            errorMessage = error.message ?: "Market scan encountered an error.",
                            scanMode = "BLUE_OCEAN"
                        )
                    }
                )
            } catch (e: Exception) {
                marketUpdateState.value = MarketUpdateState(
                    isScanning = false,
                    errorMessage = e.message ?: "Market re-analysis failed.",
                    scanMode = "BLUE_OCEAN"
                )
            }
        }
    }

    /**
     * Deep-scans the global internet across Reddit, X, Upwork, Product Hunt, Chrome Store, and Indie Hackers
     * to discover hyper-actionable $0 Upfront Investment & Simple Cashflow business ideas that require
     * no/low extensive coding and generate fast recurring cashflow.
     */
    fun scanInternetForZeroInvestmentOpportunities(onComplete: (List<BusinessIdea>) -> Unit = {}) {
        marketUpdateState.value = MarketUpdateState(isScanning = true, scanMode = "ZERO_INVESTMENT")
        viewModelScope.launch {
            try {
                val existingList = repository.allIdeas.first()
                val existingTitles = existingList.map { it.title }
                val activeModel = backupSettings.value.activeAiModel

                val result = GeminiApiClient.scanInternetForZeroInvestmentIdeas(
                    existingIdeaTitles = existingTitles,
                    count = 3,
                    activeModel = activeModel
                )

                result.fold(
                    onSuccess = { newIdeas ->
                        if (newIdeas.isNotEmpty()) {
                            repository.insertMultipleIdeas(newIdeas)
                            marketUpdateState.value = MarketUpdateState(
                                isScanning = false,
                                newlyAddedIdeas = newIdeas,
                                completedMessage = "Discovered & appended ${newIdeas.size} zero-investment cashflow ideas analyzed from global internet signals.",
                                scanMode = "ZERO_INVESTMENT"
                            )
                            onComplete(newIdeas)
                        } else {
                            marketUpdateState.value = MarketUpdateState(
                                isScanning = false,
                                errorMessage = "No new zero-investment opportunities discovered at this time.",
                                scanMode = "ZERO_INVESTMENT"
                            )
                        }
                    },
                    onFailure = { error ->
                        marketUpdateState.value = MarketUpdateState(
                             isScanning = false,
                            errorMessage = error.message ?: "Internet market intelligence scan failed.",
                            scanMode = "ZERO_INVESTMENT"
                        )
                    }
                )
            } catch (e: Exception) {
                marketUpdateState.value = MarketUpdateState(
                    isScanning = false,
                    errorMessage = e.message ?: "Failed to complete zero-investment internet scan.",
                    scanMode = "ZERO_INVESTMENT"
                )
            }
        }
    }

    /**
     * Conducts dedicated deep market intelligence research specifically for a chosen Category.
     * Inserts new opportunities directly into the user's portfolio and refreshes the list.
     */
    fun researchIdeasForCategory(
        category: BusinessModelCategory,
        onComplete: (List<BusinessIdea>) -> Unit = {}
    ) {
        marketUpdateState.value = MarketUpdateState(
            isScanning = true,
            scanMode = "CATEGORY_RESEARCH",
            targetCategoryName = category.displayName
        )
        viewModelScope.launch {
            try {
                val existingList = repository.allIdeas.first()
                val existingTitles = existingList.map { it.title }
                val activeModel = backupSettings.value.activeAiModel

                val result = GeminiApiClient.scanCategoryTargetedIdeas(
                    targetCategory = category,
                    existingIdeaTitles = existingTitles,
                    count = 3,
                    activeModel = activeModel
                )

                result.fold(
                    onSuccess = { newIdeas ->
                        if (newIdeas.isNotEmpty()) {
                            repository.insertMultipleIdeas(newIdeas)
                            marketUpdateState.value = MarketUpdateState(
                                isScanning = false,
                                newlyAddedIdeas = newIdeas,
                                completedMessage = "Synthesized & added ${newIdeas.size} new ${category.displayName} venture opportunities to your portfolio.",
                                scanMode = "CATEGORY_RESEARCH",
                                targetCategoryName = category.displayName
                            )
                            onComplete(newIdeas)
                        } else {
                            marketUpdateState.value = MarketUpdateState(
                                isScanning = false,
                                errorMessage = "No new unique ideas identified in ${category.displayName} at this time.",
                                scanMode = "CATEGORY_RESEARCH",
                                targetCategoryName = category.displayName
                            )
                        }
                    },
                    onFailure = { error ->
                        marketUpdateState.value = MarketUpdateState(
                            isScanning = false,
                            errorMessage = error.message ?: "Category research scan failed.",
                            scanMode = "CATEGORY_RESEARCH",
                            targetCategoryName = category.displayName
                        )
                    }
                )
            } catch (e: Exception) {
                marketUpdateState.value = MarketUpdateState(
                    isScanning = false,
                    errorMessage = e.message ?: "Failed to complete category research.",
                    scanMode = "CATEGORY_RESEARCH",
                    targetCategoryName = category.displayName
                )
            }
        }
    }

    fun clearMarketUpdateState() {
        marketUpdateState.value = MarketUpdateState()
    }

    // ==========================================
    // AUTO-BACKUP & DATA RECOVERY
    // ==========================================

    fun setAutoBackupEnabled(enabled: Boolean) {
        autoBackupManager?.setAutoBackupEnabled(enabled)
    }

    fun setPreferBlueOceanBias(prefer: Boolean) {
        autoBackupManager?.setPreferBlueOceanBias(prefer)
    }

    fun setActiveAiModel(model: String) {
        autoBackupManager?.setActiveAiModel(model)
    }

    fun updateBackupSettings(newSettings: AutoBackupSettings) {
        autoBackupManager?.updateSettings(newSettings)
    }

    fun triggerManualBackup(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.triggerAutoBackup(force = true)
            onComplete(true)
        }
    }

    fun listBackupArchives(): List<BackupArchiveItem> {
        return autoBackupManager?.listBackupArchives() ?: emptyList()
    }

    fun restoreIdeasFromBackup(archiveItem: BackupArchiveItem, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val file = java.io.File(archiveItem.filePath)
            val restored = autoBackupManager?.restoreIdeasFromBackupFile(file) ?: emptyList()
            if (restored.isNotEmpty()) {
                repository.restoreIdeasFromList(restored)
            }
            onComplete(restored.size)
        }
    }
}

class IdeaViewModelFactory(
    private val repository: IdeaRepository,
    private val driveService: GoogleDriveService? = null,
    private val userProfileRepository: UserProfileRepository? = null,
    private val autoBackupManager: AutoBackupManager? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IdeaViewModel::class.java)) {
            return IdeaViewModel(repository, driveService, userProfileRepository, autoBackupManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
