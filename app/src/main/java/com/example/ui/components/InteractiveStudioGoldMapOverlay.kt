package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.util.StudioContactDetails
import kotlinx.coroutines.launch

/**
 * Themes for the custom interactive cartography overlay.
 */
enum class StudioGoldMapTheme {
    STUDIO_GOLD,
    NIGHT_RADAR
}

/**
 * Geographic landmark in the Ellenabad - Talwara Khurd - Sirsa cartography matrix.
 */
data class StudioMapLandmark(
    val id: String,
    val nameEn: String,
    val nameHi: String,
    val relativeOffset: Offset, // Relative to center (0,0 is Studio)
    val category: String,
    val isStudio: Boolean = false
)

/**
 * Interactive "Studio Gold" Map Overlay component.
 *
 * Provides:
 * 1. Custom Vector "Studio Gold" Cartography on obsidian luxury base
 * 2. Gesture-driven pan and pinch-to-zoom (clamped to prevent drifting off-map)
 * 3. On-screen 48dp zoom controls (+, -, re-center, theme switcher, expand toggle)
 * 4. Animated pulsating Gold Beacon pin on Star Artist Studio in Talwara Khurd
 * 5. Interactive Info Window overlay showing exact address, GPS coordinates,
 *    and quick actions to get directions or call the studio.
 */
