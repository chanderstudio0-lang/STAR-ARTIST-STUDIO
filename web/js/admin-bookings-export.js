import { normalizeApplicationDate } from "./admin-model-growth.js";

export const BOOKING_CSV_COLUMNS = [
  { key: "id", header: "Booking ID" },
  { key: "clientName", header: "Client Name" },
  { key: "contactNumber", header: "Contact Number" },
  { key: "serviceType", header: "Service Type" },
  { key: "preferredDate", header: "Preferred Date" },
  { key: "preferredTime", header: "Preferred Time" },
  { key: "location", header: "Location" },
  { key: "status", header: "Status" },
  { key: "notes", header: "Notes" },
  { key: "createdAt", header: "Created At" }
];

const FORMULA_TRIGGER_RE = /^[\t\r ]*[=+\-@]/;

function defaultEscapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

/**
 * Formats Firestore Timestamps, serialized timestamps, Dates, or ISO strings
 * as a clean UTC timestamp string for CSV cells.
 */
export function formatBookingCreatedAt(value) {
  const dt = normalizeApplicationDate(value);
  if (!dt) return "";
  const yyyy = dt.getUTCFullYear();
  const mm = String(dt.getUTCMonth() + 1).padStart(2, "0");
  const dd = String(dt.getUTCDate()).padStart(2, "0");
  const hh = String(dt.getUTCHours()).padStart(2, "0");
  const mi = String(dt.getUTCMinutes()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd} ${hh}:${mi} UTC`;
}

/**
 * Escapes a single CSV field per RFC 4180 and neutralizes spreadsheet formula
 * injection so untrusted client inputs open safely in Excel / Google Sheets.
 */
export function sanitizeCsvCell(value) {
  let str = value === null || value === undefined ? "" : String(value);
  str = str.replace(/\r\n/g, "\n").replace(/\r/g, "\n");
  if (FORMULA_TRIGGER_RE.test(str)) {
    str = `'${str}`;
  }
  if (/[",\n]/.test(str)) {
    return `"${str.replaceAll('"', '""')}"`;
  }
  return str;
}

/**
 * Converts an array of booking documents into an RFC 4180 UTF-8 CSV string.
 */
export function buildBookingsCsv(bookings = [], { includeBom = true } = {}) {
  const rows = Array.isArray(bookings) ? bookings : [];
  const headerLine = BOOKING_CSV_COLUMNS.map((col) => sanitizeCsvCell(col.header)).join(",");
  const dataLines = rows.map((booking) =>
    BOOKING_CSV_COLUMNS.map((col) => {
      if (col.key === "createdAt") {
        return sanitizeCsvCell(formatBookingCreatedAt(booking?.createdAt));
      }
      return sanitizeCsvCell(booking?.[col.key] ?? "");
    }).join(",")
  );
  const body = [headerLine, ...dataLines].join("\r\n");
  return includeBom ? `\uFEFF${body}` : body;
}

/**
 * Generates a dated CSV filename for the Studio bookings export.
 */
export function buildBookingsCsvFileName(now = new Date()) {
  const dt = normalizeApplicationDate(now) || new Date();
  const yyyy = dt.getUTCFullYear();
  const mm = String(dt.getUTCMonth() + 1).padStart(2, "0");
  const dd = String(dt.getUTCDate()).padStart(2, "0");
  return `star-artist-studio-bookings-${yyyy}-${mm}-${dd}.csv`;
}

/**
 * Triggers a browser download of all current bookings as a UTF-8 CSV file.
 */
export function exportBookingsToCsv(
  bookings = [],
  {
    doc = typeof document !== "undefined" ? document : null,
    BlobCtor = typeof Blob !== "undefined" ? Blob : null,
    urlApi = typeof URL !== "undefined" ? URL : null,
    now = new Date()
  } = {}
) {
  const list = Array.isArray(bookings) ? bookings : [];
  if (!list.length) {
    return { status: "empty", count: 0, fileName: "", csv: "" };
  }

  const csv = buildBookingsCsv(list, { includeBom: true });
  const fileName = buildBookingsCsvFileName(now);

  if (!doc || !BlobCtor || !urlApi || typeof urlApi.createObjectURL !== "function") {
    return { status: "unsupported", count: list.length, fileName, csv };
  }

  const blob = new BlobCtor([csv], { type: "text/csv;charset=utf-8;" });
  const objectUrl = urlApi.createObjectURL(blob);
  const link = doc.createElement("a");
  link.href = objectUrl;
  link.download = fileName;
  if (link.style) {
    link.style.display = "none";
  }
  doc.body?.appendChild?.(link);
  link.click();
  if (typeof link.remove === "function") {
    link.remove();
  } else {
    doc.body?.removeChild?.(link);
  }
  if (typeof urlApi.revokeObjectURL === "function") {
    setTimeout(() => urlApi.revokeObjectURL(objectUrl), 1500);
  }

  return { status: "exported", count: list.length, fileName, csv };
}

