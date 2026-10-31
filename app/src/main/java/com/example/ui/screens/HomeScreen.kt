package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BusinessIdea
import com.example.data.model.BusinessModelCategory
import com.example.data.model.MarketType
import com.example.data.model.PipelineStage
import com.example.ui.components.GoogleDriveSyncDialog
import com.example.ui.components.IdeaCard
import com.example.ui.components.MatrixQuadrant
import com.example.ui.components.OpportunityMatrixView
import com.example.ui.components.UserDpAvatar
import com.example.ui.components.UserProfileDialog
import com.example.ui.components.formatCompactCurrency
import com.example.ui.theme.*
import java.util.Locale
import com.example.ui.viewmodel.IdeaSortOption
import com.example.ui.viewmodel.IdeaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: IdeaViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToWebCompanion: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ideas by viewModel.filteredIdeas.collectAsStateWithLifecycle()
    val stats by viewModel.portfolioStats.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedMarket by viewModel.selectedMarketFilter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedStage by viewModel.selectedStageFilter.collectAsStateWithLifecycle()
    val selectedZeroInvest by viewModel.selectedZeroInvestmentFilter.collectAsStateWithLifecycle()
    val currentSort by viewModel.sortOption.collectAsStateWithLifecycle()
    val user by viewModel.userProfile.collectAsStateWithLifecycle()

    var showMatrix by remember { mutableStateOf(false) }
    var selectedQuadrant by remember { mutableStateOf<MatrixQuadrant?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDrivePortfolioDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    if (showProfileDialog) {
        UserProfileDialog(
            viewModel = viewModel,
            onDismiss = { showProfileDialog = false },
            onOpenDriveSaver = { showDrivePortfolioDialog = true }
        )
    }

    if (showDrivePortfolioDialog) {
        GoogleDriveSyncDialog(
            viewModel = viewModel,
            targetIdea = null, // Full portfolio mode
            onDismiss = { showDrivePortfolioDialog = false }
        )
    }

    // Filter ideas by quadrant if quadrant is selected
    val displayIdeas = remember(ideas, selectedQuadrant) {
        if (selectedQuadrant == null) ideas
        else {
            when (selectedQuadrant) {
                MatrixQuadrant.QUICK_WINS -> ideas.filter { it.weeksToMvp <= 4 && it.estimatedARR >= 100000 }
                MatrixQuadrant.MOONSHOTS -> ideas.filter { it.weeksToMvp > 4 && it.estimatedARR >= 100000 }
                MatrixQuadrant.LOW_HANGING -> ideas.filter { it.weeksToMvp <= 4 && it.estimatedARR < 100000 }
                MatrixQuadrant.HEAVY_LIFTS -> ideas.filter { it.weeksToMvp > 4 && it.estimatedARR < 100000 }
                null -> ideas
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_idea_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Business Idea")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
        ) {
            // User Executive Header Card on Homepage
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E26),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProfileDialog = true }
                        .testTag("home_user_profile_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            UserDpAvatar(
                                user = user,
                                size = 44.dp,
                                borderWidth = 1.5.dp,
                                borderColor = MaterialTheme.colorScheme.primary,
                                showIndicator = true,
                                onClick = { showProfileDialog = true }
                            )

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${user.title} • Target: ${formatCompactCurrency(user.targetPortfolioARR)} ARR",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Header Executive KPI Bar with Sophisticated Dark Showcase
            item {
                val featuredIdea = ideas.firstOrNull { it.isImmediatelyActionable } ?: ideas.maxByOrNull { it.opportunityScore }
                PortfolioKpiBanner(
                    totalArr = stats.totalProjectedARR,
                    ideasCount = stats.totalIdeasCount,
                    actionableCount = stats.immediatelyActionableCount,
                    avgWeeks = stats.averageWeeksToMvp,
                    blueOceanCount = stats.blueOceanCount,
                    redOceanCount = stats.redOceanCount,
                    topActionableIdea = featuredIdea,
                    onIdeaClick = onNavigateToDetail
                )
            }

            // Web Companion Platform Quick Banner
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF161F30),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToWebCompanion() }
                        .testTag("home_web_companion_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Strat Analytics Web Platform",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "ONLINE",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Replicated web companion • Export standalone HTML",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9E9EA8)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Web Companion",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Search and Filter Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearch(it) },
                        placeholder = { Text("Search ideas, problems, markets...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearch("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_input")
                    )

                    // Google Drive Portfolio Saver Button
                    IconButton(
                        onClick = { showDrivePortfolioDialog = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E3A8A).copy(alpha = 0.35f))
                            .testTag("home_drive_backup_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Google Drive Saver",
                            tint = Color(0xFF60A5FA)
                        )
                    }

                    // Matrix Toggle Button
                    IconButton(
                        onClick = { showMatrix = !showMatrix },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (showMatrix) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("toggle_matrix_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Grid4x4,
                            contentDescription = "Opportunity Matrix",
                            tint = if (showMatrix) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sort Dropdown
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("sort_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            IdeaSortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.displayName,
                                            fontWeight = if (currentSort == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSort(option)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (currentSort == option) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Optional 2x2 Opportunity Matrix View
            if (showMatrix) {
                item {
                    OpportunityMatrixView(
                        ideas = ideas,
                        selectedQuadrant = selectedQuadrant,
                        onSelectQuadrant = { selectedQuadrant = it }
                    )
                }
            }

            // Quick Filter Chips (Market Type, $0 Capital, Categories)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedMarket == null && selectedCategory == null && selectedStage == null && !selectedZeroInvest,
                            onClick = { viewModel.resetFilters() },
                            label = { Text("All (${ideas.size})", fontSize = 12.sp) }
                        )
                    }

                    // Zero Investment ($0 Capital / No-Code) Filter Chip
                    item {
                        FilterChip(
                            selected = selectedZeroInvest,
                            onClick = { viewModel.selectZeroInvestmentFilter(!selectedZeroInvest) },
                            label = { Text("💰 \$0 Capital / No-Code", fontSize = 12.sp, fontWeight = if (selectedZeroInvest) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF065F46),
                                selectedLabelColor = Color(0xFF6EE7B7)
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = selectedMarket == MarketType.BLUE_OCEAN,
                            onClick = {
                                viewModel.selectMarketFilter(if (selectedMarket == MarketType.BLUE_OCEAN) null else MarketType.BLUE_OCEAN)
                            },
                            label = { Text("🌊 Blue Ocean", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlueOceanContainer,
                                selectedLabelColor = BlueOceanColor
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = selectedMarket == MarketType.RED_OCEAN,
                            onClick = {
                                viewModel.selectMarketFilter(if (selectedMarket == MarketType.RED_OCEAN) null else MarketType.RED_OCEAN)
                            },
                            label = { Text("🦈 Red Ocean", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedOceanContainer,
                                selectedLabelColor = RedOceanColor
                            )
                        )
                    }

                    items(BusinessModelCategory.values()) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                viewModel.selectCategoryFilter(if (selectedCategory == category) null else category)
                            },
                            label = { Text(category.displayName, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Internet Market Scout Hero Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF13221E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Internet \$0 Capital Scout",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Scan web signals across Reddit, X & Upwork for zero-cost cashflow opportunities.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        FilledTonalButton(
                            onClick = { viewModel.scanInternetForZeroInvestmentOpportunities() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF065F46),
                                contentColor = Color(0xFF6EE7B7)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan \$0 Ideas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedQuadrant != null) "${selectedQuadrant?.title} (${displayIdeas.size})" else "Tracked Ideas (${displayIdeas.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sorted by: ${currentSort.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // List of Ideas
            if (displayIdeas.isEmpty()) {
                item {
                    EmptyStateCard(
                        selectedCategory = selectedCategory,
                        selectedMarket = selectedMarket,
                        selectedZeroInvest = selectedZeroInvest,
                        onResearchCategory = { category ->
                            viewModel.researchIdeasForCategory(category)
                        },
                        onScanZeroInvestment = {
                            viewModel.scanInternetForZeroInvestmentOpportunities()
                        },
                        onScanMarket = {
                            viewModel.updateMarketIdeas()
                        },
                        onAddClick = onNavigateToAdd,
                        onResetFilters = { viewModel.resetFilters() }
                    )
                }
            } else {
                items(displayIdeas, key = { it.id }) { idea ->
                    IdeaCard(
                        idea = idea,
                        onClick = { onNavigateToDetail(idea.id) },
                        onToggleActionable = { viewModel.toggleActionable(idea.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PortfolioKpiBanner(
    totalArr: Double,
    ideasCount: Int,
    actionableCount: Int,
    avgWeeks: Double,
    blueOceanCount: Int,
    redOceanCount: Int,
    topActionableIdea: BusinessIdea? = null,
    onIdeaClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High Intent Featured Hero Card (if actionable idea exists)
        if (topActionableIdea != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onIdeaClick(topActionableIdea.id) }
                    .testTag("hero_opportunity_card"),
                color = Color(0xFF381E72),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFFD0BCFF)
                        ) {
                            Text(
                                text = if (topActionableIdea.isImmediatelyActionable) "HIGH INTENT" else "FEATURED OPPORTUNITY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF381E72),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFFEADDFF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${topActionableIdea.opportunityScore} Score",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEADDFF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = topActionableIdea.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 26.sp
                    )

                    if (topActionableIdea.tagline.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = topActionableIdea.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEADDFF).copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Divider(color = Color(0xFF49454F).copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "EST. ARR",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFEADDFF).copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = formatCompactCurrency(topActionableIdea.estimatedARR),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD0BCFF),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "View Idea",
                                    tint = Color(0xFF381E72),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2-Column KPI Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pipeline ARR Card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "PIPELINE ARR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCompactCurrency(totalArr),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "+24.5% runway",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4ADE80),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Actionable / Validated Needs Card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "VALIDATED NEEDS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$actionableCount Ideas",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Ready to build",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD0BCFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Secondary Metrics Strip
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Total Tracked:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$ideasCount Concepts", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Avg MVP:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format(Locale.US, "%.1f wks", avgWeeks), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🌊$blueOceanCount", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BlueOceanColor)
                    Text("•", color = MaterialTheme.colorScheme.outline)
                    Text("🦈$redOceanCount", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = RedOceanColor)
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    onAddClick: () -> Unit,
    onResetFilters: () -> Unit,
    selectedCategory: BusinessModelCategory? = null,
    selectedMarket: MarketType? = null,
    selectedZeroInvest: Boolean = false,
    onResearchCategory: ((BusinessModelCategory) -> Unit)? = null,
    onScanMarket: (() -> Unit)? = null,
    onScanZeroInvestment: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("empty_state_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = CardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (selectedCategory != null) Color(0xFF8B5CF6).copy(alpha = 0.18f)
                        else if (selectedZeroInvest) Color(0xFF059669).copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selectedCategory != null) Icons.Default.TravelExplore
                    else if (selectedZeroInvest) Icons.Default.MonetizationOn
                    else Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = if (selectedCategory != null) Color(0xFFA78BFA)
                    else if (selectedZeroInvest) Color(0xFF34D399)
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (selectedCategory != null) {
                    "No ${selectedCategory.displayName} Ideas Yet"
                } else if (selectedZeroInvest) {
                    "No \$0 Investment Ideas Found"
                } else if (selectedMarket != null) {
                    "No ${selectedMarket.displayName} Ideas Found"
                } else {
                    "No matching business ideas found"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (selectedCategory != null) {
                    "You don't have any tracked opportunities in '${selectedCategory.displayName}'. Run a deep AI market scout across Reddit, X, and web stores to discover high-margin ${selectedCategory.displayName} venture gaps."
                } else if (selectedZeroInvest) {
                    "Scan global internet signals for high-cashflow business opportunities requiring \$0 upfront capital and minimal coding."
                } else {
                    "Try adjusting your filters, trigger an intelligence scout, or track a new founder problem signal."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Target Research Button if category or zero-invest filter is active
            if (selectedCategory != null && onResearchCategory != null) {
                Button(
                    onClick = { onResearchCategory(selectedCategory) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("research_category_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.TravelExplore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Research ${selectedCategory.displayName} Ideas",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else if (selectedZeroInvest && onScanZeroInvestment != null) {
                Button(
                    onClick = onScanZeroInvestment,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_zero_investment_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Scout \$0 Capital Opportunities",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetFilters,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset Filters")
                }
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Track New Idea")
                }
            }
        }
    }
}
