import { safePortalHttpUrl, sanitizePortfolioPhotoList } from "./model-portal.js";

export const PROFESSIONAL_CATEGORIES = [
  "Photographer",
  "Videographer",
  "Editor",
  "Camera Operator",
  "Drone Operator",
  "Hybrid (Photo + Video)"
];

export const PROFESSIONAL_AVAILABILITY_OPTIONS = [
  "Available",
  "Limited Availability",
  "Busy / Booked"
];

export const HIRING_EVENT_TYPES = [
  "Wedding",
  "Pre-wedding",
  "Birthday",
  "Party",
  "Corporate",
  "Music Video",
  "Modeling",
  "Other"
];

export const HIRING_SERVICE_TYPES = [
  "Photography",
  "Videography",
  "Both",
  "Drone",
  "Editing"
];

export const HIRING_STATUSES = [
  "Pending Admin Review",
  "Assignment Proposed",
  "Customer Accepted",
  "Proposal Declined",
  "Confirmed",
  "Assigned",
  "In Progress",
  "Completed",
  "Cancelled",
  "Rejected"
];

export const OUTSOURCING_WORK_TYPES = [
  "Photography",
  "Videography",
  "Video Editing",
  "Album Design",
  "Drone Shooting",
  "Full Production",
  "Other"
];

export const OUTSOURCING_STATUSES = [
  "Pending Admin Review",
  "Approved",
  "Assigned",
  "In Progress",
  "Completed",
  "Rejected",
  "Cancelled"
];

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_DIGITS_RE = /[^0-9+]/g;

function defaultEscapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function sanitizeLinksField(raw) {
  const lines = String(raw || "")
    .split(/[\n,]+/)
    .map((s) => s.trim())
    .filter(Boolean);
  const valid = [];
  for (const item of lines) {
    const safe = safePortalHttpUrl(item);
    if (safe && !valid.includes(safe)) {
      valid.push(safe);
    }
    if (valid.length >= 6) break;
  }
  return valid.join("\n");
}

export function parseSafeLinksList(raw) {
  return sanitizeLinksField(raw)
    .split("\n")
    .map((s) => s.trim())
    .filter(Boolean);
}

export function formatNetworkTimestamp(value, lang = "en") {
  if (!value) return lang === "hi" ? "अभी" : "Just now";
  let date = null;
  if (value instanceof Date) date = value;
  else if (typeof value === "object" && typeof value.toDate === "function") {
    try {
      date = value.toDate();
    } catch {
      date = null;
    }
  } else if (typeof value === "object" && typeof value.seconds === "number") {
    date = new Date(value.seconds * 1000);
  } else if (typeof value === "string" || typeof value === "number") {
    date = new Date(value);
  }
  if (!date || Number.isNaN(date.getTime())) return lang === "hi" ? "अभी" : "Just now";
  try {
    return new Intl.DateTimeFormat(lang === "hi" ? "hi-IN" : "en-IN", {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: "Asia/Kolkata"
    }).format(date);
  } catch {
    return date.toISOString();
  }
}

/**
 * Validates "Join as a Professional" registration inputs.
 */
