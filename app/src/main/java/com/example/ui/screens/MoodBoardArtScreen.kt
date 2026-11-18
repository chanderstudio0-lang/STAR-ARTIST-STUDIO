package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.FirebaseConceptArtGenerator
import com.example.data.local.entity.MoodBoardConceptEntity
import com.example.data.local.entity.StudioSessionProjectEntity
import com.example.data.model.AppLanguage
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MoodBoardArtUiState
import com.example.ui.viewmodel.MoodBoardViewModel

private data class MoodPreset(
    val title: String,
    val projectType: String,
    val style: String,
    val lighting: String,
    val palette: String,
    val prompt: String
)

private val StudioMoodPresets = listOf(
    MoodPreset(
        title = "Royal Black & Gold Couture",
        projectType = "Photography",
        style = "Moody Editorial",
        lighting = "Chiaroscuro Rim Light",
        palette = "Obsidian Black, Burnished Gold",
        prompt = "High-fashion model in embroidered black velvet silhouette against dark cyclorama backdrop with warm gold architectural key lighting and crisp rim highlights"
    ),
    MoodPreset(
        title = "Bridal Heritage Courtyard",
        projectType = "Bridal",
        style = "Royal Heritage",
        lighting = "Golden Hour Warm",
        palette = "Crimson, Antique Gold, Ivory",
        prompt = "Regal Indian bridal fashion concept inside carved sandstone palace courtyard bathed in late-afternoon sunbeams and floating marigold petals"
    ),
    MoodPreset(
        title = "Neon Cinema Music Stage",
        projectType = "Video Production",
        style = "Cyberpunk Neon",
        lighting = "Anamorphic Haze Backlight",
        palette = "Amber Gold, Electric Teal, Charcoal",
        prompt = "Wide anamorphic cinema frame of a music video stage inside atmospheric haze, rain-slicked obsidian floor reflecting dramatic amber and teal spotlights"
    ),
    MoodPreset(
        title = "Luxury Jewellery Tabletop",
        projectType = "Commercial",
        style = "Minimalist Studio",
        lighting = "Softbox Specular",
        palette = "Champagne Gold, Smoked Slate",
        prompt = "Macro commercial tabletop mood board featuring sculpted dark marble plinths, warm specular reflections, and soft dramatic studio shadows"
    )
)

/**
 * Mood Board Concept Art screen powered by Firebase AI Logic image generation and Room persistence.
 */
