import { STUDIO_CONTACT } from "./data.js";
import { buildAdminEmailPreview, escapeEmailHtml } from "./admin-email-preview.js";

export const LOCKED_ADMIN_ALERT_RECIPIENT = String(
  STUDIO_CONTACT.adminEmail || "chanderstudio0@gmail.com"
)
  .trim()
  .toLowerCase();

export const DEFAULT_ADMIN_EMAIL_ALERT_SETTINGS = Object.freeze({
  bookingAlertsEnabled: true,
  modelAlertsEnabled: true,
  companyAlertsEnabled: true,
  browserAlertsEnabled: true,
  recipientEmail: LOCKED_ADMIN_ALERT_RECIPIENT
});

export function normalizeAdminEmailAlertSettings(raw = {}) {
  const src = raw && typeof raw === "object" ? raw : {};
  return {
    bookingAlertsEnabled: src.bookingAlertsEnabled !== false,
    modelAlertsEnabled: src.modelAlertsEnabled !== false,
    companyAlertsEnabled: src.companyAlertsEnabled !== false,
    browserAlertsEnabled: src.browserAlertsEnabled !== false,
    recipientEmail: LOCKED_ADMIN_ALERT_RECIPIENT
  };
}

/**
 * Detects genuinely new booking or 18+ model application records that arrived
 * after the initial Admin Dashboard snapshot (prevents false alerts on first load).
 */
export function detectNewAdminAlertItems({ prevIds = null, items = [] } = {}) {
  const list = Array.isArray(items) ? items : [];
  const nextIds = new Set(list.map((item) => String(item?.id || "").trim()).filter(Boolean));
  if (!(prevIds instanceof Set)) {
    return { newItems: [], nextIds, initialized: true };
  }
  const newItems = list.filter((item) => {
    const id = String(item?.id || "").trim();
    return id && !prevIds.has(id);
  });
  return { newItems, nextIds, initialized: false };
}

/**
 * Builds a sanitized, recipient-locked automated alert log record for a booking
 * request or 18+ model application.
 */
export function buildAdminEmailAlertRecord({
  type = "booking",
  record = {},
  status = "queued",
  source = "dashboard-live",
  messageId = "",
  errorMessage = "",
  overrides = {}
} = {}) {
  const safeType = type === "model" ? "model" : type === "company" ? "company" : "booking";
  const preview = buildAdminEmailPreview({
    type: safeType,
    record: record && typeof record === "object" ? record : {},
    overrides
  });
  const recordId =
    String(record?.id || `${safeType}_${Date.now()}`)
      .replace(/[^a-zA-Z0-9_-]/g, "")
      .slice(0, 128) || `${safeType}_item`;

  let summary = "";
  if (safeType === "model") {
    summary = `${record?.fullName || "Model Applicant"} (${record?.age || "18+"} yrs) • ${record?.city || "India"}`;
  } else if (safeType === "company") {
    summary = `${record?.companyName || "Company"} • ${record?.modelsCount || 1} ${record?.modelRequirement || "Models"}`;
  } else {
    summary = `${record?.clientName || "Client"} • ${record?.serviceType || "Studio Shoot"} (${record?.preferredDate || "TBD"})`;
  }

  return {
    type: safeType,
    recordId,
    recipientEmail: LOCKED_ADMIN_ALERT_RECIPIENT,
    subject: String(preview.subject || "Studio Admin Alert").slice(0, 200),
    summary: summary.trim().slice(0, 500),
    textBody: String(preview.text || ""),
    status: ["queued", "sent", "failed", "skipped", "acknowledged"].includes(status) ? status : "queued",
    source: ["cloud-function", "dashboard-live", "manual-resend"].includes(source) ? source : "dashboard-live",
    messageId: String(messageId || "").slice(0, 200),
    errorMessage: String(errorMessage || "").slice(0, 300),
    isRead: false
  };
}

/**
 * Builds a safe mailto: fallback URL locked strictly to the authorized Studio Admin Gmail
 * so the administrator can dispatch any alert in one click even on the Spark Free Plan.
 */
export function buildAdminMailtoFallbackUrl({ subject = "", text = "" } = {}) {
  const cleanSubject = String(subject || "STAR ARTIST STUDIO Admin Alert").replace(/[\r\n]+/g, " ").trim().slice(0, 180);
  const cleanBody = String(text || "").trim().slice(0, 1400);
  return `mailto:${LOCKED_ADMIN_ALERT_RECIPIENT}?subject=${encodeURIComponent(cleanSubject)}&body=${encodeURIComponent(cleanBody)}`;
}

