package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.BlogPost
import com.example.data.model.GalleryItem
import com.example.data.model.ModelRegistration
import com.example.data.model.ShootBooking
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.StudioStrings

/**
 * Encapsulated operational metric calculations for STAR ARTIST STUDIO administrative console.
 */
data class AdminDashboardMetrics(
    val pendingApprovalsTotal: Int,
    val pendingModelsCount: Int,
    val pendingBookingsCount: Int,
    val pendingGalleryCount: Int,
    val upcomingBookingsTotal: Int,
    val confirmedBookingsCount: Int,
    val activeUsersTotal: Int,
    val totalModelsCount: Int,
    val totalBookingsCount: Int,
    val approvedGalleryCount: Int,
    val totalGalleryCount: Int,
    val blogPostsCount: Int = 0,
    val completedBookingsCount: Int = 0,
    val cancelledBookingsCount: Int = 0
) {
    companion object {
        fun compute(
            models: List<ModelRegistration>,
            bookings: List<ShootBooking>,
            galleryItems: List<GalleryItem>,
            blogPosts: List<BlogPost> = emptyList()
        ): AdminDashboardMetrics {
            val pendingModels = models.count { it.status.isBlank() || it.status.equals("pending", ignoreCase = true) }
            val pendingBookings = bookings.count { it.status.isBlank() || it.status.equals("pending", ignoreCase = true) }
            val pendingGallery = galleryItems.count { it.status.isBlank() || it.status.equals("pending", ignoreCase = true) }
            val pendingTotal = pendingModels + pendingBookings + pendingGallery

            val confirmedBookings = bookings.count {
                val normalized = it.status.trim().lowercase()
                normalized == "confirmed" || normalized == "approved"
            }
            val completedBookings = bookings.count { it.status.trim().equals("completed", ignoreCase = true) }
            val cancelledBookings = bookings.count {
                val normalized = it.status.trim().lowercase()
                normalized == "cancelled" || normalized == "canceled" || normalized == "rejected"
            }
            val upcomingTotal = confirmedBookings + pendingBookings

            val uniqueUserIds = (models.map { it.userId } + bookings.map { it.userId } + galleryItems.map { it.userId })
                .filter { it.isNotBlank() && !it.equals("anonymous", ignoreCase = true) && !it.contains("demo", ignoreCase = true) }
                .toSet()

            val uniquePhones = (models.mapNotNull { it.phoneNumber.takeIf { p -> p.isNotBlank() } } +
                    bookings.mapNotNull { it.contactNumber.takeIf { p -> p.isNotBlank() } }).toSet()

            val activeUsers = maxOf(uniqueUserIds.size, uniquePhones.size)

            val approvedGallery = galleryItems.count { it.status.equals("approved", ignoreCase = true) }

            return AdminDashboardMetrics(
                pendingApprovalsTotal = pendingTotal,
                pendingModelsCount = pendingModels,
                pendingBookingsCount = pendingBookings,
                pendingGalleryCount = pendingGallery,
                upcomingBookingsTotal = upcomingTotal,
                confirmedBookingsCount = confirmedBookings,
                activeUsersTotal = activeUsers,
                totalModelsCount = models.size,
                totalBookingsCount = bookings.size,
                approvedGalleryCount = approvedGallery,
                totalGalleryCount = galleryItems.size,
                blogPostsCount = blogPosts.size,
                completedBookingsCount = completedBookings,
                cancelledBookingsCount = cancelledBookings
            )
        }
    }
}

/**
 * Summary dashboard component for studio administrators.
 * Displays real-time operational indicators:
 * - Pending Approvals (Model registrations + Shoot bookings + Gallery submissions)
 * - Upcoming Bookings (Confirmed + Pending scheduled shoots)
 * - Total Active Users (Deduplicated talent & client roster)
 * - Approved Showcase (Live portfolio media works)
 */
