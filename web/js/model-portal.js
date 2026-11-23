import { validateGalleryImageFile, computeScaledDimensions } from "./image-upload.js";

export const MAX_PORTFOLIO_PHOTOS = 4;
export const MAX_PORTFOLIO_PHOTO_CHARS = 150000;
export const FIRESTORE_DOC_LIMIT_BYTES = 1048576; // 1 MiB hard Firestore limit
export const MAX_MODEL_DOC_BYTES = 850000; // Strict app-level safety ceiling (~198 KiB below 1 MiB)

/**
 * Estimates the serialized Firestore document size in bytes (UTF-8 payload + field/overhead margin).
 */
export function estimateFirestoreModelDocBytes(docPayload = {}) {
  const json = JSON.stringify(docPayload || {});
  const utf8Bytes = typeof TextEncoder !== "undefined"
    ? new TextEncoder().encode(json).length
    : Buffer.byteLength(json, "utf8");
  // Add 1,024-byte conservative overhead for Firestore document path, field names, and timestamp metadata
  return utf8Bytes + 1024;
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function defaultEscapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function safePortalHttpUrl(url) {
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

export function calculateAgeFromDob(dobStr, referenceDate = new Date()) {
  const clean = String(dobStr || "").trim();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(clean)) return null;
  const [y, m, d] = clean.split("-").map((n) => Number.parseInt(n, 10));
  if (!y || m < 1 || m > 12 || d < 1 || d > 31) return null;
  const dob = new Date(Date.UTC(y, m - 1, d));
  if (
    dob.getUTCFullYear() !== y ||
    dob.getUTCMonth() !== m - 1 ||
    dob.getUTCDate() !== d
  ) {
    return null;
  }
  const ref = referenceDate instanceof Date ? referenceDate : new Date();
  let age = ref.getUTCFullYear() - y;
  const monthDiff = ref.getUTCMonth() - (m - 1);
  if (monthDiff < 0 || (monthDiff === 0 && ref.getUTCDate() < d)) {
    age -= 1;
  }
  if (!Number.isInteger(age) || age < 0 || age > 120) return null;
  return age;
}

export function isAllowedPortfolioPhotoSrc(src) {
  const clean = String(src || "").trim();
  if (!clean || clean.length > MAX_PORTFOLIO_PHOTO_CHARS) return false;
  if (/^data:image\/(jpeg|jpg|png|webp);base64,/i.test(clean)) return true;
  if (clean.startsWith("/images/")) return true;
  return Boolean(safePortalHttpUrl(clean));
}

export function sanitizePortfolioPhotoList(photos = []) {
  if (!Array.isArray(photos)) return [];
  const seen = new Set();
  const out = [];
  for (const raw of photos) {
    const clean = String(raw || "").trim();
    if (!clean || seen.has(clean)) continue;
    if (!isAllowedPortfolioPhotoSrc(clean)) continue;
    seen.add(clean);
    out.push(clean);
    if (out.length >= MAX_PORTFOLIO_PHOTOS) break;
  }
  return out;
}

/**
 * Compresses a single device photo client-side into a Spark-Free Firestore-safe JPEG Data URL
 * (<= 150 KB per photo, allowing up to 4 portfolio photos per model without Firebase Storage billing).
 */
export async function encodePortfolioPhotoFile(
  file,
  { maxDim = 760, maxChars = MAX_PORTFOLIO_PHOTO_CHARS } = {}
) {
  const check = validateGalleryImageFile(file);
  if (!check.ok) {
    throw new Error(check.error);
  }

  const rawDataUrl = await new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ""));
    reader.onerror = () => reject(new Error("Could not read selected portfolio photo."));
    reader.readAsDataURL(file);
  });

  const img = await new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error("Could not decode photo. Please use JPG, PNG, or WebP."));
    image.src = rawDataUrl;
  });

  let targetDim = maxDim;
  for (let pass = 0; pass < 4; pass++) {
    const dims = computeScaledDimensions(img.naturalWidth || img.width, img.naturalHeight || img.height, targetDim);
    const canvas = document.createElement("canvas");
    canvas.width = dims.width;
    canvas.height = dims.height;
    const ctx = canvas.getContext("2d");
    if (!ctx) throw new Error("Browser image canvas is unavailable.");
    ctx.fillStyle = "#0C0C0C";
    ctx.fillRect(0, 0, dims.width, dims.height);
    ctx.drawImage(img, 0, 0, dims.width, dims.height);

    let quality = 0.78;
    while (quality >= 0.42) {
      const dataUrl = canvas.toDataURL("image/jpeg", Number(quality.toFixed(2)));
      if (dataUrl.length <= maxChars) {
        return {
          dataUrl,
          width: dims.width,
          height: dims.height,
          approxKb: Math.max(1, Math.round((dataUrl.length * 0.75) / 1024))
        };
      }
      quality -= 0.08;
    }
    targetDim = Math.max(320, Math.round(targetDim * 0.76));
  }
  throw new Error("Photo is too large for free-tier cloud sync. Please select a smaller photo.");
}