/**
 * Builds the inner icon + label markup for the Admin Bookings CSV export button
 * across idle, processing, and done feedback states.
 */
export function getAdminCsvButtonMarkup(
  status = "idle",
  { t = (k) => k, escapeHtml = defaultEscapeHtml } = {}
) {
  if (status === "processing") {
    return `
      <span class="admin-export-csv-spinner" aria-hidden="true"></span>
      <span class="admin-export-csv-label">${escapeHtml(t("adminExportCsvLoadingLabel"))}</span>`;
  }
  if (status === "done") {
    return `
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <polyline points="20 6 9 17 4 12"></polyline>
      </svg>
      <span class="admin-export-csv-label">${escapeHtml(t("adminExportCsvDoneLabel"))}</span>`;
  }
  return `
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
      <polyline points="7 10 12 15 17 10"></polyline>
      <line x1="12" y1="15" x2="12" y2="3"></line>
    </svg>
    <span class="admin-export-csv-label">${escapeHtml(t("adminExportCsvBtn"))}</span>`;
}

/**
 * Updates an existing #adminExportBookingsCsvBtn element in-place so the
 * loading animation and confirmation text animate without re-rendering the tab.
 */
export function applyAdminCsvButtonState(
  buttonEl,
  status = "idle",
  { count = 0, t = (k) => k, escapeHtml = defaultEscapeHtml } = {}
) {
  if (!buttonEl) return;
  const safeCount = Math.max(0, Number(count) || 0);
  const normalized = status === "processing" || status === "done" ? status : "idle";
  const isProcessing = normalized === "processing";
  const isDone = normalized === "done";

  buttonEl.disabled = safeCount === 0 || isProcessing;
  buttonEl.setAttribute?.("aria-busy", isProcessing ? "true" : "false");
  buttonEl.setAttribute?.("data-export-status", normalized);
  buttonEl.classList?.toggle?.("is-loading", isProcessing);
  buttonEl.classList?.toggle?.("is-exported", isDone);
  buttonEl.innerHTML = getAdminCsvButtonMarkup(normalized, { t, escapeHtml });
}

/**
 * Runs the CSV export with temporary processing and confirmation button states
 * so admins receive clear visual feedback while booking rows are packaged.
 */
export async function runAdminBookingsCsvExport(
  bookings = [],
  {
    onStateChange = () => {},
    minProcessingMs = 450,
    exportedHoldMs = 1400,
    delayFn = (ms) => new Promise((resolve) => setTimeout(resolve, ms)),
    exportOptions = {}
  } = {}
) {
  const list = Array.isArray(bookings) ? bookings : [];
  if (!list.length) {
    onStateChange("idle", null);
    return { status: "empty", count: 0, fileName: "", csv: "" };
  }

  onStateChange("processing", null);
  try {
    if (minProcessingMs > 0) {
      await delayFn(minProcessingMs);
    }
    const result = exportBookingsToCsv(list, exportOptions);
    if (result.status === "exported") {
      onStateChange("done", result);
      if (exportedHoldMs > 0) {
        await delayFn(exportedHoldMs);
      }
      onStateChange("idle", result);
    } else {
      onStateChange("idle", result);
    }
    return result;
  } catch (err) {
    onStateChange("idle", null);
    throw err;
  }
}

/**
 * Renders the Admin Bookings tab toolbar with the Export CSV action button.
 */
export function renderAdminBookingsToolbarHtml({
  count = 0,
  status = "idle",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const safeCount = Math.max(0, Number(count) || 0);
  const normalized = status === "processing" || status === "done" ? status : "idle";
  const isProcessing = normalized === "processing";
  const isDone = normalized === "done";
  const disabledAttr = safeCount === 0 || isProcessing ? "disabled" : "";
  const stateClasses = [
    "btn-gold",
    "admin-export-csv-btn",
    isProcessing ? "is-loading" : "",
    isDone ? "is-exported" : ""
  ]
    .filter(Boolean)
    .join(" ");

  return `
    <div class="studio-card admin-bookings-toolbar">
      <div class="admin-bookings-toolbar-copy">
        <strong>${escapeHtml(t("adminBookingsToolbarTitle"))}</strong>
        <span>${escapeHtml(t("adminBookingsToolbarDesc")).replace("{count}", String(safeCount))}</span>
      </div>
      <button
        id="adminExportBookingsCsvBtn"
        type="button"
        class="${stateClasses}"
        data-adm-export-bookings-csv
        data-export-status="${normalized}"
        aria-busy="${isProcessing ? "true" : "false"}"
        aria-live="polite"
        ${disabledAttr}
      >
        ${getAdminCsvButtonMarkup(normalized, { t, escapeHtml })}
      </button>
    </div>`;
}
