package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.util.StudioContactDetails

/**
 * Visual representation of the studio address using the interactive "Studio Gold" map overlay.
 * Users can interact with the vector map (pan, zoom, switch themes, inspect landmarks)
 * directly in the card, or launch the device's native map application with the exact
 * official studio location using the directions button.
 */
@Composable
fun StudioAddressMapCard(
    modifier: Modifier = Modifier,
    language: AppLanguage = AppLanguage.ENGLISH
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                BorderStroke(
                    1.2.dp,
                    Brush.verticalGradient(
                        listOf(
                            GoldPrimary.copy(alpha = 0.8f),
                            StudioOutline,
                            GoldPrimary.copy(alpha = 0.4f)
                        )
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .testTag("studio_address_map_card"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // =================================================================
            // 1. INTERACTIVE "STUDIO GOLD" MAP OVERLAY
            // =================================================================
            InteractiveStudioGoldMapOverlay(
                modifier = Modifier.fillMaxWidth(),
                language = language,
                initialExpanded = false
            )

            // =================================================================
            // 2. BOTTOM OFFICIAL STUDIO DETAILS & DEVICE MAP APPLICATION CTA
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GoldContainer)
                                .border(1.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.HINDI) "स्टूडियो स्थान विवरण" else "Official Studio Location",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                            Text(
                                text = "Talwara Khurd • Tehsil Ellenabad • Sirsa, Haryana",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFAAAAAA)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioElevatedSurface)
                            .border(0.8.dp, StudioOutline, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "PIN: 125102",
                            color = GoldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Full Strict Invariant Address
                Text(
                    text = StudioContactDetails.ADDRESS,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // CTA Button: Opens Device Map Application with studio location
                Button(
                    onClick = { StudioContactDetails.openStudioMaps(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_studio_map_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color(0xFF141000)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Navigate to Studio",
                        modifier = Modifier.size(17.dp),
                        tint = Color(0xFF141000)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "डिवाइस मैप्स में खोलें (Open in Maps App)"
                        else
                            "Open in Maps App (Get Directions)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF141000)
                    )
                }
            }
        }
    }
}
