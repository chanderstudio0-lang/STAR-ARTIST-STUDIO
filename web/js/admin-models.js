import { renderModelExperienceOverviewCardHtml } from "./admin-model-experience.js";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function safeHttpUrl(url) {
  const clean = String(url || "").trim();
  if (clean.startsWith("https://") || clean.startsWith("http://")) {
    return clean;
  }
  return "";
}

export function normalizeModelStatus(status) {
  const clean = String(status || "").trim().toLowerCase();
  if (clean === "approved") return "approved";
  if (clean === "rejected") return "rejected";
  return "pending";
}

/**
 * Returns true when a model has completed all required casting documentation
 * and holds active Official Studio Roster approval:
 * - Approved roster status and not disabled
 * - 18+ age verified
 * - Written 18+ consent signed & photo release consent enabled
 * - Valid city & phone contact documentation
 * - Verified portfolio documentation (valid portfolio URL or uploaded portfolio photo)
 */
export function isVerifiedRosterModel(model) {
  if (!model || typeof model !== "object") return false;
  if (normalizeModelStatus(model.status) !== "approved") return false;
  if (model.isDisabled === true) return false;
  const ageNum = Number(model.age);
  if (!Number.isFinite(ageNum) || ageNum < 18) return false;
  if (model.consentGiven !== true) return false;
  if (model.photoConsent === false) return false;

  const hasCity = String(model.city || "").trim().length >= 2;
  const phoneDigits = String(model.phoneNumber || "").replace(/\D/g, "");
  const hasPhone = phoneDigits.length >= 8;
  const hasPortfolioLink = Boolean(safeHttpUrl(model.portfolioUrls));
  const hasPortfolioPhoto =
    Array.isArray(model.portfolioPhotos) &&
    model.portfolioPhotos.some(
      (src) =>
        typeof src === "string" &&
        (src.startsWith("data:image/") || safeHttpUrl(src) || src.startsWith("/images/"))
    );

  return hasCity && hasPhone && (hasPortfolioLink || hasPortfolioPhoto);
}

export function summarizeModelApplications(models = []) {
  return models.reduce(
    (acc, m) => {
      acc.total += 1;
      const st = normalizeModelStatus(m?.status);
      if (st === "approved") acc.roster += 1;
      else if (st === "rejected") acc.rejected += 1;
      else acc.pending += 1;
      return acc;
    },
    { total: 0, pending: 0, roster: 0, rejected: 0 }
  );
}

export function filterModelApplications(models = [], filter = "pending", searchQuery = "") {
  const normalizedFilter = String(filter || "pending").trim().toLowerCase();
  const statusMatches = models.filter((m) => {
    const st = normalizeModelStatus(m?.status);
    if (normalizedFilter === "approved" || normalizedFilter === "roster") return st === "approved";
    if (normalizedFilter === "rejected") return st === "rejected";
    if (normalizedFilter === "pending") return st === "pending";
    return true;
  });

  const q = String(searchQuery || "").trim().toLowerCase();
  if (!q) return statusMatches;

  return statusMatches.filter((m) => {
    const hay = [
      m?.fullName,
      m?.city,
      m?.state,
      m?.phoneNumber,
      m?.contactEmail,
      m?.languages,
      m?.skills,
      m?.portfolioUrls,
      m?.adminNotes
    ]
      .join(" ")
      .toLowerCase();
    return hay.includes(q);
  });
}

export function validateManualModelVerification({
  status,
  age,
  consentGiven,
  checklistConfirmed,
  adminNotes,
  lang = "en"
}) {
  const st = normalizeModelStatus(status);
  const isHindi = lang === "hi";

  if (st === "approved") {
    const numAge = Number(age);
    if (!Number.isFinite(numAge) || numAge < 18) {
      return isHindi
        ? "18 वर्ष से कम आयु के आवेदक को आधिकारिक स्टूडियो रोस्टर में नहीं जोड़ा जा सकता।"
        : "Applicants under 18 years old cannot be added to the Official Studio Roster.";
    }
    if (!consentGiven) {
      return isHindi
        ? "आधिकारिक रोस्टर सत्यापन से पहले लिखित 18+ सहमति अनिवार्य है।"
        : "Written 18+ consent is required before adding an applicant to the Official Studio Roster.";
    }
    if (!checklistConfirmed) {
      return isHindi
        ? "रोस्टर में जोड़ने से पहले कृपया मैन्युअल 18+ आयु, सहमति व पोर्टफोलियो सत्यापन चेकबॉक्स पर टिक करें।"
        : "Please tick the manual 18+ ID, consent & portfolio verification checkbox before adding to the Official Studio Roster.";
    }
  } else if (st === "rejected") {
    const note = String(adminNotes || "").trim();
    if (note.length < 3) {
      return isHindi
        ? "कृपया आवेदन अस्वीकार करने से पहले संक्षिप्त कारण या एडमिन नोट दर्ज करें।"
        : "Please enter or select a brief reason in Admin Notes before rejecting this application.";
    }
    if (note.length > 1000) {
      return isHindi
        ? "एडमिन नोट 1000 अक्षरों या उससे कम होना चाहिए।"
        : "Admin notes must be 1000 characters or fewer.";
    }
  }
  return null;
}

