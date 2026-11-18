package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPrimary

/**
 * Vector-drawn official 4-color Google "G" emblem inside a crisp circular badge
 * tailored for high contrast on STAR ARTIST STUDIO's Black & Gold buttons.
 */
@Composable
fun GoogleLogoBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(Color.White)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(16.dp)
                .semantics { contentDescription = "Google" }
        ) {
            val strokeWidth = size.minDimension * 0.22f
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            // Red top arc
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 212f,
                sweepAngle = 108f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            // Yellow left arc
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 140f,
                sweepAngle = 78f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            // Green bottom arc
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 40f,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            // Blue right curve + horizontal bar of "G"
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = -5f,
                sweepAngle = 52f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            val barHeight = strokeWidth
            val barWidth = size.width * 0.48f
            drawRoundRect(
                color = Color(0xFF4285F4),
                topLeft = Offset(size.width * 0.50f, (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
        }
    }
}

/**
 * Interactive Google Sign-In button backed by Jetpack Credential Manager & Firebase Auth,
 * styled in STAR ARTIST STUDIO's luxury Black & Gold theme.
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    enabled: Boolean = true,
    isStudioLight: Boolean = false,
    text: String = "Sign in with Google",
    loadingText: String = "Connecting Google...",
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .minimumInteractiveComponentSize()
            .testTag("auth_google_signin_button"),
        border = BorderStroke(1.5.dp, GoldPrimary),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isStudioLight) Color(0xFFFDFBF5) else Color(0xFF1B1812),
            contentColor = if (isStudioLight) Color(0xFF1E1705) else GoldPrimary,
            disabledContainerColor = if (isStudioLight) Color(0xFFF4EFE0) else Color(0xFF17140E),
            disabledContentColor = GoldPrimary.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = GoldPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = loadingText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = GoldPrimary
                )
            } else {
                GoogleLogoBadge()
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isStudioLight) Color(0xFF1E1705) else GoldPrimary
                )
            }
        }
    }
}
