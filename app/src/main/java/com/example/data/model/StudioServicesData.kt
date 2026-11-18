package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Service package category enumeration
 */
enum class ServicePackageCategory(val id: String, val titleEn: String, val titleHi: String) {
    ALL("all", "All Packages", "सभी पैकेज"),
    PHOTOGRAPHY("photography", "Photography", "फोटोग्राफी"),
    MODELING("modeling", "Modeling", "मॉडलिंग"),
    PRODUCTION("production", "Video Production", "वीडियो प्रोडक्शन")
}

/**
 * Sorting options for the Service Catalog screen
 */
enum class CatalogSortOrder(val id: String, val labelEn: String, val labelHi: String) {
    FEATURED("featured", "Featured", "लोकप्रिय"),
    PRICE_LOW_HIGH("price_asc", "Price: Low → High", "मूल्य: कम → अधिक"),
    PRICE_HIGH_LOW("price_desc", "Price: High → Low", "मूल्य: अधिक → कम")
}

/**
 * Budget bracket filter for the Service Catalog screen
 */
enum class CatalogBudgetFilter(val id: String, val labelEn: String, val labelHi: String) {
    ALL("all", "Any Budget", "सभी बजट"),
    UNDER_15K("under_15k", "Under ₹15K", "₹15K से कम"),
    RANGE_15K_30K("15k_30k", "₹15K – ₹30K", "₹15K – ₹30K"),
    ABOVE_30K("above_30k", "₹30K+ Cinema", "₹30K+ प्रीमियम");

    fun matchesStartingPrice(priceInr: Long): Boolean = when (this) {
        ALL -> true
        UNDER_15K -> priceInr <= 15000L
        RANGE_15K_30K -> priceInr in 15001L..30000L
        ABOVE_30K -> priceInr > 30000L
    }
}

/**
 * One-tap quick-search filter pills for rapid discovery of photography & video production packages
 */
data class CatalogQuickFilter(
    val id: String,
    val labelEn: String,
    val labelHi: String,
    val queryToken: String
)

/**
 * Pricing tier within a studio service offering (e.g., Essential, Signature Pro, Studio Flagship)
 */
data class ServicePricingTier(
    val id: String,
    val tierNameEn: String,
    val tierNameHi: String,
    val badgeEn: String,
    val badgeHi: String,
    val priceInr: Long,
    val priceFormattedEn: String,
    val priceFormattedHi: String,
    val durationEn: String,
    val durationHi: String,
    val turnaroundEn: String,
    val turnaroundHi: String,
    val deliverablesEn: List<String>,
    val deliverablesHi: List<String>,
    val isRecommended: Boolean = false
)

/**
 * Detailed studio service package with tiered pricing and deliverables
 */
data class ServicePackage(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val category: ServicePackageCategory,
    val startingPriceInr: Long,
    val priceFormattedEn: String,
    val priceFormattedHi: String,
    val durationEn: String,
    val durationHi: String,
    val tagEn: String,
    val tagHi: String,
    val summaryEn: String,
    val summaryHi: String,
    val highlightsEn: List<String>,
    val highlightsHi: List<String>,
    val icon: ImageVector,
    val isPopular: Boolean = false,
    val recommendedServiceType: String, // maps to ShootBooking serviceType options
    val pricingTiers: List<ServicePricingTier> = emptyList()
) {
    fun defaultPricingTier(): ServicePricingTier? =
        pricingTiers.firstOrNull { it.isRecommended } ?: pricingTiers.firstOrNull()

    fun bookingLabelForTier(tier: ServicePricingTier?): String {
        val label = if (tier != null) {
            "$recommendedServiceType - ${tier.tierNameEn}"
        } else {
            recommendedServiceType
        }
        return label.take(100)
    }
}

/**
 * Canonical Studio Services Repository
 */
object StudioServicesData {