export function validateProfessionalRegistration(payload = {}) {
  const fullName = String(payload.fullName || "").trim();
  const mobileNumber = String(payload.mobileNumber || "").replace(PHONE_DIGITS_RE, "").trim();
  const email = String(payload.email || "").trim().toLowerCase();
  const city = String(payload.city || "").trim();
  const state = String(payload.state || "").trim();
  const category = PROFESSIONAL_CATEGORIES.includes(payload.category)
    ? payload.category
    : "Photographer";
  const yearsExperience = Number.parseInt(String(payload.yearsExperience ?? "0"), 10);
  const skills = String(payload.skills || "").trim();
  const equipmentDetails = String(payload.equipmentDetails || "").trim();
  const workingLocations = String(payload.workingLocations || "").trim();
  const expectedCharges = String(payload.expectedCharges || "").trim().slice(0, 120);
  const portfolioPhotos = sanitizePortfolioPhotoList(payload.portfolioPhotos || []).slice(0, 4);
  const videoLinks = sanitizeLinksField(payload.videoLinks || "");
  const shortIntro = String(payload.shortIntro || "").trim();
  const availabilityStatus = PROFESSIONAL_AVAILABILITY_OPTIONS.includes(payload.availabilityStatus)
    ? payload.availabilityStatus
    : "Available";

  const errors = [];
  if (fullName.length < 2 || fullName.length > 100) errors.push("Full name must be 2–100 characters.");
  if (mobileNumber.replace(/\D/g, "").length < 8 || mobileNumber.length > 20) {
    errors.push("Enter a valid mobile number (8–20 digits).");
  }
  if (!EMAIL_RE.test(email) || email.length > 128) errors.push("Enter a valid email address.");
  if (city.length < 2 || city.length > 80) errors.push("Enter your city.");
  if (state.length < 2 || state.length > 80) errors.push("Enter your state.");
  if (!Number.isInteger(yearsExperience) || yearsExperience < 0 || yearsExperience > 60) {
    errors.push("Years of experience must be between 0 and 60.");
  }
  if (skills.length < 2 || skills.length > 500) errors.push("List your skills and services.");
  if (equipmentDetails.length < 2 || equipmentDetails.length > 600) {
    errors.push("Provide your camera/editing equipment details.");
  }
  if (workingLocations.length < 2 || workingLocations.length > 300) {
    errors.push("Specify available working locations.");
  }
  if (shortIntro.length < 10 || shortIntro.length > 1000) {
    errors.push("Short introduction must be at least 10 characters.");
  }

  return {
    valid: errors.length === 0,
    errors,
    clean: {
      fullName,
      mobileNumber,
      email,
      city,
      state,
      category,
      yearsExperience: Number.isInteger(yearsExperience) ? yearsExperience : 0,
      skills,
      equipmentDetails,
      workingLocations,
      expectedCharges,
      portfolioPhotos,
      videoLinks,
      shortIntro,
      availabilityStatus
    }
  };
}

/**
 * Produces a privacy-safe directory document strictly excluding mobileNumber, email, userId, and adminNotes.
 */
export function sanitizePublicProfessionalProfile(pro = {}) {
  const id = String(pro?.professionalId || pro?.id || "").trim();
  if (!id) return null;
  if (pro.status && String(pro.status).toLowerCase() !== "approved") return null;
  return {
    professionalId: id,
    fullName: String(pro.fullName || "").trim().slice(0, 100),
    city: String(pro.city || "").trim().slice(0, 80),
    state: String(pro.state || "").trim().slice(0, 80),
    category: PROFESSIONAL_CATEGORIES.includes(pro.category) ? pro.category : "Photographer",
    yearsExperience: Number.isInteger(pro.yearsExperience)
      ? Math.max(0, Math.min(60, pro.yearsExperience))
      : 0,
    skills: String(pro.skills || "").trim().slice(0, 500),
    equipmentDetails: String(pro.equipmentDetails || "").trim().slice(0, 600),
    workingLocations: String(pro.workingLocations || "").trim().slice(0, 300),
    expectedCharges: String(pro.expectedCharges || "").trim().slice(0, 120),
    portfolioPhotos: sanitizePortfolioPhotoList(pro.portfolioPhotos || []).slice(0, 4),
    videoLinks: sanitizeLinksField(pro.videoLinks || ""),
    shortIntro: String(pro.shortIntro || "").trim().slice(0, 1000),
    availabilityStatus: PROFESSIONAL_AVAILABILITY_OPTIONS.includes(pro.availabilityStatus)
      ? pro.availabilityStatus
      : "Available",
    verifiedBadge: true
  };
}

/**
 * Validates customer "Hire a Photographer / Videographer" request inputs.
 * Always initializes status as "Pending Admin Review" and customerAcceptance as "pending".
 */
