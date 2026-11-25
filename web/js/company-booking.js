import {
  formatHeightDisplay,
  parseCityAndState,
  normalizeExperienceLevel,
  extractModelSkillTags,
  filterDiscoveryModels
} from "./model-discovery.js";
import { sanitizePortfolioPhotoList } from "./model-portal.js";

export const ORGANIZATION_TYPES = [
  "Company",
  "Event Organizer",
  "Brand / Agency",
  "Production House",
  "Verified Content Creator",
  "Wedding Planner",
  "Other"
];

export const MODEL_REQUIREMENT_OPTIONS = [
  "Male Models",
  "Female Models",
  "Both",
  "Multiple Models"
];

export const EVENT_TYPE_OPTIONS = [
  "Fashion Show",
  "Wedding",
  "Music Video",
  "Advertisement",
  "Brand Promotion",
  "Photoshoot",
  "Other"
];

export const COMPANY_BOOKING_STATUSES = [
  "Pending",
  "Approved",
  "Contacted",
  "Completed",
  "Rejected",
  "Cancelled"
];

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Curated privacy-safe public roster seeds merged with live Firestore approved
 * publicModels. Never includes phone numbers, emails, or personal addresses.
 */
export const PUBLIC_MODEL_SHOWCASE_SEEDS = [
  {
    id: "roster_seed_ananya",
    modelId: "roster_seed_ananya",
    fullName: "Ananya Verma",
    gender: "Female",
    age: 23,
    city: "Sirsa, Haryana",
    state: "Haryana",
    heightCm: 171,
    experienceLevel: "3-5 Years",
    category: "Editorial & Runway",
    skills: "Modeling, Acting, High-Fashion Runway, Bridal Editorial, Luxury Jewellery Campaigns",
    bio: "Editorial runway model and commercial screen actor experienced in luxury bridal lookbooks and jewellery campaigns across North India.",
    portfolioUrl: "https://gen-lang-client-0548683163.web.app/#gallery",
    photoConsent: true,
    avatarUrl: "/images/model-portrait.jpg",
    status: "approved",
    isOfficialRoster: true
  },
  {
    id: "roster_seed_kabir",
    modelId: "roster_seed_kabir",
    fullName: "Kabir Gill",
    gender: "Male",
    age: 26,
    city: "Chandigarh",
    state: "Chandigarh",
    heightCm: 183,
    experienceLevel: "3-5 Years",
    category: "Commercial & Music Video",
    skills: "Acting, Modeling, Dancing, 4K Music Videos, Menswear Lookbooks, Brand Endorsements",
    bio: "Lead music-video actor, contemporary dancer, and menswear commercial model available for production houses and digital campaigns.",
    portfolioUrl: "https://gen-lang-client-0548683163.web.app/#gallery",
    photoConsent: true,
    avatarUrl: "/images/gallery-commercial.jpg",
    status: "approved",
    isOfficialRoster: true
  },
  {
    id: "roster_seed_mehak",
    modelId: "roster_seed_mehak",
    fullName: "Mehak Sandhu",
    gender: "Female",
    age: 21,
    city: "Hisar, Haryana",
    state: "Haryana",
    heightCm: 168,
    experienceLevel: "1-2 Years",
    category: "Bridal & Heritage",
    skills: "Modeling, Dancing, Royal Bridal Couture, Ethnic Catalogs, Fashion Shows",
    bio: "Bridal couture model and classical/folk stage performer specializing in heritage jewellery shoots and festive fashion weeks.",
    portfolioUrl: "https://gen-lang-client-0548683163.web.app/#gallery",
    photoConsent: true,
    avatarUrl: "/images/gallery-bridal.jpg",
    status: "approved",
    isOfficialRoster: true
  },
  {
    id: "roster_seed_rohan",
    modelId: "roster_seed_rohan",
    fullName: "Rohan Chaudhary",
    gender: "Male",
    age: 28,
    city: "Sirsa, Haryana",
    state: "Haryana",
    heightCm: 180,
    experienceLevel: "5+ Years",
    category: "Fashion & Fitness",
    skills: "Modeling, Acting, Singing, Runway Choreography, Apparel Ads, Studio Portraits",
    bio: "Senior runway model, live acoustic vocalist, and commercial brand face with 5+ years of studio and stage casting experience.",
    portfolioUrl: "https://gen-lang-client-0548683163.web.app/#gallery",
    photoConsent: true,
    avatarUrl: "/images/gallery-fashion.jpg",
    status: "approved",
    isOfficialRoster: true
  },
  {
    id: "roster_seed_simran",
    modelId: "roster_seed_simran",
    fullName: "Simran Kaur",
    gender: "Female",
    age: 20,
    city: "Ellenabad, Haryana",
    state: "Haryana",
    heightCm: 165,
    experienceLevel: "Fresher",
    category: "Advertisement & Digital",
    skills: "Acting, Singing, Modeling, TV Commercials, Skincare Brand Promotions, Print Campaigns",
    bio: "Fresh screen talent, vocalist, and creator-friendly commercial model for skincare launches, reels, and studio lookbooks.",
    portfolioUrl: "",
    photoConsent: true,
    avatarUrl: "/images/gallery-portrait.jpg",
    status: "approved",
    isOfficialRoster: true
  },
  {
    id: "roster_seed_dev",
    modelId: "roster_seed_dev",
    fullName: "Devansh Malhotra",
    gender: "Male",
    age: 24,
    city: "Ludhiana, Punjab",
    state: "Punjab",
    heightCm: 178,
    experienceLevel: "1-2 Years",
    category: "Performance & Music Video",
    skills: "Dancing, Acting, Singing, Punjabi Music Videos, Live Stage Shows",
    bio: "High-energy bhangra & freestyle dancer, actor, and vocalist available for music video shoots and brand productions.",
    portfolioUrl: "https://gen-lang-client-0548683163.web.app/#gallery",
    photoConsent: true,
    avatarUrl: "/images/studio-hero.jpg",
    status: "approved",
    isOfficialRoster: true
  }
];

function defaultEscapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function safeHttpUrl(url) {
  const clean = String(url ?? "").trim();
  if (!clean) return "";
  try {
    const parsed = new URL(clean);
    if (parsed.protocol === "https:" || parsed.protocol === "http:") {
      return parsed.href;
    }
    return "";
  } catch {
    return "";
  }
}

export function safeContactHref(type, value) {
  const clean = String(value ?? "").trim();
  if (!clean) return "";
  if (type === "tel") {
    const digits = clean.replace(/[^\d+]/g, "");
    return digits.length >= 8 ? `tel:${digits}` : "";
  }
  if (type === "mailto") {
    return EMAIL_RE.test(clean) ? `mailto:${clean}` : "";
  }
  return "";
}

/**
 * Privacy-first sanitizer: strips ALL private contact info (phoneNumber, mobileNumber,
 * email, address, userId, adminNotes), rejects non-approved models, and enforces
 * photo consent so only approved public portfolio details are exposed.
 */
export function sanitizePublicModelProfile(rawModel) {
  if (!rawModel || typeof rawModel !== "object") return null;
  if (rawModel.status && String(rawModel.status).trim().toLowerCase() !== "approved") {
    return null;
  }
  if (rawModel.isOfficialRoster === false || rawModel.isDisabled === true) {
    return null;
  }

  const id = String(rawModel.modelId || rawModel.id || "").trim();
  const fullName = String(rawModel.fullName || "").trim();
  if (!id || !fullName) return null;

  const photoConsent = rawModel.photoConsent !== false && rawModel.consentGiven !== false;
  const portfolioPhotos = photoConsent ? sanitizePortfolioPhotoList(rawModel.portfolioPhotos || []) : [];
  const rawAvatar = String(rawModel.avatarUrl || "").trim();
  const avatarUrl = !photoConsent
    ? ""
    : portfolioPhotos[0]
      ? portfolioPhotos[0]
      : rawAvatar.startsWith("/") || rawAvatar.startsWith("data:image/") || safeHttpUrl(rawAvatar)
        ? rawAvatar
        : "/images/model-portrait.jpg";

  const rawCity = String(rawModel.city || "Sirsa, Haryana").trim().slice(0, 100);
  const { cityName, stateName } = parseCityAndState(rawCity, rawModel.state);
  const parsedAge = Number.parseInt(String(rawModel.age ?? ""), 10);
  const age = Number.isInteger(parsedAge) && parsedAge >= 18 && parsedAge <= 100 ? parsedAge : null;
  const parsedHeight = Number.parseInt(String(rawModel.heightCm ?? ""), 10);
  const heightCm = Number.isInteger(parsedHeight) && parsedHeight >= 120 && parsedHeight <= 230 ? parsedHeight : null;
  const experienceLevel = normalizeExperienceLevel(rawModel.experienceLevel || rawModel.experience);
  const portfolioUrl = safeHttpUrl(rawModel.portfolioUrl || rawModel.portfolioUrls || "");
  const introVideoUrl = safeHttpUrl(rawModel.introVideoUrl || "");
  const skills = String(rawModel.skills || "").trim().slice(0, 1000);
  const languages = String(rawModel.languages || "").trim().slice(0, 300);
  const category = String(rawModel.category || "Official Studio Roster").trim().slice(0, 60);
  const bio = String(rawModel.bio || "").trim().slice(0, 500);

  return {
    id,
    modelId: id,
    fullName: fullName.slice(0, 100),
    gender: String(rawModel.gender || "Model").trim().slice(0, 50),
    age,
    city: rawCity,
    cityName: cityName.slice(0, 80),
    state: stateName.slice(0, 60),
    heightCm,
    experienceLevel,
    category,
    skills,
    languages,
    skillTags: extractModelSkillTags(skills, category),
    bio,
    portfolioUrl,
    portfolioPhotos,
    introVideoUrl,
    hasPortfolio: Boolean(portfolioUrl || portfolioPhotos.length > 0 || rawModel.portfolioAvailable === true),
    photoConsent,
    avatarUrl,
    isOfficialRoster: true
  };
}

