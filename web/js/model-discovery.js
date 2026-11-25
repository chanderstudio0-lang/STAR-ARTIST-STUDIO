/**
 * STAR ARTIST STUDIO — Casting & Model Discovery Engine
 * Privacy-first filtering, normalization, and profile rendering for Admin-approved models.
 * Strictly strips personal phone numbers, email addresses, home addresses, UIDs, and admin notes.
 */

export const CASTING_SKILL_OPTIONS = Object.freeze([
  "Acting",
  "Dancing",
  "Modeling",
  "Singing"
]);

export const EXPERIENCE_LEVEL_OPTIONS = Object.freeze([
  "Fresher",
  "1-2 Years",
  "3-5 Years",
  "5+ Years"
]);

export const DISCOVERY_STATE_OPTIONS = Object.freeze([
  "Haryana",
  "Punjab",
  "Chandigarh",
  "Delhi",
  "Rajasthan",
  "Maharashtra"
]);

export const DEFAULT_MODEL_DISCOVERY_FILTERS = Object.freeze({
  search: "",
  gender: "all",
  minAge: "",
  maxAge: "",
  city: "",
  state: "all",
  minHeightCm: "",
  maxHeightCm: "",
  skills: [],
  experience: "all",
  portfolioOnly: false
});

function defaultEscapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function safeDiscoveryHttpUrl(url) {
  const clean = String(url ?? "").trim();
  if (!clean) return "";
  try {
    const parsed = new URL(clean);
    return parsed.protocol === "https:" || parsed.protocol === "http:" ? parsed.href : "";
  } catch {
    return "";
  }
}

/**
 * Formats height in centimeters alongside feet/inches (e.g. `172 cm (5'8")`).
 */
export function formatHeightDisplay(heightCm) {
  const cm = Number(heightCm);
  if (!Number.isFinite(cm) || cm < 120 || cm > 230) return "";
  const roundedCm = Math.round(cm);
  const totalInches = Math.round(roundedCm / 2.54);
  const feet = Math.floor(totalInches / 12);
  const inches = totalInches % 12;
  return `${roundedCm} cm (${feet}'${inches}")`;
}

/**
 * Splits a combined "City, State" string cleanly into { cityName, stateName }.
 */
export function parseCityAndState(rawCity = "", rawState = "") {
  const explicitState = String(rawState || "").trim();
  const cleanCity = String(rawCity || "").trim();
  if (explicitState) {
    const cityPart = cleanCity.includes(",") ? cleanCity.split(",")[0].trim() : cleanCity;
    return {
      cityName: cityPart || "Sirsa",
      stateName: explicitState
    };
  }
  if (cleanCity.includes(",")) {
    const parts = cleanCity.split(",").map((p) => p.trim()).filter(Boolean);
    if (parts.length >= 2) {
      const candidateState = parts[parts.length - 1];
      const cityPart = parts.slice(0, -1).join(", ");
      const normalizedState =
        candidateState.toLowerCase() === "sirsa" ? "Haryana" : candidateState;
      return { cityName: cityPart || "Sirsa", stateName: normalizedState };
    }
  }
  const lower = cleanCity.toLowerCase();
  if (lower.includes("chandigarh")) return { cityName: cleanCity, stateName: "Chandigarh" };
  if (lower.includes("delhi") || lower.includes("gurugram") || lower.includes("noida")) {
    return { cityName: cleanCity, stateName: lower.includes("gurugram") ? "Haryana" : "Delhi" };
  }
  if (lower.includes("mumbai") || lower.includes("pune")) {
    return { cityName: cleanCity, stateName: "Maharashtra" };
  }
  if (lower.includes("jaipur") || lower.includes("udaipur")) {
    return { cityName: cleanCity, stateName: "Rajasthan" };
  }
  if (lower.includes("ludhiana") || lower.includes("amritsar") || lower.includes("bathinda") || lower.includes("mohali")) {
    return { cityName: cleanCity, stateName: "Punjab" };
  }
  return {
    cityName: cleanCity || "Sirsa",
    stateName: "Haryana"
  };
}

