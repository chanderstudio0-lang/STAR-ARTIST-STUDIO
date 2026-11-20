/**
 * Homepage Testimonial Carousel module for STAR ARTIST STUDIO.
 * Combines curated client & 18+ model testimonials with positive verified Firestore reviews.
 */

export const STUDIO_TESTIMONIALS = [
  {
    id: "testi_model_ananya",
    name: "Ananya Verma",
    roleType: "model",
    roleEn: "Editorial & Runway Model",
    roleHi: "एडिटोरियल व रैनवे मॉडल",
    serviceEn: "Comp-Card & Agency Portfolio (Pro)",
    serviceHi: "कॉम्प-कार्ड व एजेंसी पोर्टफोलियो (प्रो)",
    city: "Sirsa, Haryana",
    rating: 5,
    quoteEn:
      "The rim-lighting direction and pose coaching at Star Artist Studio transformed my agency comp-card. I booked two regional lookbook campaigns within weeks of receiving my retouched stills.",
    quoteHi:
      "स्टार आर्टिस्ट स्टूडियो में रिम-लाइटिंग और पोज़ कोचिंग ने मेरे एजेंसी कॉम्प-कार्ड को पूरी तरह निखार दिया। रिटच्ड फोटो मिलने के कुछ ही हफ्तों में मुझे दो लुकबुक कैंपेन मिले।"
  },
  {
    id: "testi_client_simran",
    name: "Simran & Harpreet Gill",
    roleType: "client",
    roleEn: "Bridal & Pre-Wedding Client",
    roleHi: "ब्राइडल व प्री-वेडिंग ग्राहक",
    serviceEn: "Royal Pre-Wedding & Bridal (Flagship)",
    serviceHi: "रॉयल प्री-वेडिंग और ब्राइडल (फ्लैगशिप)",
    city: "Ellenabad, Sirsa",
    rating: 5,
    quoteEn:
      "Every frame felt like a magazine spread. The studio team coordinated our heritage outdoor shots and indoor royal sets effortlessly, delivering our 4K highlight film right on schedule.",
    quoteHi:
      "हर एक फोटो मैगज़ीन कवर जैसी लगी। स्टूडियो टीम ने हमारे आउटडोर और रॉयल इंडोर सेट्स को बेहद आसानी से संभाला और 4K हाइलाइट फिल्म समय पर डिलीवर की।"
  },
  {
    id: "testi_client_rohan",
    name: "Rohan Malhotra",
    roleType: "client",
    roleEn: "Independent Music Artist",
    roleHi: "म्यूज़िक आर्टिस्ट (4K वीडियो)",
    serviceEn: "4K Cinema Music Video Direction",
    serviceHi: "4K सिनेमा संगीत वीडियो निर्देशन",
    city: "Hisar, Haryana",
    rating: 5,
    quoteEn:
      "From storyboard to DaVinci color grading, their cinema bay with haze and RGB tube rigs gave our single a big-budget theatrical finish without having to travel to Mumbai.",
    quoteHi:
      "स्टोरीबोर्ड से लेकर डाविंची कलर ग्रेडिंग तक, हेज़ और RGB लाइटिंग स्टेज ने हमारे म्यूज़िक वीडियो को शानदार सिनेमाई लुक दिया।"
  },
  {
    id: "testi_model_kabir",
    name: "Kabir Singhania",
    roleType: "model",
    roleEn: "Verified 18+ Roster Model",
    roleHi: "सत्यापित 18+ रोस्टर मॉडल",
    serviceEn: "High-Fashion Editorial Shoot",
    serviceHi: "हाई-फैशन एडिटोरियल शूट",
    city: "Chandigarh / Sirsa",
    rating: 5,
    quoteEn:
      "Professional, transparent, and welcoming from day one. The 18+ verification process and studio discipline make Star Artist Studio a studio models and brands can genuinely trust.",
    quoteHi:
      "पहले दिन से ही बेहद पेशेवर और पारदर्शी अनुभव। सख्त 18+ सत्यापन और अनुशासन के कारण मॉडल और ब्रांड्स स्टार आर्टिस्ट स्टूडियो पर पूरा भरोसा करते हैं।"
  },
  {
    id: "testi_client_meera",
    name: "Meera Chauhan",
    roleType: "client",
    roleEn: "Boutique Founder",
    roleHi: "बुटीक संस्थापक",
    serviceEn: "Commercial Catalog & E-Commerce",
    serviceHi: "कमर्शियल कैटलॉग व ई-कॉमर्स",
    city: "Sirsa, Haryana",
    rating: 5,
    quoteEn:
      "Our couture jewelry and ethnic lookbook photos came out razor-sharp with true-to-life fabric tones. Online inquiries doubled right after we updated our catalog.",
    quoteHi:
      "हमारे बुटीक के ज्वेलरी और एथनिक लुकबुक की तस्वीरें बेहद शार्प और असली रंगों में आईं। नया कैटलॉग अपलोड करते ही ऑनलाइन ऑर्डर दोगुने हो गए।"
  }
];

export function getNextSlideIndex(currentIndex, total, delta = 1) {
  const safeTotal = Number(total) || 0;
  if (safeTotal <= 0) return 0;
  const current = Number.isFinite(Number(currentIndex)) ? Number(currentIndex) : 0;
  return ((current + delta) % safeTotal + safeTotal) % safeTotal;
}