/**
 * Validates and partitions a bulk model status update ("approved" or "rejected")
 * across multiple selected models simultaneously while enforcing 18+ age and
 * written consent compliance for approvals and a valid review reason for rejections.
 */
export function evaluateBulkModelUpdate({
  models = [],
  selectedIds = [],
  nextStatus = "approved",
  bulkNote = "",
  drafts = {},
  lang = "en"
} = {}) {
  const st = normalizeModelStatus(nextStatus);
  const isHindi = lang === "hi";
  const cleanBulkNote = String(bulkNote || "").trim();

  if (st !== "approved" && st !== "rejected") {
    return {
      ok: false,
      error: isHindi ? "अमान्य बल्क कार्रवाई।" : "Invalid bulk action status.",
      eligible: [],
      skipped: []
    };
  }

  const idSet = new Set((Array.isArray(selectedIds) ? selectedIds : []).map((id) => String(id || "").trim()).filter(Boolean));
  const selectedModels = models.filter((m) => m && idSet.has(String(m.id || "")));
  if (selectedModels.length === 0) {
    return {
      ok: false,
      error: isHindi
        ? "कृपया पहले कम से कम एक मॉडल चुनें।"
        : "Please select at least one model first.",
      eligible: [],
      skipped: []
    };
  }

  if (cleanBulkNote.length > 1000) {
    return {
      ok: false,
      error: isHindi
        ? "बल्क नोट 1000 अक्षरों या उससे कम होना चाहिए।"
        : "Bulk note must be 1000 characters or fewer.",
      eligible: [],
      skipped: []
    };
  }

  const eligible = [];
  const skipped = [];

  for (const m of selectedModels) {
    const draftNote = String(drafts?.[m.id]?.notes ?? "").trim();
    const existingNote = String(m.adminNotes || "").trim();
    const resolvedNote =
      cleanBulkNote ||
      draftNote ||
      existingNote ||
      (st === "approved"
        ? isHindi
          ? "बल्क सत्यापन: आधिकारिक रोस्टर हेतु स्वीकृत"
          : "Bulk verified for Official Studio Roster"
        : "");

    if (st === "approved") {
      const ageNum = Number(m.age);
      if (!Number.isFinite(ageNum) || ageNum < 18) {
        skipped.push({
          model: m,
          reason: isHindi ? "आयु 18 वर्ष से कम है" : "Under 18 years old"
        });
        continue;
      }
      if (!m.consentGiven) {
        skipped.push({
          model: m,
          reason: isHindi ? "18+ लिखित सहमति नहीं है" : "Missing written 18+ consent"
        });
        continue;
      }
      eligible.push({ model: m, adminNotes: resolvedNote });
    } else {
      if (resolvedNote.length < 3) {
        skipped.push({
          model: m,
          reason: isHindi ? "अस्वीकृति कारण आवश्यक है" : "Rejection reason required"
        });
        continue;
      }
      eligible.push({ model: m, adminNotes: resolvedNote });
    }
  }

  if (eligible.length === 0) {
    return {
      ok: false,
      error:
        st === "rejected"
          ? isHindi
            ? "चयनित मॉडल्स को अस्वीकार करने के लिए कृपया बल्क नोट बॉक्स में संक्षिप्त कारण लिखें।"
            : "Please enter a brief rejection reason in the Bulk Note box before rejecting selected models."
          : isHindi
          ? "चयनित मॉडल्स में से कोई भी 18+ आयु और लिखित सहमति की शर्त पूरी नहीं करता।"
          : "None of the selected models meet the mandatory 18+ age and written consent requirements.",
      eligible: [],
      skipped
    };
  }

  return {
    ok: true,
    error: null,
    status: st,
    eligible,
    skipped
  };
}