/**
 * Merges live Firestore approved publicModels with curated roster seeds, guaranteeing
 * every card passes through sanitizePublicModelProfile (zero private contact leaks).
 */
export function mergePublicModels(cloudModels = [], seeds = PUBLIC_MODEL_SHOWCASE_SEEDS) {
  const seen = new Set();
  const merged = [];

  for (const item of [...(Array.isArray(cloudModels) ? cloudModels : []), ...(Array.isArray(seeds) ? seeds : [])]) {
    const clean = sanitizePublicModelProfile(item);
    if (!clean || seen.has(clean.id)) continue;
    seen.add(clean.id);
    merged.push(clean);
  }
  return merged;
}

export function filterPublicModels(models = [], filters = {}) {
  return filterDiscoveryModels(models, filters);
}

/**
 * Validates Company / Event Organizer registration fields.
 */
export function validateCompanyRegistration(payload = {}, lang = "en") {
  const hi = lang === "hi";
  const errors = [];
  const companyName = String(payload.companyName || "").trim();
  const organizationType = ORGANIZATION_TYPES.includes(payload.organizationType)
    ? payload.organizationType
    : "Company";
  const contactPerson = String(payload.contactPerson || "").trim();
  const mobileNumber = String(payload.mobileNumber || "").trim();
  const digits = mobileNumber.replace(/\D/g, "");
  const email = String(payload.email || "").trim().toLowerCase();
  const address = String(payload.address || "").trim();

  if (companyName.length < 2 || companyName.length > 120) {
    errors.push(hi ? "कृपया सही कंपनी या ऑर्गनाइज़र नाम दर्ज करें (2–120 अक्षर)।" : "Please enter a valid Company / Event Organizer name (2–120 characters).");
  }
  if (contactPerson.length < 2 || contactPerson.length > 100) {
    errors.push(hi ? "कृपया संपर्क व्यक्ति का पूरा नाम दर्ज करें।" : "Please enter the Contact Person's full name.");
  }
  if (digits.length < 10 || digits.length > 15 || mobileNumber.length > 20) {
    errors.push(hi ? "कृपया 10 अंकों का सही मोबाइल नंबर दर्ज करें।" : "Please enter a valid 10–15 digit mobile number.");
  }
  if (!EMAIL_RE.test(email) || email.length > 128) {
    errors.push(hi ? "कृपया सही कंपनी ईमेल पता दर्ज करें।" : "Please enter a valid company email address.");
  }
  if (address.length < 5 || address.length > 300) {
    errors.push(hi ? "कृपया कंपनी का पूरा पता दर्ज करें (कम से कम 5 अक्षर)।" : "Please enter your company or event office address (at least 5 characters).");
  }

  return {
    valid: errors.length === 0,
    errors,
    cleaned: {
      companyName,
      organizationType,
      contactPerson,
      mobileNumber,
      email,
      address
    }
  };
}

