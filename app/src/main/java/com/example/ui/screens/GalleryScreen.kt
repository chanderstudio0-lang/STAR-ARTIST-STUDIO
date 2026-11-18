package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.GalleryItem
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.components.EncodedPortfolioPhoto
import com.example.ui.components.FullScreenGalleryCarousel
import com.example.ui.components.GallerySkeletonGrid
import com.example.ui.components.GoldLoadingSpinner
import com.example.ui.components.ScrollToTopGridFab
import com.example.ui.components.StudioCameraCaptureDialog
import com.example.ui.components.encodePortfolioPhotoUriToDataUrl
import com.example.ui.components.isValidPortfolioMediaUrl
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.util.ShareHelper
import com.example.util.StudioStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Curated default studio project samples (strictly marked DEMO)
private val DEFAULT_STUDIO_PROJECT_SAMPLES = listOf(
    GalleryItem(
        id = "sample_bridal_royal",
        userId = "studio_director",
        title = "Royal Heritage Bridal Stills",
        category = "Bridal",
        mediaType = "photo",
        mediaUrl = "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=800&q=80",
        thumbnailUrl = "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=400&q=80",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "sample_runway_vogue",
        userId = "studio_director",
        title = "Vogue Runway High-Fashion Look",
        category = "Fashion",
        mediaType = "photo",
        mediaUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=800&q=80",
        thumbnailUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=400&q=80",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "sample_cinema_music_video",
        userId = "studio_director",
        title = "4K Cinema Music Video Production",
        category = "Video Reel",
        mediaType = "video",
        mediaUrl = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=800&q=80",
        thumbnailUrl = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=400&q=80",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "sample_commercial_perfume",
        userId = "studio_director",
        title = "Luxury Gold Commercial Ad Campaign",
        category = "Commercial",
        mediaType = "photo",
        mediaUrl = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=800&q=80",
        thumbnailUrl = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=400&q=80",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "sample_model_editorial",
        userId = "studio_director",
        title = "Studio Rim Light Editorial Portrait",
        category = "Editorial",
        mediaType = "photo",
        mediaUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&q=80",
        thumbnailUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "sample_fashion_reel_4k",
        userId = "studio_director",
        title = "Slow-Mo Gimbal Fashion Reel",
        category = "Video Reel",
        mediaType = "video",
        mediaUrl = "https://images.unsplash.com/photo-1509631179647-0177331693ae?w=800&q=80",
        thumbnailUrl = "https://images.unsplash.com/photo-1509631179647-0177331693ae?w=400&q=80",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "sample_talent_portfolio",
        userId = "studio_director",
        title = "18+ Model Casting Comp-Card",
        category = "Portfolio",
        mediaType = "photo",
        mediaUrl = "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=800&q=80",
        thumbnailUrl = "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=400&q=80",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "sample_prewedding_sunset",
        userId = "studio_director",
        title = "Sirsa Golden Hour Pre-Wedding",
        category = "Bridal",
        mediaType = "photo",
        mediaUrl = "https://images.unsplash.com/photo-1519741497674-611481863552?w=800&q=80",
        thumbnailUrl = "https://images.unsplash.com/photo-1519741497674-611481863552?w=400&q=80",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "sample_portrait_classic",
        userId = "studio_director",
        title = "Classic Studio Monochromatic Portrait",
        category = "Portrait",
        mediaType = "photo",
        mediaUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&q=80",
        thumbnailUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&q=80",
        isDemo = true,
        status = "approved"
    ),
    GalleryItem(
        id = "sample_portrait_rimlight",
        userId = "studio_director",
        title = "Dramatic Rim-Light Character Portrait",
        category = "Portrait",
        mediaType = "photo",
        mediaUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=800&q=80",
        thumbnailUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&q=80",
        isDemo = true,
        status = "approved"
    )
)

