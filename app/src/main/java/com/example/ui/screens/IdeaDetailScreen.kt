package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BusinessIdea
import com.example.data.model.MarketType
import com.example.data.model.PipelineStage
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.IdeaViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaDetailScreen(
    ideaId: Long,
    viewModel: IdeaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allIdeas by viewModel.filteredIdeas.collectAsStateWithLifecycle()
    val idea = remember(allIdeas, ideaId) { allIdeas.find { it.id == ideaId } }

    var selectedTab by remember { mutableStateOf(0) } // 0: Revenue, 1: Work/Tech, 2: Competition, 3: Buyer Market, 4: Action Plan
    var showRevenueDialog by remember { mutableStateOf(false) }
    var showGeminiSheet by remember { mutableStateOf(false) }
    var showStageMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDriveDialog by remember { mutableStateOf(false) }

    val aiState by viewModel.aiAnalysisState.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }

    if (idea == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (showDriveDialog) {
        GoogleDriveSyncDialog(
            viewModel = viewModel,
            targetIdea = idea,
            onDismiss = { showDriveDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = idea.title,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("detail_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Google Drive Sync / Saver
                    IconButton(
                        onClick = { showDriveDialog = true },
                        modifier = Modifier.testTag("detail_drive_sync_btn")
                    ) {
                        Icon(
                            imageVector = if (idea.isDriveSynced) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                            contentDescription = "Save to Google Drive",
                            tint = if (idea.isDriveSynced) Color(0xFF60A5FA) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Actionable Toggle
                    IconButton(
                        onClick = { viewModel.toggleActionable(idea.id) },
                        modifier = Modifier.testTag("detail_actionable_toggle")
                    ) {
                        Icon(
                            imageVector = if (idea.isImmediatelyActionable) Icons.Filled.Bolt else Icons.Outlined.Bolt,
                            contentDescription = "Toggle Actionable",
                            tint = if (idea.isImmediatelyActionable) ActionableGold else MaterialTheme.colorScheme.outline
                        )
                    }

                    // Edit
                    IconButton(
                        onClick = { onNavigateToEdit(idea.id) },
                        modifier = Modifier.testTag("detail_edit_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Idea")
                    }

                    // Delete
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("detail_delete_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Idea", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRevenueDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_revenue_sandbox_btn")
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Revenue Sandbox")
                    }

                    Button(
                        onClick = { showGeminiSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_ai_copilot_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Copilot")
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Executive Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = Color(0xFF381E72)
                            ) {
                                Text(
                                    text = idea.category.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD0BCFF),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            MarketTypeBadge(marketType = idea.marketType)
                        }

                        OpportunityScoreBadge(score = idea.opportunityScore)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = idea.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (idea.tagline.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = idea.tagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pipeline Stage Selector Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pipeline Stage:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box {
                            FilterChip(
                                selected = true,
                                onClick = { showStageMenu = true },
                                label = { Text(idea.stage.displayName, fontWeight = FontWeight.Bold) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                            )

                            DropdownMenu(
                                expanded = showStageMenu,
                                onDismissRequest = { showStageMenu = false }
                            ) {
                                PipelineStage.values().forEach { stage ->
                                    DropdownMenuItem(
                                        text = { Text(stage.displayName) },
                                        onClick = {
                                            viewModel.updateStage(idea.id, stage)
                                            showStageMenu = false
                                        },
                                        leadingIcon = {
                                            if (idea.stage == stage) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Google Drive & Local Storage Status Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF211F26),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (idea.isDriveSynced) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = if (idea.isDriveSynced) Color(0xFF4ADE80) else Color(0xFF60A5FA),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (idea.isDriveSynced) "Google Drive & Local Synced" else "Stored Locally in SQLite",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (idea.isDriveSynced) Color(0xFF4ADE80) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (idea.isDriveSynced) "Drive File: ${idea.driveFileId?.take(16) ?: "Synced"}..." else "Ready for Cloud Drive Backup",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { showDriveDialog = true },
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = if (idea.isDriveSynced) "Manage" else "Backup to Drive",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Tabs Selector
            val isZeroCost = idea.isZeroInvestment || idea.zeroCostPlaybook.isNotBlank()
            val tabTitles = if (isZeroCost) {
                listOf("Revenue", "\$0 Playbook", "Work & Tech", "Competition", "Buyers & Sources", "Action Plan")
            } else {
                listOf("Revenue", "Work & Tech", "Competition", "Buyers & Sources", "Action Plan")
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab.coerceAtMost(tabTitles.size - 1),
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            if (isZeroCost) {
                when (selectedTab) {
                    0 -> RevenueEngineSection(idea = idea, currencyFormat = currencyFormat, onOpenSandbox = { showRevenueDialog = true })
                    1 -> ZeroCostPlaybookSection(idea = idea)
                    2 -> WorkAndTechSection(idea = idea)
                    3 -> CompetitionAndMoatSection(idea = idea)
                    4 -> BuyerMarketSection(idea = idea)
                    5 -> ActionPlanSection(idea = idea)
                }
            } else {
                when (selectedTab) {
                    0 -> RevenueEngineSection(idea = idea, currencyFormat = currencyFormat, onOpenSandbox = { showRevenueDialog = true })
                    1 -> WorkAndTechSection(idea = idea)
                    2 -> CompetitionAndMoatSection(idea = idea)
                    3 -> BuyerMarketSection(idea = idea)
                    4 -> ActionPlanSection(idea = idea)
                }
            }
        }
    }

    // Revenue Sandbox Dialog
    if (showRevenueDialog) {
        RevenueCalculatorDialog(
            initialPrice = idea.monthlyPricePerCustomer,
            initialCustomers = idea.targetCustomersYear1,
            onDismiss = { showRevenueDialog = false },
            onApply = { p, c, mrr, arr, y1, y3 ->
                val updated = idea.copy(
                    monthlyPricePerCustomer = p,
                    targetCustomersYear1 = c,
                    estimatedMRR = mrr,
                    estimatedARR = arr,
                    projectedYear1Revenue = y1,
                    projectedYear3Revenue = y3
                )
                viewModel.saveIdea(updated)
                showRevenueDialog = false
            }
        )
    }

    // Gemini AI Bottom Sheet
    if (showGeminiSheet) {
        GeminiAiSheet(
            idea = idea,
            state = aiState,
            onRunAnalysis = { mode -> viewModel.runGeminiAnalysis(idea, mode) },
            onDismiss = {
                showGeminiSheet = false
                viewModel.clearAiAnalysis()
            },
            onAppendToNotes = { reportText ->
                val updatedNotes = if (idea.notes.isBlank()) reportText else "${idea.notes}\n\n--- AI Analysis ---\n$reportText"
                viewModel.saveIdea(idea.copy(notes = updatedNotes))
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Business Idea?") },
            text = { Text("Are you sure you want to delete '${idea.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteIdeaById(idea.id)
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RevenueEngineSection(
    idea: BusinessIdea,
    currencyFormat: NumberFormat,
    onOpenSandbox: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REVENUE PROJECTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onOpenSandbox) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tune Sandbox")
                }
            }

            // Key Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "Monthly Run-Rate",
                    value = currencyFormat.format(idea.estimatedMRR) + "/mo",
                    highlightColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Estimated ARR",
                    value = currencyFormat.format(idea.estimatedARR) + "/yr",
                    highlightColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "Year 1 Projection",
                    value = currencyFormat.format(idea.projectedYear1Revenue),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Year 3 Horizon",
                    value = currencyFormat.format(idea.projectedYear3Revenue),
                    modifier = Modifier.weight(1f)
                )
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            DetailItem(
                label = "Pricing Model & ARPU",
                content = "${idea.pricingModel} • $${idea.monthlyPricePerCustomer.toInt()}/mo average"
            )

            DetailItem(
                label = "Year 1 Target Customer Base",
                content = "${idea.targetCustomersYear1} active paying accounts"
            )

            if (idea.pricingTiersSummary.isNotBlank()) {
                DetailItem(
                    label = "Tiered Structure",
                    content = idea.pricingTiersSummary
                )
            }
        }
    }
}

@Composable
private fun WorkAndTechSection(idea: BusinessIdea) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "DEVELOPMENT COMPLEXITY & WORK REQUIRED",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "Estimated Effort",
                    value = "${idea.weeksToMvp} Weeks",
                    highlightColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Complexity Tier",
                    value = idea.devComplexity.displayName.split(" ").first(),
                    modifier = Modifier.weight(1f)
                )
            }

            MetricTile(
                title = "Est. Monthly Hosting & Infra Cost",
                value = "$${idea.estimatedMonthlyOperatingCost.toInt()}/mo",
                modifier = Modifier.fillMaxWidth()
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            DetailItem(
                label = "Proposed Tech Stack",
                content = idea.techStack
            )

            DetailItem(
                label = "MVP Core Feature Scope",
                content = idea.mvpFeatureScope
            )
        }
    }
}

@Composable
private fun CompetitionAndMoatSection(idea: BusinessIdea) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "COMPETITIVE POSITIONING & MARKET DYNAMICS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "Market Landscape",
                    value = idea.marketType.displayName,
                    highlightColor = if (idea.marketType == MarketType.BLUE_OCEAN) BlueOceanColor else RedOceanColor,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Competition Intensity",
                    value = idea.competitionLevel,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = idea.marketType.shortDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            if (idea.topCompetitors.isNotBlank()) {
                DetailItem(
                    label = "Incumbent Competitors",
                    content = idea.topCompetitors
                )
            }

            if (idea.competitorWeaknesses.isNotBlank()) {
                DetailItem(
                    label = "Competitor Flaws & Churn Triggers",
                    content = idea.competitorWeaknesses
                )
            }

            if (idea.unfairAdvantageMoat.isNotBlank()) {
                DetailItem(
                    label = "Unfair Moat & Wedge Strategy",
                    content = idea.unfairAdvantageMoat
                )
            }
        }
    }
}

@Composable
private fun BuyerMarketSection(idea: BusinessIdea) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "BUYER VALIDATION & DATA SOURCE INTEL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "Data Source Origin",
                    value = idea.dataSourceType.displayName.split(" ").first(),
                    highlightColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Willingness to Pay",
                    value = "${idea.willingnessToPayRating}/5 Stars",
                    highlightColor = ActionableGold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (idea.sourceQuote.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Raw Founder / Buyer Signal Quote:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(idea.sourceQuote, style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        if (idea.sourceUrlOrRef.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Ref: ${idea.sourceUrlOrRef}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            DetailItem(label = "Target Buyer Profile", content = idea.targetAudience)

            if (idea.readyBuyerProfiles.isNotBlank()) {
                DetailItem(label = "Ready Buyer Persona Details", content = idea.readyBuyerProfiles)
            }

            if (idea.whyNowTrigger.isNotBlank()) {
                DetailItem(label = "Why Now / Trigger Event", content = idea.whyNowTrigger)
            }
        }
    }
}

@Composable
private fun ActionPlanSection(idea: BusinessIdea) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "ACTIONABLE VALIDATION CHECKLIST & NOTES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            DetailItem(
                label = "Immediate Next Steps",
                content = idea.actionableNextSteps.ifBlank { "1. Identify 5 target buyers\n2. Conduct discovery interviews\n3. Pre-sell MVP" }
            )

            if (idea.notes.isNotBlank()) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                DetailItem(
                    label = "Founder Notes & Strategy Logs",
                    content = idea.notes
                )
            }
        }
    }
}

