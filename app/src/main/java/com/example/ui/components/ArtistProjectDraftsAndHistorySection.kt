package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProjectDraftEntity
import com.example.data.local.entity.SessionHistoryEntity
import com.example.data.model.AppLanguage
import com.example.ui.theme.DarkStudioBlack
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated Room Database section presenting artist project drafts and local session history.
 */
@Composable
fun ArtistProjectDraftsAndHistorySection(
    language: AppLanguage,
    drafts: List<ProjectDraftEntity>,
    sessions: List<SessionHistoryEntity>,
    onSaveNewDraft: (
        title: String,
        projectType: String,
        clientOrModelName: String,
        conceptNotes: String,
        lightingSetupNotes: String,
        cameraGearNotes: String,
        targetShootDate: String,
        budget: Long
    ) -> Unit,
    onToggleFavorite: (ProjectDraftEntity) -> Unit,
    onDeleteDraft: (ProjectDraftEntity) -> Unit,
    onClearAllDrafts: () -> Unit,
    onClearAllSessions: () -> Unit,
    onNavigateToBooking: () -> Unit = {}
) {
    var subTab by remember { mutableIntStateOf(0) } // 0 = Project Drafts, 1 = Session History
    var showCreateDraftDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var selectedFilterType by remember { mutableStateOf("All") }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    val filteredDrafts = remember(drafts, selectedFilterType) {
        if (selectedFilterType == "All") drafts
        else if (selectedFilterType == "Favorites") drafts.filter { it.isFavorite }
        else drafts.filter { it.projectType.equals(selectedFilterType, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("artist_drafts_section")
    ) {
        // Sub-tabs: Project Drafts vs Session History
        TabRow(
            selectedTabIndex = subTab,
            containerColor = StudioElevatedSurface,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[subTab]),
                    color = GoldPrimary
                )
            }
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "प्रोजेक्ट ड्राफ्ट्स (${drafts.size})" else "Project Drafts (${drafts.size})",
                            fontWeight = FontWeight.Bold,
                            color = if (subTab == 0) GoldPrimary else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "सत्र इतिहास (${sessions.size})" else "Session History (${sessions.size})",
                            fontWeight = FontWeight.Bold,
                            color = if (subTab == 1) GoldPrimary else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            )
        }

        if (subTab == 0) {
            // =================================================================
            // TAB 0: ARTIST PROJECT DRAFTS
            // =================================================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Action Bar: New Draft Button + Clear All
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showCreateDraftDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color(0xFF141000)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("create_new_draft_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "नया प्रोजेक्ट ड्राफ्ट" else "New Project Draft",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }

                        if (drafts.isNotEmpty()) {
                            TextButton(
                                onClick = { showClearConfirmDialog = true },
                                modifier = Modifier.testTag("clear_all_drafts_btn")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "सभी हटाएं" else "Clear All",
                                    color = Color.Gray,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }

                // Filter Chips
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filterOptions = listOf("All", "Favorites", "Photography", "Modeling", "Video Reel", "Bridal")
                        items(filterOptions) { filter ->
                            val isSelected = selectedFilterType == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilterType = filter },
                                label = { Text(filter, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = Color(0xFF141000),
                                    containerColor = StudioElevatedSurface,
                                    labelColor = Color(0xFFC0C0C0)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) GoldPrimary else StudioOutline
                                )
                            )
                        }
                    }
                }

                if (filteredDrafts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, StudioOutline, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(StudioElevatedSurface)
                                        .border(1.dp, StudioOutline, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "कोई प्रोजेक्ट ड्राफ्ट नहीं मिला" else "No Project Drafts Saved",
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI)
                                        "स्थानीय रूप से लाइटिंग सेटअप, अवधारणाएं व गियर नोट्स सहेजने हेतु 'नया प्रोजेक्ट ड्राफ्ट' पर टैप करें।"
                                    else
                                        "Tap 'New Project Draft' to save local concepts, lighting setups, and camera gear plans.",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 11.5.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredDrafts, key = { it.id }) { draft ->
                        ProjectDraftCard(
                            draft = draft,
                            language = language,
                            dateFormat = dateFormat,
                            onToggleFavorite = { onToggleFavorite(draft) },
                            onDelete = { onDeleteDraft(draft) },
                            onBookShoot = onNavigateToBooking
                        )
                    }
                }
            }
        } else {
            // =================================================================
            // TAB 1: LOCAL SESSION HISTORY
            // =================================================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (sessions.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "स्टूडियो सत्र इतिहास" else "Local Session Activity Log",
                                style = MaterialTheme.typography.titleSmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = onClearAllSessions,
                                modifier = Modifier.testTag("clear_all_sessions_btn")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "लॉग साफ़ करें" else "Clear Log",
                                    color = Color.Gray,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }

                    items(sessions, key = { it.id }) { session ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, StudioOutline, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF231E12))
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (session.sessionType) {
                                            "AI_CONCEPT" -> Icons.Default.CameraAlt
                                            "STORYBOARD" -> Icons.Default.Movie
                                            "DRAFT_CREATION" -> Icons.Default.Description
                                            else -> Icons.Default.History
                                        },
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = session.sessionTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldLight,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = dateFormat.format(Date(session.timestamp)),
                                            color = Color.Gray,
                                            fontSize = 9.5.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = session.summaryDetails,
                                        color = Color(0xFFC0C0C0),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, StudioOutline, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "कोई सक्रिय सत्र इतिहास नहीं" else "No Session History Recorded",
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI)
                                        "जब आप ड्राफ्ट्स या एआई अवधारणाएं बनाएंगे, स्थानीय सत्र यहां दिखाई देंगे।"
                                    else
                                        "Local session activity appears here as you author drafts and generate storyboard concepts.",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 11.5.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create New Draft Dialog
    if (showCreateDraftDialog) {
        CreateDraftDialog(
            language = language,
            onDismiss = { showCreateDraftDialog = false },
            onConfirm = { title, type, client, concept, lighting, gear, date, budget ->
                onSaveNewDraft(title, type, client, concept, lighting, gear, date, budget)
                showCreateDraftDialog = false
            }
        )
    }

    // Clear Drafts Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = StudioCardSurface,
            title = {
                Text(
                    text = if (language == AppLanguage.HINDI) "सभी ड्राफ्ट्स हटाएं?" else "Clear All Drafts?",
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == AppLanguage.HINDI)
                        "क्या आप वाकई स्थानीय रूम डेटाबेस से सभी प्रोजेक्ट ड्राफ्ट्स स्थायी रूप से हटाना चाहते हैं?"
                    else
                        "Are you sure you want to permanently clear all project drafts from the local Room database?",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllDrafts()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (language == AppLanguage.HINDI) "हटाएं" else "Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel", color = Color.Gray)
                }
            }
        )
    }
}

