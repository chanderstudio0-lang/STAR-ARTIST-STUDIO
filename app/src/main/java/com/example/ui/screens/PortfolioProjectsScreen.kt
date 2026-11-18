package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.PortfolioProjectEntity
import com.example.data.local.entity.PortfolioProjectMediaType
import com.example.data.model.AppLanguage
import com.example.data.repository.PortfolioProjectsRepository
import com.example.ui.components.ScrollToTopGridFab
import com.example.ui.components.resolvePortfolioMediaModel
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldContainer
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

enum class ProjectMediaFilter {
    ALL,
    PHOTO,
    VIDEO,
    FAVORITES
}

/**
 * Pure helper utilities for filtering and summarizing Room-backed portfolio projects.
 */
object PortfolioProjectFilterUtils {

    fun filterProjects(
        projects: List<PortfolioProjectEntity>,
        mediaFilter: ProjectMediaFilter,
        selectedCategory: String,
        searchQuery: String
    ): List<PortfolioProjectEntity> {
        val cleanQuery = searchQuery.trim()
        return projects.filter { project ->
            val matchesMedia = when (mediaFilter) {
                ProjectMediaFilter.ALL -> true
                ProjectMediaFilter.PHOTO -> project.mediaType.equals(PortfolioProjectMediaType.PHOTO, ignoreCase = true)
                ProjectMediaFilter.VIDEO -> project.mediaType.equals(PortfolioProjectMediaType.VIDEO, ignoreCase = true)
                ProjectMediaFilter.FAVORITES -> project.isFavorite
            }
            val matchesCategory = selectedCategory.equals("All", ignoreCase = true) ||
                project.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = cleanQuery.isEmpty() ||
                project.title.contains(cleanQuery, ignoreCase = true) ||
                project.category.contains(cleanQuery, ignoreCase = true) ||
                project.clientName.contains(cleanQuery, ignoreCase = true) ||
                project.cameraGear.contains(cleanQuery, ignoreCase = true)
            matchesMedia && matchesCategory && matchesSearch
        }
    }
}

/**
 * Jetpack Compose screen displaying Room-persisted Photography and Video Production projects
 * with image/video thumbnails, technical gear metadata, media filtering, detail preview, and
 * on-device Room insertion.
 */
