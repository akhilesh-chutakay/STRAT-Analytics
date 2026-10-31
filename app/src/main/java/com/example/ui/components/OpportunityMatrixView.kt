package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessIdea
import com.example.ui.theme.*

enum class MatrixQuadrant(val title: String, val subtitle: String, val badge: String) {
    QUICK_WINS("Quick Wins", "Low Effort • High ARR", "🔥 Priority"),
    MOONSHOTS("Moonshots", "High Effort • High ARR", "🚀 Strategic"),
    LOW_HANGING("Low-Hanging", "Low Effort • Fast MVP", "⚡ Speed"),
    HEAVY_LIFTS("Heavy Lifts", "High Effort • Long Horizon", "⏳ Caution")
}

@Composable
fun OpportunityMatrixView(
    ideas: List<BusinessIdea>,
    selectedQuadrant: MatrixQuadrant?,
    onSelectQuadrant: (MatrixQuadrant?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Quadrant classifications:
    // Low Effort: <= 4 weeks
    // High Revenue: >= $100k ARR
    val quickWins = ideas.filter { it.weeksToMvp <= 4 && it.estimatedARR >= 100000 }
    val moonshots = ideas.filter { it.weeksToMvp > 4 && it.estimatedARR >= 100000 }
    val lowHanging = ideas.filter { it.weeksToMvp <= 4 && it.estimatedARR < 100000 }
    val heavyLifts = ideas.filter { it.weeksToMvp > 4 && it.estimatedARR < 100000 }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .testTag("opportunity_matrix_card"),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(24.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Grid4x4,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Opportunity Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedQuadrant != null) {
                    TextButton(
                        onClick = { onSelectQuadrant(null) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Reset Filter", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2x2 Interactive Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Left: Quick Wins
                QuadrantCell(
                    quadrant = MatrixQuadrant.QUICK_WINS,
                    count = quickWins.size,
                    color = Color(0xFF4ADE80),
                    isSelected = selectedQuadrant == MatrixQuadrant.QUICK_WINS,
                    onClick = {
                        onSelectQuadrant(if (selectedQuadrant == MatrixQuadrant.QUICK_WINS) null else MatrixQuadrant.QUICK_WINS)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Top Right: Moonshots
                QuadrantCell(
                    quadrant = MatrixQuadrant.MOONSHOTS,
                    count = moonshots.size,
                    color = Color(0xFFD0BCFF),
                    isSelected = selectedQuadrant == MatrixQuadrant.MOONSHOTS,
                    onClick = {
                        onSelectQuadrant(if (selectedQuadrant == MatrixQuadrant.MOONSHOTS) null else MatrixQuadrant.MOONSHOTS)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bottom Left: Low-Hanging Fruit
                QuadrantCell(
                    quadrant = MatrixQuadrant.LOW_HANGING,
                    count = lowHanging.size,
                    color = ActionableGold,
                    isSelected = selectedQuadrant == MatrixQuadrant.LOW_HANGING,
                    onClick = {
                        onSelectQuadrant(if (selectedQuadrant == MatrixQuadrant.LOW_HANGING) null else MatrixQuadrant.LOW_HANGING)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Bottom Right: Heavy Lifts
                QuadrantCell(
                    quadrant = MatrixQuadrant.HEAVY_LIFTS,
                    count = heavyLifts.size,
                    color = MaterialTheme.colorScheme.outline,
                    isSelected = selectedQuadrant == MatrixQuadrant.HEAVY_LIFTS,
                    onClick = {
                        onSelectQuadrant(if (selectedQuadrant == MatrixQuadrant.HEAVY_LIFTS) null else MatrixQuadrant.HEAVY_LIFTS)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuadrantCell(
    quadrant: MatrixQuadrant,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) color else color.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("quadrant_${quadrant.name}"),
        color = if (isSelected) color.copy(alpha = 0.2f) else Color(0xFF211F26)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quadrant.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = color.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = quadrant.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = quadrant.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
        }
    }
}
