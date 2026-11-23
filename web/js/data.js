export const STUDIO_CONTACT = {
  studioName: "STAR ARTIST STUDIO",
  tagline: "Premium Modeling, Photography & 4K Cinema Video Production Studio",
  address: "Star Artist Studio, Talwara Khurd, Tehsil Ellenabad, District Sirsa, Haryana, India - 125102.",
  village: "Talwara Khurd",
  tehsil: "Ellenabad",
  district: "Sirsa",
  state: "Haryana",
  country: "India",
  pincode: "125102",
  mobileNumber: "9812890081",
  formattedMobile: "+91 9812890081",
  callUri: "tel:+919812890081",
  whatsappNumber: "9541024098",
  formattedWhatsapp: "+91 9541024098",
  whatsappUrl: "https://wa.me/919541020948",
  mapsUrl:
    "https://www.google.com/maps/search/?api=1&query=" +
    encodeURIComponent("Star Artist Studio, Talwara Khurd, Tehsil Ellenabad, District Sirsa, Haryana, India - 125102."),
  adminEmail: "chanderstudio0@gmail.com",
  hours: "10:00 AM – 8:00 PM (Everyday)",
  socialLinks: {
    instagram: "https://www.instagram.com/starartiststudio",
    facebook: "https://www.facebook.com/starartiststudio",
    linkedin: "https://www.linkedin.com/company/star-artist-studio"
  }
};

export const FIREBASE_PROJECT_DEFAULTS = {
  projectId: "gen-lang-client-0548683163",
  authDomain: "gen-lang-client-0548683163.firebaseapp.com",
  storageBucket: "gen-lang-client-0548683163.firebasestorage.app",
  messagingSenderId: "634443186065",
  databaseId: "ai-studio-android-stararti-4ffde0d6-02fd-4194-8e31-3359a6b9588b"
};

