package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.GoogleDriveSyncDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.IdeaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditIdeaScreen(
    ideaId: Long?,
    viewModel: IdeaViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allIdeas by viewModel.filteredIdeas.collectAsStateWithLifecycle()
    val existingIdea = remember(allIdeas, ideaId) {
        if (ideaId != null && ideaId > 0) allIdeas.find { it.id == ideaId } else null
    }

    val aiExtractionState by viewModel.aiExtractionState.collectAsStateWithLifecycle()

    var syncToGoogleDrive by remember { mutableStateOf(true) }
    var showDriveDialog by remember { mutableStateOf(false) }

    // Form fields
    var title by remember { mutableStateOf(existingIdea?.title ?: "") }
    var tagline by remember { mutableStateOf(existingIdea?.tagline ?: "") }
    var category by remember { mutableStateOf(existingIdea?.category ?: BusinessModelCategory.B2B_SAAS) }
    var marketType by remember { mutableStateOf(existingIdea?.marketType ?: MarketType.BLUE_OCEAN) }
    var stage by remember { mutableStateOf(existingIdea?.stage ?: PipelineStage.DISCOVERED) }
    var isImmediatelyActionable by remember { mutableStateOf(existingIdea?.isImmediatelyActionable ?: false) }
    var isZeroInvestment by remember { mutableStateOf(existingIdea?.isZeroInvestment ?: false) }
    var zeroCostPlaybook by remember { mutableStateOf(existingIdea?.zeroCostPlaybook ?: "") }
    var timeToFirstDollar by remember { mutableStateOf(existingIdea?.timeToFirstDollar ?: "< 48 Hours") }
    var zeroCostStack by remember { mutableStateOf(existingIdea?.zeroCostStack ?: "Vercel Hobby, Supabase Free, Stripe Links") }

    // Problem & Solution
    var problemStatement by remember { mutableStateOf(existingIdea?.problemStatement ?: "") }
    var solutionDescription by remember { mutableStateOf(existingIdea?.solutionDescription ?: "") }
    var targetAudience by remember { mutableStateOf(existingIdea?.targetAudience ?: "") }
    var whyNowTrigger by remember { mutableStateOf(existingIdea?.whyNowTrigger ?: "") }

    // Revenue
    var pricingModel by remember { mutableStateOf(existingIdea?.pricingModel ?: "Monthly Subscription") }
    var monthlyPriceStr by remember { mutableStateOf(existingIdea?.monthlyPricePerCustomer?.toInt()?.toString() ?: "99") }
    var targetCustomersStr by remember { mutableStateOf(existingIdea?.targetCustomersYear1?.toString() ?: "100") }
    var pricingTiersSummary by remember { mutableStateOf(existingIdea?.pricingTiersSummary ?: "Starter: $49/mo | Pro: $99/mo | Enterprise: $299/mo") }

    // Effort & Work
    var devComplexity by remember { mutableStateOf(existingIdea?.devComplexity ?: DevComplexity.MEDIUM_3_6_WEEKS) }
    var weeksToMvpStr by remember { mutableStateOf(existingIdea?.weeksToMvp?.toString() ?: "4") }
    var techStack by remember { mutableStateOf(existingIdea?.techStack ?: "Next.js, Supabase, Gemini API, Stripe") }
    var mvpFeatureScope by remember { mutableStateOf(existingIdea?.mvpFeatureScope ?: "1. Core workflow engine\n2. Dashboard & billing\n3. Webhook integration") }

    // Competition
    var competitionLevel by remember { mutableStateOf(existingIdea?.competitionLevel ?: "Medium") }
    var topCompetitors by remember { mutableStateOf(existingIdea?.topCompetitors ?: "") }
    var competitorWeaknesses by remember { mutableStateOf(existingIdea?.competitorWeaknesses ?: "") }
    var unfairAdvantageMoat by remember { mutableStateOf(existingIdea?.unfairAdvantageMoat ?: "") }

    // Data Source & Buyer Intel
    var dataSourceType by remember { mutableStateOf(existingIdea?.dataSourceType ?: DataSourceType.REDDIT) }
    var sourceUrlOrRef by remember { mutableStateOf(existingIdea?.sourceUrlOrRef ?: "") }
    var sourceQuote by remember { mutableStateOf(existingIdea?.sourceQuote ?: "") }
    var willingnessToPayRating by remember { mutableStateOf(existingIdea?.willingnessToPayRating ?: 4) }
    var readyBuyerProfiles by remember { mutableStateOf(existingIdea?.readyBuyerProfiles ?: "") }
    var actionableNextSteps by remember { mutableStateOf(existingIdea?.actionableNextSteps ?: "1. Reach out to 10 potential buyers\n2. Build 1-page pre-sale landing page\n3. Ship MVP") }
    var notes by remember { mutableStateOf(existingIdea?.notes ?: "") }

    // AI Helper Input
    var rawQuoteInput by remember { mutableStateOf("") }
    var showAiHelper by remember { mutableStateOf(false) }

    fun buildCurrentIdea(): BusinessIdea {
        val price = monthlyPriceStr.toDoubleOrNull() ?: 99.0
        val customers = targetCustomersStr.toIntOrNull() ?: 100
        val weeks = weeksToMvpStr.toIntOrNull() ?: 4
        val mrr = price * customers
        val arr = mrr * 12.0

        return BusinessIdea(
            id = existingIdea?.id ?: 0L,
            title = title.ifBlank { "Untitled Business Concept" },
            tagline = tagline,
            category = category,
            marketType = marketType,
            stage = stage,
            isImmediatelyActionable = isImmediatelyActionable,
            isZeroInvestment = isZeroInvestment,
            zeroCostPlaybook = zeroCostPlaybook,
            timeToFirstDollar = timeToFirstDollar,
            zeroCostStack = zeroCostStack,
            problemStatement = problemStatement,
            solutionDescription = solutionDescription,
            targetAudience = targetAudience,
            whyNowTrigger = whyNowTrigger,
            pricingModel = pricingModel,
            monthlyPricePerCustomer = price,
            targetCustomersYear1 = customers,
            estimatedMRR = mrr,
            estimatedARR = arr,
            projectedYear1Revenue = arr * 0.75,
            projectedYear3Revenue = arr * 2.8,
            pricingTiersSummary = pricingTiersSummary,
            devComplexity = devComplexity,
            weeksToMvp = weeks,
            techStack = techStack,
            estimatedMonthlyOperatingCost = 100.0,
            mvpFeatureScope = mvpFeatureScope,
            competitionLevel = competitionLevel,
            topCompetitors = topCompetitors,
            competitorWeaknesses = competitorWeaknesses,
            unfairAdvantageMoat = unfairAdvantageMoat,
            dataSourceType = dataSourceType,
            sourceUrlOrRef = sourceUrlOrRef,
            sourceQuote = sourceQuote,
            willingnessToPayRating = willingnessToPayRating,
            readyBuyerProfiles = readyBuyerProfiles,
            actionableNextSteps = actionableNextSteps,
            notes = notes
        )
    }

    if (showDriveDialog) {
        GoogleDriveSyncDialog(
            viewModel = viewModel,
            targetIdea = buildCurrentIdea(),
            onDismiss = { showDriveDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingIdea != null) "Edit Business Idea" else "New Business Opportunity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("form_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDriveDialog = true },
                        modifier = Modifier.testTag("drive_quick_sync_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Google Drive Saver",
                            tint = Color(0xFF60A5FA)
                        )
                    }

                    Button(
                        onClick = {
                            val ideaToSave = buildCurrentIdea()
                            if (syncToGoogleDrive) {
                                viewModel.saveIdeaLocallyAndToDrive(ideaToSave) { _, _ ->
                                    onNavigateBack()
                                }
                            } else {
                                viewModel.saveIdea(ideaToSave) {
                                    onNavigateBack()
                                }
                            }
                        },
                        modifier = Modifier.testTag("save_idea_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Save Plan", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Google Drive Dual-Storage Banner
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF211F26),
                border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E3A8A).copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Save to Device & Google Drive",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Stored in local SQLite and backed up to Google Drive",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = syncToGoogleDrive,
                        onCheckedChange = { syncToGoogleDrive = it },
                        modifier = Modifier.testTag("switch_drive_sync")
                    )
                }
            }

            // AI Idea Extractor Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF211F26),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("AI Idea Crafter from Quote", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        TextButton(onClick = { showAiHelper = !showAiHelper }) {
                            Text(if (showAiHelper) "Hide" else "Expand")
                        }
                    }

                    if (showAiHelper) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Paste a raw founder complaint or company pain point quote (e.g. from Reddit or G2), and Gemini will formulate the problem & strategy:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rawQuoteInput,
                            onValueChange = { rawQuoteInput = it },
                            placeholder = { Text("e.g. 'We spend 15 hours a week manually copy-pasting data between Stripe and QuickBooks...'") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (rawQuoteInput.isNotBlank()) {
                                    sourceQuote = rawQuoteInput
                                    viewModel.extractIdeaWithAi(rawQuoteInput, dataSourceType.displayName)
                                }
                            },
                            enabled = rawQuoteInput.isNotBlank() && !aiExtractionState.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (aiExtractionState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Pain Point...")
                            } else {
                                Text("Extract Structured Business Opportunity", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (aiExtractionState.extractedBlueprint != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("AI STRATEGIC BLUEPRINT:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(aiExtractionState.extractedBlueprint ?: "", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // 1. Core Identity Section
            FormSectionHeader(title = "1. CORE VENTURE IDENTITY")

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Business / Product Name *") },
                placeholder = { Text("e.g. ChurnSentinel") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_title"),
                singleLine = true
            )

            OutlinedTextField(
                value = tagline,
                onValueChange = { tagline = it },
                label = { Text("One-Sentence Tagline / Value Proposition") },
                placeholder = { Text("e.g. Intercept cancellation intents and recover failed SaaS renewals") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            // Market Type (Blue Ocean vs Red Ocean)
            Text("Market Classification *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = marketType == MarketType.BLUE_OCEAN,
                    onClick = { marketType = MarketType.BLUE_OCEAN },
                    label = { Text("🌊 Blue Ocean (Untapped/New)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlueOceanContainer,
                        selectedLabelColor = BlueOceanColor
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = marketType == MarketType.RED_OCEAN,
                    onClick = { marketType = MarketType.RED_OCEAN },
                    label = { Text("🦈 Red Ocean (Proven Demand)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RedOceanContainer,
                        selectedLabelColor = RedOceanColor
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Immediately Actionable Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isImmediatelyActionable) ActionableGoldContainer.copy(alpha = 0.4f) else Color(0xFF211F26))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = ActionableGold)
                        Text("Immediately Actionable Idea", fontWeight = FontWeight.Bold)
                    }
                    Text("Flag as high-urgency, low-effort build with validated buyer demand", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isImmediatelyActionable,
                    onCheckedChange = { isImmediatelyActionable = it },
                    modifier = Modifier.testTag("switch_actionable")
                )
            }

            // Zero-Investment ($0 Capital / No-Code) Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isZeroInvestment) Color(0xFF064E3B).copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF34D399))
                        Text("💰 \$0 Upfront Investment & Simple Cashflow", fontWeight = FontWeight.Bold, color = if (isZeroInvestment) Color(0xFF6EE7B7) else Color.White)
                    }
                    Text("Requires no initial ad spend, no expensive servers, zero or low-code execution", fontSize = 11.sp, color = Color(0xFF9E9EA8))
                }
                Switch(
                    checked = isZeroInvestment,
                    onCheckedChange = { isZeroInvestment = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981)
                    )
                )
            }

            if (isZeroInvestment) {
                OutlinedTextField(
                    value = timeToFirstDollar,
                    onValueChange = { timeToFirstDollar = it },
                    label = { Text("Estimated Time to First Dollar") },
                    placeholder = { Text("e.g. < 48 Hours, 3-5 Days") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = zeroCostPlaybook,
                    onValueChange = { zeroCostPlaybook = it },
                    label = { Text("🎯 \$0 Customer Acquisition Playbook") },
                    placeholder = { Text("e.g. Direct outreach on Reddit/X, free setup for first 5 clients, organic conversion") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = zeroCostStack,
                    onValueChange = { zeroCostStack = it },
                    label = { Text("🛠️ Free Tier Stack") },
                    placeholder = { Text("e.g. Vercel Hobby, Supabase Free Tier, Stripe Payment Links, Airtable") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Problem & Solution
            FormSectionHeader(title = "2. PROBLEM & SOLUTION VALIDATION")

            OutlinedTextField(
                value = problemStatement,
                onValueChange = { problemStatement = it },
                label = { Text("The Real Founder / Company Problem *") },
                placeholder = { Text("What painful friction do companies currently experience?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_problem"),
                minLines = 3
            )

            OutlinedTextField(
                value = solutionDescription,
                onValueChange = { solutionDescription = it },
                label = { Text("Proposed Software Solution *") },
                placeholder = { Text("How does your tool specifically eliminate this pain?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = targetAudience,
                onValueChange = { targetAudience = it },
                label = { Text("Target Audience & Ready Buyer Profile") },
                placeholder = { Text("e.g. Bootstrapped B2B SaaS Founders with $10k-$50k MRR") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = whyNowTrigger,
                onValueChange = { whyNowTrigger = it },
                label = { Text("Why Now? (Technological or Market Catalyst)") },
                placeholder = { Text("e.g. New Stripe webhook APIs + Gemini 3.5 Flash capabilities") },
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Revenue Engine Projections
            FormSectionHeader(title = "3. REVENUE ESTIMATIONS & PRICING")

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = monthlyPriceStr,
                    onValueChange = { monthlyPriceStr = it },
                    label = { Text("Price / Mo ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_price"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = targetCustomersStr,
                    onValueChange = { targetCustomersStr = it },
                    label = { Text("Year 1 Customers") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_customers"),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = pricingTiersSummary,
                onValueChange = { pricingTiersSummary = it },
                label = { Text("Pricing Tier Breakdown") },
                placeholder = { Text("Starter: $49/mo | Growth: $99/mo | Enterprise: $299/mo") },
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Work Required & MVP Scope
            FormSectionHeader(title = "4. WORK REQUIRED & TECH SPEC")

            OutlinedTextField(
                value = weeksToMvpStr,
                onValueChange = { weeksToMvpStr = it },
                label = { Text("Weeks to Ship MVP *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = techStack,
                onValueChange = { techStack = it },
                label = { Text("Tech Stack") },
                placeholder = { Text("e.g. Kotlin, Next.js, Supabase, Stripe, Gemini API") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mvpFeatureScope,
                onValueChange = { mvpFeatureScope = it },
                label = { Text("MVP Feature Scope Checklist") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // 5. Competition & Moats
            FormSectionHeader(title = "5. COMPETITION & UNFAIR MOATS")

            OutlinedTextField(
                value = topCompetitors,
                onValueChange = { topCompetitors = it },
                label = { Text("Incumbent Competitors") },
                placeholder = { Text("e.g. Baremetrics Recover, Churnkey") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = competitorWeaknesses,
                onValueChange = { competitorWeaknesses = it },
                label = { Text("Competitor Flaws & Churn Triggers") },
                placeholder = { Text("e.g. Too expensive ($250+/mo), bloated UI, poor customer support") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = unfairAdvantageMoat,
                onValueChange = { unfairAdvantageMoat = it },
                label = { Text("Unfair Moat / Wedge Advantage") },
                placeholder = { Text("e.g. 5-minute install, zero-commission flat fee") },
                modifier = Modifier.fillMaxWidth()
            )

            // 6. Data Source & Ready Buyer Validation
            FormSectionHeader(title = "6. DATA SOURCES & FOUNDER INTEL")

            OutlinedTextField(
                value = sourceQuote,
                onValueChange = { sourceQuote = it },
                label = { Text("Raw Problem Quote / Founder Signal") },
                placeholder = { Text("Direct quote from the buyer or founder expressing this need") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = sourceUrlOrRef,
                onValueChange = { sourceUrlOrRef = it },
                label = { Text("Source Link / Community Reference") },
                placeholder = { Text("e.g. reddit.com/r/SaaS/comments/1f4x9q or G2 review link") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Buyer Willingness to Pay: $willingnessToPayRating / 5 Stars", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Slider(
                value = willingnessToPayRating.toFloat(),
                onValueChange = { willingnessToPayRating = it.toInt() },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = actionableNextSteps,
                onValueChange = { actionableNextSteps = it },
                label = { Text("Actionable Validation Steps") },
                placeholder = { Text("1. DM 10 founders\n2. Set up pre-order landing\n3. Build prototype") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Private Notes & Strategy Logs") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val ideaToSave = buildCurrentIdea()
                    if (syncToGoogleDrive) {
                        viewModel.saveIdeaLocallyAndToDrive(ideaToSave) { _, _ ->
                            onNavigateBack()
                        }
                    } else {
                        viewModel.saveIdea(ideaToSave) {
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_idea_bottom_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (syncToGoogleDrive) Icons.Default.CloudUpload else Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (syncToGoogleDrive) "Save to Device & Google Drive" else "Save Locally",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FormSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
        letterSpacing = 0.8.sp,
        fontSize = 10.sp
    )
}

