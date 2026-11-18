package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.MoodBoardConceptEntity
import com.example.data.model.AppLanguage
import com.example.ui.theme.ErrorRed
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
import com.example.ui.viewmodel.MoodBoardArtUiState
import java.io.File

@Composable
internal fun MoodBoardGenerationCanvasCard(
    uiState: MoodBoardArtUiState,
    language: AppLanguage,
    onSaveToBoard: () -> Unit,
    onCreateDraft: () -> Unit,
    onRemixConcept: (String) -> Unit,
    onRetry: () -> Unit,
    onReset: () -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    var remixInstruction by remember { mutableStateOf("") }

    when (uiState) {
        is MoodBoardArtUiState.Idle -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
                    .testTag("moodboard_idle_card"),
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
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                    Text(
                        text = if (isHindi)
                            "मूड बोर्ड कॉन्सेप्ट कैनवास तैयार है"
                        else
                            "Mood Board Concept Canvas Ready",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                    Text(
                        text = if (isHindi)
                            "थीम, लाइटिंग और विज़ुअल प्रॉम्प्ट चुनकर ऊपर 'मूड बोर्ड आर्ट बनाएं' पर टैप करें।"
                        else
                            "Describe a scene or tap an inspiration preset, then generate high-fashion concept art via Gemini Image AI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        is MoodBoardArtUiState.Loading -> {
            val infiniteTransition = rememberInfiniteTransition(label = "moodboard_shimmer")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.75f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_alpha"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, GoldPrimary.copy(alpha = pulseAlpha), RoundedCornerShape(16.dp))
                    .testTag("moodboard_loading_card"),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldPrimary.copy(alpha = pulseAlpha * 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = GoldPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                    Text(
                        text = uiState.statusMessage,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isHindi)
                            "रेंडरिंग में आमतौर पर 30-60 सेकंड लगते हैं..."
                        else
                            "AI image synthesis typically takes 30-60 seconds...",
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        is MoodBoardArtUiState.Success -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, GoldPrimary.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                    .testTag("moodboard_success_card"),
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.boardTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${uiState.projectType} • ${uiState.visualStyle} • ${uiState.modelUsed}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        IconButton(
                            onClick = onReset,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isHindi) "कैनवास रीसेट करें" else "Reset canvas",
                                tint = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(StudioElevatedSurface)
                            .border(1.dp, StudioOutline, RoundedCornerShape(14.dp))
                    ) {
                        Image(
                            bitmap = uiState.bitmap.asImageBitmap(),
                            contentDescription = "Generated mood board artwork for ${uiState.boardTitle}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("moodboard_generated_image")
                        )
                    }

                    // Action buttons: Save to Mood Board + Promote to Draft
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSaveToBoard,
                            enabled = !uiState.isSavedToBoard,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isSavedToBoard) SuccessGreen else GoldPrimary,
                                contentColor = Color(0xFF141000)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("moodboard_save_btn")
                        ) {
                            Icon(
                                imageVector = if (uiState.isSavedToBoard) Icons.Default.CheckCircle else Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    uiState.isSavedToBoard && isHindi -> "सहेजा गया"
                                    uiState.isSavedToBoard -> "Saved to Board"
                                    isHindi -> "मूड बोर्ड में सहेजें"
                                    else -> "Save to Board"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onCreateDraft,
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("moodboard_create_draft_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "प्रोजेक्ट ड्राफ्ट बनाएं" else "Create Draft",
                                color = GoldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Image-to-Image Remix bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = remixInstruction,
                            onValueChange = { remixInstruction = it },
                            placeholder = {
                                Text(
                                    text = if (isHindi)
                                        "रंग, लाइटिंग या पृष्ठभूमि बदलें..."
                                    else
                                        "Refine art (e.g., warmer gold rim light)...",
                                    fontSize = 12.sp
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = StudioOutline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("moodboard_refine_input")
                        )
                        Button(
                            onClick = {
                                val cmd = remixInstruction
                                remixInstruction = ""
                                onRemixConcept(cmd)
                            },
                            enabled = remixInstruction.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioElevatedSurface,
                                contentColor = GoldLight
                            ),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("moodboard_refine_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "रीमिक्स" else "Remix",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        is MoodBoardArtUiState.Error -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, ErrorRed, RoundedCornerShape(16.dp))
                    .testTag("moodboard_error_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261212)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ErrorRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "इमेज जनरेशन त्रुटि" else "Concept Generation Notice",
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = uiState.message,
                        color = Color(0xFFFFCDD2),
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.testTag("moodboard_error_text")
                    )
                    if (uiState.canRetry) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color(0xFF141000)
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("moodboard_retry_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHindi) "पुनः प्रयास करें" else "Retry Generation", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SavedMoodBoardCard(
    concept: MoodBoardConceptEntity,
    language: AppLanguage,
    onToggleFavorite: () -> Unit,
    onLoadForRemix: () -> Unit,
    onCreateDraft: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isHindi = language == AppLanguage.HINDI

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (concept.isFavorite) GoldPrimary else StudioOutline,
                RoundedCornerShape(16.dp)
            )
            .testTag("moodboard_saved_card_${concept.id}"),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = concept.projectType.uppercase(),
                            color = OnGoldContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = concept.aspectRatioLabel,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("moodboard_fav_btn_${concept.id}")
                ) {
                    Icon(
                        imageVector = if (concept.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Toggle favorite mood board",
                        tint = if (concept.isFavorite) GoldPrimary else TextSecondary
                    )
                }
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(concept.localImagePath))
                    .crossfade(true)
                    .build(),
                contentDescription = concept.boardTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioElevatedSurface)
            )

            Text(
                text = concept.boardTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GoldLight
            )

            Text(
                text = concept.prompt,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MoodTagChip(text = concept.visualStyle)
                MoodTagChip(text = concept.lightingSetup)
                MoodTagChip(text = concept.colorPaletteTags)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onLoadForRemix,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("moodboard_remix_saved_btn_${concept.id}")
                ) {
                    Icon(Icons.Default.Brush, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isHindi) "रीमिक्स" else "Remix",
                        color = GoldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onCreateDraft,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color(0xFF141000)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("moodboard_promote_draft_btn_${concept.id}")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (concept.linkedDraftId != null) {
                            if (isHindi) "ड्राफ्ट जुड़ा" else "New Draft"
                        } else {
                            if (isHindi) "ड्राफ्ट बनाएं" else "To Draft"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("moodboard_delete_btn_${concept.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete mood board artwork",
                        tint = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodTagChip(text: String) {
    if (text.isBlank()) return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StudioElevatedSurface)
            .border(1.dp, StudioOutline, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
