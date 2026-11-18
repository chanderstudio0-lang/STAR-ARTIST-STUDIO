package com.example.util

import com.example.data.model.AppLanguage

object StudioConstants {
    const val STUDIO_NAME = StudioContactDetails.STUDIO_NAME
    const val ADDRESS = StudioContactDetails.ADDRESS
    const val MOBILE_NUMBER = StudioContactDetails.MOBILE_NUMBER
    const val WHATSAPP_NUMBER = StudioContactDetails.WHATSAPP_NUMBER
    const val WHATSAPP_URL = StudioContactDetails.WHATSAPP_URL
    const val CALL_URL = StudioContactDetails.CALL_URI
    const val ADMIN_EMAIL = StudioContactDetails.ADMIN_EMAIL
}

class StudioStrings(val lang: AppLanguage) {
    val studioName = StudioConstants.STUDIO_NAME
    val fullAddress = StudioConstants.ADDRESS
    val mobile = StudioConstants.MOBILE_NUMBER
    val whatsapp = StudioConstants.WHATSAPP_NUMBER

    // Navigation & General
    val navHome = if (lang == AppLanguage.HINDI) "होम" else "Home"
    val navModelReg = if (lang == AppLanguage.HINDI) "मॉडल पंजीकरण" else "Model Reg"
    val navBooking = if (lang == AppLanguage.HINDI) "शूट बुकिंग" else "Shoot Booking"
    val navGallery = if (lang == AppLanguage.HINDI) "गैलरी" else "Gallery"
    val navAiStudio = if (lang == AppLanguage.HINDI) "एआई स्टूडियो" else "AI Studio"
    val navBlog = if (lang == AppLanguage.HINDI) "ब्लॉग व टिप्स" else "Blog & Tips"
    val navAdmin = if (lang == AppLanguage.HINDI) "एडमिन पैनल" else "Admin"
    val navContact = if (lang == AppLanguage.HINDI) "संपर्क करें" else "Contact"

    val callNow = if (lang == AppLanguage.HINDI) "कॉल करें: ${StudioContactDetails.MOBILE_NUMBER}" else "Call: ${StudioContactDetails.MOBILE_NUMBER}"
    val whatsappNow = if (lang == AppLanguage.HINDI) "व्हाट्सएप करें" else "WhatsApp Us"
    val bookShootNow = if (lang == AppLanguage.HINDI) "शूट बुक करें" else "Book Shoot"
    val registerModel = if (lang == AppLanguage.HINDI) "मॉडल बनें" else "Register as Model"
    val viewGallery = if (lang == AppLanguage.HINDI) "गैलरी देखें" else "View Gallery"

    // Home Page
    val heroTagline = if (lang == AppLanguage.HINDI)
        "प्रीमियम मॉडलिंग, फोटोग्राफी और 4K सिनेमा वीडियो प्रोडक्शन स्टूडियो"
    else
        "Premium Modeling, Photography & 4K Cinema Video Production Studio"

    val heroSubtext = if (lang == AppLanguage.HINDI)
        "तलवाड़ा खुर्द, ऐलनाबाद, सिरसा स्थित पेशेवर स्टूडियो: फैशन पोर्टफोलियो, कमर्शियल विज्ञापन, सिनेमाई संगीत वीडियो और प्री-वेडिंग शूट्स।"
    else
        "Talwara Khurd, Ellenabad, Sirsa based creative studio: Fashion portfolios, commercial ad films, cinematic music videos, and pre-wedding shoots."

