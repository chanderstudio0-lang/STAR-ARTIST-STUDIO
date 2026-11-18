package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppLanguage
import com.example.data.model.ServiceReview
import com.example.data.model.ShootBooking
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

fun validateServiceReview(
    rating: Int,
    comment: String,
    isHindi: Boolean = false
): String? {
    if (rating !in 1..5) {
        return if (isHindi) "कृपया 1 से 5 स्टार रेटिंग चुनें।" else "Please select a rating between 1 and 5 stars."
    }
    val trimmed = comment.trim()
    if (trimmed.length < 5) {
        return if (isHindi) "कृपया कम से कम 5 अक्षरों की समीक्षा लिखें।" else "Please write at least 5 characters in your review."
    }
    if (trimmed.length > 1000) {
        return if (isHindi) "समीक्षा अधिकतम 1000 अक्षरों की हो सकती है।" else "Review comment must be 1000 characters or less."
    }
    return null
}

fun calculateAverageReviewRating(reviews: List<ServiceReview>): Double {
    if (reviews.isEmpty()) return 0.0
    val validRatings = reviews.map { it.rating.coerceIn(1L, 5L) }
    return validRatings.sum().toDouble() / validRatings.size
}

@Composable
fun InteractiveStarRatingBar(
    rating: Int,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 32.dp,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { star ->
            val isSelected = star <= rating
            IconButton(
                onClick = { onRatingSelected(star) },
                enabled = enabled,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("review_star_$star")
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Rate $star out of 5 stars",
                    tint = if (isSelected) GoldPrimary else TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(starSize)
                )
            }
        }
    }
}

@Composable
fun ReadOnlyStarRow(
    rating: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 16.dp
) {
    val clamped = rating.coerceIn(0, 5)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { star ->
            val isFilled = star <= clamped
            Icon(
                imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.StarOutline,
                contentDescription = if (star == 1) "$clamped out of 5 stars" else null,
                tint = if (isFilled) GoldPrimary else TextSecondary.copy(alpha = 0.45f),
                modifier = Modifier.size(starSize)
            )
        }
    }
}

