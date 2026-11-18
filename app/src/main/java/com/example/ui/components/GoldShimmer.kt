package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline

/**
 * Premium gold & dark obsidian shimmer gradient brush for skeleton screens
 */
@Composable
fun goldShimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color(0xFF1E1B12),
        Color(0xFF332A15),
        Color(0xFF55441E),
        Color(0xFF6B5525),
        Color(0xFF332A15),
        Color(0xFF1E1B12)
    )

    val transition = rememberInfiniteTransition(label = "gold_shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gold_shimmer_offset"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 600f, translateAnim - 600f),
        end = Offset(translateAnim, translateAnim)
    )
}

/**
 * Subtle gold circular loading spinner with studio gold accents and label
 */
@Composable
fun GoldLoadingSpinner(
    modifier: Modifier = Modifier,
    label: String? = null,
    size: Dp = 36.dp
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(size + 16.dp)
                .clip(CircleShape)
                .background(Color(0xFF221D12))
                .border(1.dp, GoldDark.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(size),
                color = GoldPrimary,
                trackColor = Color(0xFF3A3016),
                strokeWidth = 2.5.dp
            )
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = GoldLight.copy(alpha = 0.8f),
                modifier = Modifier.size(size / 2)
            )
        }

        if (!label.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = GoldLight,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Skeleton Grid for the Gallery Screen (2-column layout matching GalleryCard)
 */
@Composable
fun GallerySkeletonGrid(
    itemCount: Int = 6,
    modifier: Modifier = Modifier
) {
    val shimmerBrush = goldShimmerBrush()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("gallery_skeleton_grid")
    ) {
        items(itemCount) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column {
                    // Shimmer Image Box (Square 1:1)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(shimmerBrush)
                    ) {
                        // Badge placeholder
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .size(width = 44.dp, height = 14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2E2412))
                        )
                        // Category placeholder
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .size(width = 56.dp, height = 14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2E2412))
                        )
                    }

                    // Shimmer Title & Subtitle lines
                    Column(modifier = Modifier.padding(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Skeleton List for Admin Dashboard management tabs
 */
@Composable
fun AdminSkeletonList(
    itemCount: Int = 4,
    modifier: Modifier = Modifier
) {
    val shimmerBrush = goldShimmerBrush()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Row: Title & Status pill placeholder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmerBrush)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Detail Line 1
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Detail Line 2
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons row placeholder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Skeleton List for Bookings and Model Applications
 */
@Composable
fun BookingsSkeletonList(
    itemCount: Int = 3,
    modifier: Modifier = Modifier
) {
    val shimmerBrush = goldShimmerBrush()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(160.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmerBrush)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}
