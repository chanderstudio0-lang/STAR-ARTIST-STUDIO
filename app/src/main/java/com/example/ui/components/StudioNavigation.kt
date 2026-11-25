package com.example.ui.components

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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppLanguage
import com.example.ui.theme.DarkStudioBlack
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.viewmodel.StudioScreen
import com.example.util.StudioConstants
import com.example.util.StudioContactDetails
import com.example.util.StudioStrings

fun openWhatsApp(context: Context, url: String = "https://wa.me/919541020948") {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun makeStudioCall(context: Context, callUri: String = "tel:+919812890081") {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse(callUri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not initiate call: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Top App Bar theme icon button toggling between Black & Gold and High-Contrast Studio Light mode.
 */
@Composable
fun ThemeToggleIconButton(
    isStudioLight: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier.testTag("theme_toggle_button")
    ) {
        Icon(
            imageVector = if (isStudioLight) Icons.Default.DarkMode else Icons.Default.LightMode,
            contentDescription = if (isStudioLight)
                "Switch to Black and Gold Design"
            else
                "Switch to High-Contrast Studio Light Mode",
            tint = if (isStudioLight) GoldPrimary else GoldLight
        )
    }
}

/**
 * Pill-shaped theme mode toggle component for navigation and settings.
 */
@Composable
fun ThemeToggleComponent(
    isStudioLight: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isStudioLight) Color(0xFFE5E7EB) else Color(0xFF221D12))
            .border(
                BorderStroke(
                    1.dp,
                    if (isStudioLight) GoldPrimary else GoldDark.copy(alpha = 0.5f)
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("theme_mode_toggle"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isStudioLight) Icons.Default.WbSunny else Icons.Default.NightlightRound,
                contentDescription = null,
                tint = if (isStudioLight) GoldPrimary else GoldLight,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isStudioLight) "Studio Light" else "Black & Gold",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isStudioLight) GoldPrimary else GoldLight
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioTopAppBar(
    language: AppLanguage,
    isAdmin: Boolean,
    isStudioLight: Boolean = false,
    avatarUrl: String? = null,
    unreadNotificationsCount: Int = 0,
    onToggleTheme: () -> Unit = {},
    onToggleLanguage: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    onOpenPrivacy: () -> Unit,
    onOpenProfile: () -> Unit = onOpenPrivacy,
    onSignOut: () -> Unit
) {
    val s = StudioStrings(language)
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Studio Emblem",
                        tint = if (isStudioLight) Color.White else DarkStudioBlack,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = StudioConstants.STUDIO_NAME,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Sirsa, Haryana",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isStudioLight) Color(0xFF5F6368) else GoldLight.copy(alpha = 0.7f)
                    )
                }
            }
        },
        actions = {
            // Real-time Notifications & Announcements Inbox
            IconButton(
                onClick = onOpenNotifications,
                modifier = Modifier.testTag("notifications_bell_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotificationsCount > 0) {
                            Badge(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White,
                                modifier = Modifier.testTag("notifications_unread_badge")
                            ) {
                                Text(
                                    text = if (unreadNotificationsCount > 9) "9+" else unreadNotificationsCount.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = if (language == AppLanguage.HINDI) "सूचनाएं व अलर्ट" else "Notifications & Studio Alerts",
                        tint = if (unreadNotificationsCount > 0) GoldPrimary else if (isStudioLight) GoldPrimary else GoldLight
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // User Profile & Data Management
            IconButton(
                onClick = onOpenProfile,
                modifier = Modifier.testTag("user_profile_button")
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Artist Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, GoldPrimary, CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Artist Profile",
                        tint = if (isStudioLight) GoldPrimary else GoldLight
                    )
                }
            }

            // Privacy & Policy
            IconButton(
                onClick = onOpenPrivacy,
                modifier = Modifier.testTag("privacy_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Privacy Policy",
                    tint = if (isStudioLight) Color(0xFF5F6368) else GoldLight.copy(alpha = 0.8f)
                )
            }

            // Sign out
            IconButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("signout_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Sign Out",
                    tint = Color(0xFFE57373)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = StudioBlack,
            titleContentColor = GoldPrimary
        )
    )
}

@Composable
fun StudioBottomNavigation(
    currentScreen: StudioScreen,
    isAdmin: Boolean,
    language: AppLanguage,
    isStudioLight: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onNavigate: (StudioScreen) -> Unit,
    onToggleLanguage: () -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit = {}
) {
    val s = StudioStrings(language)
    Surface(
        color = StudioBlack,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = StudioOutline)
            .testTag("studio_bottom_nav_container")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Integrated Accessibility Bar: Language Toggle on left, Theme Mode on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isStudioLight) Color(0xFFF3F4F6) else Color(0xFF14120A))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("nav_language_toggle_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Language / भाषा",
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (language == AppLanguage.ENGLISH) "Lang:" else "भाषा:",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isStudioLight) Color(0xFF374151) else GoldLight,
                        fontWeight = FontWeight.Medium
                    )
                    LanguageToggleComponent(
                        currentLanguage = language,
                        onLanguageSelected = { selected ->
                            if (selected != language) {
                                onSelectLanguage(selected)
                            }
                        }
                    )
                }

                // Global Theme Mode Toggle Chip
                ThemeToggleComponent(
                    isStudioLight = isStudioLight,
                    onToggle = onToggleTheme
                )
            }

            // NavigationBar Items
            NavigationBar(
                containerColor = StudioBlack,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("studio_bottom_nav")
            ) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (isStudioLight) Color.White else DarkStudioBlack,
                    selectedTextColor = GoldPrimary,
                    indicatorColor = GoldPrimary,
                    unselectedIconColor = if (isStudioLight) Color(0xFF5F6368) else Color(0xFF909090),
                    unselectedTextColor = if (isStudioLight) Color(0xFF5F6368) else Color(0xFF909090)
                )

                NavigationBarItem(
                    selected = currentScreen is StudioScreen.Home,
                    onClick = { onNavigate(StudioScreen.Home) },
                    icon = { Icon(Icons.Default.Home, contentDescription = s.navHome) },
                    label = { Text(s.navHome, fontSize = 10.sp) },
                    colors = itemColors
                )

                NavigationBarItem(
                    selected = currentScreen is StudioScreen.ModelRegistration,
                    onClick = { onNavigate(StudioScreen.ModelRegistration) },
                    icon = { Icon(Icons.Default.Person, contentDescription = s.navModelReg) },
                    label = { Text(s.navModelReg, fontSize = 10.sp) },
                    colors = itemColors
                )

                NavigationBarItem(
                    selected = currentScreen is StudioScreen.ShootBooking,
                    onClick = { onNavigate(StudioScreen.ShootBooking) },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = s.navBooking) },
                    label = { Text(s.navBooking, fontSize = 10.sp) },
                    colors = itemColors
                )

                NavigationBarItem(
                    selected = currentScreen is StudioScreen.Gallery || currentScreen is StudioScreen.Portfolio,
                    onClick = { onNavigate(StudioScreen.Portfolio) },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = s.navGallery) },
                    label = { Text(s.navGallery, fontSize = 10.sp) },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_gallery").testTag("nav_portfolio")
                )

                NavigationBarItem(
                    selected = currentScreen is StudioScreen.AiStudio,
                    onClick = { onNavigate(StudioScreen.AiStudio) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = s.navAiStudio) },
                    label = { Text(s.navAiStudio, fontSize = 10.sp) },
                    colors = itemColors
                )

                NavigationBarItem(
                    selected = currentScreen is StudioScreen.Blog,
                    onClick = { onNavigate(StudioScreen.Blog) },
                    icon = { Icon(Icons.Default.Article, contentDescription = s.navBlog) },
                    label = { Text(s.navBlog, fontSize = 10.sp) },
                    colors = itemColors
                )

                if (isAdmin) {
                    NavigationBarItem(
                        selected = currentScreen is StudioScreen.AdminDashboard,
                        onClick = { onNavigate(StudioScreen.AdminDashboard) },
                        icon = {
                            BadgedBox(badge = { Badge { Text("★") } }) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = s.navAdmin)
                            }
                        },
                        label = { Text(s.navAdmin, fontSize = 10.sp) },
                        colors = itemColors
                    )
                }

                NavigationBarItem(
                    selected = currentScreen is StudioScreen.Contact,
                    onClick = { onNavigate(StudioScreen.Contact) },
                    icon = { Icon(Icons.Default.Call, contentDescription = s.navContact) },
                    label = { Text(s.navContact, fontSize = 10.sp) },
                    colors = itemColors
                )
            }
        }
    }
}
