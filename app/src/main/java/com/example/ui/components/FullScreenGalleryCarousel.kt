package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.GalleryItem
import com.example.ui.theme.DarkGoldContainer
import com.example.ui.theme.DarkGoldDark
import com.example.ui.theme.DarkGoldLight
import com.example.ui.theme.DarkGoldPrimary
import com.example.ui.theme.DarkStudioElevatedSurface
import com.example.ui.theme.DarkStudioOutline
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.util.ShareHelper
import com.example.util.StudioStrings
import kotlinx.coroutines.launch

/**
 * Resolves bundled `drawable://` keys into local Android drawable resource IDs so curated
 * photography samples load deterministically offline and in tests while preserving remote URLs,
 * `data:image/...` strings, and `content://` URIs.
 */
fun resolvePortfolioMediaModel(mediaUrl: String): Any {
    return when (mediaUrl.trim()) {
        "drawable://fashion_editorial" -> R.drawable.img_sample_fashion_editorial_1794620320125
        "drawable://bridal_heritage" -> R.drawable.img_sample_bridal_heritage_1794620318592
        "drawable://commercial_luxury" -> R.drawable.img_sample_commercial_luxury_1794620321554
        "drawable://dramatic_portrait" -> R.drawable.img_sample_dramatic_portrait_1794620320124
        "drawable://studio_hero" -> R.drawable.studio_hero_banner_1793955301083
        "drawable://studio_model" -> R.drawable.studio_model_portrait_1793955328333
        else -> mediaUrl
    }
}

/**
 * Full-screen high-resolution image viewer & carousel dialog for portfolio gallery items.
 * Supports horizontal swiping, pinch/double-tap zoom, lateral navigation arrows,
 * thumbnail preview strip, share sheet, and direct shoot booking.
 */
