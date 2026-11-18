package com.example.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppLanguage
import com.example.data.model.GalleryItem
import com.example.ui.components.FullScreenGalleryCarousel
import com.example.ui.components.GallerySkeletonGrid
import com.example.ui.components.ScrollToTopGridFab
import com.example.ui.components.resolvePortfolioMediaModel
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Curated high-resolution photography and video showcase samples for Star Artist Studio.
 * Uses bundled drawables (`drawable://...`) so sample photography renders instantaneously offline
 * and merges seamlessly with live Firestore `approvedGallery` works.
 */
private val DEFAULT_PORTFOLIO_WORKS = listOf(
    GalleryItem(
        id = "portfolio_fashion_editorial",
        userId = "studio_director",
        title = "Vogue Runway Couture Editorial",
        category = "Fashion",
        mediaType = "photo",
        mediaUrl = "drawable://fashion_editorial",
        thumbnailUrl = "drawable://fashion_editorial",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "portfolio_bridal_royal",
        userId = "studio_director",
        title = "Royal Heritage Bridal Stills",
        category = "Bridal",
        mediaType = "photo",
        mediaUrl = "drawable://bridal_heritage",
        thumbnailUrl = "drawable://bridal_heritage",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "portfolio_portrait_rimlight",
        userId = "studio_director",
        title = "Dramatic Rim-Light Character Portrait",
        category = "Portrait",
        mediaType = "photo",
        mediaUrl = "drawable://dramatic_portrait",
        thumbnailUrl = "drawable://dramatic_portrait",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "portfolio_commercial_perfume",
        userId = "studio_director",
        title = "Luxury Gold Commercial Ad Campaign",
        category = "Commercial",
        mediaType = "photo",
        mediaUrl = "drawable://commercial_luxury",
        thumbnailUrl = "drawable://commercial_luxury",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "portfolio_talent_compcard",
        userId = "studio_director",
        title = "18+ Model Casting Comp-Card",
        category = "Portfolio",
        mediaType = "photo",
        mediaUrl = "drawable://studio_model",
        thumbnailUrl = "drawable://studio_model",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "portfolio_studio_stage",
        userId = "studio_director",
        title = "Star Artist Lighting Stage Showcase",
        category = "Editorial",
        mediaType = "photo",
        mediaUrl = "drawable://studio_hero",
        thumbnailUrl = "drawable://studio_hero",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "portfolio_cinema_reel",
        userId = "studio_director",
        title = "4K Cinema Music Video Direction",
        category = "Video Reel",
        mediaType = "video",
        mediaUrl = "drawable://studio_hero",
        thumbnailUrl = "drawable://studio_hero",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "portfolio_fashion_reel_4k",
        userId = "studio_director",
        title = "Slow-Mo Gimbal Fashion Reel",
        category = "Video Reel",
        mediaType = "video",
        mediaUrl = "drawable://fashion_editorial",
        thumbnailUrl = "drawable://fashion_editorial",
        isDemo = true,
        status = "approved"
    )
)

/**
 * PortfolioScreen: Adaptive Material 3 gallery displaying professional studio photography samples
 * and Firestore works in a responsive grid. Tapping any card opens [FullScreenGalleryCarousel]
 * for pinch-zoom, double-tap inspection, swiping, sharing, and quick shoot booking.
 */