export function normalizeExperienceLevel(rawExperience) {
  const clean = String(rawExperience ?? "").trim();
  if (!clean) return "1-2 Years";
  if (EXPERIENCE_LEVEL_OPTIONS.includes(clean)) return clean;
  const lower = clean.toLowerCase();
  if (lower.includes("fresh") || lower === "0" || lower.includes("beginner")) return "Fresher";
  const num = Number.parseFloat(clean);
  if (Number.isFinite(num)) {
    if (num < 1) return "Fresher";
    if (num <= 2) return "1-2 Years";
    if (num <= 5) return "3-5 Years";
    return "5+ Years";
  }
  if (lower.includes("5+") || lower.includes("senior") || lower.includes("veteran")) return "5+ Years";
  if (lower.includes("3") || lower.includes("4") || lower.includes("mid")) return "3-5 Years";
  return "1-2 Years";
}

/**
 * Parses comma/pipe separated skills text into normalized display tags.
 */
export function extractModelSkillTags(skills = "", category = "") {
  const rawList = Array.isArray(skills)
    ? skills
    : String(skills || "")
        .split(/[,|•/]+/)
        .map((s) => s.trim())
        .filter(Boolean);
  const seen = new Set();
  const tags = [];
  for (const tag of rawList) {
    const key = tag.toLowerCase();
    if (!key || seen.has(key)) continue;
    seen.add(key);
    tags.push(tag.slice(0, 48));
  }
  if (!tags.length && category) {
    tags.push(String(category).trim().slice(0, 48));
  }
  return tags.slice(0, 10);
}

/**
 * Checks whether a model matches a canonical skill filter (Acting, Dancing, Modeling, Singing).
 */
export function modelMatchesSkillFilter(model, skill) {
  const target = String(skill || "").trim().toLowerCase();
  if (!target) return true;
  const hay = [
    model?.skills,
    model?.category,
    model?.bio,
    ...(Array.isArray(model?.skillTags) ? model.skillTags : [])
  ]
    .join(" ")
    .toLowerCase();

  if (hay.includes(target)) return true;
  if (target === "modeling") {
    return /\b(model|runway|editorial|bridal|couture|fashion|lookbook|print|catalog|ramp)\b/.test(hay);
  }
  if (target === "acting") {
    return /\b(act|actor|actress|film|cinema|commercial|music video|theatre|drama)\b/.test(hay);
  }
  if (target === "dancing") {
    return /\b(danc|choreograph|kathak|bhangra|hip[- ]?hop|classical|freestyle)\b/.test(hay);
  }
  if (target === "singing") {
    return /\b(sing|vocal|singer|music|playback|acoustic|live performer)\b/.test(hay);
  }
  return false;
}

/**
 * Collects unique State values from live approved models + baseline discovery states.
 */
export function getAvailableStatesFromModels(models = []) {
  const states = new Set(DISCOVERY_STATE_OPTIONS);
  for (const m of Array.isArray(models) ? models : []) {
    const st = String(m?.state || "").trim();
    if (st) states.add(st);
  }
  return [...states].sort((a, b) => a.localeCompare(b));
}

/**
 * Collects unique City values (optionally scoped by selected state) from live approved models.
 */
export function getAvailableCitiesFromModels(models = [], selectedState = "all") {
  const stateFilter = String(selectedState || "all").trim().toLowerCase();
  const cities = new Set();
  for (const m of Array.isArray(models) ? models : []) {
    const st = String(m?.state || "").trim().toLowerCase();
    if (stateFilter !== "all" && st !== stateFilter) continue;
    const cityName = String(m?.cityName || "").trim();
    if (cityName) cities.add(cityName);
  }
  return [...cities].sort((a, b) => a.localeCompare(b));
}

/**
 * Counts how many non-default discovery filters are currently active.
 */
export function countActiveDiscoveryFilters(filters = {}) {
  let count = 0;
  if (String(filters.search || "").trim()) count += 1;
  if (filters.gender && filters.gender !== "all") count += 1;
  if (String(filters.minAge || "").trim() || String(filters.maxAge || "").trim()) count += 1;
  if (String(filters.city || "").trim()) count += 1;
  if (filters.state && filters.state !== "all") count += 1;
  if (String(filters.minHeightCm || "").trim() || String(filters.maxHeightCm || "").trim()) count += 1;
  if (Array.isArray(filters.skills) && filters.skills.length > 0) count += filters.skills.length;
  if (filters.experience && filters.experience !== "all") count += 1;
  if (filters.portfolioOnly) count += 1;
  return count;
}