@Composable
fun FullScreenGalleryCarousel(
    items: List<GalleryItem>,
    initialIndex: Int,
    language: AppLanguage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onBookStyle: (GalleryItem) -> Unit = {}
) {
    if (items.isEmpty()) {
        onDismiss()
        return
    }

    val s = StudioStrings(language)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val safeInitialIndex = initialIndex.coerceIn(0, items.size - 1)
    val pagerState = rememberPagerState(initialPage = safeInitialIndex) { items.size }
    val thumbnailListState = rememberLazyListState()
    var currentZoomScale by remember { mutableFloatStateOf(1f) }
    var zoomResetSignal by remember { mutableStateOf(0) }

    LaunchedEffect(pagerState.currentPage) {
        currentZoomScale = 1f
        thumbnailListState.animateScrollToItem(
            (pagerState.currentPage - 2).coerceAtLeast(0)
        )
    }

    val currentItem = items[pagerState.currentPage]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        Surface(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF080808))
                .testTag("gallery_fullscreen_carousel"),
            color = Color(0xFF080808)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0E0D09),
                                Color(0xFF050505),
                                Color(0xFF120F08)
                            )
                        )
                    )
            ) {
                // =============================================================
                // 1. HORIZONTAL PAGER: FULL-SCREEN ZOOMABLE VIEWER
                // =============================================================
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = currentZoomScale <= 1.05f,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("carousel_pager"),
                    key = { index -> items[index].id.ifBlank { "item_$index" } }
                ) { page ->
                    val pageItem = items[page]
                    CarouselPageItem(
                        item = pageItem,
                        isActive = page == pagerState.currentPage,
                        externalResetSignal = zoomResetSignal,
                        onScaleChanged = { newScale ->
                            if (page == pagerState.currentPage) {
                                currentZoomScale = newScale
                            }
                        }
                    )
                }

                // =============================================================
                // 2. TOP FLOATING CONTROL BAR
                // =============================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Current Page Indicator Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.78f))
                            .border(1.dp, DarkGoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                            .semantics(mergeDescendants = true) {}
                            .testTag("carousel_counter")
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${items.size}",
                            color = DarkGoldLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Action Icons: Zoom Toggle, Share & Close (48.dp touch targets)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { zoomResetSignal += 1 },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.78f))
                                .border(1.dp, DarkStudioOutline, CircleShape)
                                .testTag("carousel_zoom_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (currentZoomScale > 1.05f) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                                contentDescription = if (currentZoomScale > 1.05f) "Reset Zoom" else "Zoom In",
                                tint = DarkGoldLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                ShareHelper.shareGalleryItem(
                                    context = context,
                                    title = currentItem.title,
                                    category = currentItem.category,
                                    mediaType = currentItem.mediaType,
                                    mediaUrl = currentItem.mediaUrl
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.78f))
                                .border(1.dp, DarkStudioOutline, CircleShape)
                                .testTag("carousel_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = if (language == AppLanguage.HINDI) "शेयर करें" else "Share Piece",
                                tint = DarkGoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.84f))
                                .border(1.dp, DarkGoldPrimary.copy(alpha = 0.7f), CircleShape)
                                .testTag("carousel_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (language == AppLanguage.HINDI) "बंद करें" else "Close Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // =============================================================
                // 3. LATERAL NAVIGATION CHEVRONS (48.dp TOUCH TARGETS)
                // =============================================================
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f))
                            .border(1.dp, DarkGoldPrimary.copy(alpha = 0.55f), CircleShape)
                            .testTag("carousel_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (language == AppLanguage.HINDI) "पिछली तस्वीर" else "Previous Piece",
                            tint = DarkGoldLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (pagerState.currentPage < items.size - 1) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f))
                            .border(1.dp, DarkGoldPrimary.copy(alpha = 0.55f), CircleShape)
                            .testTag("carousel_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = if (language == AppLanguage.HINDI) "अगली तस्वीर" else "Next Piece",
                            tint = DarkGoldLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // =============================================================
                // 4. BOTTOM OVERLAY: DETAILS, THUMBNAIL STRIP & BOOKING ACTIONS
                // =============================================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.88f),
                                    Color.Black
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkGoldContainer.copy(alpha = 0.9f))
                                .border(1.dp, DarkGoldDark.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentItem.category.uppercase(),
                                color = DarkGoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (currentItem.isDemo) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF282010))
                                    .border(0.5.dp, DarkGoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = s.demoBadge,
                                    color = DarkGoldPrimary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentItem.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkGoldLight,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("carousel_item_title")
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    val zoomHint = if (currentZoomScale > 1.05f) {
                        String.format("%.1fx • ", currentZoomScale) +
                            if (language == AppLanguage.HINDI) "रीसेट करने के लिए डबल-टैप करें" else "Double-tap or press zoom icon to reset"
                    } else {
                        if (language == AppLanguage.HINDI)
                            "STAR ARTIST STUDIO • ज़ूम के लिए पिंच या डबल-टैप करें"
                        else
                            "STAR ARTIST STUDIO • Pinch or double-tap photo to zoom"
                    }

                    Text(
                        text = zoomHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkTextSecondary,
                        fontSize = 11.sp,
                        letterSpacing = 0.4.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("carousel_zoom_status")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (items.size > 1) {
                        LazyRow(
                            state = thumbnailListState,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("carousel_thumbnail_strip")
                        ) {
                            itemsIndexed(
                                items,
                                key = { index, item -> item.id.ifBlank { "thumb_$index" } }
                            ) { idx, thumbItem ->
                                val isSelected = idx == pagerState.currentPage
                                val thumbSource = remember(thumbItem.thumbnailUrl, thumbItem.mediaUrl) {
                                    resolvePortfolioMediaModel(
                                        thumbItem.thumbnailUrl.ifBlank { thumbItem.mediaUrl }
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkStudioElevatedSurface)
                                        .border(
                                            BorderStroke(
                                                width = if (isSelected) 2.dp else 0.5.dp,
                                                color = if (isSelected) DarkGoldPrimary else DarkStudioOutline
                                            ),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(idx)
                                            }
                                        }
                                        .testTag("carousel_thumb_$idx")
                                ) {
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbSource)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = thumbItem.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        error = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(DarkStudioElevatedSurface),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.BrokenImage,
                                                    contentDescription = null,
                                                    tint = DarkTextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(DarkGoldPrimary.copy(alpha = 0.2f))
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onBookStyle(currentItem) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("carousel_book_style_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkGoldPrimary,
                                contentColor = Color(0xFF141000)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "यह स्टाइल बुक करें" else "Book This Style",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val msg = "Hello Star Artist Studio, I am inquiring about portfolio piece: ${currentItem.title} (${currentItem.category}). Please share details."
                                val encodedUrl = "https://wa.me/919541020948?text=" + java.net.URLEncoder.encode(msg, "UTF-8")
                                openWhatsApp(context, encodedUrl)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("carousel_whatsapp_button"),
                            border = BorderStroke(1.dp, WhatsAppGreen),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = WhatsAppGreen
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "व्हाट्सएप पूछताछ" else "WhatsApp Us",
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

/**
 * Individual full-screen page rendering a high-resolution portfolio piece with
 * pinch-to-zoom, double-tap zoom toggle, and video reel indicators without blocking
 * horizontal pager swipes at 1.0x scale.
 */
@Composable
private fun CarouselPageItem(
    item: GalleryItem,
    isActive: Boolean,
    externalResetSignal: Int,
    onScaleChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isActive) {
        if (!isActive) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            onScaleChanged(1f)
        }
    }

    LaunchedEffect(externalResetSignal) {
        if (isActive && externalResetSignal > 0) {
            if (scale > 1.05f) {
                scale = 1f
                offsetX = 0f
                offsetY = 0f
            } else {
                scale = 2.2f
            }
            onScaleChanged(scale)
        }
    }

    val mediaModel = remember(item.mediaUrl) {
        resolvePortfolioMediaModel(item.mediaUrl)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 72.dp)
            .pointerInput(item.id) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.2f
                        }
                        onScaleChanged(scale)
                    }
                )
            }
            .pointerInput(item.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.count { it.pressed }
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        if (pointerCount > 1 || scale > 1.05f) {
                            val updatedScale = (scale * zoomChange).coerceIn(1f, 3.5f)
                            scale = updatedScale
                            onScaleChanged(updatedScale)

                            if (updatedScale > 1.05f) {
                                val maxOffsetX = (size.width * (updatedScale - 1f)) / 2f
                                val maxOffsetY = (size.height * (updatedScale - 1f)) / 2f
                                offsetX = (offsetX + panChange.x).coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = (offsetY + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }

                            event.changes.forEach { change ->
                                if (change.positionChanged()) {
                                    change.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .clip(RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(0.5.dp, DarkGoldPrimary.copy(alpha = 0.4f)),
                    RoundedCornerShape(12.dp)
                )
                .testTag("carousel_image_container"),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaModel)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .testTag("carousel_fullscreen_image"),
                error = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .background(Color(0xFF161618))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = DarkGoldPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = item.title,
                            color = DarkGoldLight,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Preview unavailable offline",
                            color = DarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            if (item.mediaType.equals("video", ignoreCase = true)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0C0C0C).copy(alpha = 0.85f))
                            .border(1.5.dp, DarkGoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "4K Video Preview",
                            tint = DarkGoldPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.82f))
                            .border(1.dp, DarkGoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "4K CINEMA PRODUCTION REEL",
                            color = DarkGoldLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
