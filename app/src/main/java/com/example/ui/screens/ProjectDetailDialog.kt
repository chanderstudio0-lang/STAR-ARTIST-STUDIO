package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Full Project Detail View showing session production metadata, live In-Production stopwatch,
 * and a persistent Room-backed text field allowing artists to save custom task notes for
 * 'In Production' sessions.
 */
@Composable
internal fun ProjectDetailDialog(
    project: StudioSessionProjectEntity,
    language: AppLanguage,
    currentTimeMillis: Long,
    onDismiss: () -> Unit,
    onSaveInProductionNotes: (String) -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onOpenEditStageDialog: () -> Unit,
    onOpenMoodBoard: () -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    val accent = statusAccentColor(project.status)
    val isInProduction = project.status == StudioProjectStatus.IN_PRODUCTION
    val activeBlue = Color(0xFF38BDF8)

    var taskNotesDraft by remember(project.id, project.inProductionTaskNotes) {
        mutableStateOf(project.inProductionTaskNotes)
    }
    val hasUnsavedChanges = taskNotesDraft.trim() != project.inProductionTaskNotes.trim()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 580.dp)
                .border(1.2.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .testTag("project_detail_dialog"),
            color = StudioCardSurface,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                                .clip(RoundedCornerShape(999.dp))
                                .background(accent.copy(alpha = 0.16f))
                                .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isHindi) project.status.labelHi else project.status.labelEn,
                                color = accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(StudioElevatedSurface)
                            .testTag("project_detail_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isHindi) "बंद करें" else "Close project details",
                            tint = TextSecondary
                        )
                    }
                }

                // Title & client/director overview
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = project.projectTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldLight
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
                        Text(
                            text = "${project.clientName} • ${project.leadDirector}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
                            Icon(Icons.Default.Movie, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Text(
                                text = project.studioStage,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(13.dp))
                                Text(
                                    text = project.scheduledDate.ifBlank { "TBD" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (project.deadlineDate.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.testTag("project_detail_deadline_label")
                                ) {
                                    Icon(
                                        Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isHindi) "अंतिम: ${project.deadlineDate}" else "Due ${project.deadlineDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFBBF24),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Progress strip
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isHindi)
                                "${project.serviceCategory} • ${project.deliverableCount} डिलीवरी फ़ाइलें"
                            else
                                "${project.serviceCategory} • ${project.deliverableCount} deliverables",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${project.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent
                        )
                    }
                    LinearProgressIndicator(
                        progress = { project.progressPercent.coerceIn(0, 100) / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = accent,
                        trackColor = StudioElevatedSurface
                    )
                }

                if (project.productionNotes.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StudioOutline, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = if (isHindi) "प्रोजेक्ट ब्रीफ़ नोट्स" else "Session Brief",
                                color = GoldPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = project.productionNotes,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Live stopwatch (or logged time badge) inside detail view
                if (isInProduction) {
                    InProductionStopwatchPanel(
                        projectId = project.id,
                        elapsedMillis = project.computeTotalTrackedMillis(currentTimeMillis),
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

                // Persistent Room Task Notes editor for 'In Production' tasks
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isInProduction) activeBlue.copy(alpha = 0.7f) else StudioOutline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .testTag("project_detail_in_production_notes_section"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isInProduction) {
                            activeBlue.copy(alpha = 0.08f)
                        } else {
                            StudioElevatedSurface
                        }
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
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
                                    imageVector = Icons.Default.NoteAlt,
                                    contentDescription = null,
                                    tint = if (isInProduction) activeBlue else GoldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isHindi)
                                        "'प्रोडक्शन में' कार्य नोट्स"
                                    else
                                        "'In Production' Task Notes",
                                    color = if (isInProduction) activeBlue else GoldLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            if (isInProduction) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (!hasUnsavedChanges && project.inProductionTaskNotes.isNotBlank()) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = when {
                                            hasUnsavedChanges -> if (isHindi) "असहेजे संपादन" else "Unsaved edits"
                                            project.inProductionTaskNotes.isNotBlank() ->
                                                if (isHindi) "Room में सहेजा गया" else "Saved in Room"
                                            else -> if (isHindi) "तैयार" else "Ready"
                                        },
                                        color = when {
                                            hasUnsavedChanges -> GoldPrimary
                                            project.inProductionTaskNotes.isNotBlank() -> SuccessGreen
                                            else -> TextSecondary
                                        },
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("project_detail_notes_sync_badge")
                                    )
                                }
                            }
                        }

                        if (isInProduction) {
                            OutlinedTextField(
                                value = taskNotesDraft,
                                onValueChange = { taskNotesDraft = it },
                                label = {
                                    Text(
                                        if (isHindi)
                                            "सेट लाइटिंग, लेंस, टेक व वार्डरोब नोट्स दर्ज करें"
                                        else
                                            "On-set lighting, lens, take & wardrobe notes"
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = if (isHindi)
                                            "उदा. 85mm f/1.4 पर 45° की-लाइट, लुक 2 अनुमोदित..."
                                        else
                                            "e.g., Switched key light to 85mm f/1.4, Look 2 color check passed...",
                                        fontSize = 12.sp
                                    )
                                },
                                minLines = 3,
                                maxLines = 6,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activeBlue,
                                    unfocusedBorderColor = StudioOutline,
                                    focusedContainerColor = StudioCardSurface,
                                    unfocusedContainerColor = StudioCardSurface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("project_detail_in_production_notes_input")
                            )

                            Button(
                                onClick = { onSaveInProductionNotes(taskNotesDraft) },
                                enabled = hasUnsavedChanges || taskNotesDraft.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = activeBlue,
                                    contentColor = Color(0xFF071018)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("project_detail_save_notes_btn")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "कार्य नोट्स सहेजें" else "Save Task Notes",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Text(
                                text = if (project.inProductionTaskNotes.isNotBlank()) {
                                    project.inProductionTaskNotes
                                } else if (isHindi) {
                                    "कस्टम कार्य नोट्स लिखने के लिए प्रोजेक्ट की स्थिति 'प्रोडक्शन में' चुनें।"
                                } else {
                                    "Switch session status to 'In Production' to record live on-set task notes in Room."
                                },
                                color = if (project.inProductionTaskNotes.isNotBlank()) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.testTag("project_detail_readonly_notes_text")
                            )
                        }
                    }
                }

                // Bottom action bar: Edit Stage/Brief & Open Mood Board
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenEditStageDialog,
                        border = BorderStroke(1.dp, StudioOutline),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("project_detail_edit_stage_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "स्टेज व प्रगति" else "Edit Progress",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onOpenMoodBoard,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF141000)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("project_detail_moodboard_btn")
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "मूड बोर्ड" else "Mood Board",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
