package com.example

import com.example.ui.components.SCROLL_TO_TOP_THRESHOLD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollToTopFabTest {

    @Test
    fun scrollToTopThreshold_isConfiguredToTwo() {
        assertEquals(2, SCROLL_TO_TOP_THRESHOLD)
    }

    @Test
    fun visibilityCalculation_matchesScrollThresholds() {
        // Top of page (index 0, offset 0) -> FAB hidden
        val atTopIndex = 0
        val atTopOffset = 0
        val isVisibleAtTop = atTopIndex >= SCROLL_TO_TOP_THRESHOLD ||
                (atTopIndex == 1 && atTopOffset > 250)
        assertFalse("FAB must be hidden when at index 0", isVisibleAtTop)

        // Slight scroll (index 1, small offset) -> FAB hidden
        val item1SmallOffset = 100
        val isVisibleItem1Small = 1 >= SCROLL_TO_TOP_THRESHOLD ||
                (1 == 1 && item1SmallOffset > 250)
        assertFalse("FAB must remain hidden for small offset on item 1", isVisibleItem1Small)

        // Deeper scroll on item 1 (> 250px) -> FAB becomes visible
        val item1DeepOffset = 300
        val isVisibleItem1Deep = 1 >= SCROLL_TO_TOP_THRESHOLD ||
                (1 == 1 && item1DeepOffset > 250)
        assertTrue("FAB must become visible once scrolled past 250px on item 1", isVisibleItem1Deep)

        // Scrolled to item 2 or beyond -> FAB always visible
        val item2Visible = 2 >= SCROLL_TO_TOP_THRESHOLD
        assertTrue("FAB must be visible on item index >= threshold (2)", item2Visible)

        val item5Visible = 5 >= SCROLL_TO_TOP_THRESHOLD
        assertTrue("FAB must be visible on deeper scroll items", item5Visible)
    }
}