/**
 * Filters Admin-approved, sanitized public models across all 7 casting criteria:
 * 1) Gender, 2) Age range, 3) City & State, 4) Height range,
 * 5) Skills (Acting, Dancing, Modeling, Singing), 6) Experience, 7) Portfolio availability.
 */
export function filterDiscoveryModels(models = [], filters = {}) {
  const gFilter = String(filters.gender || "all").trim().toLowerCase();
  const q = String(filters.search || "").trim().toLowerCase();
  const cityQuery = String(filters.city || "").trim().toLowerCase();
  const stateFilter = String(filters.state || "all").trim().toLowerCase();
  const minAge = Number.parseInt(String(filters.minAge ?? ""), 10);
  const maxAge = Number.parseInt(String(filters.maxAge ?? ""), 10);
  const minHeight = Number.parseInt(String(filters.minHeightCm ?? ""), 10);
  const maxHeight = Number.parseInt(String(filters.maxHeightCm ?? ""), 10);
  const requiredSkills = Array.isArray(filters.skills)
    ? filters.skills.map((s) => String(s || "").trim()).filter(Boolean)
    : [];
  const expFilter = String(filters.experience || "all").trim().toLowerCase();
  const portfolioOnly = Boolean(filters.portfolioOnly);

  return (Array.isArray(models) ? models : []).filter((m) => {
    if (!m || m.isOfficialRoster === false) return false;
    if (m.status && String(m.status).toLowerCase() !== "approved") return false;

    const modelGender = String(m.gender || "").trim().toLowerCase();
    if (gFilter === "male" && modelGender !== "male") return false;
    if (gFilter === "female" && modelGender !== "female") return false;

    if (Number.isFinite(minAge) && minAge >= 18) {
      if (!Number.isFinite(Number(m.age)) || Number(m.age) < minAge) return false;
    }
    if (Number.isFinite(maxAge) && maxAge >= 18) {
      if (!Number.isFinite(Number(m.age)) || Number(m.age) > maxAge) return false;
    }

    if (stateFilter && stateFilter !== "all") {
      const modelState = String(m.state || "").trim().toLowerCase();
      const modelCityFull = String(m.city || "").trim().toLowerCase();
      if (modelState !== stateFilter && !modelCityFull.includes(stateFilter)) return false;
    }

    if (cityQuery) {
      const cityHay = `${m.cityName || ""} ${m.city || ""}`.toLowerCase();
      if (!cityHay.includes(cityQuery)) return false;
    }

    if (Number.isFinite(minHeight) && minHeight >= 120) {
      if (!Number.isFinite(Number(m.heightCm)) || Number(m.heightCm) < minHeight) return false;
    }
    if (Number.isFinite(maxHeight) && maxHeight >= 120) {
      if (!Number.isFinite(Number(m.heightCm)) || Number(m.heightCm) > maxHeight) return false;
    }

    if (requiredSkills.length > 0) {
      const matchesAll = requiredSkills.every((skill) => modelMatchesSkillFilter(m, skill));
      if (!matchesAll) return false;
    }

    if (expFilter && expFilter !== "all") {
      const modelExp = normalizeExperienceLevel(m.experienceLevel).toLowerCase();
      if (modelExp !== expFilter) return false;
    }

    if (portfolioOnly) {
      const hasPortfolio = Boolean(m.portfolioUrl || m.hasPortfolio);
      if (!hasPortfolio) return false;
    }

    if (q) {
      const hay = [
        m.fullName,
        m.city,
        m.cityName,
        m.state,
        m.skills,
        m.languages,
        m.category,
        m.gender,
        m.experienceLevel,
        m.bio
      ]
        .join(" ")
        .toLowerCase();
      if (!hay.includes(q)) return false;
    }

    return true;
  });
}

/**
 * Renders the privacy-safe full Model Profile modal body (strictly excludes phone/email/address,
 * respects photoConsent, and offers Casting Inquiry + Shoot Booking actions).
 */