export const STUDIO_SERVICES = [
  {
    id: "pkg_photo_fashion",
    category: "photography",
    imageUrl: "/images/gallery-fashion.jpg",
    titleEn: "High-Fashion Editorial",
    titleHi: "हाई-फैशन एडिटोरियल",
    startingPriceInr: 14999,
    priceFormatted: "₹14,999",
    durationEn: "4-5 Hours Studio Session",
    durationHi: "4-5 घंटे स्टूडियो सत्र",
    tagEn: "SIGNATURE EDITORIAL",
    tagHi: "सिग्नेचर एडिटोरियल",
    summaryEn: "Vogue-caliber fashion stills with professional high-key and rim lighting setups.",
    summaryHi: "पेशेवर हाई-की और रिम लाइटिंग सेटअप के साथ अंतरराष्ट्रीय स्तर के फैशन स्टिल्स।",
    recommendedServiceType: "Photography (High-Fashion Editorial)",
    isPopular: true,
    tiers: [
      {
        id: "essential",
        tierNameEn: "Essential",
        tierNameHi: "एसेंशियल",
        priceFormatted: "₹14,999",
        durationEn: "3 Hours Studio",
        turnaroundEn: "72 Hours",
        deliverablesEn: [
          "2 wardrobe looks & 2 editorial lighting backdrops",
          "15 retouched high-resolution editorial frames",
          "Digital color-calibrated gallery link"
        ]
      },
      {
        id: "pro",
        tierNameEn: "Pro",
        tierNameHi: "प्रो",
        priceFormatted: "₹21,999",
        durationEn: "4-5 Hours Studio",
        turnaroundEn: "48 Hours",
        isRecommended: true,
        deliverablesEn: [
          "4 wardrobe looks with multi-rim strobe setups",
          "25 magazine-grade master retouched stills",
          "1 vertical BTS editorial teaser reel"
        ]
      },
      {
        id: "flagship",
        tierNameEn: "Flagship",
        tierNameHi: "फ्लैगशिप",
        priceFormatted: "₹34,999",
        durationEn: "Full Day Creative",
        turnaroundEn: "24 Hours Priority",
        deliverablesEn: [
          "6 designer looks + on-set stylist & makeup bay",
          "45 beauty & skin-retouched print-ready masters",
          "Cinema 4K campaign teaser + raw contact sheet"
        ]
      }
    ]
  },
  {
    id: "pkg_photo_portrait",
    category: "photography",
    imageUrl: "/images/gallery-portrait.jpg",
    titleEn: "Studio Portrait & Glamour",
    titleHi: "स्टूडियो पोर्ट्रेट व ग्लैमर",
    startingPriceInr: 7999,
    priceFormatted: "₹7,999",
    durationEn: "2-3 Hours Session",
    durationHi: "2-3 घंटे सत्र",
    tagEn: "POPULAR CHOICE",
    tagHi: "लोकप्रिय चयन",
    summaryEn: "Crisp, expressive personal and corporate portraiture with artistic shadow gradients.",
    summaryHi: "सटीक प्रकाश व छाया संतुलन के साथ व्यक्तिगत और कॉर्पोरेट पोर्ट्रेट फोटोग्राफी।",
    recommendedServiceType: "Photography (Portrait & Glamour Stills)",
    isPopular: false,
    tiers: [
      {
        id: "essential",
        tierNameEn: "Essential",
        tierNameHi: "एसेंशियल",
        priceFormatted: "₹7,999",
        durationEn: "90 Mins Session",
        turnaroundEn: "48 Hours",
        deliverablesEn: [
          "1 signature backdrop + softbox beauty dish",
          "10 retouched portraits in color & monochrome",
          "LinkedIn & casting-ready headshot crops"
        ]
      },
      {
        id: "pro",
        tierNameEn: "Pro",
        tierNameHi: "प्रो",
        priceFormatted: "₹11,999",
        durationEn: "2.5 Hours Session",
        turnaroundEn: "36 Hours",
        isRecommended: true,
        deliverablesEn: [
          "2 lighting setups (Softbox Beauty + Hard Rim)",
          "18 expertly retouched glamour portraits",
          "3 outfit changes with pose coaching"
        ]
      },
      {
        id: "flagship",
        tierNameEn: "Flagship",
        tierNameHi: "फ्लैगशिप",
        priceFormatted: "₹17,999",
        durationEn: "4 Hours Studio",
        turnaroundEn: "24 Hours",
        deliverablesEn: [
          "4 bespoke textured studio sets + dramatic gels",
          "30 full-resolution retouched portraits",
          "2 archival framed 12x18 metallic prints"
        ]
      }
    ]
  },
  {
    id: "pkg_photo_bridal",
    category: "photography",
    imageUrl: "/images/gallery-bridal.jpg",
    titleEn: "Royal Pre-Wedding & Bridal",
    titleHi: "रॉयल प्री-वेडिंग और ब्राइडल",
    startingPriceInr: 29999,
    priceFormatted: "₹29,999",
    durationEn: "Full Day (Studio + Outdoor)",
    durationHi: "पूरा दिन (स्टूडियो + आउटडोर)",
    tagEn: "ROYAL LUXURY",
    tagHi: "रॉयल लग्जरी",
    summaryEn: "Cinematic storytelling capturing timeless romance with royal sets and scenic outdoor locations.",
    summaryHi: "शाही सेट्स और रमणीय आउटडोर लोकेशन्स पर तैयार किया गया सिनेमाई प्री-वेडिंग शूट।",
    recommendedServiceType: "Photography (Royal Pre-Wedding & Bridal)",
    isPopular: true,
    tiers: [
      {
        id: "essential",
        tierNameEn: "Essential",
        tierNameHi: "एसेंशियल",
        priceFormatted: "₹29,999",
        durationEn: "6 Hours Studio",
        turnaroundEn: "5 Days",
        deliverablesEn: [
          "Indoor royal sets + 3 bridal/couple attire looks",
          "35 retouched high-definition romantic stills",
          "Curated cloud album for family sharing"
        ]
      },
      {
        id: "pro",
        tierNameEn: "Pro",
        tierNameHi: "प्रो",
        priceFormatted: "₹44,999",
        durationEn: "Full Day Hybrid",
        turnaroundEn: "4 Days",
        isRecommended: true,
        deliverablesEn: [
          "Studio + Sirsa/Ellenabad heritage outdoor sets",
          "55 retouched frames + velvet teaser photo book",
          "60-sec 4K save-the-date highlight reel"
        ]
      },
      {
        id: "flagship",
        tierNameEn: "Flagship",
        tierNameHi: "फ्लैगशिप",
        priceFormatted: "₹69,999",
        durationEn: "2-Day Destination",
        turnaroundEn: "72 Hours Sneak Peek",
        deliverablesEn: [
          "2-day multi-location cinema crew + drone aerials",
          "100 master stills + handcrafted Italian leather album",
          "3-minute 4K cinematic pre-wedding film"
        ]
      }
    ]
  },
  {
    id: "pkg_wedding_collection",
    category: "photography",
    imageUrl: "/images/gallery-bridal.jpg",
    titleEn: "Complete Wedding Photography & Cinema",
    titleHi: "संपूर्ण वेडिंग फोटोग्राफी व सिनेमा",
    startingPriceInr: 45000,
    priceFormatted: "₹45,000",
    durationEn: "1 to 3 Days Full Wedding Coverage",
    durationHi: "1 से 3 दिन संपूर्ण वेडिंग कवरेज",
    tagEn: "WEDDING TIERS",
    tagHi: "वेडिंग पैकेज",
    summaryEn: "End-to-end wedding photography, candid storytelling, drone aerials & 4K cinema films across Basic, Premium, and Luxury wedding tiers.",
    summaryHi: "बेसिक, प्रीमियम और लक्ज़री वेडिंग पैकेज में संपूर्ण वेडिंग फोटोग्राफी, कैंडिड स्टोरीटेलिंग, ड्रोन और 4K सिनेमा फिल्म।",
    recommendedServiceType: "Wedding Photography & Cinema",
    isWeddingCollection: true,
    isPopular: true,
    tiers: [
      {
        id: "wedding_basic",
        tierNameEn: "Basic Wedding Package",
        tierNameHi: "बेसिक वेडिंग पैकेज",
        priceInr: 45000,
        priceFormatted: "₹45,000",
        durationEn: "1-Day Wedding Ceremony",
        turnaroundEn: "7 Days Delivery",
        deliverablesEn: [
          "Full wedding day Traditional Photography + HD Videography",
          "1 Candid Photographer for key rituals & family portraits",
          "150 color-graded high-resolution wedding photographs",
          "30-sheet premium printed matte wedding photo album",
          "Private cloud gallery link for family sharing"
        ]
      },
      {
        id: "wedding_premium",
        tierNameEn: "Premium Wedding Package",
        tierNameHi: "प्रीमियम वेडिंग पैकेज",
        priceInr: 85000,
        priceFormatted: "₹85,000",
        durationEn: "2-Day Events (Haldi, Mehndi & Wedding)",
        turnaroundEn: "5 Days Priority",
        isRecommended: true,
        deliverablesEn: [
          "2-day coverage across Haldi, Mehndi, Sangeet & Wedding ceremony",
          "Candid + Traditional Photography & 4K Cinematic Videography",
          "Drone aerial coverage for Baraat & venue establishing shots",
          "50-sheet luxury Karizma hardbound wedding album + couple box",
          "3-minute 4K cinematic wedding highlight film + 2 Instagram reels",
          "350 retouched high-resolution stills + digital cloud backup"
        ]
      },
      {
        id: "wedding_luxury",
        tierNameEn: "Luxury Wedding Package",
        tierNameHi: "लक्ज़री वेडिंग पैकेज",
        priceInr: 145000,
        priceFormatted: "₹1,45,000",
        durationEn: "3-Day Royal Destination Coverage",
        turnaroundEn: "48 Hours Sneak Peek",
        deliverablesEn: [
          "3-day complete royal destination coverage with 6-member cinema crew",
          "Complimentary Pre-Wedding editorial portrait & teaser film shoot",
          "Dual 4K cinema cameras, gimbal rig, drone aerials & LED stage lighting",
          "Same-Day Edit (SDE) highlight film screened live at Reception",
          "2 Handcrafted Italian leather luxury albums + 2 mini parent albums",
          "Full 4K master wedding feature film + 5 vertical social cutdowns",
          "600+ editorial retouched photographs + USB crystal keepsake box"
        ]
      }
    ]
  },
  {
    id: "pkg_photo_commercial",
    category: "photography",
    imageUrl: "/images/gallery-commercial.jpg",
    titleEn: "Commercial Catalog & E-Commerce",
    titleHi: "कमर्शियल कैटलॉग व ई-कॉमर्स",
    startingPriceInr: 11999,
    priceFormatted: "₹11,999",
    durationEn: "Half Day Studio Booking",
    durationHi: "आधा दिन स्टूडियो बुकिंग",
    tagEn: "BRAND ESSENTIAL",
    tagHi: "ब्रांड्स के लिए",
    summaryEn: "Pixel-perfect product, apparel, and footwear catalog shoots calibrated for Amazon and luxury boutiques.",
    summaryHi: "अमेज़ॉन, मिंत्रा और लक्ज़री बुटीक के लिए मानक श्वेत पृष्ठभूमि और लाइफस्टाइल शूट।",
    recommendedServiceType: "Photography (Commercial Product & Catalog)",
    isPopular: false,
    tiers: [
      {
        id: "essential",
        tierNameEn: "Essential",
        tierNameHi: "एसेंशियल",
        priceFormatted: "₹11,999",
        durationEn: "Half Day (4 Hours)",
        turnaroundEn: "48 Hours",
        deliverablesEn: [
          "Up to 15 product SKUs on pure 255 RGB white",
          "3 angles per SKU with color accuracy",
          "Amazon / Flipkart / Shopify size exports"
        ]
      },
      {
        id: "pro",
        tierNameEn: "Pro",
        tierNameHi: "प्रो",
        priceFormatted: "₹19,999",
        durationEn: "Full Day (7 Hours)",
        turnaroundEn: "36 Hours",
        isRecommended: true,
        deliverablesEn: [
          "Up to 30 SKUs + styled lifestyle flat-lay props",
          "Ghost mannequin & reflective jewelry lighting",
          "Web banners + social carousel cutdowns"
        ]
      },
      {
        id: "flagship",
        tierNameEn: "Flagship",
        tierNameHi: "फ्लैगशिप",
        priceFormatted: "₹32,999",
        durationEn: "Full Day + Model",
        turnaroundEn: "24 Hours",
        deliverablesEn: [
          "50 SKUs + verified Star Artist catalog model",
          "Custom art-directed brand set & lookbook",
          "4K motion product spins for paid ads"
        ]
      }
    ]
  },
  {
    id: "pkg_model_portfolio",
    category: "modeling",
    imageUrl: "/images/model-portrait.jpg",
    titleEn: "Comp-Card & Agency Portfolio",
    titleHi: "कॉम्प-कार्ड व एजेंसी पोर्टफोलियो",
    startingPriceInr: 12999,
    priceFormatted: "₹12,999",
    durationEn: "3-4 Hours Direction Session",
    durationHi: "3-4 घंटे निर्देशन सत्र",
    tagEn: "CAREER LAUNCHPAD",
    tagHi: "करियर लॉन्चपैड",
    summaryEn: "Complete model audition package including standardized industry comp-cards and expressive posing.",
    summaryHi: "मानक मॉडलिंग उद्योग कॉम्प-कार्ड, क्लोज-अप्स, फुल-लेंथ शॉट्स और पोज़िंग गाइडेंस।",
    recommendedServiceType: "Modeling (Portfolio & Comp-Card Shoot)",
    isPopular: true,
    tiers: [
      {
        id: "essential",
        tierNameEn: "Essential",
        tierNameHi: "एसेंशियल",
        priceFormatted: "₹12,999",
        durationEn: "3 Hours Studio",
        turnaroundEn: "72 Hours",
        deliverablesEn: [
          "2-look audition comp-card + clean Polaroids",
          "12 retouched headshots & full-length frames",
          "Studio director posture & camera orientation"
        ]
      },
      {
        id: "pro",
        tierNameEn: "Pro",
        tierNameHi: "प्रो",
        priceFormatted: "₹18,999",
        durationEn: "4-5 Hours Studio",
        turnaroundEn: "48 Hours",
        isRecommended: true,
        deliverablesEn: [
          "4-look digital & printed 300gsm agency comp-card",
          "24 editorial, commercial & ethnic portfolio stills",
          "Priority placement on Star Artist 18+ Roster"
        ]
      },
      {
        id: "flagship",
        tierNameEn: "Flagship",
        tierNameHi: "फ्लैगशिप",
        priceFormatted: "₹27,999",
        durationEn: "Full Day + Reel",
        turnaroundEn: "36 Hours",
        deliverablesEn: [
          "6 designer looks with HMUA hair & styling team",
          "40 retouched agency & magazine masters",
          "4K audition introduction & ramp walk showreel"
        ]
      }
    ]
  },
  {
    id: "pkg_model_runway",
    category: "modeling",
    imageUrl: "/images/gallery-fashion.jpg",
    titleEn: "Runway & Lookbook Motion",
    titleHi: "रैनवे व लुकबुक मोशन",
    startingPriceInr: 18999,
    priceFormatted: "₹18,999",
    durationEn: "Full Day Production",
    durationHi: "पूरा दिन प्रोडक्शन",
    tagEn: "DESIGNER LOOKBOOK",
    tagHi: "डिज़ाइनर लुकबुक",
    summaryEn: "Dynamic runway walk pacing with continuous lighting and synchronized slow-motion video captures.",
    summaryHi: "कैटवॉक और हाई-फैशन गारमेंट्स के लिए स्लो-मोशन वीडियो और स्टिल्स का सम्मिलित सत्र।",
    recommendedServiceType: "Modeling (Runway & Lookbook Session)",
    isPopular: false,
    tiers: [
      {
        id: "essential",
        tierNameEn: "Essential",
        tierNameHi: "एसेंशियल",
        priceFormatted: "₹18,999",
        durationEn: "4 Hours Bay",
        turnaroundEn: "4 Days",
        deliverablesEn: [
          "3 designer outfit transformations",
          "20 still frames + 30-sec 4K runway walk clip",
          "Standard sound design & color grade"
        ]
      },
      {
        id: "pro",
        tierNameEn: "Pro",
        tierNameHi: "प्रो",
        priceFormatted: "₹26,999",
        durationEn: "Full Day Studio",
        turnaroundEn: "72 Hours",
        isRecommended: true,
        deliverablesEn: [
          "5 ensemble transformations + 4K 120fps slow-mo",
          "35 high-fashion lookbook stills",
          "Color-graded agency showreel + vertical IG reel"
        ]
      },
      {
        id: "flagship",
        tierNameEn: "Flagship",
        tierNameHi: "फ्लैगशिप",
        priceFormatted: "₹39,999",
        durationEn: "Full Day + Crew",
        turnaroundEn: "48 Hours",
        deliverablesEn: [
          "8 designer garments + motorized runway dolly rig",
          "60 lookbook masters + digital catalog",
          "3 bespoke vertical & widescreen campaign cuts"
        ]
      }
    ]
  },
  {
    id: "pkg_video_music",
    category: "production",
    imageUrl: "/images/hero-banner.jpg",
    titleEn: "4K Cinema Music Video Direction",
    titleHi: "4K सिनेमा संगीत वीडियो निर्देशन",
    startingPriceInr: 49999,
    priceFormatted: "₹49,999",
    durationEn: "2-Day Multi-Location Shoot",
    durationHi: "2-दिवसीय मल्टी-लोकेशन शूट",
    tagEn: "FLAGSHIP CINEMA",
    tagHi: "फ्लैगशिप सिनेमा",
    summaryEn: "Full cinema-grade music video production from script storyboard to 4K DaVinci color grade.",
    summaryHi: "पटकथा व स्टोरीबोर्ड से लेकर 4K डाविंची कलर ग्रेडिंग तक संपूर्ण संगीत वीडियो निर्माण।",
    recommendedServiceType: "Video (4K Cinema Music Video Direction)",
    isPopular: true,
    tiers: [
      {
        id: "essential",
        tierNameEn: "Essential",
        tierNameHi: "एसेंशियल",
        priceFormatted: "₹49,999",
        durationEn: "1-Day Studio Bay",
        turnaroundEn: "7 Days",
        deliverablesEn: [
          "4K cinema camera + gimbal lip-sync performance shoot",
          "In-house lighting stage with haze & RGB tube rig",
          "Full YouTube 4K master + 15-sec Reels teaser"
        ]
      },
      {
        id: "pro",
        tierNameEn: "Pro",
        tierNameHi: "प्रो",
        priceFormatted: "₹74,999",
        durationEn: "2-Day Hybrid",
        turnaroundEn: "7 Days",
        isRecommended: true,
        deliverablesEn: [
          "Narrative storyboard + studio & outdoor Sirsa sets",
          "Motorized dolly, smoke rig & DaVinci Resolve DI grade",
          "4K DCI YouTube master + 3 vertical social promos"
        ]
      },
      {
        id: "flagship",
        tierNameEn: "Flagship",
        tierNameHi: "फ्लैगशिप",
        priceFormatted: "₹1,19,999",
        durationEn: "3-Day Cinema Crew",
        turnaroundEn: "10 Days Master",
        deliverablesEn: [
          "Multi-location unit, drone aerials & verified cast",
          "Anamorphic lens kit + full VFX & sound mastering",
          "YouTube 4K HDR master + poster photoshoot included"
        ]
      }
    ]
  },
  {
    id: "pkg_video_brand_ad",
    category: "production",
    imageUrl: "/images/gallery-commercial.jpg",
    titleEn: "Commercial Brand Ad Film",
    titleHi: "कमर्शियल ब्रांड विज्ञापन फिल्म",
    startingPriceInr: 24999,
    priceFormatted: "₹24,999",
    durationEn: "Full Day Studio Bay",
    durationHi: "पूरा दिन स्टूडियो बे",
    tagEn: "COMMERCIAL PRO",
    tagHi: "कमर्शियल प्रो",
    summaryEn: "High-impact 30-60 second television and digital ad films highlighting products and services.",
    summaryHi: "ब्रांड्स व व्यवसायों के लिए प्रभावशाली 30-60 सेकंड के टीवी और डिजिटल विज्ञापन।",
    recommendedServiceType: "Video (Commercial Brand Ad Film)",
    isPopular: false,
    tiers: [
      {
        id: "essential",
        tierNameEn: "Essential",
        tierNameHi: "एसेंशियल",
        priceFormatted: "₹24,999",
        durationEn: "Half Day Studio",
        turnaroundEn: "4 Days",
        deliverablesEn: [
          "30-sec crisp commercial spot shot in 4K UHD",
          "Licensed background track + clean logo sting",
          "16:9 landscape + 9:16 Instagram/WhatsApp cut"
        ]
      },
      {
        id: "pro",
        tierNameEn: "Pro",
        tierNameHi: "प्रो",
        priceFormatted: "₹39,999",
        durationEn: "Full Day Studio",
        turnaroundEn: "5 Days",
        isRecommended: true,
        deliverablesEn: [
          "60-sec script-to-screen ad film with voiceover",
          "Motion graphics product callouts & typography",
          "3 A/B tested ad hooks for Meta & YouTube Ads"
        ]
      },
      {
        id: "flagship",
        tierNameEn: "Flagship",
        tierNameHi: "फ्लैगशिप",
        priceFormatted: "₹64,999",
        durationEn: "2-Day Production",
        turnaroundEn: "7 Days",
        deliverablesEn: [
          "Full script, 2 on-camera models & macro product rig",
          "Broadcast loudness mix & cinema DI color grading",
          "90-sec brand film + 15s/30s TV cutdowns"
        ]
      }
    ]
  }
];