/**
 * Validates Model Self-Registration or Model Dashboard Profile Edit payloads.
 */
export function validateModelSelfRegistration(payload = {}, { isEdit = false, lang = "en" } = {}) {
  const hi = lang === "hi";
  const errors = [];

  const fullName = String(payload.fullName || "").trim();
  const gender = String(payload.gender || "Female").trim().slice(0, 50) || "Female";
  const dateOfBirth = String(payload.dateOfBirth || "").trim();
  const dobAge = dateOfBirth ? calculateAgeFromDob(dateOfBirth) : null;
  const parsedAge = Number.parseInt(String(payload.age ?? ""), 10);
  const resolvedAge = Number.isInteger(dobAge) ? dobAge : parsedAge;

  const city = String(payload.city || "").trim();
  const state = String(payload.state || "").trim().slice(0, 60);
  const rawHeight = String(payload.heightCm ?? "").trim();
  const parsedHeight = rawHeight ? Number.parseInt(rawHeight, 10) : null;
  const skills = String(payload.skills || "").trim().slice(0, 1000);
  const languages = String(payload.languages || "").trim().slice(0, 300);
  const experienceLevel = String(payload.experienceLevel || "1-2 Years").trim().slice(0, 40) || "1-2 Years";
  const bio = String(payload.bio || "").trim().slice(0, 500);
  const rawPortfolioUrl = String(payload.portfolioUrls || "").trim();
  const portfolioUrls = rawPortfolioUrl ? safePortalHttpUrl(rawPortfolioUrl) : "";
  const rawVideoUrl = String(payload.introVideoUrl || "").trim();
  const introVideoUrl = rawVideoUrl ? safePortalHttpUrl(rawVideoUrl) : "";
  const contactEmail = String(payload.contactEmail || "").trim().toLowerCase();
  const phoneNumber = String(payload.phoneNumber || "").trim();
  const phoneDigits = phoneNumber.replace(/\D/g, "");
  const portfolioPhotos = sanitizePortfolioPhotoList(payload.portfolioPhotos || []);
  const photoConsent = payload.photoConsent !== false;
  const consentGiven = Boolean(payload.consentGiven);

  if (fullName.length < 2 || fullName.length > 100) {
    errors.push(hi ? "कृपया सही पूरा नाम दर्ज करें (2–100 अक्षर)।" : "Please enter your full name (2–100 characters).");
  }

  if (dateOfBirth && dobAge === null) {
    errors.push(hi ? "कृपया सही जन्म तिथि चुनें।" : "Please enter a valid Date of Birth.");
  }

  if (!Number.isInteger(resolvedAge) || resolvedAge < 18 || resolvedAge > 100) {
    errors.push(
      hi
        ? "आवेदक की आयु कम से कम 18 वर्ष होनी अनिवार्य है।"
        : "Applicant must be strictly 18 years of age or older."
    );
  }

  if (city.length < 2 || city.length > 100) {
    errors.push(hi ? "कृपया अपना शहर दर्ज करें।" : "Please enter your city.");
  }

  if (rawHeight && (!Number.isInteger(parsedHeight) || parsedHeight < 120 || parsedHeight > 230)) {
    errors.push(hi ? "कृपया सही कद (120–230 सेमी) दर्ज करें।" : "Please enter a valid height between 120 and 230 cm.");
  }

  if (phoneDigits.length < 8 || phoneDigits.length > 15 || phoneNumber.length > 20) {
    errors.push(hi ? "कृपया सही मोबाइल नंबर (8–20 अंक) दर्ज करें।" : "Please enter a valid mobile number (8–20 digits).");
  }

  if (contactEmail && (!EMAIL_RE.test(contactEmail) || contactEmail.length > 128)) {
    errors.push(hi ? "कृपया सही संपर्क ईमेल पता दर्ज करें।" : "Please enter a valid contact email address.");
  }

  if (rawPortfolioUrl && !portfolioUrls) {
    errors.push(hi ? "पोर्टफोलियो लिंक https:// से शुरू होना चाहिए।" : "Portfolio URL must be a valid https:// link.");
  }

  if (rawVideoUrl && !introVideoUrl) {
    errors.push(hi ? "परिचय वीडियो लिंक https:// से शुरू होना चाहिए।" : "Introduction Video link must be a valid https:// URL (YouTube, Instagram Reel, Vimeo, or Drive).");
  }

  if (!isEdit && !consentGiven) {
    errors.push(hi ? "18+ आयु व कास्टिंग सहमति अनिवार्य है।" : "18+ consent verification is mandatory.");
  }

  const cleaned = {
    fullName,
    gender,
    dateOfBirth,
    age: Number.isInteger(resolvedAge) ? resolvedAge : 18,
    city,
    state,
    heightCm: Number.isInteger(parsedHeight) ? parsedHeight : null,
    skills,
    languages,
    experienceLevel,
    bio,
    portfolioUrls,
    portfolioPhotos,
    introVideoUrl,
    contactEmail,
    phoneNumber,
    photoConsent,
    consentGiven: isEdit ? true : consentGiven
  };

  if (estimateFirestoreModelDocBytes(cleaned) > MAX_MODEL_DOC_BYTES) {
    errors.push(
      hi
        ? "कुल पोर्टफोलियो फ़ोटो का आकार फ़्री क्लाउड सीमा से अधिक है। कृपया कम या छोटे फ़ोटो चुनें।"
        : "Total portfolio photos exceed the safe free-tier cloud document limit. Please use smaller photos."
    );
  }

  return {
    valid: errors.length === 0,
    errors,
    cleaned
  };
}