export function renderModelProfileModalHtml(
  model,
  {
    isSelected = false,
    t = (k) => k,
    escapeHtml = defaultEscapeHtml
  } = {}
) {
  if (!model) return "";
  const heightText = formatHeightDisplay(model.heightCm);
  const ageText = Number.isFinite(Number(model.age)) ? `${Number(model.age)} ${t("castingYearsUnit")}` : "";
  const locationText = [model.cityName || model.city, model.state]
    .filter((v, idx, arr) => v && (idx === 0 || !String(arr[0]).toLowerCase().includes(String(v).toLowerCase())))
    .join(", ");
  const skillTags = extractModelSkillTags(model.skills, model.category);
  const hasPhotoConsent = model.photoConsent !== false;
  const monogram = String(model.fullName || "SA")
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join("") || "SA";

  const galleryPhotos = hasPhotoConsent
    ? (Array.isArray(model.portfolioPhotos) && model.portfolioPhotos.length > 0
        ? model.portfolioPhotos
        : model.avatarUrl
        ? [model.avatarUrl]
        : [])
    : [];
  const mainPhotoSrc = galleryPhotos[0] || model.avatarUrl || "";
  const introVideoHref = safeDiscoveryHttpUrl(model.introVideoUrl || "");

  const mediaHtml =
    hasPhotoConsent && mainPhotoSrc
      ? `<img id="modelProfileMainImg" src="${escapeHtml(mainPhotoSrc)}" alt="${escapeHtml(model.fullName)} — ${escapeHtml(t("rosterBadge"))}" class="model-profile-portrait" />`
      : `<div class="model-photo-consent-placeholder is-modal" role="img" aria-label="${escapeHtml(t("castingPhotoPrivateLabel"))}">
          <span class="model-consent-monogram">${escapeHtml(monogram)}</span>
          <span class="model-consent-caption">🔒 ${escapeHtml(t("castingPhotoPrivateLabel"))}</span>
        </div>`;

  const thumbStripHtml =
    hasPhotoConsent && galleryPhotos.length > 1
      ? `<div class="model-profile-thumbs" role="group" aria-label="Portfolio photos">
          ${galleryPhotos
            .map(
              (src, idx) => `
            <button type="button" class="model-profile-thumb-btn ${idx === 0 ? "active" : ""}" data-profile-photo-src="${escapeHtml(src)}" aria-label="View photo ${idx + 1}">
              <img src="${escapeHtml(src)}" alt="${escapeHtml(model.fullName)} portfolio ${idx + 1}" loading="lazy" />
            </button>`
            )
            .join("")}
        </div>`
      : "";

  return `
    <div class="model-profile-layout">
      <div class="model-profile-media-col">
        <div class="model-profile-media">
          ${mediaHtml}
          <span class="public-model-roster-tag">★ ${escapeHtml(t("castingAdminApprovedBadge"))}</span>
        </div>
        ${thumbStripHtml}
      </div>
      <div class="model-profile-details">
        <div class="model-profile-title-row">
          <div>
            <h3 class="model-profile-name">${escapeHtml(model.fullName)}</h3>
            <p class="model-profile-subtitle">${escapeHtml(model.gender)} • ${escapeHtml(locationText || model.city)}</p>
          </div>
          <span class="badge">${escapeHtml(model.category)}</span>
        </div>

        <div class="model-profile-specs">
          ${ageText ? `<div class="model-spec-chip"><span>${escapeHtml(t("castingSpecAge"))}</span><strong>${escapeHtml(ageText)}</strong></div>` : ""}
          ${heightText ? `<div class="model-spec-chip"><span>${escapeHtml(t("castingSpecHeight"))}</span><strong>${escapeHtml(heightText)}</strong></div>` : ""}
          <div class="model-spec-chip"><span>${escapeHtml(t("castingSpecExperience"))}</span><strong>${escapeHtml(model.experienceLevel || "1-2 Years")}</strong></div>
          <div class="model-spec-chip"><span>${escapeHtml(t("castingSpecPortfolio"))}</span><strong>${escapeHtml(model.portfolioUrl || model.hasPortfolio ? t("castingPortfolioYes") : t("castingPortfolioOnRequest"))}</strong></div>
        </div>

        ${model.languages ? `<div class="model-profile-languages"><strong>${escapeHtml(t("modelLanguagesLabel"))}:</strong> ${escapeHtml(model.languages)}</div>` : ""}
        ${model.bio ? `<p class="model-profile-bio">${escapeHtml(model.bio)}</p>` : ""}

        <div class="model-profile-skills-wrap">
          <span class="model-profile-section-label">${escapeHtml(t("castingSkillsHeading"))}</span>
          <div class="model-skill-pills">
            ${skillTags.map((tag) => `<span class="model-skill-pill">${escapeHtml(tag)}</span>`).join("")}
          </div>
        </div>

        ${
          model.portfolioUrl || introVideoHref
            ? `<div style="display:flex;gap:8px;flex-wrap:wrap;">
                ${
                  model.portfolioUrl
                    ? `<a class="btn-outline btn-sm" href="${escapeHtml(model.portfolioUrl)}" target="_blank" rel="noopener noreferrer">${escapeHtml(t("companyViewPortfolioLink"))} ↗</a>`
                    : ""
                }
                ${
                  introVideoHref
                    ? `<a class="btn-outline btn-sm" href="${escapeHtml(introVideoHref)}" target="_blank" rel="noopener noreferrer">${escapeHtml(t("modelWatchIntroVideoBtn"))} ↗</a>`
                    : ""
                }
              </div>`
            : ""
        }

        <div class="public-model-privacy-note">🔒 ${escapeHtml(t("companyModelPrivacyNote"))}</div>

        <div class="model-profile-actions">
          <button
            type="button"
            class="btn-gold"
            data-casting-inquiry-model-id="${escapeHtml(model.id)}"
            data-casting-inquiry-model-name="${escapeHtml(model.fullName)}"
          >${escapeHtml(isSelected ? t("castingInquiryContinueBtn") : t("castingInquiryBtn"))}</button>
          <button
            type="button"
            class="btn-outline"
            data-audition-request-model-id="${escapeHtml(model.id)}"
            data-audition-request-model-name="${escapeHtml(model.fullName)}"
          >${escapeHtml(t("castingRequestAuditionBtn"))}</button>
          <button
            type="button"
            class="btn-outline"
            data-shoot-booking-model-id="${escapeHtml(model.id)}"
            data-shoot-booking-model-name="${escapeHtml(model.fullName)}"
          >${escapeHtml(t("castingShootBookingBtn"))}</button>
          <button
            type="button"
            class="btn-outline public-model-share-btn"
            data-share-model-profile="${escapeHtml(model.id)}"
            data-share-model-name="${escapeHtml(model.fullName)}"
            aria-label="${escapeHtml(t("castingShareBtn"))} ${escapeHtml(model.fullName)}"
          >
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="18" cy="5" r="3"></circle>
              <circle cx="6" cy="12" r="3"></circle>
              <circle cx="18" cy="19" r="3"></circle>
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line>
              <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
            </svg>
            <span class="share-btn-label">${escapeHtml(t("castingShareBtn"))}</span>
          </button>
        </div>
      </div>
    </div>`;
}

