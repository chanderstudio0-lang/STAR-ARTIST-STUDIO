package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.StudioSessionProjectEntity
import com.example.data.model.AppLanguage
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
import com.example.ui.viewmodel.MoodBoardMediaFilter
import com.example.ui.viewmodel.MoodBoardOrganizerViewModel
import com.example.ui.viewmodel.MoodBoardTile
import com.example.ui.viewmodel.MoodBoardWorkspaceStats
import com.example.ui.viewmodel.StudioImportCandidate

/**
 * Dedicated 'Mood Board' workspace where artists and producers organize generated concept art
 * alongside reference photos for their ongoing studio projects.
 */
@Composable
fun MoodBoardWorkspaceScreen(
    language: AppLanguage,
    viewModel: MoodBoardOrganizerViewModel,
    onBack: () -> Unit,
    onOpenConceptGenerator: (projectId: Long?) -> Unit,
    onOpenCameraReference: (projectId: Long?) -> Unit,
    onRemixConceptInAiStudio: (MoodBoardTile.ConceptArtTile) -> Unit
) {
    val ongoingProjects by viewModel.ongoingProjects.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val selectedProject by viewModel.selectedProject.collectAsStateWithLifecycle()
    val mediaFilter by viewModel.mediaFilter.collectAsStateWithLifecycle()
    val selectedSection by viewModel.selectedSectionTag.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val tiles by viewModel.filteredTiles.collectAsStateWithLifecycle()
    val stats by viewModel.workspaceStats.collectAsStateWithLifecycle()
    val projectCounts by viewModel.projectItemCounts.collectAsStateWithLifecycle()
    val importCandidates by viewModel.importCandidates.collectAsStateWithLifecycle()

    var pendingPickedUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingPickedUri = uri
        }
    }

    pendingPickedUri?.let { uri ->
        AddGalleryReferenceDialog(
            selectedUri = uri,
            ongoingProjects = ongoingProjects,
            initialProjectId = selectedProjectId,
            language = language,
            onConfirm = { title, sectionTag, notes, swatches, projectId ->
                viewModel.addReferenceFromGalleryUri(
                    uri = uri,
                    title = title,
                    sectionTag = sectionTag,
                    notes = notes,
                    colorSwatches = swatches,
                    targetProjectId = projectId
                )
                pendingPickedUri = null
            },
            onDismiss = { pendingPickedUri = null }
        )
    }

    MoodBoardWorkspaceContent(
        language = language,
        ongoingProjects = ongoingProjects,
        selectedProjectId = selectedProjectId,
        selectedProject = selectedProject,
        projectCounts = projectCounts,
        mediaFilter = mediaFilter,
        selectedSection = selectedSection,
        searchQuery = searchQuery,
        tiles = tiles,
        stats = stats,
        importCandidates = importCandidates,
        onBack = onBack,
        onSelectProject = { viewModel.selectProject(it) },
        onSelectMediaFilter = { viewModel.setMediaFilter(it) },
        onSelectSectionTag = { viewModel.setSectionFilter(it) },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onLaunchGalleryPicker = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onOpenCameraReference = { onOpenCameraReference(selectedProjectId) },
        onOpenConceptGenerator = { onOpenConceptGenerator(selectedProjectId) },
        onImportStudioCandidate = { candidate, sectionTag ->
            viewModel.importStudioCandidate(candidate, sectionTag, selectedProjectId)
        },
        onTogglePin = { viewModel.toggleTilePin(it) },
        onUpdateTileOrganization = { tile, projectId, sectionTag, notes ->
            viewModel.updateTileOrganization(tile, projectId, sectionTag, notes)
        },
        onRemixConcept = onRemixConceptInAiStudio,
        onDeleteTile = { viewModel.deleteTile(it) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodBoardWorkspaceContent(
    language: AppLanguage,
    ongoingProjects: List<StudioSessionProjectEntity>,
    selectedProjectId: Long?,
    selectedProject: StudioSessionProjectEntity?,
    projectCounts: Map<Long, Int>,
    mediaFilter: MoodBoardMediaFilter,
    selectedSection: String,
    searchQuery: String,
    tiles: List<MoodBoardTile>,
    stats: MoodBoardWorkspaceStats,
    importCandidates: List<StudioImportCandidate>,
    onBack: () -> Unit,
    onSelectProject: (Long?) -> Unit,
    onSelectMediaFilter: (MoodBoardMediaFilter) -> Unit,
    onSelectSectionTag: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onLaunchGalleryPicker: () -> Unit,
    onOpenCameraReference: () -> Unit,
    onOpenConceptGenerator: () -> Unit,
    onImportStudioCandidate: (StudioImportCandidate, String) -> Unit,
    onTogglePin: (MoodBoardTile) -> Unit,
    onUpdateTileOrganization: (MoodBoardTile, Long?, String, String) -> Unit,
    onRemixConcept: (MoodBoardTile.ConceptArtTile) -> Unit,
    onDeleteTile: (MoodBoardTile) -> Unit
) {
    BackHandler { onBack() }

    val isHindi = language == AppLanguage.HINDI
    var showImportDialog by remember { mutableStateOf(false) }
    var inspectingTile by remember { mutableStateOf<MoodBoardTile?>(null) }

    val projectTitleById = remember(ongoingProjects) {
        ongoingProjects.associate { it.id to "${it.sessionCode} • ${it.projectTitle}" }
    }

    if (showImportDialog) {
        ImportStudioShotDialog(
            candidates = importCandidates,
            language = language,
            onImport = { candidate, section ->
                onImportStudioCandidate(candidate, section)
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
        )
    }

    inspectingTile?.let { tile ->
        MoodBoardTileInspectorDialog(
            tile = tile,
            ongoingProjects = ongoingProjects,
            language = language,
            onSaveOrganization = { projectId, sectionTag, notes ->
                onUpdateTileOrganization(tile, projectId, sectionTag, notes)
                inspectingTile = null
            },
            onTogglePin = {
                onTogglePin(tile)
                inspectingTile = null
            },
            onRemixConcept = if (tile is MoodBoardTile.ConceptArtTile) {
                {
                    inspectingTile = null
                    onRemixConcept(tile)
                }
            } else {
                null
            },
            onDelete = {
                onDeleteTile(tile)
                inspectingTile = null
            },
            onDismiss = { inspectingTile = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("moodboard_workspace_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(StudioElevatedSurface)
                            .testTag("moodboard_ws_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isHindi) "वापस जाएं" else "Back",
                            tint = GoldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DashboardCustomize,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "मूड बोर्ड ऑर्गनाइज़र" else "Project Mood Board",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldLight
                            )
                        }
                        Text(
                            text = if (isHindi)
                                "चल रहे प्रोजेक्ट्स के लिए AI कॉन्सेप्ट आर्ट और रेफरेंस फ़ोटो व्यवस्थित करें"
                            else
                                "Organize AI concept art & reference shots for ongoing shoots",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        // 2. Ongoing Projects Horizontal Rail
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "चल रहे स्टूडियो प्रोजेक्ट्स" else "Ongoing Studio Projects",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    Text(
                        text = selectedProject?.projectTitle ?: if (isHindi) "सभी प्रोजेक्ट्स" else "All Boards",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        val totalAll = projectCounts.values.sum()
                        Card(
                            modifier = Modifier
                                .width(136.dp)
                                .border(
                                    width = if (selectedProjectId == null) 1.5.dp else 1.dp,
                                    color = if (selectedProjectId == null) GoldPrimary else StudioOutline,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectProject(null) }
                                .testTag("moodboard_project_all_pill"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedProjectId == null) {
                                    GoldPrimary.copy(alpha = 0.14f)
                                } else {
                                    StudioCardSurface
                                }
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "WORKSPACE",
                                    color = GoldPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (isHindi) "सभी प्रोजेक्ट्स" else "All Projects",
                                    color = if (selectedProjectId == null) GoldLight else TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$totalAll linked",
                                    color = TextSecondary,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }

                    items(ongoingProjects, key = { it.id }) { project ->
                        OngoingProjectSelectorCard(
                            project = project,
                            itemCount = projectCounts[project.id] ?: 0,
                            isSelected = selectedProjectId == project.id,
                            onClick = { onSelectProject(project.id) }
                        )
                    }
                }
            }
        }

        // 3. Stats Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BoardStatMiniCard(
                    label = if (isHindi) "कुल आइटम" else "Board Items",
                    value = stats.totalItems.toString(),
                    accent = GoldPrimary,
                    modifier = Modifier.weight(1f)
                )
                BoardStatMiniCard(
                    label = if (isHindi) "AI आर्ट" else "AI Concepts",
                    value = stats.conceptArtCount.toString(),
                    accent = GoldLight,
                    modifier = Modifier.weight(1f)
                )
                BoardStatMiniCard(
                    label = if (isHindi) "रेफरेंस फ़ोटो" else "Ref Photos",
                    value = stats.referencePhotoCount.toString(),
                    accent = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                BoardStatMiniCard(
                    label = if (isHindi) "पिन किए गए" else "Pinned",
                    value = stats.pinnedCount.toString(),
                    accent = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Quick Add / Generate Action Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenConceptGenerator,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color(0xFF141000)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("moodboard_ws_generate_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "AI कॉन्सेप्ट आर्ट" else "AI Concept Art",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onLaunchGalleryPicker,
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("moodboard_ws_add_photo_btn")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "गैलरी फ़ोटो" else "Add Ref Photo",
                                color = GoldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenCameraReference,
                            border = BorderStroke(1.dp, StudioOutline),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("moodboard_ws_camera_btn")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "कैमरा रेफरेंस" else "Camera Ref",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            border = BorderStroke(1.dp, StudioOutline),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("moodboard_ws_import_btn")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "स्टूडियो शॉट्स" else "Studio Shots",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 5. Search & Organization Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = if (isHindi)
                                "शीर्षक, लाइटिंग या रंग खोजें..."
                            else
                                "Search concepts, lighting refs, notes...",
                            fontSize = 12.5.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = StudioOutline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("moodboard_ws_search_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val mediaOptions = listOf(
                        MoodBoardMediaFilter.ALL to (if (isHindi) "सभी" else "All"),
                        MoodBoardMediaFilter.CONCEPT_ART to (if (isHindi) "AI कॉन्सेप्ट" else "Concept Art"),
                        MoodBoardMediaFilter.REFERENCE_PHOTOS to (if (isHindi) "रेफरेंस फ़ोटो" else "Ref Photos"),
                        MoodBoardMediaFilter.PINNED to (if (isHindi) "पिन किए गए" else "Pinned")
                    )
                    mediaOptions.forEach { (filter, label) ->
                        FilterChip(
                            selected = mediaFilter == filter,
                            onClick = { onSelectMediaFilter(filter) },
                            label = { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color(0xFF141000)
                            ),
                            modifier = Modifier.testTag("moodboard_ws_media_${filter.name.lowercase()}")
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val sections = listOf(MoodBoardOrganizerViewModel.ALL_SECTIONS) + MoodBoardOrganizerViewModel.SECTION_TAGS
                    items(sections) { section ->
                        FilterChip(
                            selected = selectedSection == section,
                            onClick = { onSelectSectionTag(section) },
                            label = { Text(section, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldContainer,
                                selectedLabelColor = OnGoldContainer
                            ),
                            modifier = Modifier.testTag("moodboard_ws_section_${section.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        // 6. Side-by-Side 2-Column Visual Collage Grid
        if (tiles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
                        .testTag("moodboard_ws_empty_state"),
                    colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DashboardCustomize,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = if (isHindi)
                                "इस प्रोजेक्ट मूड बोर्ड में अभी कोई आइटम नहीं है"
                            else
                                "Your Project Mood Board Canvas is Empty",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isHindi)
                                "ऊपर 'AI कॉन्सेप्ट आर्ट' बनाएं या 'गैलरी फ़ोटो' व 'कैमरा रेफरेंस' से संदर्भ चित्र जोड़ें।"
                            else
                                "Generate AI concept art, pick gallery reference shots, or import studio portfolio frames above to organize this shoot visually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val pairedRows = tiles.chunked(2)
            items(
                items = pairedRows,
                key = { row -> row.joinToString("_") { it.stableKey } }
            ) { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTiles.forEach { tile ->
                        MoodBoardVisualTileCard(
                            tile = tile,
                            projectTitle = tile.sessionProjectId?.let { projectTitleById[it] },
                            onInspect = { inspectingTile = tile },
                            onTogglePin = { onTogglePin(tile) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowTiles.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardStatMiniCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, StudioOutline, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