    val packages: List<ServicePackage> = listOf(
        // =====================================================================
        // PHOTOGRAPHY PACKAGES
        // =====================================================================
        ServicePackage(
            id = "pkg_photo_fashion",
            titleEn = "High-Fashion Editorial",
            titleHi = "हाई-फैशन एडिटोरियल",
            category = ServicePackageCategory.PHOTOGRAPHY,
            startingPriceInr = 14999L,
            priceFormattedEn = "₹14,999",
            priceFormattedHi = "₹14,999 से शुरू",
            durationEn = "4-5 Hours Studio Session",
            durationHi = "4-5 घंटे स्टूडियो सत्र",
            tagEn = "SIGNATURE EDITORIAL",
            tagHi = "सिग्नेचर एडिटोरियल",
            summaryEn = "Vogue-caliber fashion stills with professional high-key and rim lighting setups.",
            summaryHi = "पेशेवर हाई-की और रिम लाइटिंग सेटअप के साथ अंतरराष्ट्रीय स्तर के फैशन स्टिल्स।",
            highlightsEn = listOf(
                "Up to 4 custom costume/wardrobe looks",
                "25 master retouched high-resolution deliverables",
                "Complete magazine print-ready deliverables",
                "Full in-house continuous & strobe lighting bay"
            ),
            highlightsHi = listOf(
                "4 कस्टम परिधान / वॉर्डरोब लुक्स",
                "25 मास्टर रीटच हाई-रेज़ोल्यूशन इमेजिस",
                "मैगज़ीन प्रिंट-रेडी डिजिटल डिलीवरी",
                "इन-हाउस निरंतर व स्ट्रोब लाइटिंग बे"
            ),
            icon = Icons.Default.CameraAlt,
            isPopular = true,
            recommendedServiceType = "Photography (High-Fashion Editorial)",
            pricingTiers = listOf(
                ServicePricingTier(
                    id = "essential",
                    tierNameEn = "Essential",
                    tierNameHi = "एसेंशियल",
                    badgeEn = "STARTER",
                    badgeHi = "स्टार्टर",
                    priceInr = 14999L,
                    priceFormattedEn = "₹14,999",
                    priceFormattedHi = "₹14,999",
                    durationEn = "3 Hours Studio",
                    durationHi = "3 घंटे स्टूडियो",
                    turnaroundEn = "72 Hours",
                    turnaroundHi = "72 घंटे",
                    deliverablesEn = listOf(
                        "2 wardrobe looks & 2 editorial lighting backdrops",
                        "15 retouched high-resolution editorial frames",
                        "Digital color-calibrated gallery link"
                    ),
                    deliverablesHi = listOf(
                        "2 वॉर्डरोब लुक्स और 2 एडिटोरियल लाइटिंग बैकड्रॉप",
                        "15 रीटच हाई-रेज़ोल्यूशन एडिटोरियल फोटो",
                        "डिजिटल कलर-कैलिब्रेटेड गैलरी लिंक"
                    )
                ),
                ServicePricingTier(
                    id = "pro",
                    tierNameEn = "Pro",
                    tierNameHi = "प्रो",
                    badgeEn = "BEST VALUE",
                    badgeHi = "सर्वश्रेष्ठ",
                    priceInr = 21999L,
                    priceFormattedEn = "₹21,999",
                    priceFormattedHi = "₹21,999",
                    durationEn = "4-5 Hours Studio",
                    durationHi = "4-5 घंटे स्टूडियो",
                    turnaroundEn = "48 Hours",
                    turnaroundHi = "48 घंटे",
                    deliverablesEn = listOf(
                        "4 wardrobe looks with multi-rim strobe setups",
                        "25 magazine-grade master retouched stills",
                        "1 vertical BTS editorial teaser reel"
                    ),
                    deliverablesHi = listOf(
                        "4 वॉर्डरोब लुक्स और मल्टी-रिम स्ट्रोब सेटअप",
                        "25 मैगज़ीन-ग्रेड मास्टर रीटच स्टिल्स",
                        "1 वर्टिकल बीटीएस एडिटोरियल टीज़र रील"
                    ),
                    isRecommended = true
                ),
                ServicePricingTier(
                    id = "flagship",
                    tierNameEn = "Flagship",
                    tierNameHi = "फ्लैगशिप",
                    badgeEn = "LUXURY",
                    badgeHi = "लक्ज़री",
                    priceInr = 34999L,
                    priceFormattedEn = "₹34,999",
                    priceFormattedHi = "₹34,999",
                    durationEn = "Full Day Creative",
                    durationHi = "पूरा दिन क्रिएटिव",
                    turnaroundEn = "24 Hours Priority",
                    turnaroundHi = "24 घंटे प्राथमिकता",
                    deliverablesEn = listOf(
                        "6 designer looks + on-set stylist & makeup bay",
                        "45 beauty & skin-retouched print-ready masters",
                        "Cinema 4K campaign teaser + raw contact sheet"
                    ),
                    deliverablesHi = listOf(
                        "6 डिज़ाइनर लुक्स + ऑन-सेट स्टाइलिस्ट व मेकअप बे",
                        "45 प्रिंट-रेडी मास्टर रीटच इमेजिस",
                        "सिनेमा 4K कैंपेन टीज़र + रॉ कॉन्टैक्ट शीट"
                    )
                )
            )
        ),
        ServicePackage(
            id = "pkg_photo_portrait",
            titleEn = "Studio Portrait & Glamour",
            titleHi = "स्टूडियो पोर्ट्रेट व ग्लैमर",
            category = ServicePackageCategory.PHOTOGRAPHY,
            startingPriceInr = 7999L,
            priceFormattedEn = "₹7,999",
            priceFormattedHi = "₹7,999 से शुरू",
            durationEn = "2-3 Hours Session",
            durationHi = "2-3 घंटे सत्र",
            tagEn = "POPULAR CHOICE",
            tagHi = "लोकप्रिय चयन",
            summaryEn = "Crisp, expressive personal and corporate portraiture with artistic shadow gradients.",
            summaryHi = "सटीक प्रकाश व छाया संतुलन के साथ व्यक्तिगत और कॉर्पोरेट पोर्ट्रेट फोटोग्राफी।",
            highlightsEn = listOf(
                "2 distinct lighting setups (Softbox & Hard Rim)",
                "15 expertly retouched beauty portraits",
                "Digital delivery within 48 business hours",
                "High-res color and B&W monochrome editions"
            ),
            highlightsHi = listOf(
                "2 अलग लाइटिंग सेटअप (सॉफ्टबॉक्स व रिम)",
                "15 मास्टर रीटच ब्यूटी पोर्ट्रेट्स",
                "48 घंटों में त्वरित डिजिटल डिलीवरी",
                "कलर और मोनोक्रोम ब्लैक & व्हाइट संस्करण"
            ),
            icon = Icons.Default.Person,
            isPopular = false,
            recommendedServiceType = "Photography (Portrait & Glamour Stills)",
            pricingTiers = listOf(
                ServicePricingTier(
                    id = "essential",
                    tierNameEn = "Essential",
                    tierNameHi = "एसेंशियल",
                    badgeEn = "SOLO",
                    badgeHi = "सोलो",
                    priceInr = 7999L,
                    priceFormattedEn = "₹7,999",
                    priceFormattedHi = "₹7,999",
                    durationEn = "90 Mins Session",
                    durationHi = "90 मिनट सत्र",
                    turnaroundEn = "48 Hours",
                    turnaroundHi = "48 घंटे",
                    deliverablesEn = listOf(
                        "1 signature backdrop + softbox beauty dish",
                        "10 retouched portraits in color & monochrome",
                        "LinkedIn & casting-ready headshot crops"
                    ),
                    deliverablesHi = listOf(
                        "1 सिग्नेचर बैकड्रॉप + सॉफ्टबॉक्स ब्यूटी डिश",
                        "10 रीटच पोर्ट्रेट्स (कलर व ब्लैक-एंड-व्हाइट)",
                        "लिंक्डइन व कास्टिंग-रेडी हेडशॉट क्रॉप"
                    )
                ),
                ServicePricingTier(
                    id = "pro",
                    tierNameEn = "Pro",
                    tierNameHi = "प्रो",
                    badgeEn = "POPULAR",
                    badgeHi = "लोकप्रिय",
                    priceInr = 11999L,
                    priceFormattedEn = "₹11,999",
                    priceFormattedHi = "₹11,999",
                    durationEn = "2.5 Hours Session",
                    durationHi = "2.5 घंटे सत्र",
                    turnaroundEn = "36 Hours",
                    turnaroundHi = "36 घंटे",
                    deliverablesEn = listOf(
                        "2 lighting setups (Softbox Beauty + Hard Rim)",
                        "18 expertly retouched glamour portraits",
                        "3 outfit changes with pose coaching"
                    ),
                    deliverablesHi = listOf(
                        "2 लाइटिंग सेटअप (सॉफ्टबॉक्स + हार्ड रिम)",
                        "18 मास्टर रीटच ग्लैमर पोर्ट्रेट्स",
                        "पोज़ कोचिंग के साथ 3 आउटफिट बदलाव"
                    ),
                    isRecommended = true
                ),
                ServicePricingTier(
                    id = "flagship",
                    tierNameEn = "Flagship",
                    tierNameHi = "फ्लैगशिप",
                    badgeEn = "EXECUTIVE",
                    badgeHi = "एग्जीक्यूटिव",
                    priceInr = 17999L,
                    priceFormattedEn = "₹17,999",
                    priceFormattedHi = "₹17,999",
                    durationEn = "4 Hours Studio",
                    durationHi = "4 घंटे स्टूडियो",
                    turnaroundEn = "24 Hours",
                    turnaroundHi = "24 घंटे",
                    deliverablesEn = listOf(
                        "4 bespoke textured studio sets + dramatic gels",
                        "30 full-resolution retouched portraits",
                        "2 archival framed 12x18 metallic prints"
                    ),
                    deliverablesHi = listOf(
                        "4 कस्टम टेक्सचर्ड स्टूडियो सेट + ड्रामेटिक जेल्स",
                        "30 फुल-रेज़ोल्यूशन रीटच पोर्ट्रेट्स",
                        "2 फ्रेम्ड 12x18 मैटेलिक प्रिंट्स"
                    )
                )
            )
        ),
        ServicePackage(
            id = "pkg_photo_bridal",
            titleEn = "Royal Pre-Wedding & Bridal",
            titleHi = "रॉयल प्री-वेडिंग और ब्राइडल",
            category = ServicePackageCategory.PHOTOGRAPHY,
            startingPriceInr = 29999L,
            priceFormattedEn = "₹29,999",
            priceFormattedHi = "₹29,999 से शुरू",
            durationEn = "Full Day (Studio + Outdoor)",
            durationHi = "पूरा दिन (स्टूडियो + आउटडोर)",
            tagEn = "ROYAL LUXURY",
            tagHi = "रॉयल लग्जरी",
            summaryEn = "Cinematic storytelling capturing timeless romance with royal sets and scenic outdoor locations.",
            summaryHi = "शाही सेट्स और रमणीय आउटडोर लोकेशन्स पर तैयार किया गया सिनेमाई प्री-वेडिंग शूट।",
            highlightsEn = listOf(
                "Unlimited indoor studio + Sirsa/Ellenabad outdoor sets",
                "50 ultra-high-definition retouched portraits",
                "Luxury velvet-bound printed teaser photo book",
                "High-resolution social media instant preview reel"
            ),
            highlightsHi = listOf(
                "इनडोर स्टूडियो + सिरसा व ऐलनाबाद आउटडोर सेट्स",
                "50 अल्ट्रा हाई-डेफिनिशन रीटच तस्वीरें",
                "प्रीमियम वेलवेट फोटो टीज़र एल्बम",
                "सोशल मीडिया के लिए त्वरित हाई-रेज़ प्रीव्यू"
            ),
            icon = Icons.Default.Diamond,
            isPopular = true,
            recommendedServiceType = "Photography (Royal Pre-Wedding & Bridal)",
            pricingTiers = listOf(
                ServicePricingTier(
                    id = "essential",
                    tierNameEn = "Essential",
                    tierNameHi = "एसेंशियल",
                    badgeEn = "CLASSIC",
                    badgeHi = "क्लासिक",
                    priceInr = 29999L,
                    priceFormattedEn = "₹29,999",
                    priceFormattedHi = "₹29,999",
                    durationEn = "6 Hours Studio",
                    durationHi = "6 घंटे स्टूडियो",
                    turnaroundEn = "5 Days",
                    turnaroundHi = "5 दिन",
                    deliverablesEn = listOf(
                        "Indoor royal sets + 3 bridal/couple attire looks",
                        "35 retouched high-definition romantic stills",
                        "Curated cloud album for family sharing"
                    ),
                    deliverablesHi = listOf(
                        "इनडोर रॉयल सेट्स + 3 ब्राइडल/कपल लुक्स",
                        "35 रीटच हाई-डेफिनिशन रोमांटिक तस्वीरें",
                        "परिवार के साथ शेयर करने हेतु क्लाउड एल्बम"
                    )
                ),
                ServicePricingTier(
                    id = "pro",
                    tierNameEn = "Pro",
                    tierNameHi = "प्रो",
                    badgeEn = "SIGNATURE",
                    badgeHi = "सिग्नेचर",
                    priceInr = 44999L,
                    priceFormattedEn = "₹44,999",
                    priceFormattedHi = "₹44,999",
                    durationEn = "Full Day Hybrid",
                    durationHi = "पूरा दिन हाइब्रिड",
                    turnaroundEn = "4 Days",
                    turnaroundHi = "4 दिन",
                    deliverablesEn = listOf(
                        "Studio + Sirsa/Ellenabad heritage outdoor sets",
                        "55 retouched frames + velvet teaser photo book",
                        "60-sec 4K save-the-date highlight reel"
                    ),
                    deliverablesHi = listOf(
                        "स्टूडियो + सिरसा/ऐलनाबाद आउटडोर हेरिटेज सेट्स",
                        "55 रीटच फोटो + वेलवेट टीज़र फोटो बुक",
                        "60 सेकंड की 4K सेव-द-डेट हाईलाइट रील"
                    ),
                    isRecommended = true
                ),
                ServicePricingTier(
                    id = "flagship",
                    tierNameEn = "Flagship",
                    tierNameHi = "फ्लैगशिप",
                    badgeEn = "ROYAL",
                    badgeHi = "शाही",
                    priceInr = 69999L,
                    priceFormattedEn = "₹69,999",
                    priceFormattedHi = "₹69,999",
                    durationEn = "2-Day Destination",
                    durationHi = "2-दिवसीय डेस्टिनेशन",
                    turnaroundEn = "72 Hours Sneak Peek",
                    turnaroundHi = "72 घंटे स्नीक पीक",
                    deliverablesEn = listOf(
                        "2-day multi-location cinema crew + drone aerials",
                        "100 master stills + handcrafted Italian leather album",
                        "3-minute 4K cinematic pre-wedding film"
                    ),
                    deliverablesHi = listOf(
                        "2-दिवसीय मल्टी-लोकेशन क्रू + ड्रोन एरियल शॉट्स",
                        "100 मास्टर फोटो + इटैलियन लेदर एल्बम",
                        "3 मिनट की 4K सिनेमैटिक प्री-वेडिंग फिल्म"
                    )
                )
            )
        ),
        ServicePackage(
            id = "pkg_photo_commercial",
            titleEn = "Commercial Catalog & E-Commerce",
            titleHi = "कमर्शियल कैटलॉग व ई-कॉमर्स",
            category = ServicePackageCategory.PHOTOGRAPHY,
            startingPriceInr = 11999L,
            priceFormattedEn = "₹11,999",
            priceFormattedHi = "₹11,999 से शुरू",
            durationEn = "Half Day Studio Booking",
            durationHi = "आधा दिन स्टूडियो बुकिंग",
            tagEn = "BRAND ESSENTIAL",
            tagHi = "ब्रांड्स के लिए",
            summaryEn = "Pixel-perfect product, apparel, and footwear catalog shoots calibrated for Amazon and luxury boutiques.",
            summaryHi = "अमेज़ॉन, मिंत्रा और लक्ज़री बुटीक के लिए मानक श्वेत पृष्ठभूमि और लाइफस्टाइल शूट।",
            highlightsEn = listOf(
                "Up to 30 product SKUs / outfit changes",
                "Pure white 255 RGB + styled contextual vignettes",
                "Web & print optimized high-DPI outputs",
                "Fast turnaround with color calibration"
            ),
            highlightsHi = listOf(
                "30 उत्पाद / परिधान परिदृश्य तक",
                "प्योर व्हाइट बैकड्रॉप + लाइफस्टाइल सेट",
                "वेब व प्रिंट के लिए ऑप्टिमाइज़्ड फाइल्स",
                "कलर कैलिब्रेटेड सटीक आउटपुट"
            ),
            icon = Icons.Default.ShoppingBag,
            isPopular = false,
            recommendedServiceType = "Photography (Commercial Product & Catalog)",
            pricingTiers = listOf(
                ServicePricingTier(
                    id = "essential",
                    tierNameEn = "Essential",
                    tierNameHi = "एसेंशियल",
                    badgeEn = "CATALOG",
                    badgeHi = "कैटलॉग",
                    priceInr = 11999L,
                    priceFormattedEn = "₹11,999",
                    priceFormattedHi = "₹11,999",
                    durationEn = "Half Day (4 Hours)",
                    durationHi = "आधा दिन (4 घंटे)",
                    turnaroundEn = "48 Hours",
                    turnaroundHi = "48 घंटे",
                    deliverablesEn = listOf(
                        "Up to 15 product SKUs on pure 255 RGB white",
                        "3 angles per SKU with color accuracy",
                        "Amazon / Flipkart / Shopify size exports"
                    ),
                    deliverablesHi = listOf(
                        "15 प्रोडक्ट SKU प्योर व्हाइट 255 RGB पर",
                        "हर प्रोडक्ट के 3 एंगल सटीक कलर के साथ",
                        "अमेज़ॉन / फ्लिपकार्ट / शॉपिफाई साइज एक्सपोर्ट"
                    )
                ),
                ServicePricingTier(
                    id = "pro",
                    tierNameEn = "Pro",
                    tierNameHi = "प्रो",
                    badgeEn = "BOUTIQUE",
                    badgeHi = "बुटीक",
                    priceInr = 19999L,
                    priceFormattedEn = "₹19,999",
                    priceFormattedHi = "₹19,999",
                    durationEn = "Full Day (7 Hours)",
                    durationHi = "पूरा दिन (7 घंटे)",
                    turnaroundEn = "36 Hours",
                    turnaroundHi = "36 घंटे",
                    deliverablesEn = listOf(
                        "Up to 30 SKUs + styled lifestyle flat-lay props",
                        "Ghost mannequin & reflective jewelry lighting",
                        "Web banners + social carousel cutdowns"
                    ),
                    deliverablesHi = listOf(
                        "30 SKU + स्टाइल्ड लाइफस्टाइल प्रॉप्स",
                        "घोस्ट मैनिकिन व ज्वेलरी रिफ्लेक्टिव लाइटिंग",
                        "वेब बैनर + सोशल मीडिया कैरोसेल कट्स"
                    ),
                    isRecommended = true
                ),
                ServicePricingTier(
                    id = "flagship",
                    tierNameEn = "Flagship",
                    tierNameHi = "फ्लैगशिप",
                    badgeEn = "CAMPAIGN",
                    badgeHi = "कैंपेन",
                    priceInr = 32999L,
                    priceFormattedEn = "₹32,999",
                    priceFormattedHi = "₹32,999",
                    durationEn = "Full Day + Model",
                    durationHi = "पूरा दिन + मॉडल",
                    turnaroundEn = "24 Hours",
                    turnaroundHi = "24 घंटे",
                    deliverablesEn = listOf(
                        "50 SKUs + verified Star Artist catalog model",
                        "Custom art-directed brand set & lookbook PDF",
                        "4K motion product spins for paid ads"
                    ),
                    deliverablesHi = listOf(
                        "50 SKU + सत्यापित स्टार आर्टिस्ट मॉडल",
                        "कस्टम ब्रांड सेट व डिजिटल लुकबुक PDF",
                        "विज्ञापनों के लिए 4K प्रोडक्ट मोशन शॉट्स"
                    )
                )
            )
        ),

        // =====================================================================
        // MODELING PACKAGES
        // =====================================================================
        ServicePackage(
            id = "pkg_model_portfolio",
            titleEn = "Comp-Card & Agency Portfolio",
            titleHi = "कॉम्प-कार्ड व एजेंसी पोर्टफोलियो",
            category = ServicePackageCategory.MODELING,
            startingPriceInr = 12999L,
            priceFormattedEn = "₹12,999",
            priceFormattedHi = "₹12,999 से शुरू",
            durationEn = "3-4 Hours Direction Session",
            durationHi = "3-4 घंटे निर्देशन सत्र",
            tagEn = "CAREER LAUNCHPAD",
            tagHi = "करियर लॉन्चपैड",
            summaryEn = "Complete model audition package including standardized industry comp-cards and expressive posing.",
            summaryHi = "मानक मॉडलिंग उद्योग कॉम्प-कार्ड, क्लोज-अप्स, फुल-लेंथ शॉट्स और पोज़िंग गाइडेंस।",
            highlightsEn = listOf(
                "Digital & printed ready-to-submit 4-look Comp Card",
                "Direction & posture coaching by studio director",
                "Headshots, three-quarter, and full-length editorial frames",
                "Direct inclusion in Star Artist Studio 18+ Model Roster"
            ),
            highlightsHi = listOf(
                "4-लुक डिजिटल व प्रिंटेड एजेंसी कॉम्प-कार्ड",
                "स्टूडियो डायरेक्टर द्वारा पोज़िंग व पोस्चर कोचिंग",
                "हेडशॉट्स, थ्री-क्वार्टर व फुल-बॉडी फ्रेम",
                "स्टार आर्टिस्ट 18+ मॉडल नेटवर्क में सीधी लिस्टिंग"
            ),
            icon = Icons.Default.Star,
            isPopular = true,
            recommendedServiceType = "Modeling (Portfolio & Comp-Card Shoot)",
            pricingTiers = listOf(
                ServicePricingTier(
                    id = "essential",
                    tierNameEn = "Essential",
                    tierNameHi = "एसेंशियल",
                    badgeEn = "DEBUT",
                    badgeHi = "डेब्यू",
                    priceInr = 12999L,
                    priceFormattedEn = "₹12,999",
                    priceFormattedHi = "₹12,999",
                    durationEn = "3 Hours Studio",
                    durationHi = "3 घंटे स्टूडियो",
                    turnaroundEn = "72 Hours",
                    turnaroundHi = "72 घंटे",
                    deliverablesEn = listOf(
                        "2-look audition comp-card + clean Polaroids",
                        "12 retouched headshots & full-length frames",
                        "Studio director posture & camera orientation"
                    ),
                    deliverablesHi = listOf(
                        "2-लुक ऑडिशन कॉम्प-कार्ड + क्लीन पोलरॉइड्स",
                        "12 रीटच हेडशॉट्स व फुल-लेंथ फ्रेम",
                        "डायरेक्टर द्वारा पोज़िंग व कैमरा ओरिएंटेशन"
                    )
                ),
                ServicePricingTier(
                    id = "pro",
                    tierNameEn = "Pro",
                    tierNameHi = "प्रो",
                    badgeEn = "AGENCY PICK",
                    badgeHi = "एजेंसी पसंद",
                    priceInr = 18999L,
                    priceFormattedEn = "₹18,999",
                    priceFormattedHi = "₹18,999",
                    durationEn = "4-5 Hours Studio",
                    durationHi = "4-5 घंटे स्टूडियो",
                    turnaroundEn = "48 Hours",
                    turnaroundHi = "48 घंटे",
                    deliverablesEn = listOf(
                        "4-look digital & printed 300gsm agency comp-card",
                        "24 editorial, commercial & ethnic portfolio stills",
                        "Priority placement on Star Artist 18+ Roster"
                    ),
                    deliverablesHi = listOf(
                        "4-लुक डिजिटल व प्रिंटेड 300gsm एजेंसी कॉम्प-कार्ड",
                        "24 एडिटोरियल, कमर्शियल व एथनिक पोर्टफोलियो फोटो",
                        "स्टार आर्टिस्ट 18+ रोस्टर में प्राथमिकता लिस्टिंग"
                    ),
                    isRecommended = true
                ),
                ServicePricingTier(
                    id = "flagship",
                    tierNameEn = "Flagship",
                    tierNameHi = "फ्लैगशिप",
                    badgeEn = "SUPERMODEL",
                    badgeHi = "सुपरमॉडल",
                    priceInr = 27999L,
                    priceFormattedEn = "₹27,999",
                    priceFormattedHi = "₹27,999",
                    durationEn = "Full Day + Reel",
                    durationHi = "पूरा दिन + रील",
                    turnaroundEn = "36 Hours",
                    turnaroundHi = "36 घंटे",
                    deliverablesEn = listOf(
                        "6 designer looks with HMUA hair & styling team",
                        "40 retouched agency & magazine masters",
                        "4K audition introduction & ramp walk showreel"
                    ),
                    deliverablesHi = listOf(
                        "6 डिज़ाइनर लुक्स + प्रो हेयर व मेकअप टीम",
                        "40 रीटच एजेंसी व मैगज़ीन मास्टर्स",
                        "4K ऑडिशन इंट्रोडक्शन व रैंप वॉक शोरिल"
                    )
                )
            )
        ),
        ServicePackage(
            id = "pkg_model_runway",
            titleEn = "Runway & Lookbook Motion",
            titleHi = "रैनवे व लुकबुक मोशन",
            category = ServicePackageCategory.MODELING,
            startingPriceInr = 18999L,
            priceFormattedEn = "₹18,999",
            priceFormattedHi = "₹18,999 से शुरू",
            durationEn = "Full Day Production",
            durationHi = "पूरा दिन प्रोडक्शन",
            tagEn = "DESIGNER LOOKBOOK",
            tagHi = "डिज़ाइनर लुकबुक",
            summaryEn = "Dynamic runway walk pacing with continuous lighting and synchronized slow-motion video captures.",
            summaryHi = "कैटवॉक और हाई-फैशन गारमेंट्स के लिए स्लो-मोशन वीडियो और स्टिल्स का सम्मिलित सत्र।",
            highlightsEn = listOf(
                "Synchronized 4K 120fps motion catwalk captures",
                "5 unique designer ensemble transformations",
                "Color-graded cinematic reel for Instagram & agency",
                "35 curated high-fashion still captures"
            ),
            highlightsHi = listOf(
                "4K 120fps स्लो-मोशन कैटवॉक वीडियो शॉट्स",
                "5 डिज़ाइनर परिधान बदलाव",
                "इंस्टाग्राम व एजेंसियों हेतु कलर-ग्रेडेड रील",
                "35 उच्च गुणवत्ता वाले एडिटोरियल स्टिल्स"
            ),
            icon = Icons.Default.Person,
            isPopular = false,
            recommendedServiceType = "Modeling (Runway & Lookbook Session)",
            pricingTiers = listOf(
                ServicePricingTier(
                    id = "essential",
                    tierNameEn = "Essential",
                    tierNameHi = "एसेंशियल",
                    badgeEn = "LOOKBOOK",
                    badgeHi = "लुकबुक",
                    priceInr = 18999L,
                    priceFormattedEn = "₹18,999",
                    priceFormattedHi = "₹18,999",
                    durationEn = "4 Hours Bay",
                    durationHi = "4 घंटे स्टेज",
                    turnaroundEn = "4 Days",
                    turnaroundHi = "4 दिन",
                    deliverablesEn = listOf(
                        "3 designer outfit transformations",
                        "20 still frames + 30-sec 4K runway walk clip",
                        "Standard sound design & color grade"
                    ),
                    deliverablesHi = listOf(
                        "3 डिज़ाइनर परिधान बदलाव",
                        "20 स्टिल फ्रेम + 30 सेकंड 4K रैनवे वॉक क्लिप",
                        "स्टैंडर्ड साउंड डिज़ाइन व कलर ग्रेडिंग"
                    )
                ),
                ServicePricingTier(
                    id = "pro",
                    tierNameEn = "Pro",
                    tierNameHi = "प्रो",
                    badgeEn = "TRENDING",
                    badgeHi = "ट्रेंडिंग",
                    priceInr = 26999L,
                    priceFormattedEn = "₹26,999",
                    priceFormattedHi = "₹26,999",
                    durationEn = "Full Day Studio",
                    durationHi = "पूरा दिन स्टूडियो",
                    turnaroundEn = "72 Hours",
                    turnaroundHi = "72 घंटे",
                    deliverablesEn = listOf(
                        "5 ensemble transformations + 4K 120fps slow-mo",
                        "35 high-fashion lookbook stills",
                        "Color-graded agency showreel + vertical IG reel"
                    ),
                    deliverablesHi = listOf(
                        "5 परिधान बदलाव + 4K 120fps स्लो-मोशन",
                        "35 हाई-फैशन लुकबुक स्टिल्स",
                        "एजेंसी शोरिल + वर्टिकल इंस्टाग्राम रील"
                    ),
                    isRecommended = true
                ),
                ServicePricingTier(
                    id = "flagship",
                    tierNameEn = "Flagship",
                    tierNameHi = "फ्लैगशिप",
                    badgeEn = "COLLECTION",
                    badgeHi = "कलेक्शन",
                    priceInr = 39999L,
                    priceFormattedEn = "₹39,999",
                    priceFormattedHi = "₹39,999",
                    durationEn = "Full Day + Crew",
                    durationHi = "पूरा दिन + क्रू",
                    turnaroundEn = "48 Hours",
                    turnaroundHi = "48 घंटे",
                    deliverablesEn = listOf(
                        "8 designer garments + motorized runway dolly rig",
                        "60 lookbook masters + digital catalog PDF",
                        "3 bespoke vertical & widescreen campaign cuts"
                    ),
                    deliverablesHi = listOf(
                        "8 डिज़ाइनर परिधान + मोटराइज्ड डॉली रिग",
                        "60 लुकबुक मास्टर्स + डिजिटल कैटलॉग PDF",
                        "3 वर्टिकल व वाइडस्क्रीन कैंपेन एडिट्स"
                    )
                )
            )
        ),

        // =====================================================================
        // PRODUCTION & 4K VIDEO PACKAGES
        // =====================================================================
        ServicePackage(
            id = "pkg_video_music",
            titleEn = "4K Cinema Music Video Direction",
            titleHi = "4K सिनेमा संगीत वीडियो निर्देशन",
            category = ServicePackageCategory.PRODUCTION,
            startingPriceInr = 49999L,
            priceFormattedEn = "₹49,999",
            priceFormattedHi = "₹49,999 से शुरू",
            durationEn = "2-Day Multi-Location Shoot",
            durationHi = "2-दिवसीय मल्टी-लोकेशन शूट",
            tagEn = "FLAGSHIP CINEMA",
            tagHi = "फ्लैगशिप सिनेमा",
            summaryEn = "Full cinema-grade music video production from script storyboard to 4K Davinci color grade.",
            summaryHi = "पटकथा व स्टोरीबोर्ड से लेकर 4K डाविंची कलर ग्रेडिंग तक संपूर्ण संगीत वीडियो निर्माण।",
            highlightsEn = listOf(
                "High-dynamic range 4K cinema camera recording",
                "Motorized gimbal, dolly, and smoke atmosphere rig",
                "Complete narrative storyboard & scene direction",
                "Master export in 4K DCI + vertical social teaser cut"
            ),
            highlightsHi = listOf(
                "हाई-डायनेमिक रेंज 4K सिनेमा कैमरा रिकॉर्डिंग",
                "मोटराइज्ड गिम्बल, डॉली व हेज़/स्मोक एटमॉस्फियर",
                "संपूर्ण पटकथा स्टोरीबोर्ड व सीन डायरेक्शन",
                "मास्टर 4K DCI एक्सपोर्ट + वर्टिकल रील्स टीज़र"
            ),
            icon = Icons.Default.Movie,
            isPopular = true,
            recommendedServiceType = "Video (4K Cinema Music Video Direction)",
            pricingTiers = listOf(
                ServicePricingTier(
                    id = "essential",
                    tierNameEn = "Essential",
                    tierNameHi = "एसेंशियल",
                    badgeEn = "SINGLE TRACK",
                    badgeHi = "सिंगल ट्रैक",
                    priceInr = 49999L,
                    priceFormattedEn = "₹49,999",
                    priceFormattedHi = "₹49,999",
                    durationEn = "1-Day Studio Bay",
                    durationHi = "1-दिन स्टूडियो बे",
                    turnaroundEn = "7 Days",
                    turnaroundHi = "7 दिन",
                    deliverablesEn = listOf(
                        "4K cinema camera + gimbal lip-sync performance shoot",
                        "In-house lighting stage with haze & RGB tube rig",
                        "Full YouTube 4K master + 15-sec Reels teaser"
                    ),
                    deliverablesHi = listOf(
                        "4K सिनेमा कैमरा + गिम्बल लिप-सिंक परफॉर्मेंस शूट",
                        "इन-हाउस लाइटिंग स्टेज, हेज़ व RGB ट्यूब रिग",
                        "यूट्यूब 4K मास्टर + 15 सेकंड रील्स टीज़र"
                    )
                ),
                ServicePricingTier(
                    id = "pro",
                    tierNameEn = "Pro",
                    tierNameHi = "प्रो",
                    badgeEn = "DIRECTOR'S CUT",
                    badgeHi = "डायरेक्टर्स कट",
                    priceInr = 74999L,
                    priceFormattedEn = "₹74,999",
                    priceFormattedHi = "₹74,999",
                    durationEn = "2-Day Hybrid",
                    durationHi = "2-दिवसीय हाइब्रिड",
                    turnaroundEn = "7 Days",
                    turnaroundHi = "7 दिन",
                    deliverablesEn = listOf(
                        "Narrative storyboard + studio & outdoor Sirsa sets",
                        "Motorized dolly, smoke rig & DaVinci Resolve DI grade",
                        "4K DCI YouTube master + 3 vertical social promos"
                    ),
                    deliverablesHi = listOf(
                        "पटकथा स्टोरीबोर्ड + स्टूडियो व सिरसा आउटडोर सेट्स",
                        "मोटराइज्ड डॉली, स्मोक रिग व डाविंची DI कलर ग्रेड",
                        "4K DCI यूट्यूब मास्टर + 3 वर्टिकल प्रोमो कट्स"
                    ),
                    isRecommended = true
                ),
                ServicePricingTier(
                    id = "flagship",
                    tierNameEn = "Flagship",
                    tierNameHi = "फ्लैगशिप",
                    badgeEn = "LABEL RELEASE",
                    badgeHi = "लेबल रिलीज़",
                    priceInr = 119999L,
                    priceFormattedEn = "₹1,19,999",
                    priceFormattedHi = "₹1,19,999",
                    durationEn = "3-Day Cinema Crew",
                    durationHi = "3-दिवसीय सिनेमा क्रू",
                    turnaroundEn = "10 Days Master",
                    turnaroundHi = "10 दिन मास्टर",
                    deliverablesEn = listOf(
                        "Multi-location unit, drone aerials & verified cast",
                        "Anamorphic lens kit + full VFX & sound mastering",
                        "YouTube 4K HDR master + poster photoshoot included"
                    ),
                    deliverablesHi = listOf(
                        "मल्टी-लोकेशन यूनिट, ड्रोन एरियल्स व सत्यापित कास्ट",
                        "एनामॉर्फिक लेंस किट + VFX व साउंड मास्टरिंग",
                        "यूट्यूब 4K HDR मास्टर + पोस्टर फोटोशूट शामिल"
                    )
                )
            )
        ),
        ServicePackage(
            id = "pkg_video_brand_ad",
            titleEn = "Commercial Brand Ad Film",
            titleHi = "कमर्शियल ब्रांड विज्ञापन फिल्म",
            category = ServicePackageCategory.PRODUCTION,
            startingPriceInr = 24999L,
            priceFormattedEn = "₹24,999",
            priceFormattedHi = "₹24,999 से शुरू",
            durationEn = "Full Day Studio Bay",
            durationHi = "पूरा दिन स्टूडियो बे",
            tagEn = "COMMERCIAL PRO",
            tagHi = "कमर्शियल प्रो",
            summaryEn = "High-impact 30-60 second television and digital ad films highlighting products and services.",
            summaryHi = "ब्रांड्स व व्यवसायों के लिए प्रभावशाली 30-60 सेकंड के टीवी और डिजिटल विज्ञापन।",
            highlightsEn = listOf(
                "Broadcast-quality audio capture & licensed music score",
                "Professional lighting grid in Talwara Khurd studio",
                "Motion graphics logo reveal & typography overlay",
                "Landscape 16:9 + Vertical 9:16 optimized exports"
            ),
            highlightsHi = listOf(
                "ब्रॉडकास्ट क्वालिटी ऑडियो व लाइसेंस प्राप्त संगीत",
                "तलवाड़ा खुर्द स्टूडियो में प्रो लाइटिंग ग्रिड",
                "मोशन ग्राफिक्स लोगो रिवील व टाइटल्स",
                "यूट्यूब (16:9) और इंस्टाग्राम (9:16) दोनों फॉर्मेट्स"
            ),
            icon = Icons.Default.Videocam,
            isPopular = false,
            recommendedServiceType = "Video (Commercial Brand Ad Film)",
            pricingTiers = listOf(
                ServicePricingTier(
                    id = "essential",
                    tierNameEn = "Essential",
                    tierNameHi = "एसेंशियल",
                    badgeEn = "DIGITAL AD",
                    badgeHi = "डिजिटल विज्ञापन",
                    priceInr = 24999L,
                    priceFormattedEn = "₹24,999",
                    priceFormattedHi = "₹24,999",
                    durationEn = "Half Day Studio",
                    durationHi = "आधा दिन स्टूडियो",
                    turnaroundEn = "4 Days",
                    turnaroundHi = "4 दिन",
                    deliverablesEn = listOf(
                        "30-sec crisp commercial spot shot in 4K UHD",
                        "Licensed background track + clean logo sting",
                        "16:9 landscape + 9:16 Instagram/WhatsApp cut"
                    ),
                    deliverablesHi = listOf(
                        "4K UHD में शूट किया गया 30 सेकंड का विज्ञापन",
                        "लाइसेंस प्राप्त संगीत + क्लीन लोगो एनीमेशन",
                        "16:9 वाइड और 9:16 इंस्टाग्राम/व्हाट्सऐप फॉर्मेट"
                    )
                ),
                ServicePricingTier(
                    id = "pro",
                    tierNameEn = "Pro",
                    tierNameHi = "प्रो",
                    badgeEn = "GROWTH",
                    badgeHi = "ग्रोथ",
                    priceInr = 39999L,
                    priceFormattedEn = "₹39,999",
                    priceFormattedHi = "₹39,999",
                    durationEn = "Full Day Studio",
                    durationHi = "पूरा दिन स्टूडियो",
                    turnaroundEn = "5 Days",
                    turnaroundHi = "5 दिन",
                    deliverablesEn = listOf(
                        "60-sec script-to-screen ad film with voiceover",
                        "Motion graphics product callouts & typography",
                        "3 A/B tested ad hooks for Meta & YouTube Ads"
                    ),
                    deliverablesHi = listOf(
                        "वॉयसओवर के साथ 60 सेकंड की ब्रांड विज्ञापन फिल्म",
                        "मोशन ग्राफिक्स व प्रोडक्ट टाइपोग्राफी कॉलआउट्स",
                        "मेटा व यूट्यूब विज्ञापनों के लिए 3 A/B टेस्ट हुक्स"
                    ),
                    isRecommended = true
                ),
                ServicePricingTier(
                    id = "flagship",
                    tierNameEn = "Flagship",
                    tierNameHi = "फ्लैगशिप",
                    badgeEn = "BROADCAST TV",
                    badgeHi = "ब्रॉडकास्ट टीवी",
                    priceInr = 64999L,
                    priceFormattedEn = "₹64,999",
                    priceFormattedHi = "₹64,999",
                    durationEn = "2-Day Production",
                    durationHi = "2-दिवसीय प्रोडक्शन",
                    turnaroundEn = "7 Days",
                    turnaroundHi = "7 दिन",
                    deliverablesEn = listOf(
                        "Full script, 2 on-camera models & macro product rig",
                        "Broadcast loudness mix & cinema DI color grading",
                        "90-sec brand film + 15-sec/30-sec TV cutdowns"
                    ),
                    deliverablesHi = listOf(
                        "स्क्रिप्ट, 2 ऑन-कैमरा मॉडल व मैक्रो प्रोडक्ट रिग",
                        "ब्रॉडकास्ट ऑडियो मिक्स व सिनेमा DI कलर ग्रेडिंग",
                        "90 सेकंड ब्रांड फिल्म + 15s/30s टीवी कट्स"
                    )
                )
            )
        )
    )

