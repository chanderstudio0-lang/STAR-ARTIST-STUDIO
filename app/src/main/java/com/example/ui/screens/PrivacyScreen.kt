package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.data.model.UserProfile
import com.example.ui.components.UserProfileSectionContent
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioOutline
import com.example.util.StudioConstants
import com.example.util.StudioStrings

@Composable
fun PrivacyScreen(
    language: AppLanguage,
    isSubmitting: Boolean,
    userProfile: UserProfile? = null,
    userEmail: String? = null,
    currentUserId: String = "",
    modelsCount: Int = 0,
    bookingsCount: Int = 0,
    onBack: () -> Unit,
    onRequestDataDeletion: (onComplete: () -> Unit) -> Unit
) {
    val s = StudioStrings(language)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("privacy_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (language == AppLanguage.HINDI) "उपयोगकर्ता प्रोफ़ाइल और डेटा गोपनीयता" else "User Profile & Privacy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // User Profile Section with direct 'Delete My Data' option
            item {
                UserProfileSectionContent(
                    userProfile = userProfile,
                    userEmail = userEmail,
                    currentUserId = currentUserId,
                    modelsCount = modelsCount,
                    bookingsCount = bookingsCount,
                    isSubmitting = isSubmitting,
                    language = language,
                    onDeleteMyData = {
                        onRequestDataDeletion {
                            onBack()
                        }
                    }
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioOutline, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Star Artist Studio Privacy Commitment",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "1. डेटा सुरक्षा: आपकी व्यक्तिगत जानकारी (नाम, फोन, पोर्टफोलियो) सुरक्षित क्लाउड डेटाबेस में सुरक्षित रखी जाती है।\n\n" +
                                "2. आयु सीमा: स्टार आर्टिस्ट स्टूडियो में केवल 18 वर्ष या उससे अधिक आयु के वयस्क ही मॉडलिंग के लिए पंजीकरण करा सकते हैं।\n\n" +
                                "3. पोर्टफोलियो कॉपीराइट: सभी डेमो सामग्री व फोटोग्राफी स्टार आर्टिस्ट स्टूडियो के कॉपीराइट के अधीन हैं।\n\n" +
                                "4. डेटा नियंत्रण: आपको अपनी जानकारी को देखने, अपडेट करने और पूर्णतः हटाने का पूर्ण अधिकार है।"
                            else
                                "1. Data Protection: Your personal information, contact number, and portfolio links are securely safeguarded in Cloud Firestore with zero-trust access controls.\n\n" +
                                "2. Age Eligibility: Participation in modeling and agency representation is strictly restricted to legal adults aged 18 and older.\n\n" +
                                "3. Portfolio Copyright: Sample works marked as DEMO remain creative assets of Star Artist Studio.\n\n" +
                                "4. User Sovereignty: You maintain full right to access, rectify, or purge your records at any time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD0D0D0),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