@Composable
private fun ZeroCostPlaybookSection(idea: BusinessIdea) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "\$0 Capital & Fast Cashflow Playbook",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Key Quick-Glance Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "Time to 1st \$",
                    value = idea.timeToFirstDollar.ifBlank { "< 48 Hours" },
                    modifier = Modifier.weight(1f),
                    highlightColor = Color(0xFF38BDF8)
                )
                MetricTile(
                    title = "Monthly OpEx",
                    value = "\$0 - \$${idea.estimatedMonthlyOperatingCost.toInt()}",
                    modifier = Modifier.weight(1f),
                    highlightColor = Color(0xFF4ADE80)
                )
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            DetailItem(
                label = "🎯 \$0-Cost Customer Acquisition Playbook",
                content = if (idea.zeroCostPlaybook.isNotBlank()) idea.zeroCostPlaybook else "1. Identify active complaint threads on Reddit/X\n2. DM 10 operators offering free setup\n3. Convert to monthly subscription via Stripe Payment Links"
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            DetailItem(
                label = "🛠️ Free-Tier Tech Stack (\$0 Upfront)",
                content = if (idea.zeroCostStack.isNotBlank()) idea.zeroCostStack else "Vercel Hobby, Supabase Free Tier, Resend (3k emails/mo), Stripe Checkout"
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            DetailItem(
                label = "⚡ Why This Requires Zero Capital",
                content = "No paid ads, no expensive servers, and no upfront inventory required. Value is delivered through targeted software automation and existing platform distribution."
            )
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    highlightColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF211F26),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = highlightColor)
        }
    }
}

@Composable
private fun DetailItem(label: String, content: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
