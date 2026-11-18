package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.StudioProjectStatus
import com.example.data.local.entity.StudioSessionProjectEntity
import com.example.data.model.AppLanguage
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
internal fun statusAccentColor(status: StudioProjectStatus): Color = when (status) {
    StudioProjectStatus.PLANNING -> GoldPrimary
    StudioProjectStatus.IN_PRODUCTION -> Color(0xFF38BDF8)
    StudioProjectStatus.POST_PROCESSING -> Color(0xFFC084FC)
    StudioProjectStatus.COMPLETED -> SuccessGreen
}

@Composable
internal fun StatusKpiPill(
    label: String,
    count: Int,
    accent: Color,
    isSelected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .heightIn(min = 64.dp)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accent else StudioOutline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accent.copy(alpha = 0.14f) else StudioCardSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                color = accent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SessionProjectCard(
    project: StudioSessionProjectEntity,
    language: AppLanguage,
    currentTimeMillis: Long = System.currentTimeMillis(),
    onChangeStatus: (StudioProjectStatus) -> Unit,
    onAdvanceStage: () -> Unit,
    onEditDetails: () -> Unit,
    onDelete: () -> Unit,
    onToggleTimer: () -> Unit = {},
    onResetTimer: () -> Unit = {},
    onArchive: () -> Unit = {},
    onRestore: () -> Unit = {},
    onInspectProject: () -> Unit = {},
    onOpenMoodBoard: () -> Unit = {}
) {
    val accent = statusAccentColor(project.status)
    val isHindi = language == AppLanguage.HINDI
    val nextStage = project.status.nextStage()
    val elapsedMillis = project.computeTotalTrackedMillis(currentTimeMillis)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .testTag("project_tracker_card_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top badge row: Session Code + Service Category + Status Pill + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = project.sessionCode.ifBlank { "SAS-${project.id}" },
                            color = OnGoldContainer,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioElevatedSurface)
                            .border(1.dp, StudioOutline, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = project.serviceCategory,
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (project.isArchived) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(StudioElevatedSurface)
                                .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("project_tracker_archived_badge_${project.id}")
                        ) {
                            Text(
                                text = if (isHindi) "आर्काइव्ड" else "ARCHIVED",
                                color = GoldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(accent.copy(alpha = 0.16f))
                            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("project_tracker_status_badge_${project.id}")
                    ) {
                        Text(
                            text = if (isHindi) project.status.labelHi else project.status.labelEn,
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Tappable Title, Client & Stage section to inspect full Project Detail View
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onInspectProject)
                    .padding(vertical = 2.dp)
                    .testTag("project_tracker_open_detail_${project.id}"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.projectTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "विवरण ›" else "Details ›",
                        color = GoldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = project.clientName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                // Stage & Date metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = project.studioStage,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = project.scheduledDate.ifBlank { "TBD" },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }
                        if (project.deadlineDate.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (isHindi) "अंतिम: ${project.deadlineDate}" else "Due ${project.deadlineDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                        }
                    }
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "प्रगति व डिलीवरी (${project.deliverableCount} फ़ाइलें)" else "Workflow Progress • ${project.deliverableCount} deliverables",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${project.progressPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                        modifier = Modifier.testTag("project_tracker_progress_label_${project.id}")
                    )
                }
                LinearProgressIndicator(
                    progress = { (project.progressPercent.coerceIn(0, 100)) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = accent,
                    trackColor = StudioElevatedSurface
                )
            }

            if (project.status == StudioProjectStatus.IN_PRODUCTION) {
                InProductionStopwatchPanel(
                    projectId = project.id,
                    elapsedMillis = elapsedMillis,
                    isRunning = project.isTimerRunning,
                    isHindi = isHindi,
                    onToggleTimer = onToggleTimer,
                    onResetTimer = onResetTimer
                )
            } else if (project.trackedProductionMillis > 0L) {
                LoggedProductionTimeBadge(
                    projectId = project.id,
                    trackedMillis = project.trackedProductionMillis,
                    isHindi = isHindi
                )
            }

            if (project.productionNotes.isNotBlank()) {
                Text(
                    text = project.productionNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }

            if (project.inProductionTaskNotes.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF38BDF8).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("project_tracker_card_task_notes_${project.id}"),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isHindi) "प्रोडक्शन कार्य नोट्स" else "In-Production Task Notes",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = project.inProductionTaskNotes,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (project.status == StudioProjectStatus.IN_PRODUCTION) {
                Text(
                    text = if (isHindi)
                        "प्रोडक्शन कार्य नोट्स जोड़ने के लिए कार्ड पर टैप करें"
                    else
                        "Tap card to open Project Details & save 'In Production' task notes",
                    color = GoldPrimary.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Quick status switcher chips (hidden for archived historical reference items)
            if (!project.isArchived) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StudioProjectStatus.entries.forEach { targetStatus ->
                        val selected = project.status == targetStatus
                        val chipAccent = statusAccentColor(targetStatus)
                        FilterChip(
                            selected = selected,
                            onClick = { onChangeStatus(targetStatus) },
                            label = {
                                Text(
                                    text = if (isHindi) targetStatus.labelHi else targetStatus.labelEn,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipAccent.copy(alpha = 0.2f),
                                selectedLabelColor = chipAccent,
                                containerColor = StudioElevatedSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                selectedBorderColor = chipAccent,
                                borderColor = StudioOutline
                            ),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .testTag("project_tracker_status_chip_${project.id}_${targetStatus.name.lowercase()}")
                        )
                    }
                }
            }

            // Footer action buttons: Advance Stage / Restore / Archive, Mood Board, Edit Notes, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (project.isArchived) {
                    OutlinedButton(
                        onClick = onRestore,
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("project_tracker_restore_btn_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Unarchive,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "डैशबोर्ड पर वापस लाएं" else "Restore to Dashboard",
                            color = GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (nextStage != null) {
                    Button(
                        onClick = onAdvanceStage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF141000)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("project_tracker_advance_btn_${project.id}")
                    ) {
                        Text(
                            text = if (isHindi) "अगला चरण: ${nextStage.labelHi}" else "Move to ${nextStage.labelEn}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SuccessGreen.copy(alpha = 0.14f))
                            .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "पूर्ण व डिलीवर" else "Signed Off",
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onArchive,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioElevatedSurface)
                            .border(1.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                            .testTag("project_tracker_archive_btn_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = if (isHindi) "पूर्ण प्रोजेक्ट आर्काइव करें" else "Archive completed project",
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onOpenMoodBoard,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioElevatedSurface)
                        .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .testTag("project_tracker_moodboard_btn_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = if (isHindi) "प्रोजेक्ट मूड बोर्ड खोलें" else "Open project mood board",
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onEditDetails,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioElevatedSurface)
                        .border(1.dp, StudioOutline, RoundedCornerShape(10.dp))
                        .testTag("project_tracker_edit_notes_btn_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = if (isHindi) "नोट्स व प्रगति संपादित करें" else "Edit production notes",
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioElevatedSurface)
                        .border(1.dp, StudioOutline, RoundedCornerShape(10.dp))
                        .testTag("project_tracker_delete_btn_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = if (isHindi) "प्रोजेक्ट हटाएं" else "Delete session project",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun InProductionStopwatchPanel(
    projectId: Long,
    elapsedMillis: Long,
    isRunning: Boolean,
    isHindi: Boolean,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit
) {
    val activeBlue = Color(0xFF38BDF8)
    val panelBorderColor = if (isRunning) activeBlue else StudioOutline
    val panelBackground = if (isRunning) activeBlue.copy(alpha = 0.12f) else StudioElevatedSurface
    val formattedDuration = StudioSessionProjectEntity.formatDurationHhMmSs(elapsedMillis)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(panelBackground)
            .border(1.dp, panelBorderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("project_tracker_stopwatch_$projectId"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    tint = if (isRunning) activeBlue else GoldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isHindi) "प्रोडक्शन स्टॉपवॉच" else "PRODUCTION STOPWATCH",
                    color = if (isRunning) activeBlue else GoldLight,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isRunning) activeBlue.copy(alpha = 0.2f) else StudioCardSurface
                    )
                    .border(
                        1.dp,
                        if (isRunning) activeBlue.copy(alpha = 0.7f) else StudioOutline,
                        RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) SuccessGreen else TextSecondary)
                )
                Text(
                    text = if (isRunning) {
                        if (isHindi) "चालू" else "LIVE"
                    } else {
                        if (isHindi) "रुका हुआ" else "PAUSED"
                    },
                    color = if (isRunning) Color.White else TextSecondary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedDuration,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = FontWeight.ExtraBold,
                color = if (isRunning) activeBlue else GoldLight,
                modifier = Modifier.testTag("project_tracker_stopwatch_time_$projectId")
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onToggleTimer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) GoldPrimary else activeBlue,
                        contentColor = Color(0xFF0A0D12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("project_tracker_stopwatch_toggle_$projectId")
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRunning) {
                            if (isHindi) "रोकें" else "Pause"
                        } else if (elapsedMillis > 0L) {
                            if (isHindi) "जारी रखें" else "Resume"
                        } else {
                            if (isHindi) "शुरू करें" else "Start"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                IconButton(
                    onClick = onResetTimer,
                    enabled = elapsedMillis > 0L || isRunning,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioCardSurface)
                        .border(1.dp, StudioOutline, RoundedCornerShape(10.dp))
                        .testTag("project_tracker_stopwatch_reset_$projectId")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (isHindi) "स्टॉपवॉच रीसेट करें" else "Reset stopwatch",
                        tint = if (elapsedMillis > 0L || isRunning) GoldPrimary else TextSecondary.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun LoggedProductionTimeBadge(
    projectId: Long,
    trackedMillis: Long,
    isHindi: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioElevatedSurface)
            .border(1.dp, StudioOutline, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("project_tracker_logged_time_$projectId"),
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
                tint = GoldPrimary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = if (isHindi) "रिकॉर्डेड प्रोडक्शन समय" else "Logged Production Time",
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = StudioSessionProjectEntity.formatDurationHhMmSs(trackedMillis),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontFeatureSettings = "tnum"
            ),
            fontWeight = FontWeight.ExtraBold,
            color = GoldLight
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddSessionProjectDialog(
    language: AppLanguage,
    initialScheduledDate: String = "2026-11-28",
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        client: String,
        category: String,
        status: StudioProjectStatus,
        scheduledDate: String,
        deadlineDate: String,
        stage: String,
        notes: String
    ) -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    var title by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Photography") }
    var selectedStatus by remember { mutableStateOf(StudioProjectStatus.PLANNING) }
    var scheduledDate by remember(initialScheduledDate) {
        mutableStateOf(initialScheduledDate.ifBlank { "2026-11-28" })
    }
    var deadlineDate by remember { mutableStateOf("2026-11-30") }
    var stage by remember { mutableStateOf("Stage A • Cyclorama Bay") }
    var notes by remember { mutableStateOf("") }

    val categories = listOf("Photography", "Modeling", "Video Production", "Bridal", "Commercial")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .border(1.2.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                .testTag("project_tracker_add_dialog"),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "नया स्टूडियो प्रोजेक्ट जोड़ें" else "New Studio Session Project",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isHindi) "प्रोजेक्ट शीर्षक" else "Project Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_tracker_dialog_title_input")
                )

                OutlinedTextField(
                    value = client,
                    onValueChange = { client = it },
                    label = { Text(if (isHindi) "क्लाइंट / ब्रांड का नाम" else "Client / Brand Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_tracker_dialog_client_input")
                )

                Text(
                    text = if (isHindi) "श्रेणी" else "Service Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            modifier = Modifier.testTag("project_tracker_dialog_category_${cat.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                Text(
                    text = if (isHindi) "प्रारंभिक स्थिति" else "Initial Workflow Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StudioProjectStatus.entries.forEach { statusOption ->
                        FilterChip(
                            selected = selectedStatus == statusOption,
                            onClick = { selectedStatus = statusOption },
                            label = {
                                Text(
                                    text = if (isHindi) statusOption.labelHi else statusOption.labelEn,
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier.testTag("project_tracker_dialog_status_${statusOption.name.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = scheduledDate,
                    onValueChange = { scheduledDate = it },
                    label = { Text(if (isHindi) "शूट दिनांक (YYYY-MM-DD)" else "Shoot Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_tracker_dialog_date_input")
                )

                OutlinedTextField(
                    value = deadlineDate,
                    onValueChange = { deadlineDate = it },
                    label = { Text(if (isHindi) "डिलीवरी डेडलाइन (YYYY-MM-DD)" else "Delivery Deadline (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_tracker_dialog_deadline_input")
                )

                OutlinedTextField(
                    value = stage,
                    onValueChange = { stage = it },
                    label = { Text(if (isHindi) "स्टूडियो स्टेज / लोकेशन" else "Studio Stage / Bay") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isHindi) "प्रोडक्शन नोट्स" else "Production Notes") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_tracker_dialog_notes_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isHindi) "रद्द करें" else "Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(title, client, selectedCategory, selectedStatus, scheduledDate, deadlineDate, stage, notes)
                        },
                        enabled = title.isNotBlank() && client.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF141000)
                        ),
                        modifier = Modifier.testTag("project_tracker_dialog_save_btn")
                    ) {
                        Text(if (isHindi) "सहेजें" else "Save Project", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun EditSessionDetailsDialog(
    project: StudioSessionProjectEntity,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (
        notes: String,
        progressPercent: Int,
        studioStage: String,
        scheduledDate: String,
        deadlineDate: String
    ) -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    var notes by remember(project.id) { mutableStateOf(project.productionNotes) }
    var stage by remember(project.id) { mutableStateOf(project.studioStage) }
    var scheduledDate by remember(project.id) { mutableStateOf(project.scheduledDate) }
    var deadlineDate by remember(project.id) { mutableStateOf(project.deadlineDate) }
    var sliderProgress by remember(project.id) { mutableFloatStateOf(project.progressPercent.toFloat()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .border(1.2.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(20.dp)),
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
                Text(
                    text = project.projectTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${if (isHindi) "प्रगति" else "Completion"}: ${sliderProgress.roundToInt()}%",
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = sliderProgress,
                    onValueChange = { sliderProgress = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = GoldPrimary,
                        activeTrackColor = GoldPrimary,
                        inactiveTrackColor = GoldDark.copy(alpha = 0.3f)
                    )
                )
                OutlinedTextField(
                    value = scheduledDate,
                    onValueChange = { scheduledDate = it },
                    label = { Text(if (isHindi) "शूट दिनांक (YYYY-MM-DD)" else "Shoot Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_tracker_edit_date_input")
                )
                OutlinedTextField(
                    value = deadlineDate,
                    onValueChange = { deadlineDate = it },
                    label = { Text(if (isHindi) "डेडलाइन (YYYY-MM-DD)" else "Deadline Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_tracker_edit_deadline_input")
                )
                OutlinedTextField(
                    value = stage,
                    onValueChange = { stage = it },
                    label = { Text(if (isHindi) "स्टूडियो स्टेज" else "Studio Stage") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isHindi) "प्रोडक्शन नोट्स" else "Production Notes") },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_tracker_edit_notes_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, StudioOutline)
                    ) {
                        Text(if (isHindi) "बंद करें" else "Close", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                notes,
                                sliderProgress.roundToInt(),
                                stage,
                                scheduledDate,
                                deadlineDate
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF141000)
                        ),
                        modifier = Modifier.testTag("project_tracker_edit_save_btn")
                    ) {
                        Text(if (isHindi) "अपडेट करें" else "Update", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