    // Studio Introduction Section
    val studioIntroTitle = if (lang == AppLanguage.HINDI) "स्टार आर्टिस्ट स्टूडियो के बारे में" else "About Star Artist Studio"
    val studioIntroTagline = if (lang == AppLanguage.HINDI)
        "सिनेमाई दृष्टि, अत्याधुनिक 4K उपकरण और पेशेवर मॉडलिंग प्रबंधन का संगम"
    else
        "Where Cinematic Vision Meets Cutting-Edge 4K Gear & Professional Modeling"
    val studioIntroBody = if (lang == AppLanguage.HINDI)
        "स्टार आर्टिस्ट स्टूडियो तलवाड़ा खुर्द, तहसील ऐलनाबाद, जिला सिरसा, हरियाणा - 125102 में स्थित एक समर्पित फोटोग्राफी और वीडियो प्रोडक्शन स्टूडियो है। हम उच्च स्तरीय फैशन पोर्टफोलियो, कमर्शियल विज्ञापन, 4K सिनेमा संगीत वीडियो और प्री-वेडिंग शूट्स में सेवाएं प्रदान करते हैं। हमारे स्टूडियो में इन-हाउस लाइटिंग स्टेज और सत्यापित 18+ मॉडल नेटवर्क उपलब्ध है।"
    else
        "Located at Talwara Khurd, Tehsil Ellenabad, District Sirsa, Haryana - 125102, STAR ARTIST STUDIO is a dedicated creative production studio. We specialize in high-fashion modeling portfolios, commercial advertising, 4K cinema music videos, and pre-wedding shoots, equipped with professional in-house lighting bays and verified 18+ talent casting."
    val studioDirectorNote = if (lang == AppLanguage.HINDI)
        "\"हम रचनात्मक दृष्टिकोण और तकनीकी दक्षता के साथ हर फोटोग्राफी व वीडियो प्रोजेक्ट को पेशेवर रूप से प्रस्तुत करते हैं।\""
    else
        "\"We bring creative vision and technical precision to every photography and cinematography project.\""

    // Quick Navigation Grid
    val quickNavTitle = if (lang == AppLanguage.HINDI) "स्टूडियो सुविधाएं व नेविगेशन" else "Studio Features & Exploration"
    val quickNavSub = if (lang == AppLanguage.HINDI) "शूटिंग, गैलरी, एआई और टैलेंट सेवाओं तक तुरंत पहुंचें" else "Quick access to bookings, portfolio, AI studio and talent roster"
    val navGridBookShoot = if (lang == AppLanguage.HINDI) "शूट बुकिंग" else "Shoot Booking"
    val navGridBookShootSub = if (lang == AppLanguage.HINDI) "तारीख व समय स्लॉट चुनें" else "Reserve photo & 4K video slots"
    val navGridGallery = if (lang == AppLanguage.HINDI) "स्टूडियो गैलरी" else "Studio Gallery"
    val navGridGallerySub = if (lang == AppLanguage.HINDI) "4K काम व डेमो पोर्टफोलियो" else "Browse signature projects"
    val navGridAiStudio = if (lang == AppLanguage.HINDI) "एआई स्टूडियो" else "AI Studio"
    val navGridAiStudioSub = if (lang == AppLanguage.HINDI) "पोज़ व स्टोरीबोर्ड जनरेटर" else "Gemini creative assistant"
    val navGridModelReg = if (lang == AppLanguage.HINDI) "मॉडल पंजीकरण" else "Model Roster"
    val navGridModelRegSub = if (lang == AppLanguage.HINDI) "18+ टैलेंट कास्टिंग" else "Join our fashion agency"
    val navGridAdmin = if (lang == AppLanguage.HINDI) "एडमिन पोर्टल" else "Admin Console"
    val navGridAdminSub = if (lang == AppLanguage.HINDI) "समीक्षा व प्रबंधन" else "Manage bookings & submissions"
    val navGridContact = if (lang == AppLanguage.HINDI) "संपर्क व लोकेशन" else "Location & Map"
    val navGridContactSub = if (lang == AppLanguage.HINDI) "तलवाड़ा खुर्द, सिरसा" else "Talwara Khurd, Sirsa"

    val ourServicesTitle = if (lang == AppLanguage.HINDI) "हमारी विशेष सेवाएं" else "Our Signature Services"