@Composable
fun GalleryScreen(
    language: AppLanguage,
    galleryItems: List<GalleryItem>,
    isSubmitting: Boolean,
    isLoading: Boolean = false,
    onUploadMedia: (title: String, category: String, mediaType: String, mediaUrl: String, isDemo: Boolean) -> Unit,
    onNavigateToBooking: () -> Unit = {}
) {
    val s = StudioStrings(language)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedMediaType by remember { mutableStateOf("all") } // "all", "photo", "video"
    var showUploadDialog by remember { mutableStateOf(false) }
    var startUploadWithCamera by remember { mutableStateOf(false) }
    var selectedCarouselIndex by remember { mutableStateOf<Int?>(null) }
    val gridState = rememberLazyGridState()

    val categories = listOf("All", "Fashion", "Portrait", "Commercial", "Bridal", "Editorial", "Video Reel", "Portfolio")

    // Merge database gallery with curated studio project samples, deduplicating by ID
    val allDisplayItems = remember(galleryItems) {
        val dbIds = galleryItems.map { it.id }.toSet()
        val combined = galleryItems.toMutableList()
        DEFAULT_STUDIO_PROJECT_SAMPLES.forEach { demo ->
            if (demo.id !in dbIds) {
                combined.add(demo)
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
            .testTag("gallery_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // 1. Header & Upload Submission Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.galleryTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = s.gallerySub,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA),
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
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
                            .testTag("gallery_camera_trigger_button")
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
                            contentColor = Color(0xFF141000)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("gallery_upload_trigger_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Submit Project",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "अपलोड" else "Upload",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(s.searchPlaceholder) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GoldPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_search_input"),
                colors = studioTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Category Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("gallery_category_row")
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = StudioBlack,
                            selectedLeadingIconColor = StudioBlack,
                            containerColor = StudioElevatedSurface,
                            labelColor = Color(0xFFC0C0C0)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) GoldPrimary else StudioOutline
                        ),
                        modifier = Modifier.testTag("gallery_chip_${cat.lowercase().replace(" ", "_")}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Media Type Filter (All Media, Photography, Video Reels)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MediaTypeFilterChip(
                        label = if (language == AppLanguage.HINDI) "सभी मीडिया" else "All Media",
                        icon = Icons.Default.Collections,
                        isSelected = selectedMediaType == "all",
                        onClick = { selectedMediaType = "all" },
                        testTag = "filter_media_all"
                    )
                    MediaTypeFilterChip(
                        label = if (language == AppLanguage.HINDI) "फोटोज़" else "Photos",
                        icon = Icons.Default.CameraAlt,
                        isSelected = selectedMediaType == "photo",
                        onClick = { selectedMediaType = "photo" },
                        testTag = "filter_media_photos"
                    )
                    MediaTypeFilterChip(
                        label = if (language == AppLanguage.HINDI) "4K रील्स" else "4K Videos",
                        icon = Icons.Default.Videocam,
                        isSelected = selectedMediaType == "video",
                        onClick = { selectedMediaType = "video" },
                        testTag = "filter_media_videos"
                    )
                }

                Text(
                    text = "${filteredItems.size} " + (if (language == AppLanguage.HINDI) "प्रोजेक्ट्स" else "Items"),
                    color = GoldLight.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Grid of Professional Samples or Gold Skeleton Screen
            if (isLoading) {
                GallerySkeletonGrid()
            } else if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "कोई प्रोजेक्ट नहीं मिला। अन्य श्रेणी या खोज शब्द चुनें।"
                            else
                                "No projects found matching current filter or search.",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("gallery_items_grid")
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        GalleryCard(
                            item = item,
                            demoBadge = s.demoBadge,
                            onClick = {
                                selectedCarouselIndex = filteredItems.indexOf(item).coerceAtLeast(0)
                            }
                        )
                    }
                }
            }
        }

        // Upload Media Dialog
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

        // Full-Screen High-Resolution Image & Video Carousel
        if (selectedCarouselIndex != null) {
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

        // Floating Action Button scrolling back to top on long scroll
        ScrollToTopGridFab(
            gridState = gridState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .testTag("gallery_scroll_to_top_fab")
        )
    }
}