/**
 * Builds a Firestore notification payload when Admin approves, rejects, or disables a model.
 */
export function buildModelNotificationPayload({
  modelId,
  userId,
  fullName = "Artist",
  status = "pending",
  adminNotes = "",
  isDisabled = false
} = {}) {
  const cleanUid = String(userId || "").trim();
  const cleanModelId = String(modelId || "").trim();
  if (!cleanUid || !cleanModelId) return null;

  const st = String(status || "pending").trim().toLowerCase();
  const noteSuffix = String(adminNotes || "").trim() ? ` Studio Note: ${String(adminNotes).trim()}` : "";

  if (isDisabled) {
    return {
      userId: cleanUid,
      bookingId: `model_${cleanModelId}`,
      title: "Model Profile Temporarily Disabled",
      message: `Hi ${fullName}, your Star Artist Studio model profile is currently hidden from the public catalog.${noteSuffix}`.slice(0, 1900),
      status: "Disabled",
      serviceType: "Model Roster Verification",
      channelType: "model_status",
      isRead: false
    };
  }

  if (st === "approved") {
    return {
      userId: cleanUid,
      bookingId: `model_${cleanModelId}`,
      title: "Congratulations! Model Profile Approved",
      message: `Welcome to the Official Star Artist Studio Roster, ${fullName}! Your profile is now live in our Casting & Model Discovery Catalog.${noteSuffix}`.slice(0, 1900),
      status: "Approved",
      serviceType: "Model Roster Verification",
      channelType: "model_status",
      isRead: false
    };
  }

  if (st === "rejected") {
    return {
      userId: cleanUid,
      bookingId: `model_${cleanModelId}`,
      title: "Model Application Update",
      message: `Hi ${fullName}, your model application was not approved at this time. You can update your profile and photos anytime.${noteSuffix}`.slice(0, 1900),
      status: "Rejected",
      serviceType: "Model Roster Verification",
      channelType: "model_status",
      isRead: false
    };
  }

  return null;
}

export function renderPortfolioPhotoStripHtml(
  photos = [],
  { editable = false, scope = "reg", t = (k) => k, escapeHtml = defaultEscapeHtml } = {}
) {
  const list = sanitizePortfolioPhotoList(photos);
  if (!list.length) {
    return `<span class="model-photos-empty">${escapeHtml(t("modelPhotosEmptyHint"))}</span>`;
  }
  return list
    .map(
      (src, idx) => `
      <div class="model-photo-thumb-item ${idx === 0 ? "is-cover" : ""}">
        <img src="${escapeHtml(src)}" alt="Portfolio photo ${idx + 1}" loading="lazy" />
        ${idx === 0 ? `<span class="model-photo-cover-pill">${escapeHtml(t("modelCoverBadge"))}</span>` : ""}
        ${
          editable
            ? `<button
                type="button"
                class="model-photo-remove-btn"
                data-remove-portfolio-photo="${idx}"
                data-photo-scope="${escapeHtml(scope)}"
                aria-label="Remove photo ${idx + 1}"
              >✕</button>`
            : ""
        }
      </div>`
    )
    .join("");
}

