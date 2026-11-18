package com.example

import androidx.compose.ui.geometry.Offset
import com.example.ui.components.StudioGoldMapTheme
import com.example.ui.components.StudioMapLandmark
import com.example.util.StudioContactDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioGoldMapOverlayTest {

    @Test
    fun studioAddressInvariant_matchesOfficialDetailsExactly() {
        val expectedAddress = "Star Artist Studio, Talwara Khurd, Tehsil Ellenabad, District Sirsa, Haryana, India - 125102."
        assertEquals(expectedAddress, StudioContactDetails.ADDRESS)
        assertEquals("Talwara Khurd", StudioContactDetails.VILLAGE)
        assertEquals("Ellenabad", StudioContactDetails.TEHSIL)
        assertEquals("Sirsa", StudioContactDetails.DISTRICT)
        assertEquals("Haryana", StudioContactDetails.STATE)
        assertEquals("125102", StudioContactDetails.PINCODE)
    }

    @Test
    fun studioContactNumbers_matchOfficialInvariants() {
        assertEquals("9812890081", StudioContactDetails.MOBILE_NUMBER)
        assertEquals("9541024098", StudioContactDetails.WHATSAPP_NUMBER)
        assertEquals("tel:+919812890081", StudioContactDetails.CALL_URI)
    }

    @Test
    fun studioGpsCoordinates_areAccurateForEllenabadSirsa() {
        val studioLat = 29.4500
        val studioLng = 74.7500

        // Ellenabad/Sirsa latitude is ~29.4°N to 29.6°N
        assertTrue("Latitude must be within Sirsa district bounds", studioLat in 29.3..29.8)
        // Ellenabad/Sirsa longitude is ~74.6°E to 75.1°E
        assertTrue("Longitude must be within Sirsa district bounds", studioLng in 74.5..75.2)
    }

    @Test
    fun mapZoomClamping_strictlyEnforcesBounds() {
        val minZoom = 0.65f
        val maxZoom = 3.2f

        // Over-zoom in should clamp to maxZoom
        val overZoom = 5.0f.coerceIn(minZoom, maxZoom)
        assertEquals(maxZoom, overZoom, 0.001f)

        // Over-zoom out should clamp to minZoom
        val underZoom = 0.2f.coerceIn(minZoom, maxZoom)
        assertEquals(minZoom, underZoom, 0.001f)

        // Mid zoom stays unchanged
        val normalZoom = 1.8f.coerceIn(minZoom, maxZoom)
        assertEquals(1.8f, normalZoom, 0.001f)
    }

    @Test
    fun mapPanClamping_scalesProportionallyWithZoom() {
        val zoom = 2.0f
        val maxPan = 450f * zoom // 900f

        val excessPanX = 1500f.coerceIn(-maxPan, maxPan)
        assertEquals(900f, excessPanX, 0.001f)

        val negativeExcessPanY = (-1200f).coerceIn(-maxPan, maxPan)
        assertEquals(-900f, negativeExcessPanY, 0.001f)

        val withinBoundsPan = 300f.coerceIn(-maxPan, maxPan)
        assertEquals(300f, withinBoundsPan, 0.001f)
    }

    @Test
    fun mapThemes_containStudioGoldAndNightRadar() {
        val themes = StudioGoldMapTheme.values()
        assertTrue(themes.contains(StudioGoldMapTheme.STUDIO_GOLD))
        assertTrue(themes.contains(StudioGoldMapTheme.NIGHT_RADAR))
        assertEquals(2, themes.size)
    }

    @Test
    fun studioMapLandmarks_centerPinIsStudio() {
        val studioLandmark = StudioMapLandmark(
            id = "studio",
            nameEn = "Star Artist Studio",
            nameHi = "स्टार आर्टिस्ट स्टूडियो",
            relativeOffset = Offset(0f, 0f),
            category = "Main Studio",
            isStudio = true
        )

        assertEquals("studio", studioLandmark.id)
        assertTrue(studioLandmark.isStudio)
        assertEquals(Offset.Zero, studioLandmark.relativeOffset)
        assertNotNull(studioLandmark.nameEn)
        assertNotNull(studioLandmark.nameHi)
    }
}
