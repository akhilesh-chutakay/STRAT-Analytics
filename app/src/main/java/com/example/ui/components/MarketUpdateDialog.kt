package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BusinessIdea
import com.example.ui.components.formatCompactCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.MarketUpdateState

@Composable
fun MarketUpdateDialog(
    state: MarketUpdateState,
    onDismiss: () -> Unit,
    onNavigateToIdea: (Long) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = CardBorder,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("market_update_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when (state.scanMode) {
                                        "ZERO_INVESTMENT" -> Color(0xFF059669).copy(alpha = 0.2f)
                                        "CATEGORY_RESEARCH" -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                        else -> PrimaryBlue.copy(alpha = 0.2f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (state.scanMode) {
                                    "ZERO_INVESTMENT" -> Icons.Default.MonetizationOn
                                    "CATEGORY_RESEARCH" -> Icons.Default.TravelExplore
                                    else -> Icons.Default.Sync
                                },
                                contentDescription = null,
                                tint = when (state.scanMode) {
                                    "ZERO_INVESTMENT" -> Color(0xFF34D399)
                                    "CATEGORY_RESEARCH" -> Color(0xFFA78BFA)
                                    else -> PrimaryBlue
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (state.isScanning) {
                                    when (state.scanMode) {
                                        "ZERO_INVESTMENT" -> "Scouting Internet for $0 Ideas..."
                                        "CATEGORY_RESEARCH" -> "Researching ${state.targetCategoryName ?: "Category"}..."
                                        else -> "Scanning Market..."
                                    }
                                } else {
                                    when (state.scanMode) {
                                        "ZERO_INVESTMENT" -> "Internet Zero-Investment Intel"
                                        "CATEGORY_RESEARCH" -> "${state.targetCategoryName ?: "Category"} Intelligence"
                                        else -> "Market Re-Analysis"
                                    }
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = when (state.scanMode) {
                                    "ZERO_INVESTMENT" -> "Zero Upfront Capital • Fast Cashflow • No-Code"
                                    "CATEGORY_RESEARCH" -> "${state.targetCategoryName ?: "Targeted"} • Reddit, X & Store Deep Scan"
                                    else -> "Blue Ocean • Founder In-Need"
                                },
                                fontSize = 11.sp,
                                color = Color(0xFF9E9EA8)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9E9EA8))
                    }
                }

                if (state.isScanning) {
                    // Scanning State Animation
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = when (state.scanMode) {
                                "ZERO_INVESTMENT" -> Color(0xFF34D399)
                                "CATEGORY_RESEARCH" -> Color(0xFFA78BFA)
                                else -> PrimaryBlue
                            },
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = when (state.scanMode) {
                                "ZERO_INVESTMENT" -> "Scanning global internet signals & buyer requests..."
                                "CATEGORY_RESEARCH" -> "Scouting niche market demand for ${state.targetCategoryName}..."
                                else -> "Analyzing macroeconomic market signals..."
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = when (state.scanMode) {
                                "ZERO_INVESTMENT" ->
                                    "Analyzing discussions on Reddit, X, Upwork, Chrome Store, and G2 to uncover simple cashflow opportunities requiring \$0 upfront investment and minimal code. Existing ideas are preserved."
                                "CATEGORY_RESEARCH" ->
                                    "Analyzing acute complaints, feature gaps, and willingness-to-pay signals on Reddit, X, and Web Stores specifically for ${state.targetCategoryName}. All existing ideas remain safe in your portfolio."
                                else ->
                                    "Scanning for unaddressed founder bottlenecks, high-margin SaaS vacancies, and uncontested Blue Ocean opportunities. Older ideas remain safe in your portfolio."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF9E9EA8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else if (state.errorMessage != null) {
                    // Error State
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(40.dp))
                        Text(
                            text = state.errorMessage,
                            fontSize = 13.sp,
                            color = Color(0xFFEF4444),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E38))
                        ) {
                            Text("Dismiss")
                        }
                    }
                } else if (state.newlyAddedIdeas.isNotEmpty()) {
                    // Success State with newly added ideas
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AccentEmerald.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                                Text(
                                    text = when (state.scanMode) {
                                        "ZERO_INVESTMENT" -> "+${state.newlyAddedIdeas.size} Zero-Investment cashflow ideas added to portfolio!"
                                        "CATEGORY_RESEARCH" -> "+${state.newlyAddedIdeas.size} New ${state.targetCategoryName ?: "Category"} ideas added to portfolio!"
                                        else -> "+${state.newlyAddedIdeas.size} New Blue Ocean ideas appended & auto-backed up!"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = AccentEmerald
                                )
                            }
                        }

                        Text(
                            text = "NEW OPPORTUNITIES DISCOVERED:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = Color(0xFF9E9EA8)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.newlyAddedIdeas) { idea ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E1E26),
                                    border = CardBorder,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDismiss()
                                            if (idea.id > 0) onNavigateToIdea(idea.id)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (idea.isZeroInvestment) Color(0xFF065F46) else PrimaryBlue.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = if (idea.isZeroInvestment) "💰 \$0 CAPITAL" else "BLUE OCEAN",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (idea.isZeroInvestment) Color(0xFF6EE7B7) else PrimaryBlue,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = idea.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Text(
                                                text = "${formatCompactCurrency(idea.estimatedARR)}/yr",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = AccentEmerald
                                            )
                                        }

                                        Text(
                                            text = idea.tagline,
                                            fontSize = 11.sp,
                                            color = Color(0xFF9E9EA8),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (idea.isZeroInvestment) "⚡ 1st \$: ${idea.timeToFirstDollar} • \$${idea.monthlyPricePerCustomer.toInt()}/mo" else "MVP in ${idea.weeksToMvp} wks • \$${idea.monthlyPricePerCustomer.toInt()}/mo",
                                                fontSize = 10.sp,
                                                color = Color(0xFF71717A)
                                            )
                                            Text(
                                                text = "Score: ${idea.opportunityScore}/100",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = AccentGold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("View in Portfolio")
                        }
                    }
                }
            }
        }
    }
}