    val servicePhotoTitle = if (lang == AppLanguage.HINDI) "हाई-फैशन फोटोग्राफी" else "High-Fashion Photography"
    val servicePhotoDesc = if (lang == AppLanguage.HINDI)
        "एडिटोरियल, ई-कॉमर्स, कैटलॉग और अंतरराष्ट्रीय स्तर के मॉडलिंग पोर्टफोलियो।"
    else
        "Editorial, catalog, e-commerce & international caliber modeling portfolios."

    val serviceVideoTitle = if (lang == AppLanguage.HINDI) "4K सिनेमा वीडियो प्रोडक्शन" else "4K Cinema Video Production"
    val serviceVideoDesc = if (lang == AppLanguage.HINDI)
        "सिनेमाई संगीत वीडियो, ब्रांड विज्ञापन, रील्स और मोशन पिक्चर निर्देशन।"
    else
        "Cinematic music videos, TV commercials, viral fashion reels & motion picture direction."

    val serviceModelTitle = if (lang == AppLanguage.HINDI) "मॉडल ग्रूमिंग और लॉन्चिंग" else "Model Grooming & Management"
    val serviceModelDesc = if (lang == AppLanguage.HINDI)
        "नए व अनुभवी मॉडल्स का पेशेवर मार्गदर्शन, प्रोफाइल प्रबंधन और कास्टिंग समर्थन।"
    else
        "Professional coaching, profile management, and casting support for new & pro talent."

    val serviceBridalTitle = if (lang == AppLanguage.HINDI) "रॉयल प्री-वेडिंग और ब्राइडल" else "Royal Pre-Wedding & Bridal"
    val serviceBridalDesc = if (lang == AppLanguage.HINDI)
        "शाही परिधानों और सिनेमैटिक लाइटिंग के साथ अविस्मरणीय जीवन के यादगार पल।"
    else
        "Unforgettable pre-wedding memories captured with royal styling & cinematic illumination."

    // Model Registration
    val modelRegTitle = if (lang == AppLanguage.HINDI) "मॉडल पंजीकरण फॉर्म" else "Model Registration"
    val modelRegSub = if (lang == AppLanguage.HINDI)
        "केवल 18 वर्ष या उससे अधिक आयु के वयस्क ही पंजीकरण कर सकते हैं। अपनी प्रोफाइल दर्ज करें।"
    else
        "Strictly for adults aged 18+. Join our prestigious modeling and talent roster."

    val fullNameLabel = if (lang == AppLanguage.HINDI) "पूरा नाम" else "Full Name"
    val ageLabel = if (lang == AppLanguage.HINDI) "आयु (वर्ष, 18+ अनिवार्य)" else "Age (Years, 18+ required)"
    val genderLabel = if (lang == AppLanguage.HINDI) "लिंग" else "Gender"
    val cityLabel = if (lang == AppLanguage.HINDI) "शहर / जिला" else "City / District"
    val phoneLabel = if (lang == AppLanguage.HINDI) "फ़ोन नंबर (10 अंक)" else "Phone Number"
    val skillsLabel = if (lang == AppLanguage.HINDI) "कौशल व अनुभव (रैंप, अभिनय, डांस, मॉडलिंग)" else "Skills & Experience (Ramp, Acting, Stills)"
    val portfolioUrlLabel = if (lang == AppLanguage.HINDI) "पोर्टफोलियो फोटो लिंक / इंस्टाग्राम आईडी" else "Portfolio Photo Link or Instagram Handle"
    val consentLabel = if (lang == AppLanguage.HINDI)
        "मैं पुष्टि करता/करती हूँ कि मेरी आयु 18 वर्ष या अधिक है और मैं स्टार आर्टिस्ट स्टूडियो की शर्तों और डेटा प्रसंस्करण से सहमत हूँ।"
    else
        "I certify that I am 18+ years of age and consent to Star Artist Studio reviewing and storing my portfolio submission."