export function summarizeAdminEmailAlerts({ alerts = [], settings = DEFAULT_ADMIN_EMAIL_ALERT_SETTINGS } = {}) {
  const list = Array.isArray(alerts) ? alerts : [];
  const cfg = normalizeAdminEmailAlertSettings(settings);
  let sent = 0;
  let queued = 0;
  let failed = 0;
  let skipped = 0;
  let unread = 0;
  let bookingCount = 0;
  let modelCount = 0;

  for (const item of list) {
    const st = String(item?.status || "queued").toLowerCase();
    if (st === "sent") sent += 1;
    else if (st === "failed") failed += 1;
    else if (st === "skipped") skipped += 1;
    else queued += 1;

    if (item?.isRead === false) unread += 1;
    if (item?.type === "model") modelCount += 1;
    else if (item?.type === "booking") bookingCount += 1;
  }

  return {
    total: list.length,
    sent,
    queued,
    failed,
    skipped,
    unread,
    bookingCount,
    modelCount,
    activeCount: (cfg.bookingAlertsEnabled ? 1 : 0) + (cfg.modelAlertsEnabled ? 1 : 0)
  };
}

export function filterAdminEmailAlerts(alerts = [], filter = "all") {
  const list = Array.isArray(alerts) ? alerts : [];
  if (filter === "booking") return list.filter((a) => a?.type === "booking");
  if (filter === "model") return list.filter((a) => a?.type === "model");
  if (filter === "unread") return list.filter((a) => a?.isRead === false);
  return list;
}

function formatAlertDate(value) {
  if (!value) return "Just now";
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
  if (!date || Number.isNaN(date.getTime())) return "Just now";
  try {
    return new Intl.DateTimeFormat("en-IN", {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: "Asia/Kolkata"
    }).format(date);
  } catch {
    return date.toISOString();
  }
}

