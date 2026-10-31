package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BusinessIdea
import com.example.ui.components.IdeaCard
import com.example.ui.theme.ActionableGold
import com.example.ui.theme.ActionableGoldContainer
import com.example.ui.viewmodel.IdeaViewModel

@Composable
fun ActionableIdeasScreen(
    viewModel: IdeaViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actionableIdeas by viewModel.immediatelyActionableIdeas.collectAsStateWithLifecycle()
    var filterZeroOnly by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val displayIdeas = remember(actionableIdeas, filterZeroOnly) {
        if (filterZeroOnly) actionableIdeas.filter { it.isZeroInvestment || it.estimatedMonthlyOperatingCost <= 20.0 }
        else actionableIdeas
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
    ) {
        // High Urgency Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = ActionableGoldContainer.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = ActionableGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Immediately Actionable Ideas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "High-urgency software concepts addressing validated burning problems where founders and companies have an active willingness to pay and MVP scope is <= 3-4 weeks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "${actionableIdeas.size} Ready to Build",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ActionableGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "Filtered by validated buyer intent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Filter Chips Row
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !filterZeroOnly,
                    onClick = { filterZeroOnly = false },
                    label = { Text("All Priority (${actionableIdeas.size})") }
                )

                FilterChip(
                    selected = filterZeroOnly,
                    onClick = { filterZeroOnly = true },
                    label = { Text("💰 \$0 Capital (${actionableIdeas.count { it.isZeroInvestment || it.estimatedMonthlyOperatingCost <= 20.0 }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = androidx.compose.ui.graphics.Color(0xFF065F46),
                        selectedLabelColor = androidx.compose.ui.graphics.Color(0xFF6EE7B7)
                    )
                )
            }
        }

        // Section Title
        item {
            Text(
                text = "Priority Build Radar (${displayIdeas.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (displayIdeas.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = ActionableGold, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (filterZeroOnly) "No \$0 capital priority ideas found" else "No immediately actionable ideas flagged yet", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap the lightning bolt on any idea in the home radar to flag it as high priority, or scan the internet for \$0 capital opportunities.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.scanInternetForZeroInvestmentOpportunities() },
                            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF065F46))
                        ) {
                            Text("Scan Internet for \$0 Ideas", color = androidx.compose.ui.graphics.Color(0xFF6EE7B7))
                        }
                    }
                }
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