@Composable
fun MoodBoardArtScreen(
    language: AppLanguage,
    viewModel: MoodBoardViewModel,
    onBack: () -> Unit,
    ongoingProjects: List<StudioSessionProjectEntity> = emptyList(),
    onOpenMoodBoardWorkspace: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedConcepts by viewModel.filteredConcepts.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val targetProjectId by viewModel.targetSessionProjectId.collectAsStateWithLifecycle()

    MoodBoardArtContent(
        language = language,
        uiState = uiState,
        savedConcepts = savedConcepts,
        selectedFilter = selectedFilter,
        ongoingProjects = ongoingProjects,
        selectedProjectId = targetProjectId,
        onSelectOngoingProject = { viewModel.setTargetSessionProject(it) },
        onOpenMoodBoardWorkspace = onOpenMoodBoardWorkspace,
        onBack = onBack,
        onFilterChange = { viewModel.setFilter(it) },
        onGenerateConcept = { title, type, prompt, style, lighting, aspect, palette, model ->
            viewModel.generateConceptArt(
                boardTitle = title,
                projectType = type,
                prompt = prompt,
                visualStyle = style,
                lightingSetup = lighting,
                aspectRatioLabel = aspect,
                colorPaletteTags = palette,
                modelName = model
            )
        },
        onSaveToBoard = { viewModel.saveCurrentArtworkToMoodBoard() },
        onCreateDraftFromCurrent = { viewModel.createProjectDraftFromCurrentArt() },
        onRemixCurrent = { instruction -> viewModel.remixCurrentConcept(instruction) },
        onRetry = { viewModel.retryLastAction() },
        onReset = { viewModel.resetCanvas() },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onLoadForRemix = { viewModel.loadConceptIntoCanvasForRemix(it) },
        onPromoteSavedToDraft = { viewModel.createProjectDraftFromSavedConcept(it) },
        onDeleteConcept = { viewModel.deleteConcept(it) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodBoardArtContent(
    language: AppLanguage,
    uiState: MoodBoardArtUiState,
    savedConcepts: List<MoodBoardConceptEntity>,
    selectedFilter: String,
    onBack: () -> Unit,
    onFilterChange: (String) -> Unit,
    onGenerateConcept: (
        title: String,
        projectType: String,
        prompt: String,
        visualStyle: String,
        lightingSetup: String,
        aspectRatioLabel: String,
        colorPaletteTags: String,
        modelName: String
    ) -> Unit,
    onSaveToBoard: () -> Unit,
    onCreateDraftFromCurrent: () -> Unit,
    onRemixCurrent: (String) -> Unit,
    onRetry: () -> Unit,
    onReset: () -> Unit,
    onToggleFavorite: (MoodBoardConceptEntity) -> Unit,
    onLoadForRemix: (MoodBoardConceptEntity) -> Unit,
    onPromoteSavedToDraft: (MoodBoardConceptEntity) -> Unit,
    onDeleteConcept: (MoodBoardConceptEntity) -> Unit,
    ongoingProjects: List<StudioSessionProjectEntity> = emptyList(),
    selectedProjectId: Long? = null,
    onSelectOngoingProject: (Long?) -> Unit = {},
    onOpenMoodBoardWorkspace: () -> Unit = {}
) {
    BackHandler { onBack() }

    val isHindi = language == AppLanguage.HINDI
    var boardTitle by remember { mutableStateOf(StudioMoodPresets.first().title) }
    var projectType by remember { mutableStateOf(StudioMoodPresets.first().projectType) }
    var visualStyle by remember { mutableStateOf(StudioMoodPresets.first().style) }
    var lightingSetup by remember { mutableStateOf(StudioMoodPresets.first().lighting) }
    var colorPalette by remember { mutableStateOf(StudioMoodPresets.first().palette) }
    var aspectLabel by remember { mutableStateOf("1:1 Lookbook") }
    var promptText by remember { mutableStateOf(StudioMoodPresets.first().prompt) }
    var selectedModel by remember { mutableStateOf(FirebaseConceptArtGenerator.MODEL_NANO_BANANA_FAST) }

    val projectTypes = listOf("Photography", "Modeling", "Video Production", "Bridal", "Commercial")
    val visualStyles = listOf("Moody Editorial", "Royal Heritage", "Cyberpunk Neon", "Minimalist Studio", "Vintage Film")
    val aspectOptions = listOf("1:1 Lookbook", "3:4 Editorial", "16:9 Cinema", "9:16 Reel")
    val filterOptions = listOf("All", "Favorites", "Photography", "Modeling", "Video Production", "Bridal", "Commercial")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("moodboard_art_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Card
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
                            .testTag("moodboard_back_btn")
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
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "मूड बोर्ड कॉन्सेप्ट आर्ट" else "Project Mood Board Art",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldLight
                            )
                        }
                        Text(
                            text = if (isHindi)
                                "Firebase AI Image Gen • नए प्रोजेक्ट के लिए विज़ुअल कॉन्सेप्ट"
                            else
                                "Gemini Image AI • Generate creative concept art & save to boards",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenMoodBoardWorkspace,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("moodboard_open_workspace_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DashboardCustomize,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHindi) "बोर्ड" else "Board",
                            color = GoldLight,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Composer Card
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
                    if (ongoingProjects.isNotEmpty()) {
                        Text(
                            text = if (isHindi) "चल रहे प्रोजेक्ट से जोड़ें (वैकल्पिक)" else "Assign to Ongoing Project (Optional)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedProjectId == null,
                                onClick = { onSelectOngoingProject(null) },
                                label = { Text("General", fontSize = 11.sp) }
                            )
                            ongoingProjects.take(5).forEach { proj ->
                                FilterChip(
                                    selected = selectedProjectId == proj.id,
                                    onClick = { onSelectOngoingProject(proj.id) },
                                    label = { Text(proj.projectTitle, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("moodboard_gen_assign_proj_${proj.id}")
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isHindi) "त्वरित स्टूडियो प्रीसेट" else "Studio Art Direction Presets",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StudioMoodPresets.forEachIndexed { idx, preset ->
                            FilterChip(
                                selected = boardTitle == preset.title,
                                onClick = {
                                    boardTitle = preset.title
                                    projectType = preset.projectType
                                    visualStyle = preset.style
                                    lightingSetup = preset.lighting
                                    colorPalette = preset.palette
                                    promptText = preset.prompt
                                },
                                label = { Text(preset.title, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldContainer,
                                    selectedLabelColor = GoldPrimary,
                                    containerColor = StudioElevatedSurface,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("moodboard_preset_$idx")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = boardTitle,
                        onValueChange = { boardTitle = it },
                        label = { Text(if (isHindi) "मूड बोर्ड शीर्षक" else "Mood Board Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = StudioOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("moodboard_title_input")
                    )

                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text(if (isHindi) "क्रिएटिव कॉन्सेप्ट प्रॉम्प्ट" else "Creative Visual Concept Prompt") },
                        minLines = 3,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = StudioOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("moodboard_prompt_input")
                    )

                    Text(
                        text = if (isHindi) "प्रोजेक्ट श्रेणी व स्टाइल" else "Project Category & Visual Style",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        projectTypes.forEach { cat ->
                            FilterChip(
                                selected = projectType == cat,
                                onClick = { projectType = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = Color(0xFF141000)
                                )
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visualStyles.forEach { style ->
                            FilterChip(
                                selected = visualStyle == style,
                                onClick = { visualStyle = style },
                                label = { Text(style, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text(
                        text = if (isHindi) "फ्रेम अनुपात व मॉडल" else "Framing & AI Image Model",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        aspectOptions.forEach { ratio ->
                            FilterChip(
                                selected = aspectLabel == ratio,
                                onClick = { aspectLabel = ratio },
                                label = { Text(ratio, fontSize = 11.sp) }
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedModel == FirebaseConceptArtGenerator.MODEL_NANO_BANANA_FAST,
                            onClick = { selectedModel = FirebaseConceptArtGenerator.MODEL_NANO_BANANA_FAST },
                            label = { Text("Nano Banana 2.1 (Fast)", fontSize = 11.sp) },
                            modifier = Modifier.testTag("moodboard_model_fast")
                        )
                        FilterChip(
                            selected = selectedModel == FirebaseConceptArtGenerator.MODEL_NANO_BANANA_PRO,
                            onClick = { selectedModel = FirebaseConceptArtGenerator.MODEL_NANO_BANANA_PRO },
                            label = { Text("Gemini 3 Pro Image", fontSize = 11.sp) },
                            modifier = Modifier.testTag("moodboard_model_pro")
                        )
                    }

                    Button(
                        onClick = {
                            onGenerateConcept(
                                boardTitle,
                                projectType,
                                promptText,
                                visualStyle,
                                lightingSetup,
                                aspectLabel,
                                colorPalette,
                                selectedModel
                            )
                        },
                        enabled = uiState !is MoodBoardArtUiState.Loading && promptText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF141000)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("moodboard_generate_btn")
                    ) {
                        if (uiState is MoodBoardArtUiState.Loading) {
                            CircularProgressIndicator(
                                color = Color(0xFF141000),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "आर्टवर्क तैयार हो रहा है..." else "Generating Concept Art...",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "मूड बोर्ड आर्ट बनाएं" else "Generate Mood Board Art",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // Live Generation Canvas / Loading / Error
        item {
            MoodBoardGenerationCanvasCard(
                uiState = uiState,
                language = language,
                onSaveToBoard = onSaveToBoard,
                onCreateDraft = onCreateDraftFromCurrent,
                onRemixConcept = onRemixCurrent,
                onRetry = onRetry,
                onReset = onReset
            )
        }

        // Saved Mood Boards Gallery Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isHindi)
                        "सहेजे गए प्रोजेक्ट मूड बोर्ड (${savedConcepts.size})"
                    else
                        "Saved Project Mood Boards (${savedConcepts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterOptions.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(filter, fontSize = 11.5.sp) },
                            modifier = Modifier.testTag("moodboard_filter_${filter.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        if (savedConcepts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioOutline, RoundedCornerShape(14.dp))
                        .testTag("moodboard_empty_archive"),
                    colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isHindi)
                            "अभी कोई मूड बोर्ड आर्ट सहेजा नहीं गया है।"
                        else
                            "No saved mood board artworks yet. Generate a concept above and tap 'Save to Board'.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(savedConcepts, key = { it.id }) { concept ->
                SavedMoodBoardCard(
                    concept = concept,
                    language = language,
                    onToggleFavorite = { onToggleFavorite(concept) },
                    onLoadForRemix = { onLoadForRemix(concept) },
                    onCreateDraft = { onPromoteSavedToDraft(concept) },
                    onDelete = { onDeleteConcept(concept) }
                )
            }
        }
    }
}