export function renderAdminEmailAlertsSectionHtml({
  settings = DEFAULT_ADMIN_EMAIL_ALERT_SETTINGS,
  alerts = [],
  recentBookings = [],
  recentModels = [],
  filter = "all",
  isSavingSettings = false,
  dispatchingKey = "",
  t = (k) => k,
  escapeHtml = escapeEmailHtml
} = {}) {
  const cfg = normalizeAdminEmailAlertSettings(settings);
  const summary = summarizeAdminEmailAlerts({ alerts, settings: cfg });
  const visibleAlerts = filterAdminEmailAlerts(alerts, filter);
  const isActive = cfg.bookingAlertsEnabled || cfg.modelAlertsEnabled;

  // Build quick-trigger rows from the latest booking & model submissions if audit log is still empty
  const recentBooking = Array.isArray(recentBookings) && recentBookings.length ? recentBookings[0] : null;
  const recentModel = Array.isArray(recentModels) && recentModels.length ? recentModels[0] : null;

  const statusMeta = (status) => {
    const clean = String(status || "queued").toLowerCase();
    if (clean === "sent") return { label: t("adminEmailAlertStatusSent"), cls: "is-sent" };
    if (clean === "failed") return { label: t("adminEmailAlertStatusFailed"), cls: "is-failed" };
    if (clean === "skipped") return { label: t("adminEmailAlertStatusSkipped"), cls: "is-skipped" };
    if (clean === "acknowledged") return { label: t("adminEmailAlertStatusAck"), cls: "is-sent" };
    return { label: t("adminEmailAlertStatusQueued"), cls: "is-queued" };
  };

  const alertsListHtml = visibleAlerts.length
    ? visibleAlerts
        .map((alert) => {
          const sm = statusMeta(alert.status);
          const typeLabel =
            alert.type === "model"
              ? t("adminEmailAlertTypeModel")
              : alert.type === "company"
              ? t("adminEmailAlertTypeCompany")
              : t("adminEmailAlertTypeBooking");
          const isUnread = alert.isRead === false;
          const dispatchKey = `${alert.type}:${alert.recordId}`;
          const isSending = dispatchingKey === dispatchKey;
          const mailtoUrl = buildAdminMailtoFallbackUrl({
            subject: alert.subject,
            text: `${alert.summary || ""}\n\nOpen Admin Dashboard: https://gen-lang-client-0548683163.web.app/#admin`
          });
          return `
          <article class="studio-card admin-alert-item ${isUnread ? "is-unread" : ""}" data-admin-alert-id="${escapeHtml(alert.id || alert.recordId)}">
            <div class="admin-alert-item-head">
              <div class="admin-alert-item-badges">
                <span class="badge">${escapeHtml(typeLabel)}</span>
                <span class="admin-alert-status-pill ${sm.cls}">${escapeHtml(sm.label)}</span>
                ${isUnread ? `<span class="admin-alert-unread-dot" aria-label="Unread alert">${escapeHtml(t("adminEmailAlertNewPill"))}</span>` : ""}
              </div>
              <span class="admin-alert-time">${escapeHtml(formatAlertDate(alert.createdAt || alert.updatedAt))}</span>
            </div>
            <strong class="admin-alert-subject">${escapeHtml(alert.subject || "Studio Admin Alert")}</strong>
            ${alert.summary ? `<p class="admin-alert-summary">${escapeHtml(alert.summary)}</p>` : ""}
            <div class="admin-alert-meta">
              <span>${escapeHtml(t("adminEmailAlertRecipientLabel"))}: <strong>${escapeHtml(LOCKED_ADMIN_ALERT_RECIPIENT)}</strong></span>
              ${alert.messageId ? `<span>ID: <code>${escapeHtml(alert.messageId)}</code></span>` : ""}
              ${alert.errorMessage ? `<span class="admin-alert-error-text">${escapeHtml(alert.errorMessage)}</span>` : ""}
            </div>
            <div class="admin-alert-actions">
              <button
                type="button"
                class="chip-btn"
                data-admin-alert-resend-type="${escapeHtml(alert.type)}"
                data-admin-alert-resend-id="${escapeHtml(alert.recordId)}"
                ${isSending ? "disabled" : ""}
              >${escapeHtml(isSending ? t("adminEmailAlertSendingBtn") : t("adminEmailAlertResendBtn"))}</button>
              <a class="chip-btn" href="${escapeHtml(mailtoUrl)}">${escapeHtml(t("adminEmailAlertMailtoBtn"))}</a>
              <button
                type="button"
                class="chip-btn"
                data-open-email-preview-type="${escapeHtml(alert.type)}"
                data-open-email-preview-id="${escapeHtml(alert.recordId)}"
              >${escapeHtml(t("adminEmailPreviewQuickBtn"))}</button>
              ${
                isUnread && alert.id
                  ? `<button type="button" class="chip-btn" data-admin-alert-mark-read="${escapeHtml(alert.id)}">${escapeHtml(t("adminEmailAlertMarkReadBtn"))}</button>`
                  : ""
              }
            </div>
          </article>`;
        })
        .join("")
    : `<div class="studio-card admin-alert-empty">
        <p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("adminEmailAlertsEmptyDesc"))}</p>
        <div class="admin-alert-actions" style="margin-top:12px;">
          ${
            recentBooking
              ? `<button type="button" class="chip-btn" data-admin-alert-resend-type="booking" data-admin-alert-resend-id="${escapeHtml(recentBooking.id)}">${escapeHtml(t("adminEmailAlertSendLatestBookingBtn"))}</button>`
              : ""
          }
          ${
            recentModel
              ? `<button type="button" class="chip-btn" data-admin-alert-resend-type="model" data-admin-alert-resend-id="${escapeHtml(recentModel.id)}">${escapeHtml(t("adminEmailAlertSendLatestModelBtn"))}</button>`
              : ""
          }
        </div>
      </div>`;

  return `
    <section id="adminEmailAlertsPanel" class="studio-card admin-email-alerts-panel" aria-labelledby="adminEmailAlertsHeading">
      <div class="admin-alerts-header">
        <div>
          <span class="eyebrow">${escapeHtml(t("adminEmailAlertsEyebrow"))}</span>
          <h3 id="adminEmailAlertsHeading" style="margin:6px 0 4px;color:var(--gold);">${escapeHtml(t("adminEmailAlertsTitle"))}</h3>
          <p style="margin:0;color:var(--text-secondary);font-size:0.9rem;">${escapeHtml(t("adminEmailAlertsSubtitle"))}</p>
        </div>
        <div class="admin-alerts-status-box">
          <span class="admin-alert-live-pill ${isActive ? "is-active" : "is-paused"}">
            ${escapeHtml(isActive ? t("adminEmailAlertsActivePill") : t("adminEmailAlertsPausedPill"))}
          </span>
          <span class="admin-alert-recipient-chip">🔒 ${escapeHtml(LOCKED_ADMIN_ALERT_RECIPIENT)}</span>
        </div>
      </div>

      <div class="admin-alerts-kpis">
        <div class="admin-alert-kpi">
          <strong>${summary.total}</strong>
          <span>${escapeHtml(t("adminEmailAlertsKpiTotal"))}</span>
        </div>
        <div class="admin-alert-kpi">
          <strong>${summary.sent}</strong>
          <span>${escapeHtml(t("adminEmailAlertsKpiSent"))}</span>
        </div>
        <div class="admin-alert-kpi">
          <strong>${summary.queued}</strong>
          <span>${escapeHtml(t("adminEmailAlertsKpiQueued"))}</span>
        </div>
        <div class="admin-alert-kpi">
          <strong>${summary.unread}</strong>
          <span>${escapeHtml(t("adminEmailAlertsKpiUnread"))}</span>
        </div>
      </div>

      <form id="adminEmailAlertSettingsForm" class="admin-alerts-toggles-card">
        <div class="admin-alerts-toggles-grid">
          <label class="admin-alert-toggle-row">
            <input
              id="adminAlertToggleBooking"
              type="checkbox"
              data-admin-alert-setting="bookingAlertsEnabled"
              ${cfg.bookingAlertsEnabled ? "checked" : ""}
            />
            <span>
              <strong>${escapeHtml(t("adminEmailAlertToggleBookingTitle"))}</strong>
              <small>${escapeHtml(t("adminEmailAlertToggleBookingDesc"))}</small>
            </span>
          </label>
          <label class="admin-alert-toggle-row">
            <input
              id="adminAlertToggleModel"
              type="checkbox"
              data-admin-alert-setting="modelAlertsEnabled"
              ${cfg.modelAlertsEnabled ? "checked" : ""}
            />
            <span>
              <strong>${escapeHtml(t("adminEmailAlertToggleModelTitle"))}</strong>
              <small>${escapeHtml(t("adminEmailAlertToggleModelDesc"))}</small>
            </span>
          </label>
          <label class="admin-alert-toggle-row">
            <input
              id="adminAlertToggleBrowser"
              type="checkbox"
              data-admin-alert-setting="browserAlertsEnabled"
              ${cfg.browserAlertsEnabled ? "checked" : ""}
            />
            <span>
              <strong>${escapeHtml(t("adminEmailAlertToggleBrowserTitle"))}</strong>
              <small>${escapeHtml(t("adminEmailAlertToggleBrowserDesc"))}</small>
            </span>
          </label>
        </div>
        <div class="admin-alerts-settings-footer">
          <button
            id="saveAdminEmailAlertSettingsBtn"
            type="submit"
            class="btn-gold btn-sm"
            ${isSavingSettings ? "disabled" : ""}
          >${escapeHtml(isSavingSettings ? t("adminEmailAlertsSavingBtn") : t("adminEmailAlertsSaveBtn"))}</button>
          <button
            type="button"
            class="btn-outline btn-sm"
            data-open-email-preview-type="booking"
            data-open-email-preview-id="__sample__"
          >${escapeHtml(t("adminEmailAlertsOpenTemplatesBtn"))}</button>
        </div>
      </form>

      <div class="admin-alerts-toolbar">
        <div class="filter-bar" style="margin:0;">
          <button type="button" class="chip-btn ${filter === "all" ? "active" : ""}" data-admin-alert-filter="all">${escapeHtml(t("filterAll"))} (${summary.total})</button>
          <button type="button" class="chip-btn ${filter === "booking" ? "active" : ""}" data-admin-alert-filter="booking">${escapeHtml(t("adminTabBookings"))} (${summary.bookingCount})</button>
          <button type="button" class="chip-btn ${filter === "model" ? "active" : ""}" data-admin-alert-filter="model">${escapeHtml(t("adminTabModels"))} (${summary.modelCount})</button>
          <button type="button" class="chip-btn ${filter === "unread" ? "active" : ""}" data-admin-alert-filter="unread">${escapeHtml(t("adminEmailAlertsFilterUnread"))} (${summary.unread})</button>
        </div>
        ${
          summary.unread > 0
            ? `<button id="adminAlertMarkAllReadBtn" type="button" class="btn-outline btn-sm" data-admin-alert-mark-all-read="true">${escapeHtml(t("adminEmailAlertsMarkAllReadBtn"))}</button>`
            : ""
        }
      </div>

      <div id="adminEmailAlertsFeed" class="admin-alerts-feed">
        ${alertsListHtml}
      </div>
    </section>
  `;
}