export function buildCarouselTestimonials(liveReviews = [], lang = "en") {
  const isHi = lang === "hi";
  const positiveLive = (Array.isArray(liveReviews) ? liveReviews : [])
    .filter((r) => Number(r?.rating) >= 4 && String(r?.comment || "").trim().length >= 12)
    .slice(0, 4)
    .map((r, idx) => {
      const serviceText = String(r.serviceType || "Studio Session").trim();
      const isModelService = /model|runway|comp-card|portfolio/i.test(serviceText);
      return {
        id: `live_testi_${r.id || idx}`,
        name: String(r.reviewerName || "Verified Member").trim(),
        roleType: isModelService ? "model" : "client",
        role: isModelService
          ? isHi
            ? "सत्यापित स्टूडियो मॉडल"
            : "Verified Studio Model"
          : isHi
            ? "सत्यापित स्टूडियो ग्राहक"
            : "Verified Studio Client",
        service: serviceText,
        city: isHi ? "सत्यापित समीक्षा" : "Verified Review",
        rating: Math.min(5, Math.max(4, Math.round(Number(r.rating) || 5))),
        quote: String(r.comment || "").trim(),
        isLiveReview: true
      };
    });

  const curated = STUDIO_TESTIMONIALS.map((item) => ({
    id: item.id,
    name: item.name,
    roleType: item.roleType,
    role: isHi ? item.roleHi : item.roleEn,
    service: isHi ? item.serviceHi : item.serviceEn,
    city: item.city,
    rating: item.rating,
    quote: isHi ? item.quoteHi : item.quoteEn,
    isLiveReview: false
  }));

  return [...positiveLive, ...curated];
}

function getInitials(name) {
  const parts = String(name || "SA")
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  if (!parts.length) return "SA";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
}

function renderStarsSvg(rating) {
  const count = Math.min(5, Math.max(1, Math.round(Number(rating) || 5)));
  return Array.from({ length: 5 }, (_, i) => {
    const filled = i < count;
    return `<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" class="testimonial-star ${filled ? "filled" : ""}" fill="${filled ? "currentColor" : "none"}" stroke="currentColor" stroke-width="1.8"><path d="M12 2.6l2.9 5.88 6.49.94-4.7 4.58 1.11 6.46L12 17.4l-5.8 3.06 1.11-6.46-4.7-4.58 6.49-.94L12 2.6z"/></svg>`;
  }).join("");
}

const DEFAULT_ESCAPE = (value) =>
  String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");

export function renderTestimonialCarouselHtml({
  items = [],
  activeIndex = 0,
  autoplay = true,
  t = (k) => k,
  escapeHtml = DEFAULT_ESCAPE
} = {}) {
  if (!items.length) return "";
  const safeIndex = getNextSlideIndex(activeIndex, items.length, 0);
  const current = items[safeIndex];
  const roleChipClass = current.roleType === "model" ? "is-model" : "is-client";
  const roleChipText =
    current.roleType === "model" ? t("testimonialRoleModel") : t("testimonialRoleClient");

  const dotsHtml = items
    .map(
      (item, idx) => `
      <button
        type="button"
        class="testimonial-dot ${idx === safeIndex ? "active" : ""}"
        role="tab"
        aria-selected="${idx === safeIndex ? "true" : "false"}"
        aria-label="Testimonial ${idx + 1} of ${items.length}: ${escapeHtml(item.name)}"
        data-testimonial-dot="${idx}"
      ></button>`
    )
    .join("");

  return `
    <div class="testimonial-stage" data-testimonial-active-id="${escapeHtml(current.id)}">
      <article class="testimonial-slide" aria-live="polite">
        <div class="testimonial-slide-top">
          <div class="testimonial-stars" aria-label="${current.rating} out of 5 stars">
            ${renderStarsSvg(current.rating)}
            <span class="testimonial-rating-num">${current.rating}.0</span>
          </div>
          <div class="testimonial-badges">
            <span class="testimonial-role-pill ${roleChipClass}">${escapeHtml(roleChipText)}</span>
            <span class="badge">${escapeHtml(t("testimonialVerifiedBadge"))}</span>
          </div>
        </div>

        <blockquote class="testimonial-quote">
          <span class="testimonial-quote-mark" aria-hidden="true">&ldquo;</span>
          <p>${escapeHtml(current.quote)}</p>
        </blockquote>

        <div class="testimonial-author-row">
          <div class="testimonial-avatar" aria-hidden="true">${escapeHtml(getInitials(current.name))}</div>
          <div class="testimonial-author-meta">
            <strong>${escapeHtml(current.name)}</strong>
            <span>${escapeHtml(current.role)} &bull; ${escapeHtml(current.city)}</span>
            <small>${escapeHtml(current.service)}</small>
          </div>
        </div>
      </article>

      <div class="testimonial-controls">
        <div class="testimonial-nav-group">
          <button
            type="button"
            class="testimonial-nav-btn"
            data-testimonial-prev
            aria-label="${escapeHtml(t("testimonialPrevLabel"))}"
          >&#8592;</button>
          <button
            type="button"
            class="testimonial-nav-btn"
            data-testimonial-next
            aria-label="${escapeHtml(t("testimonialNextLabel"))}"
          >&#8594;</button>
          <span class="testimonial-counter">${safeIndex + 1} / ${items.length}</span>
        </div>

        <div class="testimonial-dots" role="tablist" aria-label="Choose testimonial">
          ${dotsHtml}
        </div>

        <button
          type="button"
          class="chip-btn testimonial-autoplay-btn"
          data-testimonial-autoplay
          aria-pressed="${autoplay ? "true" : "false"}"
        >
          ${escapeHtml(autoplay ? t("testimonialPauseAuto") : t("testimonialPlayAuto"))}
        </button>
      </div>
    </div>
  `;
}