@Composable
fun ServiceReviewDialog(
    booking: ShootBooking,
    existingReview: ServiceReview?,
    language: AppLanguage,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmitReview: (rating: Int, comment: String, onSuccess: () -> Unit) -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    var selectedRating by remember(booking.id, existingReview?.id) {
        mutableIntStateOf((existingReview?.rating?.toInt() ?: 5).coerceIn(1, 5))
    }
    var comment by remember(booking.id, existingReview?.id) {
        mutableStateOf(existingReview?.comment.orEmpty())
    }
    var localError by remember { mutableStateOf<String?>(null) }

    val ratingDescriptor = when (selectedRating) {
        1 -> if (isHindi) "1/5 • सुधार की आवश्यकता" else "1/5 • Needs Improvement"
        2 -> if (isHindi) "2/5 • सामान्य अनुभव" else "2/5 • Fair"
        3 -> if (isHindi) "3/5 • अच्छा अनुभव" else "3/5 • Good Studio Session"
        4 -> if (isHindi) "4/5 • बहुत बढ़िया सेवा" else "4/5 • Great Quality & Team"
        else -> if (isHindi) "5/5 • उत्कृष्ट स्टूडियो अनुभव!" else "5/5 • Outstanding Studio Experience!"
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        containerColor = StudioCardSurface,
        modifier = Modifier.testTag("service_review_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GoldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RateReview,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (existingReview != null) {
                            if (isHindi) "समीक्षा संपादित करें" else "Edit Service Review"
                        } else {
                            if (isHindi) "स्टूडियो सेवा की समीक्षा करें" else "Rate & Review Service"
                        },
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = booking.serviceType,
                        color = GoldPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isHindi)
                        "सत्र दिनांक: ${booking.preferredDate} • ${booking.preferredTime}"
                    else
                        "Session: ${booking.preferredDate} • ${booking.preferredTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioElevatedSurface)
                        .border(1.dp, StudioOutline, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isHindi) "अपनी स्टार रेटिंग चुनें" else "Tap to Rate Your Experience",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    InteractiveStarRatingBar(
                        rating = selectedRating,
                        onRatingSelected = {
                            selectedRating = it
                            localError = null
                        },
                        enabled = !isSubmitting
                    )
                    Text(
                        text = ratingDescriptor,
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = {
                        if (it.length <= 1000) {
                            comment = it
                            localError = null
                        }
                    },
                    label = {
                        Text(
                            if (isHindi) "अपनी लिखित समीक्षा साझा करें *" else "Write Your Review *",
                            fontSize = 13.sp
                        )
                    },
                    placeholder = {
                        Text(
                            if (isHindi)
                                "लाइटिंग, निर्देशन, फोटोग्राफी और स्टूडियो टीम के साथ आपका अनुभव कैसा रहा?"
                            else
                                "Share how the photography, lighting, crew, and studio session went...",
                            fontSize = 12.sp
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_comment_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = StudioOutline,
                        focusedLabelColor = GoldLight,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = GoldPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = localError ?: (if (isHindi) "कम से कम 5 अक्षर आवश्यक" else "Minimum 5 characters"),
                        color = if (localError != null) ErrorRed else TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${comment.length}/1000",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val error = validateServiceReview(selectedRating, comment, isHindi)
                    if (error != null) {
                        localError = error
                    } else {
                        localError = null
                        onSubmitReview(selectedRating, comment.trim(), onDismiss)
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = StudioBlack
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("submit_review_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = StudioBlack,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "सहेज रहा है..." else "Saving...",
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = if (existingReview != null) {
                            if (isHindi) "समीक्षा अपडेट करें" else "Update Review"
                        } else {
                            if (isHindi) "समीक्षा सबमिट करें" else "Submit Review"
                        },
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("cancel_review_button")
            ) {
                Text(
                    text = if (isHindi) "रद्द करें" else "Cancel",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
fun BookingReviewStatusBlock(
    booking: ShootBooking,
    existingReview: ServiceReview?,
    language: AppLanguage,
    onOpenReviewDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!booking.status.equals("Completed", ignoreCase = true)) return
    val isHindi = language == AppLanguage.HINDI

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        if (existingReview == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(GoldContainer.copy(alpha = 0.45f))
                    .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi)
                                "शूट पूर्ण! अपनी स्टार रेटिंग व समीक्षा साझा करें।"
                            else
                                "Session completed! Share your rating & experience.",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onOpenReviewDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = StudioBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("rate_service_button_${booking.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "स्टार रेटिंग व समीक्षा दें" else "Leave a Star Rating & Review",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioElevatedSurface)
                    .border(1.dp, SuccessGreen.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
                    .testTag("submitted_review_card_${booking.id}")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ReadOnlyStarRow(rating = existingReview.rating.toInt(), starSize = 15.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${existingReview.rating}/5",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldLight,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "समीक्षा सहेजी गई" else "Reviewed",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "\"${existingReview.comment}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontStyle = FontStyle.Italic
                    )

                    OutlinedButton(
                        onClick = onOpenReviewDialog,
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("edit_review_button_${booking.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = GoldLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "समीक्षा संपादित करें" else "Edit Review",
                            color = GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudioReviewsShowcaseCard(
    reviews: List<ServiceReview>,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isHindi = language == AppLanguage.HINDI
    val avgRating = remember(reviews) { calculateAverageReviewRating(reviews) }
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
            .testTag("studio_reviews_showcase_card"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHindi) "ग्राहक रेटिंग व समीक्षाएं" else "Verified Studio Reviews",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                    Text(
                        text = if (reviews.isEmpty()) {
                            if (isHindi) "पूर्ण सत्रों के लिए पहली समीक्षा छोड़ें" else "Completed shoot clients can rate & review"
                        } else {
                            val formatted = String.format(Locale.US, "%.1f", avgRating)
                            if (isHindi) "$formatted / 5.0 औसत • ${reviews.size} समीक्षाएं"
                            else "$formatted / 5.0 average • ${reviews.size} client review${if (reviews.size == 1) "" else "s"}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(GoldContainer)
                        .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (reviews.isEmpty()) "5.0 ★" else String.format(Locale.US, "%.1f ★", avgRating),
                            color = OnGoldContainer,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (reviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioElevatedSurface)
                        .border(1.dp, StudioOutline, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isHindi)
                                "शूट पूर्ण होने के बाद 'मेरी बुकिंग' टैब में जाकर स्टार रेटिंग और समीक्षा दें।"
                            else
                                "Once a studio booking is marked Completed, rate your experience in My Bookings or Profile Activity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                reviews.take(4).forEach { review ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioElevatedSurface)
                            .border(1.dp, StudioOutline, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(GoldContainer)
                                            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (review.reviewerAvatarUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(review.reviewerAvatarUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = review.reviewerName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                text = review.reviewerName.firstOrNull()?.uppercase() ?: "S",
                                                color = OnGoldContainer,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = review.reviewerName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = review.serviceType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                ReadOnlyStarRow(rating = review.rating.toInt(), starSize = 14.dp)
                            }

                            Text(
                                text = "\"${review.comment}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

fun catalogReviewBookingId(packageId: String): String =
    "catalog_${packageId.trim()}"

fun filterReviewsForServicePackage(
    reviews: List<ServiceReview>,
    packageId: String,
    recommendedServiceType: String,
    titleEn: String
): List<ServiceReview> {
    val targetCatalogBookingId = catalogReviewBookingId(packageId)
    val cleanRecommended = recommendedServiceType.trim()
    val cleanTitle = titleEn.trim()
    return reviews.filter { review ->
        review.bookingId.equals(targetCatalogBookingId, ignoreCase = true) ||
            (cleanRecommended.isNotBlank() &&
                review.serviceType.trim().startsWith(cleanRecommended, ignoreCase = true)) ||
            (cleanTitle.isNotBlank() &&
                review.serviceType.contains(cleanTitle, ignoreCase = true))
    }
}

@Composable
fun ServiceCatalogFeedbackSection(
    packageId: String,
    serviceTitle: String,
    reviews: List<ServiceReview>,
    existingUserReview: ServiceReview?,
    isExpanded: Boolean,
    isSubmitting: Boolean,
    language: AppLanguage,
    onToggleExpanded: () -> Unit,
    onSubmitFeedback: (rating: Int, comment: String, onSuccess: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHindi = language == AppLanguage.HINDI
    val avgRating = remember(reviews) { calculateAverageReviewRating(reviews) }

    var draftRating by remember(packageId, existingUserReview?.id) {
        mutableIntStateOf((existingUserReview?.rating?.toInt() ?: 5).coerceIn(1, 5))
    }
    var draftComment by remember(packageId, existingUserReview?.id) {
        mutableStateOf(existingUserReview?.comment.orEmpty())
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StudioElevatedSurface)
            .border(1.dp, StudioOutline, RoundedCornerShape(14.dp))
            .padding(12.dp)
            .testTag("catalog_feedback_section_$packageId"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onToggleExpanded() }
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .testTag("catalog_feedback_toggle_$packageId"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = GoldContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (reviews.isEmpty()) {
                                if (isHindi) "नई समीक्षा" else "New"
                            } else {
                                String.format(Locale.US, "%.1f (%d)", avgRating, reviews.size)
                            },
                            color = OnGoldContainer,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (isHindi) {
                        if (reviews.isEmpty()) "रेटिंग व टिप्पणियाँ जोड़ें" else "समीक्षाएँ व टिप्पणियाँ (${reviews.size})"
                    } else {
                        if (reviews.isEmpty()) "Rate & Comment" else "Ratings & Comments (${reviews.size})"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse feedback for $serviceTitle" else "Expand feedback for $serviceTitle",
                tint = GoldPrimary
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_feedback_panel_$packageId"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioCardSurface)
                        .border(1.dp, StudioOutline, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (existingUserReview != null) {
                            if (isHindi) "अपनी रेटिंग व टिप्पणी अपडेट करें" else "Update Your Rating & Comment"
                        } else {
                            if (isHindi) "इस सेवा पर अपनी प्रतिक्रिया दें" else "Leave Your Star Rating & Comment"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            (1..5).forEach { star ->
                                val isSelected = star <= draftRating
                                IconButton(
                                    onClick = {
                                        draftRating = star
                                        validationError = null
                                    },
                                    enabled = !isSubmitting,
                                    modifier = Modifier
                                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("catalog_star_${packageId}_$star")
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "Rate $star stars for $serviceTitle",
                                        tint = if (isSelected) GoldPrimary else TextSecondary.copy(alpha = 0.55f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "$draftRating/5 ★",
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    OutlinedTextField(
                        value = draftComment,
                        onValueChange = {
                            if (it.length <= 1000) {
                                draftComment = it
                                validationError = null
                            }
                        },
                        label = {
                            Text(
                                text = if (isHindi) "अपनी टिप्पणी लिखें *" else "Write a comment *",
                                fontSize = 12.sp
                            )
                        },
                        placeholder = {
                            Text(
                                text = if (isHindi)
                                    "शूट गुणवत्ता, लाइटिंग और अनुभव के बारे में लिखें..."
                                else
                                    "Share feedback on shoot quality, lighting, or deliverables...",
                                fontSize = 12.sp
                            )
                        },
                        minLines = 2,
                        maxLines = 4,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("catalog_comment_input_$packageId"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = StudioOutline,
                            focusedLabelColor = GoldLight,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = GoldPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = validationError ?: (if (isHindi) "न्यूनतम 5 अक्षर" else "Min 5 characters"),
                            color = if (validationError != null) ErrorRed else TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${draftComment.length}/1000",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Button(
                        onClick = {
                            val err = validateServiceReview(draftRating, draftComment, isHindi)
                            if (err != null) {
                                validationError = err
                            } else {
                                validationError = null
                                onSubmitFeedback(draftRating, draftComment.trim()) {
                                    // Kept inline so updated comment appears immediately
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("catalog_submit_review_$packageId")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "सबमिट हो रहा है..." else "Submitting...",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.RateReview,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (existingUserReview != null) {
                                    if (isHindi) "प्रतिक्रिया अपडेट करें" else "Update Feedback"
                                } else {
                                    if (isHindi) "प्रतिक्रिया सबमिट करें" else "Post Rating & Comment"
                                },
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                if (reviews.isEmpty()) {
                    Text(
                        text = if (isHindi)
                            "अभी तक कोई टिप्पणी नहीं है। पहली समीक्षा साझा करें!"
                        else
                            "No comments yet for this service. Be the first to share feedback!",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .testTag("catalog_no_reviews_$packageId")
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("catalog_reviews_list_$packageId")
                    ) {
                        reviews.take(5).forEachIndexed { idx, review ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StudioCardSurface)
                                    .border(0.5.dp, StudioOutline, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                                    .semantics(mergeDescendants = true) {}
                                    .testTag("catalog_review_item_${packageId}_$idx")
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = review.reviewerName.ifBlank { "Studio Member" },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        ReadOnlyStarRow(
                                            rating = review.rating.toInt(),
                                            starSize = 13.dp
                                        )
                                    }
                                    Text(
                                        text = review.comment,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
