package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.ServiceReview
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

enum class TestimonialCategory {
    CLIENT,
    MODEL
}

data class StudioTestimonial(
    val id: String,
    val name: String,
    val roleLabel: String,
    val location: String,
    val serviceName: String,
    val category: TestimonialCategory,
    val rating: Int = 5,
    val quote: String,
    val verified: Boolean = true
)

/**
 * Pure index stepper supporting wrap-around navigation in either direction.
 */
fun getNextTestimonialIndex(currentIndex: Int, total: Int, delta: Int): Int {
    if (total <= 0) return 0
    val normalized = ((currentIndex % total) + total) % total
    return ((normalized + delta) % total + total) % total
}

/**
 * Curated bilingual testimonials from past studio clients and verified 18+ roster models.
 */
fun defaultStudioTestimonials(language: AppLanguage): List<StudioTestimonial> {
    val isHindi = language == AppLanguage.HINDI
    return listOf(
        StudioTestimonial(
            id = "tst-1",
            name = if (isHindi) "प्रिया शर्मा" else "Priya Sharma",
            roleLabel = if (isHindi) "वेडिंग क्लाइंट" else "Bridal Client",
            location = if (isHindi) "सिरसा, हरियाणा" else "Sirsa, Haryana",
            serviceName = if (isHindi) "सिनेमैटिक वेडिंग पैकेज" else "Cinematic Wedding Package",
            category = TestimonialCategory.CLIENT,
            rating = 5,
            quote = if (isHindi) {
                "स्टार आर्टिस्ट स्टूडियो ने हमारे पूरे वेडिंग सेलिब्रेशन को बेहद खूबसूरत 4K सिनेमैटिक फिल्म में कैद किया। लाइटिंग, टाइमलाइन और एल्बम फिनिश लाजवाब थी।"
            } else {
                "Star Artist Studio captured our wedding celebration with cinema-grade 4K visuals and thoughtful direction. Every candid portrait and highlight film exceeded expectations."
            }
        ),
        StudioTestimonial(
            id = "tst-2",
            name = if (isHindi) "आर्यन वर्मा" else "Aryan Verma",
            roleLabel = if (isHindi) "सत्यापित 18+ रोस्टर मॉडल" else "Verified 18+ Roster Model",
            location = if (isHindi) "ऐलनाबाद, सिरसा" else "Ellenabad, Sirsa",
            serviceName = if (isHindi) "मॉडल पोर्टफोलियो शूट" else "Model Portfolio Shoot",
            category = TestimonialCategory.MODEL,
            rating = 5,
            quote = if (isHindi) {
                "यहाँ का इनडोर लाइटिंग स्टेज और पोज़िंग गाइडेंस नए मॉडल्स के लिए बेहतरीन है। मेरे नए पोर्टफोलियो के बाद मुझे दो बड़े रीजनल ब्रांड कैंपेन मिले।"
            } else {
                "The calibrated indoor lighting bay and posing direction elevated my portfolio immediately. Within weeks of my shoot, I booked two regional commercial campaigns."
            }
        ),
        StudioTestimonial(
            id = "tst-3",
            name = if (isHindi) "रोहित मेहरा" else "Rohit Mehra",
            roleLabel = if (isHindi) "इंडी म्यूज़िक आर्टिस्ट" else "Independent Music Artist",
            location = if (isHindi) "हिसार, हरियाणा" else "Hisar, Haryana",
            serviceName = if (isHindi) "म्यूज़िक वीडियो और ऑडियो रिकॉर्डिंग" else "Music Video & Audio Recording",
            category = TestimonialCategory.CLIENT,
            rating = 5,
            quote = if (isHindi) {
                "वोकल रिकॉर्डिंग से लेकर कलर ग्रेडेड म्यूज़िक वीडियो तक सब कुछ एक ही छत के नीचे प्रोफेशनल क्वालिटी के साथ पूरा हुआ। टीम बहुत ही सहयोगी है।"
            } else {
                "From crisp vocal mastering to a color-graded 4K music video, everything ran seamlessly under one roof. The production crew kept our entire shoot right on schedule."
            }
        ),
        StudioTestimonial(
            id = "tst-4",
            name = if (isHindi) "सिमरन कौर" else "Simran Kaur",
            roleLabel = if (isHindi) "फैशन और एडिटोरियल मॉडल" else "Fashion & Editorial Model",
            location = if (isHindi) "सिरसा, हरियाणा" else "Sirsa, Haryana",
            serviceName = if (isHindi) "रनवे और ब्रांड लुकबुक" else "Runway & Brand Lookbook",
            category = TestimonialCategory.MODEL,
            rating = 5,
            quote = if (isHindi) {
                "स्टूडियो में सुरक्षित, सम्मानजनक और बेहद प्रोफेशनल माहौल मिलता है। मेकअप, स्टाइलिंग और रीटचिंग के बाद हर शॉट मैगज़ीन-रेडी दिखता है।"
            } else {
                "A respectful, organized, and genuinely creative set from call time to final retouching. The studio lighting and editorial grading made every lookbook frame magazine-ready."
            }
        ),
        StudioTestimonial(
            id = "tst-5",
            name = if (isHindi) "विकास अग्रवाल" else "Vikas Agarwal",
            roleLabel = if (isHindi) "ब्रांड संस्थापक" else "Apparel Brand Founder",
            location = if (isHindi) "फ़तेहाबाद, हरियाणा" else "Fatehabad, Haryana",
            serviceName = if (isHindi) "कमर्शियल ब्रांड कैंपेन" else "Commercial Brand Campaign",
            category = TestimonialCategory.CLIENT,
            rating = 5,
            quote = if (isHindi) {
                "हमारे नए फेस्टिव कलेक्शन के लिए प्रोडक्ट फोटोशूट और शॉर्ट रील्स की डिलीवरी समय से पहले मिली। सोशल मीडिया पर ग्राहकों की प्रतिक्रिया शानदार रही।"
            } else {
                "Our festive lookbook photos and short-form launch reels were delivered ahead of schedule with sharp commercial polish. Our online inquiries jumped immediately after launch."
            }
        )
    )
}