    val quickSearchFilters: List<CatalogQuickFilter> = listOf(
        CatalogQuickFilter("photography", "Photography", "फोटोग्राफी", "photography"),
        CatalogQuickFilter("4k_video", "4K Video", "4K वीडियो", "4K video"),
        CatalogQuickFilter("music_video", "Music Video", "संगीत वीडियो", "music video"),
        CatalogQuickFilter("ad_film", "Ad Film", "विज्ञापन फिल्म", "ad film"),
        CatalogQuickFilter("bridal", "Bridal", "ब्राइडल", "bridal"),
        CatalogQuickFilter("editorial", "Editorial", "एडिटोरियल", "editorial"),
        CatalogQuickFilter("portrait", "Portrait", "पोर्ट्रेट", "portrait"),
        CatalogQuickFilter("reels", "Reels", "रील्स", "reels"),
        CatalogQuickFilter("ecommerce", "E-Commerce", "ई-कॉमर्स", "catalog")
    )

    val minStartingPriceInr: Long
        get() = packages.minOfOrNull { it.startingPriceInr } ?: 0L

    private val querySeparatorRegex = Regex("[\\s,/+&]+")

    private fun ServicePackage.categorySynonyms(): String = when (category) {
        ServicePackageCategory.PHOTOGRAPHY -> "photography photo shoot stills camera portrait"
        ServicePackageCategory.MODELING -> "modeling model portfolio audition agency lookbook stills"
        ServicePackageCategory.PRODUCTION -> "video production cinema film commercial shoot 4k direction"
        ServicePackageCategory.ALL -> ""
    }

