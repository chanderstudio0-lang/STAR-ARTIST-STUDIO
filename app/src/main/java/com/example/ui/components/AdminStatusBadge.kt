package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline

/**
 * Unified status indicators with distinct color-coded styling for Admin management readability:
 * - PENDING: Warm Amber / Gold
 * - APPROVED / CONFIRMED: Emerald Success Green
 * - COMPLETED: Royal Sky Blue
 * - REJECTED / CANCELLED: Crimson Red
 */
enum class AdminItemStatus(
    val key: String,
    val displayLabel: String,
    val shortLabel: String,
    val hindiLabel: String,
    val primaryColor: Color,
    val backgroundColor: Color,
    val borderColor: Color,
    val icon: ImageVector
) {
    PENDING(
        key = "pending",
        displayLabel = "PENDING REVIEW",
        shortLabel = "Pending",
        hindiLabel = "लंबित",
        primaryColor = Color(0xFFFBBF24), // Amber Gold
        backgroundColor = Color(0xFF362205),
        borderColor = Color(0xFFD97706),
        icon = Icons.Default.HourglassTop
    ),
    APPROVED(
        key = "approved",
        displayLabel = "APPROVED",
        shortLabel = "Approved",
        hindiLabel = "स्वीकृत",
        primaryColor = Color(0xFF10B981), // Emerald Green
        backgroundColor = Color(0xFF064E3B),
        borderColor = Color(0xFF059669),
        icon = Icons.Default.CheckCircle
    ),
    CONFIRMED(
        key = "confirmed",
        displayLabel = "CONFIRMED",
        shortLabel = "Confirmed",
        hindiLabel = "पुष्ट",
        primaryColor = Color(0xFF10B981), // Emerald Green
        backgroundColor = Color(0xFF064E3B),
        borderColor = Color(0xFF059669),
        icon = Icons.Default.CheckCircle
    ),
    COMPLETED(
        key = "completed",
        displayLabel = "COMPLETED",
        shortLabel = "Completed",
        hindiLabel = "पूर्ण",
        primaryColor = Color(0xFF38BDF8), // Sky Blue
        backgroundColor = Color(0xFF0C4A6E),
        borderColor = Color(0xFF0284C7),
        icon = Icons.Default.Check
    ),
    REJECTED(
        key = "rejected",
        displayLabel = "REJECTED",
        shortLabel = "Rejected",
        hindiLabel = "अस्वीकृत",
        primaryColor = Color(0xFFEF4444), // Crimson Red
        backgroundColor = Color(0xFF450A0A),
        borderColor = Color(0xFFDC2626),
        icon = Icons.Default.Close
    ),
    CANCELLED(
        key = "cancelled",
        displayLabel = "CANCELLED",
        shortLabel = "Cancelled",
        hindiLabel = "रद्द",
        primaryColor = Color(0xFFEF4444), // Crimson Red
        backgroundColor = Color(0xFF450A0A),
        borderColor = Color(0xFFDC2626),
        icon = Icons.Default.Close
    );

    companion object {
        fun from(statusStr: String): AdminItemStatus {
            return when (statusStr.lowercase().trim()) {
                "approved" -> APPROVED
                "confirmed" -> CONFIRMED
                "completed" -> COMPLETED
                "rejected" -> REJECTED
                "cancelled", "canceled" -> CANCELLED
                else -> PENDING
            }
        }
    }
}

/**
 * Color-coded Material 3 pill chip for Firestore booking request status ('Pending', 'Confirmed',
 * 'Completed', 'Cancelled') designed for instant readability in Admin Dashboard cards & summaries.
 */
@Composable
fun BookingStatusChip(
    statusStr: String,
    modifier: Modifier = Modifier,
    language: AppLanguage = AppLanguage.ENGLISH,
    count: Int? = null,
    onClick: (() -> Unit)? = null
) {
    val status = AdminItemStatus.from(statusStr)
    val baseLabel = if (language == AppLanguage.HINDI) status.hindiLabel else status.shortLabel
    val displayText = if (count != null) "$baseLabel ($count)" else baseLabel

    val baseModifier = modifier
        .let { if (onClick != null) it.defaultMinSize(minHeight = 40.dp) else it }
        .semantics { contentDescription = "Booking status: $displayText" }

    Surface(
        modifier = if (onClick != null) baseModifier.clickable(onClick = onClick) else baseModifier,
        shape = CircleShape,
        color = status.backgroundColor,
        border = BorderStroke(1.2.dp, status.borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(status.primaryColor)
            )
            Icon(
                imageVector = status.icon,
                contentDescription = null,
                tint = status.primaryColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = displayText,
                color = status.primaryColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

/**
 * Premium Status Indicator Badge with distinct status color, icon, and glowing indicator dot.
 */
@Composable
fun AdminStatusBadge(
    statusStr: String,
    modifier: Modifier = Modifier,
    language: AppLanguage = AppLanguage.ENGLISH
) {
    val status = AdminItemStatus.from(statusStr)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(status.backgroundColor)
            .border(1.dp, status.borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(status.primaryColor)
        )
        Icon(
            imageVector = status.icon,
            contentDescription = null,
            tint = status.primaryColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = if (language == AppLanguage.HINDI) status.hindiLabel else status.displayLabel,
            color = status.primaryColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.4.sp
        )
    }
}

/**
 * Interactive color-coded filter chip for admin dashboards with colored status indicator and count.
 */
@Composable
fun StatusFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    indicatorColor: Color?,
    onClick: () -> Unit,
    testTag: String
) {
    val activeAccent = indicatorColor ?: GoldPrimary
    val bgColor = if (isSelected) {
        if (indicatorColor != null) indicatorColor.copy(alpha = 0.2f) else GoldContainer
    } else {
        StudioElevatedSurface
    }
    val borderColor = if (isSelected) {
        activeAccent
    } else {
        (indicatorColor ?: StudioOutline).copy(alpha = 0.55f)
    }

    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (indicatorColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
            }
            Text(
                text = "$label ($count)",
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    if (indicatorColor != null) indicatorColor else GoldLight
                } else {
                    Color.LightGray
                }
            )
        }
    }
}
