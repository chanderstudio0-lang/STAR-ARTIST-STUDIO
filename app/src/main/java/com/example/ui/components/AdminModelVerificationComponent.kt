package com.example.ui.components

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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.ModelRegistration
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldContainer
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

fun isPendingModelStatus(status: String): Boolean {
    val clean = status.trim()
    return clean.isEmpty() || clean.equals("pending", ignoreCase = true)
}

fun isRosterApprovedModelStatus(status: String): Boolean {
    return status.trim().equals("approved", ignoreCase = true)
}

fun isRejectedModelStatus(status: String): Boolean {
    return status.trim().equals("rejected", ignoreCase = true)
}

/**
 * Filters model registrations by verification lifecycle bucket ("PENDING", "APPROVED",
 * "REJECTED", or "ALL") and search text across name, city, phone, skills, portfolio URL, and notes.
 */
fun filterAdminModelApplications(
    models: List<ModelRegistration>,
    sectionFilter: String,
    searchQuery: String = ""
): List<ModelRegistration> {
    val statusFiltered = when (sectionFilter.uppercase()) {
        "PENDING" -> models.filter { isPendingModelStatus(it.status) }
        "APPROVED", "ROSTER" -> models.filter { isRosterApprovedModelStatus(it.status) }
        "REJECTED" -> models.filter { isRejectedModelStatus(it.status) }
        else -> models
    }

    val cleanQuery = searchQuery.trim().lowercase()
    if (cleanQuery.isEmpty()) return statusFiltered

    return statusFiltered.filter { model ->
        model.fullName.lowercase().contains(cleanQuery) ||
            model.city.lowercase().contains(cleanQuery) ||
            model.phoneNumber.lowercase().contains(cleanQuery) ||
            model.skills.lowercase().contains(cleanQuery) ||
            model.portfolioUrls.lowercase().contains(cleanQuery) ||
            model.adminNotes.lowercase().contains(cleanQuery)
    }
}

/**
 * Enforces manual admin verification rules before an applicant can be verified into the
 * Official Studio Roster or rejected.
 */
fun validateModelVerificationDecision(
    newStatus: String,
    age: Long,
    consentGiven: Boolean,
    manualChecksConfirmed: Boolean,
    adminNotes: String,
    isHindi: Boolean
): String? {
    val normalized = newStatus.trim().lowercase()
    if (normalized == "approved") {
        if (age < 18L) {
            return if (isHindi) {
                "18 वर्ष से कम आयु के आवेदक को आधिकारिक स्टूडियो रोस्टर में नहीं जोड़ा जा सकता।"
            } else {
                "Applicants under 18 years old cannot be added to the Official Studio Roster."
            }
        }
        if (!consentGiven) {
            return if (isHindi) {
                "रोस्टर सत्यापन से पहले लिखित 18+ सहमति आवश्यक है।"
            } else {
                "Written 18+ consent is required before adding an applicant to the studio roster."
            }
        }
        if (!manualChecksConfirmed) {
            return if (isHindi) {
                "आधिकारिक रोस्टर में जोड़ने से पहले कृपया ऊपर दिए गए मैन्युअल सत्यापन चेकबॉक्स की पुष्टि करें।"
            } else {
                "Confirm the manual 18+ ID, consent & portfolio check before adding to the Official Studio Roster."
            }
        }
    } else if (normalized == "rejected") {
        if (adminNotes.trim().length < 3) {
            return if (isHindi) {
                "कृपया अस्वीकार करने से पहले संक्षिप्त कारण या एडमिन नोट चुनें या लिखें।"
            } else {
                "Please select or enter a brief verification note before rejecting this application."
            }
        }
    }
    return null
}