export function validateHiringRequest(payload = {}) {
  const customerName = String(payload.customerName || "").trim();
  const contactNumber = String(payload.contactNumber || "").replace(PHONE_DIGITS_RE, "").trim();
  const eventType = HIRING_EVENT_TYPES.includes(payload.eventType) ? payload.eventType : "Wedding";
  const requiredService = HIRING_SERVICE_TYPES.includes(payload.requiredService)
    ? payload.requiredService
    : "Photography";
  const eventDate = String(payload.eventDate || "").trim();
  const eventTime = String(payload.eventTime || "").trim();
  const eventLocation = String(payload.eventLocation || "").trim();
  const workingDuration = String(payload.workingDuration || "").trim();
  const budgetRange = String(payload.budgetRange || "").trim();
  const specialRequirements = String(payload.specialRequirements || "").trim().slice(0, 1500);
  const referenceLinks = sanitizeLinksField(payload.referenceLinks || "");
  const referencePhotos = sanitizePortfolioPhotoList(payload.referencePhotos || []).slice(0, 3);

  const errors = [];
  if (customerName.length < 2 || customerName.length > 100) errors.push("Customer name is required.");
  if (contactNumber.replace(/\D/g, "").length < 8 || contactNumber.length > 20) {
    errors.push("Enter a valid contact number (8–20 digits).");
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(eventDate)) errors.push("Select a valid event date.");
  if (eventTime.length < 2 || eventTime.length > 30) errors.push("Provide event start time.");
  if (eventLocation.length < 2 || eventLocation.length > 200) errors.push("Enter event location.");
  if (workingDuration.length < 1 || workingDuration.length > 80) {
    errors.push("Specify number of working hours or days.");
  }
  if (budgetRange.length < 2 || budgetRange.length > 100) errors.push("Specify your budget range.");

  return {
    valid: errors.length === 0,
    errors,
    clean: {
      customerName,
      contactNumber,
      eventType,
      requiredService,
      eventDate,
      eventTime,
      eventLocation,
      workingDuration,
      budgetRange,
      specialRequirements,
      referenceLinks,
      referencePhotos,
      status: "Pending Admin Review",
      assignedProfessionalId: "",
      assignedProfessionalUid: "",
      assignedProfessionalName: "",
      assignedProfessionalCategory: "",
      proposalMessage: "",
      customerAcceptance: "pending",
      customerResponseNote: "",
      adminNotes: ""
    }
  };
}

/**
 * Enforces customer booking workflow rules:
 * Customers can ONLY Accept or Decline an Admin-proposed assignment.
 * Customers can NEVER confirm a booking automatically or directly assign a professional.
 */
export function evaluateCustomerProposalAction({ request = null, action = "", userId = "", note = "" } = {}) {
  if (!request || !userId || request.userId !== userId) {
    return { allowed: false, error: "You can only respond to your own hiring requests." };
  }
  if (request.status !== "Assignment Proposed") {
    return {
      allowed: false,
      error: "Proposal actions are only available once Admin proposes a professional."
    };
  }
  const cleanNote = String(note || "").trim().slice(0, 500);
  if (action === "accept") {
    return {
      allowed: true,
      patch: {
        customerAcceptance: "accepted",
        customerResponseNote: cleanNote,
        status: "Customer Accepted"
      }
    };
  }
  if (action === "decline") {
    return {
      allowed: true,
      patch: {
        customerAcceptance: "declined",
        customerResponseNote: cleanNote,
        status: "Proposal Declined"
      }
    };
  }
  return { allowed: false, error: "Invalid customer proposal action." };
}

/**
 * Validates "Outsourcing" request inputs. Always starts as "Pending Admin Review".
 */
