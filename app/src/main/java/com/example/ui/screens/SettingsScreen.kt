package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.backup.BackupArchiveItem
import com.example.ui.components.GoogleDriveSyncDialog
import com.example.ui.components.UserDpAvatar
import com.example.ui.components.UserProfileDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.IdeaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: IdeaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToWebCompanion: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backupSettings by viewModel.backupSettings.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val stats by viewModel.portfolioStats.collectAsStateWithLifecycle()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showDriveDialog by remember { mutableStateOf(false) }
    var backupArchives by remember { mutableStateOf(viewModel.listBackupArchives()) }
    var isBackingUp by remember { mutableStateOf(false) }
    var selectedArchiveToRestore by remember { mutableStateOf<BackupArchiveItem?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    fun refreshArchives() {
        backupArchives = viewModel.listBackupArchives()
    }

    if (showProfileDialog) {
        UserProfileDialog(
            viewModel = viewModel,
            onDismiss = { showProfileDialog = false },
            onOpenDriveSaver = { showDriveDialog = true }
        )
    }

    if (showDriveDialog) {
        GoogleDriveSyncDialog(
            viewModel = viewModel,
            targetIdea = null,
            onDismiss = { showDriveDialog = false }
        )
    }

    if (showRestoreConfirmDialog && selectedArchiveToRestore != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Restore Portfolio from Archive?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will restore ${selectedArchiveToRestore?.ideaCount} ideas from backup '${selectedArchiveToRestore?.fileName}'. Existing ideas with matching IDs will be updated and any new ideas will be merged."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = selectedArchiveToRestore
                        showRestoreConfirmDialog = false
                        if (item != null) {
                            viewModel.restoreIdeasFromBackup(item) { count ->
                                Toast.makeText(context, "Restored $count ideas successfully!", Toast.LENGTH_SHORT).show()
                                refreshArchives()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings & Data Engine",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Auto-Backup • Active AI • Market Intelligence",
                            fontSize = 12.sp,
                            color = Color(0xFF9E9EA8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
        ) {
            // Profile Card Link
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E26),
                    border = CardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProfileDialog = true }
                        .testTag("settings_profile_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        UserDpAvatar(
                            user = userProfile,
                            size = 48.dp,
                            showIndicator = true,
                            onClick = { showProfileDialog = true }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${userProfile.title} • ${userProfile.organization}",
                                fontSize = 12.sp,
                                color = Color(0xFF9E9EA8)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Edit Profile",
                            tint = Color(0xFF71717A)
                        )
                    }
                }
            }

            // Web Platform / Companion Card Link
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF161F30),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToWebCompanion() }
                        .testTag("settings_web_companion_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Web Platform & Standalone Site",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Interactive web companion dashboard replicating the entire app. Export as standalone HTML or test in-app.",
                                fontSize = 12.sp,
                                color = Color(0xFF9E9EA8)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Web Companion",
                            tint = Color(0xFF34D399)
                        )
                    }
                }
            }

            // 1. AUTOMATIC IDEA BACKUP & DATA VAULT SECTION
            item {
                Text(
                    text = "AUTOMATIC BACKUP & DATA VAULT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFF9E9EA8)
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E26),
                    border = CardBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Master Auto Backup Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudUpload,
                                        contentDescription = null,
                                        tint = if (backupSettings.isAutoBackupEnabled) AccentEmerald else Color(0xFF71717A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Automatic Idea Backup",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Automatically persists all venture ideas, edits, and stage transitions to local storage & cloud vaults in real-time.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9E9EA8)
                                )
                            }

                            Switch(
                                checked = backupSettings.isAutoBackupEnabled,
                                onCheckedChange = { isChecked ->
                                    viewModel.setAutoBackupEnabled(isChecked)
                                    Toast.makeText(
                                        context,
                                        if (isChecked) "Auto-Backup enabled: Ideas are backed up continuously." else "Auto-Backup paused.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.testTag("auto_backup_switch")
                            )
                        }

                        HorizontalDivider(color = Color(0xFF2E2E38))

                        // Last Backup Info & Diagnostics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Vault Status",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9E9EA8)
                                )
                                val lastBackupFormatted = if (backupSettings.lastBackupTimestamp > 0L) {
                                    val df = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault())
                                    df.format(Date(backupSettings.lastBackupTimestamp))
                                } else {
                                    "No backups saved yet"
                                }
                                Text(
                                    text = lastBackupFormatted,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (backupSettings.lastBackupTimestamp > 0L) AccentEmerald else Color(0xFFF59E0B)
                                )
                                Text(
                                    text = "${stats.totalIdeasCount} Ideas Secured in Room Database",
                                    fontSize = 11.sp,
                                    color = Color(0xFF71717A)
                                )
                            }

                            Button(
                                onClick = {
                                    isBackingUp = true
                                    viewModel.triggerManualBackup {
                                        isBackingUp = false
                                        refreshArchives()
                                        Toast.makeText(context, "Portfolio backed up successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isBackingUp,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E2E3A),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("backup_now_button")
                            ) {
                                if (isBackingUp) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Backup Now", fontSize = 12.sp)
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF2E2E38))

                        // Google Drive Cloud Sync Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDriveDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudQueue,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Google Drive Cloud Vault",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Export individual ideas or entire portfolio as JSON / Markdown",
                                        fontSize = 11.sp,
                                        color = Color(0xFF9E9EA8)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF71717A)
                            )
                        }
                    }
                }
            }

            // 2. ACTIVE AI & MARKET ANALYSIS CONFIGURATION
            item {
                Text(
                    text = "ACTIVE AI & VENTURE INTELLIGENCE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFF9E9EA8)
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E26),
                    border = CardBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Active AI Engine Indicator & Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(AccentEmerald)
                                )
                                Text(
                                    text = "Active AI Model Engine",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Select the Gemini neural model used for market teardowns, buyer discovery, and Blue Ocean opportunity generation.",
                                fontSize = 12.sp,
                                color = Color(0xFF9E9EA8)
                            )

                            // AI Model Selector Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val models = listOf(
                                    "gemini-3.5-flash" to "Gemini 3.5 Flash (Ultra Fast)",
                                    "gemini-3.1-pro-preview" to "Gemini 3.1 Pro (Deep Strategy)"
                                )

                                models.forEach { (modelKey, label) ->
                                    val isSelected = backupSettings.activeAiModel == modelKey
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) PrimaryBlue.copy(alpha = 0.2f) else Color(0xFF2A2A36),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryBlue) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.setActiveAiModel(modelKey)
                                                Toast.makeText(context, "Active AI updated to $label", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isSelected) {
                                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                                                }
                                                Text(
                                                    text = if (modelKey.contains("flash")) "3.5 Flash" else "3.1 Pro",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) PrimaryBlue else Color.White
                                                )
                                            }
                                            Text(
                                                text = if (modelKey.contains("flash")) "Recommended • Fast" else "Advanced Reasoning",
                                                fontSize = 10.sp,
                                                color = Color(0xFF9E9EA8)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF2E2E38))

                        // Blue Ocean Bias Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Blue Ocean Market Preference",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Instructs AI to prioritize uncontested market spaces, high gross margins, defensible workflow moats, and low existing competition.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9E9EA8)
                                )
                            }

                            Switch(
                                checked = backupSettings.preferBlueOceanBias,
                                onCheckedChange = { isChecked ->
                                    viewModel.setPreferBlueOceanBias(isChecked)
                                },
                                modifier = Modifier.testTag("blue_ocean_switch")
                            )
                        }

                        HorizontalDivider(color = Color(0xFF2E2E38))

                        // Founder In-Need Sensitivity Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Founder In-Need Problem Sensitivity",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Focuses opportunity discovery on verified founder complaints, high willingness-to-pay ($79-$299+/mo), and <4-week MVPs.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9E9EA8)
                                )
                            }

                            Switch(
                                checked = backupSettings.highPainPointFilter,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateBackupSettings(backupSettings.copy(highPainPointFilter = isChecked))
                                }
                            )
                        }
                    }
                }
            }

            // 3. BACKUP HISTORY & RESTORE ARCHIVES
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BACKUP HISTORY ARCHIVES (${backupArchives.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = Color(0xFF9E9EA8)
                    )
                    TextButton(onClick = { refreshArchives() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh", fontSize = 11.sp, color = PrimaryBlue)
                    }
                }
            }

            if (backupArchives.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E1E26),
                        border = CardBorder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.FolderZip, contentDescription = null, tint = Color(0xFF71717A), modifier = Modifier.size(32.dp))
                            Text("No archive snapshots found yet", fontSize = 13.sp, color = Color(0xFF9E9EA8))
                            Text("Automatic snapshots will appear here whenever ideas are created, updated, or when you tap 'Backup Now'.", fontSize = 11.sp, color = Color(0xFF71717A), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                items(backupArchives) { archive ->
                    val df = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()) }
                    val dateFormatted = df.format(Date(archive.timestamp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E1E26),
                        border = CardBorder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = dateFormatted,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${archive.ideaCount} Ideas • ${archive.fileSizeFormatted}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF9E9EA8)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    selectedArchiveToRestore = archive
                                    showRestoreConfirmDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 4. ABOUT & SYSTEM STATUS
            item {
                Text(
                    text = "ABOUT & PLATFORM STATUS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFF9E9EA8)
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E26),
                    border = CardBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Application Name", fontSize = 12.sp, color = Color(0xFF9E9EA8))
                            Text("Strat Analytics", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Version", fontSize = 12.sp, color = Color(0xFF9E9EA8))
                            Text("1.2.0 (Build 2026)", fontSize = 12.sp, color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Room Database Engine", fontSize = 12.sp, color = Color(0xFF9E9EA8))
                            Text("Online (Auto-Persist Active)", fontSize = 12.sp, color = AccentEmerald)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("AI Market Scanner", fontSize = 12.sp, color = Color(0xFF9E9EA8))
                            Text("Gemini 3.5 Flash Active", fontSize = 12.sp, color = PrimaryBlue)
                        }
                    }
                }
            }
        }
    }
}
