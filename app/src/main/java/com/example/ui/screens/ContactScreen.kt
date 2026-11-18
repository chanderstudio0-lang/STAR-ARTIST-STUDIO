package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.StudioInfoEntity
import com.example.data.model.AppLanguage
import com.example.data.repository.StudioInfoRepository
import com.example.ui.components.StudioAddressMapCard
import com.example.ui.components.makeStudioCall
import com.example.ui.components.openWhatsApp
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.WhatsAppGreen
import com.example.util.StudioConstants
import com.example.util.StudioContactDetails
import com.example.util.StudioStrings

/**
 * Launches the native Google Maps directions intent for the studio location
 */
fun openMapDirections(context: Context) {
    try {
        val gmmIntentUri = Uri.parse("geo:0,0?q=Star+Artist+Studio+Talwara+Khurd+Ellenabad+Sirsa+Haryana+125102")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/search/?api=1&query=Talwara+Khurd+Ellenabad+Sirsa+Haryana+125102")
        )
        context.startActivity(browserIntent)
    }
}

/**
 * Copies text to the Android system clipboard with a toast notification
 */
fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

/**
 * Launches email intent to the studio's official admin contact
 */
fun openStudioEmail(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${StudioContactDetails.ADMIN_EMAIL}")
            putExtra(Intent.EXTRA_SUBJECT, "Inquiry: Star Artist Studio")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Email: ${StudioContactDetails.ADMIN_EMAIL}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ContactScreen(
    language: AppLanguage,
    cachedStudioInfo: StudioInfoEntity = StudioInfoRepository.defaultStudioInfo(),
    onRefreshCache: () -> Unit = {}
) {
    val s = StudioStrings(language)
    val context = LocalContext.current

    var inquiryName by remember { mutableStateOf("") }
    var inquiryPhone by remember { mutableStateOf("") }
    var inquiryMsg by remember { mutableStateOf("") }
    var selectedServiceType by remember { mutableStateOf("Photography & Video") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("contact_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =====================================================================
        // 1. STUDIO IDENTITY & HOURS HEADER (ROOM OFFLINE CACHED)
        // =====================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.7f), StudioOutline))),
                        RoundedCornerShape(18.dp)
                    )
                    .testTag("contact_header_card"),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "OFFICIAL STUDIO DESK",
                                color = GoldLight,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        // Room Offline Caching Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF132213))
                                .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("offline_cached_badge")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "OFFLINE READY • ROOM CACHE",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = cachedStudioInfo.studioName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldPrimary,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "प्रीमियम मॉडलिंग, फोटोग्राफी और 4K सिनेमा वीडियो प्रोडक्शन स्टूडियो"
                        else
                            cachedStudioInfo.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC0C0C0),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Local Room Cache Synchronization Notice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF18150D))
                            .border(0.5.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cached in local SQLite Room database • Available offline",
                                color = GoldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = onRefreshCache,
                            modifier = Modifier.size(24.dp).testTag("refresh_studio_cache_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Cache",
                                tint = GoldLight,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Studio Operating Hours Card Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioElevatedSurface)
                            .border(1.dp, StudioOutline, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Hours",
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cachedStudioInfo.openingHours,
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 2. OFFICIAL STUDIO ADDRESS & VISUAL MAP REPRESENTATION
        // =====================================================================
        item {
            StudioAddressMapCard(language = language)
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
                    .testTag("official_address_card"),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(GoldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Studio Location",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = s.addressLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }

                        IconButton(
                            onClick = {
                                copyToClipboard(context, "Studio Address", cachedStudioInfo.fullAddress)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Address",
                                tint = GoldLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Full Exact Address
                    Text(
                        text = cachedStudioInfo.fullAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Address Breakdown Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AddressBadge(label = "Village", value = cachedStudioInfo.village, modifier = Modifier.weight(1f))
                        AddressBadge(label = "Tehsil", value = cachedStudioInfo.tehsil, modifier = Modifier.weight(1f))
                        AddressBadge(label = "District", value = cachedStudioInfo.district, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Get Directions Button (Opens Google Maps)
                    Button(
                        onClick = { openMapDirections(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("directions_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF262012),
                            contentColor = GoldLight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = GoldPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "मानचित्र में स्टूडियो देखें (Get Directions)" else "Get Studio Directions (Google Maps)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 3. OFFICIAL MOBILE NUMBER (WITH DIRECT TEL-LINK)
        // =====================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.6f), StudioOutline))),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("official_mobile_card"),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF282010))
                                    .border(1.dp, GoldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Official Phone",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "आधिकारिक मोबाइल नंबर" else "Official Studio Mobile",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight
                                )
                                Text(
                                    text = cachedStudioInfo.formattedMobile,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                copyToClipboard(context, "Mobile Number", cachedStudioInfo.mobileNumber)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Number",
                                tint = GoldLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "शूट बुकिंग, मॉडल कास्टिंग और सामान्य पूछताछ हेतु सीधे कॉल करें।"
                        else
                            "Direct voice call line for shoot scheduling, model auditions & studio queries.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Direct Tel-Link Button
                    Button(
                        onClick = { makeStudioCall(context, cachedStudioInfo.callUri) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("direct_call_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF141000)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Make Call",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s.makeCallBtn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 4. OFFICIAL WHATSAPP NUMBER (WITH DIRECT WHATSAPP LINK)
        // =====================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, Brush.horizontalGradient(listOf(WhatsAppGreen.copy(alpha = 0.6f), StudioOutline))),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("official_whatsapp_card"),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "W",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "आधिकारिक व्हाट्सएप नंबर" else "Official Studio WhatsApp",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight
                                )
                                Text(
                                    text = cachedStudioInfo.formattedWhatsapp,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WhatsAppGreen,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                copyToClipboard(context, "WhatsApp Number", cachedStudioInfo.whatsappNumber)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy WhatsApp Number",
                                tint = GoldLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "व्हाट्सएप चैट लिंक (https://wa.me/919541020948) पर तत्काल बातचीत करें।"
                        else
                            "Instant online consultation via verified WhatsApp link (${cachedStudioInfo.whatsappUrl}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Direct WhatsApp Link Button
                    Button(
                        onClick = { openWhatsApp(context, cachedStudioInfo.whatsappUrl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("direct_whatsapp_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("W", color = WhatsAppGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s.openWhatsAppBtn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 5. OFFICIAL EMAIL & CASTING DESK
        // =====================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Admin & Casting Email",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldLight
                            )
                            Text(
                                text = cachedStudioInfo.adminEmail,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { openStudioEmail(context) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Send Mail", fontSize = 11.sp)
                    }
                }
            }
        }

        // =====================================================================
        // 6. DIRECT WHATSAPP INQUIRY COMPOSER FORM
        // =====================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
                    .testTag("inquiry_form_card"),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "व्हाट्सएप पर सीधा संदेश भेजें" else "Send Direct WhatsApp Inquiry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )

                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "अपना विवरण दर्ज करें; यह संदेश स्वचालित रूप से हमारे आधिकारिक व्हाट्सएप पर तैयार हो जाएगा।"
                        else
                            "Fill in your project requirements to initiate an instant chat with our production coordinator.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA),
                        fontSize = 11.5.sp
                    )

                    OutlinedTextField(
                        value = inquiryName,
                        onValueChange = { inquiryName = it },
                        label = { Text(s.clientNameLabel) },
                        placeholder = { Text("e.g. Rahul Verma") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inquiry_name_input"),
                        colors = studioTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = inquiryPhone,
                        onValueChange = { inquiryPhone = it },
                        label = { Text(s.contactPhoneLabel) },
                        placeholder = { Text("e.g. 9812890081") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inquiry_phone_input"),
                        colors = studioTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = inquiryMsg,
                        onValueChange = { inquiryMsg = it },
                        label = { Text(if (language == AppLanguage.HINDI) "संदेश / पूछताछ" else "Message / Shoot Details") },
                        placeholder = { Text("e.g. Interested in 4K music video shoot at Talwara Khurd studio") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inquiry_msg_input"),
                        colors = studioTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 3
                    )

                    Button(
                        onClick = {
                            val msgText = buildString {
                                append("Hello Star Artist Studio!\n")
                                if (inquiryName.isNotBlank()) append("Client: $inquiryName\n")
                                if (inquiryPhone.isNotBlank()) append("Contact: $inquiryPhone\n")
                                if (inquiryMsg.isNotBlank()) append("Inquiry: $inquiryMsg\n")
                                append("Location: Talwara Khurd, Sirsa")
                            }
                            val url = StudioContactDetails.buildWhatsAppUrl(msgText)
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("inquiry_submit_whatsapp_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "व्हाट्सएप चैट भेजें (${StudioContactDetails.WHATSAPP_NUMBER})"
                            else
                                "Send via WhatsApp (${StudioContactDetails.WHATSAPP_NUMBER})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddressBadge(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StudioElevatedSurface)
            .border(0.5.dp, StudioOutline, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = GoldPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE0E0E0),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