/**
 * Validates a Company Model Requirement Booking request.
 */
export function validateCompanyModelBooking(payload = {}, lang = "en") {
  const hi = lang === "hi";
  const reg = validateCompanyRegistration(payload, lang);
  const errors = [...reg.errors];

  const modelRequirement = String(payload.modelRequirement || "").trim();
  const modelsCount = Number.parseInt(String(payload.modelsCount ?? ""), 10);
  const eventType = String(payload.eventType || "").trim();
  const eventDate = String(payload.eventDate || "").trim();
  const eventLocation = String(payload.eventLocation || "").trim();
  const budget = String(payload.budget || "").trim();
  const detailedRequirements = String(payload.detailedRequirements || "").trim();

  const rawIds = Array.isArray(payload.requestedModelIds) ? payload.requestedModelIds : [];
  const rawNames = Array.isArray(payload.requestedModelNames) ? payload.requestedModelNames : [];
  const requestedModelIds = [...new Set(rawIds.map((v) => String(v || "").trim()).filter(Boolean))].slice(0, 20);
  const requestedModelNames = rawNames.map((v) => String(v || "").trim()).filter(Boolean).slice(0, 20);

  if (!MODEL_REQUIREMENT_OPTIONS.includes(modelRequirement)) {
    errors.push(hi ? "कृपया सही मॉडल आवश्यकता चुनें।" : "Please select a valid model requirement category.");
  }
  if (!Number.isInteger(modelsCount) || modelsCount < 1 || modelsCount > 200) {
    errors.push(hi ? "कृपया आवश्यक मॉडल्स की संख्या (1 से 200) दर्ज करें।" : "Please enter the number of models required (1 to 200).");
  }
  if (!EVENT_TYPE_OPTIONS.includes(eventType)) {
    errors.push(hi ? "कृपया सही इवेंट प्रकार चुनें।" : "Please select a valid event type.");
  }
  if (eventDate.length < 6 || eventDate.length > 50) {
    errors.push(hi ? "कृपया इवेंट की तारीख चुनें।" : "Please choose an event date.");
  }
  if (eventLocation.length < 2 || eventLocation.length > 200) {
    errors.push(hi ? "कृपया इवेंट का स्थान दर्ज करें।" : "Please enter the event location.");
  }
  if (budget.length < 2 || budget.length > 80) {
    errors.push(hi ? "कृपया अनुमानित बजट दर्ज करें।" : "Please enter your estimated budget.");
  }
  if (detailedRequirements.length < 10 || detailedRequirements.length > 2000) {
    errors.push(
      hi
        ? "कृपया कम से कम 10 अक्षरों में विस्तृत आवश्यकताएं लिखें।"
        : "Please describe your detailed requirements (at least 10 characters)."
    );
  }

  return {
    valid: errors.length === 0,
    errors,
    cleaned: {
      ...reg.cleaned,
      modelRequirement,
      modelsCount: Number.isInteger(modelsCount) ? modelsCount : 1,
      eventType,
      eventDate,
      eventLocation,
      budget,
      detailedRequirements,
      requestedModelIds,
      requestedModelNames
    }
  };
}

export function normalizeCompanyBookingStatus(status) {
  const raw = String(status || "").trim().toLowerCase();
  if (raw === "approved") return "Approved";
  if (raw === "contacted") return "Contacted";
  if (raw === "completed") return "Completed";
  if (raw === "rejected") return "Rejected";
  if (raw === "cancelled") return "Cancelled";
  return "Pending";
}

export function summarizeCompanyRequests(requests = []) {
  return (Array.isArray(requests) ? requests : []).reduce(
    (acc, r) => {
      acc.total += 1;
      const st = normalizeCompanyBookingStatus(r?.status);
      if (st === "Approved") acc.approved += 1;
      else if (st === "Contacted") acc.contacted += 1;
      else if (st === "Completed") acc.completed += 1;
      else if (st === "Rejected") acc.rejected += 1;
      else if (st === "Cancelled") acc.cancelled += 1;
      else acc.pending += 1;
      return acc;
    },
    { total: 0, pending: 0, approved: 0, contacted: 0, completed: 0, rejected: 0, cancelled: 0 }
  );
}

