package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Universal content sharing helper for STAR ARTIST STUDIO.
 * Dispatches native Android Intent.ACTION_SEND chooser to share studio creative works,
 * service descriptions, and AI-generated concepts to WhatsApp, Instagram, and social channels.
 */
object ShareHelper {

    fun shareContent(
        context: Context,
        title: String,
        text: String
    ) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share via Star Artist Studio")
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share menu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareGalleryItem(
        context: Context,
        title: String,
        category: String,
        mediaType: String,
        mediaUrl: String
    ) {
        val shareBody = buildString {
            appendLine("🌟 $title")
            appendLine("Category: $category • Format: ${mediaType.uppercase()}")
            if (mediaUrl.isNotBlank() && !mediaUrl.startsWith("drawable://")) {
                appendLine("Portfolio Media: $mediaUrl")
            }
            appendLine()
            appendLine("Produced at STAR ARTIST STUDIO")
            appendLine("Talwara Khurd, Sirsa, Haryana - 125102")
            appendLine("Bookings & Enquiries: +91 ${StudioContactDetails.MOBILE_NUMBER}")
        }
        shareContent(context, title, shareBody)
    }

    fun shareService(
        context: Context,
        title: String,
        description: String,
        badge: String
    ) {
        val shareBody = buildString {
            appendLine("✨ $title [$badge]")
            appendLine(description)
            appendLine()
            appendLine("Produced at STAR ARTIST STUDIO")
            appendLine("Talwara Khurd, Sirsa, Haryana")
            appendLine("Contact: +91 ${StudioContactDetails.MOBILE_NUMBER} | WhatsApp: +91 ${StudioContactDetails.WHATSAPP_NUMBER}")
        }
        shareContent(context, title, shareBody)
    }

    fun shareAiConcept(
        context: Context,
        title: String,
        category: String,
        conceptContent: String
    ) {
        val shareBody = buildString {
            appendLine("🎬 STAR ARTIST STUDIO AI Creative Concept ($category)")
            appendLine("Title: $title")
            appendLine("----------------------------------------")
            appendLine(conceptContent)
            appendLine("----------------------------------------")
            appendLine("Executive Production Guide by STAR ARTIST STUDIO")
            appendLine("Talwara Khurd, Sirsa, Haryana • Call +91 ${StudioContactDetails.MOBILE_NUMBER}")
        }
        shareContent(context, title, shareBody)
    }
}
