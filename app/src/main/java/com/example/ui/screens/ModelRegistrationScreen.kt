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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.ModelRegistration
import com.example.ui.components.BookingsSkeletonList
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.util.StudioStrings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelRegistrationScreen(
    language: AppLanguage,
    myModels: List<ModelRegistration>,
    isSubmitting: Boolean,
    isLoading: Boolean = false,
    onSubmit: (
        fullName: String,
        ageStr: String,
        gender: String,
        city: String,
        phoneNumber: String,
        skills: String,
        portfolioUrl: String,
        consentGiven: Boolean,
        onSuccess: () -> Unit
    ) -> Unit
) {
    val s = StudioStrings(language)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Form fields
    var fullName by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Female") }
    var city by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var portfolioUrl by remember { mutableStateOf("") }
    var consentGiven by remember { mutableStateOf(false) }

    // Validation state
    var showValidationErrors by remember { mutableStateOf(false) }

    // Derived validation states
    val isNameValid by remember { derivedStateOf { fullName.trim().length >= 2 } }
    val ageNum by remember { derivedStateOf { ageStr.trim().toLongOrNull() } }
    val isAgeValid by remember { derivedStateOf { ageNum != null && ageNum!! >= 18 } }
    val isCityValid by remember { derivedStateOf { city.trim().isNotEmpty() } }
    val isPhoneValid by remember { derivedStateOf { phoneNumber.trim().length >= 10 } }
    val isFormValid by remember {
        derivedStateOf { isNameValid && isAgeValid && isCityValid && isPhoneValid && consentGiven }
    }

    val genderOptions = listOf("Female", "Male", "Non-Binary", "Other")

    // Skill Suggestions for quick-selection
    val skillSuggestions = listOf(
        "Fashion Ramp Walk",
        "Commercial Stills",
        "Editorial Poses",
        "Music Video Acting",
        "Bridal Wear",
        "Traditional Ethnic",
        "Fitness Modeling",
        "E-Commerce Catalog"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("model_registration_screen")
    ) {
        // Tab Navigation Header
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
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (selectedTab == 0) GoldPrimary else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "नया आवेदन (18+)" else "New Application (18+)",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) GoldPrimary else Color.Gray
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (selectedTab == 1) GoldPrimary else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = (if (language == AppLanguage.HINDI) "मेरे आवेदन" else "My Applications") + " (${myModels.size})",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) GoldPrimary else Color.Gray
                        )
                    }
                }
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner: Studio Model Roster Overview
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(1.dp, Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.6f), StudioOutline))),
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(R.drawable.studio_model_portrait_1793955328333)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Model Casting Spotlight",
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GoldContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "TALENT CASTING • ADULTS ONLY (18+)",
                                        color = GoldLight,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = s.modelRegTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight
                                )
                                Text(
                                    text = s.modelRegSub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA0A0A0),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // 18+ Legal Eligibility Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF211D12))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Strict 18+ Validation",
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "अनिवार्य नियम: केवल 18+ वयस्क" else "Mandatory Rule: Adults Aged 18+ Only",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                                Text(
                                    text = if (language == AppLanguage.HINDI)
                                        "स्टार आर्टिस्ट स्टूडियो में पेशेवर मॉडलिंग, फैशन शो और कमर्शियल शूटिंग के लिए केवल 18 वर्ष या उससे अधिक आयु के वयस्क ही पात्र हैं। नाबालिगों का पंजीकरण स्वीकार नहीं किया जाता।"
                                    else
                                        "STAR ARTIST STUDIO strictly accepts applications from adults aged 18 and older for professional modeling contracts, runway shows, and commercial video shoots.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC0C0C0),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Validation Errors Summary Banner (if triggered)
                if (showValidationErrors && !isFormValid) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1515)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Validation Errors",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "कृपया निम्नलिखित विवरण सही करें:" else "Please resolve the following before submitting:",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    if (!isNameValid) {
                                        Text(
                                            text = "• " + (if (language == AppLanguage.HINDI) "पूरा नाम दर्ज करें (न्यूनतम 2 अक्षर)" else "Enter full name (min 2 characters)"),
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (!isAgeValid) {
                                        Text(
                                            text = "• " + (if (language == AppLanguage.HINDI) "आयु 18 वर्ष या उससे अधिक होनी चाहिए" else "Age must be 18 years or older"),
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (!isCityValid) {
                                        Text(
                                            text = "• " + (if (language == AppLanguage.HINDI) "अपना शहर / जिला दर्ज करें" else "Enter your city / district"),
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (!isPhoneValid) {
                                        Text(
                                            text = "• " + (if (language == AppLanguage.HINDI) "मान्य 10 अंकों का मोबाइल नंबर दर्ज करें" else "Enter a valid 10-digit mobile number"),
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (!consentGiven) {
                                        Text(
                                            text = "• " + (if (language == AppLanguage.HINDI) "18+ पात्रता एवं सहमति बॉक्स चेक करें" else "Check the 18+ certification & consent box"),
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. FULL NAME FIELD
                item {
                    Column {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text(s.fullNameLabel) },
                            placeholder = { Text("e.g. Chander Sharma") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = GoldPrimary
                                )
                            },
                            trailingIcon = {
                                if (isNameValid) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Valid",
                                        tint = SuccessGreen
                                    )
                                }
                            },
                            isError = showValidationErrors && !isNameValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("model_fullname_input"),
                            colors = studioTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        if (showValidationErrors && !isNameValid) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "कृपया कम से कम 2 अक्षरों का नाम दर्ज करें।" else "Please enter your legal name (min 2 characters).",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                            )
                        }
                    }
                }

                // 2. AGE FIELD (STRICT 18+ VALIDATION)
                item {
                    Column {
                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = { if (it.length <= 3 && it.all { ch -> ch.isDigit() }) ageStr = it },
                            label = { Text(s.ageLabel) },
                            placeholder = { Text("e.g. 21") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (isAgeValid) SuccessGreen else if (ageNum != null && ageNum!! < 18) MaterialTheme.colorScheme.error else GoldPrimary
                                )
                            },
                            trailingIcon = {
                                if (isAgeValid) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified 18+",
                                        tint = SuccessGreen
                                    )
                                } else if (ageNum != null && ageNum!! < 18) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Underage",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            isError = (showValidationErrors && !isAgeValid) || (ageNum != null && ageNum!! < 18),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("model_age_input"),
                            colors = studioTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Immediate feedback below Age field
                        if (ageNum != null && ageNum!! < 18) {
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "अस्वीकृत: स्टार आर्टिस्ट स्टूडियो केवल 18+ वयस्कों के लिए है। नाबालिगों का पंजीकरण वर्जित है।"
                                else
                                    "Not Allowed: Strictly 18+ adults only. Minors are not permitted to register.",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                            )
                        } else if (isAgeValid) {
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "सत्यापित: 18+ वयस्क आयु आवश्यकता पूर्ण।"
                                else
                                    "Verified: Meets the 18+ adult age requirement.",
                                color = SuccessGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                            )
                        } else {
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "पंजीकरण हेतु आपकी आयु कम से कम 18 वर्ष होनी आवश्यक है।"
                                else
                                    "You must be at least 18 years old to submit this registration.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                            )
                        }
                    }
                }

                // 3. GENDER SELECTION
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wc,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = s.genderLabel,
                                color = GoldLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(genderOptions) { opt ->
                                FilterChip(
                                    selected = gender == opt,
                                    onClick = { gender = opt },
                                    label = { Text(opt, fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = if (gender == opt) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPrimary,
                                        selectedLabelColor = StudioBlack,
                                        selectedLeadingIconColor = StudioBlack,
                                        containerColor = StudioElevatedSurface,
                                        labelColor = Color(0xFFC0C0C0)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = gender == opt,
                                        borderColor = if (gender == opt) GoldPrimary else StudioOutline
                                    ),
                                    modifier = Modifier.testTag("model_gender_chip_$opt")
                                )
                            }
                        }
                    }
                }

                // 4. CITY FIELD
                item {
                    Column {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text(s.cityLabel) },
                            placeholder = { Text("e.g. Sirsa / Ellenabad / Chandigarh / Delhi") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationCity,
                                    contentDescription = null,
                                    tint = GoldPrimary
                                )
                            },
                            trailingIcon = {
                                if (isCityValid) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Valid",
                                        tint = SuccessGreen
                                    )
                                }
                            },
                            isError = showValidationErrors && !isCityValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("model_city_input"),
                            colors = studioTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        if (showValidationErrors && !isCityValid) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "कृपया अपना शहर दर्ज करें।" else "Please enter your city/district.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                            )
                        }
                    }
                }

                // 5. PHONE NUMBER FIELD
                item {
                    Column {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                if (digits.length <= 10) phoneNumber = digits
                            },
                            label = { Text(s.phoneLabel) },
                            placeholder = { Text("e.g. 9812890081") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = GoldPrimary
                                )
                            },
                            trailingIcon = {
                                if (isPhoneValid) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Valid",
                                        tint = SuccessGreen
                                    )
                                } else {
                                    Text(
                                        text = "${phoneNumber.length}/10",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            },
                            isError = showValidationErrors && !isPhoneValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("model_phone_input"),
                            colors = studioTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        if (showValidationErrors && !isPhoneValid) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "कृपया 10 अंकों का मान्य फोन नंबर दर्ज करें।" else "Please enter a valid 10-digit contact number.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                            )
                        }
                    }
                }

                // 6. SKILLS & EXPERIENCE FIELD WITH QUICK-ADD CHIPS
                item {
                    Column {
                        OutlinedTextField(
                            value = skills,
                            onValueChange = { skills = it },
                            label = { Text(s.skillsLabel) },
                            placeholder = { Text("e.g. Fashion ramp walk, commercial acting, traditional wear, studio poses") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = GoldPrimary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("model_skills_input"),
                            colors = studioTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (language == AppLanguage.HINDI) "त्वरित कौशल जोड़ें (टैप करें):" else "Quick Skill Suggestions (Tap to add):",
                            color = GoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            skillSuggestions.forEach { skillTag ->
                                val isSelected = skills.contains(skillTag, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF3B3012) else StudioElevatedSurface)
                                        .border(
                                            1.dp,
                                            if (isSelected) GoldPrimary else StudioOutline,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (!isSelected) {
                                                skills = if (skills.isBlank()) skillTag else "$skills, $skillTag"
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = GoldLight,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = skillTag,
                                            color = if (isSelected) GoldLight else Color(0xFFB5B5B5),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. PORTFOLIO PHOTOS / LINK (OPTIONAL)
                item {
                    Column {
                        OutlinedTextField(
                            value = portfolioUrl,
                            onValueChange = { portfolioUrl = it },
                            label = { Text(s.portfolioUrlLabel) },
                            placeholder = { Text("https://instagram.com/yourhandle or Drive link") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = GoldPrimary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("model_portfolio_input"),
                            colors = studioTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "इंस्टाग्राम प्रोफ़ाइल, गूगल ड्राइव या क्लाउड फ़ोटो लिंक साझा करें।"
                            else
                                "Share Instagram profile, Google Drive link, or direct photo portfolio URL.",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                        )
                    }
                }

                // 8. MANDATORY 18+ CONSENT CHECKBOX
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (consentGiven) GoldPrimary else if (showValidationErrors) MaterialTheme.colorScheme.error else StudioOutline,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { consentGiven = !consentGiven },
                        colors = CardDefaults.cardColors(containerColor = if (consentGiven) Color(0xFF231E12) else StudioCardSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = consentGiven,
                                onCheckedChange = { consentGiven = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = GoldPrimary,
                                    checkmarkColor = StudioBlack,
                                    uncheckedColor = if (showValidationErrors && !consentGiven) MaterialTheme.colorScheme.error else Color.Gray
                                ),
                                modifier = Modifier.testTag("model_consent_checkbox")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "18+ आयु सत्यापन एवं सहमति (अनिवार्य)" else "18+ Age Certification & Consent (Mandatory)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (consentGiven) GoldPrimary else GoldLight
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = s.consentLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (consentGiven) Color.White else Color(0xFFAAAAAA),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // 9. SUBMIT BUTTON
                item {
                    Button(
                        onClick = {
                            if (!isFormValid) {
                                showValidationErrors = true
                            } else {
                                showValidationErrors = false
                                onSubmit(
                                    fullName,
                                    ageStr,
                                    gender,
                                    city,
                                    phoneNumber,
                                    skills,
                                    portfolioUrl,
                                    consentGiven
                                ) {
                                    // Reset form and navigate to My Applications tab
                                    fullName = ""
                                    ageStr = ""
                                    city = ""
                                    phoneNumber = ""
                                    skills = ""
                                    portfolioUrl = ""
                                    consentGiven = false
                                    showValidationErrors = false
                                    selectedTab = 1
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_model_reg_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF141000)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = StudioBlack,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = s.submitRegButton,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // My Applications List Tab
            if (isLoading) {
                BookingsSkeletonList()
            } else if (myModels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(StudioElevatedSurface)
                                .border(1.dp, StudioOutline, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "आपने अभी तक कोई मॉडल आवेदन दर्ज नहीं किया है।"
                            else
                                "You haven't submitted any model registrations yet.",
                            color = Color(0xFFC0C0C0),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "स्टार आर्टिस्ट स्टूडियो के टैलेंट रोस्टर में शामिल होने के लिए नया आवेदन सबमिट करें।"
                            else
                                "Submit your application to join Star Artist Studio's exclusive talent roster.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { selectedTab = 0 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color(0xFF141000)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "आवेदन फॉर्म खोलें" else "Open Registration Form",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = (if (language == AppLanguage.HINDI) "आपके पंजीकृत आवेदन" else "Your Submitted Registrations") + " (${myModels.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(myModels) { model ->
                        ModelApplicationCard(model = model, language = language)
                    }
                }
            }
        }
    }
}

@Composable
fun ModelApplicationCard(model: ModelRegistration, language: AppLanguage) {
    val statusColor = when (model.status.lowercase()) {
        "approved" -> SuccessGreen
        "rejected" -> MaterialTheme.colorScheme.error
        else -> GoldPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(statusColor.copy(alpha = 0.5f), StudioOutline)),
                RoundedCornerShape(14.dp)
            )
            .testTag("model_app_card_${model.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF241F12))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = model.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                        Text(
                            text = "Age: ${model.age} (18+ Verified) • ${model.gender}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.18f))
                        .border(1.dp, statusColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = model.status.uppercase(),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "City: ${model.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD0D0D0)
                )
                Text(
                    text = "Phone: ${model.phoneNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (model.skills.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Skills: ${model.skills}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E),
                    lineHeight = 16.sp
                )
            }

            if (model.portfolioUrls.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Portfolio: ${model.portfolioUrls}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldLight.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            if (model.adminNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF262010))
                        .border(1.dp, GoldDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Studio Admin Note: ${model.adminNotes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight
                    )
                }
            }
        }
    }
}

@Composable
fun studioTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoldPrimary,
    unfocusedBorderColor = StudioOutline,
    focusedLabelColor = GoldLight,
    unfocusedLabelColor = if (StudioTheme.isLight) Color(0xFF5F6368) else Color.Gray,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = GoldPrimary,
    focusedContainerColor = StudioCardSurface,
    unfocusedContainerColor = StudioCardSurface
)
