export const BOOKING_STATUS_ORDER = ["Confirmed", "Pending", "Completed", "Cancelled"];

const STATUS_ICONS = {
  confirmed:
    '<svg class="badge-icon" viewBox="0 0 16 16" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="8" cy="8" r="6.25"/><path d="m5.5 8.2 1.7 1.7 3.4-3.6"/></svg>',
  pending:
    '<svg class="badge-icon" viewBox="0 0 16 16" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="8" cy="8" r="6.25"/><path d="M8 4.8V8l2.2 1.4"/></svg>',
  completed:
    '<svg class="badge-icon" viewBox="0 0 16 16" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12.8 4.6 6.8 10.6 4 7.8"/><path d="m14.2 8.1-4.8 4.8-1.1-1.1"/></svg>',
  cancelled:
    '<svg class="badge-icon" viewBox="0 0 16 16" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="8" cy="8" r="6.25"/><path d="m5.7 5.7 4.6 4.6m0-4.6-4.6 4.6"/></svg>'
};

const STATUS_LABELS = {
  en: {
    confirmed: "Confirmed",
    pending: "Pending",
    completed: "Completed",
    cancelled: "Cancelled"
  },
  hi: {
    confirmed: "पुष्ट (Confirmed)",
    pending: "लंबित (Pending)",
    completed: "पूर्ण (Completed)",
    cancelled: "रद्द (Cancelled)"
  }
};

function escapeAttr(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function normalizeBookingStatus(rawStatus) {
  const normalized = String(rawStatus ?? "")
    .trim()
    .toLowerCase();
  if (normalized === "confirmed" || normalized === "approved") {
    return "Confirmed";
  }
  if (normalized === "completed" || normalized === "done") {
    return "Completed";
  }
  if (normalized === "cancelled" || normalized === "canceled" || normalized === "rejected") {
    return "Cancelled";
  }
  return "Pending";
}

export function getBookingStatusMeta(rawStatus, lang = "en") {
  const canonical = normalizeBookingStatus(rawStatus);
  const key = canonical.toLowerCase();
  const locale = lang === "hi" ? "hi" : "en";
  const label = STATUS_LABELS[locale][key] || canonical;
  const ariaLabel = locale === "hi" ? `सत्र स्थिति: ${label}` : `Session status: ${label}`;
  return {
    canonical,
    key,
    cssClass: `status-${key}`,
    cardClass: `booking-card-${key}`,
    label,
    iconSvg: STATUS_ICONS[key] || STATUS_ICONS.pending,
    ariaLabel
  };
}

export function summarizeBookingStatuses(bookings = []) {
  const summary = {
    total: 0,
    confirmed: 0,
    pending: 0,
    completed: 0,
    cancelled: 0
  };
  for (const booking of bookings) {
    summary.total += 1;
    const key = normalizeBookingStatus(booking?.status).toLowerCase();
    if (key in summary) {
      summary[key] += 1;
    }
  }
  return summary;
}

export function filterBookingsByStatus(bookings = [], activeFilter = "all") {
  const normalizedFilter = String(activeFilter || "all").trim().toLowerCase();
  if (!normalizedFilter || normalizedFilter === "all") {
    return bookings;
  }
  return bookings.filter(
    (booking) => normalizeBookingStatus(booking?.status).toLowerCase() === normalizedFilter
  );
}

export function renderBookingStatusBadgeHtml(rawStatus, lang = "en") {
  const meta = getBookingStatusMeta(rawStatus, lang);
  const dotHtml =
    meta.key === "pending"
      ? '<span class="status-dot-pulse" aria-hidden="true"></span>'
      : "";
  return `<span class="badge status-badge ${meta.cssClass}" data-booking-status="${meta.key}" role="status" aria-label="${escapeAttr(meta.ariaLabel)}">${meta.iconSvg}${dotHtml}<span>${escapeAttr(meta.label)}</span></span>`;
}
