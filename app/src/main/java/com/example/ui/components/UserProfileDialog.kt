package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.UserProfile
import com.example.ui.viewmodel.IdeaViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun UserProfileDialog(
    viewModel: IdeaViewModel,
    onDismiss: () -> Unit,
    onOpenDriveSaver: () -> Unit = {}
) {
    val context = LocalContext.current
    val user by viewModel.userProfile.collectAsState()
    val stats by viewModel.portfolioStats.collectAsState()
    val driveSyncState by viewModel.driveSyncState.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    var editName by remember(user) { mutableStateOf(user.name) }
    var editTitle by remember(user) { mutableStateOf(user.title) }
    var editBio by remember(user) { mutableStateOf(user.bio) }
    var editTargetARR by remember(user) {
        mutableStateOf(
            if (user.targetPortfolioARR <= 0) "1000000"
            else if (user.targetPortfolioARR % 1.0 == 0.0) String.format(Locale.US, "%.0f", user.targetPortfolioARR)
            else user.targetPortfolioARR.toString()
        )
    }
    var editAvatarUrl by remember(user) { mutableStateOf(user.avatarUrl) }

    // Modern Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            viewModel.uploadAvatar(uri) { savedPath ->
                isUploading = false
                if (savedPath != null) {
                    editAvatarUrl = savedPath
                    Toast.makeText(context, "Display picture updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not upload image. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Fallback file picker for legacy media selection
    val legacyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            viewModel.uploadAvatar(uri) { savedPath ->
                isUploading = false
                if (savedPath != null) {
                    editAvatarUrl = savedPath
                    Toast.makeText(context, "Display picture updated successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun launchImagePicker() {
        try {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            legacyPickerLauncher.launch("image/*")
        }
    }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }
    val progressToGoal = if (user.targetPortfolioARR > 0.0) {
        (stats.totalProjectedARR / user.targetPortfolioARR).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(26.dp))
                .testTag("user_profile_dialog"),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Executive Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hero User Display Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E1E26),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large Avatar DP with interactive camera upload badge
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                shape = CircleShape,
                                modifier = Modifier
                                    .clickable { launchImagePicker() }
                            ) {
                                UserDpAvatar(
                                    user = if (isEditing) user.copy(avatarUrl = editAvatarUrl, name = editName) else user,
                                    size = 92.dp,
                                    borderWidth = 2.5.dp,
                                    borderColor = MaterialTheme.colorScheme.primary,
                                    showIndicator = true,
                                    onClick = { launchImagePicker() }
                                )
                            }

                            // Camera / Upload Action Badge
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable { launchImagePicker() }
                                    .testTag("upload_dp_camera_badge")
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    if (isUploading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "Upload Custom DP",
                                            tint = Color.Black,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Upload Photo Prompt
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { launchImagePicker() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Tap to upload new DP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isEditing) editName.ifBlank { "Founder Name" } else user.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (isEditing) editTitle.ifBlank { "Venture Architect" } else user.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Badges Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Google Drive Linked",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF93C5FD)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = Color(0xFF4C1D95).copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = user.tier,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDDD6FE),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar DP Selector & Presets + Upload Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVATAR PRESETS & UPLOAD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    )

                    TextButton(
                        onClick = { launchImagePicker() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Upload Image",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Item 0: Custom Upload Trigger Tile
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { launchImagePicker() }
                                .padding(4.dp)
                                .testTag("upload_custom_dp_tile")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Upload from gallery",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Upload",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(UserProfile.AVATAR_PRESETS) { preset ->
                        val isSelected = (if (isEditing) editAvatarUrl else user.avatarUrl) == preset.avatarUrl
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    editAvatarUrl = preset.avatarUrl
                                    if (!isEditing) {
                                        viewModel.updateAvatar(preset.avatarUrl)
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                border = BorderStroke(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.size(52.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(preset.avatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = preset.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Portfolio Metrics & ARR Target
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF211F26),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Portfolio Target Progress",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val progressPercentageText = if (user.targetPortfolioARR > 0.0) {
                                val pct = (stats.totalProjectedARR / user.targetPortfolioARR) * 100.0
                                if (pct >= 1000) String.format(Locale.US, "%.0f%%", pct)
                                else String.format(Locale.US, "%.1f%%", pct)
                            } else "0%"
                            Text(
                                text = progressPercentageText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progressToGoal },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Current ARR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = currencyFormat.format(stats.totalProjectedARR),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4ADE80)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Target Goal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = currencyFormat.format(user.targetPortfolioARR),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile Bio & Details Section
                if (!isEditing) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF211F26),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Founder Thesis & Bio",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = user.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    // Edit Form
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Founder Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Venture Title / Role") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Founder Thesis & Bio") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editTargetARR,
                        onValueChange = { editTargetARR = it },
                        label = { Text("Target Portfolio ARR ($)") },
                        placeholder = { Text("e.g. 5000000 or 1000000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_arr_input"),
                        supportingText = {
                            val parsed = editTargetARR.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                            if (parsed != null && parsed > 0.0) {
                                Text(
                                    text = "Preview: ${currencyFormat.format(parsed)} (${formatCompactCurrency(parsed)} ARR)",
                                    color = Color(0xFF4ADE80),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            } else {
                                Text(
                                    text = "Enter any numerical target with no numeral limit",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quick Target Presets
                    Text(
                        text = "QUICK TARGET PRESETS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val quickMilestones = listOf(
                            "1M" to 1_000_000.0,
                            "5M" to 5_000_000.0,
                            "10M" to 10_000_000.0,
                            "50M" to 50_000_000.0,
                            "100M" to 100_000_000.0,
                            "500M" to 500_000_000.0,
                            "1B" to 1_000_000_000.0,
                            "10B" to 10_000_000_000.0,
                            "100B" to 100_000_000_000.0
                        )
                        items(quickMilestones) { (label, amount) ->
                            val currentParsed = editTargetARR.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                            val isCurrent = (currentParsed == amount)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color(0xFF272730),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        editTargetARR = String.format(Locale.US, "%.0f", amount)
                                    }
                            ) {
                                Text(
                                    text = "$$label",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isEditing) {
                        OutlinedButton(
                            onClick = { onDismiss(); onOpenDriveSaver() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Drive Cloud")
                        }

                        Button(
                            onClick = { isEditing = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_profile_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                editName = user.name
                                editTitle = user.title
                                editBio = user.bio
                                editTargetARR = if (user.targetPortfolioARR <= 0) "1000000"
                                    else if (user.targetPortfolioARR % 1.0 == 0.0) String.format(Locale.US, "%.0f", user.targetPortfolioARR)
                                    else user.targetPortfolioARR.toString()
                                editAvatarUrl = user.avatarUrl
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val sanitized = editTargetARR.replace(Regex("[^0-9.]"), "")
                                val targetNum = sanitized.toDoubleOrNull()?.coerceAtLeast(0.0) ?: user.targetPortfolioARR
                                val updated = user.copy(
                                    name = editName.ifBlank { "Founder" },
                                    title = editTitle.ifBlank { "Venture Architect" },
                                    bio = editBio.ifBlank { user.bio },
                                    avatarUrl = editAvatarUrl,
                                    targetPortfolioARR = targetNum
                                )
                                viewModel.updateUserProfile(updated)
                                isEditing = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_profile_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
