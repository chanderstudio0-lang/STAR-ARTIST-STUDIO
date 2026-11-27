/**
 * STAR ARTIST STUDIO — UPI Advance Payment & Wedding Booking Payment System
 *
 * Security & Privacy Guarantees:
 *  - Never invents a UPI ID or payment QR code; stays disabled until Admin configures valid details.
 *  - Strictly supports all 8 payment statuses without ever auto-verifying from UTR/screenshots.
 *  - Only authorized Admin can verify payments, set prices/charges, reject submissions, or process refunds.
 *  - Actively blocks storing UPI PINs, passwords, OTPs, card numbers, CVVs, or banking credentials.
 *  - Strictly filters customer portal records by authenticated UID and strips internal Admin fields.
 */

import { formatNetworkTimestamp } from "./pro-network.js";

export const PAYMENT_STATUS_LIST = [
  "Payment Pending",
  "Submitted for Verification",
  "Payment Verified",
  "Payment Rejected",
  "Partially Paid",
  "Fully Paid",
  "Refund Requested",
  "Refund Processed"
];

export const MODEL_BOOKING_SERVICES = [
  "Fashion Editorial Shoot",
  "Brand Campaign / Commercial",
  "Bridal / Ethnic Lookbook",
  "Runway / Live Event",
  "Music Video Feature",
  "Studio Portfolio Collaboration"
];

export const WEDDING_EVENT_TYPES = [
  "Wedding Photography & Videography",
  "Pre-Wedding Shoot",
  "Wedding Photography",
  "Wedding Videography",
  "Event Photography",
  "Event Videography"
];

export const OFFICIAL_STUDIO_UPI_ID = "9812890081@ybl";

export const DUPLICATE_UTR_ERROR_MESSAGE =
  "This UTR has already been submitted. Please check your transaction reference number.";

export const DEFAULT_PAYMENT_CONFIG = Object.freeze({
  enabled: false,
  upiId: OFFICIAL_STUDIO_UPI_ID,
  payeeName: "STAR ARTIST STUDIO",
  qrImageDataUrl: "",
  qrVerified: false,
  instructions: "",
  modelAdvanceMode: "percentage",
  modelAdvanceValue: 30,
  weddingChargeMode: "percentage",
  weddingChargeValue: 25
});

const STRIPPED_CUSTOMER_PAYMENT_KEYS = new Set([
  "adminNotes",
  "verifiedByAdminEmail"
]);