export function filterAdminCompanyRequests(requests = [], filter = "all", searchQuery = "") {
  const f = String(filter || "all").trim().toLowerCase();
  const q = String(searchQuery || "").trim().toLowerCase();

  return (Array.isArray(requests) ? requests : []).filter((r) => {
    const st = normalizeCompanyBookingStatus(r?.status).toLowerCase();
    if (f !== "all" && st !== f) return false;
    if (!q) return true;
    const hay = [
      r?.companyName,
      r?.contactPerson,
      r?.email,
      r?.mobileNumber,
      r?.eventType,
      r?.eventLocation,
      r?.modelRequirement,
      ...(Array.isArray(r?.requestedModelNames) ? r.requestedModelNames : [])
    ]
      .join(" ")
      .toLowerCase();
    return hay.includes(q);
  });
}

/**
 * Renders privacy-safe Admin-Approved Model Discovery cards with professional specs,
 * photo-consent enforcement, View Profile, Casting Inquiry, and Shoot Booking actions.
 */
export function renderPublicModelPortfolioCardsHtml({
  models = [],
  selectedModelIds = [],
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const selectedSet = new Set(Array.isArray(selectedModelIds) ? selectedModelIds : []);
  if (!models.length) {
    return `<div class="studio-card casting-empty-state"><p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("companyNoPublicModels"))}</p></div>`;
  }

  return models
    .map((m) => {
      const clean = sanitizePublicModelProfile(m);
      if (!clean) return "";
      const isSelected = selectedSet.has(clean.id);
      const heightText = formatHeightDisplay(clean.heightCm);
      const locationText = [clean.cityName || clean.city, clean.state]
        .filter((v, idx, arr) => v && (idx === 0 || !String(arr[0]).toLowerCase().includes(String(v).toLowerCase())))
        .join(", ");
      const monogram = clean.fullName
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((w) => w[0].toUpperCase())
        .join("") || "SA";

      const mediaHtml = clean.photoConsent && clean.avatarUrl
        ? `<img src="${escapeHtml(clean.avatarUrl)}" alt="${escapeHtml(clean.fullName)} — Official Studio Roster" loading="lazy" />`
        : `<div class="model-photo-consent-placeholder" role="img" aria-label="${escapeHtml(t("castingPhotoPrivateLabel"))}">
            <span class="model-consent-monogram">${escapeHtml(monogram)}</span>
            <span class="model-consent-caption">🔒 ${escapeHtml(t("castingPhotoPrivateLabel"))}</span>
          </div>`;

      const specs = [
        clean.age ? `${clean.age} ${t("castingYearsUnit")}` : "",
        heightText,
        clean.experienceLevel
      ].filter(Boolean);

      return `
      <article class="studio-card public-model-card ${isSelected ? "is-selected" : ""}" data-public-model-id="${escapeHtml(clean.id)}">
        <div class="public-model-media">
          ${mediaHtml}
          <span class="public-model-roster-tag">★ ${escapeHtml(t("rosterBadge"))}</span>
          ${clean.hasPortfolio ? `<span class="public-model-portfolio-badge">${escapeHtml(t("castingPortfolioPill"))}</span>` : ""}
        </div>
        <div class="public-model-body">
          <div class="public-model-head">
            <div>
              <h4>${escapeHtml(clean.fullName)}</h4>
              <span class="public-model-meta">${escapeHtml(clean.gender)} • ${escapeHtml(locationText || clean.city)}</span>
            </div>
            <span class="badge">${escapeHtml(clean.category)}</span>
          </div>
          ${
            specs.length
              ? `<div class="public-model-specs-row">${specs.map((s) => `<span class="public-model-spec-pill">${escapeHtml(s)}</span>`).join("")}</div>`
              : ""
          }
          ${
            clean.skillTags.length
              ? `<div class="model-skill-pills">${clean.skillTags.slice(0, 4).map((tag) => `<span class="model-skill-pill">${escapeHtml(tag)}</span>`).join("")}</div>`
              : clean.skills
                ? `<p class="public-model-skills">${escapeHtml(clean.skills)}</p>`
                : ""
          }
          <div class="public-model-privacy-note">🔒 ${escapeHtml(t("companyModelPrivacyNote"))}</div>
          <div class="public-model-card-actions">
            <button
              type="button"
              class="btn-outline btn-sm public-model-view-btn"
              data-view-model-profile="${escapeHtml(clean.id)}"
            >${escapeHtml(t("castingViewProfileBtn"))}</button>
            <button
              type="button"
              class="btn-outline btn-sm public-model-share-btn"
              data-share-model-profile="${escapeHtml(clean.id)}"
              data-share-model-name="${escapeHtml(clean.fullName)}"
              aria-label="${escapeHtml(t("castingShareBtn"))} ${escapeHtml(clean.fullName)}"
            >
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <circle cx="18" cy="5" r="3"></circle>
                <circle cx="6" cy="12" r="3"></circle>
                <circle cx="18" cy="19" r="3"></circle>
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line>
                <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
              </svg>
              <span class="share-btn-label">${escapeHtml(t("castingShareBtn"))}</span>
            </button>
            <button
              type="button"
              class="btn-outline btn-sm public-model-audition-btn"
              data-audition-request-model-id="${escapeHtml(clean.id)}"
              data-audition-request-model-name="${escapeHtml(clean.fullName)}"
            >${escapeHtml(t("castingRequestAuditionBtn"))}</button>
            <button
              type="button"
              class="btn-outline btn-sm public-model-shoot-btn"
              data-shoot-booking-model-id="${escapeHtml(clean.id)}"
              data-shoot-booking-model-name="${escapeHtml(clean.fullName)}"
            >${escapeHtml(t("castingShootBookingBtn"))}</button>
            <button
              type="button"
              class="${isSelected ? "btn-gold" : "btn-outline"} btn-sm public-model-request-btn"
              data-toggle-req-model-id="${escapeHtml(clean.id)}"
              data-toggle-req-model-name="${escapeHtml(clean.fullName)}"
              data-casting-inquiry-model-id="${escapeHtml(clean.id)}"
              data-casting-inquiry-model-name="${escapeHtml(clean.fullName)}"
              aria-pressed="${isSelected ? "true" : "false"}"
            >${escapeHtml(isSelected ? t("companyModelSelectedBtn") : t("castingInquiryBtn"))}</button>
          </div>
        </div>
      </article>`;
    })
    .join("");
}

export function renderSelectedRequestedModelsHtml({
  selectedModels = [],
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  if (!selectedModels.length) {
    return `<span class="selected-models-empty">${escapeHtml(t("companySelectedModelsEmpty"))}</span>`;
  }
  return selectedModels
    .map(
      (m) => `
    <span class="selected-model-chip">
      <span>★ ${escapeHtml(m.fullName)}</span>
      <button type="button" class="selected-model-remove" data-remove-req-model-id="${escapeHtml(m.id)}" aria-label="Remove ${escapeHtml(m.fullName)}">✕</button>
    </span>`
    )
    .join("");
}

export function renderMyCompanyBookingsHtml({
  requests = [],
  signedIn = false,
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  if (!signedIn) {
    return `<p style="color:var(--text-secondary);margin:0;">${escapeHtml(t("authRequiredNotice"))}</p>`;
  }
  if (!requests.length) {
    return `<p style="color:var(--text-secondary);margin:0;">${escapeHtml(t("companyNoRequestsYet"))}</p>`;
  }

  return requests
    .map((r) => {
      const st = normalizeCompanyBookingStatus(r.status);
      const statusCls =
        st === "Approved" || st === "Completed"
          ? "status-approved"
          : st === "Contacted"
          ? "status-confirmed"
          : st === "Rejected" || st === "Cancelled"
          ? "status-rejected"
          : "status-pending";
      const requestedNames = Array.isArray(r.requestedModelNames) ? r.requestedModelNames : [];
      return `
      <article class="studio-card company-req-card" data-company-req-id="${escapeHtml(r.id)}">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:10px;flex-wrap:wrap;">
          <div>
            <strong style="color:var(--gold-light);font-size:1rem;">${escapeHtml(r.eventType)} — ${escapeHtml(r.modelsCount)} × ${escapeHtml(r.modelRequirement)}</strong>
            <div style="font-size:0.84rem;color:var(--text-secondary);margin-top:2px;">
              ${escapeHtml(r.companyName)} • ${escapeHtml(r.eventDate)} • ${escapeHtml(r.eventLocation)} • Budget: ${escapeHtml(r.budget)}
            </div>
          </div>
          <span class="badge ${statusCls}">${escapeHtml(st)}</span>
        </div>
        ${
          requestedNames.length
            ? `<div style="font-size:0.82rem;color:var(--gold-light);"><strong>${escapeHtml(t("companyRequestedModelsLabel"))}:</strong> ${escapeHtml(requestedNames.join(", "))}</div>`
            : ""
        }
        <p style="margin:0;font-size:0.86rem;color:var(--text-primary);">${escapeHtml(r.detailedRequirements)}</p>
        ${
          r.adminNotes
            ? `<div class="booking-card-notes"><strong>Studio Admin Note:</strong> ${escapeHtml(r.adminNotes)}</div>`
            : ""
        }
        ${
          st === "Pending"
            ? `<div><button type="button" class="btn-danger" data-cancel-company-req="${escapeHtml(r.id)}">${escapeHtml(t("cancelBookingBtn"))}</button></div>`
            : ""
        }
      </article>`;
    })
    .join("");
}

/**
 * Renders the Admin Dashboard "Company Requests" management tab (Approve, Contact, Reject, Complete, Delete).
 */
export function renderAdminCompanyRequestsSectionHtml({
  requests = [],
  companies = [],
  filter = "all",
  searchQuery = "",
  drafts = {},
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const counts = summarizeCompanyRequests(requests);
  const visible = filterAdminCompanyRequests(requests, filter, searchQuery);

  const pills = [
    { key: "all", label: `${t("statusAll")} (${counts.total})` },
    { key: "pending", label: `${t("statusPending")} (${counts.pending})` },
    { key: "approved", label: `${t("adminCompanyFilterApproved")} (${counts.approved})` },
    { key: "contacted", label: `${t("adminCompanyFilterContacted")} (${counts.contacted})` },
    { key: "rejected", label: `${t("adminFilterRejected")} (${counts.rejected})` }
  ]
    .map(
      (p) => `
      <button type="button" class="chip-btn ${filter === p.key ? "active" : ""}" data-adm-comp-filter="${p.key}">
        ${escapeHtml(p.label)}
      </button>`
    )
    .join("");

  const cardsHtml = visible.length
    ? visible
        .map((r) => {
          const st = normalizeCompanyBookingStatus(r.status);
          const statusCls =
            st === "Approved" || st === "Completed"
              ? "status-approved"
              : st === "Contacted"
              ? "status-confirmed"
              : st === "Rejected" || st === "Cancelled"
              ? "status-rejected"
              : "status-pending";
          const noteVal = drafts[r.id] !== undefined ? drafts[r.id] : r.adminNotes || "";
          const telHref = safeContactHref("tel", r.mobileNumber);
          const mailHref = safeContactHref("mailto", r.email);
          const requestedNames = Array.isArray(r.requestedModelNames) ? r.requestedModelNames : [];

          return `
          <article class="studio-card admin-company-req-card" data-adm-comp-card="${escapeHtml(r.id)}">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:10px;flex-wrap:wrap;">
              <div>
                <strong style="font-size:1.05rem;color:var(--gold-light);">${escapeHtml(r.companyName)} (${escapeHtml(r.organizationType || "Company")})</strong>
                <div style="font-size:0.84rem;color:var(--text-secondary);margin-top:2px;">
                  Contact: <strong>${escapeHtml(r.contactPerson)}</strong> • ${escapeHtml(r.mobileNumber)} • ${escapeHtml(r.email)}
                </div>
                <div style="font-size:0.8rem;color:var(--text-muted);">${escapeHtml(r.address)}</div>
              </div>
              <span class="badge ${statusCls}">${escapeHtml(st)}</span>
            </div>

            <div class="admin-company-meta-grid">
              <div><span>Requirement</span><strong>${escapeHtml(r.modelsCount)} × ${escapeHtml(r.modelRequirement)}</strong></div>
              <div><span>Event Type</span><strong>${escapeHtml(r.eventType)}</strong></div>
              <div><span>Event Date</span><strong>${escapeHtml(r.eventDate)}</strong></div>
              <div><span>Location</span><strong>${escapeHtml(r.eventLocation)}</strong></div>
              <div><span>Budget</span><strong>${escapeHtml(r.budget)}</strong></div>
            </div>

            ${
              requestedNames.length
                ? `<div class="admin-company-requested-models">
                    <span>Requested Models:</span>
                    ${requestedNames.map((n) => `<span class="badge status-approved">★ ${escapeHtml(n)}</span>`).join("")}
                  </div>`
                : ""
            }

            <div class="booking-card-notes">
              <strong>Detailed Requirements:</strong> ${escapeHtml(r.detailedRequirements)}
            </div>

            <div class="field">
              <label for="admCompNote_${escapeHtml(r.id)}">${escapeHtml(t("adminNotesLabel"))}</label>
              <textarea
                id="admCompNote_${escapeHtml(r.id)}"
                class="input"
                rows="2"
                maxlength="1000"
                data-adm-comp-notes="${escapeHtml(r.id)}"
                placeholder="${escapeHtml(t("adminCompanyNotesPlaceholder"))}"
              >${escapeHtml(noteVal)}</textarea>
            </div>

            <div style="display:flex;flex-wrap:wrap;gap:8px;align-items:center;">
              <button type="button" class="btn-gold btn-sm" data-adm-comp-id="${escapeHtml(r.id)}" data-adm-comp-st="Approved">${escapeHtml(t("adminCompanyApproveBtn"))}</button>
              <button type="button" class="chip-btn" data-adm-comp-id="${escapeHtml(r.id)}" data-adm-comp-st="Contacted">${escapeHtml(t("adminCompanyContactedBtn"))}</button>
              <button type="button" class="chip-btn" data-adm-comp-id="${escapeHtml(r.id)}" data-adm-comp-st="Completed">Complete</button>
              <button type="button" class="btn-danger btn-sm" data-adm-comp-id="${escapeHtml(r.id)}" data-adm-comp-st="Rejected">${escapeHtml(t("adminCompanyRejectBtn"))}</button>
              ${telHref ? `<a class="btn-outline btn-sm" href="${escapeHtml(telHref)}">Call Organizer</a>` : ""}
              ${mailHref ? `<a class="btn-outline btn-sm" href="${escapeHtml(mailHref)}">Email Organizer</a>` : ""}
              <button type="button" class="btn-danger btn-sm" data-adm-comp-del="${escapeHtml(r.id)}">Delete</button>
            </div>
          </article>`;
        })
        .join("")
    : `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("adminNoCompanyRequests"))}</p></div>`;

  const directoryHtml = Array.isArray(companies) && companies.length
    ? `
      <div class="studio-card" style="margin-top:8px;">
        <strong style="color:var(--gold);">${escapeHtml(t("adminRegisteredCompaniesTitle"))} (${companies.length})</strong>
        <div style="display:flex;flex-direction:column;gap:8px;margin-top:8px;">
          ${companies
            .map(
              (c) => `
            <div style="padding:10px;border:1px solid var(--outline);border-radius:10px;display:flex;justify-content:space-between;flex-wrap:wrap;gap:8px;">
              <div>
                <strong>${escapeHtml(c.companyName)}</strong> <span style="font-size:0.8rem;color:var(--gold-light);">(${escapeHtml(c.organizationType || "Company")})</span>
                <div style="font-size:0.82rem;color:var(--text-secondary);">${escapeHtml(c.contactPerson)} • ${escapeHtml(c.mobileNumber)} • ${escapeHtml(c.email)}</div>
                <div style="font-size:0.78rem;color:var(--text-muted);">${escapeHtml(c.address)}</div>
              </div>
            </div>`
            )
            .join("")}
        </div>
      </div>`
    : "";

  return `
    <div class="studio-card admin-verification-banner">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
        <div>
          <strong style="color:var(--gold);display:block;">${escapeHtml(t("adminCompaniesBannerTitle"))}</strong>
          <span style="font-size:0.84rem;color:var(--text-secondary);">${escapeHtml(t("adminCompaniesBannerDesc"))}</span>
        </div>
        <span class="badge status-pending">${counts.pending} Pending</span>
      </div>
    </div>
    <div class="filter-bar" style="margin-bottom:4px;">
      ${pills}
      <input
        type="search"
        class="input"
        style="max-width:300px;"
        data-adm-comp-search
        value="${escapeHtml(searchQuery)}"
        placeholder="${escapeHtml(t("adminCompanySearchPlaceholder"))}"
      />
    </div>
    <div style="display:flex;flex-direction:column;gap:12px;">
      ${cardsHtml}
    </div>
    ${directoryHtml}`;
}
