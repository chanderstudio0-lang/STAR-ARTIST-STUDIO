package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.CatalogSortOrder
import com.example.data.model.ServicePackage
import com.example.data.model.ServicePackageCategory
import com.example.data.model.StudioServicesData
import com.example.ui.theme.DarkStudioBlack
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.util.ShareHelper
import com.example.util.StudioContactDetails

/**
 * Interactive 'Services Offered' Section using a LazyVerticalGrid.
 * Details Photography, Modeling, and Video Production packages with category filters,
 * price sorting, favorites saving, pricing tiers, inclusions, duration, booking and WhatsApp actions.
 */
@Composable
fun ServicesOfferedGridSection(
    modifier: Modifier = Modifier,
    language: AppLanguage,
    favoritePackageIds: Set<String> = emptySet(),
    onBookPackage: (ServicePackage) -> Unit,
    onToggleFavorite: (ServicePackage) -> Unit = {},
    onInquireWhatsApp: (ServicePackage) -> Unit = {},
    onOpenFullCatalog: (() -> Unit)? = null
) {
    var selectedCategory by remember { mutableStateOf(ServicePackageCategory.ALL) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(CatalogSortOrder.FEATURED) }
    var expandedPackageId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val displayedPackages = remember(selectedCategory, favoritesOnly, favoritePackageIds, sortOrder) {
        StudioServicesData.filterAndSortPackages(
            category = selectedCategory,
            sortOrder = sortOrder,
            favoritesOnly = favoritesOnly,
            favoriteIds = favoritePackageIds
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("services_offered_section")
    ) {
        // Section Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GoldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "स्टूडियो सेवाएं एवं पैकेज" else "Services & Production Packages",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF282010))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "पारदर्शी मूल्य" else "TRANSPARENT PRICING",
                        color = GoldLight,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (language == AppLanguage.HINDI)
                    "फोटोग्राफी, 18+ मॉडलिंग पोर्टफोलियो और 4K सिनेमा वीडियो प्रोडक्शन के लिए प्रमाणित इन-हाउस पैकेज।"
                else
                    "Curated packages for fashion photography, modeling auditions, and 4K cinema video direction at Talwara Khurd.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFAAAAAA),
                lineHeight = 18.sp
            )

            if (onOpenFullCatalog != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenFullCatalog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("open_full_service_catalog_btn"),
                    border = BorderStroke(1.dp, GoldPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GoldLight
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "पूर्ण सेवा कैटलॉग और मूल्य टियर देखें"
                        else
                            "Explore Full Service Catalog & Pricing Tiers",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips (Scrollable: All, Photography, Modeling, Video Production + Favorites)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ServicePackageCategory.values().forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val label = if (language == AppLanguage.HINDI) cat.titleHi else cat.titleEn
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = StudioElevatedSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) GoldPrimary else StudioOutline
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("service_filter_${cat.id}")
                    )
                }

                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = {
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "पसंदीदा (${favoritePackageIds.size})"
                            else
                                "Favorites (${favoritePackageIds.size})",
                            fontSize = 11.5.sp,
                            fontWeight = if (favoritesOnly) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (favoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = StudioElevatedSurface,
                        labelColor = GoldLight
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = favoritesOnly,
                        borderColor = if (favoritesOnly) GoldPrimary else StudioOutline
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("service_filter_favorites")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sort Order & Results Count Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (language == AppLanguage.HINDI)
                        "${displayedPackages.size} पैकेज"
                    else
                        "${displayedPackages.size} Packages",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight,
                    modifier = Modifier.testTag("services_grid_results_count")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CatalogSortOrder.values().forEach { order ->
                        val isOrderSelected = sortOrder == order
                        Surface(
                            onClick = { sortOrder = order },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isOrderSelected) GoldContainer else StudioCardSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isOrderSelected) GoldPrimary else StudioOutline
                            ),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("services_grid_sort_${order.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) order.labelHi else order.labelEn,
                                    fontSize = 11.sp,
                                    fontWeight = if (isOrderSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isOrderSelected) OnGoldContainer else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (displayedPackages.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
                    .testTag("services_grid_empty_state"),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "इस श्रेणी में कोई पसंदीदा पैकेज सहेजा नहीं गया है"
                        else
                            "No favorite packages saved in this category yet",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    OutlinedButton(
                        onClick = {
                            selectedCategory = ServicePackageCategory.ALL
                            favoritesOnly = false
                            sortOrder = CatalogSortOrder.FEATURED
                        },
                        border = BorderStroke(1.dp, GoldPrimary),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("services_grid_reset_filter_btn")
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "सभी पैकेज दिखाएं" else "Show All Packages",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // LazyVerticalGrid rendering the service packages
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (displayedPackages.size > 2) 760.dp else 420.dp)
                    .padding(horizontal = 16.dp)
                    .testTag("services_lazy_vertical_grid"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(displayedPackages, key = { it.id }) { pkg ->
                    val isExpanded = expandedPackageId == pkg.id
                    val isFavorite = favoritePackageIds.contains(pkg.id)
                    ServicePackageGridCard(
                        pkg = pkg,
                        language = language,
                        isExpanded = isExpanded,
                        isFavorite = isFavorite,
                        onToggleExpand = {
                            expandedPackageId = if (isExpanded) null else pkg.id
                        },
                        onToggleFavorite = { onToggleFavorite(pkg) },
                        onBook = { onBookPackage(pkg) },
                        onWhatsApp = {
                            val msg = if (language == AppLanguage.HINDI)
                                "नमस्ते स्टार आर्टिस्ट स्टूडियो! मुझे आपके ${pkg.titleHi} (${pkg.priceFormattedHi}) पैकेज के बारे में जानकारी चाहिए।"
                            else
                                "Hello Star Artist Studio! I'm interested in booking the ${pkg.titleEn} package (${pkg.priceFormattedEn})."
                            val url = StudioContactDetails.buildWhatsAppUrl(msg)
                            openWhatsApp(context, url)
                            onInquireWhatsApp(pkg)
                        },
                        onShare = {
                            ShareHelper.shareService(
                                context = context,
                                title = if (language == AppLanguage.HINDI) pkg.titleHi else pkg.titleEn,
                                description = if (language == AppLanguage.HINDI) "${pkg.summaryHi}\nमूल्य: ${pkg.priceFormattedHi}" else "${pkg.summaryEn}\nStarting at: ${pkg.priceFormattedEn}",
                                badge = if (language == AppLanguage.HINDI) pkg.tagHi else pkg.tagEn
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Individual Service Package Card within the LazyVerticalGrid
 */
@Composable
fun ServicePackageGridCard(
    pkg: ServicePackage,
    language: AppLanguage,
    isExpanded: Boolean,
    isFavorite: Boolean = false,
    onToggleExpand: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onBook: () -> Unit,
    onWhatsApp: () -> Unit,
    onShare: () -> Unit
) {
    val title = if (language == AppLanguage.HINDI) pkg.titleHi else pkg.titleEn
    val tag = if (language == AppLanguage.HINDI) pkg.tagHi else pkg.tagEn
    val price = if (language == AppLanguage.HINDI) pkg.priceFormattedHi else pkg.priceFormattedEn
    val duration = if (language == AppLanguage.HINDI) pkg.durationHi else pkg.durationEn
    val summary = if (language == AppLanguage.HINDI) pkg.summaryHi else pkg.summaryEn
    val highlights = if (language == AppLanguage.HINDI) pkg.highlightsHi else pkg.highlightsEn

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    width = if (pkg.isPopular) 1.5.dp else 1.dp,
                    brush = if (pkg.isPopular) {
                        Brush.verticalGradient(listOf(GoldPrimary, GoldDark))
                    } else {
                        Brush.verticalGradient(listOf(StudioOutline, StudioOutline.copy(alpha = 0.5f)))
                    }
                ),
                RoundedCornerShape(16.dp)
            )
            .animateContentSize()
            .testTag("package_card_${pkg.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Category Badge & Popular Pill & Favorite + Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF262010))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = pkg.icon,
                            contentDescription = title,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (pkg.isPopular) GoldContainer else Color(0xFF2E2718))
                            .border(
                                0.5.dp,
                                if (pkg.isPopular) GoldPrimary else StudioOutline,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tag,
                            color = if (pkg.isPopular) GoldLight else Color(0xFFC0C0C0),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("service_favorite_btn_${pkg.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) {
                                if (language == AppLanguage.HINDI) "$title पसंदीदा से हटाएं" else "Remove $title from favorites"
                            } else {
                                if (language == AppLanguage.HINDI) "$title पसंदीदा में सहेजें" else "Save $title to favorites"
                            },
                            tint = if (isFavorite) GoldPrimary else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("share_package_${pkg.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share $title",
                            tint = GoldLight.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = duration,
                            color = Color(0xFFA5A5A5),
                            fontSize = 11.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = price,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldPrimary,
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (language == AppLanguage.HINDI) "ऑल-इन्क्लूसिव" else "All-inclusive",
                        color = Color.Gray,
                        fontSize = 9.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD4D4D4),
                lineHeight = 17.sp
            )

            // Deliverables & Inclusions Toggle
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleExpand() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (language == AppLanguage.HINDI)
                        (if (isExpanded) "विवरण छुपाएं" else "शामिल सुविधाएं देखें (${highlights.size})")
                    else
                        (if (isExpanded) "Hide Deliverables" else "View Deliverables (${highlights.size})"),
                    color = GoldLight,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = GoldLight,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(StudioElevatedSurface, RoundedCornerShape(10.dp))
                        .border(0.5.dp, StudioOutline, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    highlights.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                color = Color(0xFFE0E0E0),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Book Now (primary) + WhatsApp Inquiry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBook,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("book_package_btn_${pkg.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color(0xFF141000)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "बुक करें" else "Book Shoot",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onWhatsApp,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("whatsapp_package_btn_${pkg.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WhatsAppGreen
                    ),
                    border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "व्हाट्सऐप" else "WhatsApp",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