export const DEFAULT_STUDIO_PUBLIC_ORIGIN = "https://gen-lang-client-0548683163.web.app";

/**
 * Builds a privacy-safe direct shareable URL for a verified public model profile
 * (contains only the public model document ID — never phone, email, or address).
 */
export function buildModelProfileShareUrl(
  modelId,
  { origin = DEFAULT_STUDIO_PUBLIC_ORIGIN } = {}
) {
  const cleanId = String(modelId || "").trim();
  if (!cleanId) return "";
  const rawOrigin = String(origin || "").trim();
  const safeOrigin = /^https?:\/\/[^/\s]+/i.test(rawOrigin)
    ? rawOrigin.replace(/\/+$/, "")
    : DEFAULT_STUDIO_PUBLIC_ORIGIN;
  return `${safeOrigin}/#companies?model=${encodeURIComponent(cleanId)}`;
}

/**
 * Extracts a deep-linked public model ID from either the URL hash (#companies?model=<id>)
 * or standard query string (?model=<id>).
 */
export function extractSharedModelIdFromUrl({ hash = "", search = "" } = {}) {
  const cleanHash = String(hash || "").trim();
  const hashQueryIdx = cleanHash.indexOf("?");
  if (hashQueryIdx !== -1) {
    try {
      const hashParams = new URLSearchParams(cleanHash.slice(hashQueryIdx + 1));
      const id = String(hashParams.get("model") || "").trim();
      if (id) return id;
    } catch {
      // Ignore malformed hash params
    }
  }

  const cleanSearch = String(search || "").trim();
  if (cleanSearch) {
    try {
      const searchParams = new URLSearchParams(cleanSearch.startsWith("?") ? cleanSearch : `?${cleanSearch}`);
      const id = String(searchParams.get("model") || "").trim();
      if (id) return id;
    } catch {
      // Ignore malformed query params
    }
  }

  return "";
}
