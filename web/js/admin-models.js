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

export function renderAdminModelsSectionHtml({
  models = [],
  filter = "pending",
  searchQuery = "",
  drafts = {},
  lang = "en",
  t = (k) => k
}) {
  const counts = summarizeModelApplications(models);
  const visible = filterModelApplications(models, filter, searchQuery);
  const isHindi = lang === "hi";

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

          return `
        <article class="studio-card admin-model-card ${cardCls}" data-adm-model-card="${escapeHtml(m.id)}">
          <div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:8px;align-items:flex-start;">
            <div>
              <strong style="font-size:1.02rem;color:var(--gold-light);">${escapeHtml(m.fullName)}</strong>
              <div style="font-size:0.84rem;color:var(--text-secondary);">
                ${escapeHtml(m.gender || "Model")} • ${escapeHtml(m.city || "")}${m.state && !String(m.city || "").toLowerCase().includes(String(m.state).toLowerCase()) ? `, ${escapeHtml(m.state)}` : ""} • ${escapeHtml(m.phoneNumber || "")}
                ${m.contactEmail ? ` • ${escapeHtml(m.contactEmail)}` : ""}
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
    <div style="display:flex;flex-direction:column;gap:12px;">
      ${cardsHtml}
    </div>`;
}