/**
 * Individual Project Draft Card with Expandable Notes
 */
@Composable
fun ProjectDraftCard(
    draft: ProjectDraftEntity,
    language: AppLanguage,
    dateFormat: SimpleDateFormat,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onBookShoot: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    if (draft.isFavorite) GoldPrimary else StudioOutline
                ),
                RoundedCornerShape(14.dp)
            )
            .animateContentSize()
            .testTag("draft_card_${draft.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Type Badge + Favorite + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = draft.projectType.uppercase(),
                            color = GoldLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (draft.clientOrModelName.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${draft.clientOrModelName}",
                            color = Color(0xFFA5A5A5),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (draft.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = if (draft.isFavorite) GoldPrimary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Draft",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Draft Title
            Text(
                text = draft.draftTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GoldLight
            )

            if (draft.targetShootDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Target Date: ${draft.targetShootDate}",
                        color = Color(0xFFA5A5A5),
                        fontSize = 11.sp
                    )
                }
            }

            if (draft.conceptNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = draft.conceptNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD6D6D6),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    lineHeight = 16.sp
                )
            }

            // Expanded technical details (Lighting, Gear, Budget)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(StudioElevatedSurface, RoundedCornerShape(10.dp))
                        .border(0.5.dp, StudioOutline, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (draft.lightingSetupNotes.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Lighting Setup:", color = GoldLight, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                Text(draft.lightingSetupNotes, color = Color(0xFFE0E0E0), fontSize = 11.sp)
                            }
                        }
                    }

                    if (draft.cameraGearNotes.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Camera & Lens Plan:", color = GoldLight, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                Text(draft.cameraGearNotes, color = Color(0xFFE0E0E0), fontSize = 11.sp)
                            }
                        }
                    }

                    if (draft.estimatedBudgetInr > 0L) {
                        Text(
                            text = "Estimated Budget: ₹${draft.estimatedBudgetInr}",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = "Last Updated: ${dateFormat.format(Date(draft.updatedAt))}",
                        color = Color.Gray,
                        fontSize = 9.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Show Less" else "View Details",
                        color = GoldLight,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onBookShoot,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color(0xFF141000)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "शूट बुक करें" else "Book Shoot",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Dialog to author and save a new artist project draft
 */
@Composable
fun CreateDraftDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        type: String,
        client: String,
        concept: String,
        lighting: String,
        gear: String,
        date: String,
        budget: Long
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var projectType by remember { mutableStateOf("Photography") }
    var client by remember { mutableStateOf("") }
    var concept by remember { mutableStateOf("") }
    var lighting by remember { mutableStateOf("") }
    var gear by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var budgetStr by remember { mutableStateOf("") }

    val types = listOf("Photography", "Modeling", "Video Reel", "Bridal")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioCardSurface,
        title = {
            Text(
                text = if (language == AppLanguage.HINDI) "नया प्रोजेक्ट ड्राफ्ट बनाएं" else "Create Project Draft",
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Draft Title *") },
                    placeholder = { Text("e.g. Royal Velvet Pre-Wedding") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("draft_title_input"),
                    singleLine = true
                )

                // Project Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { t ->
                        val isSelected = projectType == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) GoldPrimary else StudioElevatedSurface)
                                .border(1.dp, if (isSelected) GoldLight else StudioOutline, RoundedCornerShape(6.dp))
                                .clickable { projectType = t }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t,
                                color = if (isSelected) Color(0xFF141000) else Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = client,
                    onValueChange = { client = it },
                    label = { Text("Client or Model Name") },
                    placeholder = { Text("e.g. Simran / Vogue Roster") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = concept,
                    onValueChange = { concept = it },
                    label = { Text("Creative Concept & Moodboard Notes") },
                    placeholder = { Text("Describe composition, styling, mood...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                OutlinedTextField(
                    value = lighting,
                    onValueChange = { lighting = it },
                    label = { Text("Lighting Bay Setup") },
                    placeholder = { Text("e.g. Key light 45 deg, gold reflector, rim flash") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = gear,
                    onValueChange = { gear = it },
                    label = { Text("Camera & Lens Rig") },
                    placeholder = { Text("e.g. 50mm f/1.2, 4K Gimbal, 85mm portrait") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Target Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = budgetStr,
                        onValueChange = { budgetStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Budget (₹)") },
                        placeholder = { Text("15000") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            title,
                            projectType,
                            client,
                            concept,
                            lighting,
                            gear,
                            date,
                            budgetStr.toLongOrNull() ?: 0L
                        )
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color(0xFF141000)
                ),
                modifier = Modifier.testTag("confirm_save_draft_button")
            ) {
                Text(if (language == AppLanguage.HINDI) "सहेजें" else "Save Draft")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel", color = Color.Gray)
            }
        }
    )
}