    val submitRegButton = if (lang == AppLanguage.HINDI) "पंजीकरण सबमिट करें" else "Submit Registration"
    val regSuccessMsg = if (lang == AppLanguage.HINDI)
        "आपका पंजीकरण सफलतापूर्वक सबमिट हो गया है! हमारी टीम जल्द ही आपसे संपर्क करेगी।"
    else
        "Your model registration was submitted successfully! Our talent team will review and contact you."

    // Shoot Booking
    val bookingTitle = if (lang == AppLanguage.HINDI) "शूट अपॉइंटमेंट बुक करें" else "Book a Production Shoot"
    val bookingSub = if (lang == AppLanguage.HINDI)
        "फोटोग्राफी, मॉडलिंग, संगीत वीडियो अथवा कमर्शियल विज्ञापन शूट शेड्यूल करें।"
    else
        "Schedule your professional photography, modeling portfolio, or cinema video shoot."

    val clientNameLabel = if (lang == AppLanguage.HINDI) "ग्राहक का नाम" else "Client Name"
    val contactPhoneLabel = if (lang == AppLanguage.HINDI) "संपर्क नंबर" else "Contact Phone Number"
    val serviceTypeLabel = if (lang == AppLanguage.HINDI) "सेवा का प्रकार" else "Service Type"
    val dateLabel = if (lang == AppLanguage.HINDI) "पसंदीदा तारीख (उदा. 2026-11-20)" else "Preferred Date (e.g. 2026-11-20)"
    val timeLabel = if (lang == AppLanguage.HINDI) "पसंदीदा समय" else "Preferred Time"
    val locationLabel = if (lang == AppLanguage.HINDI) "शूट स्थान" else "Shoot Location"
    val notesLabel = if (lang == AppLanguage.HINDI) "अतिरिक्त आवश्यकताएं या निर्देश" else "Special Requirements or Notes"
    val voiceToTextTitle = if (lang == AppLanguage.HINDI) "आवाज़ से विवरण दर्ज करें (Voice to Text)" else "Voice-to-Text Description"
    val voiceToTextPrompt = if (lang == AppLanguage.HINDI)
        "माइक पर टैप करके अपनी आवश्यकताएं, पोज़, थीम या ड्रेस कोड बोलकर बताएं..."
    else
        "Tap the microphone to describe your shoot vision, costumes, or themes conversationally..."
    val voiceListening = if (lang == AppLanguage.HINDI) "सुन रहे हैं... कृपया बोलें" else "Listening... Please speak now"
    val voiceTapToStop = if (lang == AppLanguage.HINDI) "रिकॉर्डिंग समाप्त करें" else "Tap to stop recording"
    val voicePermissionNeeded = if (lang == AppLanguage.HINDI) "माइक अनुमति आवश्यक" else "Microphone Permission Required"
    val voicePermissionRationale = if (lang == AppLanguage.HINDI)
        "अपनी फोटोग्राफी व वीडियो आवश्यकताओं को बोलकर दर्ज करने के लिए माइक्रोफ़ोन की अनुमति दें।"
    else
        "Please grant microphone permission to describe your photoshoot requirements conversationally."
    val voiceGrantPermissionBtn = if (lang == AppLanguage.HINDI) "अनुमति दें (Allow Mic)" else "Allow Microphone"
    val voiceNotSupported = if (lang == AppLanguage.HINDI)
        "इस डिवाइस पर वाक् पहचान उपलब्ध नहीं है।"
    else
        "Speech recognition is not available on this device."
    val submitBookingBtn = if (lang == AppLanguage.HINDI) "बुकिंग कन्फर्म करें" else "Confirm Booking Request"
    val bookingSuccessMsg = if (lang == AppLanguage.HINDI)
        "आपकी बुकिंग का अनुरोध दर्ज कर लिया गया है! हम पुष्टि के लिए ${StudioContactDetails.MOBILE_NUMBER} से संपर्क करेंगे।"
    else
        "Booking request placed! Our studio coordinator will contact you to finalize the schedule."

