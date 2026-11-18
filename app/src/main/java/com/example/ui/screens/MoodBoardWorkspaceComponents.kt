package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.StudioProjectStatus
import com.example.data.local.entity.StudioSessionProjectEntity
import com.example.data.model.AppLanguage
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
import com.example.ui.viewmodel.MoodBoardOrganizerViewModel
import com.example.ui.viewmodel.MoodBoardTile
import com.example.ui.viewmodel.StudioImportCandidate
import java.io.File

internal fun resolveCoilImageModel(pathOrUrl: String): Any {
    return if (pathOrUrl.startsWith("/")) File(pathOrUrl) else pathOrUrl
}

@Composable
internal fun OngoingProjectSelectorCard(
    project: StudioSessionProjectEntity,
    itemCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (project.status) {
        StudioProjectStatus.PLANNING -> GoldPrimary
        StudioProjectStatus.IN_PRODUCTION -> Color(0xFF38BDF8)
        StudioProjectStatus.POST_PROCESSING -> Color(0xFFC084FC)
        StudioProjectStatus.COMPLETED -> SuccessGreen
    }

    Card(
        modifier = modifier
            .width(210.dp)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) GoldPrimary else StudioOutline,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag("moodboard_project_pill_${project.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.14f) else StudioCardSurface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = project.status.labelEn.uppercase(),
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = "$itemCount refs",
                    color = if (isSelected) GoldLight else TextSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = project.projectTitle,
                color = if (isSelected) GoldLight else TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${project.sessionCode} • ${project.serviceCategory}",
                color = TextSecondary,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun MoodBoardVisualTileCard(
    tile: MoodBoardTile,
    projectTitle: String?,
    onInspect: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isConcept = tile is MoodBoardTile.ConceptArtTile
    val badgeColor = if (isConcept) GoldPrimary else Color(0xFF38BDF8)
    val badgeLabel = if (isConcept) "AI CONCEPT" else "REF PHOTO"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (tile.isPinned) 1.5.dp else 1.dp,
                color = if (tile.isPinned) GoldPrimary else StudioOutline,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onInspect)
            .testTag("moodboard_tile_${tile.stableKey}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.9f)
                    .background(StudioElevatedSurface)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(resolveCoilImageModel(tile.imageModelPathOrUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = tile.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top scrim for legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                            )
                        )
                )

                // Type badge top-left
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .border(0.8.dp, badgeColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isConcept) Icons.Default.AutoAwesome else Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = badgeLabel,
                            color = badgeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Pin button top-right (48.dp touch target)
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .testTag("moodboard_tile_pin_${tile.stableKey}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (tile.isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (tile.isPinned) "Unpin from mood board" else "Pin to top",
                            tint = if (tile.isPinned) GoldPrimary else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tile.title,
                    color = GoldLight,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(StudioElevatedSurface)
                            .border(0.8.dp, StudioOutline, RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tile.sectionTag,
                            color = TextSecondary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                if (!projectTitle.isNullOrBlank()) {
                    Text(
                        text = projectTitle,
                        color = GoldPrimary.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Unassigned • Tap to link",
                        color = TextSecondary.copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddGalleryReferenceDialog(
    selectedUri: Uri,
    ongoingProjects: List<StudioSessionProjectEntity>,
    initialProjectId: Long?,
    language: AppLanguage,
    onConfirm: (title: String, sectionTag: String, notes: String, swatches: String, projectId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isHindi = language == AppLanguage.HINDI
    var title by remember { mutableStateOf("Studio Lighting & Pose Ref") }
    var sectionTag by remember { mutableStateOf("Lighting Ref") }
    var notes by remember { mutableStateOf("") }
    var swatches by remember { mutableStateOf("Warm Gold, Obsidian Shadow") }
    var targetProjectId by remember { mutableStateOf(initialProjectId) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .widthIn(max = 520.dp)
                .border(1.2.dp, GoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(18.dp)),
            color = StudioCardSurface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "रेफरेंस फ़ोटो जोड़ें" else "Organize Reference Photo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldLight
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(selectedUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Selected Reference Photo Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioElevatedSurface)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isHindi) "शीर्षक" else "Reference Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = StudioOutline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("moodboard_add_ref_title_input")
                )

                Text(
                    text = if (isHindi) "मूड बोर्ड श्रेणी" else "Board Section Tag",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MoodBoardOrganizerViewModel.SECTION_TAGS.filter { it != "Concept Art" }.forEach { tag ->
                        FilterChip(
                            selected = sectionTag == tag,
                            onClick = { sectionTag = tag },
                            label = { Text(tag, fontSize = 11.sp) }
                        )
                    }
                }

                Text(
                    text = if (isHindi) "प्रोजेक्ट चुनें" else "Attach to Ongoing Project",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = targetProjectId == null,
                        onClick = { targetProjectId = null },
                        label = { Text("General Board", fontSize = 11.sp) }
                    )
                    ongoingProjects.forEach { proj ->
                        FilterChip(
                            selected = targetProjectId == proj.id,
                            onClick = { targetProjectId = proj.id },
                            label = { Text(proj.projectTitle, fontSize = 11.sp, maxLines = 1) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isHindi) "डायरेक्शन नोट्स" else "Art Direction Notes (Optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onConfirm(title, sectionTag, notes, swatches, targetProjectId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color(0xFF141000)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("moodboard_add_ref_confirm_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "मूड बोर्ड में सहेजें" else "Save to Project Mood Board",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ImportStudioShotDialog(
    candidates: List<StudioImportCandidate>,
    language: AppLanguage,
    onImport: (StudioImportCandidate, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isHindi = language == AppLanguage.HINDI
    var chosenSection by remember { mutableStateOf("Framing Guide") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .widthIn(max = 540.dp)
                .border(1.2.dp, GoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(18.dp)),
            color = StudioCardSurface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "स्टूडियो शॉट्स आयात करें" else "Import Studio Reference Shots",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldLight
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text(
                    text = if (isHindi) "मूड बोर्ड सेक्शन चुनें:" else "Tag imported photos as:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MoodBoardOrganizerViewModel.SECTION_TAGS.filter { it != "Concept Art" }.forEach { tag ->
                        FilterChip(
                            selected = chosenSection == tag,
                            onClick = { chosenSection = tag },
                            label = { Text(tag, fontSize = 11.sp) }
                        )
                    }
                }

                if (candidates.isEmpty()) {
                    Text(
                        text = if (isHindi)
                            "कोई सहेजे गए कैटलॉग या पोर्टफोलियो शॉट उपलब्ध नहीं हैं।"
                        else
                            "No studio portfolio or catalog reference shots saved yet. Use Camera Ref or Add Ref Photo!",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    candidates.forEach { candidate ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, StudioOutline, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(resolveCoilImageModel(candidate.imageUrl))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = candidate.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = candidate.title,
                                        color = GoldLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${candidate.sourceLabel} • ${candidate.category}",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                Button(
                                    onClick = { onImport(candidate, chosenSection) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldPrimary,
                                        contentColor = StudioBlack
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("moodboard_import_candidate_${candidate.id}")
                                ) {
                                    Text(if (isHindi) "जोड़ें" else "Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoodBoardTileInspectorDialog(
    tile: MoodBoardTile,
    ongoingProjects: List<StudioSessionProjectEntity>,
    language: AppLanguage,
    onSaveOrganization: (projectId: Long?, sectionTag: String, notes: String) -> Unit,
    onTogglePin: () -> Unit,
    onRemixConcept: (() -> Unit)?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isHindi = language == AppLanguage.HINDI
    var selectedProjectId by remember(tile.stableKey) { mutableStateOf(tile.sessionProjectId) }
    var selectedSection by remember(tile.stableKey) { mutableStateOf(tile.sectionTag) }
    var editedNotes by remember(tile.stableKey) { mutableStateOf(tile.subtitleOrNotes) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .border(1.2.dp, GoldPrimary.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                .testTag("moodboard_inspector_dialog"),
            color = StudioCardSurface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tile.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = if (tile.isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Pin",
                            tint = if (tile.isPinned) GoldPrimary else TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(resolveCoilImageModel(tile.imageModelPathOrUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = tile.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioElevatedSurface)
                )

                Text(
                    text = if (isHindi) "प्रोजेक्ट असाइन करें" else "Assign to Ongoing Studio Project",
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedProjectId == null,
                        onClick = { selectedProjectId = null },
                        label = { Text("Unassigned", fontSize = 11.sp) },
                        modifier = Modifier.testTag("inspector_assign_unassigned")
                    )
                    ongoingProjects.forEach { proj ->
                        FilterChip(
                            selected = selectedProjectId == proj.id,
                            onClick = { selectedProjectId = proj.id },
                            label = { Text("${proj.sessionCode}: ${proj.projectTitle}", fontSize = 11.sp, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldContainer,
                                selectedLabelColor = OnGoldContainer
                            ),
                            modifier = Modifier.testTag("inspector_assign_project_${proj.id}")
                        )
                    }
                }

                Text(
                    text = if (isHindi) "मूड बोर्ड सेक्शन टैग" else "Mood Board Category Tag",
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MoodBoardOrganizerViewModel.SECTION_TAGS.forEach { tag ->
                        FilterChip(
                            selected = selectedSection.equals(tag, ignoreCase = true),
                            onClick = { selectedSection = tag },
                            label = { Text(tag, fontSize = 11.sp) },
                            modifier = Modifier.testTag("inspector_tag_${tag.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                if (tile is MoodBoardTile.ReferencePhotoTile) {
                    OutlinedTextField(
                        value = editedNotes,
                        onValueChange = { editedNotes = it },
                        label = { Text(if (isHindi) "प्रोडक्शन नोट्स" else "Direction & Lighting Notes") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inspector_notes_input")
                    )
                } else if (tile is MoodBoardTile.ConceptArtTile) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StudioOutline, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "AI Prompt & Style • ${tile.entity.visualStyle} • ${tile.entity.lightingSetup}",
                                color = GoldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tile.entity.prompt,
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSaveOrganization(selectedProjectId, selectedSection, editedNotes) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF141000)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("inspector_save_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHindi) "सहेजें" else "Save Board Changes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (onRemixConcept != null) {
                        OutlinedButton(
                            onClick = onRemixConcept,
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("inspector_remix_btn")
                        ) {
                            Icon(Icons.Default.Brush, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "रीमिक्स" else "Remix AI", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDelete,
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("inspector_delete_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "मूड बोर्ड से हटाएं" else "Remove from Mood Board",
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