export function validateOutsourcingRequest(payload = {}) {
  const studioOrName = String(payload.studioOrName || "").trim();
  const contactInfo = String(payload.contactInfo || "").trim();
  const city = String(payload.city || "").trim();
  const workType = OUTSOURCING_WORK_TYPES.includes(payload.workType) ? payload.workType : "Video Editing";
  const projectDescription = String(payload.projectDescription || "").trim();
  const workDeadline = String(payload.workDeadline || "").trim();
  const budget = String(payload.budget || "").trim().slice(0, 100);
  const referenceLinks = sanitizeLinksField(payload.referenceLinks || "");
  const referencePhotos = sanitizePortfolioPhotoList(payload.referencePhotos || []).slice(0, 3);
  const additionalMessage = String(payload.additionalMessage || "").trim().slice(0, 1000);

  const errors = [];
  if (studioOrName.length < 2 || studioOrName.length > 120) errors.push("Name or Studio name is required.");
  if (contactInfo.length < 6 || contactInfo.length > 120) errors.push("Valid contact information is required.");
  if (city.length < 2 || city.length > 80) errors.push("City is required.");
  if (projectDescription.length < 10 || projectDescription.length > 2000) {
    errors.push("Project description must be at least 10 characters.");
  }
  if (workDeadline.length < 6 || workDeadline.length > 40) errors.push("Provide a clear work deadline.");

  return {
    valid: errors.length === 0,
    errors,
    clean: {
      studioOrName,
      contactInfo,
      city,
      workType,
      projectDescription,
      workDeadline,
      budget,
      referenceLinks,
      referencePhotos,
      additionalMessage,
      status: "Pending Admin Review",
      assignedProfessionalId: "",
      assignedProfessionalUid: "",
      assignedProfessionalName: "",
      adminNotes: ""
    }
  };
}

export function getProStatusBadgeClass(status = "") {
  const lower = String(status || "").toLowerCase();
  if (["approved", "confirmed", "completed", "customer accepted"].includes(lower)) return "status-approved";
  if (["rejected", "cancelled", "suspended", "proposal declined"].includes(lower)) return "status-rejected";
  if (["assignment proposed", "assigned", "in progress"].includes(lower)) return "status-progress";
  return "status-pending";
}

