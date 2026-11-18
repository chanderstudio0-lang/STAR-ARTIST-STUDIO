package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.theme.DarkStudioBlack
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioTheme

/**
 * Premium Black & Gold Segmented Language Toggle Component (Hindi / English).
 * Updates the app's string resources and UI state dynamically when tapped.
 */
@Composable
fun LanguageToggleComponent(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (StudioTheme.isLight) Color(0xFFE5E7EB) else Color(0xFF16140E))
            .border(
                BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                RoundedCornerShape(20.dp)
            )
            .padding(3.dp)
            .testTag("language_toggle_component")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            LanguageSegmentButton(
                label = "ENG",
                subLabel = "English",
                isSelected = currentLanguage == AppLanguage.ENGLISH,
                testTag = "language_toggle_en",
                onClick = { onLanguageSelected(AppLanguage.ENGLISH) }
            )

            LanguageSegmentButton(
                label = "हिंदी",
                subLabel = "Hindi",
                isSelected = currentLanguage == AppLanguage.HINDI,
                testTag = "language_toggle_hi",
                onClick = { onLanguageSelected(AppLanguage.HINDI) }
            )
        }
    }
}

/**
 * Individual segmented button within the language toggle.
 */
@Composable
private fun LanguageSegmentButton(
    label: String,
    subLabel: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) GoldPrimary else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "segment_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (StudioTheme.isLight) Color.White else DarkStudioBlack
        } else {
            if (StudioTheme.isLight) Color(0xFF374151) else GoldLight.copy(alpha = 0.75f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "segment_text"
    )

    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = GoldLight),
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (StudioTheme.isLight) Color.White else DarkStudioBlack)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                color = textColor,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                letterSpacing = 0.4.sp
            )
        }
    }
}

/**
 * Compact circular/pill language toggle icon for action bars and navigation headers.
 */
@Composable
fun CompactLanguageToggle(
    currentLanguage: AppLanguage,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (StudioTheme.isLight) Color(0xFFE5E7EB) else Color(0xFF241F12))
            .border(1.dp, GoldPrimary, RoundedCornerShape(17.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = GoldPrimary),
                onClick = onToggle
            )
            .padding(horizontal = 10.dp)
            .testTag("compact_language_toggle"),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = "Language",
                tint = GoldPrimary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (currentLanguage == AppLanguage.ENGLISH) "हिंदी" else "ENG",
                color = if (StudioTheme.isLight) GoldPrimary else GoldLight,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