@Composable
fun PortfolioScreen(
    language: AppLanguage,
    portfolioItems: List<GalleryItem>,
    isSubmitting: Boolean = false,
    isLoading: Boolean = false,
    onUploadMedia: (title: String, category: String, mediaType: String, mediaUrl: String, isDemo: Boolean) -> Unit = { _, _, _, _, _ -> },
    onNavigateToBooking: () -> Unit = {},
    onOpenProjectArchive: (() -> Unit)? = null,
    onOpenPhotoPreview: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedMediaType by remember { mutableStateOf("all") } // "all", "photo", "video"
    var showUploadDialog by remember { mutableStateOf(false) }
    var startUploadWithCamera by remember { mutableStateOf(false) }
    var selectedCarouselIndex by remember { mutableStateOf<Int?>(null) }
    val gridState = rememberLazyGridState()
    val isLight = StudioTheme.isLight

    val categories = listOf(
        "All",
        "Fashion",
        "Portrait",
        "Bridal",
        "Commercial",
        "Editorial",
        "Portfolio",
        "Video Reel"
    )

    // Merge live Firestore items first, followed by curated studio photography samples
    val allDisplayItems = remember(portfolioItems) {
        val dbIds = portfolioItems.map { it.id }.toSet()
        val combined = portfolioItems.toMutableList()
        DEFAULT_PORTFOLIO_WORKS.forEach { sample ->
            if (sample.id !in dbIds) {
                combined.add(sample)
            }
        }
        combined
    }

    val filteredItems by remember(searchQuery, selectedCategory, selectedMediaType, allDisplayItems) {
        derivedStateOf {
            allDisplayItems.filter { item ->
                val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
                val matchesType = selectedMediaType == "all" || item.mediaType.equals(selectedMediaType, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesType && matchesSearch
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("portfolio_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // 1. Header with Title & Capture/Upload Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "स्टूडियो पोर्टफोलियो व गैलरी" else "Studio Portfolio Gallery",
                        style = MaterialTheme.typography.titleLarge,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "फुल-स्क्रीन में देखने के लिए किसी भी फोटोग्राफी सैंपल पर टैप करें"
                        else
                            "Tap any photography sample to inspect in full screen with pinch zoom",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            startUploadWithCamera = true
                            showUploadDialog = true
                        },
                        border = BorderStroke(1.dp, GoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("portfolio_camera_trigger_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = if (language == AppLanguage.HINDI) "कैमरा से पोर्टफोलियो फ़ोटो लें" else "Capture Portfolio Photo",
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = {
                            startUploadWithCamera = false
                            showUploadDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("portfolio_upload_trigger_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "अपलोड" else "Upload",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (language == AppLanguage.HINDI) "पोर्टफोलियो सैंपल खोजें..." else "Search photography samples...",
                        color = TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = if (language == AppLanguage.HINDI) "खोजें" else "Search",
                        tint = GoldPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (language == AppLanguage.HINDI) "खोज साफ़ करें" else "Clear Search",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portfolio_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = StudioOutline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = GoldPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Category Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("portfolio_category_row")
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = localizedPortfolioCategory(cat, language),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
                            .testTag("portfolio_chip_${cat.lowercase().replace(" ", "_")}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Media Type Filter & Sample Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PortfolioMediaTypeChip(
                        label = if (language == AppLanguage.HINDI) "सभी" else "All Media",
                        icon = Icons.Default.Collections,
                        isSelected = selectedMediaType == "all",
                        onClick = { selectedMediaType = "all" },
                        testTag = "portfolio_filter_all"
                    )
                    PortfolioMediaTypeChip(
                        label = if (language == AppLanguage.HINDI) "फोटोज़" else "Photos",
                        icon = Icons.Default.PhotoCamera,
                        isSelected = selectedMediaType == "photo",
                        onClick = { selectedMediaType = "photo" },
                        testTag = "portfolio_filter_photos"
                    )
                    PortfolioMediaTypeChip(
                        label = if (language == AppLanguage.HINDI) "4K रील्स" else "4K Videos",
                        icon = Icons.Default.Videocam,
                        isSelected = selectedMediaType == "video",
                        onClick = { selectedMediaType = "video" },
                        testTag = "portfolio_filter_videos"
                    )
                }

                Text(
                    text = "${filteredItems.size} " + if (language == AppLanguage.HINDI) "सैंपल" else "Samples",
                    color = if (isLight) GoldDark else GoldLight.copy(alpha = 0.85f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("portfolio_works_count")
                )
            }

            if (onOpenProjectArchive != null) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onOpenProjectArchive,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("portfolio_open_projects_archive_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "फोटोग्राफी व वीडियो प्रोजेक्ट आर्काइव खोलें (Room DB)"
                        else
                            "Open Photo & Video Project Archive (Room DB)",
                        color = if (isLight) GoldDark else GoldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (onOpenPhotoPreview != null) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onOpenPhotoPreview,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("portfolio_open_photo_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "लाइव कैमरा फोटो प्रीव्यू व संदर्भ शॉट"
                        else
                            "Open Live Camera Photo Preview Studio",
                        color = if (isLight) GoldDark else GoldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Adaptive Photography & Media Sample Grid
            if (isLoading) {
                GallerySkeletonGrid()
            } else if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("portfolio_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = GoldPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "कोई पोर्टफोलियो सैंपल नहीं मिला"
                            else
                                "No photography samples match your filters",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "सभी स्टूडियो सैंपल देखने के लिए फ़िल्टर रीसेट करें।"
                            else
                                "Reset category or search filters to explore all studio samples.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                selectedCategory = "All"
                                selectedMediaType = "all"
                            },
                            border = BorderStroke(1.dp, GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("portfolio_reset_filters_button")
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "फ़िल्टर रीसेट करें" else "Reset Filters",
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 165.dp),
                    state = gridState,
                    contentPadding = PaddingValues(bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("portfolio_grid")
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        PortfolioCard(
                            item = item,
                            language = language,
                            onClick = {
                                selectedCarouselIndex = filteredItems.indexOf(item).coerceAtLeast(0)
                            }
                        )
                    }
                }
            }
        }

        // Upload Media Dialog (CameraX + Photo Picker + URL)
        if (showUploadDialog) {
            UploadMediaDialog(
                language = language,
                isSubmitting = isSubmitting,
                initialStartCamera = startUploadWithCamera,
                onDismiss = {
                    showUploadDialog = false
                    startUploadWithCamera = false
                },
                onUpload = { title, cat, type, url, isDemo ->
                    onUploadMedia(title, cat, type, url, isDemo)
                    showUploadDialog = false
                    startUploadWithCamera = false
                }
            )
        }

        // Full-Screen Photography Viewer / Carousel
        if (selectedCarouselIndex != null && filteredItems.isNotEmpty()) {
            FullScreenGalleryCarousel(
                items = filteredItems,
                initialIndex = selectedCarouselIndex!!,
                language = language,
                onDismiss = { selectedCarouselIndex = null },
                onBookStyle = {
                    selectedCarouselIndex = null
                    onNavigateToBooking()
                }
            )
        }

        ScrollToTopGridFab(
            gridState = gridState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .testTag("portfolio_scroll_to_top_fab")
        )
    }
}

@Composable
fun PortfolioCard(
    item: GalleryItem,
    language: AppLanguage = AppLanguage.ENGLISH,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isLight = StudioTheme.isLight
    val mediaModel = remember(item.thumbnailUrl, item.mediaUrl) {
        resolvePortfolioMediaModel(item.thumbnailUrl.ifEmpty { item.mediaUrl })
    }
    val isVideo = item.mediaType.equals("video", ignoreCase = true)
    val demoBadge = if (language == AppLanguage.HINDI) "सैंपल" else "SAMPLE"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(GoldPrimary.copy(alpha = 0.45f), StudioOutline)
                    )
                ),
                RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .testTag("portfolio_card_${item.id}"),
        colors = CardDefaults.elevatedCardColors(containerColor = StudioCardSurface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f)
                    .background(StudioElevatedSurface)
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(mediaModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(StudioElevatedSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = null,
                                tint = GoldPrimary.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                )

                // Bottom contrast scrim so overlaid category badge stays legible over bright images
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                            )
                        )
                )

                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.72f))
                            .border(1.dp, GoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video Reel",
                            tint = GoldPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.82f))
                            .border(0.5.dp, GoldPrimary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "4K HDR",
                            color = Color(0xFFF6E79D),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    // Subtle full-screen inspect hint badge for photography samples
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(0.5.dp, Color(0xFFD4AF37).copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = if (language == AppLanguage.HINDI) "फुल स्क्रीन में देखें" else "View Full Screen",
                            tint = Color(0xFFF6E79D),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                if (item.isDemo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF8B5A00).copy(alpha = 0.9f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = demoBadge,
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else if (item.status.equals("pending", ignoreCase = true)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF9A6700).copy(alpha = 0.92f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("portfolio_pending_badge_${item.id}")
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "समीक्षाधीन" else "PENDING REVIEW",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp
                        )
                    }
                } else if (item.status.equals("approved", ignoreCase = true)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1B5E20).copy(alpha = 0.92f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("portfolio_saved_badge_${item.id}")
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "सहेजा गया" else "SAVED",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp
                        )
                    }
                }

                // Category pill tag
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(0.5.dp, Color(0xFFD4AF37).copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.category.uppercase(),
                        color = Color(0xFFF6E79D),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Title & Media Metadata
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = localizedPortfolioCategory(item.category, language),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLight) GoldDark else GoldLight.copy(alpha = 0.85f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isVideo) {
                            if (language == AppLanguage.HINDI) "वीडियो" else "Video"
                        } else {
                            if (language == AppLanguage.HINDI) "फोटो" else "Photo"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PortfolioMediaTypeChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) GoldPrimary.copy(alpha = 0.16f) else StudioElevatedSurface)
            .border(
                BorderStroke(1.dp, if (isSelected) GoldPrimary else StudioOutline),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) GoldPrimary else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = if (isSelected) GoldPrimary else TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private fun localizedPortfolioCategory(category: String, language: AppLanguage): String {
    if (language == AppLanguage.ENGLISH) return category
    return when (category.lowercase()) {
        "all" -> "सभी"
        "fashion" -> "फैशन"
        "portrait" -> "पोर्ट्रेट"
        "bridal" -> "ब्राइडल"
        "commercial" -> "कमर्शियल"
        "editorial" -> "एडिटोरियल"
        "portfolio" -> "पोर्टफोलियो"
        "video reel" -> "वीडियो रील"
        else -> category
    }
}