@Composable
fun PortfolioProjectsScreen(
    language: AppLanguage = AppLanguage.ENGLISH,
    projects: List<PortfolioProjectEntity>,
    onBack: () -> Unit = {},
    onToggleFavorite: (PortfolioProjectEntity) -> Unit = {},
    onDeleteProject: (PortfolioProjectEntity) -> Unit = {},
    onAddProject: (
        title: String,
        mediaType: String,
        category: String,
        thumbnailUrl: String,
        mediaUrl: String,
        clientName: String,
        cameraGear: String,
        durationSeconds: Int,
        shotCount: Int,
        resolutionTag: String,
        description: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    onBookSimilarShoot: (serviceType: String) -> Unit = {},
    onOpenPhotoPreview: () -> Unit = {}
) {
    BackHandler { onBack() }

    val isHindi = language == AppLanguage.HINDI
    val isLight = StudioTheme.isLight
    val accentText = if (isLight) GoldDark else GoldLight

    var searchQuery by remember { mutableStateOf("") }
    var mediaFilter by remember { mutableStateOf(ProjectMediaFilter.ALL) }
    var selectedCategory by remember { mutableStateOf("All") }
    var activeProject by remember { mutableStateOf<PortfolioProjectEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    val categories = listOf("All", "Fashion", "Bridal", "Portrait", "Commercial", "Music Video", "Editorial")

    val photoCount by remember(projects) {
        derivedStateOf {
            projects.count { it.mediaType.equals(PortfolioProjectMediaType.PHOTO, ignoreCase = true) }
        }
    }
    val videoCount by remember(projects) {
        derivedStateOf {
            projects.count { it.mediaType.equals(PortfolioProjectMediaType.VIDEO, ignoreCase = true) }
        }
    }
    val favoritesCount by remember(projects) {
        derivedStateOf { projects.count { it.isFavorite } }
    }

    val filteredProjects by remember(projects, mediaFilter, selectedCategory, searchQuery) {
        derivedStateOf {
            PortfolioProjectFilterUtils.filterProjects(
                projects = projects,
                mediaFilter = mediaFilter,
                selectedCategory = selectedCategory,
                searchQuery = searchQuery
            )
        }
    }

    // Keep detail sheet synced if item favorite flag toggles in Room
    val resolvedActiveProject = activeProject?.let { current ->
        projects.find { it.id == current.id } ?: current
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("portfolio_projects_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("portfolio_projects_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isHindi) "वापस जाएं" else "Back",
                            tint = GoldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = if (isHindi) "फोटोग्राफी व वीडियो प्रोजेक्ट्स" else "Project Portfolio Archive",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isHindi) "रूम डेटाबेस • फ़ोटो सेट व 4K फ़िल्म थंबनेल" else "Room SQLite Archive • Photo Sets & 4K Film Reels",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onOpenPhotoPreview,
                        border = BorderStroke(1.dp, GoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("portfolio_projects_camera_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = if (isHindi) "रेफरेंस कैमरा खोलें" else "Open Photo Preview Camera",
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("add_portfolio_project_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHindi) "नया जोड़ें" else "Add Project",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // KPI Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProjectSummaryPill(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("projects_summary_photo_count"),
                    icon = Icons.Default.PhotoCamera,
                    count = photoCount,
                    label = if (isHindi) "फोटो प्रोजेक्ट" else "Photography",
                    accentColor = accentText
                )
                ProjectSummaryPill(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("projects_summary_video_count"),
                    icon = Icons.Default.Videocam,
                    count = videoCount,
                    label = if (isHindi) "वीडियो फ़िल्में" else "Video Films",
                    accentColor = accentText
                )
                ProjectSummaryPill(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("projects_summary_favorites_count"),
                    icon = Icons.Default.Favorite,
                    count = favoritesCount,
                    label = if (isHindi) "पसंदीदा" else "Favorites",
                    accentColor = accentText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (isHindi) "प्रोजेक्ट, क्लाइंट या कैमरा गियर खोजें..." else "Search project, client, or camera gear...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = if (isHindi) "खोजें" else "Search projects",
                        tint = GoldPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isHindi) "खोज साफ़ करें" else "Clear search",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = StudioOutline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = StudioCardSurface,
                    unfocusedContainerColor = StudioCardSurface,
                    cursorColor = GoldPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("projects_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Media Type Filter Row (All / Photography / Video / Favorites)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                ProjectFilterChipItem(
                    selected = mediaFilter == ProjectMediaFilter.ALL,
                    label = if (isHindi) "सभी (${projects.size})" else "All (${projects.size})",
                    icon = Icons.Default.Collections,
                    testTag = "projects_filter_all",
                    onClick = { mediaFilter = ProjectMediaFilter.ALL }
                )
                ProjectFilterChipItem(
                    selected = mediaFilter == ProjectMediaFilter.PHOTO,
                    label = if (isHindi) "फोटोग्राफी ($photoCount)" else "Photos ($photoCount)",
                    icon = Icons.Default.PhotoCamera,
                    testTag = "projects_filter_photo",
                    onClick = { mediaFilter = ProjectMediaFilter.PHOTO }
                )
                ProjectFilterChipItem(
                    selected = mediaFilter == ProjectMediaFilter.VIDEO,
                    label = if (isHindi) "वीडियो ($videoCount)" else "Videos ($videoCount)",
                    icon = Icons.Default.Videocam,
                    testTag = "projects_filter_video",
                    onClick = { mediaFilter = ProjectMediaFilter.VIDEO }
                )
                ProjectFilterChipItem(
                    selected = mediaFilter == ProjectMediaFilter.FAVORITES,
                    label = if (isHindi) "पसंदीदा ($favoritesCount)" else "Favorites ($favoritesCount)",
                    icon = Icons.Default.Favorite,
                    testTag = "projects_filter_favorites",
                    onClick = { mediaFilter = ProjectMediaFilter.FAVORITES }
                )
            }

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = cat,
                                fontSize = 11.5.sp,
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
                            selectedContainerColor = GoldContainer,
                            selectedLabelColor = accentText,
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
                            .testTag("projects_category_chip_${cat.lowercase().replace(" ", "_")}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Project Grid or Empty State
            if (filteredProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("projects_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = GoldPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (isHindi) "कोई पोर्टफोलियो प्रोजेक्ट नहीं मिला" else "No portfolio projects match these filters",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isHindi)
                                "अन्य फोटोग्राफी और वीडियो प्रोजेक्ट देखने के लिए फ़िल्टर रीसेट करें या नया प्रोजेक्ट जोड़ें।"
                            else
                                "Reset your active filters or add a new photo/video project to your Room archive.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                mediaFilter = ProjectMediaFilter.ALL
                                selectedCategory = "All"
                            },
                            border = BorderStroke(1.dp, GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("projects_reset_filters_button")
                        ) {
                            Text(
                                text = if (isHindi) "फ़िल्टर रीसेट करें" else "Reset Filters",
                                color = accentText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 172.dp),
                    state = gridState,
                    contentPadding = PaddingValues(top = 4.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("portfolio_projects_grid")
                ) {
                    // Featured Banner Strip when browsing All
                    val featuredLead = filteredProjects.firstOrNull { it.isFeatured }
                    if (featuredLead != null && mediaFilter == ProjectMediaFilter.ALL && selectedCategory == "All" && searchQuery.isBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "featured_banner_${featuredLead.id}") {
                            FeaturedProjectSpotlightCard(
                                project = featuredLead,
                                language = language,
                                onClick = { activeProject = featuredLead },
                                onToggleFavorite = { onToggleFavorite(featuredLead) }
                            )
                        }
                    }

                    items(filteredProjects, key = { it.id }) { project ->
                        PortfolioProjectCard(
                            project = project,
                            language = language,
                            onClick = { activeProject = project },
                            onToggleFavorite = { onToggleFavorite(project) }
                        )
                    }
                }
            }
        }

        ScrollToTopGridFab(
            gridState = gridState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .testTag("projects_scroll_to_top_fab")
        )

        // Detail & Full-Size Media Preview Dialog
        if (resolvedActiveProject != null) {
            PortfolioProjectDetailDialog(
                project = resolvedActiveProject,
                language = language,
                onDismiss = { activeProject = null },
                onToggleFavorite = { onToggleFavorite(resolvedActiveProject) },
                onDelete = {
                    onDeleteProject(resolvedActiveProject)
                    activeProject = null
                },
                onBookSimilar = {
                    val targetService = if (resolvedActiveProject.mediaType.equals(PortfolioProjectMediaType.VIDEO, ignoreCase = true)) {
                        "Video Production"
                    } else {
                        "Photography (${resolvedActiveProject.category})"
                    }
                    activeProject = null
                    onBookSimilarShoot(targetService)
                }
            )
        }

        // Add New Room Portfolio Project Dialog
        if (showAddDialog) {
            AddPortfolioProjectDialog(
                language = language,
                onDismiss = { showAddDialog = false },
                onSave = { title, mediaType, category, thumbUrl, mediaUrl, clientName, cameraGear, durationSecs, shots, resolution, desc ->
                    onAddProject(
                        title,
                        mediaType,
                        category,
                        thumbUrl,
                        mediaUrl,
                        clientName,
                        cameraGear,
                        durationSecs,
                        shots,
                        resolution,
                        desc
                    )
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun ProjectSummaryPill(
    icon: ImageVector,
    count: Int,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = StudioElevatedSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioOutline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(GoldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Column {
                Text(
                    text = count.toString(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = accentColor
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProjectFilterChipItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
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
            containerColor = StudioCardSurface,
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
            .testTag(testTag)
    )
}

@Composable
private fun FeaturedProjectSpotlightCard(
    project: PortfolioProjectEntity,
    language: AppLanguage,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    val isVideo = project.mediaType.equals(PortfolioProjectMediaType.VIDEO, ignoreCase = true)
    val context = LocalContext.current
    val mediaModel = remember(project.thumbnailUrl, project.mediaUrl) {
        resolvePortfolioMediaModel(project.thumbnailUrl.ifBlank { project.mediaUrl })
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(GoldPrimary, GoldDark, StudioOutline))),
                RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("featured_project_spotlight_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
        ) {
            ProjectThumbnailImage(
                mediaModel = mediaModel,
                contentDescription = project.title,
                isVideo = isVideo,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // Spotlight Badge
            Surface(
                color = GoldPrimary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF141108),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (isHindi) "फीचर्ड प्रोजेक्ट" else "FEATURED STUDIO WORK",
                        color = Color(0xFF141108),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            // Favorite Icon Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (project.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (project.isFavorite) "Remove from favorites" else "Save to favorites",
                    tint = if (project.isFavorite) GoldPrimary else Color.White
                )
            }

            if (isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.68f))
                        .border(1.5.dp, GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video project",
                        tint = GoldPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "${project.category} • ${project.resolutionTag}",
                    color = GoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = project.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(project.clientName, project.cameraGear)
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    color = Color(0xFFDCDCDC),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PortfolioProjectCard(
    project: PortfolioProjectEntity,
    language: AppLanguage = AppLanguage.ENGLISH,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    val isLight = StudioTheme.isLight
    val accentText = if (isLight) GoldDark else GoldLight
    val isVideo = project.mediaType.equals(PortfolioProjectMediaType.VIDEO, ignoreCase = true)
    val mediaModel = remember(project.thumbnailUrl, project.mediaUrl) {
        resolvePortfolioMediaModel(project.thumbnailUrl.ifBlank { project.mediaUrl })
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, Brush.verticalGradient(listOf(GoldPrimary.copy(alpha = 0.5f), StudioOutline))),
                RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .testTag("project_card_${project.id}"),
        colors = CardDefaults.elevatedCardColors(containerColor = StudioCardSurface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            // Visual Thumbnail Box (Photo or Video Cover + Play Indicator)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.9f)
                    .background(StudioElevatedSurface)
            ) {
                ProjectThumbnailImage(
                    mediaModel = mediaModel,
                    contentDescription = project.title,
                    isVideo = isVideo,
                    modifier = Modifier.fillMaxSize()
                )

                // Scrim for legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                            )
                        )
                )

                // Media Type Pill (PHOTO vs VIDEO)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.78f))
                        .border(0.6.dp, GoldPrimary.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag(if (isVideo) "project_video_badge_${project.id}" else "project_photo_badge_${project.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (isVideo) {
                                if (isHindi) "वीडियो" else "VIDEO"
                            } else {
                                if (isHindi) "फोटो" else "PHOTO"
                            },
                            color = Color(0xFFF6E79D),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Favorite Toggle Button (48.dp touch target)
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .minimumInteractiveComponentSize()
                        .testTag("project_favorite_btn_${project.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.58f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (project.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (project.isFavorite) "Remove favorite" else "Mark favorite",
                            tint = if (project.isFavorite) GoldPrimary else Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Center Play Overlay for Video Thumbnails
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.72f))
                            .border(1.2.dp, GoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play video project preview",
                            tint = GoldPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Bottom Overlay Metadata: Category + Duration or Shot Count
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.category,
                        color = Color(0xFFF6E79D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                            .let { mod ->
                                if (isVideo) mod.testTag("project_video_duration_${project.id}") else mod
                            }
                    ) {
                        Text(
                            text = if (isVideo) {
                                PortfolioProjectsRepository.formatDuration(project.durationSeconds)
                            } else {
                                "${project.shotCount} " + if (isHindi) "शॉट्स" else "Shots"
                            },
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Card Text Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.resolutionTag,
                        color = accentText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = project.shootYear.toString(),
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )

                if (project.cameraGear.isNotBlank()) {
                    Text(
                        text = project.cameraGear,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectThumbnailImage(
    mediaModel: Any,
    contentDescription: String,
    isVideo: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(mediaModel)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(StudioElevatedSurface, GoldContainer)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Movie else Icons.Default.BrokenImage,
                    contentDescription = null,
                    tint = GoldPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    )
}

@Composable
private fun PortfolioProjectDetailDialog(
    project: PortfolioProjectEntity,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onBookSimilar: () -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    val isLight = StudioTheme.isLight
    val accentText = if (isLight) GoldDark else GoldLight
    val isVideo = project.mediaType.equals(PortfolioProjectMediaType.VIDEO, ignoreCase = true)
    val mediaModel = remember(project.thumbnailUrl, project.mediaUrl) {
        resolvePortfolioMediaModel(project.thumbnailUrl.ifBlank { project.mediaUrl })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .border(1.2.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                .testTag("project_detail_dialog"),
            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Media Header Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(StudioElevatedSurface)
                ) {
                    ProjectThumbnailImage(
                        mediaModel = mediaModel,
                        contentDescription = project.title,
                        isVideo = isVideo,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("project_detail_favorite")
                        ) {
                            Icon(
                                imageVector = if (project.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (project.isFavorite) GoldPrimary else Color.White
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("project_detail_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isHindi) "बंद करें" else "Close preview",
                                tint = Color.White
                            )
                        }
                    }

                    if (isVideo) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.76f))
                                    .border(1.5.dp, GoldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Video Reel Preview",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = Color.Black.copy(alpha = 0.78f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${project.resolutionTag} • ${PortfolioProjectsRepository.formatDuration(project.durationSeconds)}",
                                    color = Color(0xFFF6E79D),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = GoldPrimary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isVideo) "VIDEO FILM" else "PHOTO SERIES",
                                color = Color(0xFF141108),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = Color.Black.copy(alpha = 0.76f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = project.category,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Body Details
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (project.description.isNotBlank()) {
                        Text(
                            text = project.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 19.sp
                        )
                    }

                    // Production Specs Card
                    Surface(
                        color = StudioElevatedSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StudioOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (project.clientName.isNotBlank()) {
                                ProjectSpecRow(
                                    label = if (isHindi) "क्लाइंट" else "Client",
                                    value = project.clientName,
                                    accentColor = accentText
                                )
                            }
                            if (project.cameraGear.isNotBlank()) {
                                ProjectSpecRow(
                                    label = if (isHindi) "कैमरा और लेंस" else "Camera Gear",
                                    value = project.cameraGear,
                                    accentColor = accentText
                                )
                            }
                            ProjectSpecRow(
                                label = if (isHindi) "आउटपुट / अवधि" else if (isVideo) "Runtime" else "Curated Frames",
                                value = if (isVideo) {
                                    "${PortfolioProjectsRepository.formatDuration(project.durationSeconds)} (${project.resolutionTag})"
                                } else {
                                    "${project.shotCount} Shots (${project.resolutionTag})"
                                },
                                accentColor = accentText
                            )
                            if (project.location.isNotBlank()) {
                                ProjectSpecRow(
                                    label = if (isHindi) "स्थान" else "Set Location",
                                    value = project.location,
                                    accentColor = accentText
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDelete,
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("project_detail_delete")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "हटाएं" else "Remove",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = onBookSimilar,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("project_detail_book_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "समान शूट बुक करें" else "Book Similar Shoot",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectSpecRow(
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun AddPortfolioProjectDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        mediaType: String,
        category: String,
        thumbnailUrl: String,
        mediaUrl: String,
        clientName: String,
        cameraGear: String,
        durationSeconds: Int,
        shotCount: Int,
        resolutionTag: String,
        description: String
    ) -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    val isLight = StudioTheme.isLight
    val accentText = if (isLight) GoldDark else GoldLight

    var mediaType by remember { mutableStateOf(PortfolioProjectMediaType.PHOTO) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Fashion") }
    var clientName by remember { mutableStateOf("") }
    var cameraGear by remember { mutableStateOf("") }
    var durationSecondsText by remember { mutableStateOf("90") }
    var shotCountText by remember { mutableStateOf("24") }
    var description by remember { mutableStateOf("") }
    var selectedCoverKey by remember { mutableStateOf("drawable://fashion_editorial") }
    var pickedMediaUri by remember { mutableStateOf("") }
    var showValidationError by remember { mutableStateOf(false) }

    val visualPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedMediaUri = uri.toString()
            selectedCoverKey = uri.toString()
        }
    }

    val coverPresets = listOf(
        "Fashion" to "drawable://fashion_editorial",
        "Bridal" to "drawable://bridal_heritage",
        "Portrait" to "drawable://dramatic_portrait",
        "Commercial" to "drawable://commercial_luxury",
        "Cinema Stage" to "drawable://studio_hero"
    )
    val categories = listOf("Fashion", "Bridal", "Portrait", "Commercial", "Music Video", "Editorial")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .border(1.2.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                .testTag("add_project_dialog"),
            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.Collections,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "नया पोर्टफोलियो प्रोजेक्ट जोड़ें" else "Add Room Portfolio Project",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentText
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isHindi) "बंद करें" else "Close dialog",
                            tint = TextSecondary
                        )
                    }
                }

                // Photo vs Video Project Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mediaType == PortfolioProjectMediaType.PHOTO,
                        onClick = { mediaType = PortfolioProjectMediaType.PHOTO },
                        label = { Text(if (isHindi) "फोटोग्राफी" else "Photo Project", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = StudioElevatedSurface,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("add_project_media_photo")
                    )
                    FilterChip(
                        selected = mediaType == PortfolioProjectMediaType.VIDEO,
                        onClick = { mediaType = PortfolioProjectMediaType.VIDEO },
                        label = { Text(if (isHindi) "वीडियो प्रोजेक्ट" else "Video Project", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = StudioElevatedSurface,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("add_project_media_video")
                    )
                }

                // Device Media Picker + Preset Cover Thumbnails
                OutlinedButton(
                    onClick = {
                        visualPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    border = BorderStroke(1.dp, GoldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("add_project_pick_media_btn")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pickedMediaUri.isNotBlank()) {
                            if (isHindi) "मीडिया थंबनेल चुना गया" else "Custom Device Media Selected"
                        } else {
                            if (isHindi) "गैलरी से फोटो/वीडियो चुनें" else "Pick Photo or Video Thumbnail"
                        },
                        color = accentText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    coverPresets.forEach { (presetLabel, presetKey) ->
                        val isPresetSelected = selectedCoverKey == presetKey
                        FilterChip(
                            selected = isPresetSelected,
                            onClick = {
                                selectedCoverKey = presetKey
                                pickedMediaUri = ""
                            },
                            label = { Text(presetLabel, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldContainer,
                                selectedLabelColor = accentText,
                                containerColor = StudioElevatedSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isPresetSelected,
                                borderColor = if (isPresetSelected) GoldPrimary else StudioOutline
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        showValidationError = false
                    },
                    label = { Text(if (isHindi) "प्रोजेक्ट शीर्षक" else "Project Title") },
                    placeholder = { Text("e.g. Autumn Couture Editorial") },
                    singleLine = true,
                    isError = showValidationError && title.trim().length < 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_project_title_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldContainer,
                                selectedLabelColor = accentText,
                                containerColor = StudioElevatedSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) GoldPrimary else StudioOutline
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text(if (isHindi) "क्लाइंट / ब्रांड नाम" else "Client / Brand Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_project_client_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = cameraGear,
                    onValueChange = { cameraGear = it },
                    label = { Text(if (isHindi) "कैमरा और लेंस गियर" else "Camera & Lens Gear") },
                    placeholder = { Text("e.g. Sony FX3 • 35mm GM") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_project_gear_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                if (mediaType == PortfolioProjectMediaType.VIDEO) {
                    OutlinedTextField(
                        value = durationSecondsText,
                        onValueChange = { input ->
                            durationSecondsText = input.filter { it.isDigit() }.take(5)
                        },
                        label = { Text(if (isHindi) "अवधि (सेकंड में)" else "Video Duration (seconds)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_project_duration_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = shotCountText,
                        onValueChange = { input ->
                            shotCountText = input.filter { it.isDigit() }.take(4)
                        },
                        label = { Text(if (isHindi) "फ़ोटो संख्या (Shot Count)" else "Curated Shot Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_project_shots_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isHindi) "प्रोजेक्ट विवरण" else "Project Brief & Lighting Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                AnimatedVisibility(visible = showValidationError, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = if (isHindi) "कृपया मान्य प्रोजेक्ट शीर्षक दर्ज करें।" else "Please enter a project title (at least 2 characters).",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("add_project_cancel_btn")
                    ) {
                        Text(
                            text = if (isHindi) "रद्द करें" else "Cancel",
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.trim().length < 2) {
                                showValidationError = true
                            } else {
                                val isVideo = mediaType == PortfolioProjectMediaType.VIDEO
                                val durationSecs = durationSecondsText.toIntOrNull()?.coerceAtLeast(10) ?: 90
                                val shots = shotCountText.toIntOrNull()?.coerceAtLeast(1) ?: 24
                                val finalThumb = selectedCoverKey.ifBlank { "drawable://studio_hero" }
                                val finalMedia = pickedMediaUri.ifBlank { finalThumb }
                                onSave(
                                    title.trim(),
                                    mediaType,
                                    category,
                                    finalThumb,
                                    finalMedia,
                                    clientName.trim(),
                                    cameraGear.trim(),
                                    if (isVideo) durationSecs else 0,
                                    if (isVideo) 1 else shots,
                                    if (isVideo) "4K HDR" else "61MP RAW",
                                    description.trim()
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("add_project_save_btn")
                    ) {
                        Text(
                            text = if (isHindi) "रूम में सहेजें" else "Save Project",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
