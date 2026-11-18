package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.ArtistBioEntity
import com.example.data.model.AppLanguage
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
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

/**
 * Normalizes and validates a portfolio link entered by an artist.
 * Returns null if blank, too long (> 300 chars), or uses an unsafe scheme.
 */
fun normalizePortfolioLink(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty() || trimmed.length > 300 || trimmed.any { it.isWhitespace() }) {
        return null
    }
    val lower = trimmed.lowercase()
    if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("file:")) {
        return null
    }
    if (trimmed.startsWith("@") && trimmed.length > 1) {
        val handle = trimmed.removePrefix("@")
        return if (handle.all { it.isLetterOrDigit() || it == '.' || it == '_' }) {
            "https://instagram.com/$handle"
        } else {
            null
        }
    }
    val candidate = if (lower.startsWith("http://") || lower.startsWith("https://")) {
        trimmed
    } else if (trimmed.contains(".")) {
        "https://$trimmed"
    } else {
        return null
    }
    val hostPart = candidate.substringAfter("://").substringBefore("/")
    return if (hostPart.contains(".") && !hostPart.startsWith(".") && !hostPart.endsWith(".")) {
        candidate
    } else {
        null
    }
}

/**
 * Identifies the creative platform from a normalized portfolio URL for visual badging.
 */
fun detectPortfolioPlatform(url: String): String {
    val lower = url.lowercase()
    return when {
        "instagram.com" in lower || "instagr.am" in lower -> "Instagram"
        "youtube.com" in lower || "youtu.be" in lower -> "YouTube"
        "behance.net" in lower -> "Behance"
        "drive.google.com" in lower -> "Google Drive"
        "vimeo.com" in lower -> "Vimeo"
        "linkedin.com" in lower -> "LinkedIn"
        else -> "Portfolio Web"
    }
}

/**
 * Computes artist profile completeness (0f..1f) across Identity, Contact Info, Portfolio Links, and Bio.
 */
fun computeArtistProfileCompleteness(
    displayName: String,
    avatarUrl: String,
    phoneNumber: String,
    city: String,
    instagramHandle: String,
    portfolioLinks: List<String>,
    specialty: String,
    bio: String
): Float {
    var completed = 0
    val total = 5
    if (displayName.trim().length >= 2) completed++
    if (phoneNumber.trim().length >= 8 && city.trim().isNotBlank()) completed++
    if (portfolioLinks.isNotEmpty() || instagramHandle.trim().isNotBlank()) completed++
    if (bio.trim().length >= 15) completed++
    if (avatarUrl.trim().isNotBlank() || specialty.trim().isNotBlank()) completed++
    return completed.toFloat() / total.toFloat()
}

val artistBioStarterTemplates = listOf(
    "Fashion Model" to "Editorial, runway, and commercial catalog model available for studio lookbooks and brand shoots in Sirsa & NCR.",
    "Photographer" to "High-fashion portrait, bridal, and commercial photographer specializing in 4K key-light studio setups.",
    "Videographer" to "4K music video director and commercial reel cinematographer crafting dynamic visual storytelling."
)

val quickPortfolioPlatformTemplates = listOf(
    "Instagram" to "https://instagram.com/",
    "YouTube Reel" to "https://youtube.com/@",
    "Behance" to "https://behance.net/",
    "Google Drive" to "https://drive.google.com/"
)