function defaultEscapeHtml(str) {
  return String(str ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function formatInrAmount(value) {
  const num = Math.max(0, Math.round(Number(value) || 0));
  return `₹${num.toLocaleString("en-IN")}`;
}

export function isValidUpiId(upiId = "") {
  const cleaned = String(upiId || "").trim();
  return cleaned.length >= 5 && cleaned.length <= 120 && /^[a-zA-Z0-9._-]{2,}@[a-zA-Z0-9]{2,}$/.test(cleaned);
}

export function isAllowedPaymentImageSrc(raw = "") {
  const s = String(raw || "").trim();
  if (!s) return false;
  if (/^data:image\/(jpeg|jpg|png|webp);base64,[A-Za-z0-9+/=]+$/i.test(s) && s.length <= 180000) {
    return true;
  }
  try {
    const parsed = new URL(s);
    return parsed.protocol === "https:";
  } catch {
    return false;
  }
}

export function isUploadedQrImageDataUrl(raw = "") {
  const s = String(raw || "").trim();
  return (
    s.length >= 100 &&
    s.length <= 180000 &&
    /^data:image\/(jpeg|jpg|png|webp);base64,[A-Za-z0-9+/=]+$/i.test(s)
  );
}

/**
 * Validates selected image file metadata for UPI QR or payment screenshot uploads.
 * Handles mobile/Android browsers where File.type may be empty or application/octet-stream
 * by checking safe extensions (.jpg/.jpeg/.png/.webp), while strictly blocking SVG/GIF/PDF/etc.
 */
export function isAllowedPaymentImageFileMeta(file) {
  if (!file || typeof file !== "object") {
    return { valid: false, error: "Please select a JPG, PNG, or WebP QR image file." };
  }
  const type = String(file.type || "").trim().toLowerCase();
  const name = String(file.name || "").trim().toLowerCase();
  const size = Number(file.size || 0);
  const hasAllowedMime = /^image\/(jpeg|jpg|png|webp)$/.test(type);
  const hasMissingOrGenericMime = !type || type === "application/octet-stream";
  const hasAllowedExt = /\.(jpe?g|png|webp)$/.test(name);

  if (!hasAllowedMime && !(hasMissingOrGenericMime && hasAllowedExt)) {
    return { valid: false, error: "Unsupported file format. Please choose a JPG, PNG, or WebP image." };
  }
  if (size > 15 * 1024 * 1024) {
    return { valid: false, error: "Selected image file is too large (max 15 MB). Please choose a smaller QR image." };
  }
  return { valid: true };
}

/**
 * Payment UI is strictly disabled until Admin uploads the official UPI QR image,
 * explicitly verifies it matches the official UPI account (9812890081@ybl), and enables payments.
 */
export function isPaymentConfigReady(config = null) {
  if (!config || typeof config !== "object") return false;
  return (
    config.enabled === true &&
    config.qrVerified === true &&
    isValidUpiId(config.upiId) &&
    String(config.payeeName || "").trim().length >= 2 &&
    isUploadedQrImageDataUrl(config.qrImageDataUrl)
  );
}

/**
 * Rejects accidental submission of UPI PINs, OTPs, passwords, CVVs, or 13-19 digit card numbers.
 */
export function containsSensitiveBankingData(raw = "") {
  const s = String(raw || "").trim();
  if (!s) return false;
  if (/\b(upi\s*pin|mpin|atm\s*pin|cvv|cvc|otp|netbanking\s*password|bank\s*password|card\s*number)\b/i.test(s)) {
    return true;
  }
  const digitsOnly = s.replace(/[\s-]+/g, "");
  if (/^(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})$/.test(digitsOnly)) {
    return true;
  }
  return false;
}

/**
 * Normalizes a UTR / UPI transaction reference into a canonical uppercase alphanumeric key
 * so uppercase/lowercase changes, extra spaces, hyphens, underscores, or slashes cannot
 * bypass duplicate UTR detection.
 */
export function normalizeUtrKey(rawUtr = "") {
  return String(rawUtr || "")
    .trim()
    .replace(/[\s/_-]+/g, "")
    .toUpperCase();
}

export function validateUtrReference(rawUtr = "") {
  const raw = String(rawUtr || "").trim();
  const utr = raw.replace(/\s+/g, "").toUpperCase();
  const canonicalKey = normalizeUtrKey(utr);
  if (utr.length < 6 || utr.length > 60) {
    return { valid: false, error: "Please enter a valid UTR / UPI transaction reference (6–60 characters)." };
  }
  if (!/^[A-Z0-9/_-]{6,60}$/.test(utr)) {
    return { valid: false, error: "UTR / reference number may contain only letters, numbers, hyphens, or slashes." };
  }
  if (canonicalKey.length < 6 || canonicalKey.length > 60 || !/^[A-Z0-9]{6,60}$/.test(canonicalKey)) {
    return { valid: false, error: "UTR / reference number must contain at least 6 letters or digits." };
  }
  if (
    containsSensitiveBankingData(raw) ||
    containsSensitiveBankingData(utr) ||
    containsSensitiveBankingData(canonicalKey)
  ) {
    return { valid: false, error: "Never enter UPI PIN, OTP, card number, or banking passwords." };
  }
  return { valid: true, clean: utr, canonicalKey };
}

/**
 * Detects duplicate UTRs across payment records using canonical normalization
 * (safe for both newly submitted records with utrKey and legacy records with only utrNumber).
 */
export function detectDuplicateUtrGroups(payments = []) {
  const byKey = new Map();
  const list = Array.isArray(payments) ? payments : [];
  for (const p of list) {
    if (!p || typeof p !== "object") continue;
    const key = normalizeUtrKey(p.utrKey || p.utrNumber || "");
    if (key.length < 6) continue;
    if (!byKey.has(key)) byKey.set(key, []);
    byKey.get(key).push(p);
  }

  const duplicateKeys = new Set();
  const duplicateRecordIds = new Set();
  const groups = [];
  for (const [key, records] of byKey.entries()) {
    if (records.length > 1) {
      duplicateKeys.add(key);
      for (const r of records) {
        if (r.id) duplicateRecordIds.add(String(r.id));
      }
      groups.push({ utrKey: key, records });
    }
  }

  return {
    duplicateKeys,
    duplicateRecordIds,
    groups,
    duplicateCount: groups.length
  };
}

/**
 * Calculates required advance / booking charge and remaining balance from Admin-approved pricing.
 */
export function calculateAdvanceAndBalance({
  totalPrice = 0,
  mode = "percentage",
  value = 0,
  paidAmount = 0
} = {}) {
  const cleanTotal = Math.max(0, Math.round(Number(totalPrice) || 0));
  const cleanPaid = Math.max(0, Math.round(Number(paidAmount) || 0));
  const cleanMode = mode === "fixed" ? "fixed" : "percentage";
  const cleanVal = Math.max(0, Number(value) || 0);

  let requiredAdvance = 0;
  if (cleanTotal > 0) {
    if (cleanMode === "fixed") {
      requiredAdvance = Math.min(cleanTotal, Math.round(cleanVal));
    } else {
      const pct = Math.min(100, cleanVal);
      requiredAdvance = Math.min(cleanTotal, Math.round((cleanTotal * pct) / 100));
    }
  }
  const remainingBalance = Math.max(0, cleanTotal - cleanPaid);
  const payableNow = Math.max(0, Math.min(remainingBalance, requiredAdvance > cleanPaid ? requiredAdvance - cleanPaid : remainingBalance));

  return {
    totalPrice: cleanTotal,
    chargeMode: cleanMode,
    chargeValue: cleanVal,
    requiredAdvance,
    paidAmount: cleanPaid,
    remainingBalance,
    payableNow
  };
}

export function validateAdminPaymentConfigPayload(input = {}) {
  const enabled = Boolean(input.enabled);
  const upiId = String(input.upiId || "").trim();
  const payeeName = String(input.payeeName || "").trim();
  const qrImageDataUrl = String(input.qrImageDataUrl || "").trim();
  const qrVerified = Boolean(input.qrVerified);
  const instructions = String(input.instructions || "").trim();
  const modelAdvanceMode = input.modelAdvanceMode === "fixed" ? "fixed" : "percentage";
  const modelAdvanceValue = Math.max(0, Number(input.modelAdvanceValue) || 0);
  const weddingChargeMode = input.weddingChargeMode === "fixed" ? "fixed" : "percentage";
  const weddingChargeValue = Math.max(0, Number(input.weddingChargeValue) || 0);
  const hasUploadedQr = isUploadedQrImageDataUrl(qrImageDataUrl);

  if (containsSensitiveBankingData(instructions) || containsSensitiveBankingData(payeeName)) {
    return { valid: false, error: "Never store UPI PINs, OTPs, passwords, or banking credentials." };
  }
  if (qrImageDataUrl && !hasUploadedQr) {
    return { valid: false, error: "Official UPI QR image must be an uploaded JPG/PNG/WebP image under 180KB." };
  }
  if (qrVerified && !hasUploadedQr) {
    return { valid: false, error: "Upload the official UPI QR code image first before marking it verified." };
  }
  if (enabled) {
    if (!isValidUpiId(upiId)) {
      return { valid: false, error: "Enter a valid UPI ID (example: 9812890081@ybl) before enabling UPI payments." };
    }
    if (payeeName.length < 2 || payeeName.length > 120) {
      return { valid: false, error: "Enter a valid Payee Name (2–120 characters) before enabling UPI payments." };
    }
    if (!hasUploadedQr) {
      return { valid: false, error: "Upload the official Studio UPI QR code image before activating payments." };
    }
    if (!qrVerified) {
      return { valid: false, error: `Verify that the uploaded QR code matches the official UPI account (${upiId}) before activating payments.` };
    }
  } else if (upiId && !isValidUpiId(upiId)) {
    return { valid: false, error: "UPI ID format is invalid (example: 9812890081@ybl)." };
  }

  if (modelAdvanceMode === "percentage" && modelAdvanceValue > 100) {
    return { valid: false, error: "Model advance percentage must be between 0% and 100%." };
  }
  if (weddingChargeMode === "percentage" && weddingChargeValue > 100) {
    return { valid: false, error: "Wedding booking charge percentage must be between 0% and 100%." };
  }
  if (instructions.length > 1000) {
    return { valid: false, error: "Payment instructions must be 1000 characters or fewer." };
  }

  return {
    valid: true,
    clean: {
      enabled,
      upiId,
      payeeName,
      qrImageDataUrl,
      qrVerified,
      instructions,
      modelAdvanceMode,
      modelAdvanceValue,
      weddingChargeMode,
      weddingChargeValue
    }
  };
}

export function validateModelAdvanceRequestPayload(input = {}) {
  const customerName = String(input.customerName || "").trim();
  const customerContact = String(input.customerContact || "").trim();
  const modelId = String(input.modelId || "").trim();
  const modelName = String(input.modelName || "").trim();
  const serviceType = String(input.serviceType || "").trim();
  const shootDate = String(input.shootDate || "").trim();
  const shootTime = String(input.shootTime || "").trim();
  const eventLocation = String(input.eventLocation || "").trim();
  const eventDetails = String(input.eventDetails || "").trim();

  if (customerName.length < 2 || customerName.length > 100) {
    return { valid: false, error: "Please enter your full name (2–100 characters)." };
  }
  if (customerContact.length < 8 || customerContact.length > 30) {
    return { valid: false, error: "Please enter a valid contact number (8–20 digits)." };
  }
  if (!modelId || modelName.length < 2) {
    return { valid: false, error: "Please select a verified model from the roster." };
  }
  if (serviceType.length < 2 || serviceType.length > 120) {
    return { valid: false, error: "Please select a service type." };
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(shootDate)) {
    return { valid: false, error: "Please choose a valid shoot date." };
  }
  if (!shootTime) {
    return { valid: false, error: "Please choose a shoot time." };
  }
  if (containsSensitiveBankingData(eventDetails)) {
    return { valid: false, error: "Do not include UPI PINs, OTPs, card numbers, or passwords in notes." };
  }

  return {
    valid: true,
    clean: {
      customerName,
      customerContact,
      modelId,
      modelName,
      serviceType,
      shootDate,
      shootTime,
      eventLocation: eventLocation.slice(0, 200),
      eventDetails: eventDetails.slice(0, 1500)
    }
  };
}

export function validateWeddingPaymentRequestPayload(input = {}) {
  const customerName = String(input.customerName || "").trim();
  const customerContact = String(input.customerContact || "").trim();
  const eventType = String(input.eventType || "").trim();
  const shootDate = String(input.shootDate || "").trim();
  const shootTime = String(input.shootTime || "").trim();
  const eventLocation = String(input.eventLocation || "").trim();
  const eventDetails = String(input.eventDetails || "").trim();

  if (customerName.length < 2 || customerName.length > 100) {
    return { valid: false, error: "Please enter your full name (2–100 characters)." };
  }
  if (customerContact.length < 8 || customerContact.length > 30) {
    return { valid: false, error: "Please enter a valid contact number (8–20 digits)." };
  }
  if (!eventType || eventType.length < 2) {
    return { valid: false, error: "Please select a Wedding / Event booking type." };
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(shootDate)) {
    return { valid: false, error: "Please select a valid event date." };
  }
  if (eventLocation.length < 2 || eventLocation.length > 200) {
    return { valid: false, error: "Please enter the event venue / city." };
  }
  if (eventDetails.length < 5 || eventDetails.length > 1500) {
    return { valid: false, error: "Please enter brief event details (5–1500 characters)." };
  }
  if (containsSensitiveBankingData(eventDetails)) {
    return { valid: false, error: "Do not include UPI PINs, OTPs, card numbers, or passwords." };
  }

  return {
    valid: true,
    clean: {
      customerName,
      customerContact,
      eventType,
      serviceType: eventType,
      shootDate,
      shootTime: shootTime || "Full Day / Custom",
      eventLocation,
      eventDetails
    }
  };
}

/**
 * Computes next verified payment totals & statuses strictly for Admin verification.
 */
export function computeVerifiedPaymentTransition(
  payment = {},
  { verifiedAmount = 0, confirmBooking = false } = {}
) {
  const totalPrice = Math.max(0, Math.round(Number(payment.totalPrice) || 0));
  const previousPaid = Math.max(0, Math.round(Number(payment.paidAmount) || 0));
  const newlyVerified = Math.max(0, Math.round(Number(verifiedAmount) || 0));
  if (newlyVerified <= 0) {
    return { valid: false, error: "Enter a verified payment amount greater than ₹0." };
  }

  const nextPaid = previousPaid + newlyVerified;
  const remainingBalance = Math.max(0, totalPrice - nextPaid);
  let nextStatus = "Payment Verified";
  if (totalPrice > 0 && remainingBalance === 0) {
    nextStatus = "Fully Paid";
  } else if (totalPrice > 0 && nextPaid > 0 && remainingBalance > 0) {
    nextStatus = "Partially Paid";
  }

  const nextBookingStatus = confirmBooking ? "Confirmed" : payment.bookingStatus || "Payment Pending";
  return {
    valid: true,
    paidAmount: nextPaid,
    verifiedAmount: newlyVerified,
    remainingBalance,
    paymentStatus: nextStatus,
    bookingStatus: nextBookingStatus
  };
}

export function stripAdminPaymentFieldsForCustomer(payment = {}) {
  if (!payment || typeof payment !== "object") return {};
  const clean = {};
  for (const [k, v] of Object.entries(payment)) {
    if (STRIPPED_CUSTOMER_PAYMENT_KEYS.has(k)) continue;
    clean[k] = v;
  }
  return clean;
}

export function filterCustomerPayments(payments = [], currentUid = "") {
  const uid = String(currentUid || "").trim();
  if (!uid || !Array.isArray(payments)) return [];
  return payments
    .filter((p) => p && String(p.customerUid || "").trim() === uid)
    .map((p) => stripAdminPaymentFieldsForCustomer(p));
}

export function getPaymentStatusClass(status = "") {
  const norm = String(status || "").trim().toLowerCase();
  if (norm === "fully paid" || norm === "payment verified") return "pay-status-verified";
  if (norm === "partially paid") return "pay-status-partial";
  if (norm === "submitted for verification") return "pay-status-submitted";
  if (norm === "payment rejected") return "pay-status-rejected";
  if (norm === "refund requested") return "pay-status-refund-req";
  if (norm === "refund processed") return "pay-status-refund-done";
  return "pay-status-pending";
}

export function buildPrintablePaymentReceiptHtml(payment = {}, config = {}, { escapeHtml = defaultEscapeHtml } = {}) {
  const receiptNo = escapeHtml(payment.id || "SAS-REC");
  const customerName = escapeHtml(payment.customerName || "Customer");
  const categoryLabel = payment.paymentCategory === "wedding_booking"
    ? "Wedding / Event Booking Payment"
    : "Model Booking Advance Payment";
  const serviceLabel = escapeHtml(
    payment.paymentCategory === "wedding_booking"
      ? payment.eventType || payment.serviceType || "Wedding / Event"
      : `${payment.modelName || "Studio Model"} — ${payment.serviceType || "Model Shoot"}`
  );
  const dateLabel = escapeHtml(`${payment.shootDate || "—"} ${payment.shootTime ? `• ${payment.shootTime}` : ""}`);
  const payeeName = escapeHtml(config?.payeeName || "STAR ARTIST STUDIO");
  const upiId = isPaymentConfigReady(config) ? escapeHtml(config.upiId) : "Verified Studio UPI";

  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <title>Payment Receipt — ${receiptNo}</title>
  <style>
    body { font-family: Arial, sans-serif; background: #f8f7f3; color: #18181b; padding: 28px; }
    .receipt { max-width: 680px; margin: 0 auto; background: #fff; border: 1px solid #d4af37; border-radius: 12px; padding: 28px; }
    .head { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid #d4af37; padding-bottom: 14px; margin-bottom: 18px; }
    h1 { margin: 0; font-size: 20px; color: #9a6b00; letter-spacing: 0.04em; }
    .sub { font-size: 12px; color: #52525b; margin-top: 4px; }
    table { width: 100%; border-collapse: collapse; margin-top: 12px; }
    th, td { text-align: left; padding: 9px 8px; border-bottom: 1px solid #e4e4e7; font-size: 14px; }
    th { color: #52525b; width: 42%; font-weight: 600; }
    .totals { background: #fffbeb; font-weight: 700; }
    .foot { margin-top: 20px; font-size: 12px; color: #52525b; }
    @media print { body { background: #fff; padding: 0; } .no-print { display: none; } }
  </style>
</head>
<body>
  <div class="receipt">
    <div class="head">
      <div>
        <h1>STAR ARTIST STUDIO</h1>
        <div class="sub">Talwara Khurd, Sirsa, Haryana - 125102</div>
        <div class="sub">Payee: ${payeeName} (${upiId})</div>
      </div>
      <div style="text-align:right;">
        <strong>PAYMENT RECEIPT</strong>
        <div class="sub">Ref: ${receiptNo}</div>
        <div class="sub">Status: ${escapeHtml(payment.paymentStatus || "Payment Pending")}</div>
      </div>
    </div>
    <table>
      <tr><th>Customer Name</th><td>${customerName}</td></tr>
      <tr><th>Category</th><td>${escapeHtml(categoryLabel)}</td></tr>
      <tr><th>Booking / Service</th><td>${serviceLabel}</td></tr>
      <tr><th>Shoot / Event Date</th><td>${dateLabel}</td></tr>
      <tr><th>Booking Status</th><td>${escapeHtml(payment.bookingStatus || "Payment Pending")}</td></tr>
      <tr><th>UTR / Transaction Ref</th><td>${escapeHtml(payment.utrNumber || "Pending Submission")}</td></tr>
      <tr><th>Admin-Approved Total Price</th><td>${escapeHtml(formatInrAmount(payment.totalPrice))}</td></tr>
      <tr><th>Required Advance / Booking Charge</th><td>${escapeHtml(formatInrAmount(payment.requiredAdvance))}</td></tr>
      <tr class="totals"><th>Verified Amount Paid</th><td>${escapeHtml(formatInrAmount(payment.paidAmount))}</td></tr>
      <tr class="totals"><th>Remaining Balance</th><td>${escapeHtml(formatInrAmount(payment.remainingBalance))}</td></tr>
      ${payment.refundStatus ? `<tr><th>Refund Status (${escapeHtml(payment.refundReference || "—")})</th><td>${escapeHtml(payment.refundStatus)} • ${escapeHtml(formatInrAmount(payment.refundAmount))}</td></tr>` : ""}
    </table>
    <div class="foot">
      This receipt reflects manual UPI verification recorded by the authorized STAR ARTIST STUDIO Administrator.
    </div>
    <div class="no-print" style="margin-top:16px;">
      <button onclick="window.print()" style="padding:10px 18px;background:#b8860b;color:#fff;border:0;border-radius:8px;cursor:pointer;">Print / Save PDF</button>
    </div>
  </div>
</body>
</html>`;
}

export function renderUpiPaymentPanelHtml({
  config = DEFAULT_PAYMENT_CONFIG,
  payableAmount = 0,
  title = "",
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  if (!isPaymentConfigReady(config)) {
    return `
      <div class="upi-disabled-box" data-upi-configured="false">
        <strong>⚠ ${escapeHtml(t("payUpiDisabledTitle"))}</strong>
        <p>${escapeHtml(t("payUpiDisabledBody"))}</p>
      </div>
    `;
  }

  const safeQr = isAllowedPaymentImageSrc(config.qrImageDataUrl) ? config.qrImageDataUrl : "";
  const exactAmount = Math.max(0, Math.round(Number(payableAmount) || 0));

  return `
    <div class="upi-qr-panel" data-upi-configured="true">
      <div class="upi-qr-panel-head">
        <div>
          <div class="upi-eyebrow">${escapeHtml(title || t("payUpiOfficialTitle"))}</div>
          <div class="upi-payee-name">${escapeHtml(config.payeeName)}</div>
          <div class="upi-id-row">
            <span>UPI ID:</span>
            <strong class="upi-id-code">${escapeHtml(config.upiId)}</strong>
            <button type="button" class="btn-outline upi-copy-btn" data-copy-upi="${escapeHtml(config.upiId)}">${escapeHtml(t("payCopyUpiBtn"))}</button>
          </div>
        </div>
        <div class="upi-amount-pill">
          <span>${escapeHtml(t("payExactAmountLabel"))}</span>
          <strong>${escapeHtml(exactAmount > 0 ? formatInrAmount(exactAmount) : t("payAwaitingAdminPrice"))}</strong>
        </div>
      </div>
      ${safeQr ? `<div class="upi-qr-img-wrap"><img src="${escapeHtml(safeQr)}" alt="${escapeHtml(config.payeeName)} UPI QR" class="upi-qr-img" /></div>` : ""}
      ${config.instructions ? `<p class="upi-instructions">${escapeHtml(config.instructions)}</p>` : ""}
      <p class="upi-security-note">🔒 ${escapeHtml(t("paySecurityReminder"))}</p>
    </div>
  `;
}

export function renderCustomerPaymentsPortalHtml({
  payments = [],
  paymentConfig = DEFAULT_PAYMENT_CONFIG,
  publicModels = [],
  currentUserUid = "",
  currentUserName = "",
  activePaymentForm = "model",
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  if (!String(currentUserUid || "").trim()) {
    return `<div class="portal-empty-card"><p>${escapeHtml(t("portalSignInRequired"))}</p></div>`;
  }

  const myPayments = filterCustomerPayments(payments, currentUserUid);
  const models = Array.isArray(publicModels) ? publicModels : [];
  const upiReady = isPaymentConfigReady(paymentConfig);

  const modelOptionsHtml = models.length
    ? models
        .map((m) => `<option value="${escapeHtml(m.id || m.modelId || "")}" data-model-name="${escapeHtml(m.fullName || "")}">${escapeHtml(m.fullName || "Model")} (${escapeHtml(m.city || "Studio")})</option>`)
        .join("")
    : `<option value="studio_roster_model" data-model-name="Official Studio Roster Model">${escapeHtml(t("payDefaultModelOption"))}</option>`;

  const modelServiceOptionsHtml = MODEL_BOOKING_SERVICES
    .map((svc) => `<option value="${escapeHtml(svc)}">${escapeHtml(svc)}</option>`)
    .join("");

  const weddingTypeOptionsHtml = WEDDING_EVENT_TYPES
    .map((evt) => `<option value="${escapeHtml(evt)}">${escapeHtml(evt)}</option>`)
    .join("");

  const paymentsCardsHtml = myPayments.length
    ? myPayments
        .map((p) => {
          const calc = calculateAdvanceAndBalance({
            totalPrice: p.totalPrice,
            mode: p.chargeMode || "percentage",
            value: p.chargeValue || 0,
            paidAmount: p.paidAmount
          });
          const statusCls = getPaymentStatusClass(p.paymentStatus);
          const canSubmitUtr =
            p.priceSetByAdmin === true &&
            p.totalPrice > 0 &&
            upiReady &&
            ["Payment Pending", "Payment Rejected", "Partially Paid"].includes(p.paymentStatus);
          const canRequestRefund =
            ["Payment Verified", "Partially Paid", "Fully Paid"].includes(p.paymentStatus) &&
            Number(p.paidAmount || 0) > 0;
          const historyItems = Array.isArray(p.history) ? p.history : [];

          return `
            <article class="payment-record-card" data-payment-id="${escapeHtml(p.id || "")}">
              <div class="payment-record-top">
                <div>
                  <span class="payment-cat-tag">${escapeHtml(p.paymentCategory === "wedding_booking" ? t("payWeddingCategory") : t("payModelCategory"))}</span>
                  <h4>${escapeHtml(p.paymentCategory === "wedding_booking" ? p.eventType || p.serviceType : `${p.modelName || "Model"} — ${p.serviceType || ""}`)}</h4>
                  <p class="payment-meta-line">📅 ${escapeHtml(p.shootDate || "—")} ${p.shootTime ? `• ⏰ ${escapeHtml(p.shootTime)}` : ""} ${p.eventLocation ? `• 📍 ${escapeHtml(p.eventLocation)}` : ""}</p>
                </div>
                <div class="payment-status-stack">
                  <span class="payment-status-pill ${statusCls}">${escapeHtml(p.paymentStatus || "Payment Pending")}</span>
                  <span class="payment-booking-pill">${escapeHtml(t("payBookingStateLabel"))}: ${escapeHtml(p.bookingStatus || "Payment Pending")}</span>
                </div>
              </div>

              <div class="payment-money-grid">
                <div class="payment-money-box">
                  <span>${escapeHtml(t("payTotalPriceLabel"))}</span>
                  <strong>${escapeHtml(p.priceSetByAdmin ? formatInrAmount(p.totalPrice) : t("payAwaitingAdminPrice"))}</strong>
                </div>
                <div class="payment-money-box">
                  <span>${escapeHtml(t("payRequiredAdvanceLabel"))}</span>
                  <strong>${escapeHtml(p.priceSetByAdmin ? formatInrAmount(p.requiredAdvance || calc.requiredAdvance) : "—")}</strong>
                </div>
                <div class="payment-money-box is-paid">
                  <span>${escapeHtml(t("payAdvancePaidLabel"))}</span>
                  <strong>${escapeHtml(formatInrAmount(p.paidAmount || 0))}</strong>
                </div>
                <div class="payment-money-box is-balance">
                  <span>${escapeHtml(t("payRemainingBalanceLabel"))}</span>
                  <strong>${escapeHtml(p.priceSetByAdmin ? formatInrAmount(p.remainingBalance) : "—")}</strong>
                </div>
              </div>

              ${p.utrNumber ? `<div class="payment-ref-line">🧾 ${escapeHtml(t("payUtrLabel"))}: <strong>${escapeHtml(p.utrNumber)}</strong></div>` : ""}
              ${p.rejectionReason ? `<div class="payment-reject-note">⚠ ${escapeHtml(t("payRejectionReasonLabel"))}: ${escapeHtml(p.rejectionReason)}</div>` : ""}

              ${
                p.priceSetByAdmin && ["Payment Pending", "Payment Rejected", "Partially Paid"].includes(p.paymentStatus)
                  ? renderUpiPaymentPanelHtml({
                      config: paymentConfig,
                      payableAmount: calc.payableNow > 0 ? calc.payableNow : p.requiredAdvance || p.remainingBalance,
                      title: t("payScanAndPayTitle"),
                      lang,
                      t,
                      escapeHtml
                    })
                  : ""
              }

              ${
                canSubmitUtr
                  ? `
                    <form class="payment-utr-form" data-customer-utr-form="${escapeHtml(p.id || "")}">
                      <div class="payment-utr-row">
                        <input
                          type="text"
                          name="utrNumber"
                          class="input"
                          required
                          minlength="6"
                          maxlength="60"
                          placeholder="${escapeHtml(t("payUtrPlaceholder"))}"
                          value="${escapeHtml(p.utrNumber || "")}"
                        />
                        <input
                          type="file"
                          name="screenshotFile"
                          accept="image/png,image/jpeg,image/webp"
                          class="input payment-file-input"
                          title="${escapeHtml(t("payScreenshotOptional"))}"
                        />
                        <button type="submit" class="btn-gold">${escapeHtml(t("paySubmitUtrBtn"))}</button>
                      </div>
                      <div class="payment-utr-error hidden" data-utr-error-box role="alert" aria-live="polite"></div>
                      <small class="payment-helper-note">${escapeHtml(t("payManualVerifyNote"))}</small>
                    </form>
                  `
                  : !p.priceSetByAdmin
                  ? `<p class="payment-wait-note">⏳ ${escapeHtml(t("payWaitingAdminReview"))}</p>`
                  : ""
              }

              <div class="payment-card-actions">
                <button type="button" class="btn-outline" data-print-payment-receipt="${escapeHtml(p.id || "")}">
                  🖨 ${escapeHtml(t("payDownloadReceiptBtn"))}
                </button>
                ${
                  canRequestRefund
                    ? `<button type="button" class="btn-danger" data-request-payment-refund="${escapeHtml(p.id || "")}">${escapeHtml(t("payRequestRefundBtn"))}</button>`
                    : ""
                }
              </div>

              ${
                historyItems.length
                  ? `
                    <details class="payment-history-details">
                      <summary>${escapeHtml(t("payHistoryHeading"))} (${historyItems.length})</summary>
                      <ul class="payment-history-list">
                        ${historyItems
                          .map((h) => `<li><strong>${escapeHtml(h.status || h.action || "")}</strong> — ${escapeHtml(h.note || "")} ${h.at ? `<small>(${escapeHtml(formatNetworkTimestamp(h.at, lang))})</small>` : ""}</li>`)
                          .join("")}
                      </ul>
                    </details>
                  `
                  : ""
              }
            </article>
          `;
        })
        .join("")
    : `<div class="portal-empty-card"><p>${escapeHtml(t("payEmptyCustomerList"))}</p></div>`;

  return `
    <div class="customer-payments-portal" data-customer-payments-portal="true">
      <div class="payment-section-banner">
        <strong>💳 ${escapeHtml(t("payPortalTitle"))}</strong>
        <p>${escapeHtml(t("payPortalSubtitle"))}</p>
      </div>

      ${renderUpiPaymentPanelHtml({
        config: paymentConfig,
        payableAmount: 0,
        title: t("payStudioUpiStatusTitle"),
        lang,
        t,
        escapeHtml
      })}

      <div class="payment-form-switch" role="tablist">
        <button type="button" class="chip-btn ${activePaymentForm === "model" ? "active" : ""}" data-payment-form-tab="model">
          ✨ ${escapeHtml(t("payModelTabBtn"))}
        </button>
        <button type="button" class="chip-btn ${activePaymentForm === "wedding" ? "active" : ""}" data-payment-form-tab="wedding">
          💍 ${escapeHtml(t("payWeddingTabBtn"))}
        </button>
      </div>

      ${
        activePaymentForm === "model"
          ? `
            <form id="customerModelPaymentForm" class="studio-card form-grid two-col">
              <div class="field full">
                <h4 style="margin:0;color:var(--gold);">${escapeHtml(t("payModelFormTitle"))}</h4>
                <p style="margin:4px 0 0;font-size:0.84rem;color:var(--text-secondary);">${escapeHtml(t("payModelFormSub"))}</p>
              </div>
              <div class="field">
                <label for="payModelCustomerName">${escapeHtml(t("clientNameLabel"))}</label>
                <input id="payModelCustomerName" class="input" required minlength="2" maxlength="100" value="${escapeHtml(currentUserName)}" />
              </div>
              <div class="field">
                <label for="payModelContact">${escapeHtml(t("phoneLabel"))}</label>
                <input id="payModelContact" class="input" type="tel" required minlength="8" maxlength="20" placeholder="+91 98128..." />
              </div>
              <div class="field">
                <label for="payModelSelect">${escapeHtml(t("paySelectModelLabel"))}</label>
                <select id="payModelSelect" class="input" required>${modelOptionsHtml}</select>
              </div>
              <div class="field">
                <label for="payModelService">${escapeHtml(t("serviceLabel"))}</label>
                <select id="payModelService" class="input" required>${modelServiceOptionsHtml}</select>
              </div>
              <div class="field">
                <label for="payModelDate">${escapeHtml(t("dateLabel"))}</label>
                <input id="payModelDate" class="input" type="date" required />
              </div>
              <div class="field">
                <label for="payModelTime">${escapeHtml(t("timeLabel"))}</label>
                <select id="payModelTime" class="input" required>
                  <option value="10:00 AM">10:00 AM</option>
                  <option value="11:30 AM">11:30 AM</option>
                  <option value="02:00 PM">02:00 PM</option>
                  <option value="04:30 PM">04:30 PM</option>
                  <option value="06:30 PM">06:30 PM</option>
                </select>
              </div>
              <div class="field full">
                <label for="payModelNotes">${escapeHtml(t("notesLabel"))}</label>
                <textarea id="payModelNotes" class="input" rows="2" maxlength="1000" placeholder="${escapeHtml(t("payModelNotesPlaceholder"))}"></textarea>
              </div>
              <div class="field full">
                <button type="submit" class="btn-gold">${escapeHtml(t("paySubmitModelReqBtn"))}</button>
              </div>
            </form>
          `
          : `
            <form id="customerWeddingPaymentForm" class="studio-card form-grid two-col">
              <div class="field full">
                <h4 style="margin:0;color:var(--gold);">${escapeHtml(t("payWeddingFormTitle"))}</h4>
                <p style="margin:4px 0 0;font-size:0.84rem;color:var(--text-secondary);">${escapeHtml(t("payWeddingFormSub"))}</p>
              </div>
              <div class="field">
                <label for="payWedCustomerName">${escapeHtml(t("clientNameLabel"))}</label>
                <input id="payWedCustomerName" class="input" required minlength="2" maxlength="100" value="${escapeHtml(currentUserName)}" />
              </div>
              <div class="field">
                <label for="payWedContact">${escapeHtml(t("phoneLabel"))}</label>
                <input id="payWedContact" class="input" type="tel" required minlength="8" maxlength="20" placeholder="+91 98128..." />
              </div>
              <div class="field">
                <label for="payWedEventType">${escapeHtml(t("payEventTypeLabel"))}</label>
                <select id="payWedEventType" class="input" required>${weddingTypeOptionsHtml}</select>
              </div>
              <div class="field">
                <label for="payWedDate">${escapeHtml(t("dateLabel"))}</label>
                <input id="payWedDate" class="input" type="date" required />
              </div>
              <div class="field">
                <label for="payWedTime">${escapeHtml(t("timeLabel"))}</label>
                <input id="payWedTime" class="input" type="text" maxlength="50" placeholder="e.g. 09:00 AM - Full Day" />
              </div>
              <div class="field">
                <label for="payWedLocation">${escapeHtml(t("locationLabel"))}</label>
                <input id="payWedLocation" class="input" required minlength="2" maxlength="200" placeholder="Venue / City" />
              </div>
              <div class="field full">
                <label for="payWedDetails">${escapeHtml(t("payEventDetailsLabel"))}</label>
                <textarea id="payWedDetails" class="input" rows="3" required minlength="5" maxlength="1500" placeholder="${escapeHtml(t("payEventDetailsPlaceholder"))}"></textarea>
              </div>
              <div class="field full">
                <button type="submit" class="btn-gold">${escapeHtml(t("paySubmitWeddingReqBtn"))}</button>
              </div>
            </form>
          `
      }

      <div class="payment-records-block">
        <h4>${escapeHtml(t("payMyRecordsHeading"))} (${myPayments.length})</h4>
        <div class="payment-records-list">${paymentsCardsHtml}</div>
      </div>
    </div>
  `;
}

export function renderAdminPaymentDashboardHtml({
  payments = [],
  paymentConfig = DEFAULT_PAYMENT_CONFIG,
  qrUploadState = null,
  filterStatus = "all",
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const cfg = { ...DEFAULT_PAYMENT_CONFIG, ...(paymentConfig || {}) };
  const uploadState = qrUploadState && typeof qrUploadState === "object" ? qrUploadState : {};
  const hasSavedQr = isUploadedQrImageDataUrl(cfg.qrImageDataUrl);
  const isUploadingQr = uploadState.status === "uploading";
  const previewCandidate = isUploadedQrImageDataUrl(uploadState.previewDataUrl)
    ? uploadState.previewDataUrl
    : "";
  const displayQrSrc = previewCandidate || (hasSavedQr ? cfg.qrImageDataUrl : "");
  const hasVisibleQr = Boolean(displayQrSrc);
  const qrErrorMsg = String(uploadState.error || "").trim();
  const qrStatusClass = isUploadingQr
    ? "is-uploading"
    : hasSavedQr
    ? "is-saved"
    : previewCandidate
    ? "is-pending-save"
    : "";
  const qrStatusText = isUploadingQr
    ? t("adminPayQrUploadingBadge")
    : hasSavedQr
    ? `${t("adminPayQrSavedBadge")}${uploadState.approxKb ? ` (~${uploadState.approxKb} KB)` : ""}`
    : previewCandidate
    ? t("adminPayQrPendingSaveBadge")
    : t("adminPayStatusPendingQr");
  const allPayments = Array.isArray(payments) ? payments : [];
  const filtered =
    filterStatus && filterStatus !== "all"
      ? allPayments.filter((p) => p.paymentStatus === filterStatus)
      : allPayments;

  const pendingVerifyCount = allPayments.filter((p) => p.paymentStatus === "Submitted for Verification").length;
  const totalVerifiedInr = allPayments.reduce((sum, p) => sum + Math.max(0, Number(p.paidAmount || 0)), 0);
  const duplicateInfo = detectDuplicateUtrGroups(allPayments);

  const duplicateAlertBannerHtml = duplicateInfo.duplicateCount
    ? `
      <div id="adminDuplicateUtrAlert" class="admin-dup-utr-alert" role="alert" data-duplicate-utr-count="${duplicateInfo.duplicateCount}">
        <div class="admin-dup-utr-alert-head">
          <strong>🚨 ${escapeHtml(t("adminPayDupUtrAlertTitle"))} (${duplicateInfo.duplicateCount})</strong>
          <span>${escapeHtml(t("adminPayDupUtrAlertSub"))}</span>
        </div>
        <ul class="admin-dup-utr-list">
          ${duplicateInfo.groups
            .map((g) => {
              const labels = g.records
                .map((r) => `${r.customerName || "Customer"} (${r.id || "—"} • ${r.paymentStatus || "Pending"})`)
                .join(" · ");
              return `<li><strong>UTR ${escapeHtml(g.utrKey)}</strong> (${g.records.length}×): ${escapeHtml(labels)}</li>`;
            })
            .join("")}
        </ul>
      </div>
    `
    : `
      <div id="adminDuplicateUtrStatus" class="admin-dup-utr-ok" data-duplicate-utr-count="0">
        🛡 <strong>${escapeHtml(t("adminPayDupUtrProtectedLabel"))}</strong> — ${escapeHtml(t("adminPayDupUtrNoneMsg"))}
      </div>
    `;

  const filterChipsHtml = ["all", ...PAYMENT_STATUS_LIST]
    .map((st) => {
      const active = (filterStatus || "all") === st;
      const count = st === "all" ? allPayments.length : allPayments.filter((p) => p.paymentStatus === st).length;
      return `<button type="button" class="chip-btn ${active ? "active" : ""}" data-admin-pay-filter="${escapeHtml(st)}">${escapeHtml(st === "all" ? t("filterAll") : st)} (${count})</button>`;
    })
    .join("");

  const rowsHtml = filtered.length
    ? filtered
        .map((p) => {
          const statusCls = getPaymentStatusClass(p.paymentStatus);
          const safeShot = isAllowedPaymentImageSrc(p.screenshotDataUrl) ? p.screenshotDataUrl : "";
          const historyItems = Array.isArray(p.history) ? p.history : [];
          const normalizedRowUtr = normalizeUtrKey(p.utrKey || p.utrNumber || "");
          const isDuplicateUtr = Boolean(normalizedRowUtr && duplicateInfo.duplicateKeys.has(normalizedRowUtr));
          return `
            <article class="admin-payment-card ${isDuplicateUtr ? "is-duplicate-utr" : ""}" data-admin-payment-id="${escapeHtml(p.id || "")}" ${isDuplicateUtr ? 'data-duplicate-utr="true"' : ""}>
              <div class="payment-record-top">
                <div>
                  <span class="payment-cat-tag">${escapeHtml(p.paymentCategory === "wedding_booking" ? "Wedding / Event" : "Model Advance")}</span>
                  <h4>${escapeHtml(p.customerName || "Customer")} (${escapeHtml(p.customerContact || "—")})</h4>
                  <p class="payment-meta-line">
                    ${escapeHtml(p.paymentCategory === "wedding_booking" ? p.eventType || p.serviceType : `${p.modelName || "Model"} • ${p.serviceType || ""}`)}
                    • 📅 ${escapeHtml(p.shootDate || "—")} ${p.shootTime ? `(${escapeHtml(p.shootTime)})` : ""}
                  </p>
                  ${p.eventLocation ? `<p class="payment-meta-line">📍 ${escapeHtml(p.eventLocation)}</p>` : ""}
                  ${p.eventDetails ? `<p class="payment-meta-line">${escapeHtml(p.eventDetails)}</p>` : ""}
                </div>
                <div class="payment-status-stack">
                  <span class="payment-status-pill ${statusCls}">${escapeHtml(p.paymentStatus || "Payment Pending")}</span>
                  <span class="payment-booking-pill">Booking: ${escapeHtml(p.bookingStatus || "Payment Pending")}</span>
                </div>
              </div>

              <div class="payment-money-grid">
                <div class="payment-money-box"><span>Total Price</span><strong>${escapeHtml(formatInrAmount(p.totalPrice))}</strong></div>
                <div class="payment-money-box"><span>Required Advance</span><strong>${escapeHtml(formatInrAmount(p.requiredAdvance))}</strong></div>
                <div class="payment-money-box is-paid"><span>Verified Paid</span><strong>${escapeHtml(formatInrAmount(p.paidAmount))}</strong></div>
                <div class="payment-money-box is-balance"><span>Remaining</span><strong>${escapeHtml(formatInrAmount(p.remainingBalance))}</strong></div>
              </div>

              ${
                p.utrNumber
                  ? `<div class="payment-ref-line">🧾 Customer UTR: <strong>${escapeHtml(p.utrNumber)}</strong> ${
                      isDuplicateUtr
                        ? `<span class="admin-dup-utr-pill" role="alert">⚠ ${escapeHtml(t("adminPayDupUtrBadge"))}</span>`
                        : ""
                    }</div>`
                  : ""
              }
              ${safeShot ? `<div class="admin-pay-shot-wrap"><a href="${escapeHtml(safeShot)}" target="_blank" rel="noopener noreferrer"><img src="${escapeHtml(safeShot)}" alt="Customer payment proof" class="admin-pay-shot-thumb" /></a></div>` : ""}

              <!-- Step 1: Admin Set Package Price & Advance Charge -->
              <form class="admin-pay-inline-form" data-admin-set-price-form="${escapeHtml(p.id || "")}">
                <div class="admin-pay-form-title">1. ${escapeHtml(t("adminPaySetPricingHeading"))}</div>
                <div class="admin-pay-inline-grid">
                  <input type="number" name="totalPrice" class="input" min="1" max="100000000" required placeholder="Total Price (₹)" value="${escapeHtml(p.totalPrice || "")}" />
                  <select name="chargeMode" class="input">
                    <option value="percentage" ${p.chargeMode !== "fixed" ? "selected" : ""}>Advance %</option>
                    <option value="fixed" ${p.chargeMode === "fixed" ? "selected" : ""}>Fixed ₹</option>
                  </select>
                  <input type="number" name="chargeValue" class="input" min="0" max="100000000" required placeholder="Charge % or ₹" value="${escapeHtml(p.chargeValue ?? (p.paymentCategory === "wedding_booking" ? cfg.weddingChargeValue : cfg.modelAdvanceValue))}" />
                  <button type="submit" class="btn-outline">${escapeHtml(t("adminPaySavePricingBtn"))}</button>
                </div>
              </form>

              <!-- Step 2: Admin Manual Bank/UPI Verification -->
              <form class="admin-pay-inline-form" data-admin-verify-pay-form="${escapeHtml(p.id || "")}">
                <div class="admin-pay-form-title">2. ${escapeHtml(t("adminPayVerifyHeading"))}</div>
                <div class="admin-pay-inline-grid">
                  <input type="number" name="verifiedAmount" class="input" min="1" max="100000000" required placeholder="Verified ₹ Received" value="${escapeHtml(p.requiredAdvance || p.remainingBalance || "")}" />
                  <input type="text" name="adminNotes" class="input" maxlength="500" placeholder="Internal bank check note" value="${escapeHtml(p.adminNotes || "")}" />
                  <label class="admin-pay-checkbox">
                    <input type="checkbox" name="confirmBooking" checked />
                    <span>Confirm Booking</span>
                  </label>
                  <button type="submit" class="btn-gold">${escapeHtml(t("adminPayVerifyBtn"))}</button>
                </div>
              </form>

              <!-- Step 3: Reject / Refund / Receipt -->
              <div class="admin-pay-actions-row">
                <form class="admin-pay-mini-form" data-admin-reject-pay-form="${escapeHtml(p.id || "")}">
                  <input type="text" name="rejectionReason" class="input" maxlength="300" placeholder="Rejection reason (e.g. UTR not found)" />
                  <button type="submit" class="btn-danger">${escapeHtml(t("adminPayRejectBtn"))}</button>
                </form>
                <form class="admin-pay-mini-form" data-admin-refund-pay-form="${escapeHtml(p.id || "")}">
                  <input type="number" name="refundAmount" class="input" min="1" max="100000000" placeholder="Refund ₹" value="${escapeHtml(p.refundAmount || p.paidAmount || "")}" />
                  <input type="text" name="refundReference" class="input" maxlength="80" placeholder="Refund UTR / Ref" value="${escapeHtml(p.refundReference || "")}" />
                  <button type="submit" class="btn-outline">${escapeHtml(t("adminPayMarkRefundBtn"))}</button>
                </form>
                <button type="button" class="btn-outline" data-print-payment-receipt="${escapeHtml(p.id || "")}">🖨 ${escapeHtml(t("payDownloadReceiptBtn"))}</button>
              </div>

              ${
                historyItems.length
                  ? `
                    <details class="payment-history-details">
                      <summary>${escapeHtml(t("payHistoryHeading"))} (${historyItems.length})</summary>
                      <ul class="payment-history-list">
                        ${historyItems
                          .map((h) => `<li><strong>${escapeHtml(h.status || h.action || "")}</strong> — ${escapeHtml(h.note || "")} ${h.at ? `<small>(${escapeHtml(formatNetworkTimestamp(h.at, lang))})</small>` : ""}</li>`)
                          .join("")}
                      </ul>
                    </details>
                  `
                  : ""
              }
            </article>
          `;
        })
        .join("")
    : `<div class="portal-empty-card"><p>${escapeHtml(t("adminPayEmptyList"))}</p></div>`;

  return `
    <div class="admin-payments-dashboard" data-admin-payments-dashboard="true">
      <div class="admin-pay-summary-strip">
        <div class="payment-money-box"><span>Total Payment Records</span><strong>${allPayments.length}</strong></div>
        <div class="payment-money-box is-balance"><span>Pending Verification</span><strong>${pendingVerifyCount}</strong></div>
        <div class="payment-money-box is-paid"><span>Verified Received</span><strong>${escapeHtml(formatInrAmount(totalVerifiedInr))}</strong></div>
        <div class="payment-money-box"><span>UPI Status</span><strong>${escapeHtml(isPaymentConfigReady(cfg) ? t("adminPayStatusActive") : t("adminPayStatusPendingQr"))}</strong></div>
      </div>

      <form id="adminUpiConfigForm" class="studio-card form-grid two-col" style="margin-bottom:18px;">
        <div class="field full">
          <h4 style="margin:0;color:var(--gold);">🏦 ${escapeHtml(t("adminPayUpiConfigTitle"))}</h4>
          <p style="margin:4px 0 0;font-size:0.84rem;color:var(--text-secondary);">${escapeHtml(t("adminPayUpiConfigSub"))}</p>
        </div>
        <div class="field">
          <label for="adminUpiId">${escapeHtml(t("adminPayUpiIdLabel"))}</label>
          <input id="adminUpiId" class="input" maxlength="120" placeholder="${escapeHtml(OFFICIAL_STUDIO_UPI_ID)}" value="${escapeHtml(cfg.upiId || OFFICIAL_STUDIO_UPI_ID)}" />
        </div>
        <div class="field">
          <label for="adminUpiPayeeName">${escapeHtml(t("adminPayPayeeLabel"))}</label>
          <input id="adminUpiPayeeName" class="input" maxlength="120" placeholder="STAR ARTIST STUDIO" value="${escapeHtml(cfg.payeeName || "STAR ARTIST STUDIO")}" />
        </div>
        <div class="field">
          <label for="adminModelAdvMode">${escapeHtml(t("adminPayModelAdvLabel"))}</label>
          <div class="admin-pay-dual">
            <select id="adminModelAdvMode" class="input">
              <option value="percentage" ${cfg.modelAdvanceMode !== "fixed" ? "selected" : ""}>Percentage (%)</option>
              <option value="fixed" ${cfg.modelAdvanceMode === "fixed" ? "selected" : ""}>Fixed (₹)</option>
            </select>
            <input id="adminModelAdvValue" type="number" class="input" min="0" max="10000000" value="${escapeHtml(cfg.modelAdvanceValue)}" />
          </div>
        </div>
        <div class="field">
          <label for="adminWedChargeMode">${escapeHtml(t("adminPayWedChargeLabel"))}</label>
          <div class="admin-pay-dual">
            <select id="adminWedChargeMode" class="input">
              <option value="percentage" ${cfg.weddingChargeMode !== "fixed" ? "selected" : ""}>Percentage (%)</option>
              <option value="fixed" ${cfg.weddingChargeMode === "fixed" ? "selected" : ""}>Fixed (₹)</option>
            </select>
            <input id="adminWedChargeValue" type="number" class="input" min="0" max="10000000" value="${escapeHtml(cfg.weddingChargeValue)}" />
          </div>
        </div>
        <div class="field full">
          <label for="adminUpiQrFile">${escapeHtml(t("adminPayQrUploadLabel"))}</label>
          <input id="adminUpiQrFile" type="file" accept="image/png,image/jpeg,image/webp" class="input" ${isUploadingQr ? "disabled" : ""} />
          <div id="adminUpiQrErrorBox" class="payment-utr-error ${qrErrorMsg ? "" : "hidden"}" role="alert" aria-live="polite">${escapeHtml(qrErrorMsg)}</div>
          <div id="adminUpiQrLivePreviewBox" class="admin-qr-live-preview" data-has-saved-qr="${hasSavedQr ? "true" : "false"}" data-upload-status="${escapeHtml(uploadState.status || (hasSavedQr ? "saved" : "idle"))}">
            <div class="admin-qr-live-preview-head">
              <strong>📷 ${escapeHtml(t("adminPayQrLivePreviewTitle"))}</strong>
              <span id="adminUpiQrPreviewStatus" class="admin-qr-live-badge ${qrStatusClass}">
                ${escapeHtml(qrStatusText)}
              </span>
            </div>
            <div class="admin-qr-live-body">
              <div id="adminUpiQrImgWrap" class="upi-qr-img-wrap ${hasVisibleQr ? "" : "hidden"}">
                <img
                  id="adminUpiQrPreviewImg"
                  src="${hasVisibleQr ? escapeHtml(displayQrSrc) : ""}"
                  alt="Studio UPI QR Code Live Preview"
                  class="upi-qr-preview-thumb"
                />
              </div>
              <div class="admin-qr-live-info">
                <p id="adminUpiQrEmptyNote" class="payment-wait-note ${hasVisibleQr ? "hidden" : ""}">
                  ⚠ ${escapeHtml(t("adminPayQrNotUploadedNote"))}
                </p>
                <p id="adminUpiQrSavedNote" class="admin-qr-saved-note ${hasSavedQr && !isUploadingQr ? "" : "hidden"}">
                  ✓ ${escapeHtml(t("adminPayQrSavedHelper"))}
                </p>
                <div class="admin-qr-live-upi-line">
                  <span>UPI ID:</span>
                  <strong class="upi-id-code">${escapeHtml(cfg.upiId || OFFICIAL_STUDIO_UPI_ID)}</strong>
                </div>
                <button type="button" id="adminUpiQrClearSelectionBtn" class="btn-outline btn-sm ${previewCandidate && !isUploadingQr ? "" : "hidden"}">
                  ${escapeHtml(t("adminPayQrResetSelectionBtn"))}
                </button>
              </div>
            </div>
          </div>
        </div>
        <div class="field full">
          <label class="admin-pay-checkbox ${hasSavedQr ? "" : "is-disabled"}">
            <input id="adminUpiQrVerified" type="checkbox" ${cfg.qrVerified && hasSavedQr ? "checked" : ""} ${hasSavedQr ? "" : "disabled"} />
            <span>${escapeHtml(t("adminPayQrVerifiedLabel"))}</span>
          </label>
        </div>
        <div class="field full">
          <label class="admin-pay-checkbox ${hasSavedQr && cfg.qrVerified ? "" : "is-disabled"}">
            <input id="adminUpiEnabled" type="checkbox" ${isPaymentConfigReady(cfg) ? "checked" : ""} ${hasSavedQr && cfg.qrVerified ? "" : "disabled"} />
            <strong>${escapeHtml(t("adminPayEnableToggle"))}</strong>
          </label>
        </div>
        <div class="field full">
          <label for="adminUpiInstructions">${escapeHtml(t("adminPayInstructionsLabel"))}</label>
          <textarea id="adminUpiInstructions" class="input" rows="2" maxlength="1000" placeholder="Scan QR or pay via UPI ID, then submit your 12-digit UTR number for studio verification.">${escapeHtml(cfg.instructions || "")}</textarea>
        </div>
        <div class="field full">
          <button type="submit" class="btn-gold">${escapeHtml(t("adminPaySaveConfigBtn"))}</button>
        </div>
      </form>

      <div class="admin-pay-records-header">
        <div>
          <h4>🧾 ${escapeHtml(t("adminPayRecordsSectionTitle"))} (${allPayments.length})</h4>
          <p>${escapeHtml(t("adminPayRecordsSectionSub"))}</p>
        </div>
      </div>

      ${duplicateAlertBannerHtml}

      <div class="admin-pay-filters">${filterChipsHtml}</div>
      <div class="admin-payments-list">${rowsHtml}</div>
    </div>
  `;
}