    // Gallery
    val galleryTitle = if (lang == AppLanguage.HINDI) "स्टूडियो गैलरी और पोर्टफोलियो" else "Studio Portfolio Gallery"
    val gallerySub = if (lang == AppLanguage.HINDI)
        "स्टार आर्टिस्ट स्टूडियो के चुनिंदा फोटोग्राफी और 4K वीडियो काम की झलकियां।"
    else
        "High-definition photography and video productions crafted at Star Artist Studio."
    val demoBadge = if (lang == AppLanguage.HINDI) "डेमो कंटेंट" else "DEMO"
    val approvedBadge = if (lang == AppLanguage.HINDI) "सत्यापित" else "APPROVED"
    val filterAll = if (lang == AppLanguage.HINDI) "सभी" else "All"
    val filterFashion = if (lang == AppLanguage.HINDI) "फैशन" else "Fashion"
    val filterCommercial = if (lang == AppLanguage.HINDI) "कमर्शियल" else "Commercial"
    val filterReel = if (lang == AppLanguage.HINDI) "वीडियो रील" else "Video Reel"
    val filterBridal = if (lang == AppLanguage.HINDI) "ब्राइडल" else "Bridal"
    val filterEditorial = if (lang == AppLanguage.HINDI) "एडिटोरियल" else "Editorial"
    val searchPlaceholder = if (lang == AppLanguage.HINDI) "गैलरी में खोजें..." else "Search gallery..."

    // AI Studio
    val aiStudioTitle = if (lang == AppLanguage.HINDI) "स्टार एआई क्रिएटिव स्टूडियो" else "Star AI Creative Studio"
    val aiStudioSub = if (lang == AppLanguage.HINDI)
        "जेमिनी एआई द्वारा संचालित क्रिएटिव विज़न, फोटोशूट कॉन्सेप्ट्स और वीडियो स्टोरीबोर्ड जनरेटर।"
    else
        "Powered by Firebase AI Logic (Gemini API) for creative photo direction, lighting setups, and video storyboards."
    val tabPhotoAi = if (lang == AppLanguage.HINDI) "एआई फोटो कॉन्सेप्ट" else "AI Photo Concept"
    val tabVideoAi = if (lang == AppLanguage.HINDI) "एआई वीडियो स्टोरीबोर्ड" else "AI Video Storyboard"
    val generateBtn = if (lang == AppLanguage.HINDI) "एआई से जनरेट करें" else "Generate with AI"
    val aiPromptPlaceholderPhoto = if (lang == AppLanguage.HINDI)
        "जैसे: गोल्डन ऑवर में रॉयल लहंगा ब्राइडल फोटोशूट के लिए 5 सिनेमैटिक पोज़ और लाइटिंग गाइड..."
    else
        "e.g. 5 cinematic couture poses and lighting setups for a luxury black and gold jewelry campaign..."
    val aiPromptPlaceholderVideo = if (lang == AppLanguage.HINDI)
        "जैसे: 30 सेकंड की हाई-एनर्जी पंजाबी संगीत वीडियो के लिए 4K कैमरा शॉट्स और स्टोरीबोर्ड..."
    else
        "e.g. 30-second music video reel storyboard with drone opening, rim lighting, and slow-mo camera gimbal transitions..."
    val aiQuotaNotice = if (lang == AppLanguage.HINDI)
        "यह सुविधा मुफ़्त कोटा (Spark/Firebase AI Logic) पर आधारित है। कोई छिपी हुई लागत नहीं है।"
    else
        "Operates securely on standard free quota. No paid subscription required."

