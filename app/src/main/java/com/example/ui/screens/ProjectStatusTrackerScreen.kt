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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.StudioProjectStatus
import com.example.data.local.entity.StudioSessionProjectEntity
import com.example.data.model.AppLanguage
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
import com.example.ui.viewmodel.ProjectStatusSummary
import com.example.ui.viewmodel.ProjectStatusViewModel
import com.example.ui.viewmodel.ProjectTrackerUiState
import com.example.ui.viewmodel.ProjectTrackerViewMode
import kotlinx.coroutines.delay

/**
 * Room-backed Session Production Tracker screen observing [ProjectStatusViewModel]
 * to manage studio sessions through Planning, In Production, Post-Processing, and Completed stages,
 * complete with a persistent stopwatch timer for active 'In Production' tasks.
 */
@Composable
fun ProjectStatusTrackerScreen(
    language: AppLanguage,
    viewModel: ProjectStatusViewModel,
    onBack: () -> Unit,
    onOpenMoodBoard: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val archivedProjects by viewModel.archivedProjects.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val showArchived = viewMode == ProjectTrackerViewMode.ARCHIVED
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusSummary by viewModel.statusSummary.collectAsStateWithLifecycle()

    var currentTimeMillis by remember { mutableLongStateOf(viewModel.clockMillis()) }
    LaunchedEffect(statusSummary.activeTimersCount, allProjects) {
        currentTimeMillis = viewModel.clockMillis()
        if (statusSummary.activeTimersCount > 0) {
            while (true) {
                delay(500L)
                currentTimeMillis = viewModel.clockMillis()
            }
        }
    }

    val displayedSourceProjects = if (showArchived) archivedProjects else allProjects
    val totalProductionTrackedMillis = displayedSourceProjects.sumOf {
        it.computeTotalTrackedMillis(currentTimeMillis)
    }

    ProjectStatusTrackerContent(
        language = language,
        uiState = uiState,
        selectedStatusFilter = selectedStatusFilter,
        viewMode = viewMode,
        showArchived = showArchived,
        searchQuery = searchQuery,
        statusSummary = statusSummary,
        currentTimeMillis = currentTimeMillis,
        totalProductionTrackedMillis = totalProductionTrackedMillis,
        onBack = onBack,
        onSelectStatusFilter = { viewModel.setStatusFilter(it) },
        onSelectViewMode = { viewModel.setViewMode(it) },
        onToggleShowArchived = { viewModel.setShowArchived(it) },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onCreateProject = { title, client, category, status, date, deadlineDate, stage, notes ->
            viewModel.createSessionProject(
                projectTitle = title,
                clientName = client,
                serviceCategory = category,
                status = status,
                scheduledDate = date,
                deadlineDate = deadlineDate,
                studioStage = stage,
                productionNotes = notes
            )
        },
        onUpdateStatus = { project, newStatus ->
            viewModel.updateProjectStatus(project, newStatus)
        },
        onAdvanceStage = { project ->
            viewModel.advanceProjectStage(project)
        },
        onUpdateDetails = { project, notes, progress, stage, scheduledDate, deadlineDate ->
            viewModel.updateProductionDetails(
                project = project,
                productionNotes = notes,
                progressPercent = progress,
                studioStage = stage,
                scheduledDate = scheduledDate,
                deadlineDate = deadlineDate
            )
        },
        onDeleteProject = { project ->
            viewModel.deleteSessionProject(project)
        },
        onToggleTimer = { project ->
            viewModel.toggleProductionTimer(project)
        },
        onResetTimer = { project ->
            viewModel.resetProductionTimer(project)
        },
        onArchiveProject = { project ->
            viewModel.archiveProject(project)
        },
        onRestoreProject = { project ->
            viewModel.restoreArchivedProject(project)
        },
        onArchiveAllCompleted = {
            viewModel.archiveAllCompleted()
        },
        onSaveInProductionNotes = { project, notes ->
            viewModel.saveInProductionTaskNotes(project, notes)
        },
        onOpenMoodBoard = onOpenMoodBoard
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectStatusTrackerContent(
    language: AppLanguage,
    uiState: ProjectTrackerUiState,
    selectedStatusFilter: StudioProjectStatus?,
    viewMode: ProjectTrackerViewMode = ProjectTrackerViewMode.BOARD,
    showArchived: Boolean = (viewMode == ProjectTrackerViewMode.ARCHIVED),
    searchQuery: String,
    statusSummary: ProjectStatusSummary,
    currentTimeMillis: Long = System.currentTimeMillis(),
    totalProductionTrackedMillis: Long = 0L,
    onBack: () -> Unit,
    onSelectStatusFilter: (StudioProjectStatus?) -> Unit,
    onSelectViewMode: (ProjectTrackerViewMode) -> Unit = {},
    onToggleShowArchived: (Boolean) -> Unit = { archived ->
        onSelectViewMode(if (archived) ProjectTrackerViewMode.ARCHIVED else ProjectTrackerViewMode.BOARD)
    },
    onSearchQueryChange: (String) -> Unit,
    onCreateProject: (
        title: String,
        client: String,
        category: String,
        status: StudioProjectStatus,
        scheduledDate: String,
        deadlineDate: String,
        stage: String,
        notes: String
    ) -> Unit,
    onUpdateStatus: (StudioSessionProjectEntity, StudioProjectStatus) -> Unit,
    onAdvanceStage: (StudioSessionProjectEntity) -> Unit,
    onUpdateDetails: (StudioSessionProjectEntity, String, Int, String, String, String) -> Unit,
    onDeleteProject: (StudioSessionProjectEntity) -> Unit,
    onToggleTimer: (StudioSessionProjectEntity) -> Unit = {},
    onResetTimer: (StudioSessionProjectEntity) -> Unit = {},
    onArchiveProject: (StudioSessionProjectEntity) -> Unit = {},
    onRestoreProject: (StudioSessionProjectEntity) -> Unit = {},
    onArchiveAllCompleted: () -> Unit = {},
    onSaveInProductionNotes: (StudioSessionProjectEntity, String) -> Unit = { _, _ -> },
    onOpenMoodBoard: (Long) -> Unit = {}
) {
    BackHandler { onBack() }

    val isHindi = language == AppLanguage.HINDI
    var showAddDialog by remember { mutableStateOf(false) }
    var initialAddScheduledDate by remember { mutableStateOf("2026-11-28") }
    var editingProject by remember { mutableStateOf<StudioSessionProjectEntity?>(null) }
    var inspectingProjectId by remember { mutableStateOf<Long?>(null) }

    val liveInspectingProject = remember(inspectingProjectId, uiState) {
        val targetId = inspectingProjectId ?: return@remember null
        (uiState as? ProjectTrackerUiState.Success)?.projects?.firstOrNull { it.id == targetId }
    }

    if (showAddDialog) {
        AddSessionProjectDialog(
            language = language,
            initialScheduledDate = initialAddScheduledDate,
            onDismiss = { showAddDialog = false },
            onSave = { title, client, category, status, date, deadlineDate, stage, notes ->
                onCreateProject(title, client, category, status, date, deadlineDate, stage, notes)
                showAddDialog = false
            }
        )
    }

    editingProject?.let { target ->
        EditSessionDetailsDialog(
            project = target,
            language = language,
            onDismiss = { editingProject = null },
            onSave = { notes, progress, stage, scheduledDate, deadlineDate ->
                onUpdateDetails(target, notes, progress, stage, scheduledDate, deadlineDate)
                editingProject = null
            }
        )
    }

    liveInspectingProject?.let { target ->
        ProjectDetailDialog(
            project = target,
            language = language,
            currentTimeMillis = currentTimeMillis,
            onDismiss = { inspectingProjectId = null },
            onSaveInProductionNotes = { notes -> onSaveInProductionNotes(target, notes) },
            onToggleTimer = { onToggleTimer(target) },
            onResetTimer = { onResetTimer(target) },
            onOpenEditStageDialog = {
                inspectingProjectId = null
                editingProject = target
            },
            onOpenMoodBoard = {
                inspectingProjectId = null
                onOpenMoodBoard(target.id)
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("project_tracker_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(StudioElevatedSurface)
                                    .testTag("project_tracker_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (isHindi) "वापस जाएं" else "Back",
                                    tint = GoldPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "सेशन प्रोजेक्ट ट्रैकर" else "Session Production Tracker",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldLight
                                )
                                Text(
                                    text = if (isHindi)
                                        "योजना • शूटिंग • पूर्ण (Room ऑफ़लाइन सिंक)"
                                    else
                                        "Planning • In Production • Completed (Room DB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                initialAddScheduledDate = "2026-11-28"
                                showAddDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color(0xFF141000)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("project_tracker_add_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "नया सेशन" else "New Session",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Pipeline completion bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi)
                                "कुल ${statusSummary.total} सक्रिय/पूर्ण प्रोजेक्ट"
                            else
                                "Pipeline Average (${statusSummary.total} sessions)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${statusSummary.averageCompletion}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPrimary,
                            modifier = Modifier.testTag("project_tracker_avg_completion")
                        )
                    }
                    LinearProgressIndicator(
                        progress = { statusSummary.averageCompletion.coerceIn(0, 100) / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .testTag("project_tracker_overall_progress"),
                        color = GoldPrimary,
                        trackColor = StudioElevatedSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioElevatedSurface)
                            .border(
                                1.dp,
                                if (statusSummary.activeTimersCount > 0) Color(0xFF38BDF8) else StudioOutline,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (statusSummary.activeTimersCount > 0) Color(0xFF38BDF8) else GoldPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isHindi) "कुल प्रोडक्शन समय" else "Total Production Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (statusSummary.activeTimersCount > 0) {
                                Text(
                                    text = if (isHindi) "${statusSummary.activeTimersCount} चालू" else "${statusSummary.activeTimersCount} active",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = StudioSessionProjectEntity.formatDurationHhMmSs(totalProductionTrackedMillis),
                                color = GoldLight,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.testTag("project_tracker_total_time_label")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isBoardSelected = viewMode == ProjectTrackerViewMode.BOARD
                        val isCalendarSelected = viewMode == ProjectTrackerViewMode.CALENDAR
                        val isArchivedSelected = viewMode == ProjectTrackerViewMode.ARCHIVED

                        FilterChip(
                            selected = isBoardSelected,
                            onClick = {
                                onSelectViewMode(ProjectTrackerViewMode.BOARD)
                                onToggleShowArchived(false)
                            },
                            label = {
                                Text(
                                    text = if (isHindi) "बोर्ड (${statusSummary.total})" else "Board (${statusSummary.total})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color(0xFF141000),
                                containerColor = StudioElevatedSurface,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .testTag("project_tracker_view_active_tab")
                        )

                        FilterChip(
                            selected = isCalendarSelected,
                            onClick = { onSelectViewMode(ProjectTrackerViewMode.CALENDAR) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isCalendarSelected) Color(0xFF141000) else GoldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (isHindi) "कैलेंडर" else "Calendar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color(0xFF141000),
                                containerColor = StudioElevatedSurface,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .testTag("project_tracker_view_calendar_tab")
                        )

                        FilterChip(
                            selected = isArchivedSelected,
                            onClick = {
                                onSelectViewMode(ProjectTrackerViewMode.ARCHIVED)
                                onToggleShowArchived(true)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = if (isArchivedSelected) Color(0xFF141000) else GoldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (isHindi) "आर्काइव (${statusSummary.archivedCount})" else "Archive (${statusSummary.archivedCount})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color(0xFF141000),
                                containerColor = StudioElevatedSurface,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .testTag("project_tracker_view_archived_tab")
                        )
                    }

                    if (viewMode == ProjectTrackerViewMode.BOARD && statusSummary.completedCount > 0) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = onArchiveAllCompleted,
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("project_tracker_archive_all_completed_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi)
                                    "सभी पूर्ण प्रोजेक्ट आर्काइव करें (${statusSummary.completedCount})"
                                else
                                    "Archive Completed Projects (${statusSummary.completedCount})",
                                color = GoldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4 KPI pills (shown on Active Dashboard)
        if (!showArchived) {
            item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusKpiPill(
                    label = if (isHindi) "योजना" else "Planning",
                    count = statusSummary.planningCount,
                    accent = statusAccentColor(StudioProjectStatus.PLANNING),
                    isSelected = selectedStatusFilter == StudioProjectStatus.PLANNING,
                    testTag = "project_tracker_kpi_planning",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSelectStatusFilter(
                            if (selectedStatusFilter == StudioProjectStatus.PLANNING) null else StudioProjectStatus.PLANNING
                        )
                    }
                )
                StatusKpiPill(
                    label = if (isHindi) "प्रोडक्शन" else "In Prod.",
                    count = statusSummary.inProductionCount,
                    accent = statusAccentColor(StudioProjectStatus.IN_PRODUCTION),
                    isSelected = selectedStatusFilter == StudioProjectStatus.IN_PRODUCTION,
                    testTag = "project_tracker_kpi_in_production",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSelectStatusFilter(
                            if (selectedStatusFilter == StudioProjectStatus.IN_PRODUCTION) null else StudioProjectStatus.IN_PRODUCTION
                        )
                    }
                )
                StatusKpiPill(
                    label = if (isHindi) "एडिटिंग" else "Post-Edit",
                    count = statusSummary.postProcessingCount,
                    accent = statusAccentColor(StudioProjectStatus.POST_PROCESSING),
                    isSelected = selectedStatusFilter == StudioProjectStatus.POST_PROCESSING,
                    testTag = "project_tracker_kpi_post_processing",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSelectStatusFilter(
                            if (selectedStatusFilter == StudioProjectStatus.POST_PROCESSING) null else StudioProjectStatus.POST_PROCESSING
                        )
                    }
                )
                StatusKpiPill(
                    label = if (isHindi) "पूर्ण" else "Completed",
                    count = statusSummary.completedCount,
                    accent = statusAccentColor(StudioProjectStatus.COMPLETED),
                    isSelected = selectedStatusFilter == StudioProjectStatus.COMPLETED,
                    testTag = "project_tracker_kpi_completed",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSelectStatusFilter(
                            if (selectedStatusFilter == StudioProjectStatus.COMPLETED) null else StudioProjectStatus.COMPLETED
                        )
                    }
                )
            }
        }
        }

        // Search & Filter chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = if (isHindi)
                                "प्रोजेक्ट, क्लाइंट, कोड या स्टेज खोजें..."
                            else
                                "Search session, client, code, or stage...",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search sessions",
                            tint = GoldPrimary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
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
                        focusedContainerColor = StudioCardSurface,
                        unfocusedContainerColor = StudioCardSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_tracker_search_input")
                )

                if (!showArchived) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedStatusFilter == null,
                            onClick = { onSelectStatusFilter(null) },
                            label = {
                                Text(
                                    text = if (isHindi) "सभी (${statusSummary.total})" else "All (${statusSummary.total})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldContainer,
                                selectedLabelColor = GoldPrimary,
                                containerColor = StudioElevatedSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedStatusFilter == null,
                                selectedBorderColor = GoldPrimary,
                                borderColor = StudioOutline
                            ),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("project_tracker_filter_all")
                        )

                        StudioProjectStatus.entries.forEach { status ->
                            val isSelected = selectedStatusFilter == status
                            val accent = statusAccentColor(status)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectStatusFilter(if (isSelected) null else status) },
                                label = {
                                    Text(
                                        text = if (isHindi) status.labelHi else status.labelEn,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent.copy(alpha = 0.2f),
                                    selectedLabelColor = accent,
                                    containerColor = StudioElevatedSurface,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = accent,
                                    borderColor = StudioOutline
                                ),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .testTag("project_tracker_filter_${status.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // State content
        when (uiState) {
            is ProjectTrackerUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GoldPrimary)
                    }
                }
            }
            is ProjectTrackerUiState.Error -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ErrorRed, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = StudioCardSurface)
                    ) {
                        Text(
                            text = uiState.message,
                            color = ErrorRed,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            is ProjectTrackerUiState.Success -> {
                if (viewMode == ProjectTrackerViewMode.CALENDAR) {
                    item(key = "project_schedule_calendar_item") {
                        ProjectScheduleCalendarView(
                            projects = uiState.projects,
                            language = language,
                            currentTimeMillis = currentTimeMillis,
                            onInspectProject = { project -> inspectingProjectId = project.id },
                            onScheduleOnDate = { dateIso ->
                                initialAddScheduledDate = dateIso
                                showAddDialog = true
                            }
                        )
                    }
                } else if (uiState.projects.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, StudioOutline), RoundedCornerShape(16.dp))
                                .testTag("project_tracker_empty_state"),
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
                                    imageVector = Icons.Default.AssignmentTurnedIn,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = if (showArchived) {
                                        if (isHindi) "आर्काइव में कोई प्रोजेक्ट नहीं है" else "No archived projects yet"
                                    } else {
                                        if (isHindi) "कोई मिलान प्रोजेक्ट नहीं मिला" else "No matching studio sessions"
                                    },
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (showArchived) {
                                        if (isHindi)
                                            "पूर्ण प्रोजेक्ट्स को आर्काइव करने के बाद वे ऐतिहासिक संदर्भ के लिए यहाँ दिखेंगे।"
                                        else
                                            "Archive completed shoots from your Active Board to store them here for historical reference."
                                    } else if (isHindi)
                                        "फ़िल्टर साफ़ करें या 'नया सेशन' जोड़ें।"
                                    else
                                        "Clear active filters or tap 'New Session' to add a studio project.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.projects, key = { it.id }) { project ->
                        SessionProjectCard(
                            project = project,
                            language = language,
                            currentTimeMillis = currentTimeMillis,
                            onChangeStatus = { newStatus -> onUpdateStatus(project, newStatus) },
                            onAdvanceStage = { onAdvanceStage(project) },
                            onEditDetails = { editingProject = project },
                            onDelete = { onDeleteProject(project) },
                            onToggleTimer = { onToggleTimer(project) },
                            onResetTimer = { onResetTimer(project) },
                            onArchive = { onArchiveProject(project) },
                            onRestore = { onRestoreProject(project) },
                            onInspectProject = { inspectingProjectId = project.id },
                            onOpenMoodBoard = { onOpenMoodBoard(project.id) }
                        )
                    }
                }
            }
        }
    }
}