export function renderApprovedProfessionalsDirectoryHtml({
  professionals = [],
  categoryFilter = "all",
  searchQuery = "",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const q = String(searchQuery || "").trim().toLowerCase();
  const filtered = (Array.isArray(professionals) ? professionals : []).filter((p) => {
    if (categoryFilter !== "all" && p.category !== categoryFilter) return false;
    if (!q) return true;
    const hay = `${p.fullName || ""} ${p.city || ""} ${p.state || ""} ${p.category || ""} ${p.skills || ""} ${p.workingLocations || ""}`.toLowerCase();
    return hay.includes(q);
  });

  if (!filtered.length) {
    return `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("proDirEmpty"))}</p></div>`;
  }

  return `<div class="pro-directory-grid">
    ${filtered
      .map((p) => {
        const photos = sanitizePortfolioPhotoList(p.portfolioPhotos || []);
        const links = parseSafeLinksList(p.videoLinks || "");
        const cover = photos[0] || "";
        return `
        <article class="studio-card pro-directory-card" data-public-pro-id="${escapeHtml(p.professionalId || p.id || "")}">
          ${
            cover
              ? `<div class="pro-card-cover"><img src="${escapeHtml(cover)}" alt="${escapeHtml(p.fullName)}" loading="lazy" /></div>`
              : ""
          }
          <div class="pro-card-head">
            <div>
              <strong>${escapeHtml(p.fullName)}</strong>
              <div class="pro-card-sub">${escapeHtml(p.category)} • ${escapeHtml(p.city)}, ${escapeHtml(p.state)}</div>
            </div>
            <span class="badge status-approved">✓ ${escapeHtml(t("proVerifiedBadge"))}</span>
          </div>
          <div class="pro-card-chips">
            <span class="badge">${escapeHtml(String(p.yearsExperience || 0))} ${escapeHtml(t("proYearsShort"))}</span>
            <span class="badge">${escapeHtml(p.availabilityStatus || "Available")}</span>
            ${p.expectedCharges ? `<span class="badge">${escapeHtml(p.expectedCharges)}</span>` : ""}
          </div>
          <p class="pro-card-intro">${escapeHtml(p.shortIntro || "")}</p>
          <div class="pro-card-details">
            <div><strong>${escapeHtml(t("proSkillsLabel"))}:</strong> ${escapeHtml(p.skills || "")}</div>
            <div><strong>${escapeHtml(t("proEquipmentLabel"))}:</strong> ${escapeHtml(p.equipmentDetails || "")}</div>
            <div><strong>${escapeHtml(t("proLocationsLabel"))}:</strong> ${escapeHtml(p.workingLocations || "")}</div>
          </div>
          ${
            links.length
              ? `<div class="pro-links-row">${links
                  .map(
                    (url, idx) =>
                      `<a class="chip-btn" href="${escapeHtml(url)}" target="_blank" rel="noopener noreferrer">${escapeHtml(t("proPortfolioLinkBtn"))} ${idx + 1} ↗</a>`
                  )
                  .join("")}</div>`
              : ""
          }
          <div class="pro-privacy-note">🔒 ${escapeHtml(t("proPrivacyNote"))}</div>
        </article>`;
      })
      .join("")}
  </div>`;
}

export function renderMyProApplicationsHtml({
  myProfessionals = [],
  myOutsourcing = [],
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const proCards = (myProfessionals || []).length
    ? myProfessionals
        .map(
          (p) => `
      <article class="studio-card pro-record-card">
        <div class="pro-card-head">
          <div>
            <strong>${escapeHtml(p.fullName)} — ${escapeHtml(p.category)}</strong>
            <div class="pro-card-sub">${escapeHtml(p.city)}, ${escapeHtml(p.state)} • ${escapeHtml(String(p.yearsExperience || 0))} ${escapeHtml(t("proYearsShort"))}</div>
          </div>
          <span class="badge ${getProStatusBadgeClass(p.status)}">${escapeHtml(String(p.status || "pending").toUpperCase())}</span>
        </div>
        <div class="pro-card-details">
          <div><strong>${escapeHtml(t("proAvailabilityLabel"))}:</strong> ${escapeHtml(p.availabilityStatus || "Available")}</div>
          <div><strong>${escapeHtml(t("proSkillsLabel"))}:</strong> ${escapeHtml(p.skills || "")}</div>
          <div><strong>${escapeHtml(t("proSubmittedAtLabel"))}:</strong> ${escapeHtml(formatNetworkTimestamp(p.createdAt, lang))}</div>
          ${p.adminNotes ? `<div class="pro-admin-note-box"><strong>${escapeHtml(t("proAdminNoteLabel"))}:</strong> ${escapeHtml(p.adminNotes)}</div>` : ""}
        </div>
        <div class="pro-card-actions">
          <button
            type="button"
            class="chip-btn"
            data-open-private-chat="true"
            data-chat-context-type="professional"
            data-chat-context-id="${escapeHtml(p.id)}"
            data-chat-context-label="${escapeHtml(`${t("proTabJoin")}: ${p.fullName}`)}"
            data-chat-role="professional"
          >💬 ${escapeHtml(t("proMessageAdminBtn"))}</button>
        </div>
      </article>`
        )
        .join("")
    : `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("proNoMyProfiles"))}</p></div>`;

  const outCards = (myOutsourcing || []).length
    ? myOutsourcing
        .map(
          (o) => `
      <article class="studio-card pro-record-card">
        <div class="pro-card-head">
          <div>
            <strong>${escapeHtml(o.studioOrName)} — ${escapeHtml(o.workType)}</strong>
            <div class="pro-card-sub">${escapeHtml(o.city)} • ${escapeHtml(t("proOutDeadlineLabel"))}: ${escapeHtml(o.workDeadline)}</div>
          </div>
          <span class="badge ${getProStatusBadgeClass(o.status)}">${escapeHtml(o.status)}</span>
        </div>
        <p class="pro-card-intro">${escapeHtml(o.projectDescription || "")}</p>
        <div class="pro-card-details">
          ${o.budget ? `<div><strong>${escapeHtml(t("proBudgetLabel"))}:</strong> ${escapeHtml(o.budget)}</div>` : ""}
          ${o.assignedProfessionalName ? `<div><strong>${escapeHtml(t("proAssignedProLabel"))}:</strong> ${escapeHtml(o.assignedProfessionalName)}</div>` : ""}
          <div><strong>${escapeHtml(t("proSubmittedAtLabel"))}:</strong> ${escapeHtml(formatNetworkTimestamp(o.createdAt, lang))}</div>
          ${o.adminNotes ? `<div class="pro-admin-note-box"><strong>${escapeHtml(t("proAdminNoteLabel"))}:</strong> ${escapeHtml(o.adminNotes)}</div>` : ""}
        </div>
        <div class="pro-card-actions">
          <button
            type="button"
            class="chip-btn"
            data-open-private-chat="true"
            data-chat-context-type="outsourcing"
            data-chat-context-id="${escapeHtml(o.id)}"
            data-chat-context-label="${escapeHtml(`${t("proTabOutsourcing")}: ${o.workType} (${o.studioOrName})`)}"
            data-chat-role="professional"
          >💬 ${escapeHtml(t("proMessageAdminBtn"))}</button>
          ${
            o.status === "Pending Admin Review"
              ? `<button type="button" class="btn-danger" data-cancel-outsourcing-id="${escapeHtml(o.id)}">${escapeHtml(t("proCancelRequestBtn"))}</button>`
              : ""
          }
        </div>
      </article>`
        )
        .join("")
    : `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("proNoMyOutsourcing"))}</p></div>`;

  return `
    <div class="pro-section-stack">
      <h3 class="pro-subsection-title">${escapeHtml(t("proMyProfilesHeading"))}</h3>
      ${proCards}
      <h3 class="pro-subsection-title" style="margin-top:8px;">${escapeHtml(t("proMyOutsourcingHeading"))}</h3>
      ${outCards}
    </div>`;
}

export function renderMyHiringBookingsHtml({
  myHiringRequests = [],
  assignedToMe = [],
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const customerCards = (myHiringRequests || []).length
    ? myHiringRequests
        .map((req) => {
          const isProposal = req.status === "Assignment Proposed";
          const canCancel = ["Pending Admin Review", "Assignment Proposed", "Proposal Declined"].includes(req.status);
          return `
      <article class="studio-card pro-record-card" data-hiring-card-id="${escapeHtml(req.id)}">
        <div class="pro-card-head">
          <div>
            <strong>${escapeHtml(req.eventType)} — ${escapeHtml(req.requiredService)}</strong>
            <div class="pro-card-sub">${escapeHtml(req.eventDate)} • ${escapeHtml(req.eventTime)} • ${escapeHtml(req.eventLocation)}</div>
          </div>
          <span class="badge ${getProStatusBadgeClass(req.status)}">${escapeHtml(req.status)}</span>
        </div>
        <div class="pro-card-details">
          <div><strong>${escapeHtml(t("proHireDurationLabel"))}:</strong> ${escapeHtml(req.workingDuration)} • <strong>${escapeHtml(t("proBudgetLabel"))}:</strong> ${escapeHtml(req.budgetRange)}</div>
          ${req.specialRequirements ? `<div><strong>${escapeHtml(t("proHireRequirementsLabel"))}:</strong> ${escapeHtml(req.specialRequirements)}</div>` : ""}
          <div><strong>${escapeHtml(t("proSubmittedAtLabel"))}:</strong> ${escapeHtml(formatNetworkTimestamp(req.createdAt, lang))}</div>
        </div>
        ${
          req.assignedProfessionalName
            ? `<div class="pro-proposal-box">
                <div class="pro-proposal-title">✦ ${escapeHtml(t("proAssignedProLabel"))}: <strong>${escapeHtml(req.assignedProfessionalName)}</strong> ${req.assignedProfessionalCategory ? `(${escapeHtml(req.assignedProfessionalCategory)})` : ""}</div>
                ${req.proposalMessage ? `<p style="margin:6px 0 0;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(req.proposalMessage)}</p>` : ""}
                <div class="pro-proposal-meta">${escapeHtml(t("proCustomerAcceptanceLabel"))}: <strong>${escapeHtml(req.customerAcceptance || "pending")}</strong></div>
                ${
                  isProposal
                    ? `<div class="pro-proposal-actions">
                        <input type="text" class="input" data-customer-proposal-note="${escapeHtml(req.id)}" placeholder="${escapeHtml(t("proProposalNotePlaceholder"))}" maxlength="400" />
                        <div class="pro-card-actions">
                          <button type="button" class="btn-gold btn-sm" data-customer-proposal-action="accept" data-hiring-id="${escapeHtml(req.id)}">✓ ${escapeHtml(t("proAcceptProposalBtn"))}</button>
                          <button type="button" class="btn-outline btn-sm" data-customer-proposal-action="decline" data-hiring-id="${escapeHtml(req.id)}">✕ ${escapeHtml(t("proDeclineProposalBtn"))}</button>
                        </div>
                        <small style="color:var(--text-muted);">${escapeHtml(t("proFinalConfirmAdminNote"))}</small>
                      </div>`
                    : ""
                }
              </div>`
            : ""
        }
        ${req.adminNotes ? `<div class="pro-admin-note-box"><strong>${escapeHtml(t("proAdminNoteLabel"))}:</strong> ${escapeHtml(req.adminNotes)}</div>` : ""}
        <div class="pro-card-actions">
          <button
            type="button"
            class="chip-btn"
            data-open-private-chat="true"
            data-chat-context-type="hiring"
            data-chat-context-id="${escapeHtml(req.id)}"
            data-chat-context-label="${escapeHtml(`${req.eventType} (${req.eventDate})`)}"
            data-chat-role="customer"
          >💬 ${escapeHtml(t("proMessageAdminBtn"))}</button>
          ${
            canCancel
              ? `<button type="button" class="btn-danger" data-cancel-hiring-id="${escapeHtml(req.id)}">${escapeHtml(t("proCancelRequestBtn"))}</button>`
              : ""
          }
        </div>
      </article>`;
        })
        .join("")
    : `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("proNoMyHiring"))}</p></div>`;

  const assignedSection = (assignedToMe || []).length
    ? `
      <h3 class="pro-subsection-title" style="margin-top:8px;">${escapeHtml(t("proAssignedToMeHeading"))}</h3>
      ${assignedToMe
        .map(
          (job) => `
        <article class="studio-card pro-record-card">
          <div class="pro-card-head">
            <div>
              <strong>${escapeHtml(job.eventType || job.workType || "Assignment")} — ${escapeHtml(job.requiredService || job.city || "")}</strong>
              <div class="pro-card-sub">${escapeHtml(job.eventDate || job.workDeadline || "")} • ${escapeHtml(job.eventLocation || job.city || "")}</div>
            </div>
            <span class="badge ${getProStatusBadgeClass(job.status)}">${escapeHtml(job.status)}</span>
          </div>
          <div class="pro-card-actions">
            <button
              type="button"
              class="chip-btn"
              data-open-private-chat="true"
              data-chat-context-type="${escapeHtml(job._assignmentKind === "outsourcing" ? "outsourcing" : "hiring")}"
              data-chat-context-id="${escapeHtml(job.id)}"
              data-chat-context-label="${escapeHtml(`Assignment: ${job.eventType || job.workType || job.id}`)}"
              data-chat-role="professional"
            >💬 ${escapeHtml(t("proMessageAdminBtn"))}</button>
          </div>
        </article>`
        )
        .join("")}`
    : "";

  return `
    <div class="pro-section-stack">
      <h3 class="pro-subsection-title">${escapeHtml(t("proMyHiringHeading"))}</h3>
      ${customerCards}
      ${assignedSection}
    </div>`;
}