    private fun ServicePackage.searchableCorpus(): String = buildString {
        append(titleEn).append(' ').append(titleHi).append(' ')
        append(category.id).append(' ').append(category.titleEn).append(' ').append(category.titleHi).append(' ')
        append(categorySynonyms()).append(' ')
        append(tagEn).append(' ').append(tagHi).append(' ')
        append(summaryEn).append(' ').append(summaryHi).append(' ')
        append(durationEn).append(' ').append(durationHi).append(' ')
        append(recommendedServiceType).append(' ')
        highlightsEn.forEach { append(it).append(' ') }
        highlightsHi.forEach { append(it).append(' ') }
        pricingTiers.forEach { tier ->
            append(tier.tierNameEn).append(' ').append(tier.tierNameHi).append(' ')
            append(tier.badgeEn).append(' ').append(tier.badgeHi).append(' ')
            append(tier.durationEn).append(' ').append(tier.durationHi).append(' ')
            append(tier.turnaroundEn).append(' ').append(tier.turnaroundHi).append(' ')
            append(tier.priceFormattedEn).append(' ')
            tier.deliverablesEn.forEach { append(it).append(' ') }
            tier.deliverablesHi.forEach { append(it).append(' ') }
        }
    }.lowercase()

    fun tokenizeQuery(query: String): List<String> =
        query.trim()
            .lowercase()
            .split(querySeparatorRegex)
            .filter { it.isNotBlank() }