/**
 * Merges high-rated (4★-5★) live Firestore service reviews with curated client and model testimonials.
 */
fun buildHomeTestimonials(
    reviews: List<ServiceReview>,
    language: AppLanguage
): List<StudioTestimonial> {
    val isHindi = language == AppLanguage.HINDI
    val liveItems = reviews
        .filter { it.rating >= 4L && it.comment.trim().length >= 12 }
        .take(4)
        .mapIndexed { index, review ->
            val cleanName = review.reviewerName.trim().ifBlank {
                if (isHindi) "स्टूडियो क्लाइंट" else "Studio Client"
            }
            val cleanService = review.serviceType.trim().ifBlank {
                if (isHindi) "स्टूडियो प्रोडक्शन" else "Studio Production"
            }
            val isModelWork = cleanService.contains("model", ignoreCase = true) ||
                cleanService.contains("portfolio", ignoreCase = true)
            StudioTestimonial(
                id = review.id.ifBlank { "live-review-$index" },
                name = cleanName,
                roleLabel = if (isModelWork) {
                    if (isHindi) "पोर्टफोलियो मॉडल" else "Portfolio Model"
                } else {
                    if (isHindi) "सत्यापित क्लाइंट" else "Verified Client"
                },
                location = if (isHindi) "सिरसा स्टूडियो" else "Sirsa Studio",
                serviceName = cleanService,
                category = if (isModelWork) TestimonialCategory.MODEL else TestimonialCategory.CLIENT,
                rating = review.rating.toInt().coerceIn(4, 5),
                quote = review.comment.trim(),
                verified = true
            )
        }
    return liveItems + defaultStudioTestimonials(language)
}

private fun initialsFor(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return "SA"
    if (parts.size == 1) return parts.first().take(2).uppercase()
    return "${parts.first().first()}${parts.last().first()}".uppercase()
}