@Composable
fun ArtistProfileTopBar(
    isHindi: Boolean,
    isEmailVerified: Boolean,
    hasUnsavedChanges: Boolean,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
                    .testTag("profile_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isHindi) "वापस जाएं" else "Back",
                    tint = GoldPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = if (isHindi) "कलाकार प्रोफ़ाइल" else "Artist Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldPrimary
                )
                Text(
                    text = if (isHindi)
                        "संपर्क, पोर्टफोलियो लिंक व प्रोफ़ेशनल बायो"
                    else
                        "Contact info, portfolio links & bio",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        AssistChip(
            onClick = {},
            label = {
                Text(
                    text = when {
                        hasUnsavedChanges -> if (isHindi) "असहेजा" else "Unsaved"
                        isEmailVerified -> if (isHindi) "सत्यापित" else "Verified"
                        else -> if (isHindi) "सदस्य" else "Member"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = if (hasUnsavedChanges) GoldPrimary else if (isEmailVerified) SuccessGreen else GoldPrimary,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (isEmailVerified && !hasUnsavedChanges) SuccessGreen.copy(alpha = 0.14f) else GoldContainer,
                labelColor = if (isEmailVerified && !hasUnsavedChanges) SuccessGreen else OnGoldContainer
            ),
            border = BorderStroke(
                1.dp,
                if (isEmailVerified && !hasUnsavedChanges) SuccessGreen.copy(alpha = 0.55f) else GoldPrimary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun ArtistHeroIdentityCard(
    displayName: String,
    avatarUrl: String,
    specialty: String,
    emailText: String,
    city: String,
    phoneNumber: String,
    portfolioCount: Int,
    completeness: Float,
    modelsCount: Int,
    bookingsCount: Int,
    projectsCount: Int,
    favoritesCount: Int = 0,
    isHindi: Boolean,
    bio: String = "",
    onPickPhoto: () -> Unit
) {
    val context = LocalContext.current
    val completenessPercent = (completeness * 100).toInt().coerceIn(0, 100)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.2.dp,
                    Brush.horizontalGradient(listOf(GoldPrimary, StudioOutline, GoldPrimary.copy(alpha = 0.6f)))
                ),
                RoundedCornerShape(20.dp)
            )
            .testTag("artist_identity_hero_card"),
        colors = CardDefaults.elevatedCardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clickable(onClick = onPickPhoto)
                    .testTag("profile_avatar_preview"),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Artist Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, GoldPrimary, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(GoldPrimary, GoldDark)))
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(StudioBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.ifBlank { "S" }.take(1).uppercase(),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldPrimary
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(GoldPrimary)
                        .border(2.dp, StudioCardSurface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = if (isHindi) "अवतार बदलें" else "Change Avatar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = displayName.ifBlank { if (isHindi) "स्टार आर्टिस्ट" else "Star Artist" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (specialty.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldContainer,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = specialty,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnGoldContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = emailText,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (city.isNotBlank() || phoneNumber.isNotBlank() || portfolioCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                val summaryParts = buildList {
                    if (city.isNotBlank()) add(city.trim())
                    if (phoneNumber.isNotBlank()) add(phoneNumber.trim())
                    if (portfolioCount > 0) {
                        add(if (isHindi) "$portfolioCount लिंक" else "$portfolioCount Portfolio Links")
                    }
                }
                Text(
                    text = summaryParts.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = bio.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier = Modifier.testTag("hero_bio_preview")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Material 3 LinearProgressIndicator for Profile Completeness
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioElevatedSurface)
                    .border(1.dp, StudioOutline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "प्रोफ़ाइल पूर्णता" else "Artist Profile Completeness",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$completenessPercent%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (completenessPercent >= 100) SuccessGreen else GoldPrimary
                    )
                }
                LinearProgressIndicator(
                    progress = { completeness },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .testTag("profile_completeness_indicator"),
                    color = if (completenessPercent >= 100) SuccessGreen else GoldPrimary,
                    trackColor = StudioOutline.copy(alpha = 0.4f),
                    strokeCap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArtistMetricTile(
                    label = if (isHindi) "आवेदन" else "Applications",
                    value = "$modelsCount",
                    modifier = Modifier.weight(1f)
                )
                ArtistMetricTile(
                    label = if (isHindi) "बुकिंग्स" else "Bookings",
                    value = "$bookingsCount",
                    modifier = Modifier.weight(1f)
                )
                ArtistMetricTile(
                    label = if (isHindi) "पसंदीदा" else "Favorites",
                    value = "$favoritesCount",
                    modifier = Modifier.weight(1f)
                )
                ArtistMetricTile(
                    label = if (isHindi) "ड्राफ्ट्स" else "Drafts",
                    value = "$projectsCount",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ArtistMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(StudioElevatedSurface)
            .border(1.dp, StudioOutline, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = GoldPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistContactInfoCard(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    accountEmail: String,
    isEmailVerified: Boolean,
    isHindi: Boolean,
    displayNameError: String? = null,
    phoneError: String? = null
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
            .testTag("profile_contact_info_card"),
        colors = CardDefaults.elevatedCardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ContactPhone, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isHindi) "संपर्क और पहचान विवरण" else "Contact & Identity Info",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    Text(
                        text = if (isHindi)
                            "शूट समन्वय और कास्टिंग कॉल के लिए Firestore में सुरक्षित"
                        else
                            "Stored in Firestore for casting callbacks & shoot confirmations",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                label = { Text(if (isHindi) "प्रदर्शन नाम (Display Name) *" else "Artist Display Name *") },
                placeholder = { Text("e.g. Jasmeet Singh") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary)
                },
                isError = displayNameError != null,
                supportingText = displayNameError?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_display_name_input"),
                shape = RoundedCornerShape(12.dp),
                colors = artistProfileFieldColors()
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneChange,
                label = { Text(if (isHindi) "फ़ोन / व्हाट्सएप नंबर" else "Contact Phone / WhatsApp") },
                placeholder = { Text("+91 98128 90081") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = GoldPrimary)
                },
                isError = phoneError != null,
                supportingText = {
                    Text(
                        text = phoneError ?: if (isHindi)
                            "बुकिंग अपडेट और कॉल टाइम के लिए 8-30 अंकों का नंबर"
                        else
                            "8-30 digits for WhatsApp booking updates & call times",
                        fontSize = 11.sp,
                        color = if (phoneError != null) MaterialTheme.colorScheme.error else TextSecondary
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_phone_input"),
                shape = RoundedCornerShape(12.dp),
                colors = artistProfileFieldColors()
            )

            OutlinedTextField(
                value = city,
                onValueChange = onCityChange,
                label = { Text(if (isHindi) "शहर / स्थान" else "City / Studio Base") },
                placeholder = { Text("Sirsa, Haryana") },
                leadingIcon = {
                    Icon(Icons.Default.LocationCity, contentDescription = null, tint = GoldPrimary)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_city_input"),
                shape = RoundedCornerShape(12.dp),
                colors = artistProfileFieldColors()
            )

            // Read-only Verified Account Email Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = StudioCardSurface,
                border = BorderStroke(1.dp, StudioOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "पंजीकृत ईमेल (खाते से लिंक)" else "Account Email",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = accountEmail,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = if (isEmailVerified) "Verified" else "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isEmailVerified) SuccessGreen else GoldPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistPortfolioLinksCard(
    instagramHandle: String,
    onInstagramChange: (String) -> Unit,
    portfolioLinks: List<String>,
    newLinkDraft: String,
    onNewLinkDraftChange: (String) -> Unit,
    linkError: String?,
    onAddLink: () -> Unit,
    onRemoveLink: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    isHindi: Boolean
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
            .testTag("profile_portfolio_links_card"),
        colors = CardDefaults.elevatedCardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Icon(Icons.Default.CollectionsBookmark, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isHindi) "पोर्टफोलियो लिंक व सोशल मीडिया" else "Portfolio Links & Socials",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Text(
                            text = if (isHindi)
                                "इंस्टाग्राम, शोरिल, बिहैंस या ड्राइव लिंक जोड़ें"
                            else
                                "Instagram, YouTube showreels, Behance or Drive folders",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldContainer
                ) {
                    Text(
                        text = "${portfolioLinks.size}/10",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnGoldContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            OutlinedTextField(
                value = instagramHandle,
                onValueChange = onInstagramChange,
                label = { Text(if (isHindi) "प्राथमिक इंस्टाग्राम हैंडल" else "Primary Instagram Handle") },
                placeholder = { Text("@starartist_talent") },
                leadingIcon = {
                    Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = GoldPrimary)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_instagram_input"),
                shape = RoundedCornerShape(12.dp),
                colors = artistProfileFieldColors()
            )

            HorizontalDivider(color = StudioOutline.copy(alpha = 0.6f))

            Text(
                text = if (isHindi) "अतिरिक्त पोर्टफोलियो लिंक जोड़ें:" else "Add Portfolio & Reel Links:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            // Quick Platform Prefill AssistChips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPortfolioPlatformTemplates.forEach { (platformName, prefix) ->
                    AssistChip(
                        onClick = { onNewLinkDraftChange(prefix) },
                        label = { Text(platformName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (platformName.contains("YouTube")) Icons.Default.VideoLibrary else Icons.Default.Public,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = StudioCardSurface,
                            labelColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, StudioOutline)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = newLinkDraft,
                    onValueChange = onNewLinkDraftChange,
                    label = { Text(if (isHindi) "पोर्टफोलियो URL" else "Portfolio URL") },
                    placeholder = { Text("https://youtube.com/@artist") },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = GoldPrimary)
                    },
                    isError = linkError != null,
                    supportingText = linkError?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (portfolioLinks.size < 10 && newLinkDraft.isNotBlank()) onAddLink() }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_portfolio_url_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = artistProfileFieldColors()
                )

                FilledTonalButton(
                    onClick = onAddLink,
                    enabled = portfolioLinks.size < 10 && newLinkDraft.isNotBlank(),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .heightIn(min = 52.dp)
                        .testTag("profile_add_portfolio_link_button"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = GoldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddLink, contentDescription = if (isHindi) "लिंक जोड़ें" else "Add Link", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isHindi) "जोड़ें" else "Add", fontWeight = FontWeight.Bold)
                }
            }

            if (portfolioLinks.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = StudioCardSurface,
                    border = BorderStroke(1.dp, StudioOutline.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = if (isHindi)
                            "अभी कोई पोर्टफोलियो लिंक नहीं जोड़ा गया है। ऊपर अपना रील, बिहैंस या इंस्टाग्राम लिंक जोड़ें।"
                        else
                            "No portfolio links added yet. Add your showreel, Behance, Instagram, or Drive portfolio above so studio directors can review your work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    portfolioLinks.forEachIndexed { index, url ->
                        val platform = detectPortfolioPlatform(url)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_portfolio_link_item_$index"),
                            shape = RoundedCornerShape(12.dp),
                            color = StudioCardSurface,
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (platform == "YouTube" || platform == "Vimeo") Icons.Default.VideoLibrary else Icons.Default.Public,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = platform,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                    Text(
                                        text = url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onOpenLink(url) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("profile_open_portfolio_link_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = if (isHindi) "लिंक खोलें" else "Open Link",
                                        tint = GoldLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveLink(url) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("profile_remove_portfolio_link_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isHindi) "लिंक हटाएं" else "Remove Link",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Validates the artist bio text before persisting locally to Room.
 */
fun validateArtistBioForRoom(bioText: String): String? {
    val trimmed = bioText.trim()
    if (trimmed.isEmpty()) {
        return "Please enter your professional bio before saving."
    }
    if (trimmed.length > 500) {
        return "Artist bio must be 500 characters or less."
    }
    return null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArtistBioAndSpecialtyCard(
    specialty: String,
    specialtyOptions: List<String>,
    onSpecialtySelect: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    isHindi: Boolean,
    localArtistBio: ArtistBioEntity? = null,
    onSaveLocalBio: (specialty: String, bioText: String, onComplete: (Boolean, String) -> Unit) -> Unit = { _, _, onComplete ->
        onComplete(true, "Professional bio saved locally on your device!")
    }
) {
    var bioInlineError by remember { mutableStateOf<String?>(null) }
    var bioSaveBanner by remember { mutableStateOf<String?>(null) }

    val isSyncedWithLocalRoom = localArtistBio != null &&
        localArtistBio.bioText.isNotBlank() &&
        bio.trim() == localArtistBio.bioText.trim() &&
        specialty.trim() == localArtistBio.specialty.trim()

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
            .testTag("profile_bio_card"),
        colors = CardDefaults.elevatedCardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Icon(Icons.Default.Badge, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isHindi) "प्रोफ़ेशनल बायो और विशेषज्ञता" else "Professional Bio & Specialty",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Text(
                            text = if (isHindi)
                                "Cloud Firestore में सिंक और डिवाइस पर ऑफ़लाइन कैश"
                            else
                                "Synced to Cloud Firestore & cached offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                AssistChip(
                    onClick = {},
                    modifier = Modifier.testTag("profile_bio_room_status_chip"),
                    label = {
                        Text(
                            text = when {
                                isSyncedWithLocalRoom -> if (isHindi) "डिवाइस पर सहेजा" else "Saved Locally"
                                localArtistBio != null && localArtistBio.bioText.isNotBlank() ->
                                    if (isHindi) "संपादित ड्राफ्ट" else "Draft Unsaved"
                                else -> if (isHindi) "स्थानीय ड्राफ्ट" else "Local Ready"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isSyncedWithLocalRoom) Icons.Default.CheckCircle else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (isSyncedWithLocalRoom) SuccessGreen else GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSyncedWithLocalRoom) SuccessGreen.copy(alpha = 0.14f) else StudioCardSurface,
                        labelColor = if (isSyncedWithLocalRoom) SuccessGreen else GoldLight
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSyncedWithLocalRoom) SuccessGreen.copy(alpha = 0.5f) else GoldPrimary.copy(alpha = 0.45f)
                    )
                )
            }

            Text(
                text = if (isHindi) "कलाकार श्रेणी (Specialty)" else "Creative Specialty",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                specialtyOptions.forEach { option ->
                    val selected = specialty.equals(option, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            bioInlineError = null
                            bioSaveBanner = null
                            onSpecialtySelect(if (selected) "" else option)
                        },
                        label = { Text(option, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = StudioCardSurface,
                            labelColor = TextPrimary
                        )
                    )
                }
            }

            if (bio.isBlank()) {
                Text(
                    text = if (isHindi) "त्वरित बायो टेम्प्लेट:" else "Quick Bio Starters:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    artistBioStarterTemplates.forEach { (roleLabel, templateText) ->
                        AssistChip(
                            onClick = {
                                bioInlineError = null
                                bioSaveBanner = null
                                onBioChange(templateText)
                            },
                            label = { Text("+ $roleLabel", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = StudioCardSurface,
                                labelColor = GoldLight
                            ),
                            border = BorderStroke(1.dp, StudioOutline)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = bio,
                onValueChange = {
                    if (it.length <= 500) {
                        bioInlineError = null
                        bioSaveBanner = null
                        onBioChange(it)
                    }
                },
                label = { Text(if (isHindi) "प्रोफ़ेशनल बायो (Bio)" else "Professional Bio & Experience") },
                placeholder = {
                    Text(
                        if (isHindi)
                            "अपने अनुभव, रैंप वॉक, ब्रांड शूट या क्रिएटिव पोर्टफोलियो के बारे में लिखें..."
                        else
                            "Share your modeling credits, photography focus, gear familiarity, or production experience..."
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Description, contentDescription = null, tint = GoldPrimary)
                },
                isError = bioInlineError != null,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = bioInlineError ?: if (isHindi)
                                "डिवाइस पर ऑफ़लाइन सहेजा जाता है (Room DB)"
                            else
                                "Saved offline on device & ready for casting profiles",
                            color = if (bioInlineError != null) ErrorRed else TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${bio.length}/500",
                            color = if (bio.length >= 480) GoldPrimary else TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                },
                minLines = 4,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_bio_input"),
                shape = RoundedCornerShape(12.dp),
                colors = artistProfileFieldColors()
            )

            AnimatedVisibility(
                visible = bioSaveBanner != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SuccessGreen.copy(alpha = 0.14f))
                        .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("profile_bio_save_banner"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bioSaveBanner.orEmpty(),
                        color = SuccessGreen,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = bioInlineError != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ErrorRed.copy(alpha = 0.14f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("profile_bio_error_banner"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bioInlineError.orEmpty(),
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            FilledTonalButton(
                onClick = {
                    val validationError = validateArtistBioForRoom(bio)
                    if (validationError != null) {
                        bioInlineError = if (isHindi && validationError.contains("enter")) {
                            "सहेजने से पहले कृपया अपना प्रोफ़ेशनल बायो लिखें।"
                        } else if (isHindi) {
                            "बायो 500 अक्षरों से अधिक नहीं हो सकता।"
                        } else {
                            validationError
                        }
                        bioSaveBanner = null
                    } else {
                        bioInlineError = null
                        onSaveLocalBio(specialty.trim(), bio.trim()) { success, msg ->
                            if (success) {
                                bioSaveBanner = msg
                                bioInlineError = null
                            } else {
                                bioInlineError = msg
                                bioSaveBanner = null
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("profile_save_bio_button"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = GoldContainer,
                    contentColor = OnGoldContainer
                ),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHindi) "बायो स्थानीय रूप से सहेजें" else "Save Bio Locally",
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
            }
        }
    }
}

@Composable
fun ArtistAvatarPickerCard(
    avatarUrl: String,
    onAvatarUrlChange: (String) -> Unit,
    onLaunchPhotoPicker: () -> Unit,
    isHindi: Boolean
) {
    val context = LocalContext.current
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioOutline, RoundedCornerShape(16.dp)),
        colors = CardDefaults.outlinedCardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isHindi) "अवतार व प्रोफ़ाइल फ़ोटो" else "Artist Avatar & Photo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onLaunchPhotoPicker,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("profile_pick_avatar_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "गैलरी से चुनें" else "Choose Photo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (avatarUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onAvatarUrlChange("") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("profile_clear_avatar_button"),
                        border = BorderStroke(1.dp, StudioOutline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isHindi) "हटाएं" else "Clear",
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Text(
                text = if (isHindi) "या स्टूडियो अवतार प्रीसेट चुनें:" else "Or pick a Studio Preset:",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                studioAvatarPresets.forEachIndexed { index, presetUrl ->
                    val isSelected = avatarUrl == presetUrl
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) GoldPrimary else StudioOutline,
                                shape = CircleShape
                            )
                            .clickable { onAvatarUrlChange(presetUrl) }
                            .testTag("profile_avatar_preset_$index")
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(presetUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Studio Avatar Preset ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            OutlinedTextField(
                value = avatarUrl,
                onValueChange = onAvatarUrlChange,
                label = { Text(if (isHindi) "कस्टम अवतार URL" else "Avatar Image URL (Optional)") },
                placeholder = { Text("https://...") },
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null, tint = GoldPrimary)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_avatar_url_input"),
                shape = RoundedCornerShape(12.dp),
                colors = artistProfileFieldColors()
            )
        }
    }
}

@Composable
private fun artistProfileFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoldPrimary,
    unfocusedBorderColor = StudioOutline,
    focusedLabelColor = GoldPrimary,
    unfocusedLabelColor = TextSecondary,
    cursorColor = GoldPrimary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
