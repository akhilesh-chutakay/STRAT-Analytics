package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.UserProfile

@Composable
fun UserDpAvatar(
    user: UserProfile,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    showIndicator: Boolean = true,
    indicatorColor: Color = Color(0xFF4ADE80),
    borderWidth: Dp = 1.5.dp,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(clickModifier)
            .testTag("user_dp_avatar"),
        contentAlignment = Alignment.Center
    ) {
        // Outer avatar container
        Surface(
            shape = CircleShape,
            border = BorderStroke(
                borderWidth,
                Brush.linearGradient(
                    listOf(
                        borderColor,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                    )
                )
            ),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (user.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user.avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture for ${user.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    // Initials Fallback
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFF8B5CF6)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = (size.value * 0.38f).sp
                        )
                    }
                }
            }
        }

        // Live Indicator Dot (Cloud Synced / Active)
        if (showIndicator) {
            val indicatorSize = (size.value * 0.28f).coerceIn(8f, 14f).dp
            Box(
                modifier = Modifier
                    .size(indicatorSize)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(indicatorColor)
                    .border(
                        1.5.dp,
                        MaterialTheme.colorScheme.background,
                        CircleShape
                    )
            )
        }
    }
}
