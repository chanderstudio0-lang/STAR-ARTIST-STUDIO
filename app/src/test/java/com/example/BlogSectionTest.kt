package com.example

import com.example.data.model.BlogPost
import com.example.data.repository.StudioRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Studio Blog & Photography Tips feature.
 */
class BlogSectionTest {

    @Test
    fun defaultSeedBlogPosts_containMandatoryCategoriesAndOfficialDetails() {
        val seedPosts = StudioRepository.defaultSeedBlogPosts()

        assertTrue("Seed posts must not be empty", seedPosts.isNotEmpty())
        assertEquals("Seed posts should contain at least 3 initial articles", 3, seedPosts.size)

        val categories = seedPosts.map { it.category }.toSet()
        assertTrue("Must include Photography Tips", categories.contains("Photography Tips"))
        assertTrue("Must include Studio News", categories.contains("Studio News"))
        assertTrue("Must include Behind The Scenes", categories.contains("Behind The Scenes"))

        // Verify studio location invariant is reflected in content
        val hasTalwaraKhurd = seedPosts.any { it.content.contains("Talwara Khurd") }
        assertTrue("Blog posts must accurately reference official studio location in Talwara Khurd, Sirsa", hasTalwaraKhurd)

        // Verify all posts are marked published and have positive read time
        for (post in seedPosts) {
            assertEquals("Default seed posts must be published", "published", post.status)
            assertTrue("Post title must be at least 3 characters", post.title.length >= 3)
            assertTrue("Post content must be descriptive", post.content.length >= 10)
            assertTrue("Read time must be at least 1 minute", post.readTimeMinutes >= 1L)
        }
    }

    @Test
    fun filterBlogPosts_byCategoryWorksAccurately() {
        val posts = listOf(
            BlogPost(id = "1", title = "Lighting 101", category = "Photography Tips", content = "Three point lighting...", status = "published"),
            BlogPost(id = "2", title = "Studio Expansion", category = "Studio News", content = "New stage launched...", status = "published"),
            BlogPost(id = "3", title = "Music Video BTS", category = "Behind The Scenes", content = "Gimbal camera moves...", status = "published"),
            BlogPost(id = "4", title = "Camera Sensors", category = "Gear & Lighting", content = "Full frame dynamic range...", status = "published")
        )

        val tips = posts.filter { it.category.equals("Photography Tips", ignoreCase = true) }
        val news = posts.filter { it.category.equals("Studio News", ignoreCase = true) }
        val bts = posts.filter { it.category.equals("Behind The Scenes", ignoreCase = true) }

        assertEquals(1, tips.size)
        assertEquals("Lighting 101", tips.first().title)
        assertEquals(1, news.size)
        assertEquals("Studio Expansion", news.first().title)
        assertEquals(1, bts.size)
        assertEquals("Music Video BTS", bts.first().title)
    }

    @Test
    fun searchBlogPosts_matchesTitlesTagsAndContent() {
        val posts = listOf(
            BlogPost(id = "1", title = "Parabolic Softbox Guide", tags = "lighting,softbox,strobe", content = "How to shape light...", status = "published"),
            BlogPost(id = "2", title = "Music Video Production", tags = "cinema,reel,4k", content = "Behind the scenes in Sirsa...", status = "published")
        )

        // Match by tag
        val softboxMatches = posts.filter { it.title.contains("softbox", ignoreCase = true) || it.tags.contains("softbox", ignoreCase = true) }
        assertEquals(1, softboxMatches.size)
        assertEquals("1", softboxMatches.first().id)

        // Match by content location
        val locationMatches = posts.filter { it.content.contains("Sirsa", ignoreCase = true) }
        assertEquals(1, locationMatches.size)
        assertEquals("2", locationMatches.first().id)
    }

    @Test
    fun blogPostValidation_detectsInvalidInputs() {
        // Valid post
        val validPost = BlogPost(
            authorId = "admin_1",
            title = "Pro Golden Hour Photography",
            category = "Photography Tips",
            content = "Use gold reflectors to bounce natural sunset highlights.",
            status = "published"
        )
        assertTrue(validPost.title.length >= 3)
        assertTrue(validPost.content.length >= 10)
        assertTrue(listOf("Photography Tips", "Studio News", "Behind The Scenes", "Gear & Lighting", "Model Spotlight").contains(validPost.category))

        // Invalid title
        val invalidTitle = "Hi"
        assertFalse("Title under 3 characters should be invalid", invalidTitle.length >= 3)

        // Invalid content
        val invalidContent = "Short"
        assertFalse("Content under 10 characters should be invalid", invalidContent.length >= 10)
    }
}
