package com.example

import com.example.data.model.ServiceReview
import com.example.ui.components.calculateAverageReviewRating
import com.example.ui.components.validateServiceReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ServiceReviewTest {

    @Test
    fun validateServiceReview_validInput_returnsNull() {
        assertNull(validateServiceReview(5, "Great photography session and lighting setup!"))
        assertNull(validateServiceReview(1, "Needs improvement in timing."))
    }

    @Test
    fun validateServiceReview_invalidRating_returnsError() {
        assertNotNull(validateServiceReview(0, "Valid comment length here."))
        assertNotNull(validateServiceReview(6, "Valid comment length here."))
    }

    @Test
    fun validateServiceReview_shortOrExcessiveComment_returnsError() {
        assertNotNull(validateServiceReview(5, "Good"))
        assertNotNull(validateServiceReview(5, "   "))
        val tooLong = "a".repeat(1001)
        assertNotNull(validateServiceReview(5, tooLong))
    }

    @Test
    fun calculateAverageReviewRating_computesAccurateMean() {
        assertEquals(0.0, calculateAverageReviewRating(emptyList()), 0.001)

        val reviews = listOf(
            ServiceReview(id = "1", rating = 5L),
            ServiceReview(id = "2", rating = 4L),
            ServiceReview(id = "3", rating = 5L)
        )
        assertEquals(4.666, calculateAverageReviewRating(reviews), 0.01)
    }
}
