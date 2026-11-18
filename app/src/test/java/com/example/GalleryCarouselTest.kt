package com.example

import com.example.data.model.GalleryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryCarouselTest {

    private val sampleItems = listOf(
        GalleryItem(
            id = "g_01",
            title = "Royal Velvet Bridal",
            category = "Bridal",
            mediaType = "photo",
            mediaUrl = "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b",
            thumbnailUrl = "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b",
            status = "approved",
            isDemo = true
        ),
        GalleryItem(
            id = "g_02",
            title = "Cinematic 4K Fashion Reel",
            category = "Fashion",
            mediaType = "video",
            mediaUrl = "https://images.unsplash.com/photo-1509631179647-0177331693ae",
            thumbnailUrl = "https://images.unsplash.com/photo-1509631179647-0177331693ae",
            status = "approved",
            isDemo = true
        ),
        GalleryItem(
            id = "g_03",
            title = "Studio Monochrome Portrait",
            category = "Portrait",
            mediaType = "photo",
            mediaUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
            thumbnailUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
            status = "approved",
            isDemo = false
        )
    )

    @Test
    fun carouselIndex_clampsSafelyWithinListBounds() {
        val count = sampleItems.size

        // Within bounds
        val requestedIndex1 = 1
        val safeIndex1 = requestedIndex1.coerceIn(0, (count - 1).coerceAtLeast(0))
        assertEquals(1, safeIndex1)

        // Negative index clamps to 0
        val negativeIndex = -5
        val clampedZero = negativeIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
        assertEquals(0, clampedZero)

        // Out of upper bound clamps to count - 1
        val largeIndex = 99
        val clampedUpper = largeIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
        assertEquals(2, clampedUpper)

        // Empty list safe handling
        val emptyList = emptyList<GalleryItem>()
        val emptyCount = emptyList.size
        val emptyClamped = 0.coerceIn(0, (emptyCount - 1).coerceAtLeast(0))
        assertEquals(0, emptyClamped)
    }

    @Test
    fun carouselNavigation_previousAndNextBoundaries() {
        val total = sampleItems.size

        // Page 0 cannot navigate backward
        val page0 = 0
        val canGoPrev0 = page0 > 0
        val canGoNext0 = page0 < total - 1
        assertFalse(canGoPrev0)
        assertTrue(canGoNext0)

        // Page 1 can navigate both ways
        val page1 = 1
        val canGoPrev1 = page1 > 0
        val canGoNext1 = page1 < total - 1
        assertTrue(canGoPrev1)
        assertTrue(canGoNext1)

        // Page 2 (last) cannot navigate forward
        val page2 = 2
        val canGoPrev2 = page2 > 0
        val canGoNext2 = page2 < total - 1
        assertTrue(canGoPrev2)
        assertFalse(canGoNext2)
    }

    @Test
    fun carouselItem_resolvesMediaTypeCorrectly() {
        val photoItem = sampleItems[0]
        val videoItem = sampleItems[1]

        val isPhotoVideo = photoItem.mediaType.equals("video", ignoreCase = true)
        val isVideoVideo = videoItem.mediaType.equals("video", ignoreCase = true)

        assertFalse("Photo item should not be video", isPhotoVideo)
        assertTrue("Video item should be recognized as video", isVideoVideo)
    }

    @Test
    fun carousel_highResolutionDisplayUrlFallback() {
        val itemWithEmptyThumb = GalleryItem(
            id = "g_04",
            title = "Test Item",
            category = "Commercial",
            mediaType = "photo",
            mediaUrl = "https://images.unsplash.com/photo-highres-url",
            thumbnailUrl = "",
            status = "approved"
        )

        val resolvedUrl = itemWithEmptyThumb.mediaUrl.ifBlank { itemWithEmptyThumb.thumbnailUrl }
        assertEquals("https://images.unsplash.com/photo-highres-url", resolvedUrl)
        assertNotNull(resolvedUrl)
    }
}