    // Admin Dashboard
    val adminTitle = if (lang == AppLanguage.HINDI) "एडमिन प्रबंधन डैशबोर्ड" else "Admin Management Console"
    val adminRestricted = if (lang == AppLanguage.HINDI)
        "यह क्षेत्र केवल अधिकृत स्टूडियो एडमिन के लिए आरक्षित है (chanderstudio0@gmail.com)।"
    else
        "Restricted to authorized studio administrators (chanderstudio0@gmail.com)."
    val tabModels = if (lang == AppLanguage.HINDI) "मॉडल आवेदन" else "Model Applications"
    val tabBookings = if (lang == AppLanguage.HINDI) "शूट बुकिंग्स" else "Shoot Bookings"
    val tabGalleryMgmt = if (lang == AppLanguage.HINDI) "गैलरी समीक्षा" else "Gallery Review"
    val tabBlogMgmt = if (lang == AppLanguage.HINDI) "ब्लॉग प्रबंधन" else "Blog Posts"
    val tabAnalytics = if (lang == AppLanguage.HINDI) "विज़ुअल एनालिटिक्स" else "Visual Analytics"
    val approveBtn = if (lang == AppLanguage.HINDI) "स्वीकार करें" else "Approve"
    val rejectBtn = if (lang == AppLanguage.HINDI) "अस्वीकार करें" else "Reject"
    val deleteBtn = if (lang == AppLanguage.HINDI) "हटाएं" else "Delete"
    val updateStatus = if (lang == AppLanguage.HINDI) "स्थिति बदलें" else "Update Status"

    // Visual Analytics Dashboard (Recharts-inspired)
    val analyticsHeaderTitle = if (lang == AppLanguage.HINDI) "स्टूडियो ट्रेंड्स व विज़ुअल एनालिटिक्स" else "Studio Trends & Visual Analytics"
    val analyticsHeaderSubtitle = if (lang == AppLanguage.HINDI)
        "रीचार्ट्स स्टाइल इंटरैक्टिव चार्ट्स: समय के साथ बुकिंग रुझान और लोकप्रिय सेवा अनुरोध"
    else
        "Recharts-style interactive metrics: Booking trends over time & popular service requests"
    val bookingTrendsTitle = if (lang == AppLanguage.HINDI) "समय के साथ बुकिंग रुझान (Booking Trends Over Time)" else "Booking Trends Over Time"
    val popularServicesTitle = if (lang == AppLanguage.HINDI) "लोकप्रिय सेवा अनुरोध (Popular Service Requests)" else "Popular Service Requests"
    val serviceDistributionTitle = if (lang == AppLanguage.HINDI) "सेवा श्रेणी वितरण (Service Mix)" else "Service Category Distribution"
    val trendPeriod7D = if (lang == AppLanguage.HINDI) "7 दिन" else "7 Days"
    val trendPeriod30D = if (lang == AppLanguage.HINDI) "30 दिन" else "30 Days"
    val trendPeriodAll = if (lang == AppLanguage.HINDI) "सभी समय" else "All Time"
    val filterAllBookings = if (lang == AppLanguage.HINDI) "सभी बुकिंग्स" else "All Bookings"
    val filterConfirmedOnly = if (lang == AppLanguage.HINDI) "केवल स्वीकृत" else "Confirmed Only"
    val analyticsKpiTotalBookings = if (lang == AppLanguage.HINDI) "कुल ट्रैक की गई बुकिंग्स" else "Total Bookings Tracked"
    val analyticsKpiConfirmedRate = if (lang == AppLanguage.HINDI) "स्वीकृति अनुपात" else "Confirmation Rate"
    val analyticsKpiPeakDay = if (lang == AppLanguage.HINDI) "सर्वाधिक मांग वाला दिन" else "Peak Activity Day"
    val analyticsKpiTopService = if (lang == AppLanguage.HINDI) "शीर्ष सेवा अनुरोध" else "Top Requested Service"

