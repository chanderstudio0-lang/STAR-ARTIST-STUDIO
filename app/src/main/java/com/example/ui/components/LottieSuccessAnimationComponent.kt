package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

/**
 * Subtle Lottie-style animated success checkmark and fade-in confirmation card.
 * Uses high-performance vector Canvas path stroke drawing, expanding pulse glow rings,
 * floating golden sparkles, and fade-in session summary.
 */
@Composable
fun LottieSuccessAnimationComponent(
    clientName: String,
    serviceType: String,
    date: String,
    time: String,
    location: String,
    bookingId: String = "",
    language: AppLanguage = AppLanguage.ENGLISH,
    onViewTimeline: () -> Unit = {},
    onBookAnother: () -> Unit = {},
    onWhatsAppConsult: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Animation drivers
    val circleScale = remember { Animatable(0f) }
    val checkmarkProgress = remember { Animatable(0f) }
    val messageAlpha = remember { Animatable(0f) }

    // Infinite breathing glow transition
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val pulseRingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ring"
    )
    val pulseRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(Unit) {
        // Step 1: Pop in the golden circle
        circleScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
        // Step 2: Draw the checkmark path
        checkmarkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(550, easing = FastOutSlowInEasing)
        )
        // Step 3: Fade in the confirmation card and session review details
        messageAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = LinearEasing)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("booking_success_animation"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // =====================================================================
        // LOTTIE-STYLE ANIMATED CANVAS (Pulse Rings + Checkmark Stroke Drawing)
        // =====================================================================
        Box(
            modifier = Modifier
                .size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulse glow ring 1
            Box(
                modifier = Modifier
                    .size(110.dp * pulseRingScale)
                    .clip(CircleShape)
                    .background(GoldPrimary.copy(alpha = pulseRingAlpha))
            )

            // Pulse glow ring 2 (inner subtle)
            Box(
                modifier = Modifier
                    .size(90.dp * (pulseRingScale * 0.9f))
                    .clip(CircleShape)
                    .background(GoldLight.copy(alpha = pulseRingAlpha * 0.7f))
            )

            val primaryGold = GoldPrimary
            val lightGold = GoldLight
            val darkGold = GoldDark

            // Center Badge with Animated Vector Path Stroke
            Canvas(
                modifier = Modifier
                    .size(76.dp)
                    .testTag("animated_success_canvas")
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.minDimension / 2f - 4.dp.toPx()) * circleScale.value

                if (radius > 0f) {
                    // Outer golden circle background
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF2C2410), Color(0xFF141004)),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )

                    // Outer golden border stroke
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(primaryGold, lightGold, darkGold, primaryGold),
                            center = center
                        ),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Checkmark Path Stroke Drawing
                    val path = Path().apply {
                        val startX = size.width * 0.30f
                        val startY = size.height * 0.52f
                        val midX = size.width * 0.45f
                        val midY = size.height * 0.67f
                        val endX = size.width * 0.72f
                        val endY = size.height * 0.36f

                        moveTo(startX, startY)
                        lineTo(midX, midY)
                        lineTo(endX, endY)
                    }

                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(path, false)
                    val pathLength = pathMeasure.length
                    val drawLength = pathLength * checkmarkProgress.value

                    val animatedPath = Path()
                    pathMeasure.getSegment(0f, drawLength, animatedPath, true)

                    drawPath(
                        path = animatedPath,
                        color = lightGold,
                        style = Stroke(
                            width = 5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =====================================================================
        // FADE-IN CONFIRMATION MESSAGE & SESSION REVIEW CARD
        // =====================================================================
        AnimatedVisibility(
            visible = messageAlpha.value > 0.1f,
            enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(400))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.2.dp,
                        Brush.verticalGradient(listOf(GoldPrimary, StudioOutline, SuccessGreen.copy(alpha = 0.5f))),
                        RoundedCornerShape(18.dp)
                    )
                    .testTag("booking_success_confirmation"),
                colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Success Header
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldContainer)
                            .border(0.8.dp, GoldPrimary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "बुकिंग अनुरोध प्राप्त हुआ" else "REQUEST RECEIVED & PERSISTED",
                            color = GoldLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Text(
                        text = if (language == AppLanguage.HINDI) "शूट बुकिंग सफलतापूर्वक दर्ज की गई!" else "Session Request Confirmed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldLight,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "आपकी बुकिंग का विवरण क्लाउड फायरस्टोर में सुरक्षित रूप से सहेज लिया गया है। तलवाड़ा खुर्द स्टूडियो टीम द्वारा समीक्षा उपरांत पुष्टि की जाएगी।"
                        else
                            "Your session request has been saved to Cloud Firestore. Studio director is reviewing equipment setup and slot confirmation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD4C8B0),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    // Booking Details Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141006)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StudioOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Service Type
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "सेवा:" else "Service:",
                                    color = Color.Gray,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = serviceType,
                                    color = GoldLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Date & Time
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "तिथि व समय:" else "Date & Time:",
                                    color = Color.Gray,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$date • $time",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Client Name
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "ग्राहक:" else "Client:",
                                    color = Color.Gray,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = clientName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Location
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "स्थान:" else "Location:",
                                    color = Color.Gray,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = location,
                                    color = Color.LightGray,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            if (bookingId.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ID: $bookingId",
                                        color = SuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onViewTimeline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("view_booking_timeline_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = StudioBlack),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "बुकिंग स्थिति व टाइमलाइन देखें" else "Track Approval Timeline",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onBookAnother,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("book_another_session_button"),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.8f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "नया सत्र अनुरोध करें" else "Book Another Session",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