    /**
     * Returns a concise matching highlight/deliverable line when the search query matched inside
     * deliverables, highlights, or tier tags rather than only the main title.
     */
    fun findMatchingSnippet(pkg: ServicePackage, query: String, isHindi: Boolean): String? {
        val tokens = tokenizeQuery(query)
        if (tokens.isEmpty()) return null
        val localizedTitle = (if (isHindi) pkg.titleHi else pkg.titleEn).lowercase()
        if (tokens.all { localizedTitle.contains(it) }) return null

        val candidateLines = buildList {
            addAll(if (isHindi) pkg.highlightsHi else pkg.highlightsEn)
            pkg.pricingTiers.forEach { tier ->
                val tierLabel = if (isHindi) tier.tierNameHi else tier.tierNameEn
                val items = if (isHindi) tier.deliverablesHi else tier.deliverablesEn
                items.forEach { add("$tierLabel: $it") }
            }
        }
        return candidateLines.firstOrNull { line ->
            val lowerLine = line.lowercase()
            tokens.any { lowerLine.contains(it) }
        }
    }

    fun findPackageById(id: String): ServicePackage? =
        packages.firstOrNull { it.id == id }

    fun filterAndSortPackages(
        category: ServicePackageCategory = ServicePackageCategory.ALL,
        query: String = "",
        sortOrder: CatalogSortOrder = CatalogSortOrder.FEATURED,
        budgetFilter: CatalogBudgetFilter = CatalogBudgetFilter.ALL,
        favoritesOnly: Boolean = false,
        favoriteIds: Set<String> = emptySet()
    ): List<ServicePackage> {
        val tokens = tokenizeQuery(query)
        val filtered = packages.filter { pkg ->
            val matchesCategory = category == ServicePackageCategory.ALL || pkg.category == category
            val matchesBudget = budgetFilter.matchesStartingPrice(pkg.startingPriceInr)
            val matchesFavorite = !favoritesOnly || pkg.id in favoriteIds
            val matchesQuery = tokens.isEmpty() || run {
                val corpus = pkg.searchableCorpus()
                tokens.all { token -> corpus.contains(token) }
            }
            matchesCategory && matchesBudget && matchesFavorite && matchesQuery
        }
        return when (sortOrder) {
            CatalogSortOrder.FEATURED -> filtered
            CatalogSortOrder.PRICE_LOW_HIGH -> filtered.sortedBy { it.startingPriceInr }
            CatalogSortOrder.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.startingPriceInr }
        }
    }
}
