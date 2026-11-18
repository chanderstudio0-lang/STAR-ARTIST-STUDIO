package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import com.example.ui.components.ArtistProjectDraftsAndHistorySection
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GenerationHistoryEntity
import com.example.data.model.AppLanguage
import com.example.ui.ai.AiStudioUiState
import com.example.ui.ai.AiStudioViewModel
import com.example.ui.components.GoldLoadingSpinner
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.util.ShareHelper
import com.example.util.StudioStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiStudioScreen(
    language: AppLanguage,
    aiViewModel: AiStudioViewModel,
    onNavigateToBooking: () -> Unit,
    onOpenMoodBoardArt: () -> Unit = {},
    onOpenMoodBoardWorkspace: () -> Unit = {}
) {
    val s = StudioStrings(language)
    val context = LocalContext.current
    val uiState by aiViewModel.uiState.collectAsState()
    val historyItems by aiViewModel.history.collectAsState()

    val drafts by aiViewModel.drafts.collectAsState()
    val sessions by aiViewModel.sessions.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Photo, 1 = Video, 2 = Project Drafts & Sessions, 3 = Generation History

    // Photo Concept Parameters
    var photoTheme by remember { mutableStateOf("Royal Black & Gold Couture") }
    var photoModelType by remember { mutableStateOf("Editorial High Fashion Female") }
    var photoSetting by remember { mutableStateOf("Studio Talwara Khurd (Dark Mood with Rim Light)") }
    var photoLighting by remember { mutableStateOf("Dramatic Chiaroscuro with Warm Gold Reflectors") }
    var photoNotes by remember { mutableStateOf("") }

    // Video Storyboard Parameters
    var videoGenre by remember { mutableStateOf("Cinematic Punjabi/Hindi Music Video") }
    var videoSongVibe by remember { mutableStateOf("High-Energy Bass & Royal Aesthetic") }
    var videoDurationSeconds by remember { mutableIntStateOf(30) }
    var videoPlatform by remember { mutableStateOf("4K Instagram Reel (9:16) & YouTube 4K") }
    var videoBrief by remember { mutableStateOf("") }

    val photoThemes = listOf(
        "Royal Black & Gold Couture",
        "Vintage Bollywood Royalty",
        "Cyberpunk Neon Fashion",
        "Minimalist Vogue Editorial",
        "Traditional Bridal Glamour"
    )

    val videoGenres = listOf(
        "Cinematic Punjabi/Hindi Music Video",
        "High-Fashion Brand Teaser",
        "Model Showreel & Ramp Motion",
        "Luxury Pre-Wedding Film",
        "Commercial Product Launch"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("ai_studio_screen")
    ) {
        // AI Sub-Tabs: Photo vs Video vs Generation History
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = StudioBlack,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = GoldPrimary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.tabPhotoAi, fontWeight = FontWeight.Bold, color = if (selectedTab == 0) GoldPrimary else Color.Gray)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.tabVideoAi, fontWeight = FontWeight.Bold, color = if (selectedTab == 1) GoldPrimary else Color.Gray)
                    }
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "ड्राफ्ट्स (${drafts.size})" else "Drafts (${drafts.size})",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 2) GoldPrimary else Color.Gray
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "इतिहास (${historyItems.size})" else "History (${historyItems.size})",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 3) GoldPrimary else Color.Gray
                        )
                    }
                }
            )
        }

        if (selectedTab == 2) {
            // =====================================================================
            // ARTIST PROJECT DRAFTS & SESSION HISTORY (ROOM DATABASE)
            // =====================================================================
            ArtistProjectDraftsAndHistorySection(
                language = language,
                drafts = drafts,
                sessions = sessions,
                onSaveNewDraft = { title, type, client, concept, lighting, gear, date, budget ->
                    aiViewModel.saveProjectDraft(title, type, client, concept, lighting, gear, date, budget)
                },
                onToggleFavorite = { aiViewModel.toggleFavoriteDraft(it) },
                onDeleteDraft = { aiViewModel.deleteProjectDraft(it) },
                onClearAllDrafts = { aiViewModel.clearAllProjectDrafts() },
                onClearAllSessions = { aiViewModel.clearAllSessions() },
                onNavigateToBooking = onNavigateToBooking
            )
        } else if (selectedTab == 3) {
            // =====================================================================
            // GENERATION HISTORY SCREEN (ROOM DATABASE)
            // =====================================================================
            GenerationHistorySection(
                language = language,
                historyItems = historyItems,
                onDelete = { aiViewModel.deleteHistoryItem(it) },
                onClearAll = { aiViewModel.clearAllHistory() }
            )
        } else {
            // =====================================================================
            // GENERATION FORM & OUTPUT VIEW
            // =====================================================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Info Card with Strict Text-Only Notice
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StudioOutline, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = s.aiStudioTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = s.aiStudioSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA0A0A0)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Notice: This produces an executive creative shooting guide and shot list designed by AI, not a rendered photo or video file.",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldLight.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onOpenMoodBoardArt,
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.75f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("ai_studio_open_moodboard_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI)
                                        "मूड बोर्ड कॉन्सेप्ट आर्ट बनाएं (Image AI)"
                                    else
                                        "Generate Mood Board Concept Art (Image AI)",
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onOpenMoodBoardWorkspace,
                                border = BorderStroke(1.dp, StudioOutline),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("ai_studio_open_moodboard_workspace_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DashboardCustomize,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI)
                                        "प्रोजेक्ट मूड बोर्ड ऑर्गनाइज़र खोलें"
                                    else
                                        "Open Project Mood Boards (Art + Ref Photos)",
                                    color = GoldLight,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }

                if (selectedTab == 0) {
                    // PHOTO CONCEPT GENERATOR
                    item {
                        Text(
                            text = if (language == AppLanguage.HINDI) "फोटोशूट थीम चुनें" else "Select Photoshoot Theme",
                            color = GoldLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(photoThemes) { theme ->
                                FilterChip(
                                    selected = photoTheme == theme,
                                    onClick = { photoTheme = theme },
                                    label = { Text(theme, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPrimary,
                                        selectedLabelColor = StudioBlack,
                                        containerColor = StudioElevatedSurface,
                                        labelColor = GoldLight
                                    )
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = photoModelType,
                            onValueChange = { photoModelType = it },
                            label = { Text(if (language == AppLanguage.HINDI) "मॉडल प्रोफाइल / शैली" else "Model Profile & Style", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().testTag("ai_photo_model_input")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = photoSetting,
                            onValueChange = { photoSetting = it },
                            label = { Text(if (language == AppLanguage.HINDI) "स्टूडियो सेटअप / पृष्ठभूमि" else "Studio Setting / Backdrop", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().testTag("ai_photo_setting_input")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = photoLighting,
                            onValueChange = { photoLighting = it },
                            label = { Text(if (language == AppLanguage.HINDI) "सिनेमैटिक लाइटिंग निर्देश" else "Cinematic Lighting Preference", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().testTag("ai_photo_lighting_input")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = photoNotes,
                            onValueChange = { photoNotes = it },
                            label = { Text(if (language == AppLanguage.HINDI) "अतिरिक्त क्लाइंट नोट्स (वैकल्पिक)" else "Additional Production Notes (Optional)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }

                    item {
                        Button(
                            onClick = {
                                aiViewModel.generatePhotoConcept(
                                    theme = photoTheme,
                                    modelType = photoModelType,
                                    setting = photoSetting,
                                    lightingStyle = photoLighting,
                                    additionalNotes = photoNotes
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("generate_photo_concept_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color(0xFF141000)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            enabled = uiState !is AiStudioUiState.Loading
                        ) {
                            if (uiState is AiStudioUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = StudioBlack,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Directing Concept...", color = StudioBlack, fontWeight = FontWeight.Bold)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "एआई फोटोशूट कॉन्सेप्ट बनाएं" else "Generate Photoshoot Concept",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // VIDEO STORYBOARD GENERATOR
                    item {
                        Text(
                            text = if (language == AppLanguage.HINDI) "वीडियो शैली चुनें" else "Select Video Genre",
                            color = GoldLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(videoGenres) { genre ->
                                FilterChip(
                                    selected = videoGenre == genre,
                                    onClick = { videoGenre = genre },
                                    label = { Text(genre, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPrimary,
                                        selectedLabelColor = StudioBlack,
                                        containerColor = StudioElevatedSurface,
                                        labelColor = GoldLight
                                    )
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = videoSongVibe,
                            onValueChange = { videoSongVibe = it },
                            label = { Text(if (language == AppLanguage.HINDI) "म्यूजिक वाइब / मूड" else "Song Vibe & Mood", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().testTag("ai_video_vibe_input")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = videoPlatform,
                            onValueChange = { videoPlatform = it },
                            label = { Text(if (language == AppLanguage.HINDI) "लक्षित प्लेटफॉर्म (उदा. इंस्टाग्राम 9:16, यूट्यूब 4K)" else "Target Platform", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().testTag("ai_video_platform_input")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = videoBrief,
                            onValueChange = { videoBrief = it },
                            label = { Text(if (language == AppLanguage.HINDI) "स्टोरीलाइन / थीम ब्रीफ" else "Storyline / Theme Brief", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Primary AI Generation Button (Gemini Free-Tier)
                            Button(
                                onClick = {
                                    aiViewModel.generateVideoStoryboard(
                                        videoGenre = videoGenre,
                                        songVibe = videoSongVibe,
                                        durationSeconds = videoDurationSeconds,
                                        targetPlatform = videoPlatform,
                                        creativeBrief = videoBrief
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("generate_video_storyboard_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = Color(0xFF141000)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                enabled = uiState !is AiStudioUiState.Loading
                            ) {
                                if (uiState is AiStudioUiState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = StudioBlack,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Writing Storyboard with Gemini...", color = StudioBlack, fontWeight = FontWeight.Bold)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "एआई वीडियो स्टोरीबोर्ड बनाएं (जेमिनी)" else "Generate 4K Video Storyboard (Gemini AI)",
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Secondary / Offline Cinema Template Button
                            OutlinedButton(
                                onClick = {
                                    aiViewModel.generateLocalVideoStoryboard(
                                        videoGenre = videoGenre,
                                        songVibe = videoSongVibe,
                                        durationSeconds = videoDurationSeconds,
                                        targetPlatform = videoPlatform,
                                        creativeBrief = videoBrief
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("generate_local_storyboard_button"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = GoldLight
                                ),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(14.dp),
                                enabled = uiState !is AiStudioUiState.Loading
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Movie, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "लोकल टेम्पलेट से बनाएं (ऑफ़लाइन फॉलबैक)" else "Use Cinema Template Engine (Offline Fallback)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GoldLight
                                    )
                                }
                            }
                        }
                    }
                }

                // AI OUTPUT / LOADING / ERROR DISPLAY
                item {
                    when (val state = uiState) {
                        is AiStudioUiState.Loading -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .testTag("ai_loading_card"),
                                colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    GoldLoadingSpinner(size = 40.dp)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = state.progressMessage,
                                        color = GoldLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Connecting to supported free-tier Gemini models • Configuring production cinematography...",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        is AiStudioUiState.Success -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (state.isOfflineFallback) Color(0xFFD4AF37) else GoldPrimary,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .testTag("ai_success_card"),
                                colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (state.category == "photo") "★ Photoshoot Concept Ready" else "★ 4K Video Storyboard Ready",
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldPrimary,
                                                    fontSize = 15.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                // Engine Badge
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (state.isOfflineFallback) Color(0xFF382A0F) else Color(0xFF143016))
                                                        .border(1.dp, if (state.isOfflineFallback) GoldPrimary else Color(0xFF81C784), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = state.engineBadge,
                                                        color = if (state.isOfflineFallback) GoldLight else Color(0xFFA5D6A7),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Saved to Generation History (Room Database & Firebase sync)",
                                                color = GoldLight.copy(alpha = 0.8f),
                                                fontSize = 10.sp
                                            )
                                        }

                                        Row {
                                            // Copy Button
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("Star Artist Studio Concept", state.generatedConcept)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "Copied concept to clipboard!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.testTag("copy_ai_concept_button")
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = GoldLight)
                                            }

                                            // Share Button
                                            IconButton(
                                                onClick = {
                                                    ShareHelper.shareAiConcept(
                                                        context = context,
                                                        title = state.title,
                                                        category = state.category,
                                                        conceptContent = state.generatedConcept
                                                    )
                                                },
                                                modifier = Modifier.testTag("share_ai_concept_button")
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "Share", tint = GoldPrimary)
                                            }

                                            // Reset Button
                                            IconButton(onClick = { aiViewModel.resetState() }) {
                                                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.Gray)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Clear disclaimer box with offline fallback / AI status
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (state.isOfflineFallback) Color(0xFF281F0E) else Color(0xFF241C10))
                                            .border(
                                                1.dp,
                                                if (state.isOfflineFallback) GoldPrimary.copy(alpha = 0.8f) else GoldDark.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = if (state.isOfflineFallback) {
                                                    "⚠️ OFFLINE CINEMA TEMPLATE (NON-AI FALLBACK) • Generated using Star Artist Studio's local template director. Zero live AI quota consumed."
                                                } else {
                                                    "✨ LIVE GEMINI 3.8 FLASH • AI-powered director's creative guide."
                                                },
                                                color = GoldPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp
                                            )
                                            Text(
                                                text = "Notice: Professional text-based production guide with scene-by-scene camera angles, lighting plots, and durations. No video file (MP4) is rendered.",
                                                color = GoldLight.copy(alpha = 0.9f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = state.generatedConcept,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE8E8E8),
                                        lineHeight = 22.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = onNavigateToBooking,
                                        modifier = Modifier.fillMaxWidth().testTag("book_shoot_concept_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GoldPrimary,
                                            contentColor = StudioBlack
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Book Studio Shoot with this Concept", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        is AiStudioUiState.Error -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(14.dp))
                                    .testTag("ai_error_card"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF261212)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "AI Service Notice",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = state.message,
                                        color = Color(0xFFFFCDD2),
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Action buttons: Retry and/or Offline Fallback
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (state.offerOfflineFallback) {
                                            Button(
                                                onClick = { aiViewModel.triggerLocalStoryboardFallback() },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("generate_offline_fallback_button"),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = GoldPrimary,
                                                    contentColor = StudioBlack
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Generate with Cinema Template Engine (Offline Fallback)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            if (state.canRetry) {
                                                OutlinedButton(
                                                    onClick = { aiViewModel.retryLastRequest() },
                                                    modifier = Modifier.weight(1f).testTag("retry_ai_button"),
                                                    border = BorderStroke(1.dp, GoldPrimary),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = GoldLight
                                                    ),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Retry AI", fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = { aiViewModel.resetState() },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Dismiss", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

/**
 * Generation History Screen backed by Room database.
 * Lists all previously generated photo concepts and video storyboards.
 */
@Composable
fun GenerationHistorySection(
    language: AppLanguage,
    historyItems: List<GenerationHistoryEntity>,
    onDelete: (GenerationHistoryEntity) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    var filterType by remember { mutableStateOf("all") } // "all", "photo", "video"
    var showClearConfirm by remember { mutableStateOf(false) }

    val filteredList = remember(historyItems, filterType) {
        when (filterType) {
            "photo" -> historyItems.filter { it.category == "photo" }
            "video" -> historyItems.filter { it.category == "video" }
            else -> historyItems
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = {
                Text(
                    text = if (language == AppLanguage.HINDI) "इतिहास साफ़ करें?" else "Clear Generation History?",
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == AppLanguage.HINDI)
                        "क्या आप वास्तव में अपने सभी सहेजे गए AI कॉन्सेप्ट को अपने डिवाइस से हटाना चाहते हैं?"
                    else
                        "Are you sure you want to delete all saved AI concepts from your device's local database?",
                    color = Color(0xFFD0D0D0)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = StudioElevatedSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("generation_history_list"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Controls Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = filterType == "all",
                        onClick = { filterType = "all" },
                        label = { Text("All (${historyItems.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = StudioBlack,
                            containerColor = StudioElevatedSurface,
                            labelColor = GoldLight
                        )
                    )
                    FilterChip(
                        selected = filterType == "photo",
                        onClick = { filterType = "photo" },
                        label = { Text("Photos", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = StudioBlack,
                            containerColor = StudioElevatedSurface,
                            labelColor = GoldLight
                        )
                    )
                    FilterChip(
                        selected = filterType == "video",
                        onClick = { filterType = "video" },
                        label = { Text("Videos", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = StudioBlack,
                            containerColor = StudioElevatedSurface,
                            labelColor = GoldLight
                        )
                    )
                }

                if (historyItems.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.testTag("clear_all_history_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = Color.Gray)
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .border(1.dp, StudioOutline, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(GoldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "कोई सहेजा गया इतिहास नहीं" else "No Generation History Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "अपनी पहली फोटोशूट अवधारणा या 4K स्टोरीबोर्ड तैयार करें। वे स्वचालित रूप से यहां दिखाई देंगे।"
                            else
                                "Generate your first photoshoot concept or 4K video storyboard. Saved ideas will persist here in your local database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAAAAAA),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { item ->
                HistoryItemCard(
                    item = item,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(item.title, item.content)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    onShare = {
                        ShareHelper.shareAiConcept(
                            context = context,
                            title = item.title,
                            category = item.category,
                            conceptContent = item.content
                        )
                    },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: GenerationHistoryEntity,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, Brush.verticalGradient(listOf(GoldPrimary.copy(alpha = 0.4f), StudioOutline))),
                RoundedCornerShape(14.dp)
            )
            .testTag("history_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Category Badge + Timestamp + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (item.category == "photo") GoldPrimary else Color(0xFF1E88E5))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.category == "photo") "PHOTO CONCEPT" else "4K STORYBOARD",
                            color = StudioBlack,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        color = Color(0xFF909090),
                        fontSize = 10.sp
                    )
                }

                Row {
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = GoldLight, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GoldLight
            )

            if (item.parametersSummary.isNotBlank()) {
                Text(
                    text = item.parametersSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Concept Content Preview / Expanded
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD5D5D5),
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (expanded) "Show Less ▲" else "Read Full Guide ▼",
                color = GoldPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp)
            )
        }
    }
}
