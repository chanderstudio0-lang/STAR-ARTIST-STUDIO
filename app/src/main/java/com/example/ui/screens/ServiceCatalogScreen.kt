package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.ServiceCatalogReferenceEntity
import com.example.data.model.AppLanguage
import com.example.data.model.CatalogBudgetFilter
import com.example.data.model.CatalogSortOrder
import com.example.data.model.ServicePackage
import com.example.data.model.ServicePackageCategory
import com.example.data.model.ServicePricingTier
import com.example.data.model.ServiceReview
import com.example.data.model.StudioServicesData
import com.example.ui.components.ScrollToTopFab
import com.example.ui.components.ServiceCatalogFeedbackSection
import com.example.ui.components.catalogReviewBookingId
import com.example.ui.components.filterReviewsForServicePackage
import com.example.ui.components.openWhatsApp
import com.example.ui.components.resolvePortfolioMediaModel
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

@Composable
fun ServiceCatalogScreen(
    language: AppLanguage,
    favoritePackageIds: Set<String> = emptySet(),
    catalogReferenceShots: Map<String, List<ServiceCatalogReferenceEntity>> = emptyMap(),
    studioReviews: List<ServiceReview> = emptyList(),
    myReviews: List<ServiceReview> = emptyList(),
    isSubmittingReview: Boolean = false,
    onToggleFavoritePackage: (ServicePackage) -> Unit = {},
    onOpenReferenceCamera: (packageId: String?) -> Unit = {},
    onDeleteReferenceShot: (ServiceCatalogReferenceEntity) -> Unit = {},
    onSubmitServiceReview: (
        packageId: String,
        serviceType: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit
    ) -> Unit = { _, _, _, _, _ -> },
    onBack: () -> Unit,
    onBookService: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val isHindi = language == AppLanguage.HINDI

    BackHandler(onBack = onBack)

    var selectedCategory by remember { mutableStateOf(ServicePackageCategory.ALL) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var selectedBudget by remember { mutableStateOf(CatalogBudgetFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(CatalogSortOrder.FEATURED) }
    var expandedStudioHighlightsId by remember { mutableStateOf<String?>(null) }
    var expandedFeedbackPackageId by remember { mutableStateOf<String?>(null) }

    // Remember selected pricing tier ID per offering card (defaults to recommended/first tier)
    val selectedTierIds = remember {
        mutableStateMapOf<String, String>().apply {
            StudioServicesData.packages.forEach { pkg ->
                pkg.defaultPricingTier()?.let { put(pkg.id, it.id) }
            }
        }
    }

    val filteredPackages = remember(selectedCategory, favoritesOnly, favoritePackageIds, searchQuery, sortOrder, selectedBudget) {
        StudioServicesData.filterAndSortPackages(
            category = selectedCategory,
            query = searchQuery,
            sortOrder = sortOrder,
            budgetFilter = selectedBudget,
            favoritesOnly = favoritesOnly,
            favoriteIds = favoritePackageIds
        )
    }

    val hasActiveFilters = selectedCategory != ServicePackageCategory.ALL ||
        favoritesOnly ||
        selectedBudget != CatalogBudgetFilter.ALL ||
        searchQuery.isNotBlank() ||
        sortOrder != CatalogSortOrder.FEATURED

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("service_catalog_screen")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("service_catalog_list"),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =================================================================
            // 1. HEADER BANNER CARD WITH BACK ACTION & TRANSPARENCY METRICS
            // =================================================================
            item(key = "catalog_header") {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(listOf(GoldPrimary, GoldDark))
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .testTag("service_catalog_header"),
                    colors = CardDefaults.elevatedCardColors(containerColor = StudioCardSurface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
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
                                        .testTag("service_catalog_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = if (isHindi) "होम पर वापस जाएं" else "Back to Home",
                                        tint = GoldPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isHindi) "स्टूडियो सेवा कैटलॉग" else "Service Catalog & Pricing",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldPrimary
                                    )
                                    Text(
                                        text = if (isHindi)
                                            "पारदर्शी पैकेज टियर • फोटोग्राफी, मॉडलिंग व 4K सिनेमा"
                                        else
                                            "Transparent tiered rates • Photography, Modeling & 4K Video",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { onOpenReferenceCamera(null) },
                                border = BorderStroke(1.dp, GoldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("catalog_open_photo_preview_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHindi) "रेफ कैम" else "Ref Cam",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Summary Metrics Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CatalogStatPill(
                                value = "${StudioServicesData.packages.size}",
                                label = if (isHindi) "स्टूडियो सेवाएं" else "Offerings",
                                modifier = Modifier.weight(1f)
                            )
                            CatalogStatPill(
                                value = "₹7,999+",
                                label = if (isHindi) "शुरुआती मूल्य" else "Starting Rate",
                                modifier = Modifier.weight(1f)
                            )
                            CatalogStatPill(
                                value = if (isHindi) "3 टियर" else "3 Tiers",
                                label = if (isHindi) "प्रति सेवा" else "Per Service",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 2. SEARCH, CATEGORY FILTER CHIPS & SORT BAR
            // =================================================================
            item(key = "catalog_filters") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_catalog_search_input"),
                        placeholder = {
                            Text(
                                text = if (isHindi)
                                    "फोटोग्राफी, 4K वीडियो, ब्राइडल, रील्स या ड्रोन खोजें..."
                                else
                                    "Search photography, 4K video, bridal, reels, or drone...",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = if (isHindi) "खोजें" else "Search",
                                tint = GoldPrimary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("service_catalog_search_clear")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isHindi) "खोज साफ करें" else "Clear search",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = StudioOutline,
                            focusedContainerColor = StudioCardSurface,
                            unfocusedContainerColor = StudioCardSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Quick-Search Suggestion Pills (One-tap Photography & Video Filters)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("catalog_quick_search_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "त्वरित:" else "QUICK:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldLight,
                                letterSpacing = 0.6.sp
                            )
                        }

                        StudioServicesData.quickSearchFilters.forEach { quick ->
                            val isActive = searchQuery.trim().equals(quick.queryToken, ignoreCase = true)
                            Surface(
                                onClick = {
                                    searchQuery = if (isActive) "" else quick.queryToken
                                    focusManager.clearFocus()
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isActive) GoldContainer else StudioElevatedSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isActive) GoldPrimary else StudioOutline
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("catalog_quick_filter_${quick.id}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) quick.labelHi else quick.labelEn,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isActive) OnGoldContainer else TextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Category Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ServicePackageCategory.values().forEach { cat ->
                            val isSelected = selectedCategory == cat
                            val label = if (isHindi) cat.titleHi else cat.titleEn
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = StudioCardSurface,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) GoldPrimary else StudioOutline
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("catalog_category_chip_${cat.id}")
                            )
                        }

                        FilterChip(
                            selected = favoritesOnly,
                            onClick = { favoritesOnly = !favoritesOnly },
                            label = {
                                Text(
                                    text = if (isHindi)
                                        "पसंदीदा (${favoritePackageIds.size})"
                                    else
                                        "Favorites (${favoritePackageIds.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (favoritesOnly) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (favoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = StudioCardSurface,
                                labelColor = GoldLight
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = favoritesOnly,
                                borderColor = if (favoritesOnly) GoldPrimary else StudioOutline
                            ),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("catalog_favorites_filter_chip")
                        )
                    }

                    // Budget Bracket Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("catalog_budget_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CatalogBudgetFilter.values().forEach { budget ->
                            val isSelected = selectedBudget == budget
                            Surface(
                                onClick = { selectedBudget = budget },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) GoldContainer else StudioCardSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) GoldPrimary else StudioOutline
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("catalog_budget_chip_${budget.id}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) budget.labelHi else budget.labelEn,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) OnGoldContainer else TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Sort Row, Active Clear Action & Results Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isHindi)
                                    "${filteredPackages.size} सेवाएं उपलब्ध"
                                else
                                    "${filteredPackages.size} Offerings",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight,
                                modifier = Modifier.testTag("catalog_results_count")
                            )

                            if (hasActiveFilters) {
                                Surface(
                                    onClick = {
                                        selectedCategory = ServicePackageCategory.ALL
                                        favoritesOnly = false
                                        selectedBudget = CatalogBudgetFilter.ALL
                                        searchQuery = ""
                                        sortOrder = CatalogSortOrder.FEATURED
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = StudioElevatedSurface,
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("catalog_clear_all_filters_chip")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isHindi) "रीसेट" else "Clear",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CatalogSortOrder.values().forEach { order ->
                                val isSelected = sortOrder == order
                                Surface(
                                    onClick = { sortOrder = order },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) GoldContainer else StudioCardSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) GoldPrimary else StudioOutline
                                    ),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("catalog_sort_chip_${order.id}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = if (isHindi) order.labelHi else order.labelEn,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) OnGoldContainer else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 3. EMPTY SEARCH / FILTER STATE
            // =================================================================
            if (filteredPackages.isEmpty()) {
                item(key = "catalog_empty_state") {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp)
                            .border(1.dp, StudioOutline, RoundedCornerShape(20.dp))
                            .testTag("service_catalog_empty_state"),
                        colors = CardDefaults.elevatedCardColors(containerColor = StudioCardSurface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (isHindi)
                                    "कोई मेल खाने वाली स्टूडियो सेवा नहीं मिली"
                                else
                                    "No Matching Studio Offerings Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isHindi)
                                    "कृपया खोज शब्द बदलें या सभी श्रेणियों को देखने के लिए फ़िल्टर रीसेट करें।"
                                else
                                    "Try another keyword or reset filters to view all studio tiers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    selectedCategory = ServicePackageCategory.ALL
                                    favoritesOnly = false
                                    selectedBudget = CatalogBudgetFilter.ALL
                                    searchQuery = ""
                                    sortOrder = CatalogSortOrder.FEATURED
                                },
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag("catalog_reset_filters_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "फ़िल्टर रीसेट करें" else "Reset Filters",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // =================================================================
                // 4. MATERIAL 3 SERVICE OFFERING CARDS WITH PRICING TIERS
                // =================================================================
                items(filteredPackages, key = { it.id }) { pkg ->
                    val activeTierId = selectedTierIds[pkg.id]
                    val activeTier = pkg.pricingTiers.firstOrNull { it.id == activeTierId }
                        ?: pkg.defaultPricingTier()
                    val isStudioHighlightsExpanded = expandedStudioHighlightsId == pkg.id
                    val matchingSnippet = remember(pkg.id, searchQuery, isHindi) {
                        StudioServicesData.findMatchingSnippet(pkg, searchQuery, isHindi)
                    }

                    val packageRefs = catalogReferenceShots[pkg.id].orEmpty()
                    val packageReviews = remember(studioReviews, pkg.id) {
                        filterReviewsForServicePackage(
                            reviews = studioReviews,
                            packageId = pkg.id,
                            recommendedServiceType = pkg.recommendedServiceType,
                            titleEn = pkg.titleEn
                        )
                    }
                    val existingPackageReview = remember(myReviews, pkg.id) {
                        val targetBookingId = catalogReviewBookingId(pkg.id)
                        myReviews.firstOrNull { it.bookingId.equals(targetBookingId, ignoreCase = true) }
                    }
                    val isFeedbackExpanded = expandedFeedbackPackageId == pkg.id

                    ServiceCatalogCard(
                        pkg = pkg,
                        selectedTier = activeTier,
                        language = language,
                        isStudioHighlightsExpanded = isStudioHighlightsExpanded,
                        isFavorite = favoritePackageIds.contains(pkg.id),
                        matchingSnippet = matchingSnippet,
                        referenceShots = packageRefs,
                        packageReviews = packageReviews,
                        existingUserReview = existingPackageReview,
                        isFeedbackExpanded = isFeedbackExpanded,
                        isSubmittingReview = isSubmittingReview,
                        onToggleFeedback = {
                            expandedFeedbackPackageId = if (isFeedbackExpanded) null else pkg.id
                        },
                        onSubmitFeedback = { rating, comment, onSuccess ->
                            onSubmitServiceReview(
                                pkg.id,
                                pkg.recommendedServiceType,
                                rating,
                                comment,
                                onSuccess
                            )
                        },
                        onSelectTier = { tier -> selectedTierIds[pkg.id] = tier.id },
                        onToggleStudioHighlights = {
                            expandedStudioHighlightsId =
                                if (isStudioHighlightsExpanded) null else pkg.id
                        },
                        onToggleFavorite = { onToggleFavoritePackage(pkg) },
                        onCaptureReferenceShot = { onOpenReferenceCamera(pkg.id) },
                        onDeleteReferenceShot = onDeleteReferenceShot,
                        onBook = {
                            onBookService(pkg.bookingLabelForTier(activeTier))
                        },
                        onWhatsAppQuote = {
                            val tierName = activeTier?.let {
                                if (isHindi) it.tierNameHi else it.tierNameEn
                            }.orEmpty()
                            val tierPrice = activeTier?.let {
                                if (isHindi) it.priceFormattedHi else it.priceFormattedEn
                            } ?: if (isHindi) pkg.priceFormattedHi else pkg.priceFormattedEn
                            val title = if (isHindi) pkg.titleHi else pkg.titleEn
                            val msg = if (isHindi) {
                                "नमस्ते स्टार आर्टिस्ट स्टूडियो! मुझे $title ($tierName - $tierPrice) पैकेज की बुकिंग के बारे में बात करनी है।"
                            } else {
                                "Hello Star Artist Studio! I would like to book $title ($tierName tier - $tierPrice)."
                            }
                            openWhatsApp(context, StudioContactDetails.buildWhatsAppUrl(msg))
                        },
                        onShare = {
                            val title = if (isHindi) pkg.titleHi else pkg.titleEn
                            val badge = if (isHindi) pkg.tagHi else pkg.tagEn
                            val summary = if (isHindi) pkg.summaryHi else pkg.summaryEn
                            val tierInfo = activeTier?.let {
                                if (isHindi) "${it.tierNameHi}: ${it.priceFormattedHi}" else "${it.tierNameEn}: ${it.priceFormattedEn}"
                            } ?: if (isHindi) pkg.priceFormattedHi else pkg.priceFormattedEn
                            ShareHelper.shareService(
                                context = context,
                                title = title,
                                description = "$summary\n$tierInfo",
                                badge = badge
                            )
                        }
                    )
                }
            }

            // =================================================================
            // 5. CUSTOM PRODUCTION QUOTE FOOTER CARD
            // =================================================================
            item(key = "catalog_footer_cta") {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .border(1.dp, StudioOutline, RoundedCornerShape(20.dp))
                        .testTag("catalog_custom_quote_card"),
                    colors = CardDefaults.elevatedCardColors(containerColor = StudioElevatedSurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi)
                                    "कस्टम प्रोडक्शन कोटेशन चाहिए?"
                                else
                                    "Need a Custom Production Bundle?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }
                        Text(
                            text = if (isHindi)
                                "मल्टी-डे म्यूज़िक वीडियो, पूरे ब्रांड कैटलॉग या आउटडोर शूट के लिए हमारे स्टूडियो डायरेक्टर से कस्टम पैकेज बनवाएं।"
                            else
                                "Combine photography, verified 18+ talent casting, and 4K cinema direction for multi-day campaigns at Talwara Khurd.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                        Button(
                            onClick = { onBookService("Photography (High-Fashion Editorial)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("catalog_custom_quote_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "कस्टम शूट कंसल्टेशन बुक करें" else "Request Custom Shoot Schedule",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item(key = "catalog_bottom_spacer") {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        ScrollToTopFab(
            listState = listState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .testTag("service_catalog_scroll_top_fab")
        )
    }
}

@Composable
private fun CatalogStatPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = StudioElevatedSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioOutline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = GoldPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ServiceCatalogCard(
    pkg: ServicePackage,
    selectedTier: ServicePricingTier?,
    language: AppLanguage,
    isStudioHighlightsExpanded: Boolean,
    isFavorite: Boolean = false,
    matchingSnippet: String? = null,
    referenceShots: List<ServiceCatalogReferenceEntity> = emptyList(),
    packageReviews: List<ServiceReview> = emptyList(),
    existingUserReview: ServiceReview? = null,
    isFeedbackExpanded: Boolean = false,
    isSubmittingReview: Boolean = false,
    onToggleFeedback: () -> Unit = {},
    onSubmitFeedback: (rating: Int, comment: String, onSuccess: () -> Unit) -> Unit = { _, _, _ -> },
    onSelectTier: (ServicePricingTier) -> Unit,
    onToggleStudioHighlights: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onCaptureReferenceShot: () -> Unit = {},
    onDeleteReferenceShot: (ServiceCatalogReferenceEntity) -> Unit = {},
    onBook: () -> Unit,
    onWhatsAppQuote: () -> Unit,
    onShare: () -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    val title = if (isHindi) pkg.titleHi else pkg.titleEn
    val categoryLabel = if (isHindi) pkg.category.titleHi else pkg.category.titleEn
    val tag = if (isHindi) pkg.tagHi else pkg.tagEn
    val summary = if (isHindi) pkg.summaryHi else pkg.summaryEn
    val highlights = if (isHindi) pkg.highlightsHi else pkg.highlightsEn

    val activePrice = selectedTier?.let {
        if (isHindi) it.priceFormattedHi else it.priceFormattedEn
    } ?: if (isHindi) pkg.priceFormattedHi else pkg.priceFormattedEn

    val activeDuration = selectedTier?.let {
        if (isHindi) it.durationHi else it.durationEn
    } ?: if (isHindi) pkg.durationHi else pkg.durationEn

    val activeTurnaround = selectedTier?.let {
        if (isHindi) it.turnaroundHi else it.turnaroundEn
    } ?: if (isHindi) "48-72 घंटे" else "48-72 Hours"

    val activeDeliverables = selectedTier?.let {
        if (isHindi) it.deliverablesHi else it.deliverablesEn
    } ?: highlights

    var isTierGuideExpanded by remember(pkg.id) { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .border(
                BorderStroke(
                    width = if (pkg.isPopular) 1.5.dp else 1.dp,
                    brush = if (pkg.isPopular) {
                        Brush.verticalGradient(listOf(GoldPrimary, GoldDark))
                    } else {
                        Brush.verticalGradient(listOf(StudioOutline, StudioOutline.copy(alpha = 0.6f)))
                    }
                ),
                RoundedCornerShape(20.dp)
            )
            .animateContentSize()
            .testTag("catalog_card_${pkg.id}"),
        colors = CardDefaults.elevatedCardColors(containerColor = StudioCardSurface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (pkg.isPopular) 6.dp else 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Header: Icon, Category & Tag Pills, Share Button
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GoldContainer)
                            .border(1.dp, GoldPrimary.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = pkg.icon,
                            contentDescription = title,
                            tint = GoldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = StudioElevatedSurface,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, StudioOutline)
                            ) {
                                Text(
                                    text = categoryLabel.uppercase(),
                                    color = TextSecondary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }

                            if (pkg.isPopular) {
                                Surface(
                                    color = GoldContainer,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, GoldPrimary)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isHindi) "लोकप्रिय" else "POPULAR",
                                            color = OnGoldContainer,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = tag,
                            color = GoldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCaptureReferenceShot,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("catalog_card_ref_cam_${pkg.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = if (isHindi) "$title संदर्भ फोटो लें" else "Capture reference photo for $title",
                            tint = GoldPrimary
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("catalog_favorite_btn_${pkg.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) {
                                if (isHindi) "$title पसंदीदा से हटाएं" else "Remove $title from favorites"
                            } else {
                                if (isHindi) "$title पसंदीदा में सहेजें" else "Save $title to favorites"
                            },
                            tint = if (isFavorite) GoldPrimary else TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("catalog_share_btn_${pkg.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = if (isHindi) "$title साझा करें" else "Share $title",
                            tint = GoldLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Offering Title, Description & Search Match Highlight
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = GoldLight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary.copy(alpha = 0.9f),
                lineHeight = 20.sp
            )

            if (!matchingSnippet.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = GoldContainer.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, GoldPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_match_hint_${pkg.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "मेल: $matchingSnippet" else "Match: $matchingSnippet",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnGoldContainer,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. Pricing Tiers Selector
            if (pkg.pricingTiers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "मूल्य टियर चुनें" else "PRICING TIERS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPrimary,
                            letterSpacing = 0.6.sp
                        )
                    }

                    Surface(
                        color = if (isTierGuideExpanded) GoldPrimary else GoldContainer,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { isTierGuideExpanded = !isTierGuideExpanded }
                            .testTag("catalog_tier_guide_toggle_${pkg.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = if (isHindi) "टियर विवरण" else "Tier breakdown info",
                                tint = if (isTierGuideExpanded) Color(0xFF141000) else GoldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isTierGuideExpanded) {
                                    if (isHindi) "जानकारी छिपाएं" else "Hide Info"
                                } else {
                                    if (isHindi) "टियर जानकारी" else "Tier Info"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isTierGuideExpanded) Color(0xFF141000) else GoldLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pkg.pricingTiers.forEach { tier ->
                        val isTierSelected = selectedTier?.id == tier.id
                        val tierTitle = if (isHindi) tier.tierNameHi else tier.tierNameEn
                        val tierPrice = if (isHindi) tier.priceFormattedHi else tier.priceFormattedEn
                        val tierBadge = if (isHindi) tier.badgeHi else tier.badgeEn

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 68.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.RadioButton) { onSelectTier(tier) }
                                .testTag("catalog_tier_${pkg.id}_${tier.id}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isTierSelected) GoldContainer else StudioElevatedSurface,
                            border = BorderStroke(
                                width = if (isTierSelected) 1.5.dp else 1.dp,
                                color = if (isTierSelected) GoldPrimary else StudioOutline
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = tierBadge,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isTierSelected) GoldPrimary else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tierTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTierSelected) OnGoldContainer else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tierPrice,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isTierSelected) GoldPrimary else GoldLight,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = isTierGuideExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioElevatedSurface)
                            .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .testTag("catalog_tier_guide_panel_${pkg.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "Essential • Pro • Flagship तुलना" else "What Each Tier Includes",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldLight
                            )
                        }
                        pkg.pricingTiers.forEach { tier ->
                            val tName = if (isHindi) tier.tierNameHi else tier.tierNameEn
                            val tPrice = if (isHindi) tier.priceFormattedHi else tier.priceFormattedEn
                            val tDuration = if (isHindi) tier.durationHi else tier.durationEn
                            val tTurnaround = if (isHindi) tier.turnaroundHi else tier.turnaroundEn
                            val tRoleBadge = catalogTierRoleBadge(tier.id, isHindi)
                            val tOverview = catalogTierOverview(tier.id, isHindi)
                            val firstDeliverable = (if (isHindi) tier.deliverablesHi else tier.deliverablesEn).firstOrNull().orEmpty()

                            Surface(
                                color = StudioCardSurface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(0.5.dp, StudioOutline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) {}
                                    .testTag("catalog_tier_guide_item_${pkg.id}_${tier.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$tName • $tPrice",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = GoldPrimary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(GoldContainer)
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = tRoleBadge,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = OnGoldContainer
                                            )
                                        }
                                    }
                                    Text(
                                        text = tOverview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp
                                    )
                                    Text(
                                        text = "$tDuration • $tTurnaround${if (firstDeliverable.isNotBlank()) " • $firstDeliverable" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Selected Tier Breakdown Surface
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = StudioElevatedSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (StudioTheme.isLight) StudioOutline else GoldPrimary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedTier?.let {
                                    val tName = if (isHindi) it.tierNameHi else it.tierNameEn
                                    if (isHindi) "$tName टियर मूल्य" else "$tName Tier Rate"
                                } ?: if (isHindi) "पैकेज मूल्य" else "Package Rate",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = activePrice,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldPrimary,
                                modifier = Modifier.testTag("catalog_selected_price_${pkg.id}")
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = GoldLight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeDuration,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = GoldLight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHindi) "डिलीवरी: $activeTurnaround" else "Delivery: $activeTurnaround",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = StudioOutline.copy(alpha = 0.6f))

                    if (selectedTier != null) {
                        val activeTierName = if (isHindi) selectedTier.tierNameHi else selectedTier.tierNameEn
                        val activeRoleBadge = catalogTierRoleBadge(selectedTier.id, isHindi)
                        val activeOverview = catalogTierOverview(selectedTier.id, isHindi)
                        Surface(
                            color = GoldContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, GoldPrimary.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {}
                                .testTag("catalog_active_tier_badge_${pkg.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(15.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "$activeTierName — $activeRoleBadge",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldPrimary
                                    )
                                    Text(
                                        text = activeOverview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isHindi) "इस टियर में शामिल:" else "Included in this tier:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        activeDeliverables.forEach { deliverable ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = deliverable,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // Expandable studio-wide inclusions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleStudioHighlights() }
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .testTag("catalog_highlights_toggle_${pkg.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isHindi)
                                (if (isStudioHighlightsExpanded) "स्टूडियो सुविधाएं छुपाएं" else "स्टूडियो-स्तरीय सुविधाएं देखें (${highlights.size})")
                            else
                                (if (isStudioHighlightsExpanded) "Hide Studio Inclusions" else "View Base Studio Inclusions (${highlights.size})"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Icon(
                            imageVector = if (isStudioHighlightsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isStudioHighlightsExpanded) "Collapse" else "Expand",
                            tint = GoldPrimary
                        )
                    }

                    AnimatedVisibility(visible = isStudioHighlightsExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StudioCardSurface, RoundedCornerShape(10.dp))
                                .border(0.5.dp, StudioOutline, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            highlights.forEach { item ->
                                Text(
                                    text = "• $item",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }

            if (referenceShots.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_card_ref_strip_${pkg.id}"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isHindi)
                            "संलग्न कलाकार संदर्भ शॉट्स (${referenceShots.size})"
                        else
                            "Attached Artist Reference Shots (${referenceShots.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(referenceShots, key = { it.id }) { ref ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StudioElevatedSurface,
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
                                modifier = Modifier.width(156.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(86.dp)
                                    ) {
                                        SubcomposeAsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(resolvePortfolioMediaModel(ref.imageDataUrl))
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = ref.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = { onDeleteReferenceShot(ref) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                .testTag("catalog_delete_ref_${ref.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = if (isHindi) "संदर्भ फोटो हटाएं" else "Remove reference shot",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                    .padding(4.dp)
                                                    .size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = ref.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ServiceCatalogFeedbackSection(
                packageId = pkg.id,
                serviceTitle = title,
                reviews = packageReviews,
                existingUserReview = existingUserReview,
                isExpanded = isFeedbackExpanded,
                isSubmitting = isSubmittingReview,
                language = language,
                onToggleExpanded = onToggleFeedback,
                onSubmitFeedback = onSubmitFeedback
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Primary Booking & WhatsApp Quote Action Buttons (>= 48.dp height)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onBook,
                    modifier = Modifier
                        .weight(1.2f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("catalog_book_btn_${pkg.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) {
                            selectedTier?.let { "${it.tierNameHi} बुक करें" } ?: "शूट बुक करें"
                        } else {
                            selectedTier?.let { "Book ${it.tierNameEn}" } ?: "Book Shoot"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onWhatsAppQuote,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("catalog_whatsapp_btn_${pkg.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WhatsAppGreen
                    ),
                    border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isHindi) "व्हाट्सऐप कोट" else "WhatsApp Quote",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun catalogTierRoleBadge(tierId: String, isHindi: Boolean): String {
    return when (tierId.trim().lowercase()) {
        "essential" -> if (isHindi) "कोर सत्र" else "CORE SESSION"
        "pro" -> if (isHindi) "सबसे लोकप्रिय" else "MOST POPULAR"
        "flagship" -> if (isHindi) "फुल VIP प्रोडक्शन" else "FULL VIP PRODUCTION"
        else -> if (isHindi) "स्टूडियो टियर" else "STUDIO TIER"
    }
}

private fun catalogTierOverview(tierId: String, isHindi: Boolean): String {
    return when (tierId.trim().lowercase()) {
        "essential" -> if (isHindi) {
            "कोर स्टूडियो लाइटिंग सेटअप, आवश्यक रिटच डिलीवरी और स्टैंडर्ड टर्नअराउंड।"
        } else {
            "Entry studio session with core lighting setups, key retouched deliverables & standard delivery."
        }
        "pro" -> if (isHindi) {
            "अतिरिक्त आउटफिट लुक्स, सिग्नेचर लाइटिंग, रील्स और तेज़ प्राथमिकता डिलीवरी।"
        } else {
            "Extended multi-look production with signature strobe lighting, reels & faster priority turnaround."
        }
        "flagship" -> if (isHindi) {
            "क्रिएटिव डायरेक्शन, VIP स्टाइलिंग, RAW/4K मास्टर और एक्सप्रेस डिलीवरी सहित संपूर्ण पैकेज।"
        } else {
            "All-inclusive campaign production with creative direction, VIP styling, RAW/4K masters & express delivery."
        }
        else -> if (isHindi) {
            "इस पैकेज टियर के अनुसार संपूर्ण स्टूडियो प्रोडक्शन।"
        } else {
            "Complete studio deliverables calibrated to the selected package tier."
        }
    }
}
