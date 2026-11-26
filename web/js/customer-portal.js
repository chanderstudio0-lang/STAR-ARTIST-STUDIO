import { formatNetworkTimestamp } from "./pro-network.js";
import { renderUserPrivateChatHtml } from "./private-chat.js";
import {
  DEFAULT_PAYMENT_CONFIG,
  filterCustomerPayments,
  renderCustomerPaymentsPortalHtml
} from "./payment-system.js";

export const CUSTOMER_PORTAL_TABS = [
  "my-catalog",
  "my-slot",
  "my-registration",
  "approval-status",
  "my-bookings",
  "my-payments",
  "my-messages"
];

export const APPROVAL_STATUS_VALUES = ["Pending", "Approved", "Rejected", "Confirmed"];

const STRIPPED_INTERNAL_KEYS = new Set([
  "adminNotes",
  "internalNotes",
  "sharedByAdminEmail",
  "assignedByAdminEmail",
  "updatedByEmail",
  "fcmToken",
  "targetFcmToken",
  "photographerPhone",
  "photographerEmail",
  "professionalPhone",
  "professionalEmail",
  "totalBookings",
  "totalBookingCount",
  "studioRevenue",
  "internalMetrics"
]);

function defaultEscapeHtml(str) {
  return String(str ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function isSafeExternalUrl(raw) {
  const s = String(raw || "").trim();
  if (!s) return false;
  try {
    const parsed = new URL(s);
    return parsed.protocol === "https:" || parsed.protocol === "http:";
  } catch {
    return false;
  }
}

function isSafeImageSrc(raw) {
  const s = String(raw || "").trim();
  if (!s) return false;
  if (s.startsWith("/images/") || s.startsWith("/icons/")) return true;
  if (/^data:image\/(jpeg|jpg|png|webp);base64,[A-Za-z0-9+/=]+$/i.test(s) && s.length <= 180000) {
    return true;
  }
  return isSafeExternalUrl(s);
}

/**
 * Returns true ONLY if the record explicitly belongs to the authenticated customer's UID.
 */
export function isOwnedByCustomer(record, currentUid) {
  const uid = String(currentUid || "").trim();
  if (!uid || !record || typeof record !== "object") return false;
  const ownerUid = String(
    record.customerUid || record.userId || record.participantUid || ""
  ).trim();
  return ownerUid.length > 0 && ownerUid === uid;
}

/**
 * Removes admin-only notes, internal studio metrics, FCM tokens, and private photographer contact info
 * before rendering any record in the customer portal.
 */
export function stripAdminAndPrivateFields(record) {
  if (!record || typeof record !== "object") return {};
  const clean = {};
  for (const [key, value] of Object.entries(record)) {
    if (STRIPPED_INTERNAL_KEYS.has(key)) continue;
    clean[key] = value;
  }
  return clean;
}

/**
 * Filters a list strictly to records owned by currentUid and scrubs all admin/private fields.
 */
export function filterCustomerOwnedRecords(records = [], currentUid = "") {
  if (!Array.isArray(records) || !String(currentUid || "").trim()) return [];
  return records
    .filter((rec) => isOwnedByCustomer(rec, currentUid))
    .map((rec) => stripAdminAndPrivateFields(rec));
}

/**
 * Normalizes any booking/slot/registration status into one of the four customer approval states:
 * Pending | Approved | Rejected | Confirmed
 */
export function normalizePortalApprovalStatus(rawStatus = "") {
  const s = String(rawStatus || "").trim().toLowerCase();
  if (s === "confirmed" || s === "completed" || s === "assigned" || s === "in progress") {
    return "Confirmed";
  }
  if (s === "approved" || s === "customer accepted" || s === "assignment proposed" || s === "contacted") {
    return "Approved";
  }
  if (s === "rejected" || s === "cancelled" || s === "declined" || s === "proposal declined" || s === "suspended") {
    return "Rejected";
  }
  return "Pending";
}

export function getApprovalStatusBadgeClass(status) {
  const norm = normalizePortalApprovalStatus(status);
  if (norm === "Confirmed") return "portal-status-confirmed";
  if (norm === "Approved") return "portal-status-approved";
  if (norm === "Rejected") return "portal-status-rejected";
  return "portal-status-pending";
}

/**
 * Prevents cross-customer access via URL query parameter or document ID tampering
 * (e.g. #portal?booking=OTHER_USER_BOOKING_ID or ?catalog=OTHER_CAT_ID or ?slot=OTHER_SLOT_ID).
 */
export function resolvePortalDeepLinkAccess({
  requestedId = "",
  ownedRecords = [],
  currentUid = ""
} = {}) {
  const cleanId = String(requestedId || "").trim();
  const uid = String(currentUid || "").trim();
  if (!cleanId) {
    return { allowed: true, denied: false, record: null };
  }
  if (!uid) {
    return { allowed: false, denied: true, record: null };
  }
  const owned = filterCustomerOwnedRecords(ownedRecords, uid);
  const match = owned.find((item) => String(item.id || "").trim() === cleanId) || null;
  if (!match) {
    return { allowed: false, denied: true, record: null };
  }
  return { allowed: true, denied: false, record: match };
}

/**
 * Parses URL query params targeting a specific portal item (?booking=, ?catalog=, ?slot=, ?tab=).
 */
export function extractPortalQueryParams({ hash = "", search = "" } = {}) {
  const merged = new URLSearchParams();
  const appendFrom = (raw) => {
    const qIdx = String(raw || "").indexOf("?");
    const qs = qIdx >= 0 ? String(raw).slice(qIdx + 1) : String(raw || "").replace(/^\?/, "");
    if (!qs) return;
    try {
      const p = new URLSearchParams(qs);
      for (const [k, v] of p.entries()) {
        if (v) merged.set(k.toLowerCase(), v.trim());
      }
    } catch {
      // Ignore malformed query string
    }
  };
  appendFrom(search);
  appendFrom(hash);

  const tab = String(merged.get("tab") || "").trim().toLowerCase();
  return {
    tab: CUSTOMER_PORTAL_TABS.includes(tab) ? tab : "",
    bookingId: merged.get("booking") || merged.get("bookingid") || "",
    catalogId: merged.get("catalog") || merged.get("catalogid") || "",
    slotId: merged.get("slot") || merged.get("slotid") || ""
  };
}

/**
 * Validates Admin input when assigning a private catalog to a single customer UID.
 */
export function validateCustomerCatalogPayload(input = {}) {
  const customerUid = String(input.customerUid || "").trim();
  const bookingId = String(input.bookingId || "").trim().slice(0, 128);
  const title = String(input.title || "").trim();
  const category = String(input.category || "Private Selection").trim().slice(0, 80);
  const description = String(input.description || "").trim().slice(0, 1000);
  const coverImageUrl = String(input.coverImageUrl || "").trim();
  const rawItems = Array.isArray(input.items)
    ? input.items
    : String(input.itemsText || "")
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean);

  if (!customerUid || customerUid.length > 128) {
    return { valid: false, error: "Customer UID is required to assign a private catalog." };
  }
  if (title.length < 2 || title.length > 140) {
    return { valid: false, error: "Catalog title must be between 2 and 140 characters." };
  }
  if (coverImageUrl && !isSafeImageSrc(coverImageUrl)) {
    return { valid: false, error: "Cover image URL must be a valid https:// URL or image." };
  }
  const cleanItems = rawItems.slice(0, 50).map((entry) => {
    if (typeof entry === "string") {
      const parts = entry.split("|").map((p) => p.trim());
      const label = (parts[0] || "").slice(0, 140);
      const link = parts[1] && isSafeExternalUrl(parts[1]) ? parts[1].slice(0, 500) : "";
      return link ? { label, url: link } : { label, url: "" };
    }
    const label = String(entry?.label || entry?.title || "").trim().slice(0, 140);
    const url = entry?.url && isSafeExternalUrl(entry.url) ? String(entry.url).trim().slice(0, 500) : "";
    return { label, url };
  }).filter((item) => item.label.length > 0);

  return {
    valid: true,
    clean: {
      customerUid,
      bookingId,
      title,
      category,
      description,
      coverImageUrl,
      items: cleanItems,
      status: input.status === "archived" ? "archived" : "active"
    }
  };
}

/**
 * Validates Admin input when assigning a private booking slot & approval status to one customer UID.
 */
export function validateCustomerSlotPayload(input = {}) {
  const customerUid = String(input.customerUid || "").trim();
  const bookingId = String(input.bookingId || "").trim().slice(0, 128);
  const eventTitle = String(input.eventTitle || "").trim();
  const eventDate = String(input.eventDate || "").trim();
  const eventTime = String(input.eventTime || "").trim();
  const slotLabel = String(input.slotLabel || "").trim();
  const location = String(input.location || "Star Artist Studio, Talwara Khurd").trim().slice(0, 200);
  const approvalStatus = APPROVAL_STATUS_VALUES.includes(input.approvalStatus)
    ? input.approvalStatus
    : "Pending";
  const customerNote = String(input.customerNote || "").trim().slice(0, 500);

  if (!customerUid || customerUid.length > 128) {
    return { valid: false, error: "Customer UID is required to assign a booking slot." };
  }
  if (eventTitle.length < 2 || eventTitle.length > 140) {
    return { valid: false, error: "Event title must be between 2 and 140 characters." };
  }
  if (eventDate.length < 6 || eventDate.length > 50) {
    return { valid: false, error: "Event date is required." };
  }
  if (eventTime.length < 2 || eventTime.length > 50) {
    return { valid: false, error: "Event time is required." };
  }
  if (slotLabel.length < 2 || slotLabel.length > 120) {
    return { valid: false, error: "Assigned slot label is required." };
  }

  return {
    valid: true,
    clean: {
      customerUid,
      bookingId,
      eventTitle,
      eventDate,
      eventTime,
      slotLabel,
      location,
      approvalStatus,
      customerNote
    }
  };
}

/**
 * Builds the customer-only slot list by combining explicitly assigned `customerSlots`
 * with slot details from the customer's OWN bookings/hiring requests (never any other customer).
 */
export function buildCustomerOwnSlots({ currentUid = "", assignedSlots = [], bookings = [], hiringRequests = [] } = {}) {
  const ownSlots = filterCustomerOwnedRecords(assignedSlots, currentUid).map((s) => ({
    id: s.id,
    source: "assigned",
    bookingId: s.bookingId || "",
    eventTitle: s.eventTitle || "Studio Booking Slot",
    eventDate: s.eventDate || "",
    eventTime: s.eventTime || "",
    slotLabel: s.slotLabel || "",
    location: s.location || "Star Artist Studio, Talwara Khurd",
    approvalStatus: normalizePortalApprovalStatus(s.approvalStatus),
    customerNote: s.customerNote || "",
    createdAt: s.createdAt || null
  }));

  const assignedBookingIds = new Set(ownSlots.map((s) => s.bookingId).filter(Boolean));

  const ownBookings = filterCustomerOwnedRecords(bookings, currentUid);
  for (const b of ownBookings) {
    if (b.id && assignedBookingIds.has(b.id)) continue;
    ownSlots.push({
      id: `booking_${b.id || Math.random()}`,
      source: "booking",
      bookingId: b.id || "",
      eventTitle: b.serviceType || "Studio Shoot",
      eventDate: b.preferredDate || "",
      eventTime: b.preferredTime || "",
      slotLabel: b.preferredTime ? `Requested Slot • ${b.preferredTime}` : "Requested Shoot Slot",
      location: b.location || "Star Artist Studio, Talwara Khurd",
      approvalStatus: normalizePortalApprovalStatus(b.status),
      customerNote: "",
      createdAt: b.createdAt || null
    });
  }

  const ownHiring = filterCustomerOwnedRecords(hiringRequests, currentUid);
  for (const h of ownHiring) {
    if (h.id && assignedBookingIds.has(h.id)) continue;
    ownSlots.push({
      id: `hiring_${h.id || Math.random()}`,
      source: "hiring",
      bookingId: h.id || "",
      eventTitle: `${h.eventType || "Event"} — ${h.requiredService || "Shoot"}`,
      eventDate: h.eventDate || "",
      eventTime: h.eventTime || "",
      slotLabel: h.workingDuration ? `${h.eventTime || ""} (${h.workingDuration})`.trim() : h.eventTime || "Scheduled Event Slot",
      location: h.eventLocation || "",
      approvalStatus: normalizePortalApprovalStatus(h.status),
      customerNote: h.proposalMessage || "",
      createdAt: h.createdAt || null
    });
  }

  return ownSlots;
}

/**
 * Builds the customer's own unified approval status list across their own slots, bookings, and registrations.
 */
export function buildCustomerApprovalSummary({
  currentUid = "",
  assignedSlots = [],
  bookings = [],
  hiringRequests = [],
  models = [],
  companyBookings = []
} = {}) {
  const items = [];
  for (const s of filterCustomerOwnedRecords(assignedSlots, currentUid)) {
    items.push({
      id: s.id,
      type: "Booking Slot",
      title: s.eventTitle || s.slotLabel || "Assigned Slot",
      dateLabel: [s.eventDate, s.eventTime].filter(Boolean).join(" • "),
      status: normalizePortalApprovalStatus(s.approvalStatus),
      rawStatus: s.approvalStatus || "Pending",
      submittedAt: s.createdAt || null
    });
  }
  for (const b of filterCustomerOwnedRecords(bookings, currentUid)) {
    items.push({
      id: b.id,
      type: "Shoot Booking",
      title: b.serviceType || "Studio Shoot",
      dateLabel: [b.preferredDate, b.preferredTime].filter(Boolean).join(" • "),
      status: normalizePortalApprovalStatus(b.status),
      rawStatus: b.status || "Pending",
      submittedAt: b.createdAt || null
    });
  }
  for (const h of filterCustomerOwnedRecords(hiringRequests, currentUid)) {
    items.push({
      id: h.id,
      type: "Photographer Hiring",
      title: `${h.eventType || "Event"} • ${h.requiredService || "Service"}`,
      dateLabel: [h.eventDate, h.eventTime].filter(Boolean).join(" • "),
      status: normalizePortalApprovalStatus(h.status),
      rawStatus: h.status || "Pending Admin Review",
      submittedAt: h.createdAt || null
    });
  }
  for (const m of filterCustomerOwnedRecords(models, currentUid)) {
    items.push({
      id: m.id,
      type: "Model Registration",
      title: m.fullName || "Model Application",
      dateLabel: m.city || "",
      status: normalizePortalApprovalStatus(m.status),
      rawStatus: m.status || "pending",
      submittedAt: m.createdAt || null
    });
  }
  for (const c of filterCustomerOwnedRecords(companyBookings, currentUid)) {
    items.push({
      id: c.id,
      type: "Event / Company Booking",
      title: `${c.companyName || "Event"} • ${c.eventType || "Booking"}`,
      dateLabel: c.eventDate || "",
      status: normalizePortalApprovalStatus(c.status),
      rawStatus: c.status || "Pending",
      submittedAt: c.createdAt || null
    });
  }
  return items;
}

/**
 * Renders the strict Customer Private Booking Portal (#view-portal body).
 * Guarantees that EVERY displayed record matches `user.uid` and zero admin/cross-customer data appears.
 */
export function renderCustomerPortalHtml({
  user = null,
  userProfile = null,
  catalogs = [],
  slots = [],
  bookings = [],
  hiringRequests = [],
  models = [],
  companyBookings = [],
  chatThreads = [],
  activeChatId = "",
  chatMessages = [],
  isChatBlocked = false,
  chatBlockReason = "",
  chatDraftText = "",
  chatDraftImage = "",
  isChatSending = false,
  payments = [],
  paymentConfig = DEFAULT_PAYMENT_CONFIG,
  publicModels = [],
  activePaymentForm = "model",
  activeTab = "my-catalog",
  deniedResourceNotice = "",
  highlightedBookingId = "",
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const uid = String(user?.uid || "").trim();
  if (!uid || user?.isAnonymous === true) {
    return `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("portalSignInRequired"))}</p></div>`;
  }

  const safeTab = CUSTOMER_PORTAL_TABS.includes(activeTab) ? activeTab : "my-catalog";
  const ownCatalogs = filterCustomerOwnedRecords(catalogs, uid).filter((c) => c.status !== "archived");
  const ownAssignedSlots = filterCustomerOwnedRecords(slots, uid);
  const ownBookings = filterCustomerOwnedRecords(bookings, uid);
  const ownHiring = filterCustomerOwnedRecords(hiringRequests, uid);
  const ownModels = filterCustomerOwnedRecords(models, uid);
  const ownCompanyBookings = filterCustomerOwnedRecords(companyBookings, uid);
  const ownThreads = filterCustomerOwnedRecords(chatThreads, uid);
  const ownPayments = filterCustomerPayments(payments, uid);

  const safeProfile =
    userProfile && (String(userProfile.userId || userProfile.id || "").trim() === uid)
      ? stripAdminAndPrivateFields(userProfile)
      : null;

  const combinedSlots = buildCustomerOwnSlots({
    currentUid: uid,
    assignedSlots: ownAssignedSlots,
    bookings: ownBookings,
    hiringRequests: ownHiring
  });

  const approvalList = buildCustomerApprovalSummary({
    currentUid: uid,
    assignedSlots: ownAssignedSlots,
    bookings: ownBookings,
    hiringRequests: ownHiring,
    models: ownModels,
    companyBookings: ownCompanyBookings
  });

  const unreadMsgs = ownThreads.reduce((acc, th) => acc + Math.max(0, Number(th.unreadByUser || 0)), 0);

  const tabsHtml = `
    <div class="portal-tabs-bar" role="tablist" aria-label="${escapeHtml(t("portalHeading"))}">
      <button type="button" class="chip-btn ${safeTab === "my-catalog" ? "active" : ""}" role="tab" aria-selected="${safeTab === "my-catalog"}" data-portal-tab="my-catalog">
        <span>${escapeHtml(t("portalTabMyCatalog"))}</span>
        <span class="filter-count-badge">${ownCatalogs.length}</span>
      </button>
      <button type="button" class="chip-btn ${safeTab === "my-slot" ? "active" : ""}" role="tab" aria-selected="${safeTab === "my-slot"}" data-portal-tab="my-slot">
        <span>${escapeHtml(t("portalTabMySlot"))}</span>
        <span class="filter-count-badge">${combinedSlots.length}</span>
      </button>
      <button type="button" class="chip-btn ${safeTab === "my-registration" ? "active" : ""}" role="tab" aria-selected="${safeTab === "my-registration"}" data-portal-tab="my-registration">
        <span>${escapeHtml(t("portalTabMyRegistration"))}</span>
      </button>
      <button type="button" class="chip-btn ${safeTab === "approval-status" ? "active" : ""}" role="tab" aria-selected="${safeTab === "approval-status"}" data-portal-tab="approval-status">
        <span>${escapeHtml(t("portalTabApprovalStatus"))}</span>
        <span class="filter-count-badge">${approvalList.length}</span>
      </button>
      <button type="button" class="chip-btn ${safeTab === "my-bookings" ? "active" : ""}" role="tab" aria-selected="${safeTab === "my-bookings"}" data-portal-tab="my-bookings">
        <span>${escapeHtml(t("portalTabMyBookings"))}</span>
        <span class="filter-count-badge">${ownBookings.length + ownHiring.length + ownCompanyBookings.length}</span>
      </button>
      <button type="button" class="chip-btn ${safeTab === "my-payments" ? "active" : ""}" role="tab" aria-selected="${safeTab === "my-payments"}" data-portal-tab="my-payments">
        <span>${escapeHtml(t("portalTabMyPayments"))}</span>
        <span class="filter-count-badge">${ownPayments.length}</span>
      </button>
      <button type="button" class="chip-btn ${safeTab === "my-messages" ? "active" : ""}" role="tab" aria-selected="${safeTab === "my-messages"}" data-portal-tab="my-messages">
        <span>${escapeHtml(t("portalTabMyMessages"))}</span>
        ${unreadMsgs > 0 ? `<span class="chat-unread-pill">${unreadMsgs}</span>` : `<span class="filter-count-badge">${ownThreads.length}</span>`}
      </button>
    </div>`;

  const deniedBannerHtml = deniedResourceNotice
    ? `<div class="portal-denied-banner" role="alert" data-portal-access-denied="true">
         <strong>🔒 ${escapeHtml(t("portalAccessDeniedTitle"))}</strong>
         <p style="margin:4px 0 0;font-size:0.86rem;">${escapeHtml(deniedResourceNotice)}</p>
       </div>`
    : "";

  let panelHtml = "";

  if (safeTab === "my-catalog") {
    panelHtml = `
      <div class="portal-section-panel" data-portal-panel="my-catalog">
        <div class="portal-panel-header">
          <h3 style="margin:0;color:var(--gold);">${escapeHtml(t("portalTabMyCatalog"))}</h3>
          <p style="margin:4px 0 0;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("portalMyCatalogDesc"))}</p>
        </div>
        ${
          ownCatalogs.length
            ? `<div class="portal-cards-grid">
                ${ownCatalogs
                  .map((cat) => {
                    const itemsList = Array.isArray(cat.items) ? cat.items : [];
                    const safeCover = cat.coverImageUrl && isSafeImageSrc(cat.coverImageUrl) ? cat.coverImageUrl : "";
                    return `
                    <article class="studio-card portal-item-card" data-portal-catalog-id="${escapeHtml(cat.id || "")}">
                      ${safeCover ? `<img class="portal-catalog-cover" src="${escapeHtml(safeCover)}" alt="${escapeHtml(cat.title || "Private Catalog")}" loading="lazy" />` : ""}
                      <div class="portal-card-top">
                        <span class="badge">${escapeHtml(cat.category || "Private Catalog")}</span>
                        <span class="portal-private-lock">🔒 ${escapeHtml(t("portalPrivateTag"))}</span>
                      </div>
                      <h4 style="margin:6px 0 4px;color:var(--gold-light);">${escapeHtml(cat.title || "")}</h4>
                      ${cat.description ? `<p style="margin:0 0 8px;font-size:0.88rem;color:var(--text-secondary);">${escapeHtml(cat.description)}</p>` : ""}
                      ${
                        itemsList.length
                          ? `<ul class="portal-catalog-items">
                              ${itemsList
                                .map((it) => {
                                  const label = typeof it === "string" ? it : it?.label || "";
                                  const url = typeof it === "object" && it?.url && isSafeExternalUrl(it.url) ? it.url : "";
                                  return `<li>${
                                    url
                                      ? `<a href="${escapeHtml(url)}" target="_blank" rel="noopener noreferrer">${escapeHtml(label)} ↗</a>`
                                      : escapeHtml(label)
                                  }</li>`;
                                })
                                .join("")}
                            </ul>`
                          : ""
                      }
                      <div class="portal-card-meta">
                        ${cat.bookingId ? `<span>${escapeHtml(t("portalLinkedBookingLabel"))}: #${escapeHtml(String(cat.bookingId).slice(-6))}</span>` : ""}
                        <span>${escapeHtml(formatNetworkTimestamp(cat.updatedAt || cat.createdAt, lang))}</span>
                      </div>
                    </article>`;
                  })
                  .join("")}
              </div>`
            : `<div class="studio-card portal-empty-card">
                <p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("portalNoCatalogsYet"))}</p>
              </div>`
        }
      </div>`;
  } else if (safeTab === "my-slot") {
    panelHtml = `
      <div class="portal-section-panel" data-portal-panel="my-slot">
        <div class="portal-panel-header">
          <h3 style="margin:0;color:var(--gold);">${escapeHtml(t("portalTabMySlot"))}</h3>
          <p style="margin:4px 0 0;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("portalMySlotDesc"))}</p>
        </div>
        ${
          combinedSlots.length
            ? `<div class="portal-cards-grid">
                ${combinedSlots
                  .map(
                    (slot) => `
                  <article class="studio-card portal-item-card" data-portal-slot-id="${escapeHtml(slot.id || "")}">
                    <div class="portal-card-top">
                      <span class="portal-status-pill ${getApprovalStatusBadgeClass(slot.approvalStatus)}">${escapeHtml(slot.approvalStatus)}</span>
                      <span class="portal-private-lock">🔒 ${escapeHtml(t("portalPrivateTag"))}</span>
                    </div>
                    <h4 style="margin:6px 0 4px;color:var(--gold-light);">${escapeHtml(slot.eventTitle)}</h4>
                    <div class="portal-kv-list">
                      <div><span>${escapeHtml(t("portalEventDateLabel"))}:</span> <strong>${escapeHtml(slot.eventDate || "—")}</strong></div>
                      <div><span>${escapeHtml(t("portalEventTimeLabel"))}:</span> <strong>${escapeHtml(slot.eventTime || "—")}</strong></div>
                      <div><span>${escapeHtml(t("portalAssignedSlotLabel"))}:</span> <strong>${escapeHtml(slot.slotLabel || "—")}</strong></div>
                      <div><span>${escapeHtml(t("portalLocationLabel"))}:</span> <strong>${escapeHtml(slot.location || "—")}</strong></div>
                    </div>
                    ${slot.customerNote ? `<p class="portal-customer-note">${escapeHtml(slot.customerNote)}</p>` : ""}
                  </article>`
                  )
                  .join("")}
              </div>`
            : `<div class="studio-card portal-empty-card">
                <p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("portalNoSlotsYet"))}</p>
              </div>`
        }
      </div>`;
  } else if (safeTab === "my-registration") {
    const displayName = safeProfile?.displayName || user?.displayName || user?.email?.split("@")[0] || "Customer";
    const email = safeProfile?.email || user?.email || "";
    const registeredAt = safeProfile?.createdAt || user?.metadata?.creationTime || null;
    const phone = safeProfile?.phoneNumber || ownBookings[0]?.contactNumber || ownHiring[0]?.contactNumber || "";
    const city = safeProfile?.city || ownModels[0]?.city || "";

    panelHtml = `
      <div class="portal-section-panel" data-portal-panel="my-registration">
        <div class="portal-panel-header">
          <h3 style="margin:0;color:var(--gold);">${escapeHtml(t("portalTabMyRegistration"))}</h3>
          <p style="margin:4px 0 0;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("portalMyRegistrationDesc"))}</p>
        </div>
        <div class="studio-card portal-item-card">
          <div class="portal-card-top">
            <span class="badge">${escapeHtml(t("portalAccountVerifiedBadge"))}</span>
            <span class="portal-private-lock">🔒 ${escapeHtml(t("portalPrivateTag"))}</span>
          </div>
          <div class="portal-kv-list" style="margin-top:8px;">
            <div><span>${escapeHtml(t("authFullNameLabel"))}:</span> <strong>${escapeHtml(displayName)}</strong></div>
            <div><span>${escapeHtml(t("emailLabel"))}:</span> <strong>${escapeHtml(email)}</strong></div>
            ${phone ? `<div><span>${escapeHtml(t("portalPhoneLabel"))}:</span> <strong>${escapeHtml(phone)}</strong></div>` : ""}
            ${city ? `<div><span>${escapeHtml(t("proCityLabel"))}:</span> <strong>${escapeHtml(city)}</strong></div>` : ""}
            <div><span>${escapeHtml(t("portalSubmissionDateLabel"))}:</span> <strong>${escapeHtml(registeredAt ? formatNetworkTimestamp(registeredAt, lang) : "—")}</strong></div>
          </div>
        </div>
        ${
          ownModels.length
            ? `<div class="studio-card portal-item-card" style="margin-top:12px;">
                <h4 style="margin:0 0 8px;color:var(--gold-light);">${escapeHtml(t("portalMyModelRegistrationsTitle"))}</h4>
                ${ownModels
                  .map(
                    (m) => `
                  <div class="portal-mini-row">
                    <div>
                      <strong>${escapeHtml(m.fullName || "")}</strong>
                      <div style="font-size:0.8rem;color:var(--text-secondary);">${escapeHtml(t("portalSubmissionDateLabel"))}: ${escapeHtml(formatNetworkTimestamp(m.createdAt, lang))}</div>
                    </div>
                    <span class="portal-status-pill ${getApprovalStatusBadgeClass(m.status)}">${escapeHtml(normalizePortalApprovalStatus(m.status))}</span>
                  </div>`
                  )
                  .join("")}
              </div>`
            : ""
        }
      </div>`;
  } else if (safeTab === "approval-status") {
    panelHtml = `
      <div class="portal-section-panel" data-portal-panel="approval-status">
        <div class="portal-panel-header">
          <h3 style="margin:0;color:var(--gold);">${escapeHtml(t("portalTabApprovalStatus"))}</h3>
          <p style="margin:4px 0 0;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("portalApprovalStatusDesc"))}</p>
        </div>
        <div class="portal-status-legend">
          <span class="portal-status-pill portal-status-pending">Pending</span>
          <span class="portal-status-pill portal-status-approved">Approved</span>
          <span class="portal-status-pill portal-status-confirmed">Confirmed</span>
          <span class="portal-status-pill portal-status-rejected">Rejected</span>
        </div>
        ${
          approvalList.length
            ? `<div class="portal-cards-grid">
                ${approvalList
                  .map(
                    (item) => `
                  <article class="studio-card portal-item-card">
                    <div class="portal-card-top">
                      <span class="badge">${escapeHtml(item.type)}</span>
                      <span class="portal-status-pill ${getApprovalStatusBadgeClass(item.status)}" data-portal-approval-status="${escapeHtml(item.status)}">${escapeHtml(item.status)}</span>
                    </div>
                    <h4 style="margin:6px 0 4px;color:var(--gold-light);">${escapeHtml(item.title)}</h4>
                    ${item.dateLabel ? `<div style="font-size:0.85rem;color:var(--text-secondary);">${escapeHtml(item.dateLabel)}</div>` : ""}
                    <div class="portal-card-meta">
                      <span>${escapeHtml(t("portalSubmissionDateLabel"))}: ${escapeHtml(formatNetworkTimestamp(item.submittedAt, lang))}</span>
                    </div>
                  </article>`
                  )
                  .join("")}
              </div>`
            : `<div class="studio-card portal-empty-card">
                <p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("portalNoApprovalsYet"))}</p>
              </div>`
        }
      </div>`;
  } else if (safeTab === "my-bookings") {
    const hasAnyBookings = ownBookings.length > 0 || ownHiring.length > 0 || ownCompanyBookings.length > 0;
    panelHtml = `
      <div class="portal-section-panel" data-portal-panel="my-bookings">
        <div class="portal-panel-header">
          <h3 style="margin:0;color:var(--gold);">${escapeHtml(t("portalTabMyBookings"))}</h3>
          <p style="margin:4px 0 0;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("portalMyBookingsDesc"))}</p>
        </div>
        ${
          hasAnyBookings
            ? `<div class="portal-cards-grid">
                ${ownBookings
                  .map((b) => {
                    const normStatus = normalizePortalApprovalStatus(b.status);
                    const isHighlighted = highlightedBookingId && highlightedBookingId === b.id;
                    return `
                    <article class="studio-card portal-item-card ${isHighlighted ? "is-highlighted" : ""}" data-portal-booking-id="${escapeHtml(b.id || "")}">
                      <div class="portal-card-top">
                        <span class="badge">${escapeHtml(t("portalShootBookingType"))}</span>
                        <span class="portal-status-pill ${getApprovalStatusBadgeClass(normStatus)}">${escapeHtml(normStatus)}</span>
                      </div>
                      <h4 style="margin:6px 0 4px;color:var(--gold-light);">${escapeHtml(b.serviceType || "Studio Shoot")}</h4>
                      <div class="portal-kv-list">
                        <div><span>${escapeHtml(t("clientNameLabel"))}:</span> <strong>${escapeHtml(b.clientName || "")}</strong></div>
                        <div><span>${escapeHtml(t("portalEventDateLabel"))}:</span> <strong>${escapeHtml(b.preferredDate || "—")}</strong></div>
                        <div><span>${escapeHtml(t("portalEventTimeLabel"))}:</span> <strong>${escapeHtml(b.preferredTime || "—")}</strong></div>
                        <div><span>${escapeHtml(t("portalLocationLabel"))}:</span> <strong>${escapeHtml(b.location || "—")}</strong></div>
                        ${b.notes ? `<div><span>${escapeHtml(t("portalMyNotesLabel"))}:</span> <strong>${escapeHtml(b.notes)}</strong></div>` : ""}
                      </div>
                      <div class="portal-card-actions">
                        <button
                          type="button"
                          class="btn-outline btn-sm"
                          data-portal-message-admin="true"
                          data-chat-context-type="hiring"
                          data-chat-context-id="${escapeHtml(b.id || "studio")}"
                          data-chat-context-label="${escapeHtml(`Booking: ${b.serviceType || "Shoot"} (${b.preferredDate || ""})`)}"
                        >${escapeHtml(t("portalMessageAdminBtn"))}</button>
                      </div>
                    </article>`;
                  })
                  .join("")}
                ${ownHiring
                  .map((h) => {
                    const normStatus = normalizePortalApprovalStatus(h.status);
                    return `
                    <article class="studio-card portal-item-card" data-portal-hiring-id="${escapeHtml(h.id || "")}">
                      <div class="portal-card-top">
                        <span class="badge">${escapeHtml(t("portalHiringBookingType"))}</span>
                        <span class="portal-status-pill ${getApprovalStatusBadgeClass(normStatus)}">${escapeHtml(normStatus)}</span>
                      </div>
                      <h4 style="margin:6px 0 4px;color:var(--gold-light);">${escapeHtml(`${h.eventType || "Event"} • ${h.requiredService || "Shoot"}`)}</h4>
                      <div class="portal-kv-list">
                        <div><span>${escapeHtml(t("portalEventDateLabel"))}:</span> <strong>${escapeHtml(h.eventDate || "—")}</strong></div>
                        <div><span>${escapeHtml(t("portalEventTimeLabel"))}:</span> <strong>${escapeHtml(h.eventTime || "—")}</strong></div>
                        <div><span>${escapeHtml(t("portalLocationLabel"))}:</span> <strong>${escapeHtml(h.eventLocation || "—")}</strong></div>
                        <div><span>${escapeHtml(t("proBudgetLabel"))}:</span> <strong>${escapeHtml(h.budgetRange || "—")}</strong></div>
                      </div>
                      <div class="portal-card-actions">
                        <button
                          type="button"
                          class="btn-outline btn-sm"
                          data-portal-message-admin="true"
                          data-chat-context-type="hiring"
                          data-chat-context-id="${escapeHtml(h.id || "hiring")}"
                          data-chat-context-label="${escapeHtml(`${h.eventType || "Event"} (${h.eventDate || ""})`)}"
                        >${escapeHtml(t("portalMessageAdminBtn"))}</button>
                      </div>
                    </article>`;
                  })
                  .join("")}
                ${ownCompanyBookings
                  .map((c) => {
                    const normStatus = normalizePortalApprovalStatus(c.status);
                    return `
                    <article class="studio-card portal-item-card" data-portal-company-booking-id="${escapeHtml(c.id || "")}">
                      <div class="portal-card-top">
                        <span class="badge">${escapeHtml(t("portalCompanyBookingType"))}</span>
                        <span class="portal-status-pill ${getApprovalStatusBadgeClass(normStatus)}">${escapeHtml(normStatus)}</span>
                      </div>
                      <h4 style="margin:6px 0 4px;color:var(--gold-light);">${escapeHtml(`${c.companyName || "Company"} • ${c.eventType || "Event"}`)}</h4>
                      <div class="portal-kv-list">
                        <div><span>${escapeHtml(t("portalEventDateLabel"))}:</span> <strong>${escapeHtml(c.eventDate || "—")}</strong></div>
                        <div><span>${escapeHtml(t("portalLocationLabel"))}:</span> <strong>${escapeHtml(c.eventLocation || "—")}</strong></div>
                        <div><span>${escapeHtml(t("proBudgetLabel"))}:</span> <strong>${escapeHtml(c.budget || "—")}</strong></div>
                      </div>
                    </article>`;
                  })
                  .join("")}
              </div>`
            : `<div class="studio-card portal-empty-card">
                <p style="margin:0 0 12px;color:var(--text-secondary);">${escapeHtml(t("portalNoBookingsYet"))}</p>
                <button type="button" class="btn-gold btn-sm" data-route="booking">${escapeHtml(t("heroBookCta"))}</button>
              </div>`
        }
      </div>`;
  } else if (safeTab === "my-payments") {
    panelHtml = `
      <div class="portal-section-panel" data-portal-panel="my-payments">
        ${renderCustomerPaymentsPortalHtml({
          payments: ownPayments,
          paymentConfig,
          publicModels,
          currentUserUid: uid,
          currentUserName: safeProfile?.displayName || user?.displayName || "",
          activePaymentForm,
          lang,
          t,
          escapeHtml
        })}
      </div>`;
  } else {
    // my-messages: strictly this customer's private conversation with Studio Admin
    panelHtml = `
      <div class="portal-section-panel" data-portal-panel="my-messages">
        ${renderUserPrivateChatHtml({
          threads: ownThreads,
          activeChatId,
          messages: chatMessages,
          currentUserUid: uid,
          draftText: chatDraftText,
          draftImage: chatDraftImage,
          isBlocked: isChatBlocked,
          blockReason: chatBlockReason,
          isSending: isChatSending,
          lang,
          t,
          escapeHtml
        })}
      </div>`;
  }

  return `
    <div class="portal-privacy-banner studio-card" role="note" data-portal-owner-uid="${escapeHtml(uid)}">
      <div class="portal-privacy-icon" aria-hidden="true">🔒</div>
      <div>
        <strong style="color:var(--gold);">${escapeHtml(t("portalPrivacyTitle"))}</strong>
        <p style="margin:3px 0 0;font-size:0.85rem;color:var(--text-secondary);">${escapeHtml(t("portalPrivacyDesc"))}</p>
      </div>
    </div>
    ${deniedBannerHtml}
    ${tabsHtml}
    ${panelHtml}
  `;
}

/**
 * Renders the Admin-only Customer Private Portal manager inside the Admin Dashboard.
 * Never rendered or exposed to customers.
 */
export function renderAdminCustomerPortalTabHtml({
  bookings = [],
  catalogs = [],
  slots = [],
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const customerOptionsMap = new Map();
  for (const b of Array.isArray(bookings) ? bookings : []) {
    const uid = String(b?.userId || "").trim();
    if (!uid) continue;
    const label = `${b.clientName || "Customer"} — ${b.serviceType || "Booking"} (${b.preferredDate || ""}) [${uid.slice(0, 8)}…]`;
    customerOptionsMap.set(`${uid}|${b.id || ""}`, {
      uid,
      bookingId: b.id || "",
      label,
      eventTitle: b.serviceType || "Studio Shoot",
      eventDate: b.preferredDate || "",
      eventTime: b.preferredTime || "",
      location: b.location || "Star Artist Studio, Talwara Khurd"
    });
  }
  const customerOptionsHtml = Array.from(customerOptionsMap.values())
    .map(
      (opt) =>
        `<option value="${escapeHtml(opt.uid)}" data-booking-id="${escapeHtml(opt.bookingId)}" data-event-title="${escapeHtml(opt.eventTitle)}" data-event-date="${escapeHtml(opt.eventDate)}" data-event-time="${escapeHtml(opt.eventTime)}" data-location="${escapeHtml(opt.location)}">${escapeHtml(opt.label)}</option>`
    )
    .join("");

  return `
    <div class="studio-card">
      <h3 style="margin:0 0 6px;color:var(--gold);">${escapeHtml(t("adminPortalCatalogAssignTitle"))}</h3>
      <p style="margin:0 0 12px;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("adminPortalCatalogAssignDesc"))}</p>
      <form id="adminAssignCatalogForm" class="form-grid two-col">
        <div class="field full">
          <label for="adminCatalogQuickPick">${escapeHtml(t("adminPortalSelectBookingLabel"))}</label>
          <select id="adminCatalogQuickPick" class="input">
            <option value="">-- ${escapeHtml(t("adminPortalPickCustomerHint"))} --</option>
            ${customerOptionsHtml}
          </select>
        </div>
        <div class="field">
          <label for="adminCatalogCustomerUid">${escapeHtml(t("adminPortalCustomerUidLabel"))}</label>
          <input id="adminCatalogCustomerUid" class="input" required maxlength="128" placeholder="Firebase User UID" />
        </div>
        <div class="field">
          <label for="adminCatalogBookingId">${escapeHtml(t("portalLinkedBookingLabel"))} (Optional)</label>
          <input id="adminCatalogBookingId" class="input" maxlength="128" placeholder="Booking Document ID" />
        </div>
        <div class="field">
          <label for="adminCatalogTitle">${escapeHtml(t("adminPortalCatalogTitleLabel"))}</label>
          <input id="adminCatalogTitle" class="input" required minlength="2" maxlength="140" placeholder="e.g. Wedding Album Selection Catalog" />
        </div>
        <div class="field">
          <label for="adminCatalogCategory">${escapeHtml(t("adminPortalCategoryLabel"))}</label>
          <input id="adminCatalogCategory" class="input" maxlength="80" value="Private Selection" />
        </div>
        <div class="field full">
          <label for="adminCatalogDescription">${escapeHtml(t("adminPortalDescriptionLabel"))}</label>
          <textarea id="adminCatalogDescription" class="input" rows="2" maxlength="1000"></textarea>
        </div>
        <div class="field full">
          <label for="adminCatalogItems">${escapeHtml(t("adminPortalCatalogItemsLabel"))}</label>
          <textarea id="adminCatalogItems" class="input" rows="3" placeholder="Bridal Portrait Set | https://...&#10;4K Teaser Cut | https://..."></textarea>
        </div>
        <div class="field full">
          <button type="submit" class="btn-gold">${escapeHtml(t("adminPortalAssignCatalogBtn"))}</button>
        </div>
      </form>
    </div>

    <div class="studio-card">
      <h3 style="margin:0 0 6px;color:var(--gold);">${escapeHtml(t("adminPortalSlotAssignTitle"))}</h3>
      <p style="margin:0 0 12px;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("adminPortalSlotAssignDesc"))}</p>
      <form id="adminAssignSlotForm" class="form-grid two-col">
        <div class="field full">
          <label for="adminSlotQuickPick">${escapeHtml(t("adminPortalSelectBookingLabel"))}</label>
          <select id="adminSlotQuickPick" class="input">
            <option value="">-- ${escapeHtml(t("adminPortalPickCustomerHint"))} --</option>
            ${customerOptionsHtml}
          </select>
        </div>
        <div class="field">
          <label for="adminSlotCustomerUid">${escapeHtml(t("adminPortalCustomerUidLabel"))}</label>
          <input id="adminSlotCustomerUid" class="input" required maxlength="128" placeholder="Firebase User UID" />
        </div>
        <div class="field">
          <label for="adminSlotBookingId">${escapeHtml(t("portalLinkedBookingLabel"))} (Optional)</label>
          <input id="adminSlotBookingId" class="input" maxlength="128" placeholder="Booking Document ID" />
        </div>
        <div class="field">
          <label for="adminSlotEventTitle">${escapeHtml(t("adminPortalEventTitleLabel"))}</label>
          <input id="adminSlotEventTitle" class="input" required minlength="2" maxlength="140" placeholder="e.g. Royal Bridal Shoot" />
        </div>
        <div class="field">
          <label for="adminSlotApprovalStatus">${escapeHtml(t("portalTabApprovalStatus"))}</label>
          <select id="adminSlotApprovalStatus" class="input">
            ${APPROVAL_STATUS_VALUES.map((st) => `<option value="${st}">${st}</option>`).join("")}
          </select>
        </div>
        <div class="field">
          <label for="adminSlotDate">${escapeHtml(t("portalEventDateLabel"))}</label>
          <input id="adminSlotDate" class="input" type="date" required />
        </div>
        <div class="field">
          <label for="adminSlotTime">${escapeHtml(t("portalEventTimeLabel"))}</label>
          <input id="adminSlotTime" class="input" required maxlength="50" placeholder="10:30 AM – 01:30 PM" />
        </div>
        <div class="field">
          <label for="adminSlotLabel">${escapeHtml(t("portalAssignedSlotLabel"))}</label>
          <input id="adminSlotLabel" class="input" required maxlength="120" placeholder="Studio Bay A • Morning Slot" />
        </div>
        <div class="field">
          <label for="adminSlotLocation">${escapeHtml(t("portalLocationLabel"))}</label>
          <input id="adminSlotLocation" class="input" maxlength="200" value="Star Artist Studio, Talwara Khurd" />
        </div>
        <div class="field full">
          <label for="adminSlotCustomerNote">${escapeHtml(t("adminPortalCustomerNoteLabel"))}</label>
          <textarea id="adminSlotCustomerNote" class="input" rows="2" maxlength="500"></textarea>
        </div>
        <div class="field full">
          <button type="submit" class="btn-gold">${escapeHtml(t("adminPortalAssignSlotBtn"))}</button>
        </div>
      </form>
    </div>

    <div class="studio-card">
      <h4 style="margin:0 0 10px;color:var(--gold);">${escapeHtml(t("adminPortalAssignedCatalogsList"))} (${catalogs.length})</h4>
      ${
        catalogs.length
          ? catalogs
              .map(
                (c) => `
            <div class="portal-mini-row">
              <div>
                <strong>${escapeHtml(c.title || "")}</strong>
                <div style="font-size:0.8rem;color:var(--text-secondary);">UID: ${escapeHtml(c.customerUid || "")} • ${escapeHtml(c.category || "")} • ${escapeHtml(formatNetworkTimestamp(c.updatedAt || c.createdAt, lang))}</div>
              </div>
              <button type="button" class="btn-outline btn-xs" data-admin-delete-catalog="${escapeHtml(c.id || "")}">${escapeHtml(t("deleteBtn") || "Delete")}</button>
            </div>`
              )
              .join("")
          : `<p style="margin:0;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("adminPortalNoAssignedCatalogs"))}</p>`
      }
    </div>

    <div class="studio-card">
      <h4 style="margin:0 0 10px;color:var(--gold);">${escapeHtml(t("adminPortalAssignedSlotsList"))} (${slots.length})</h4>
      ${
        slots.length
          ? slots
              .map(
                (s) => `
            <div class="portal-mini-row">
              <div>
                <strong>${escapeHtml(s.eventTitle || "")}</strong> — ${escapeHtml(s.slotLabel || "")}
                <div style="font-size:0.8rem;color:var(--text-secondary);">UID: ${escapeHtml(s.customerUid || "")} • ${escapeHtml(s.eventDate || "")} ${escapeHtml(s.eventTime || "")}</div>
              </div>
              <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
                <select class="input" style="min-height:36px;padding:4px 8px;width:auto;" data-admin-slot-status="${escapeHtml(s.id || "")}">
                  ${APPROVAL_STATUS_VALUES.map((st) => `<option value="${st}" ${s.approvalStatus === st ? "selected" : ""}>${st}</option>`).join("")}
                </select>
                <button type="button" class="btn-outline btn-xs" data-admin-delete-slot="${escapeHtml(s.id || "")}">${escapeHtml(t("deleteBtn") || "Delete")}</button>
              </div>
            </div>`
              )
              .join("")
          : `<p style="margin:0;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("adminPortalNoAssignedSlots"))}</p>`
      }
    </div>
  `;
}