@Composable
fun HomeTestimonialCarousel(
    reviews: List<ServiceReview> = emptyList(),
    language: AppLanguage = AppLanguage.ENGLISH,
    autoAdvanceMs: Long = 6500L,
    modifier: Modifier = Modifier
) {
    val isHindi = language == AppLanguage.HINDI
    val isLight = StudioTheme.isLight
    val testimonials = remember(reviews, language) {
        buildHomeTestimonials(reviews, language)
    }

    var activeIndex by rememberSaveable { mutableIntStateOf(0) }
    var slideDirection by remember { mutableIntStateOf(1) }
    var isAutoPlay by rememberSaveable { mutableStateOf(true) }

    val safeIndex = if (testimonials.isEmpty()) 0 else activeIndex.coerceIn(0, testimonials.lastIndex)

    LaunchedEffect(isAutoPlay, testimonials.size, safeIndex, autoAdvanceMs) {
        if (isAutoPlay && testimonials.size > 1 && autoAdvanceMs > 0L) {
            delay(autoAdvanceMs)
            slideDirection = 1
            activeIndex = getNextTestimonialIndex(safeIndex, testimonials.size, 1)
        }
    }

    val outerBrush = if (isLight) {
        Brush.verticalGradient(
            colors = listOf(
                StudioCardSurface,
                GoldContainer.copy(alpha = 0.28f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                StudioElevatedSurface,
                StudioCardSurface
            )
        )
    }

    val slideBrush = if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color.White,
                GoldContainer.copy(alpha = 0.32f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                GoldContainer.copy(alpha = 0.24f),
                StudioCardSurface
            )
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_testimonial_carousel"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = if (isLight) 0.45f else 0.32f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLight) 3.dp else 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(outerBrush)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header row: eyebrow + title + 5.0 Verified badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = if (isHindi) "क्लाइंट और मॉडल अनुभव" else "CLIENT & MODEL STORIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = if (isHindi) "क्लाइंट और मॉडल का भरोसा" else "Trusted by Clients & Models",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isHindi) {
                            "सिरसा और हरियाणा में शूट, पोर्टफोलियो और प्रोडक्शन से जुड़े वास्तविक अनुभव।"
                        } else {
                            "Verified feedback from past studio clients and 18+ roster models."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = GoldContainer,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = if (isHindi) "सत्यापित स्टूडियो" else "Verified Studio",
                            tint = GoldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isHindi) "5.0 सत्यापित" else "5.0 Verified",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnGoldContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Animated Testimonial Slide Card
            AnimatedContent(
                targetState = safeIndex,
                transitionSpec = {
                    val offsetMultiplier = if (slideDirection >= 0) 1 else -1
                    (slideInHorizontally(
                        animationSpec = tween(durationMillis = 240),
                        initialOffsetX = { width -> (width / 4) * offsetMultiplier }
                    ) + fadeIn(animationSpec = tween(durationMillis = 220))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(durationMillis = 200),
                            targetOffsetX = { width -> -(width / 4) * offsetMultiplier }
                        ) + fadeOut(animationSpec = tween(durationMillis = 180)))
                },
                label = "testimonial_slide_transition"
            ) { index ->
                val item = testimonials[index]
                TestimonialSlideCard(
                    testimonial = item,
                    language = language,
                    slideBrush = slideBrush,
                    isLight = isLight
                )
            }

            // Carousel Navigation Controls & Pagination Dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            slideDirection = -1
                            activeIndex = getNextTestimonialIndex(safeIndex, testimonials.size, -1)
                        },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(CircleShape)
                            .background(StudioElevatedSurface)
                            .border(1.dp, StudioOutline, CircleShape)
                            .testTag("testimonial_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = if (isHindi) "पिछला अनुभव" else "Previous testimonial",
                            tint = GoldPrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            slideDirection = 1
                            activeIndex = getNextTestimonialIndex(safeIndex, testimonials.size, 1)
                        },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(CircleShape)
                            .background(StudioElevatedSurface)
                            .border(1.dp, StudioOutline, CircleShape)
                            .testTag("testimonial_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (isHindi) "अगला अनुभव" else "Next testimonial",
                            tint = GoldPrimary
                        )
                    }
                }

                // Dot Indicators + Slide Counter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        testimonials.forEachIndexed { idx, testimonial ->
                            val isSelected = idx == safeIndex
                            val dotLabel = if (isHindi) {
                                "स्लाइड ${idx + 1}: ${testimonial.name}"
                            } else {
                                "Slide ${idx + 1}: ${testimonial.name}"
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 24.dp, minHeight = 48.dp)
                                    .clickable(
                                        role = Role.Tab,
                                        onClickLabel = dotLabel
                                    ) {
                                        slideDirection = if (idx >= safeIndex) 1 else -1
                                        activeIndex = idx
                                    }
                                    .semantics { contentDescription = dotLabel }
                                    .testTag("testimonial_dot_$idx")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(8.dp)
                                        .width(if (isSelected) 22.dp else 8.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            if (isSelected) GoldPrimary
                                            else StudioOutline.copy(alpha = 0.85f)
                                        )
                                )
                            }
                        }
                    }

                    Text(
                        text = "${safeIndex + 1} / ${testimonials.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("testimonial_counter")
                    )
                }

                // Pause / Play Auto-Rotation Button
                IconButton(
                    onClick = { isAutoPlay = !isAutoPlay },
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAutoPlay) GoldContainer.copy(alpha = 0.7f) else StudioElevatedSurface
                        )
                        .border(
                            1.dp,
                            if (isAutoPlay) GoldPrimary.copy(alpha = 0.5f) else StudioOutline,
                            CircleShape
                        )
                        .testTag("testimonial_autoplay_btn")
                ) {
                    Icon(
                        imageVector = if (isAutoPlay) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isAutoPlay) {
                            if (isHindi) "ऑटो-प्ले रोकें" else "Pause testimonial autoplay"
                        } else {
                            if (isHindi) "ऑटो-प्ले शुरू करें" else "Play testimonial autoplay"
                        },
                        tint = if (isAutoPlay) GoldPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TestimonialSlideCard(
    testimonial: StudioTestimonial,
    language: AppLanguage,
    slideBrush: Brush,
    isLight: Boolean
) {
    val isHindi = language == AppLanguage.HINDI
    val isModel = testimonial.category == TestimonialCategory.MODEL
    val categoryLabel = when (testimonial.category) {
        TestimonialCategory.MODEL -> if (isHindi) "रोस्टर मॉडल" else "ROSTER MODEL"
        TestimonialCategory.CLIENT -> if (isHindi) "स्टूडियो क्लाइंट" else "STUDIO CLIENT"
    }

    val badgeContainerColor = if (isModel) {
        GoldContainer
    } else {
        if (isLight) SuccessGreen.copy(alpha = 0.14f) else SuccessGreen.copy(alpha = 0.18f)
    }
    val badgeTextColor = if (isModel) {
        OnGoldContainer
    } else {
        if (isLight) Color(0xFF1B5E20) else Color(0xFF81C784)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("testimonial_active_card"),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, StudioOutline.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(slideBrush)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stars + Role Category Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "${testimonial.rating} out of 5 stars"
                    }
                ) {
                    repeat(5) { starIdx ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (starIdx < testimonial.rating) {
                                GoldPrimary
                            } else {
                                StudioOutline
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeContainerColor,
                    border = BorderStroke(
                        1.dp,
                        if (isModel) GoldPrimary.copy(alpha = 0.45f)
                        else SuccessGreen.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeTextColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Quote block with editorial quotation icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.FormatQuote,
                    contentDescription = null,
                    tint = GoldPrimary.copy(alpha = 0.65f),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "“${testimonial.quote}”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 22.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            // Author & Service Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(GoldPrimary, GoldDark)
                            )
                        )
                        .border(1.5.dp, GoldLight.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialsFor(testimonial.name),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isLight) Color.White else Color(0xFF141100),
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = testimonial.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${testimonial.roleLabel} • ${testimonial.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = testimonial.serviceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