@Composable
fun InteractiveStudioGoldMapOverlay(
    modifier: Modifier = Modifier,
    language: AppLanguage = AppLanguage.ENGLISH,
    initialExpanded: Boolean = false,
    onExpandChanged: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Map gesture state: Zoom scale (0.65x to 3.2x) and pan offset
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Display mode
    var mapTheme by remember { mutableStateOf(StudioGoldMapTheme.STUDIO_GOLD) }
    var isExpanded by remember { mutableStateOf(initialExpanded) }
    var isInfoWindowVisible by remember { mutableStateOf(true) }

    // Animated zoom for smooth button interactions
    val animatedZoom by animateFloatAsState(
        targetValue = zoomScale,
        animationSpec = spring(stiffness = 400f),
        label = "animated_zoom"
    )

    // Pulsing beacon ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "studio_beacon")
    val beaconRadiusMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_radius"
    )
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )

    // Landmarks around Star Artist Studio (Talwara Khurd, Ellenabad, Sirsa)
    val landmarks = remember {
        listOf(
            StudioMapLandmark(
                id = "studio",
                nameEn = "Star Artist Studio",
                nameHi = "स्टार आर्टिस्ट स्टूडियो",
                relativeOffset = Offset(0f, 0f),
                category = "Main Studio",
                isStudio = true
            ),
            StudioMapLandmark(
                id = "talwara_bus",
                nameEn = "Talwara Khurd Bus Stand",
                nameHi = "तलवाड़ा खुर्द बस स्टैंड",
                relativeOffset = Offset(-25f, -80f),
                category = "Transit"
            ),
            StudioMapLandmark(
                id = "ellenabad_town",
                nameEn = "Tehsil Ellenabad (~8km)",
                nameHi = "तहसील ऐलनाबाद (~8 किमी)",
                relativeOffset = Offset(-180f, 40f),
                category = "Town Center"
            ),
            StudioMapLandmark(
                id = "sirsa_road",
                nameEn = "Sirsa Highway Link",
                nameHi = "सिरसा हाईवे लिंक",
                relativeOffset = Offset(170f, -90f),
                category = "Highway"
            ),
            StudioMapLandmark(
                id = "ghaggar_canal",
                nameEn = "Ghaggar Feeder Canal",
                nameHi = "घग्गर फीडर नहर",
                relativeOffset = Offset(60f, 130f),
                category = "Waterway"
            )
        )
    }

    var selectedLandmark by remember { mutableStateOf<StudioMapLandmark?>(landmarks.first()) }

    val mapHeight = if (isExpanded) 420.dp else 250.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        GoldPrimary.copy(alpha = 0.85f),
                        StudioOutline,
                        GoldPrimary.copy(alpha = 0.45f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .testTag("interactive_studio_gold_map_overlay"),
        colors = CardDefaults.cardColors(containerColor = StudioBlack),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // =================================================================
            // 1. TOP HEADER / TELEMETRY STATUS BAR
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141008))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (mapTheme == StudioGoldMapTheme.STUDIO_GOLD) GoldPrimary else Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (language == AppLanguage.HINDI) "स्टूडियो गोल्ड मैप ओवरले" else "STUDIO GOLD MAP OVERLAY",
                            color = GoldLight,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "29.4500° N • 74.7500° E • Talwara Khurd, Sirsa",
                            color = Color(0xFFAAAAAA),
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Theme Mode & Expand Toggle buttons
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Theme Switcher Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StudioElevatedSurface)
                            .border(1.dp, StudioOutline, RoundedCornerShape(8.dp))
                            .clickable {
                                mapTheme = if (mapTheme == StudioGoldMapTheme.STUDIO_GOLD) {
                                    StudioGoldMapTheme.NIGHT_RADAR
                                } else {
                                    StudioGoldMapTheme.STUDIO_GOLD
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("map_theme_toggle_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Toggle Map Theme",
                                tint = GoldPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (mapTheme == StudioGoldMapTheme.STUDIO_GOLD) "GOLD" else "RADAR",
                                color = GoldLight,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Expand / Collapse Map View Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldPrimary.copy(alpha = 0.18f))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable {
                                isExpanded = !isExpanded
                                onExpandChanged?.invoke(isExpanded)
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("map_expand_toggle_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse Map" else "Expand Map",
                                tint = GoldPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isExpanded) "MIN" else "EXPAND",
                                color = GoldPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 2. INTERACTIVE CANVAS VIEW WITH PAN & ZOOM GESTURES
            // =================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight)
                    .background(if (mapTheme == StudioGoldMapTheme.STUDIO_GOLD) Color(0xFF0C0904) else Color(0xFF05080A))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                // Double tap resets or toggles zoom
                                if (zoomScale > 1.2f) {
                                    zoomScale = 1.0f
                                    panOffset = Offset.Zero
                                } else {
                                    zoomScale = 1.8f
                                }
                            },
                            onTap = {
                                isInfoWindowVisible = true
                                selectedLandmark = landmarks.first()
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newZoom = (zoomScale * zoom).coerceIn(0.65f, 3.2f)
                            zoomScale = newZoom
                            // Clamp pan boundaries relative to zoom
                            val maxPan = 450f * newZoom
                            panOffset = Offset(
                                (panOffset.x + pan.x).coerceIn(-maxPan, maxPan),
                                (panOffset.y + pan.y).coerceIn(-maxPan, maxPan)
                            )
                        }
                    }
                    .testTag("interactive_map_canvas_box")
            ) {
                // Procedural Cartography Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val baseCenter = Offset(w * 0.5f, h * 0.48f)
                    val mapCenter = baseCenter + panOffset

                    // Draw themed background grid and artwork
                    drawMapCartography(
                        mapCenter = mapCenter,
                        zoom = animatedZoom,
                        theme = mapTheme,
                        beaconRadiusMultiplier = beaconRadiusMultiplier,
                        beaconAlpha = beaconAlpha,
                        landmarks = landmarks
                    )
                }

                // =============================================================
                // 3. FLOATING ON-SCREEN INTERACTIVE CONTROLS (Right Side)
                // =============================================================
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Zoom In Button (+)
                    Surface(
                        onClick = {
                            val nextZoom = (zoomScale + 0.35f).coerceAtMost(3.2f)
                            zoomScale = nextZoom
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_zoom_in_button"),
                        shape = CircleShape,
                        color = Color(0xFF1E170A).copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldPrimary),
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Zoom In",
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Zoom Out Button (-)
                    Surface(
                        onClick = {
                            val nextZoom = (zoomScale - 0.35f).coerceAtLeast(0.65f)
                            zoomScale = nextZoom
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_zoom_out_button"),
                        shape = CircleShape,
                        color = Color(0xFF1E170A).copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldPrimary),
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Zoom Out",
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Re-Center Studio Button (⌖)
                    Surface(
                        onClick = {
                            zoomScale = 1.0f
                            panOffset = Offset.Zero
                            selectedLandmark = landmarks.first()
                            isInfoWindowVisible = true
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_recenter_button"),
                        shape = CircleShape,
                        color = GoldPrimary,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, StudioBlack),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Re-center Star Artist Studio",
                                tint = StudioBlack,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Info Window Toggle Button
                    Surface(
                        onClick = {
                            isInfoWindowVisible = !isInfoWindowVisible
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_info_toggle_button"),
                        shape = CircleShape,
                        color = if (isInfoWindowVisible) GoldDark else Color(0xFF1E170A).copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Toggle Studio Information",
                                tint = GoldLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // =============================================================
                // 4. FLOATING STUDIO BEACON BADGE & PIN (Center Overlay)
                // =============================================================
                val studioPointOffset = Offset(
                    x = 0f * animatedZoom + panOffset.x,
                    y = 0f * animatedZoom + panOffset.y
                )

                // Scale indicator HUD (Bottom Left)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = if (isInfoWindowVisible) 130.dp else 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioBlack.copy(alpha = 0.8f))
                        .border(0.8.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ZOOM: ${String.format("%.1f", animatedZoom)}x • 1cm ≈ ${(500 / animatedZoom).toInt()}m",
                        color = GoldLight,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }

                // =============================================================
                // 5. INTERACTIVE STUDIO INFO WINDOW OVERLAY
                // =============================================================
                androidx.compose.animation.AnimatedVisibility(
                    visible = isInfoWindowVisible,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(14.dp))
                            .border(
                                1.2.dp,
                                Brush.horizontalGradient(
                                    listOf(GoldPrimary, GoldLight, GoldPrimary)
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .testTag("studio_map_info_window"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A140A).copy(alpha = 0.96f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Header Row with Close
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(GoldPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = StudioBlack,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = StudioContactDetails.STUDIO_NAME,
                                            color = GoldLight,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.5.sp
                                        )
                                        Text(
                                            text = if (language == AppLanguage.HINDI)
                                                "तलवाड़ा खुर्द, ऐलनाबाद, सिरसा (125102)"
                                            else
                                                "Talwara Khurd, Ellenabad, Sirsa (125102)",
                                            color = Color(0xFFCCCCCC),
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { isInfoWindowVisible = false },
                                    modifier = Modifier.size(28.dp),
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = GoldLight)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss Info Window",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Exact Invariant Address String
                            Text(
                                text = StudioContactDetails.ADDRESS,
                                color = Color.White,
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Normal
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick Action Buttons (Directions, Call, Copy GPS)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Directions Button
                                Button(
                                    onClick = { StudioContactDetails.openStudioMaps(context) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("map_overlay_get_directions_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldPrimary,
                                        contentColor = StudioBlack
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = "Navigate",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "दिशानिर्देश" else "Directions",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                }

                                // Direct Call Button
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse(StudioContactDetails.CALL_URI))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("map_overlay_call_button"),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call Studio",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "कॉल करें" else "Call",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                }

                                // Copy GPS Coordinates Button
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Studio GPS", "29.4500,74.7500")
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "GPS Coordinates Copied (29.4500, 74.7500)", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .height(42.dp)
                                        .testTag("map_overlay_copy_gps_button"),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioOutline),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy GPS Coordinates",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 6. BOTTOM CARRIER STRIP (Landmarks Guide & Instructions)
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF110E06))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (language == AppLanguage.HINDI)
                        "नेविगेट करने के लिए खींचें और पिंच-टू-ज़ूम करें"
                    else
                        "Pan & pinch-to-zoom to explore studio surroundings",
                    color = Color(0xFFAAAAAA),
                    fontSize = 10.sp
                )

                Text(
                    text = "Haryana • 125102",
                    color = GoldPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Draws the customized "Studio Gold" Vector Cartography on the Compose Canvas.
 */
private fun DrawScope.drawMapCartography(
    mapCenter: Offset,
    zoom: Float,
    theme: StudioGoldMapTheme,
    beaconRadiusMultiplier: Float,
    beaconAlpha: Float,
    landmarks: List<StudioMapLandmark>
) {
    val w = size.width
    val h = size.height

    val isGold = theme == StudioGoldMapTheme.STUDIO_GOLD
    val gridColor = if (isGold) Color(0xFF261E0E) else Color(0xFF0F1E19)
    val arterialRoadColor = if (isGold) Color(0xFFD4AF37) else Color(0xFF10B981)
    val arterialRoadEdge = if (isGold) Color(0xFF7A601E) else Color(0xFF064E3B)
    val minorRoadColor = if (isGold) Color(0xFF4D3D1D) else Color(0xFF132B24)
    val canalColor = if (isGold) Color(0xFF17362E) else Color(0xFF0B3A42)
    val canalEdgeColor = if (isGold) Color(0xFF2D594C) else Color(0xFF14B8A6)
    val studioGoldAccent = Color(0xFFE5C158)

    // 1. Cartographic Coordinate Grid
    val gridSize = 45f * zoom
    var currX = (mapCenter.x % gridSize)
    while (currX < w) {
        drawLine(
            color = gridColor,
            start = Offset(currX, 0f),
            end = Offset(currX, h),
            strokeWidth = 1f
        )
        currX += gridSize
    }
    var currY = (mapCenter.y % gridSize)
    while (currY < h) {
        drawLine(
            color = gridColor,
            start = Offset(0f, currY),
            end = Offset(w, currY),
            strokeWidth = 1f
        )
        currY += gridSize
    }

    // 2. Ghaggar Feeder Canal / Water Branch (Curving through region)
    val canalPath = Path().apply {
        val startY = mapCenter.y + 160f * zoom
        moveTo(0f, startY)
        cubicTo(
            mapCenter.x - 120f * zoom, startY - 20f * zoom,
            mapCenter.x + 80f * zoom, startY + 40f * zoom,
            w, startY - 40f * zoom
        )
    }
    drawPath(canalPath, color = canalColor, style = Stroke(width = 16f * zoom))
    drawPath(canalPath, color = canalEdgeColor, style = Stroke(width = 2f * zoom))

    // 3. Railway Line (Ellenabad - Sirsa track with cross-ties)
    val railY = mapCenter.y - 120f * zoom
    drawLine(
        color = Color(0xFF4A4234),
        start = Offset(0f, railY),
        end = Offset(w, railY),
        strokeWidth = 4f * zoom
    )
    val crossTieSpacing = 20f * zoom
    var tieX = 0f
    while (tieX < w) {
        drawLine(
            color = Color(0xFF8A7A5D),
            start = Offset(tieX, railY - 6f * zoom),
            end = Offset(tieX, railY + 6f * zoom),
            strokeWidth = 2f * zoom
        )
        tieX += crossTieSpacing
    }

    // 4. Secondary Village Arteries & Feeder Lanes
    // North-South connecting road
    drawLine(
        color = minorRoadColor,
        start = Offset(mapCenter.x - 30f * zoom, 0f),
        end = Offset(mapCenter.x - 30f * zoom, h),
        strokeWidth = 5f * zoom
    )
    // Talwara Khurd village access loop
    val villageLoop = Path().apply {
        moveTo(mapCenter.x - 30f * zoom, mapCenter.y - 80f * zoom)
        cubicTo(
            mapCenter.x + 60f * zoom, mapCenter.y - 70f * zoom,
            mapCenter.x + 90f * zoom, mapCenter.y + 30f * zoom,
            mapCenter.x, mapCenter.y
        )
    }
    drawPath(villageLoop, color = minorRoadColor, style = Stroke(width = 4.5f * zoom))

    // 5. Major State Highway: Ellenabad - Sirsa Road (SH-23 corridor)
    val arterialPath = Path().apply {
        val y1 = mapCenter.y + 90f * zoom
        val y2 = mapCenter.y - 60f * zoom
        moveTo(0f, y1)
        cubicTo(
            mapCenter.x * 0.4f, y1 - 20f * zoom,
            mapCenter.x * 0.8f, mapCenter.y + 15f * zoom,
            mapCenter.x, mapCenter.y
        )
        cubicTo(
            mapCenter.x * 1.2f, mapCenter.y - 15f * zoom,
            mapCenter.x * 1.6f, y2 + 20f * zoom,
            w, y2
        )
    }
    // Road Casing
    drawPath(arterialPath, color = arterialRoadEdge, style = Stroke(width = 11f * zoom))
    // Road Surface
    drawPath(arterialPath, color = arterialRoadColor, style = Stroke(width = 7.5f * zoom))
    // Dashed center line
    drawPath(
        arterialPath,
        color = Color(0xFF141005),
        style = Stroke(
            width = 1.8f * zoom,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f * zoom, 8f * zoom), 0f)
        )
    )

    // 6. Star Artist Studio Campus Perimeter
    val studioGroundsWidth = 90f * zoom
    val studioGroundsHeight = 60f * zoom
    val studioBounds = androidx.compose.ui.geometry.Rect(
        offset = Offset(mapCenter.x - studioGroundsWidth * 0.5f, mapCenter.y - studioGroundsHeight * 0.5f),
        size = Size(studioGroundsWidth, studioGroundsHeight)
    )
    drawRoundRect(
        color = Color(0x33D4AF37),
        topLeft = studioBounds.topLeft,
        size = studioBounds.size,
        cornerRadius = CornerRadius(8f * zoom, 8f * zoom)
    )
    drawRoundRect(
        color = studioGoldAccent,
        topLeft = studioBounds.topLeft,
        size = studioBounds.size,
        cornerRadius = CornerRadius(8f * zoom, 8f * zoom),
        style = Stroke(width = 1.8f * zoom)
    )

    // 7. Surrounding Landmark Nodes
    landmarks.filter { !it.isStudio }.forEach { landmark ->
        val nodePos = mapCenter + (landmark.relativeOffset * zoom)
        if (nodePos.x in -50f..(w + 50f) && nodePos.y in -50f..(h + 50f)) {
            drawCircle(
                color = Color(0xFF6E5624),
                radius = 5f * zoom,
                center = nodePos
            )
            drawCircle(
                color = Color(0xFFD4AF37),
                radius = 3f * zoom,
                center = nodePos
            )
        }
    }

    // 8. Animated Pulsating Studio Radar Beacon
    drawCircle(
        color = studioGoldAccent.copy(alpha = beaconAlpha * 0.35f),
        radius = 75f * zoom * beaconRadiusMultiplier,
        center = mapCenter
    )
    drawCircle(
        color = studioGoldAccent.copy(alpha = beaconAlpha * 0.75f),
        radius = 48f * zoom * beaconRadiusMultiplier,
        center = mapCenter,
        style = Stroke(width = 2.2f * zoom)
    )
    drawCircle(
        color = studioGoldAccent.copy(alpha = 0.5f),
        radius = 26f * zoom,
        center = mapCenter,
        style = Stroke(width = 2.5f * zoom)
    )

    // Center Golden Beacon Core
    drawCircle(
        color = studioGoldAccent,
        radius = 8f * zoom,
        center = mapCenter
    )
    drawCircle(
        color = Color(0xFF0C0C0C),
        radius = 4f * zoom,
        center = mapCenter
    )
}