/**
 * Renders the authenticated Model Dashboard: Approval Notifications, status stepper,
 * submitted profile details, portfolio photos, and inline Edit Profile & Photos form.
 */
export function renderModelDashboardHtml({
  user = null,
  models = [],
  notifications = [],
  editingModelId = null,
  editDraftPhotos = [],
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  if (!user) {
    return `
      <div class="model-dashboard-signin-card">
        <p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("modelDashboardSignInPrompt"))}</p>
        <button type="button" class="btn-gold btn-sm" data-open-auth-modal="true" style="align-self:flex-start;">
          ${escapeHtml(t("modelDashboardSignInBtn"))}
        </button>
      </div>`;
  }

  const modelNotifs = (Array.isArray(notifications) ? notifications : []).filter(
    (n) => n && (n.channelType === "model_status" || String(n.bookingId || "").startsWith("model_"))
  );

  const notifHtml = modelNotifs.length
    ? `<div class="model-notifications-box" aria-live="polite">
        <div class="model-notifications-head">
          <strong>🔔 ${escapeHtml(t("modelNotificationsHeading"))}</strong>
          <span class="badge">${modelNotifs.filter((n) => !n.isRead).length} ${escapeHtml(t("modelNotificationsUnread"))}</span>
        </div>
        ${modelNotifs
          .slice(0, 5)
          .map((n) => {
            const st = String(n.status || "").toLowerCase();
            const toneCls = st === "approved" ? "is-approved" : st === "rejected" || st === "disabled" ? "is-rejected" : "";
            return `
            <div class="model-notif-item ${toneCls} ${n.isRead ? "is-read" : ""}">
              <div style="display:flex;justify-content:space-between;gap:8px;align-items:flex-start;">
                <strong>${escapeHtml(n.title || "Studio Update")}</strong>
                ${
                  !n.isRead && n.id
                    ? `<button type="button" class="btn-outline btn-xs" data-mark-model-notif-read="${escapeHtml(n.id)}">${escapeHtml(t("modelMarkReadBtn"))}</button>`
                    : `<span class="badge">${escapeHtml(n.status || "Info")}</span>`
                }
              </div>
              <p style="margin:4px 0 0;font-size:0.84rem;color:var(--text-secondary);">${escapeHtml(n.message || "")}</p>
            </div>`;
          })
          .join("")}
      </div>`
    : "";

  if (!Array.isArray(models) || !models.length) {
    return `
      ${notifHtml}
      <div class="model-dashboard-empty">
        <p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("modelDashboardEmpty"))}</p>
      </div>`;
  }

  const cardsHtml = models
    .map((m) => {
      const normalized = String(m.status || "pending").toLowerCase();
      const isDisabled = m.isDisabled === true;
      const statusCls = isDisabled ? "status-rejected" : `status-${normalized}`;
      const statusLabel = isDisabled
        ? t("modelStatusDisabled")
        : normalized === "approved"
        ? t("rosterBadge")
        : normalized === "rejected"
        ? t("modelStatusRejected")
        : t("modelStatusPending");

      const isEditing = editingModelId === m.id;
      const photos = sanitizePortfolioPhotoList(m.portfolioPhotos || []);
      const videoHref = safePortalHttpUrl(m.introVideoUrl);
      const portfolioHref = safePortalHttpUrl(m.portfolioUrls);

      const step1Done = true;
      const step2Done = normalized === "approved" || normalized === "rejected" || isDisabled;
      const step3Done = normalized === "approved" && !isDisabled;

      const editFormHtml = isEditing
        ? `
        <form class="model-edit-form form-grid two-col" data-model-edit-form="${escapeHtml(m.id)}" novalidate>
          <div class="field full">
            <strong style="color:var(--gold);">${escapeHtml(t("modelEditProfileTitle"))}</strong>
            <span style="font-size:0.78rem;color:var(--text-muted);">${escapeHtml(t("modelEditSecurityHint"))}</span>
          </div>
          <div class="field">
            <label>${escapeHtml(t("fullNameLabel"))}</label>
            <input name="fullName" class="input" required minlength="2" maxlength="100" value="${escapeHtml(m.fullName || "")}" />
          </div>
          <div class="field">
            <label>${escapeHtml(t("genderLabel"))}</label>
            <select name="gender" class="input">
              ${["Female", "Male", "Non-Binary / Prefer not to say"]
                .map((g) => `<option value="${escapeHtml(g)}" ${m.gender === g ? "selected" : ""}>${escapeHtml(g)}</option>`)
                .join("")}
            </select>
          </div>
          <div class="field">
            <label>${escapeHtml(t("modelDobLabel"))}</label>
            <input name="dateOfBirth" type="date" class="input" value="${escapeHtml(m.dateOfBirth || "")}" />
          </div>
          <div class="field">
            <label>${escapeHtml(t("ageLabel"))}</label>
            <input name="age" type="number" min="18" max="100" class="input" required value="${escapeHtml(m.age || 18)}" />
          </div>
          <div class="field">
            <label>${escapeHtml(t("castingCityLabel"))}</label>
            <input name="city" class="input" required maxlength="100" value="${escapeHtml(m.city || "")}" />
          </div>
          <div class="field">
            <label>${escapeHtml(t("modelStateLabel"))}</label>
            <input name="state" class="input" maxlength="60" value="${escapeHtml(m.state || "")}" />
          </div>
          <div class="field">
            <label>${escapeHtml(t("modelHeightLabel"))}</label>
            <input name="heightCm" type="number" min="120" max="230" class="input" value="${escapeHtml(m.heightCm || "")}" />
          </div>
          <div class="field">
            <label>${escapeHtml(t("modelExperienceLabel"))}</label>
            <select name="experienceLevel" class="input">
              ${["Fresher", "1-2 Years", "3-5 Years", "5+ Years"]
                .map((exp) => `<option value="${escapeHtml(exp)}" ${(m.experienceLevel || "1-2 Years") === exp ? "selected" : ""}>${escapeHtml(exp)}</option>`)
                .join("")}
            </select>
          </div>
          <div class="field">
            <label>${escapeHtml(t("skillsLabel"))}</label>
            <input name="skills" class="input" maxlength="1000" value="${escapeHtml(m.skills || "")}" />
          </div>
          <div class="field">
            <label>${escapeHtml(t("modelLanguagesLabel"))}</label>
            <input name="languages" class="input" maxlength="300" value="${escapeHtml(m.languages || "")}" />
          </div>
          <div class="field">
            <label>${escapeHtml(t("modelEmailPrivateLabel"))}</label>
            <input name="contactEmail" type="email" class="input" maxlength="128" value="${escapeHtml(m.contactEmail || "")}" />
          </div>
          <div class="field">
            <label>${escapeHtml(t("modelPhonePrivateLabel"))}</label>
            <input name="phoneNumber" type="tel" class="input" required minlength="8" maxlength="20" value="${escapeHtml(m.phoneNumber || "")}" />
          </div>
          <div class="field full">
            <label>${escapeHtml(t("modelBioLabel"))}</label>
            <textarea name="bio" class="input" rows="2" maxlength="500">${escapeHtml(m.bio || "")}</textarea>
          </div>
          <div class="field">
            <label>${escapeHtml(t("portfolioUrlLabel"))}</label>
            <input name="portfolioUrls" type="url" class="input" maxlength="2000" value="${escapeHtml(m.portfolioUrls || "")}" />
          </div>
          <div class="field">
            <label>${escapeHtml(t("modelIntroVideoLabel"))}</label>
            <input name="introVideoUrl" type="url" class="input" maxlength="2000" value="${escapeHtml(m.introVideoUrl || "")}" />
          </div>
          <div class="field full">
            <label>${escapeHtml(t("modelPortfolioPhotosLabel"))} (${editDraftPhotos.length}/${MAX_PORTFOLIO_PHOTOS})</label>
            <div class="model-photo-upload-row">
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                class="input"
                data-edit-portfolio-photos-input="${escapeHtml(m.id)}"
              />
            </div>
            <div class="model-photo-strip" data-edit-portfolio-strip>
              ${renderPortfolioPhotoStripHtml(editDraftPhotos, { editable: true, scope: "edit", t, escapeHtml })}
            </div>
          </div>
          <div class="field full">
            <label class="checkbox-row">
              <input name="photoConsent" type="checkbox" ${m.photoConsent !== false ? "checked" : ""} />
              <span>${escapeHtml(t("modelPhotoConsentLabel"))}</span>
            </label>
          </div>
          <div class="field full" style="display:flex;gap:8px;flex-wrap:wrap;">
            <button type="submit" class="btn-gold btn-sm">${escapeHtml(t("modelSaveProfileChangesBtn"))}</button>
            <button type="button" class="btn-outline btn-sm" data-cancel-edit-model="${escapeHtml(m.id)}">${escapeHtml(t("modelCancelEditBtn"))}</button>
          </div>
        </form>`
        : "";

      return `
      <article class="studio-card model-dashboard-card" data-my-model-card="${escapeHtml(m.id)}">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:8px;flex-wrap:wrap;">
          <div>
            <strong style="font-size:1.04rem;color:var(--gold-light);">${escapeHtml(m.fullName)} (${escapeHtml(m.age)} ${escapeHtml(t("castingYearsUnit"))})</strong>
            <div style="font-size:0.84rem;color:var(--text-secondary);">
              ${escapeHtml(m.gender || "Model")} • ${escapeHtml(m.city || "")}${m.state && !String(m.city || "").toLowerCase().includes(String(m.state).toLowerCase()) ? `, ${escapeHtml(m.state)}` : ""}
              ${m.heightCm ? ` • ${escapeHtml(m.heightCm)} cm` : ""}
              ${m.experienceLevel ? ` • ${escapeHtml(m.experienceLevel)}` : ""}
            </div>
          </div>
          <span class="badge ${statusCls}">${escapeHtml(statusLabel)}</span>
        </div>

        <div class="model-status-stepper" aria-label="Application status">
          <span class="model-step-pill ${step1Done ? "is-done" : ""}">1. ${escapeHtml(t("modelStepSubmitted"))}</span>
          <span class="model-step-pill ${step2Done ? "is-done" : "is-active"}">2. ${escapeHtml(t("modelStepReview"))}</span>
          <span class="model-step-pill ${step3Done ? "is-done" : ""}">3. ${escapeHtml(t("modelStepLive"))}</span>
        </div>

        ${
          photos.length
            ? `<div class="model-photo-strip">${renderPortfolioPhotoStripHtml(photos, { editable: false, t, escapeHtml })}</div>`
            : ""
        }

        <div class="model-dashboard-meta-list">
          ${m.skills ? `<div><strong>${escapeHtml(t("castingSkillsFilterLabel"))}:</strong> ${escapeHtml(m.skills)}</div>` : ""}
          ${m.languages ? `<div><strong>${escapeHtml(t("modelLanguagesLabel"))}:</strong> ${escapeHtml(m.languages)}</div>` : ""}
          ${m.bio ? `<div><strong>${escapeHtml(t("modelBioLabel"))}:</strong> ${escapeHtml(m.bio)}</div>` : ""}
          <div style="font-size:0.78rem;color:var(--text-muted);">
            🔒 ${escapeHtml(t("modelDashboardPrivateNote"))} • ${m.photoConsent !== false ? escapeHtml(t("modelPhotoConsentGrantedShort")) : escapeHtml(t("modelPhotoConsentHiddenShort"))}
          </div>
        </div>

        <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
          ${
            !isEditing
              ? `<button type="button" class="btn-outline btn-sm" data-start-edit-model="${escapeHtml(m.id)}">✎ ${escapeHtml(t("modelEditProfileBtn"))}</button>`
              : ""
          }
          ${
            portfolioHref
              ? `<a href="${escapeHtml(portfolioHref)}" target="_blank" rel="noopener noreferrer" class="btn-outline btn-xs">${escapeHtml(t("companyViewPortfolioLink"))} ↗</a>`
              : ""
          }
          ${
            videoHref
              ? `<a href="${escapeHtml(videoHref)}" target="_blank" rel="noopener noreferrer" class="btn-outline btn-xs">${escapeHtml(t("modelWatchIntroVideoBtn"))} ↗</a>`
              : ""
          }
        </div>

        ${
          m.adminNotes
            ? `<div class="booking-card-notes"><strong>Studio Admin Note:</strong> ${escapeHtml(m.adminNotes)}</div>`
            : ""
        }

        ${editFormHtml}
      </article>`;
    })
    .join("");

  return `${notifHtml}${cardsHtml}`;
}
