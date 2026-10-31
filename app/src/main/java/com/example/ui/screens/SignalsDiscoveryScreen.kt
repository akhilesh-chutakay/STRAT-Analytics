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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BusinessIdea
import com.example.data.model.DataSourceType
import com.example.data.model.FounderSignal
import com.example.ui.components.SignalItemCard
import com.example.ui.viewmodel.IdeaViewModel

@Composable
fun SignalsDiscoveryScreen(
    viewModel: IdeaViewModel,
    onNavigateToAddWithDraft: (BusinessIdea) -> Unit,
    modifier: Modifier = Modifier
) {
    val signals by viewModel.allSignals.collectAsStateWithLifecycle()
    var showAddSignalDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSignalDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_signal_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Founder Signal")
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
            // Header Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
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
                                imageVector = Icons.Default.Hearing,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Founder Pain Points & Market Signals",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Unfiltered complaints, feature requests, and burning operational friction sourced directly from SaaS founders, developers, and company operators with ready budgets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Verified Problem Vault (${signals.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (signals.isEmpty()) {
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
                            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No problem signals recorded yet", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Log quotes from Reddit, Twitter/X, G2, or customer calls.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(signals, key = { it.id }) { signal ->
                    SignalItemCard(
                        signal = signal,
                        onConvertToIdea = {
                            viewModel.convertSignalToIdea(signal) { draftedIdea ->
                                onNavigateToAddWithDraft(draftedIdea)
                            }
                        },
                        onDelete = {
                            viewModel.deleteSignal(signal)
                        }
                    )
                }
            }
        }
    }

    if (showAddSignalDialog) {
        AddSignalDialog(
            onDismiss = { showAddSignalDialog = false },
            onSave = { newSignal ->
                viewModel.saveSignal(newSignal)
                showAddSignalDialog = false
            }
        )
    }
}

@Composable
fun AddSignalDialog(
    onDismiss: () -> Unit,
    onSave: (FounderSignal) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf("") }
    var founder by remember { mutableStateOf("") }
    var niche by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf(DataSourceType.REDDIT) }
    var sourceRef by remember { mutableStateOf("") }
    var willingnessToPayStr by remember { mutableStateOf("99") }
    var urgency by remember { mutableStateOf("High") }
    var toolIdea by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Capture Market Signal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Problem Summary *") },
                    placeholder = { Text("e.g. Stripe failed payment recovery") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = quote,
                    onValueChange = { quote = it },
                    label = { Text("Raw Quote / Founder Pain Statement *") },
                    placeholder = { Text("\"We lost $1,400 MRR last month because cards expired...\"") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = founder,
                        onValueChange = { founder = it },
                        label = { Text("Founder / Company") },
                        placeholder = { Text("Marc (FeedPulse)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = willingnessToPayStr,
                        onValueChange = { willingnessToPayStr = it },
                        label = { Text("Budget ($/mo)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = toolIdea,
                    onValueChange = { toolIdea = it },
                    label = { Text("Suggested Tool Concept") },
                    placeholder = { Text("Automated Stripe failed payment retry bot") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val signal = FounderSignal(
                                problemTitle = title.ifBlank { "Uncategorized Problem Signal" },
                                rawProblemQuote = quote,
                                founderOrCompanyName = founder.ifBlank { "Anonymous Founder" },
                                industryNiche = niche.ifBlank { "B2B SaaS" },
                                sourceType = sourceType,
                                sourceLinkOrHandle = sourceRef,
                                estimatedWillingnessToPayMonthly = willingnessToPayStr.toDoubleOrNull() ?: 99.0,
                                urgencyLevel = urgency,
                                suggestedToolIdea = toolIdea
                            )
                            onSave(signal)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Signal")
                    }
                }
            }
        }
    }
}