export const SHOWCASE_GALLERY_SEEDS = [
  {
    id: "seed_fashion",
    title: "Royal Gold Rim Editorial",
    category: "Editorial",
    mediaType: "photo",
    mediaUrl: "/images/gallery-fashion.jpg",
    status: "approved",
    isDemo: true,
    metadata: {
      camera: "Sony α7R V",
      lens: "85mm f/1.4 GM",
      aperture: "f/2.2",
      shutterSpeed: "1/200s",
      iso: "ISO 100",
      dateTaken: "2026-10-14"
    }
  },
  {
    id: "seed_bridal",
    title: "Heritage Bridal Majesty",
    category: "Bridal",
    mediaType: "photo",
    mediaUrl: "/images/gallery-bridal.jpg",
    status: "approved",
    isDemo: true,
    metadata: {
      camera: "Canon EOS R5",
      lens: "50mm f/1.2L",
      aperture: "f/1.8",
      shutterSpeed: "1/250s",
      iso: "ISO 160",
      dateTaken: "2026-10-28"
    }
  },
  {
    id: "seed_portrait",
    title: "Noir Dramatic Studio Portrait",
    category: "Portrait",
    mediaType: "photo",
    mediaUrl: "/images/gallery-portrait.jpg",
    status: "approved",
    isDemo: true,
    metadata: {
      camera: "Sony α7R V",
      lens: "85mm f/1.4 GM",
      aperture: "f/2.8",
      shutterSpeed: "1/160s",
      iso: "ISO 100",
      dateTaken: "2026-11-02"
    }
  },
  {
    id: "seed_commercial",
    title: "Luxury Fragrance & Product",
    category: "Commercial",
    mediaType: "photo",
    mediaUrl: "/images/gallery-commercial.jpg",
    status: "approved",
    isDemo: true,
    metadata: {
      camera: "Canon EOS R5",
      lens: "100mm f/2.8L Macro",
      aperture: "f/8.0",
      shutterSpeed: "1/125s",
      iso: "ISO 100",
      dateTaken: "2026-11-07"
    }
  },
  {
    id: "seed_model",
    title: "Runway Talent Headshot",
    category: "Fashion",
    mediaType: "photo",
    mediaUrl: "/images/model-portrait.jpg",
    status: "approved",
    isDemo: true,
    metadata: {
      camera: "Sony α7 IV",
      lens: "70-200mm f/2.8 GM II",
      aperture: "f/2.8",
      shutterSpeed: "1/250s",
      iso: "ISO 200",
      dateTaken: "2026-11-12"
    }
  }
];