    // Studio Blog & Photography Tips
    val blogTitle = if (lang == AppLanguage.HINDI) "स्टूडियो ब्लॉग व टिप्स" else "Studio Journal & Photography Tips"
    val blogSub = if (lang == AppLanguage.HINDI)
        "स्टार आर्टिस्ट स्टूडियो से आधिकारिक समाचार, लाइटिंग तकनीक, कैमरा सेटअप्स और पर्दे के पीछे की कहानियां।"
    else
        "Studio news, lighting setups, cinematography breakdowns & behind-the-scenes stories from Star Artist Studio."
    val filterStudioNews = if (lang == AppLanguage.HINDI) "स्टूडियो समाचार" else "Studio News"
    val filterPhotoTips = if (lang == AppLanguage.HINDI) "फोटोग्राफी टिप्स" else "Photography Tips"
    val filterBts = if (lang == AppLanguage.HINDI) "पर्दे के पीछे (BTS)" else "Behind The Scenes"
    val filterGear = if (lang == AppLanguage.HINDI) "उपकरण व लाइटिंग" else "Gear & Lighting"
    val filterModelSpotlight = if (lang == AppLanguage.HINDI) "मॉडल स्पॉटलाइट" else "Model Spotlight"
    val searchBlogPlaceholder = if (lang == AppLanguage.HINDI) "टिप्स, तकनीक या लेख खोजें..." else "Search tips, articles, or gear..."
    val publishPostBtn = if (lang == AppLanguage.HINDI) "नया लेख प्रकाशित करें" else "Create Article"
    val editPostBtn = if (lang == AppLanguage.HINDI) "संपादित करें" else "Edit Post"
    val deletePostBtn = if (lang == AppLanguage.HINDI) "हटाएं" else "Delete"
    val readMoreBtn = if (lang == AppLanguage.HINDI) "पूरा लेख पढ़ें" else "Read Full Article"
    val minReadLabel = if (lang == AppLanguage.HINDI) "मिनट का पाठ" else "min read"
    val postAuthorLabel = if (lang == AppLanguage.HINDI) "स्टार आर्टिस्ट स्टूडियो" else "Star Artist Studio"
    val noPostsFound = if (lang == AppLanguage.HINDI) "इस श्रेणी में कोई लेख उपलब्ध नहीं है।" else "No articles found in this category."
    val postFormTitle = if (lang == AppLanguage.HINDI) "नया ब्लॉग / टिप्स पोस्ट" else "Publish Studio Article"
    val postTitleLabel = if (lang == AppLanguage.HINDI) "लेख का शीर्षक" else "Article Title"
    val postCategoryLabel = if (lang == AppLanguage.HINDI) "श्रेणी" else "Category"
    val postSummaryLabel = if (lang == AppLanguage.HINDI) "संक्षिप्त विवरण (Summary)" else "Summary / Excerpt"
    val postContentLabel = if (lang == AppLanguage.HINDI) "मुख्य सामग्री (Content body)" else "Article Content Body"
    val postImageUrlLabel = if (lang == AppLanguage.HINDI) "कवर फोटो लिंक (वैकल्पिक)" else "Cover Photo Image URL (Optional)"
    val postTagsLabel = if (lang == AppLanguage.HINDI) "टैग्स (कॉमा द्वारा अलग)" else "Tags (comma separated)"

    // Admin Dashboard Metrics
    val adminMetricsTitle = if (lang == AppLanguage.HINDI) "स्टूडियो संचालन मेट्रिक्स" else "Studio Executive Metrics"
    val adminLiveSyncLabel = if (lang == AppLanguage.HINDI) "लाइव सिंक" else "LIVE SYNC"
    val metricPendingApprovals = if (lang == AppLanguage.HINDI) "लंबित स्वीकृतियां" else "Pending Approvals"
    val metricUpcomingBookings = if (lang == AppLanguage.HINDI) "आगामी शूट्स" else "Upcoming Bookings"
    val metricActiveUsers = if (lang == AppLanguage.HINDI) "सक्रिय सदस्य" else "Active Users"
    val metricGalleryShowcase = if (lang == AppLanguage.HINDI) "लाइव गैलरी" else "Live Showcase"
    val metricActionTapToView = if (lang == AppLanguage.HINDI) "देखने के लिए टैप करें" else "Tap to view"