@Composable
fun AdminModelVerificationSection(
    models: List<ModelRegistration>,
    isLoading: Boolean,
    language: AppLanguage,
    onUpdateModelStatus: (modelId: String, status: String, adminNotes: String) -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    val focusManager = LocalFocusManager.current

    var sectionFilter by remember { mutableStateOf("PENDING") }
    var searchQuery by remember { mutableStateOf("") }

    val pendingCount = remember(models) { models.count { isPendingModelStatus(it.status) } }
    val rosterCount = remember(models) { models.count { isRosterApprovedModelStatus(it.status) } }
    val rejectedCount = remember(models) { models.count { isRejectedModelStatus(it.status) } }

    val filteredModels = remember(models, sectionFilter, searchQuery) {
        filterAdminModelApplications(
            models = models,
            sectionFilter = sectionFilter,
            searchQuery = searchQuery
        )
    }

    if (isLoading) {
        AdminSkeletonList()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_model_verification_section")
    ) {
        // Dedicated Policy & Official Studio Roster Gate Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .border(
                    1.dp,
                    Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.65f), StudioOutline)),
                    RoundedCornerShape(14.dp)
                )
                .testTag("admin_model_verification_header"),
            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GoldContainer)
                        .border(1.dp, GoldPrimary.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHindi) {
                            "मॉडल सत्यापन व आधिकारिक स्टूडियो रोस्टर"
                        } else {
                            "Manual Model Verification & Official Roster"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                    Text(
                        text = if (isHindi) {
                            "आवेदक एडमिन द्वारा 18+ आयु, सहमति और पोर्टफोलियो सत्यापन के बाद ही आधिकारिक रोस्टर में जुड़ते हैं।"
                        } else {
                            "Review 18+ ID, written consent, and portfolio links before approving talent onto the official roster."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }

                Surface(
                    color = Color(0xFF064E3B),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .clickable { sectionFilter = "APPROVED" }
                        .testTag("admin_roster_summary_badge")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$rosterCount",
                            color = SuccessGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isHindi) "रोस्टर" else "ROSTER",
                            color = GoldLight,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Section Filter Chips: Verify Queue, Official Roster, Rejected, All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusFilterChip(
                label = if (isHindi) "सत्यापन कतार" else "Verify Queue",
                count = pendingCount,
                isSelected = sectionFilter == "PENDING",
                indicatorColor = Color(0xFFFBBF24),
                onClick = { sectionFilter = "PENDING" },
                testTag = "admin_model_filter_pending"
            )
            StatusFilterChip(
                label = if (isHindi) "आधिकारिक रोस्टर" else "Official Roster",
                count = rosterCount,
                isSelected = sectionFilter == "APPROVED",
                indicatorColor = Color(0xFF10B981),
                onClick = { sectionFilter = "APPROVED" },
                testTag = "admin_model_filter_approved"
            )
            StatusFilterChip(
                label = if (isHindi) "अस्वीकृत" else "Rejected",
                count = rejectedCount,
                isSelected = sectionFilter == "REJECTED",
                indicatorColor = Color(0xFFEF4444),
                onClick = { sectionFilter = "REJECTED" },
                testTag = "admin_model_filter_rejected"
            )
            StatusFilterChip(
                label = if (isHindi) "सभी" else "All",
                count = models.size,
                isSelected = sectionFilter == "ALL",
                indicatorColor = null,
                onClick = { sectionFilter = "ALL" },
                testTag = "admin_model_filter_all"
            )
        }

        // Search input for screening applicants & official roster talent
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            placeholder = {
                Text(
                    text = if (isHindi) {
                        "नाम, शहर, कौशल या फ़ोन से खोजें..."
                    } else {
                        "Search applicant name, city, phone, or skills..."
                    },
                    fontSize = 12.5.sp,
                    color = TextSecondary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("admin_model_search_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = GoldLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = StudioOutline,
                focusedContainerColor = StudioElevatedSurface,
                unfocusedContainerColor = StudioElevatedSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = GoldPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("admin_model_search_input")
        )

        if (filteredModels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .testTag("admin_models_empty_state"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = when {
                            models.isEmpty() -> if (isHindi) "अभी तक कोई मॉडल आवेदन प्राप्त नहीं हुआ है।" else "No model registrations submitted yet."
                            sectionFilter == "PENDING" -> if (isHindi) "सभी मॉडल आवेदनों की समीक्षा हो चुकी है!" else "All pending model applications have been reviewed!"
                            sectionFilter == "APPROVED" -> if (isHindi) "अभी तक कोई सत्यापित मॉडल आधिकारिक रोस्टर में नहीं है।" else "No manually verified models in the Official Studio Roster yet."
                            else -> if (isHindi) "चयनित फ़िल्टर के लिए कोई मॉडल आवेदन नहीं मिला।" else "No model applications match the current filter."
                        },
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (sectionFilter == "PENDING" && rosterCount > 0) {
                        OutlinedButton(
                            onClick = { sectionFilter = "APPROVED" },
                            border = BorderStroke(1.dp, GoldPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("admin_view_roster_cta")
                        ) {
                            Text(
                                text = if (isHindi) "आधिकारिक रोस्टर देखें ($rosterCount)" else "View Official Roster ($rosterCount)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("admin_models_lazy_list"),
                contentPadding = PaddingValues(top = 6.dp, bottom = 44.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredModels, key = { it.id.ifBlank { "${it.userId}_${it.fullName}" } }) { model ->
                    AdminModelVerificationCard(
                        model = model,
                        language = language,
                        onUpdate = onUpdateModelStatus
                    )
                }
            }
        }
    }
}

@Composable
fun AdminModelVerificationCard(
    model: ModelRegistration,
    language: AppLanguage = AppLanguage.ENGLISH,
    onUpdate: (String, String, String) -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    val itemStatus = AdminItemStatus.from(model.status)
    val isApprovedRoster = isRosterApprovedModelStatus(model.status)
    val isRejected = isRejectedModelStatus(model.status)

    var adminNotes by remember(model.id, model.adminNotes) { mutableStateOf(model.adminNotes) }
    var manualChecksConfirmed by remember(model.id, model.status) {
        mutableStateOf(isApprovedRoster)
    }
    var validationError by remember(model.id, model.status) { mutableStateOf<String?>(null) }
    var showRejectConfirmDialog by remember(model.id) { mutableStateOf(false) }

    val quickNotes = if (isHindi) {
        listOf(
            "कमर्शियल व एडिटोरियल रोस्टर हेतु सत्यापित",
            "पोर्टफोलियो लिंक अपूर्ण — पुनः अपलोड करें",
            "18+ आयु प्रमाण स्पष्ट नहीं"
        )
    } else {
        listOf(
            "Verified for Official Commercial Roster",
            "Incomplete portfolio — please resubmit photos",
            "Clearer 18+ age verification needed"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.2.dp,
                    Brush.horizontalGradient(
                        listOf(
                            itemStatus.primaryColor.copy(alpha = 0.75f),
                            itemStatus.borderColor.copy(alpha = 0.45f),
                            StudioOutline
                        )
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .testTag("admin_model_card_${model.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Applicant Name + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldLight
                    )
                    Text(
                        text = "${model.gender} • ${model.city}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                AdminStatusBadge(
                    statusStr = model.status,
                    language = language
                )
            }

            // Roster / 18+ Compliance Pill Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val ageEligible = model.age >= 18L
                Surface(
                    color = if (ageEligible) Color(0xFF064E3B) else Color(0xFF450A0A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (ageEligible) SuccessGreen else ErrorRed)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (ageEligible) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (ageEligible) SuccessGreen else ErrorRed,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (ageEligible) "18+ Verified (${model.age} yrs)" else "Under 18 (${model.age})",
                            color = if (ageEligible) SuccessGreen else ErrorRed,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = if (model.consentGiven) GoldContainer else Color(0xFF450A0A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (model.consentGiven) GoldPrimary.copy(alpha = 0.6f) else ErrorRed)
                ) {
                    Text(
                        text = if (model.consentGiven) {
                            if (isHindi) "लिखित सहमति ✓" else "Consent Signed ✓"
                        } else {
                            if (isHindi) "सहमति लंबित" else "Missing Consent"
                        },
                        color = if (model.consentGiven) OnGoldContainer else ErrorRed,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (isApprovedRoster) {
                    Surface(
                        color = GoldContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, GoldPrimary),
                        modifier = Modifier.testTag("model_official_roster_badge_${model.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "आधिकारिक स्टूडियो रोस्टर" else "OFFICIAL STUDIO ROSTER",
                                color = GoldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                }
            }

            // Applicant Details Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioElevatedSurface)
                    .border(0.5.dp, StudioOutline, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Phone: ${model.phoneNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (model.skills.isNotBlank()) {
                    Text(
                        text = "Skills: ${model.skills}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }

                if (model.portfolioUrls.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = GoldLight,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Portfolio: ${model.portfolioUrls}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldLight,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Manual Verification Checklist Gate (Required prior to Roster Verification)
            if (!isApprovedRoster) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (manualChecksConfirmed) Color(0xFF064E3B).copy(alpha = 0.55f)
                            else StudioElevatedSurface
                        )
                        .border(
                            1.dp,
                            if (manualChecksConfirmed) SuccessGreen else GoldPrimary.copy(alpha = 0.55f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(role = Role.Checkbox) {
                            manualChecksConfirmed = !manualChecksConfirmed
                            validationError = null
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("model_verify_check_${model.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = manualChecksConfirmed,
                        onCheckedChange = {
                            manualChecksConfirmed = it
                            validationError = null
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SuccessGreen,
                            uncheckedColor = GoldPrimary,
                            checkmarkColor = StudioBlack
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isHindi) {
                            "मैन्युअल सत्यापन: 18+ पहचान, लिखित सहमति और पोर्टफोलियो की समीक्षा पूर्ण"
                        } else {
                            "Manual Check: Verified 18+ ID, written consent & portfolio quality"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (manualChecksConfirmed) SuccessGreen else TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Admin Verification / Rejection Notes Input
            OutlinedTextField(
                value = adminNotes,
                onValueChange = {
                    if (it.length <= 1000) {
                        adminNotes = it
                        validationError = null
                    }
                },
                label = {
                    Text(
                        text = if (isHindi) "एडमिन सत्यापन / अस्वीकृति नोट" else "Admin Verification / Rejection Note",
                        fontSize = 11.5.sp
                    )
                },
                placeholder = {
                    Text(
                        text = if (isHindi) {
                            "रोस्टर श्रेणी या अस्वीकृति का कारण लिखें..."
                        } else {
                            "Add roster category note or reason for rejection..."
                        },
                        fontSize = 11.5.sp
                    )
                },
                minLines = 1,
                maxLines = 3,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = StudioOutline,
                    focusedLabelColor = GoldLight,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = GoldPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("model_admin_notes_input_${model.id}")
            )

            // Quick-select Note Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickNotes.forEachIndexed { idx, preset ->
                    Surface(
                        onClick = {
                            adminNotes = preset
                            validationError = null
                        },
                        color = StudioElevatedSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, StudioOutline),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .testTag("model_quick_note_${model.id}_$idx")
                    ) {
                        Text(
                            text = preset,
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (validationError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF450A0A))
                        .border(1.dp, ErrorRed, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("model_verify_error_${model.id}")
                ) {
                    Text(
                        text = validationError ?: "",
                        color = ErrorRed,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Action Buttons: Verify & Add to Official Roster, Reject Application, Move to Pending
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val err = validateModelVerificationDecision(
                            newStatus = "approved",
                            age = model.age,
                            consentGiven = model.consentGiven,
                            manualChecksConfirmed = manualChecksConfirmed,
                            adminNotes = adminNotes,
                            isHindi = isHindi
                        )
                        if (err != null) {
                            validationError = err
                        } else {
                            validationError = null
                            onUpdate(model.id, "approved", adminNotes.trim())
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("model_approve_button_${model.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApprovedRoster) Color(0xFF065F46) else SuccessGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isApprovedRoster) {
                            if (isHindi) "रोस्टर में सक्रिय ✓" else "In Roster ✓"
                        } else {
                            if (isHindi) "रोस्टर में जोड़ें" else "Verify & Roster"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        val err = validateModelVerificationDecision(
                            newStatus = "rejected",
                            age = model.age,
                            consentGiven = model.consentGiven,
                            manualChecksConfirmed = manualChecksConfirmed,
                            adminNotes = adminNotes,
                            isHindi = isHindi
                        )
                        if (err != null) {
                            validationError = err
                        } else {
                            validationError = null
                            showRejectConfirmDialog = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("model_reject_button_${model.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRejected) Color(0xFF7F1D1D) else MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isRejected) {
                            if (isHindi) "अस्वीकृत ✕" else "Rejected ✕"
                        } else {
                            if (isHindi) "अस्वीकार करें" else "Reject"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isApprovedRoster || isRejected) {
                    OutlinedButton(
                        onClick = {
                            validationError = null
                            onUpdate(model.id, "pending", adminNotes.trim())
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("model_pending_button_${model.id}"),
                        border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isHindi) "लंबित" else "Pending",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showRejectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRejectConfirmDialog = false },
            containerColor = StudioCardSurface,
            modifier = Modifier.testTag("model_reject_dialog_${model.id}"),
            title = {
                Text(
                    text = if (isHindi) "आवेदन अस्वीकार करें?" else "Reject Model Application?",
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isHindi) {
                            "${model.fullName} को आधिकारिक स्टूडियो रोस्टर में नहीं जोड़ा जाएगा। यह एडमिन नोट आवेदक को दिखाई देगा:"
                        } else {
                            "${model.fullName} will not be added to the Official Studio Roster. The following note will be saved with the decision:"
                        },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "\"${adminNotes.trim()}\"",
                        color = GoldPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectConfirmDialog = false
                        onUpdate(model.id, "rejected", adminNotes.trim())
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("model_reject_confirm_${model.id}")
                ) {
                    Text(if (isHindi) "पुष्टि करें व अस्वीकार करें" else "Confirm Reject")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRejectConfirmDialog = false },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("model_reject_cancel_${model.id}")
                ) {
                    Text(
                        text = if (isHindi) "रद्द करें" else "Cancel",
                        color = TextSecondary
                    )
                }
            }
        )
    }
}
