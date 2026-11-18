package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Official Studio Contact Details and Constants for STAR ARTIST STUDIO.
 * Use this object across the entire application to ensure 100% consistent usage
 * of official studio information.
 */
object StudioContactDetails {
    // Official Studio Identity
    const val STUDIO_NAME = "STAR ARTIST STUDIO"
    const val TAGLINE = "Premium Modeling, Photography & 4K Cinema Video Production Studio"

    // Official Address
    const val ADDRESS = "Star Artist Studio, Talwara Khurd, Tehsil Ellenabad, District Sirsa, Haryana, India - 125102."
    const val VILLAGE = "Talwara Khurd"
    const val TEHSIL = "Ellenabad"
    const val DISTRICT = "Sirsa"
    const val STATE = "Haryana"
    const val COUNTRY = "India"
    const val PINCODE = "125102"

    // Official Contact Numbers
    const val MOBILE_NUMBER = "9812890081"
    const val FORMATTED_MOBILE = "+91 9812890081"
    const val CALL_URI = "tel:+919812890081"

    const val WHATSAPP_NUMBER = "9541024098"
    const val FORMATTED_WHATSAPP = "+91 9541024098"
    const val WHATSAPP_URL = "https://wa.me/919541020948"

    // Administrative Details
    const val ADMIN_EMAIL = "chanderstudio0@gmail.com"
    const val STUDIO_HOURS = "10:00 AM - 8:00 PM (Everyday)"

    // Official Social Media Profiles
    const val INSTAGRAM_URL = "https://www.instagram.com/starartiststudio"
    const val FACEBOOK_URL = "https://www.facebook.com/starartiststudio"
    const val LINKEDIN_URL = "https://www.linkedin.com/company/star-artist-studio"

    /**
     * Opens an external web or social profile link safely.
     */
    fun openExternalUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Safely ignore if no browser/app handler is available
        }
    }

    /**
     * Builds a WhatsApp direct message URL with prefilled text
     */
    fun buildWhatsAppUrl(message: String = ""): String {
        return if (message.isBlank()) {
            WHATSAPP_URL
        } else {
            "https://wa.me/919541020948?text=${Uri.encode(message)}"
        }
    }

    /**
     * Launches the device's map application with the studio location.
     * Uses geo URI with fallback to Google Maps web search.
     */
    fun openStudioMaps(context: Context) {
        val geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(ADDRESS))
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val fallbackUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(ADDRESS))
            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
        }
    }
}