    // Admin Login Page
    val adminLoginTitle = if (lang == AppLanguage.HINDI) "सुरक्षित एडमिन लॉगिन" else "Secure Admin Login"
    val adminLoginSub = if (lang == AppLanguage.HINDI)
        "स्टार आर्टिस्ट स्टूडियो प्रबंधन व सत्यापन पोर्टल"
    else
        "Star Artist Studio Management & Verification Portal"
    val adminAuthorizedNotice = if (lang == AppLanguage.HINDI)
        "यह कंसोल केवल अधिकृत स्टूडियो व्यवस्थापक (${StudioContactDetails.ADMIN_EMAIL}) के लिए आरक्षित है।"
    else
        "Access strictly restricted to authorized studio administrator (${StudioContactDetails.ADMIN_EMAIL})."
    val adminCurrentAccountLabel = if (lang == AppLanguage.HINDI) "वर्तमान सक्रिय सत्र" else "Current Active Session"
    val adminAccessDeniedLabel = if (lang == AppLanguage.HINDI)
        "पहुंच अस्वीकृत: वर्तमान खाता एडमिन रोस्टर में अधिकृत नहीं है।"
    else
        "Access Denied: This account is not in the authorized studio administrator roster."
    val adminSignInWithGoogle = if (lang == AppLanguage.HINDI)
        "अधिकृत एडमिन खाते से साइन इन करें"
    else
        "Sign In with Authorized Admin Account"
    val adminSwitchAccount = if (lang == AppLanguage.HINDI)
        "खाता बदलें (Switch Account)"
    else
        "Switch Google Account"
    val adminSecurityPolicy = if (lang == AppLanguage.HINDI)
        "फ़ायरबेस प्रमाणीकरण और शून्य-विश्वास सुरक्षा नियमों द्वारा सुरक्षित।"
    else
        "Protected by Firebase Authentication & Zero-Trust Cloud Firestore rules."
    val adminBackToHome = if (lang == AppLanguage.HINDI) "स्टूडियो होम पर वापस जाएं" else "Back to Studio Home"

    // Contact
    val contactTitle = if (lang == AppLanguage.HINDI) "स्टूडियो का पता व संपर्क" else "Official Studio Address & Contact"
    val addressLabel = if (lang == AppLanguage.HINDI) "आधिकारिक स्टूडियो पता" else "Official Studio Address"
    val openHours = if (lang == AppLanguage.HINDI) "खुलने का समय: प्रातः 10:00 बजे से सायं 8:00 बजे तक (प्रतिदिन)" else "Studio Hours: 10:00 AM - 8:00 PM (Everyday)"
    val openWhatsAppBtn = if (lang == AppLanguage.HINDI) "व्हाट्सएप चैट खोलें (${StudioContactDetails.WHATSAPP_NUMBER})" else "Open WhatsApp (${StudioContactDetails.WHATSAPP_NUMBER})"
    val makeCallBtn = if (lang == AppLanguage.HINDI) "सीधा कॉल करें (${StudioContactDetails.MOBILE_NUMBER})" else "Direct Call (${StudioContactDetails.MOBILE_NUMBER})"

    // Privacy & Data Deletion
    val privacyPolicyTitle = if (lang == AppLanguage.HINDI) "गोपनीयता नीति व शर्तें" else "Privacy Policy & Terms"
    val requestDataDeletionBtn = if (lang == AppLanguage.HINDI) "डेटा हटाने का अनुरोध (Data Deletion)" else "Request Data Deletion"
    val dataDeletionNotice = if (lang == AppLanguage.HINDI)
        "आप किसी भी समय अपने सभी मॉडल पंजीकरण और बुकिंग रिकॉर्ड हटाने का अनुरोध कर सकते हैं।"
    else
        "You can delete your submissions, portfolio records, and bookings at any time."
}
