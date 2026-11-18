package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppLanguage
import com.example.data.model.StudioServicesData
import com.example.ui.screens.CapturedReferenceShot
import com.example.ui.screens.PhotoPreviewDefaults
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
import com.example.ui.viewmodel.PhotoPreviewDestination

/**
 * Post-capture Reference Shot review, session filmstrip, metadata editor, and destination routing panel
 * allowing artists to save directly into Portfolio Archive (Room), Service Catalog packages, or Studio Gallery (Firestore).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoPreviewReviewPanel(
    language: AppLanguage,
    sessionShots: List<CapturedReferenceShot>,
    selectedShot: CapturedReferenceShot?,
    selectedDestination: PhotoPreviewDestination,
    selectedCategory: String,
    selectedPackageId: String,
    title: String,
    notes: String,
    isSubmitting: Boolean,
    lastSavedDestination: PhotoPreviewDestination?,
    onSelectShot: (CapturedReferenceShot) -> Unit,
    onRetakePhoto: () -> Unit,
    onSelectDestination: (PhotoPreviewDestination) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectPackageId: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSaveReferenceShot: () -> Unit,
    onOpenSavedTarget: (PhotoPreviewDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHindi = language == AppLanguage.HINDI
    val isLight = StudioTheme.isLight
    val accentText = if (isLight) GoldDark else GoldLight
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Session Reference Filmstrip (only shown when >= 1 shot has been taken)
        if (sessionShots.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isHindi)
                        "सत्र रेफरेंस फिल्मस्ट्रिप (${sessionShots.size})"
                    else
                        "Session Reference Filmstrip (${sessionShots.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentText
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photo_preview_filmstrip")
                ) {
                    items(sessionShots, key = { it.id }) { shot ->
                        val isCurrent = selectedShot?.id == shot.id
                        Box(
                            modifier = Modifier
                                .size(width = 74.dp, height = 74.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isCurrent) 2.5.dp else 1.dp,
                                    color = if (isCurrent) GoldPrimary else StudioOutline,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectShot(shot) }
                                .testTag("photo_preview_filmstrip_item_${shot.id}")
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(resolvePortfolioMediaModel(shot.encodedPhoto.dataUrl))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = shot.resolutionBadge,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.72f),
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            ) {
                                Text(
                                    text = shot.aspectGuide.labelEn.substringBefore(" "),
                                    color = GoldLight,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Review & Routing Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.dp, Brush.verticalGradient(listOf(GoldPrimary.copy(alpha = 0.55f), StudioOutline))),
                    RoundedCornerShape(18.dp)
                )
                .testTag("photo_preview_review_panel"),
            colors = CardDefaults.elevatedCardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedShot != null) {
                    // Captured Image Preview + Resolution Telemetry
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(StudioElevatedSurface)
                            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(resolvePortfolioMediaModel(selectedShot.encodedPhoto.dataUrl))
                                .crossfade(true)
                                .build(),
                            contentDescription = if (isHindi) "खींची गई संदर्भ फोटो" else "Captured Reference Shot Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("photo_preview_captured_image")
                        )

                        Surface(
                            color = Color.Black.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, GoldPrimary),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.HighQuality, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "${selectedShot.resolutionBadge} • ${selectedShot.sizeKb} KB",
                                    color = GoldLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onRetakePhoto,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.74f),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("photo_preview_retake_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "नया शॉट" else "Retake",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Surface(
                        color = StudioElevatedSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StudioOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("photo_preview_awaiting_shot_hint")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                            Text(
                                text = if (isHindi)
                                    "संदर्भ शॉट लेने के लिए ऊपर दिए गोल्ड शटर बटन को दबाएं, फिर लक्ष्य चुनें।"
                                else
                                    "Tap the gold shutter button above (or pick from Library) to preview & route your reference shot.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Destination Selector: Portfolio Archive vs Service Catalog vs Studio Gallery
                Text(
                    text = if (isHindi) "कहां सहेजें (Destination)" else "Save Destination",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentText
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DestinationChip(
                        selected = selectedDestination == PhotoPreviewDestination.PORTFOLIO_ARCHIVE,
                        label = if (isHindi) "पोर्टफोलियो आर्काइव" else "Portfolio Archive",
                        icon = Icons.Default.Collections,
                        testTag = "photo_preview_dest_portfolio",
                        onClick = { onSelectDestination(PhotoPreviewDestination.PORTFOLIO_ARCHIVE) }
                    )
                    DestinationChip(
                        selected = selectedDestination == PhotoPreviewDestination.SERVICE_CATALOG,
                        label = if (isHindi) "सेवा कैटलॉग" else "Service Catalog",
                        icon = Icons.Default.WorkspacePremium,
                        testTag = "photo_preview_dest_catalog",
                        onClick = { onSelectDestination(PhotoPreviewDestination.SERVICE_CATALOG) }
                    )
                    DestinationChip(
                        selected = selectedDestination == PhotoPreviewDestination.STUDIO_GALLERY,
                        label = if (isHindi) "स्टूडियो गैलरी" else "Studio Gallery",
                        icon = Icons.Default.CloudUpload,
                        testTag = "photo_preview_dest_gallery",
                        onClick = { onSelectDestination(PhotoPreviewDestination.STUDIO_GALLERY) }
                    )
                    DestinationChip(
                        selected = selectedDestination == PhotoPreviewDestination.MOOD_BOARD,
                        label = if (isHindi) "मूड बोर्ड" else "Mood Board",
                        icon = Icons.Default.Palette,
                        testTag = "photo_preview_dest_moodboard",
                        onClick = { onSelectDestination(PhotoPreviewDestination.MOOD_BOARD) }
                    )
                }

                // Destination-specific target selector (Package for Service Catalog OR Category for Portfolio/Gallery)
                if (selectedDestination == PhotoPreviewDestination.SERVICE_CATALOG) {
                    Text(
                        text = if (isHindi) "कैटलॉग सेवा पैकेज चुनें" else "Attach to Service Catalog Package",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StudioServicesData.packages.forEach { pkg ->
                            val isSelected = selectedPackageId == pkg.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectPackageId(pkg.id) },
                                label = {
                                    Text(
                                        text = if (isHindi) pkg.titleHi else pkg.titleEn,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else pkg.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldContainer,
                                    selectedLabelColor = OnGoldContainer,
                                    selectedLeadingIconColor = GoldPrimary,
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
                                    .semantics(mergeDescendants = true) {}
                                    .testTag("photo_preview_package_chip_${pkg.id}")
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (isHindi) "प्रोजेक्ट श्रेणी" else "Portfolio Category",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PhotoPreviewDefaults.portfolioCategories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectCategory(cat) },
                                label = {
                                    Text(
                                        text = cat,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldContainer,
                                    selectedLabelColor = OnGoldContainer,
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
                                    .testTag("photo_preview_category_chip_${cat.lowercase()}")
                            )
                        }
                    }
                }

                // Editable Title & Reference Lighting/Pose Notes
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(if (isHindi) "शॉट शीर्षक" else "Reference Shot Title") },
                    placeholder = { Text(if (isHindi) "उदा. की-लाइट ब्राइडल टेस्ट" else "e.g. Key-Light Editorial Framing") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = StudioOutline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photo_preview_title_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text(if (isHindi) "लाइटिंग व पोज़ नोट्स (वैकल्पिक)" else "Lighting, Lens & Framing Notes (Optional)") },
                    placeholder = { Text(if (isHindi) "उदा. 85mm f/1.8 • रिम लाइट" else "e.g. 85mm f/1.8 • 45° Rim Light") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = StudioOutline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photo_preview_notes_input")
                )

                Button(
                    onClick = onSaveReferenceShot,
                    enabled = selectedShot != null && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .semantics(mergeDescendants = true) {}
                        .testTag("photo_preview_save_btn")
                ) {
                    Icon(Icons.Default.BookmarkAdded, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (selectedDestination) {
                            PhotoPreviewDestination.PORTFOLIO_ARCHIVE ->
                                if (isHindi) "पोर्टफोलियो आर्काइव में सहेजें" else "Save to Portfolio Archive"
                            PhotoPreviewDestination.SERVICE_CATALOG ->
                                if (isHindi) "कैटलॉग पैकेज में जोड़ें" else "Attach to Service Catalog"
                            PhotoPreviewDestination.STUDIO_GALLERY ->
                                if (isHindi) "स्टूडियो गैलरी में प्रकाशित करें" else "Publish to Studio Gallery"
                            PhotoPreviewDestination.MOOD_BOARD ->
                                if (isHindi) "मूड बोर्ड में सहेजें" else "Save to Project Mood Board"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }

                // Inline Saved Confirmation Banner with Direct Navigation
                AnimatedVisibility(visible = lastSavedDestination != null) {
                    if (lastSavedDestination != null) {
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("photo_preview_saved_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (lastSavedDestination) {
                                            PhotoPreviewDestination.PORTFOLIO_ARCHIVE ->
                                                if (isHindi) "पोर्टफोलियो आर्काइव में सहेजा गया" else "Saved to Portfolio Archive"
                                            PhotoPreviewDestination.SERVICE_CATALOG ->
                                                if (isHindi) "सेवा कैटलॉग में जोड़ा गया" else "Attached to Service Catalog"
                                            PhotoPreviewDestination.STUDIO_GALLERY ->
                                                if (isHindi) "गैलरी के लिए सबमिट किया गया" else "Submitted to Studio Gallery"
                                            PhotoPreviewDestination.MOOD_BOARD ->
                                                if (isHindi) "मूड बोर्ड में सहेजा गया" else "Saved to Project Mood Board"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onOpenSavedTarget(lastSavedDestination) },
                                    border = BorderStroke(1.dp, SuccessGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("photo_preview_open_saved_target_btn")
                                ) {
                                    Text(
                                        text = if (isHindi) "देखें" else "View",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
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

@Composable
private fun DestinationChip(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = GoldPrimary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = StudioElevatedSurface,
            labelColor = TextSecondary,
            iconColor = GoldPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) GoldPrimary else StudioOutline
        ),
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .semantics(mergeDescendants = true) {}
            .testTag(testTag)
    )
}