@Composable
fun AdminSummaryDashboard(
    metrics: AdminDashboardMetrics,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onSelectTab: (Int) -> Unit = {}
) {
    val s = StudioStrings(language)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("admin_summary_dashboard")
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            GoldPrimary.copy(alpha = 0.5f),
                            StudioOutline.copy(alpha = 0.4f),
                            GoldPrimary.copy(alpha = 0.3f)
                        )
                    )
                ),
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Section Title & Live Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(GoldContainer)
                            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Studio Metrics",
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = s.adminMetricsTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight,
                            fontSize = 13.5.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Analytics Quick Action Button (Jumps to Recharts Visual Analytics tab)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldPrimary.copy(alpha = 0.16f))
                            .border(0.8.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .clickable { onSelectTab(4) }
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                            .testTag("admin_summary_analytics_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "View Visual Analytics",
                                tint = GoldPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "एनालिटिक्स" else "ANALYTICS",
                                color = GoldLight,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }

                    // Live Sync Badge with glowing emerald dot
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F2618))
                            .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                            )
                            Text(
                                text = s.adminLiveSyncLabel,
                                color = SuccessGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Responsive 2x2 or 4x1 metrics grid depending on screen width
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 540.dp

                if (isWide) {
                    // Expanded screen / Tablet: 4 cards in single row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminMetricCard(
                            modifier = Modifier.weight(1f),
                            title = s.metricPendingApprovals,
                            count = metrics.pendingApprovalsTotal,
                            subtitle = "${metrics.pendingModelsCount} M • ${metrics.pendingBookingsCount} B • ${metrics.pendingGalleryCount} G",
                            accentColor = Color(0xFFFBBF24), // Amber Gold
                            icon = Icons.Default.HourglassTop,
                            testTag = "metric_card_pending_approvals",
                            onClick = {
                                when {
                                    metrics.pendingModelsCount > 0 -> onSelectTab(0)
                                    metrics.pendingBookingsCount > 0 -> onSelectTab(1)
                                    metrics.pendingGalleryCount > 0 -> onSelectTab(2)
                                    else -> onSelectTab(0)
                                }
                            }
                        )

                        AdminMetricCard(
                            modifier = Modifier.weight(1f),
                            title = s.metricUpcomingBookings,
                            count = metrics.upcomingBookingsTotal,
                            subtitle = "${metrics.confirmedBookingsCount} Confirmed • ${metrics.pendingBookingsCount} Pending",
                            accentColor = Color(0xFF10B981), // Emerald Green
                            icon = Icons.Default.CalendarMonth,
                            testTag = "metric_card_upcoming_bookings",
                            onClick = { onSelectTab(1) }
                        )

                        AdminMetricCard(
                            modifier = Modifier.weight(1f),
                            title = s.metricActiveUsers,
                            count = metrics.activeUsersTotal,
                            subtitle = "${metrics.totalModelsCount} Models • ${metrics.totalBookingsCount} Bookings",
                            accentColor = Color(0xFF38BDF8), // Royal Sky Blue
                            icon = Icons.Default.Group,
                            testTag = "metric_card_active_users",
                            onClick = { onSelectTab(0) }
                        )

                        AdminMetricCard(
                            modifier = Modifier.weight(1f),
                            title = s.metricGalleryShowcase,
                            count = metrics.approvedGalleryCount,
                            subtitle = "${metrics.approvedGalleryCount} Live • ${metrics.totalGalleryCount} Total",
                            accentColor = GoldPrimary,
                            icon = Icons.Default.PhotoLibrary,
                            testTag = "metric_card_gallery_showcase",
                            onClick = { onSelectTab(2) }
                        )
                    }
                } else {
                    // Compact / Mobile Handheld: 2x2 grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdminMetricCard(
                                modifier = Modifier.weight(1f),
                                title = s.metricPendingApprovals,
                                count = metrics.pendingApprovalsTotal,
                                subtitle = "${metrics.pendingModelsCount} M • ${metrics.pendingBookingsCount} B • ${metrics.pendingGalleryCount} G",
                                accentColor = Color(0xFFFBBF24), // Amber Gold
                                icon = Icons.Default.HourglassTop,
                                testTag = "metric_card_pending_approvals",
                                onClick = {
                                    when {
                                        metrics.pendingModelsCount > 0 -> onSelectTab(0)
                                        metrics.pendingBookingsCount > 0 -> onSelectTab(1)
                                        metrics.pendingGalleryCount > 0 -> onSelectTab(2)
                                        else -> onSelectTab(0)
                                    }
                                }
                            )

                            AdminMetricCard(
                                modifier = Modifier.weight(1f),
                                title = s.metricUpcomingBookings,
                                count = metrics.upcomingBookingsTotal,
                                subtitle = "${metrics.confirmedBookingsCount} Confirmed • ${metrics.pendingBookingsCount} Pending",
                                accentColor = Color(0xFF10B981), // Emerald Green
                                icon = Icons.Default.CalendarMonth,
                                testTag = "metric_card_upcoming_bookings",
                                onClick = { onSelectTab(1) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdminMetricCard(
                                modifier = Modifier.weight(1f),
                                title = s.metricActiveUsers,
                                count = metrics.activeUsersTotal,
                                subtitle = "${metrics.totalModelsCount} Models • ${metrics.totalBookingsCount} Bookings",
                                accentColor = Color(0xFF38BDF8), // Royal Sky Blue
                                icon = Icons.Default.Group,
                                testTag = "metric_card_active_users",
                                onClick = { onSelectTab(0) }
                            )

                            AdminMetricCard(
                                modifier = Modifier.weight(1f),
                                title = s.metricGalleryShowcase,
                                count = metrics.approvedGalleryCount,
                                subtitle = "${metrics.approvedGalleryCount} Live • ${metrics.totalGalleryCount} Total",
                                accentColor = GoldPrimary,
                                icon = Icons.Default.PhotoLibrary,
                                testTag = "metric_card_gallery_showcase",
                                onClick = { onSelectTab(2) }
                            )
                        }
                    }
                }
            }

            // Color-coded Booking Status Indicator Chips across all Firestore booking requests
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("admin_overview_booking_status_chips"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookingStatusChip(
                    statusStr = "Pending",
                    language = language,
                    count = metrics.pendingBookingsCount,
                    onClick = { onSelectTab(1) },
                    modifier = Modifier.testTag("overview_chip_booking_pending")
                )
                BookingStatusChip(
                    statusStr = "Confirmed",
                    language = language,
                    count = metrics.confirmedBookingsCount,
                    onClick = { onSelectTab(1) },
                    modifier = Modifier.testTag("overview_chip_booking_confirmed")
                )
                BookingStatusChip(
                    statusStr = "Completed",
                    language = language,
                    count = metrics.completedBookingsCount,
                    onClick = { onSelectTab(1) },
                    modifier = Modifier.testTag("overview_chip_booking_completed")
                )
                BookingStatusChip(
                    statusStr = "Cancelled",
                    language = language,
                    count = metrics.cancelledBookingsCount,
                    onClick = { onSelectTab(1) },
                    modifier = Modifier.testTag("overview_chip_booking_cancelled")
                )
            }
        }
    }
}

/**
 * Individual metric tile featuring responsive typography, color accenting,
 * icon badge, and minimum 48dp touch target accessibility.
 */
@Composable
private fun AdminMetricCard(
    title: String,
    count: Int,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StudioElevatedSurface)
            .border(
                BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Number
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    fontSize = 22.sp,
                    lineHeight = 24.sp
                )

                // Icon in translucent badge
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(0.5.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle / Breakdown
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