export function renderAdminModelsSectionHtml({
  models = [],
  filter = "pending",
  searchQuery = "",
  drafts = {},
  experienceScope = "all",
  selectedIds = [],
  bulkNote = "",
  bulkBusy = false,
  lang = "en",
  t = (k) => k
}) {
  const counts = summarizeModelApplications(models);
  const visible = filterModelApplications(models, filter, searchQuery);
  const isHindi = lang === "hi";
  const activeExpScope = experienceScope === "approved" ? "approved" : "all";
  const selectedSet = new Set((Array.isArray(selectedIds) ? selectedIds : []).map((id) => String(id || "")));
  const visibleSelectedCount = visible.filter((m) => selectedSet.has(String(m.id || ""))).length;
  const allVisibleSelected = visible.length > 0 && visibleSelectedCount === visible.length;
  const totalSelectedCount = selectedSet.size;

  const filterPills = [
    { key: "pending", label: `${t("adminFilterPending")} (${counts.pending})` },
    { key: "approved", label: `${t("adminFilterRoster")} (${counts.roster})` },
    { key: "rejected", label: `${t("adminFilterRejected")} (${counts.rejected})` },
    { key: "all", label: `${t("statusAll")} (${counts.total})` }
  ]
    .map(
      (pill) => `
      <button
        type="button"
        class="chip-btn ${filter === pill.key ? "active" : ""}"
        data-adm-model-filter="${pill.key}"
      >${escapeHtml(pill.label)}</button>`
    )
    .join("");

  const quickNotes = isHindi
    ? [
        "कमर्शियल व एडिटोरियल रोस्टर हेतु सत्यापित",
        "पोर्टफोलियो लिंक अपूर्ण — पुनः अपलोड करें",
        "18+ आयु पहचान सत्यापन आवश्यक"
      ]
    : [
        "Verified for Official Commercial Roster",
        "Incomplete portfolio — please resubmit links",
        "18+ age proof verification required"
      ];

  const cardsHtml = visible.length
    ? visible
        .map((m) => {
          const st = normalizeModelStatus(m.status);
          const isDisabled = m.isDisabled === true;
          const draft = drafts[m.id] || {};
          const checked = draft.checked !== undefined ? Boolean(draft.checked) : st === "approved";
          const noteValue = draft.notes !== undefined ? draft.notes : m.adminNotes || "";
          const ageOk = Number(m.age) >= 18;
          const portfolioHref = safeHttpUrl(m.portfolioUrls);
          const introVideoHref = safeHttpUrl(m.introVideoUrl);
          const photos = Array.isArray(m.portfolioPhotos)
            ? m.portfolioPhotos.filter((src) => typeof src === "string" && (src.startsWith("data:image/") || safeHttpUrl(src) || src.startsWith("/images/"))).slice(0, 4)
            : [];
          const statusCls = isDisabled
            ? "status-rejected"
            : st === "approved"
            ? "status-approved"
            : st === "rejected"
            ? "status-rejected"
            : "status-pending";
          const cardCls = isDisabled
            ? "is-rejected"
            : st === "approved"
            ? "is-roster"
            : st === "rejected"
            ? "is-rejected"
            : "";
          const badgeLabel = isDisabled
            ? isHindi
              ? "प्रोफ़ाइल निष्क्रिय"
              : "DISABLED"
            : st === "approved"
            ? t("rosterBadge")
            : st === "rejected"
            ? isHindi
              ? "अस्वीकृत"
              : "REJECTED"
            : isHindi
            ? "सत्यापन लंबित"
            : "PENDING REVIEW";

          const isVerifiedRoster = isVerifiedRosterModel(m);
          const verifiedRosterBadgeHtml = isVerifiedRoster
            ? `<span class="verified-roster-icon-badge" data-verified-roster-badge="${escapeHtml(m.id)}" title="${escapeHtml(t("verifiedRosterTooltip"))}" aria-label="${escapeHtml(t("verifiedRosterBadgeLabel"))}">
                <svg class="verified-roster-icon-svg" viewBox="0 0 24 24" width="14" height="14" fill="none" aria-hidden="true" focusable="false">
                  <path d="M12 2L4 5.5V11.2C4 16.4 7.4 21.1 12 22.5C16.6 21.1 20 16.4 20 11.2V5.5L12 2Z" fill="currentColor" fill-opacity="0.2" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
                  <path d="M8.5 12.2L10.9 14.6L15.8 9.6" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>${escapeHtml(t("verifiedRosterBadgeLabel"))}</span>
              </span>`
            : "";

          const isBulkSelected = selectedSet.has(String(m.id || ""));

          return `
        <article class="studio-card admin-model-card ${cardCls} ${isBulkSelected ? "is-bulk-selected" : ""}" data-adm-model-card="${escapeHtml(m.id)}">
          <div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:8px;align-items:flex-start;">
            <div class="admin-model-header-left">
              <label class="admin-bulk-checkbox-wrap" title="${escapeHtml(t("adminBulkSelectModelLabel"))}">
                <input
                  type="checkbox"
                  class="admin-bulk-checkbox"
                  data-adm-model-select="${escapeHtml(m.id)}"
                  aria-label="${escapeHtml(t("adminBulkSelectModelLabel"))} ${escapeHtml(m.fullName)}"
                  ${isBulkSelected ? "checked" : ""}
                  ${bulkBusy ? "disabled" : ""}
                />
              </label>
              <div>
                <div class="admin-model-name-row">
                  <strong style="font-size:1.02rem;color:var(--gold-light);">${escapeHtml(m.fullName)}</strong>
                  ${verifiedRosterBadgeHtml}
                </div>
                <div style="font-size:0.84rem;color:var(--text-secondary);">
                  ${escapeHtml(m.gender || "Model")} • ${escapeHtml(m.city || "")}${m.state && !String(m.city || "").toLowerCase().includes(String(m.state).toLowerCase()) ? `, ${escapeHtml(m.state)}` : ""} • ${escapeHtml(m.phoneNumber || "")}
                  ${m.contactEmail ? ` • ${escapeHtml(m.contactEmail)}` : ""}
                </div>
              </div>
            </div>
            <span class="badge ${statusCls}">${escapeHtml(badgeLabel)}</span>
          </div>

          <div class="admin-model-compliance-row">
            <span class="badge ${ageOk ? "status-confirmed" : "status-rejected"}">
              ${ageOk ? `18+ Eligible (${escapeHtml(m.age)} yrs${m.dateOfBirth ? ` • DOB: ${escapeHtml(m.dateOfBirth)}` : ""})` : `Under 18 (${escapeHtml(m.age)})`}
            </span>
            <span class="badge ${m.consentGiven ? "status-confirmed" : "status-rejected"}">
              ${m.consentGiven ? (isHindi ? "सहमति हस्ताक्षरित ✓" : "Written Consent ✓") : "Missing Consent"}
            </span>
            <span class="badge ${m.photoConsent !== false ? "status-confirmed" : "status-pending"}">
              ${m.photoConsent !== false ? "Photo Consent ✓" : "Photo Private"}
            </span>
            ${m.heightCm ? `<span class="badge">${escapeHtml(m.heightCm)} cm</span>` : ""}
            ${m.experienceLevel ? `<span class="badge">${escapeHtml(m.experienceLevel)}</span>` : ""}
          </div>

          ${
            photos.length
              ? `<div class="admin-model-photo-strip">
                  ${photos
                    .map(
                      (src, idx) =>
                        `<img src="${escapeHtml(src)}" alt="${escapeHtml(m.fullName)} photo ${idx + 1}" class="admin-model-thumb" loading="lazy" />`
                    )
                    .join("")}
                </div>`
              : ""
          }

          ${
            m.skills
              ? `<div style="font-size:0.84rem;color:var(--text-primary);"><strong>Skills:</strong> ${escapeHtml(m.skills)}</div>`
              : ""
          }
          ${
            m.languages
              ? `<div style="font-size:0.84rem;color:var(--text-primary);"><strong>Languages:</strong> ${escapeHtml(m.languages)}</div>`
              : ""
          }
          ${
            m.bio
              ? `<div style="font-size:0.84rem;color:var(--text-secondary);"><strong>Introduction:</strong> ${escapeHtml(m.bio)}</div>`
              : ""
          }
          ${
            m.portfolioUrls
              ? `<div style="font-size:0.84rem;color:var(--text-secondary);overflow-wrap:anywhere;">
                  <strong>Portfolio:</strong>
                  ${
                    portfolioHref
                      ? `<a href="${escapeHtml(portfolioHref)}" target="_blank" rel="noopener noreferrer" style="color:var(--gold);text-decoration:underline;">${escapeHtml(portfolioHref)}</a>`
                      : escapeHtml(m.portfolioUrls)
                  }
                </div>`
              : ""
          }
          ${
            introVideoHref
              ? `<div style="font-size:0.84rem;color:var(--text-secondary);overflow-wrap:anywhere;">
                  <strong>Intro Video:</strong>
                  <a href="${escapeHtml(introVideoHref)}" target="_blank" rel="noopener noreferrer" style="color:var(--gold);text-decoration:underline;">${escapeHtml(introVideoHref)}</a>
                </div>`
              : ""
          }

          ${
            st !== "approved"
              ? `
            <label class="checkbox-row admin-checklist-row">
              <input type="checkbox" data-adm-model-check="${escapeHtml(m.id)}" ${checked ? "checked" : ""} />
              <span>${escapeHtml(t("adminVerifyChecklistLabel"))}</span>
            </label>`
              : `<div class="roster-verified-banner">★ ${escapeHtml(isDisabled ? t("modelStatusDisabled") : t("rosterBadge"))}</div>`
          }

          <div class="field">
            <label for="admModelNote_${escapeHtml(m.id)}">${escapeHtml(t("adminNotesLabel"))}</label>
            <textarea
              id="admModelNote_${escapeHtml(m.id)}"
              class="input"
              rows="2"
              maxlength="1000"
              data-adm-model-notes="${escapeHtml(m.id)}"
              placeholder="${escapeHtml(t("adminNotesPlaceholder"))}"
            >${escapeHtml(noteValue)}</textarea>
          </div>

          <div class="admin-quick-reasons">
            ${quickNotes
              .map(
                (noteText) =>
                  `<button type="button" class="booking-status-pill" data-adm-model-quicknote="${escapeHtml(m.id)}" data-adm-quicknote-val="${escapeHtml(noteText)}">${escapeHtml(noteText)}</button>`
              )
              .join("")}
          </div>

          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button
              type="button"
              class="btn-gold"
              style="min-height:44px;padding:8px 14px;flex:1;"
              data-adm-mdl-id="${escapeHtml(m.id)}"
              data-adm-mdl-st="approved"
            >${st === "approved" && !isDisabled ? "Official Roster ✓" : escapeHtml(t("adminVerifyRosterBtn"))}</button>
            <button
              type="button"
              class="btn-danger"
              style="min-height:44px;padding:8px 14px;"
              data-adm-mdl-id="${escapeHtml(m.id)}"
              data-adm-mdl-st="rejected"
            >${escapeHtml(t("adminRejectModelBtn"))}</button>
            ${
              st !== "pending"
                ? `<button type="button" class="chip-btn" data-adm-mdl-id="${escapeHtml(m.id)}" data-adm-mdl-st="pending">${escapeHtml(t("adminMovePendingBtn"))}</button>`
                : ""
            }
            <button
              type="button"
              class="btn-outline btn-sm"
              data-adm-mdl-disable="${escapeHtml(m.id)}"
              data-adm-mdl-disabled-next="${isDisabled ? "false" : "true"}"
            >${escapeHtml(isDisabled ? t("adminEnableModelBtn") : t("adminDisableModelBtn"))}</button>
            <button
              type="button"
              class="btn-danger btn-sm"
              data-adm-mdl-delete="${escapeHtml(m.id)}"
              data-adm-mdl-delete-name="${escapeHtml(m.fullName)}"
            >${escapeHtml(t("adminDeleteModelBtn"))}</button>
          </div>
        </article>`;
        })
        .join("")
    : `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">No model applications in this verification view.</p></div>`;

  return `
    <div class="studio-card admin-verification-banner">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
        <div>
          <strong style="color:var(--gold);display:block;">${escapeHtml(t("adminModelsBannerTitle"))}</strong>
          <span style="font-size:0.84rem;color:var(--text-secondary);">${escapeHtml(t("adminModelsBannerDesc"))}</span>
        </div>
        <span class="badge status-approved">${counts.roster} Official Roster</span>
      </div>
    </div>
    ${renderModelExperienceOverviewCardHtml(models, { t, escapeHtml })}
    <section id="adminModelExperienceCard" class="studio-card model-experience-card" aria-label="${escapeHtml(t("adminExpChartTitle"))}">
      <div class="model-experience-header">
        <div>
          <span class="badge status-confirmed">${escapeHtml(t("adminExpBadge"))}</span>
          <h3 class="model-experience-title">${escapeHtml(t("adminExpChartTitle"))}</h3>
          <p class="model-experience-subtitle">${escapeHtml(t("adminExpChartSubtitle"))}</p>
        </div>
        <div class="model-experience-controls" role="group" aria-label="${escapeHtml(t("adminExpScopeGroupLabel"))}">
          <button
            type="button"
            class="chip-btn ${activeExpScope === "all" ? "active" : ""}"
            data-exp-chart-scope="all"
            aria-pressed="${activeExpScope === "all" ? "true" : "false"}"
          >${escapeHtml(t("adminExpScopeAll"))} (${counts.total})</button>
          <button
            type="button"
            class="chip-btn ${activeExpScope === "approved" ? "active" : ""}"
            data-exp-chart-scope="approved"
            aria-pressed="${activeExpScope === "approved" ? "true" : "false"}"
          >${escapeHtml(t("adminExpScopeApproved"))} (${counts.roster})</button>
        </div>
      </div>
      <div class="model-experience-layout">
        <div class="model-experience-chart-col">
          <div id="adminModelExperienceChartMount" class="model-experience-chart-host" aria-live="polite"></div>
          <div class="model-experience-legend">
            <span class="model-experience-legend-item">
              <span class="model-experience-dot is-fresher"></span>
              <span>${escapeHtml(t("adminExpFresherLabel"))}</span>
            </span>
            <span class="model-experience-legend-item">
              <span class="model-experience-dot is-experienced"></span>
              <span>${escapeHtml(t("adminExpExperiencedLabel"))}</span>
            </span>
          </div>
        </div>
        <div id="adminModelExperienceSummary" class="model-experience-summary"></div>
      </div>
      <div id="adminModelExperienceStatus" class="model-experience-status" aria-live="polite"></div>
    </section>
    <div class="filter-bar" style="margin-bottom:4px;">
      ${filterPills}
      <input
        type="search"
        class="input"
        style="max-width:300px;"
        data-adm-model-search
        value="${escapeHtml(searchQuery)}"
        placeholder="${escapeHtml(t("adminModelSearchPlaceholder"))}"
      />
    </div>
    <div id="adminModelBulkBar" class="studio-card admin-bulk-bar ${totalSelectedCount > 0 ? "has-selection" : ""}" role="region" aria-label="${escapeHtml(t("adminBulkBarLabel"))}">
      <div class="admin-bulk-bar-top">
        <label class="admin-bulk-select-all-label">
          <input
            type="checkbox"
            class="admin-bulk-checkbox"
            data-adm-model-select-all="true"
            ${allVisibleSelected ? "checked" : ""}
            ${visible.length === 0 || bulkBusy ? "disabled" : ""}
          />
          <span>${escapeHtml(t("adminBulkSelectAllVisible"))} (${visible.length})</span>
        </label>
        <div class="admin-bulk-count-wrap">
          <span class="badge ${totalSelectedCount > 0 ? "status-confirmed" : ""}" data-adm-model-selected-count="${totalSelectedCount}">
            ${totalSelectedCount} ${escapeHtml(t("adminBulkSelectedSuffix"))}
          </span>
          ${
            totalSelectedCount > 0
              ? `<button type="button" class="chip-btn" data-adm-model-bulk-clear="true" ${bulkBusy ? "disabled" : ""}>${escapeHtml(t("adminBulkClearBtn"))}</button>`
              : ""
          }
        </div>
      </div>
      <div class="admin-bulk-controls">
        <input
          type="text"
          class="input admin-bulk-note-input"
          maxlength="1000"
          data-adm-model-bulk-note="true"
          value="${escapeHtml(bulkNote)}"
          placeholder="${escapeHtml(t("adminBulkNotePlaceholder"))}"
          ${bulkBusy ? "disabled" : ""}
        />
        <div class="admin-bulk-action-btns">
          <button
            type="button"
            class="btn-gold admin-bulk-btn"
            data-adm-model-bulk-action="approved"
            ${totalSelectedCount === 0 || bulkBusy ? "disabled" : ""}
          >${escapeHtml(bulkBusy ? t("adminBulkProcessing") : t("adminBulkApproveBtn"))}${totalSelectedCount > 0 && !bulkBusy ? ` (${totalSelectedCount})` : ""}</button>
          <button
            type="button"
            class="btn-danger admin-bulk-btn"
            data-adm-model-bulk-action="rejected"
            ${totalSelectedCount === 0 || bulkBusy ? "disabled" : ""}
          >${escapeHtml(bulkBusy ? t("adminBulkProcessing") : t("adminBulkRejectBtn"))}${totalSelectedCount > 0 && !bulkBusy ? ` (${totalSelectedCount})` : ""}</button>
        </div>
      </div>
    </div>
    <div style="display:flex;flex-direction:column;gap:12px;">
      ${cardsHtml}
    </div>`;
}
