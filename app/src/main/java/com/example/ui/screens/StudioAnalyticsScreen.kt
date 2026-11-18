package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.ShootBooking
import com.example.ui.components.StudioVisualAnalyticsDashboard
import com.example.ui.components.isPendingBookingStatus
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Dedicated screen visualizing studio booking trends and service popularity data
 * sourced directly from Cloud Firestore using Recharts-styled interactive Compose charts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioAnalyticsScreen(
    language: AppLanguage,
    bookings: List<ShootBooking>,
    isAdmin: Boolean,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onRefresh: () -> Unit = {},
    onBookShoot: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val isHindi = language == AppLanguage.HINDI
    var liveFirestoreOnly by remember { mutableStateOf(true) }

    val confirmedCount = remember(bookings) {
        bookings.count {
            val s = it.status.trim().lowercase()
            s == "confirmed" || s == "approved"
        }
    }
    val pendingCount = remember(bookings) {
        bookings.count { isPendingBookingStatus(it.status) }
    }
    val completedCount = remember(bookings) {
        bookings.count { it.status.trim().equals("completed", ignoreCase = true) }
    }
    val cancelledCount = remember(bookings) {
        bookings.count {
            val s = it.status.trim().lowercase()
            s == "cancelled" || s == "canceled" || s == "rejected"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("studio_analytics_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
                .testTag("studio_analytics_screen_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Navigation & Scope Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(CircleShape)
                                    .background(StudioElevatedSurface)
                                    .border(1.dp, StudioOutline, CircleShape)
                                    .testTag("analytics_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (isHindi) "वापस जाएं" else "Go back",
                                    tint = GoldPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (isHindi) "बुकिंग रुझान व लोकप्रियता" else "Booking Trends & Popularity",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isAdmin) {
                                            if (isHindi) "स्टूडियो Firestore • ${bookings.size} रिकॉर्ड"
                                            else "Studio-Wide Firestore • ${bookings.size} Records"
                                        } else {
                                            if (isHindi) "मेरा Firestore डेटा • ${bookings.size} रिकॉर्ड"
                                            else "My Firestore Bookings • ${bookings.size} Records"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        modifier = Modifier.testTag("analytics_scope_subtitle")
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(CircleShape)
                                .background(StudioElevatedSurface)
                                .border(1.dp, StudioOutline, CircleShape)
                                .testTag("analytics_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isHindi) "डेटा रीफ्रेश करें" else "Refresh Firestore analytics",
                                tint = GoldPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Data Mode Card: Live Firestore Only vs With Studio Benchmark
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(
                                    1.dp,
                                    Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.55f), StudioOutline))
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .testTag("analytics_data_mode_row"),
                        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = { liveFirestoreOnly = true },
                                    color = if (liveFirestoreOnly) GoldPrimary else StudioElevatedSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (liveFirestoreOnly) GoldLight else StudioOutline
                                    ),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("analytics_mode_live_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timeline,
                                            contentDescription = null,
                                            tint = if (liveFirestoreOnly) MaterialTheme.colorScheme.onPrimary else GoldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isHindi) "लाइव Firestore डेटा" else "Live Firestore",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (liveFirestoreOnly) MaterialTheme.colorScheme.onPrimary else TextPrimary
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { liveFirestoreOnly = false },
                                    color = if (!liveFirestoreOnly) GoldPrimary else StudioElevatedSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (!liveFirestoreOnly) GoldLight else StudioOutline
                                    ),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("analytics_mode_benchmark_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Assessment,
                                            contentDescription = null,
                                            tint = if (!liveFirestoreOnly) MaterialTheme.colorScheme.onPrimary else GoldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isHindi) "स्टूडियो बेंचमार्क सहित" else "With Studio Benchmark",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!liveFirestoreOnly) MaterialTheme.colorScheme.onPrimary else TextPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (liveFirestoreOnly) {
                                    if (isHindi) {
                                        "Cloud Firestore के /bookings संग्रह से वास्तविक समय के रुझान और सेवा मांग दिखा रहा है।"
                                    } else {
                                        "Visualizing live booking documents directly from Cloud Firestore (/bookings)."
                                    }
                                } else {
                                    if (isHindi) {
                                        "लाइव Firestore बुकिंग के साथ स्टूडियो के ऐतिहासिक मौसमी बेंचमार्क जोड़कर दिखा रहा है।"
                                    } else {
                                        "Overlaying live Firestore bookings with Star Artist Studio seasonal baselines."
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

            // Loading state
            if (isLoading && bookings.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .border(1.dp, StudioOutline, RoundedCornerShape(14.dp))
                        .testTag("analytics_loading_indicator"),
                    colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = GoldPrimary,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isHindi) "Firestore से एनालिटिक्स लोड हो रहा है..." else "Syncing live Firestore analytics...",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Empty Firestore Callout in Pure Live Mode
            if (liveFirestoreOnly && bookings.isEmpty() && !isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .testTag("analytics_firestore_empty_card"),
                    colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (isHindi) "अभी तक कोई लाइव बुकिंग दर्ज नहीं है" else "No Live Firestore Bookings Yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isHindi) {
                                "नया शूट शेड्यूल करें ताकि Recharts विज़ुअलाइज़ेशन में आपके वास्तविक ट्रेंड्स दिखें, या स्टूडियो बेंचमार्क प्रीव्यू देखें।"
                            } else {
                                "Submit a shoot booking to see live trend curves populate automatically, or preview historical studio benchmarks."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onBookShoot("Fashion Portfolio Photography") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("analytics_empty_book_cta")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "नया शूट बुक करें" else "Book a Shoot",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { liveFirestoreOnly = false },
                                border = BorderStroke(1.dp, GoldPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("analytics_switch_benchmark_cta")
                            ) {
                                Text(
                                    text = if (isHindi) "बेंचमार्क देखें" else "Preview Benchmark",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Booking Status Breakdown Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
                    .testTag("analytics_status_breakdown_card"),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (isHindi) "Firestore बुकिंग स्थिति सारांश" else "Firestore Booking Status Breakdown",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusCountPill(
                            label = if (isHindi) "स्वीकृत" else "Confirmed",
                            count = confirmedCount,
                            accent = SuccessGreen,
                            modifier = Modifier.weight(1f),
                            testTag = "analytics_status_confirmed"
                        )
                        StatusCountPill(
                            label = if (isHindi) "लंबित" else "Pending",
                            count = pendingCount,
                            accent = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f),
                            testTag = "analytics_status_pending"
                        )
                        StatusCountPill(
                            label = if (isHindi) "पूर्ण" else "Completed",
                            count = completedCount,
                            accent = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f),
                            testTag = "analytics_status_completed"
                        )
                        StatusCountPill(
                            label = if (isHindi) "रद्द" else "Cancelled",
                            count = cancelledCount,
                            accent = ErrorRed,
                            modifier = Modifier.weight(1f),
                            testTag = "analytics_status_cancelled"
                        )
                    }
                }
            }

            // Recharts Interactive Analytics Dashboard (Trends Area Chart, Popular Services Bar, 30D Service Bar, Donut Mix)
            StudioVisualAnalyticsDashboard(
                bookings = bookings,
                language = language,
                includeStudioBaseline = !liveFirestoreOnly,
                onBookService = onBookShoot
            )
        }
    }
}

@Composable
private fun StatusCountPill(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(StudioElevatedSurface)
            .border(0.8.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            color = accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 9.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
