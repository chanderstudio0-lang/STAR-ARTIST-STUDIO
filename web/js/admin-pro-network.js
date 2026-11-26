import {
  HIRING_STATUSES,
  OUTSOURCING_STATUSES,
  formatNetworkTimestamp,
  getProStatusBadgeClass,
  parseSafeLinksList
} from "./pro-network.js";

function defaultEscapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

/**
 * Admin Tab 1: Professional Registrations (Approve, Reject, Suspend, Reactivate)
 */
export function renderAdminProRegistrationsHtml({
  professionals = [],
  filter = "all",
  search = "",
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const list = Array.isArray(professionals) ? professionals : [];
  const q = String(search || "").trim().toLowerCase();
  const visible = list.filter((p) => {
    const st = String(p.status || "pending").toLowerCase();
    if (filter !== "all" && st !== filter) return false;
    if (!q) return true;
    return `${p.fullName || ""} ${p.city || ""} ${p.state || ""} ${p.category || ""} ${p.mobileNumber || ""} ${p.email || ""}`
      .toLowerCase()
      .includes(q);
  });

  const cardsHtml = visible.length
    ? visible
        .map((p) => {
          const st = String(p.status || "pending").toLowerCase();
          const links = parseSafeLinksList(p.videoLinks || "");
          return `
        <article class="studio-card pro-record-card" data-admin-pro-id="${escapeHtml(p.id)}">
          <div class="pro-card-head">
            <div>
              <strong>${escapeHtml(p.fullName)} — ${escapeHtml(p.category)}</strong>
              <div class="pro-card-sub">${escapeHtml(p.city)}, ${escapeHtml(p.state)} • ${escapeHtml(String(p.yearsExperience || 0))} ${escapeHtml(t("proYearsShort"))} • ${escapeHtml(p.availabilityStatus || "Available")}</div>
            </div>
            <span class="badge ${getProStatusBadgeClass(st)}">${escapeHtml(st.toUpperCase())}</span>
          </div>
          <div class="pro-card-details">
            <div><strong>${escapeHtml(t("proPrivateContactLabel"))}:</strong> 📞 ${escapeHtml(p.mobileNumber)} • ✉ ${escapeHtml(p.email)}</div>
            <div><strong>${escapeHtml(t("proSkillsLabel"))}:</strong> ${escapeHtml(p.skills || "")}</div>
            <div><strong>${escapeHtml(t("proEquipmentLabel"))}:</strong> ${escapeHtml(p.equipmentDetails || "")}</div>
            <div><strong>${escapeHtml(t("proLocationsLabel"))}:</strong> ${escapeHtml(p.workingLocations || "")}</div>
            ${p.expectedCharges ? `<div><strong>${escapeHtml(t("proChargesLabel"))}:</strong> ${escapeHtml(p.expectedCharges)}</div>` : ""}
            <div><strong>${escapeHtml(t("proIntroLabel"))}:</strong> ${escapeHtml(p.shortIntro || "")}</div>
            <div><strong>${escapeHtml(t("proSubmittedAtLabel"))}:</strong> ${escapeHtml(formatNetworkTimestamp(p.createdAt, lang))}</div>
          </div>
          ${
            links.length
              ? `<div class="pro-links-row">${links
                  .map((url, idx) => `<a class="chip-btn" href="${escapeHtml(url)}" target="_blank" rel="noopener noreferrer">${escapeHtml(t("proPortfolioLinkBtn"))} ${idx + 1} ↗</a>`)
                  .join("")}</div>`
              : ""
          }
          <input type="text" class="input" data-admin-pro-note="${escapeHtml(p.id)}" value="${escapeHtml(p.adminNotes || "")}" placeholder="${escapeHtml(t("adminProNotePlaceholder"))}" />
          <div class="pro-card-actions">
            ${st !== "approved" ? `<button type="button" class="btn-gold btn-sm" data-admin-pro-status="approved" data-pro-id="${escapeHtml(p.id)}">✓ ${escapeHtml(st === "suspended" ? t("adminProReactivateBtn") : t("adminProApproveBtn"))}</button>` : ""}
            ${st !== "rejected" ? `<button type="button" class="chip-btn" data-admin-pro-status="rejected" data-pro-id="${escapeHtml(p.id)}">✕ ${escapeHtml(t("adminProRejectBtn"))}</button>` : ""}
            ${st !== "suspended" ? `<button type="button" class="chip-btn" data-admin-pro-status="suspended" data-pro-id="${escapeHtml(p.id)}">⏸ ${escapeHtml(t("adminProSuspendBtn"))}</button>` : ""}
            <button
              type="button"
              class="chip-btn"
              data-admin-start-chat-uid="${escapeHtml(p.userId)}"
              data-admin-start-chat-name="${escapeHtml(p.fullName)}"
              data-admin-start-chat-email="${escapeHtml(p.email || "")}"
              data-admin-start-chat-role="professional"
              data-admin-start-chat-ctx-type="professional"
              data-admin-start-chat-ctx-id="${escapeHtml(p.id)}"
              data-admin-start-chat-ctx-label="${escapeHtml(`${t("proTabJoin")}: ${p.fullName}`)}"
            >💬 ${escapeHtml(t("adminMessageUserBtn"))}</button>
          </div>
        </article>`;
        })
        .join("")
    : `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("adminProEmpty"))}</p></div>`;

  return `
    <div class="pro-section-stack">
      <div class="studio-card">
        <strong style="color:var(--gold);">${escapeHtml(t("adminTabProRegistrations"))}</strong>
        <p style="margin:4px 0 10px;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("adminProBannerDesc"))}</p>
        <div class="filter-bar" style="margin:0 0 10px;">
          ${["all", "pending", "approved", "rejected", "suspended"]
            .map(
              (f) =>
                `<button type="button" class="chip-btn ${filter === f ? "active" : ""}" data-admin-pro-filter="${f}">${escapeHtml(f.toUpperCase())}</button>`
            )
            .join("")}
        </div>
        <input type="search" class="input" data-admin-pro-search="true" value="${escapeHtml(search)}" placeholder="${escapeHtml(t("adminProSearchPlaceholder"))}" />
      </div>
      ${cardsHtml}
    </div>`;
}

/**
 * Admin Tab 2: Photographer Hiring Requests (Assign Approved Pro, Propose, Confirm, Update Status)
 */
export function renderAdminHiringRequestsHtml({
  hiringRequests = [],
  approvedProfessionals = [],
  filter = "all",
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const list = Array.isArray(hiringRequests) ? hiringRequests : [];
  const approved = Array.isArray(approvedProfessionals) ? approvedProfessionals : [];
  const visible = filter === "all" ? list : list.filter((r) => r.status === filter);

  const cardsHtml = visible.length
    ? visible
        .map((req) => {
          const proOptions = [
            `<option value="">${escapeHtml(t("adminSelectApprovedProPlaceholder"))}</option>`,
            ...approved.map(
              (p) =>
                `<option value="${escapeHtml(p.id)}" ${req.assignedProfessionalId === p.id ? "selected" : ""}>${escapeHtml(p.fullName)} — ${escapeHtml(p.category)} (${escapeHtml(p.city)})</option>`
            )
          ].join("");

          const statusOptions = HIRING_STATUSES.map(
            (st) => `<option value="${escapeHtml(st)}" ${req.status === st ? "selected" : ""}>${escapeHtml(st)}</option>`
          ).join("");

          return `
        <article class="studio-card pro-record-card" data-admin-hiring-id="${escapeHtml(req.id)}">
          <div class="pro-card-head">
            <div>
              <strong>${escapeHtml(req.customerName)} — ${escapeHtml(req.eventType)} (${escapeHtml(req.requiredService)})</strong>
              <div class="pro-card-sub">📞 ${escapeHtml(req.contactNumber)} • ${escapeHtml(req.eventDate)} ${escapeHtml(req.eventTime)} • ${escapeHtml(req.eventLocation)}</div>
            </div>
            <span class="badge ${getProStatusBadgeClass(req.status)}">${escapeHtml(req.status)}</span>
          </div>
          <div class="pro-card-details">
            <div><strong>${escapeHtml(t("proHireDurationLabel"))}:</strong> ${escapeHtml(req.workingDuration)} • <strong>${escapeHtml(t("proBudgetLabel"))}:</strong> ${escapeHtml(req.budgetRange)}</div>
            ${req.specialRequirements ? `<div><strong>${escapeHtml(t("proHireRequirementsLabel"))}:</strong> ${escapeHtml(req.specialRequirements)}</div>` : ""}
            <div><strong>${escapeHtml(t("proCustomerAcceptanceLabel"))}:</strong> <span class="badge">${escapeHtml(req.customerAcceptance || "pending")}</span> ${req.customerResponseNote ? `— ${escapeHtml(req.customerResponseNote)}` : ""}</div>
            ${req.assignedProfessionalName ? `<div><strong>${escapeHtml(t("proAssignedProLabel"))}:</strong> ${escapeHtml(req.assignedProfessionalName)} (${escapeHtml(req.assignedProfessionalCategory || "")})</div>` : ""}
            <div><strong>${escapeHtml(t("proSubmittedAtLabel"))}:</strong> ${escapeHtml(formatNetworkTimestamp(req.createdAt, lang))}</div>
          </div>

          <div class="admin-assign-grid">
            <select class="input" data-admin-hiring-pro-select="${escapeHtml(req.id)}">${proOptions}</select>
            <input type="text" class="input" data-admin-hiring-proposal-msg="${escapeHtml(req.id)}" value="${escapeHtml(req.proposalMessage || "")}" placeholder="${escapeHtml(t("adminProposalMsgPlaceholder"))}" />
            <button type="button" class="btn-gold btn-sm" data-admin-propose-hiring="${escapeHtml(req.id)}">${escapeHtml(t("adminSendProposalBtn"))}</button>
          </div>

          <div class="admin-status-row">
            <input type="text" class="input" data-admin-hiring-notes="${escapeHtml(req.id)}" value="${escapeHtml(req.adminNotes || "")}" placeholder="${escapeHtml(t("adminProNotePlaceholder"))}" />
            <select class="input" data-admin-hiring-status-select="${escapeHtml(req.id)}">${statusOptions}</select>
            <button type="button" class="chip-btn" data-admin-hiring-save-status="${escapeHtml(req.id)}">${escapeHtml(t("adminUpdateStatusBtn"))}</button>
          </div>

          <div class="pro-card-actions">
            ${
              req.customerAcceptance === "accepted" && req.status !== "Confirmed"
                ? `<button type="button" class="btn-gold btn-sm" data-admin-hiring-quick-status="Confirmed" data-hiring-id="${escapeHtml(req.id)}">✓ ${escapeHtml(t("adminFinalConfirmBookingBtn"))}</button>`
                : ""
            }
            <button type="button" class="chip-btn" data-admin-hiring-quick-status="Completed" data-hiring-id="${escapeHtml(req.id)}">${escapeHtml(t("statusCompleted"))}</button>
            <button type="button" class="chip-btn" data-admin-hiring-quick-status="Cancelled" data-hiring-id="${escapeHtml(req.id)}">${escapeHtml(t("statusCancelled"))}</button>
            <button
              type="button"
              class="chip-btn"
              data-admin-start-chat-uid="${escapeHtml(req.userId)}"
              data-admin-start-chat-name="${escapeHtml(req.customerName)}"
              data-admin-start-chat-email=""
              data-admin-start-chat-role="customer"
              data-admin-start-chat-ctx-type="hiring"
              data-admin-start-chat-ctx-id="${escapeHtml(req.id)}"
              data-admin-start-chat-ctx-label="${escapeHtml(`${req.eventType} (${req.eventDate})`)}"
            >💬 ${escapeHtml(t("adminMessageCustomerBtn"))}</button>
          </div>
        </article>`;
        })
        .join("")
    : `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("adminHiringEmpty"))}</p></div>`;

  return `
    <div class="pro-section-stack">
      <div class="studio-card">
        <strong style="color:var(--gold);">${escapeHtml(t("adminTabProHiring"))}</strong>
        <p style="margin:4px 0 10px;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("adminHiringBannerDesc"))}</p>
        <div class="filter-bar" style="margin:0;">
          ${["all", "Pending Admin Review", "Assignment Proposed", "Customer Accepted", "Confirmed", "In Progress", "Completed"]
            .map(
              (f) =>
                `<button type="button" class="chip-btn ${filter === f ? "active" : ""}" data-admin-hiring-filter="${escapeHtml(f)}">${escapeHtml(f === "all" ? t("filterAll") : f)}</button>`
            )
            .join("")}
        </div>
      </div>
      ${cardsHtml}
    </div>`;
}

/**
 * Admin Tab 3: Outsourcing Requests
 */
export function renderAdminOutsourcingRequestsHtml({
  outsourcingRequests = [],
  approvedProfessionals = [],
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const list = Array.isArray(outsourcingRequests) ? outsourcingRequests : [];
  const approved = Array.isArray(approvedProfessionals) ? approvedProfessionals : [];

  if (!list.length) {
    return `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("adminOutsourcingEmpty"))}</p></div>`;
  }

  return `<div class="pro-section-stack">
    ${list
      .map((req) => {
        const proOptions = [
          `<option value="">${escapeHtml(t("adminSelectApprovedProPlaceholder"))}</option>`,
          ...approved.map(
            (p) =>
              `<option value="${escapeHtml(p.id)}" ${req.assignedProfessionalId === p.id ? "selected" : ""}>${escapeHtml(p.fullName)} — ${escapeHtml(p.category)} (${escapeHtml(p.city)})</option>`
          )
        ].join("");
        const statusOptions = OUTSOURCING_STATUSES.map(
          (st) => `<option value="${escapeHtml(st)}" ${req.status === st ? "selected" : ""}>${escapeHtml(st)}</option>`
        ).join("");
        return `
      <article class="studio-card pro-record-card" data-admin-outsourcing-id="${escapeHtml(req.id)}">
        <div class="pro-card-head">
          <div>
            <strong>${escapeHtml(req.studioOrName)} — ${escapeHtml(req.workType)}</strong>
            <div class="pro-card-sub">${escapeHtml(req.city)} • ${escapeHtml(req.contactInfo)} • ${escapeHtml(t("proOutDeadlineLabel"))}: ${escapeHtml(req.workDeadline)}</div>
          </div>
          <span class="badge ${getProStatusBadgeClass(req.status)}">${escapeHtml(req.status)}</span>
        </div>
        <p class="pro-card-intro">${escapeHtml(req.projectDescription || "")}</p>
        <div class="pro-card-details">
          ${req.budget ? `<div><strong>${escapeHtml(t("proBudgetLabel"))}:</strong> ${escapeHtml(req.budget)}</div>` : ""}
          ${req.additionalMessage ? `<div><strong>${escapeHtml(t("proOutMsgLabel"))}:</strong> ${escapeHtml(req.additionalMessage)}</div>` : ""}
          ${req.assignedProfessionalName ? `<div><strong>${escapeHtml(t("proAssignedProLabel"))}:</strong> ${escapeHtml(req.assignedProfessionalName)}</div>` : ""}
          <div><strong>${escapeHtml(t("proSubmittedAtLabel"))}:</strong> ${escapeHtml(formatNetworkTimestamp(req.createdAt, lang))}</div>
        </div>
        <div class="admin-assign-grid">
          <select class="input" data-admin-out-pro-select="${escapeHtml(req.id)}">${proOptions}</select>
          <select class="input" data-admin-out-status-select="${escapeHtml(req.id)}">${statusOptions}</select>
          <button type="button" class="btn-gold btn-sm" data-admin-out-save="${escapeHtml(req.id)}">${escapeHtml(t("adminUpdateStatusBtn"))}</button>
        </div>
        <input type="text" class="input" data-admin-out-notes="${escapeHtml(req.id)}" value="${escapeHtml(req.adminNotes || "")}" placeholder="${escapeHtml(t("adminProNotePlaceholder"))}" />
        <div class="pro-card-actions">
          <button type="button" class="chip-btn" data-admin-out-quick-status="Approved" data-out-id="${escapeHtml(req.id)}">✓ ${escapeHtml(t("adminProApproveBtn"))}</button>
          <button type="button" class="chip-btn" data-admin-out-quick-status="Rejected" data-out-id="${escapeHtml(req.id)}">✕ ${escapeHtml(t("adminProRejectBtn"))}</button>
          <button
            type="button"
            class="chip-btn"
            data-admin-start-chat-uid="${escapeHtml(req.userId)}"
            data-admin-start-chat-name="${escapeHtml(req.studioOrName)}"
            data-admin-start-chat-email=""
            data-admin-start-chat-role="professional"
            data-admin-start-chat-ctx-type="outsourcing"
            data-admin-start-chat-ctx-id="${escapeHtml(req.id)}"
            data-admin-start-chat-ctx-label="${escapeHtml(`${t("proTabOutsourcing")}: ${req.workType}`)}"
          >💬 ${escapeHtml(t("adminMessageUserBtn"))}</button>
        </div>
      </article>`;
      })
      .join("")}
  </div>`;
}

/**
 * Admin Tab 4: Professional Assignments (All active & proposed assignments across Hiring & Outsourcing)
 */
export function renderAdminAssignmentsHtml({
  hiringRequests = [],
  outsourcingRequests = [],
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const assignedHiring = (hiringRequests || [])
    .filter((r) => r.assignedProfessionalId || r.assignedProfessionalName)
    .map((r) => ({
      kind: "Hiring",
      id: r.id,
      title: `${r.customerName} — ${r.eventType} (${r.requiredService})`,
      proName: r.assignedProfessionalName || "Assigned Professional",
      proUid: r.assignedProfessionalUid || "",
      date: `${r.eventDate} ${r.eventTime || ""}`.trim(),
      location: r.eventLocation || "",
      status: r.status,
      acceptance: r.customerAcceptance || "pending",
      updatedAt: r.updatedAt || r.createdAt
    }));
  const assignedOutsourcing = (outsourcingRequests || [])
    .filter((o) => o.assignedProfessionalId || o.assignedProfessionalName)
    .map((o) => ({
      kind: "Outsourcing",
      id: o.id,
      title: `${o.studioOrName} — ${o.workType}`,
      proName: o.assignedProfessionalName || "Assigned Professional",
      proUid: o.assignedProfessionalUid || "",
      date: o.workDeadline || "",
      location: o.city || "",
      status: o.status,
      acceptance: "admin-assigned",
      updatedAt: o.updatedAt || o.createdAt
    }));
  const combined = [...assignedHiring, ...assignedOutsourcing];

  if (!combined.length) {
    return `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("adminAssignmentsEmpty"))}</p></div>`;
  }

  return `<div class="pro-section-stack">
    ${combined
      .map(
        (item) => `
      <article class="studio-card pro-record-card">
        <div class="pro-card-head">
          <div>
            <span class="badge">${escapeHtml(item.kind)}</span>
            <strong style="margin-left:6px;">${escapeHtml(item.title)}</strong>
            <div class="pro-card-sub">${escapeHtml(t("proAssignedProLabel"))}: <strong>${escapeHtml(item.proName)}</strong> • ${escapeHtml(item.date)} • ${escapeHtml(item.location)}</div>
          </div>
          <span class="badge ${getProStatusBadgeClass(item.status)}">${escapeHtml(item.status)}</span>
        </div>
        <div class="pro-card-details">
          <div><strong>${escapeHtml(t("proCustomerAcceptanceLabel"))}:</strong> ${escapeHtml(item.acceptance)} • <strong>Updated:</strong> ${escapeHtml(formatNetworkTimestamp(item.updatedAt, lang))}</div>
        </div>
        ${
          item.proUid
            ? `<div class="pro-card-actions">
                <button
                  type="button"
                  class="chip-btn"
                  data-admin-start-chat-uid="${escapeHtml(item.proUid)}"
                  data-admin-start-chat-name="${escapeHtml(item.proName)}"
                  data-admin-start-chat-email=""
                  data-admin-start-chat-role="professional"
                  data-admin-start-chat-ctx-type="${item.kind === "Hiring" ? "hiring" : "outsourcing"}"
                  data-admin-start-chat-ctx-id="${escapeHtml(item.id)}"
                  data-admin-start-chat-ctx-label="${escapeHtml(`${item.kind}: ${item.title}`)}"
                >💬 ${escapeHtml(t("adminMessageProBtn"))}</button>
              </div>`
            : ""
        }
      </article>`
      )
      .join("")}
  </div>`;
}

/**
 * Admin Tab 6: Booking History (Unified chronological audit trail with timestamps)
 */
export function renderAdminBookingHistoryHtml({
  hiringRequests = [],
  outsourcingRequests = [],
  studioBookings = [],
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const rows = [
    ...(hiringRequests || []).map((r) => ({
      category: t("adminHistoryTypeHiring"),
      title: `${r.customerName} — ${r.eventType} (${r.requiredService})`,
      sub: `${r.eventDate} • ${r.eventLocation}${r.assignedProfessionalName ? ` • Pro: ${r.assignedProfessionalName}` : ""}`,
      status: r.status,
      ts: r.updatedAt || r.createdAt
    })),
    ...(outsourcingRequests || []).map((o) => ({
      category: t("adminHistoryTypeOutsourcing"),
      title: `${o.studioOrName} — ${o.workType}`,
      sub: `${o.city} • Deadline: ${o.workDeadline}${o.assignedProfessionalName ? ` • Pro: ${o.assignedProfessionalName}` : ""}`,
      status: o.status,
      ts: o.updatedAt || o.createdAt
    })),
    ...(studioBookings || []).map((b) => ({
      category: t("adminHistoryTypeStudio"),
      title: `${b.clientName} — ${b.serviceType}`,
      sub: `${b.preferredDate} ${b.preferredTime || ""}`.trim(),
      status: b.status || "Pending",
      ts: b.updatedAt || b.createdAt
    }))
  ].sort((a, b) => (b.ts?.seconds || 0) - (a.ts?.seconds || 0));

  if (!rows.length) {
    return `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("adminHistoryEmpty"))}</p></div>`;
  }

  return `<div class="pro-section-stack">
    ${rows
      .map(
        (row) => `
      <article class="studio-card pro-record-card">
        <div class="pro-card-head">
          <div>
            <span class="badge">${escapeHtml(row.category)}</span>
            <strong style="margin-left:6px;">${escapeHtml(row.title)}</strong>
            <div class="pro-card-sub">${escapeHtml(row.sub)}</div>
          </div>
          <span class="badge ${getProStatusBadgeClass(row.status)}">${escapeHtml(row.status)}</span>
        </div>
        <div style="font-size:0.78rem;color:var(--text-muted);">${escapeHtml(formatNetworkTimestamp(row.ts, lang))}</div>
      </article>`
      )
      .join("")}
  </div>`;
}