@Composable
fun MediaTypeFilterChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF332912) else StudioElevatedSurface)
            .border(
                1.dp,
                if (isSelected) GoldPrimary else StudioOutline,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag(testTag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) GoldPrimary else Color.Gray,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isSelected) GoldLight else Color(0xFFB0B0B0),
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun GalleryCard(item: GalleryItem, demoBadge: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, Brush.verticalGradient(listOf(GoldPrimary.copy(alpha = 0.4f), StudioOutline))),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag("gallery_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFF18181A))
            ) {
                // Media Thumbnail via Coil
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.thumbnailUrl.ifEmpty { item.mediaUrl })
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Video play overlay if media is 4K video reel
                if (item.mediaType.equals("video", ignoreCase = true)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(StudioBlack.copy(alpha = 0.75f))
                            .border(1.dp, GoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video Reel",
                            tint = GoldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 4K Cinema Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(StudioBlack.copy(alpha = 0.85f))
                            .border(0.5.dp, GoldPrimary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "4K HDR",
                            color = GoldLight,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Strictly Required DEMO Badge
                if (item.isDemo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFBF360C))
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
                }

                // Category pill tag
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(StudioBlack.copy(alpha = 0.85f))
                        .border(0.5.dp, GoldDark.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.category.uppercase(),
                        color = GoldLight,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    val cardContext = LocalContext.current
                    IconButton(
                        onClick = {
                            ShareHelper.shareGalleryItem(
                                context = cardContext,
                                title = item.title,
                                category = item.category,
                                mediaType = item.mediaType,
                                mediaUrl = item.mediaUrl
                            )
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("share_gallery_card_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share ${item.title}",
                            tint = GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item.mediaType.equals("video", ignoreCase = true)) "4K Cinema Video" else "Editorial Stills",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA0A0A0),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Star Studio",
                        color = GoldPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun UploadMediaDialog(
    language: AppLanguage,
    isSubmitting: Boolean,
    initialStartCamera: Boolean = false,
    onDismiss: () -> Unit,
    onUpload: (title: String, category: String, mediaType: String, mediaUrl: String, isDemo: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isHindi = language == AppLanguage.HINDI

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Fashion") }
    var mediaType by remember { mutableStateOf("photo") }
    var mediaUrl by remember { mutableStateOf("") }
    var capturedPhoto by remember { mutableStateOf<EncodedPortfolioPhoto?>(null) }
    var showCameraCapture by remember { mutableStateOf(initialStartCamera) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    val categories = listOf("Fashion", "Portrait", "Commercial", "Bridal", "Editorial", "Video Reel", "Portfolio")

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingImage = true
            uploadError = null
            coroutineScope.launch {
                val encoded = withContext(Dispatchers.IO) {
                    encodePortfolioPhotoUriToDataUrl(context, uri)
                }
                isProcessingImage = false
                if (encoded != null) {
                    capturedPhoto = encoded
                    mediaUrl = encoded.dataUrl
                    mediaType = "photo"
                    if (title.isBlank()) {
                        title = if (isHindi) "स्टूडियो $category पोर्टफोलियो" else "Studio $category Photo"
                    }
                } else {
                    uploadError = if (isHindi) {
                        "चयनित फ़ोटो प्रोसेस नहीं हो सकी। कृपया दूसरी उच्च-गुणवत्ता वाली छवि चुनें।"
                    } else {
                        "Unable to optimize selected photo within 850 KB upload limit."
                    }
                }
            }
        }
    }

    if (showCameraCapture) {
        StudioCameraCaptureDialog(
            isHindi = isHindi,
            onDismiss = { showCameraCapture = false },
            onPhotoCaptured = { encoded ->
                capturedPhoto = encoded
                mediaUrl = encoded.dataUrl
                mediaType = "photo"
                if (title.isBlank()) {
                    title = if (isHindi) "स्टूडियो $category कैप्चर" else "Studio $category Capture"
                }
                uploadError = null
                showCameraCapture = false
            }
        )
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .border(1.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isHindi) "पोर्टफोलियो फ़ोटो / मीडिया अपलोड" else "Upload Portfolio Photo & Media",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldContainer)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (isHindi)
                                "नोट: आपकी ली गई फ़ोटो तुरंत आपके पोर्टफोलियो में दिखेंगी और स्टूडियो एडमिन स्वीकृति के बाद सार्वजनिक गैलरी में प्रकाशित होंगी।"
                            else
                                "Your captured photos appear immediately in your portfolio and go public once approved by studio admins.",
                            fontSize = 11.sp,
                            color = OnGoldContainer
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (isHindi) "प्रोजेक्ट शीर्षक" else "Project Title") },
                        placeholder = { Text(if (isHindi) "उदा. रॉयल गोल्ड ब्राइडल पोर्ट्रेट" else "e.g. Royal Gold Bridal Portrait") },
                        colors = studioTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_title_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = mediaType == "photo",
                            onClick = { mediaType = "photo" },
                            label = { Text(if (isHindi) "फ़ोटो स्टिल्स" else "Photo Stills") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = StudioElevatedSurface,
                                labelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("upload_type_photo_chip")
                        )
                        FilterChip(
                            selected = mediaType == "video",
                            onClick = { mediaType = "video" },
                            label = { Text(if (isHindi) "वीडियो रील" else "Video Reel") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = StudioElevatedSurface,
                                labelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("upload_type_video_chip")
                        )
                    }

                    if (mediaType == "photo") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showCameraCapture = true },
                                enabled = !isSubmitting && !isProcessingImage,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("upload_camera_capture_btn")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (capturedPhoto != null) {
                                        if (isHindi) "पुनः लें" else "Retake Photo"
                                    } else {
                                        if (isHindi) "कैमरा फोटो" else "Take Photo"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    galleryPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isSubmitting && !isProcessingImage,
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.65f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("upload_pick_photo_btn")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "गैलरी चुनें" else "Choose Photo",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isProcessingImage) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi) "उच्च गुणवत्ता वाली फ़ोटो तैयार हो रही है..." else "Optimizing HD portfolio photo...",
                                    color = GoldLight,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        capturedPhoto?.let { photo ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                    .testTag("upload_photo_preview_card"),
                                colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(StudioBlack)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(photo.dataUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = if (isHindi) "कैप्चर किया गया पोर्टफोलियो फ़ोटो पूर्वावलोकन" else "Captured portfolio photo preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = {
                                                capturedPhoto = null
                                                mediaUrl = ""
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                .testTag("upload_clear_captured_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = if (isHindi) "हटाएं" else "Remove Captured Photo",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.testTag("upload_photo_quality_badge")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HighQuality,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "HD ${photo.width}×${photo.height} • ${(photo.byteLength / 1024).coerceAtLeast(1)} KB",
                                            color = SuccessGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (capturedPhoto == null) {
                        OutlinedTextField(
                            value = mediaUrl,
                            onValueChange = {
                                mediaUrl = it
                                uploadError = null
                            },
                            label = { Text(if (isHindi) "फ़ोटो या 4K वीडियो URL" else "Photo or 4K Video URL") },
                            placeholder = { Text("https://images.unsplash.com/...") },
                            colors = studioTextFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_url_input")
                        )
                    }

                    if (uploadError != null) {
                        Text(
                            text = uploadError.orEmpty(),
                            color = ErrorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(if (isHindi) "श्रेणी:" else "Category:", fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.SemiBold)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = StudioElevatedSurface,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(if (isHindi) "रद्द करें" else "Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (!isValidPortfolioMediaUrl(mediaUrl)) {
                                    uploadError = if (isHindi) {
                                        "कृपया कैमरा से फ़ोटो लें, गैलरी से चुनें, या मान्य HTTPS मीडिया URL दर्ज करें।"
                                    } else {
                                        "Capture a photo, choose from gallery, or enter a valid HTTPS media URL."
                                    }
                                } else {
                                    onUpload(title, category, mediaType, mediaUrl, false)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            enabled = !isSubmitting && !isProcessingImage && title.isNotBlank() && mediaUrl.isNotBlank(),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("upload_submit_btn")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (isHindi) "पोर्टफोलियो में सहेजें" else "Save to Portfolio",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewItemDialog(item: GalleryItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    val previewContext = LocalContext.current
                    IconButton(onClick = {
                        ShareHelper.shareGalleryItem(
                            context = previewContext,
                            title = item.title,
                            category = item.category,
                            mediaType = item.mediaType,
                            mediaUrl = item.mediaUrl
                        )
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = GoldPrimary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
            }
        },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141416))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.mediaUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (item.mediaType.equals("video", ignoreCase = true)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(StudioBlack.copy(alpha = 0.8f))
                                .border(1.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = GoldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Category: ${item.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldLight,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Format: ${item.mediaType.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Studio Set: Talwara Khurd, Sirsa (4K Lighting & Cinema Bay)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA)
                )

                if (item.isDemo) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF261912))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DEMO: Star Artist Studio Official Sample Showcase",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFB74D),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = StudioBlack)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            val previewContext = LocalContext.current
            Button(
                onClick = {
                    ShareHelper.shareGalleryItem(
                        context = previewContext,
                        title = item.title,
                        category = item.category,
                        mediaType = item.mediaType,
                        mediaUrl = item.mediaUrl
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262010), contentColor = GoldPrimary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = StudioCardSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
