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

export const PAYMENT_TRANSACTIONS_COLLECTION = "paymentTransactions";

export const DUPLICATE_UTR_ALERT_TITLE = "Duplicate UTR Detected";

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

export const QR_DECODER_CDN_URL = "https://esm.sh/jsqr@1.4.0";

let cachedQrDecoderPromise = null;

/**
 * Parses a standard UPI deep-link QR payload (e.g. upi://pay?pa=9812890081@ybl&pn=...).
 * Strictly rejects non-UPI URLs, missing payee addresses, or invalid UPI IDs.
 */
export function parseUpiQrPayload(rawText = "") {
  const rawPayload = String(rawText || "").trim();
  if (!rawPayload) {
    return {
      valid: false,
      upiId: "",
      payeeName: "",
      rawPayload: "",
      error: "No QR code payload could be decoded from the selected image."
    };
  }
  if (!/^upi:\/\/pay(?:\?|$)/i.test(rawPayload)) {
    return {
      valid: false,
      upiId: "",
      payeeName: "",
      rawPayload,
      error: "Uploaded QR code is not a valid UPI payment QR (expected a upi://pay link)."
    };
  }

  const qIdx = rawPayload.indexOf("?");
  const queryPart = qIdx >= 0 ? rawPayload.slice(qIdx + 1) : "";
  const params = new URLSearchParams(queryPart);
  let rawPa = params.get("pa");
  let rawPn = params.get("pn");
  if (rawPa === null || rawPn === null) {
    for (const [key, value] of params.entries()) {
      const lowerKey = String(key || "").trim().toLowerCase();
      if (rawPa === null && lowerKey === "pa") rawPa = value;
      if (rawPn === null && lowerKey === "pn") rawPn = value;
    }
  }
  const upiId = String(rawPa || "").trim().toLowerCase();
  const payeeName = String(rawPn || "").trim().slice(0, 120);

  if (!upiId || !isValidUpiId(upiId)) {
    return {
      valid: false,
      upiId: "",
      payeeName,
      rawPayload,
      error: "Uploaded UPI QR code does not contain a valid payee UPI ID (pa parameter)."
    };
  }

  return {
    valid: true,
    upiId,
    payeeName,
    rawPayload
  };
}

/**
 * Confirms a decoded QR payload resolves to the exact mandated Studio UPI ID (9812890081@ybl).
 */
export function verifyDecodedUpiQrPayload(rawPayload = "", expectedUpiId = OFFICIAL_STUDIO_UPI_ID) {
  const expectedClean = String(expectedUpiId || OFFICIAL_STUDIO_UPI_ID).trim().toLowerCase();
  if (!isValidUpiId(expectedClean)) {
    return {
      valid: false,
      decodedUpiId: "",
      expectedUpiId: expectedClean,
      rawPayload: String(rawPayload || "").trim(),
      payeeName: "",
      error: "Expected Studio UPI ID configuration is invalid."
    };
  }

  const parsed = parseUpiQrPayload(rawPayload);
  if (!parsed.valid) {
    return {
      valid: false,
      decodedUpiId: "",
      expectedUpiId: expectedClean,
      rawPayload: parsed.rawPayload,
      payeeName: parsed.payeeName,
      error: parsed.error
    };
  }

  if (parsed.upiId !== expectedClean) {
    return {
      valid: false,
      decodedUpiId: parsed.upiId,
      expectedUpiId: expectedClean,
      rawPayload: parsed.rawPayload,
      payeeName: parsed.payeeName,
      error: `QR code UPI ID mismatch: scanned "${parsed.upiId}", but the official Studio UPI ID must be "${expectedClean}". Upload the matching QR image before saving.`
    };
  }

  return {
    valid: true,
    decodedUpiId: parsed.upiId,
    expectedUpiId: expectedClean,
    rawPayload: parsed.rawPayload,
    payeeName: parsed.payeeName
  };
}

function resolveJsQrDecodeFn(mod) {
  if (typeof mod === "function") return mod;
  if (typeof mod?.default === "function") return mod.default;
  if (typeof mod?.default?.default === "function") return mod.default.default;
  return null;
}

/**
 * Lazily loads the browser-based jsQR decoder library (or an injected loader for unit tests).
 */
export async function loadQrDecoderRuntime({ customLoader = null } = {}) {
  if (typeof customLoader === "function") {
    const mod = await customLoader();
    const decodeFn = resolveJsQrDecodeFn(mod);
    if (typeof decodeFn !== "function") {
      throw new Error("QR decoder runtime did not expose a callable decode function.");
    }
    return decodeFn;
  }

  if (!cachedQrDecoderPromise) {
    cachedQrDecoderPromise = import(/* @vite-ignore */ QR_DECODER_CDN_URL)
      .then((mod) => {
        const decodeFn = resolveJsQrDecodeFn(mod);
        if (typeof decodeFn !== "function") {
          throw new Error("jsQR library export is unavailable.");
        }
        return decodeFn;
      })
      .catch((err) => {
        cachedQrDecoderPromise = null;
        throw err;
      });
  }
  return cachedQrDecoderPromise;
}

/**
 * Decodes a QR string from raw RGBA ImageData using jsQR.
 */
export async function decodeQrFromImageData(imageData, { customDecoder = null, customLoader = null } = {}) {
  if (
    !imageData ||
    typeof imageData !== "object" ||
    !imageData.data ||
    !Number.isInteger(imageData.width) ||
    !Number.isInteger(imageData.height) ||
    imageData.width <= 0 ||
    imageData.height <= 0 ||
    imageData.data.length < imageData.width * imageData.height * 4
  ) {
    return "";
  }

  const decoder = typeof customDecoder === "function"
    ? customDecoder
    : await loadQrDecoderRuntime({ customLoader });
  const result = decoder(imageData.data, imageData.width, imageData.height, {
    inversionAttempts: "attemptBoth"
  });
  return String(result?.data || "").trim();
}

function loadBrowserImageForQrScan(source) {
  return new Promise((resolve, reject) => {
    if (typeof Image === "undefined") {
      reject(new Error("Browser Image API is unavailable to verify the QR image."));
      return;
    }

    const loadDataUrl = (dataUrl) => {
      const img = new Image();
      img.onload = () => resolve(img);
      img.onerror = () => reject(new Error("Invalid or corrupted QR image file. Please select a valid JPG, PNG, or WebP QR image."));
      img.src = dataUrl;
    };

    if (typeof source === "string") {
      loadDataUrl(source);
      return;
    }

    if (typeof FileReader === "undefined") {
      reject(new Error("Browser FileReader API is unavailable to verify the QR image."));
      return;
    }

    const reader = new FileReader();
    reader.onerror = () => reject(new Error("Unable to read the selected QR image file."));
    reader.onload = () => loadDataUrl(String(reader.result || ""));
    reader.readAsDataURL(source);
  });
}

async function decodeQrFromBrowserImage(img, { customDecoder = null, customLoader = null, barcodeDetector = null } = {}) {
  if (typeof document === "undefined" || typeof document.createElement !== "function") {
    throw new Error("Browser canvas is unavailable to decode the uploaded QR image.");
  }

  const srcW = Math.max(1, img.naturalWidth || img.width || 1);
  const srcH = Math.max(1, img.naturalHeight || img.height || 1);

  const renderCropToImageData = ({ sx = 0, sy = 0, sw = srcW, sh = srcH, maxDim = 1000 }) => {
    const scale = Math.min(1, maxDim / Math.max(sw, sh));
    const canvas = document.createElement("canvas");
    canvas.width = Math.max(1, Math.round(sw * scale));
    canvas.height = Math.max(1, Math.round(sh * scale));
    const ctx2d = canvas.getContext("2d", { willReadFrequently: true });
    if (!ctx2d) {
      throw new Error("Browser 2D canvas context is unavailable to decode the QR image.");
    }
    ctx2d.fillStyle = "#FFFFFF";
    ctx2d.fillRect(0, 0, canvas.width, canvas.height);
    ctx2d.drawImage(img, sx, sy, sw, sh, 0, 0, canvas.width, canvas.height);
    return { canvas, imageData: ctx2d.getImageData(0, 0, canvas.width, canvas.height) };
  };

  // Build multi-pass scan regions: full scaled image, center QR band for tall payment posters (PhonePe/GPay/Paytm), and higher-res full scan
  const scanPasses = [{ sx: 0, sy: 0, sw: srcW, sh: srcH, maxDim: 1000 }];
  if (srcH > srcW * 1.15) {
    scanPasses.push({
      sx: 0,
      sy: Math.round(srcH * 0.14),
      sw: srcW,
      sh: Math.round(srcH * 0.72),
      maxDim: 800
    });
  } else {
    scanPasses.push({
      sx: Math.round(srcW * 0.08),
      sy: Math.round(srcH * 0.08),
      sw: Math.round(srcW * 0.84),
      sh: Math.round(srcH * 0.84),
      maxDim: 800
    });
  }
  if (Math.max(srcW, srcH) > 1000) {
    scanPasses.push({ sx: 0, sy: 0, sw: srcW, sh: srcH, maxDim: 1600 });
  }

  let jsQrLoadError = null;
  let decoder = typeof customDecoder === "function" ? customDecoder : null;
  if (!decoder) {
    try {
      decoder = await loadQrDecoderRuntime({ customLoader });
    } catch (err) {
      jsQrLoadError = err;
    }
  }

  let lastCanvas = null;
  let firstImageData = null;
  for (const pass of scanPasses) {
    const { canvas, imageData } = renderCropToImageData(pass);
    lastCanvas = canvas;
    if (!firstImageData) firstImageData = imageData;
    if (decoder) {
      const decoded = await decodeQrFromImageData(imageData, { customDecoder: decoder });
      if (decoded) return decoded;
    }
  }

  // High-contrast grayscale fallback pass for low-contrast phone screenshots or stylized QR posters
  if (decoder && firstImageData?.data?.length) {
    const boosted = new Uint8ClampedArray(firstImageData.data.length);
    for (let i = 0; i < firstImageData.data.length; i += 4) {
      const lum =
        0.299 * firstImageData.data[i] +
        0.587 * firstImageData.data[i + 1] +
        0.114 * firstImageData.data[i + 2];
      const bw = lum >= 145 ? 255 : 0;
      boosted[i] = bw;
      boosted[i + 1] = bw;
      boosted[i + 2] = bw;
      boosted[i + 3] = 255;
    }
    const decodedBoosted = await decodeQrFromImageData(
      { data: boosted, width: firstImageData.width, height: firstImageData.height },
      { customDecoder: decoder }
    );
    if (decodedBoosted) return decodedBoosted;
  }

  // Fallback to native browser BarcodeDetector API if available (works even offline on supported Chromium/Android browsers)
  const detectorInstance =
    barcodeDetector ||
    (typeof globalThis !== "undefined" && typeof globalThis.BarcodeDetector === "function"
      ? new globalThis.BarcodeDetector({ formats: ["qr_code"] })
      : null);
  if (detectorInstance && typeof detectorInstance.detect === "function") {
    try {
      const detected = await detectorInstance.detect(lastCanvas || img);
      const firstValue = String(detected?.[0]?.rawValue || "").trim();
      if (firstValue) return firstValue;
    } catch {
      // Ignore fallback detector failure and fail closed below
    }
  }

  if (jsQrLoadError) {
    throw new Error(
      `Unable to load browser QR decoder to verify official UPI ID (${OFFICIAL_STUDIO_UPI_ID}). Check internet connectivity and retry.`
    );
  }
  return "";
}

/**
 * Verifies that an uploaded QR image Blob (or File, Data URL, or ImageData) decodes cleanly
 * via browser-based QR decoding (jsQR + BarcodeDetector fallback) and resolves to the exact
 * mandated official Studio UPI ID (9812890081@ybl).
 * Fails closed whenever decoding fails or the decoded UPI ID does not match.
 */
export async function verifyQrCode(
  qrImageBlob,
  {
    expectedUpiId = OFFICIAL_STUDIO_UPI_ID,
    customDecoder = null,
    customLoader = null,
    barcodeDetector = null
  } = {}
) {
  const expectedClean = String(expectedUpiId || OFFICIAL_STUDIO_UPI_ID).trim().toLowerCase();
  const fail = (error) => ({
    valid: false,
    decodedUpiId: "",
    expectedUpiId: expectedClean,
    rawPayload: "",
    payeeName: "",
    error
  });

  if (!qrImageBlob) {
    return fail("Please select a JPG, PNG, or WebP UPI QR image to verify.");
  }

  try {
    // 1. Direct ImageData-like object { data, width, height }
    if (
      typeof qrImageBlob === "object" &&
      qrImageBlob.data &&
      Number.isInteger(qrImageBlob.width) &&
      Number.isInteger(qrImageBlob.height)
    ) {
      const decodedText = await decodeQrFromImageData(qrImageBlob, { customDecoder, customLoader });
      if (!decodedText) {
        return fail(
          `Could not detect a readable QR code in the uploaded image. Upload a clear UPI QR code for ${expectedClean}.`
        );
      }
      return verifyDecodedUpiQrPayload(decodedText, expectedClean);
    }

    // 2. Base64 Data URL string
    if (typeof qrImageBlob === "string") {
      const cleanUrl = qrImageBlob.trim();
      if (!isUploadedQrImageDataUrl(cleanUrl)) {
        return fail("Official UPI QR image must be an uploaded JPG, PNG, or WebP image under 180 KB.");
      }
      const img = await loadBrowserImageForQrScan(cleanUrl);
      const decodedText = await decodeQrFromBrowserImage(img, {
        customDecoder,
        customLoader,
        barcodeDetector
      });
      if (!decodedText) {
        return fail(
          `Could not decode a readable QR code from the uploaded image. Upload a sharp UPI QR image for ${expectedClean}.`
        );
      }
      return verifyDecodedUpiQrPayload(decodedText, expectedClean);
    }

    // 3. Browser Blob / File
    if (typeof qrImageBlob === "object") {
      const blobType = String(qrImageBlob.type || "").trim().toLowerCase();
      const blobName = String(qrImageBlob.name || "").trim();
      const blobSize = Number(qrImageBlob.size || 0);
      const isGenericNamelessBlob = !blobName && (!blobType || blobType === "application/octet-stream");

      if (blobSize <= 0) {
        return fail("Selected QR image file is empty. Please choose a valid JPG, PNG, or WebP QR image.");
      }
      if (blobSize > 15 * 1024 * 1024) {
        return fail("Selected image file is too large (max 15 MB). Please choose a smaller QR image.");
      }
      if (!isGenericNamelessBlob && ("name" in qrImageBlob || "type" in qrImageBlob)) {
        const metaCheck = isAllowedPaymentImageFileMeta(qrImageBlob);
        if (!metaCheck.valid) {
          return fail(metaCheck.error);
        }
      }
      const img = await loadBrowserImageForQrScan(qrImageBlob);
      const decodedText = await decodeQrFromBrowserImage(img, {
        customDecoder,
        customLoader,
        barcodeDetector
      });
      if (!decodedText) {
        return fail(
          `Could not decode a readable QR code from the selected image. Upload a clear PhonePe/UPI QR image for ${expectedClean}.`
        );
      }
      return verifyDecodedUpiQrPayload(decodedText, expectedClean);
    }

    return fail("Unsupported QR image input.");
  } catch (err) {
    return fail(
      err?.message || `Unable to verify uploaded QR code against official UPI ID (${expectedClean}).`
    );
  }
}

export async function verifyUploadedQrImageMatchesOfficialUpi(source, options = {}) {
  return verifyQrCode(source, options);
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

export function createDuplicateUtrError({
  utrKey = "",
  utrNumber = "",
  message = DUPLICATE_UTR_ERROR_MESSAGE
} = {}) {
  const err = new Error(message || DUPLICATE_UTR_ERROR_MESSAGE);
  err.code = "payment/duplicate-utr";
  err.isDuplicateUtr = true;
  err.utrKey = String(utrKey || "").trim();
  err.utrNumber = String(utrNumber || "").trim();
  return err;
}

export function isDuplicateUtrError(err) {
  if (!err || typeof err !== "object") return false;
  return (
    err.code === "payment/duplicate-utr" ||
    err.isDuplicateUtr === true ||
    err.message === DUPLICATE_UTR_ERROR_MESSAGE
  );
}

/**
 * Atomically checks for duplicate UTR numbers within the `paymentTransactions` collection
 * using a Firestore transaction (deterministic doc ID `paymentTransactions/{canonicalUtrKey}`)
 * and reserves the UTR in a single atomic commit.
 *
 * All reads (`beforeClaim` + `tx.get(paymentTransactions/{canonicalKey})`) execute strictly
 * before any writes (`tx.set(paymentTransactions/{canonicalKey})` + `afterClaim`).
 */
export async function checkAndClaimPaymentTransactionUtr({
  db,
  fsMod,
  utrNumber = "",
  paymentId = "",
  customerUid = "",
  beforeClaim = null,
  afterClaim = null,
  throwOnDuplicate = false
} = {}) {
  const utrCheck = validateUtrReference(utrNumber);
  if (!utrCheck.valid) {
    throw new Error(utrCheck.error);
  }
  const cleanPaymentId = String(paymentId || "").trim();
  if (!cleanPaymentId) {
    throw new Error("A valid payment record ID is required to register a UTR.");
  }
  const cleanUid = String(customerUid || "").trim();
  if (!cleanUid) {
    throw new Error("Authenticated user ID is required to register a UTR.");
  }
  if (
    !db ||
    !fsMod ||
    typeof fsMod.doc !== "function" ||
    typeof fsMod.runTransaction !== "function"
  ) {
    throw new Error("Firestore transaction engine is unavailable.");
  }

  const canonicalKey = utrCheck.canonicalKey || normalizeUtrKey(utrCheck.clean);
  const txDocRef = fsMod.doc(db, PAYMENT_TRANSACTIONS_COLLECTION, canonicalKey);
  const timestampValue =
    typeof fsMod.serverTimestamp === "function" ? fsMod.serverTimestamp() : new Date();

  try {
    await fsMod.runTransaction(db, async (tx) => {
      let beforeResult = null;
      if (typeof beforeClaim === "function") {
        beforeResult = await beforeClaim(tx, {
          canonicalKey,
          cleanUtr: utrCheck.clean,
          txDocRef
        });
      }

      const existingTxSnap = await tx.get(txDocRef);
      if (existingTxSnap && typeof existingTxSnap.exists === "function" && existingTxSnap.exists()) {
        throw createDuplicateUtrError({
          utrKey: canonicalKey,
          utrNumber: utrCheck.clean
        });
      }

      tx.set(txDocRef, {
        utrKey: canonicalKey,
        utrNumber: utrCheck.clean,
        paymentId: cleanPaymentId,
        customerUid: cleanUid,
        status: "Submitted for Verification",
        createdAt: timestampValue,
        updatedAt: timestampValue
      });

      if (typeof afterClaim === "function") {
        await afterClaim(tx, {
          canonicalKey,
          cleanUtr: utrCheck.clean,
          txDocRef,
          beforeResult
        });
      }
    });

    return {
      ok: true,
      duplicate: false,
      utrKey: canonicalKey,
      utrNumber: utrCheck.clean,
      collection: PAYMENT_TRANSACTIONS_COLLECTION
    };
  } catch (err) {
    const code = String(err?.code || "").toLowerCase();
    const isCollision =
      isDuplicateUtrError(err) ||
      code.includes("permission-denied") ||
      code.includes("already-exists") ||
      code.includes("aborted") ||
      code.includes("failed-precondition");

    if (isCollision) {
      if (throwOnDuplicate) {
        throw createDuplicateUtrError({
          utrKey: canonicalKey,
          utrNumber: utrCheck.clean
        });
      }
      return {
        ok: false,
        duplicate: true,
        utrKey: canonicalKey,
        utrNumber: utrCheck.clean,
        collection: PAYMENT_TRANSACTIONS_COLLECTION,
        error: DUPLICATE_UTR_ERROR_MESSAGE
      };
    }

    throw err;
  }
}

/**
 * Renders a prominent, accessible alert banner specifically when a duplicate UTR is detected.
 */
export function renderDuplicateUtrAlertHtml({
  utrNumber = "",
  message = DUPLICATE_UTR_ERROR_MESSAGE,
  title = DUPLICATE_UTR_ALERT_TITLE,
  hint = "Check your PhonePe / GPay / UPI app payment history and enter a unique, unused 12-digit UTR reference number.",
  escapeHtml = defaultEscapeHtml
} = {}) {
  const cleanUtr = String(utrNumber || "").trim();
  return `
    <div class="payment-utr-duplicate-alert-inner" data-duplicate-utr-alert="true">
      <div class="payment-utr-duplicate-alert-head">
        <strong>⚠ ${escapeHtml(title || DUPLICATE_UTR_ALERT_TITLE)}</strong>
        ${cleanUtr ? `<span class="payment-utr-duplicate-utr-code">UTR: ${escapeHtml(cleanUtr)}</span>` : ""}
      </div>
      <p class="payment-utr-duplicate-alert-msg">${escapeHtml(message || DUPLICATE_UTR_ERROR_MESSAGE)}</p>
      ${hint ? `<small class="payment-utr-duplicate-alert-hint">${escapeHtml(hint)}</small>` : ""}
    </div>
  `;
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
  const rawModelVal = Number(input.modelAdvanceValue);
  const modelAdvanceValue = Number.isFinite(rawModelVal) ? Math.max(0, rawModelVal) : 0;
  const weddingChargeMode = input.weddingChargeMode === "fixed" ? "fixed" : "percentage";
  const rawWedVal = Number(input.weddingChargeValue);
  const weddingChargeValue = Number.isFinite(rawWedVal) ? Math.max(0, rawWedVal) : 0;
  const hasUploadedQr = isUploadedQrImageDataUrl(qrImageDataUrl);

  if (containsSensitiveBankingData(instructions) || containsSensitiveBankingData(payeeName)) {
    return { valid: false, error: "Never store UPI PINs, OTPs, passwords, or banking credentials." };
  }
  if (upiId.length > 120) {
    return { valid: false, error: "UPI ID must be 120 characters or fewer." };
  }
  if (payeeName.length > 120) {
    return { valid: false, error: "Payee Name must be 120 characters or fewer." };
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

  if (modelAdvanceValue > 10000000 || weddingChargeValue > 10000000) {
    return { valid: false, error: "Advance / booking charge amount must be ₹1,00,00,000 or less." };
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
  scannedUpiPanelHtml = "",
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
                      <div class="payment-utr-duplicate-alert hidden" data-duplicate-utr-alert-box role="alert" aria-live="assertive"></div>
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
      <div class="payment-section-banner payment-section-banner-row">
        <div>
          <strong>💳 ${escapeHtml(t("payPortalTitle"))}</strong>
          <p>${escapeHtml(t("payPortalSubtitle"))}</p>
        </div>
        <button
          type="button"
          class="btn-gold btn-sm portal-scan-qr-btn"
          data-open-portal-qr-scanner="true"
        >
          <span aria-hidden="true">📷</span>
          <span>${escapeHtml(t("portalScanQrToPayBtn"))}</span>
        </button>
      </div>

      ${scannedUpiPanelHtml || ""}

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

export const PAYMENT_HISTORY_STATUS_FILTERS = ["all", "verified", "pending", "rejected"];

export function resolvePaymentHistoryTimestampMs(raw) {
  if (!raw) return 0;
  if (typeof raw === "number") return Number.isFinite(raw) ? raw : 0;
  if (typeof raw === "string") {
    const ms = Date.parse(raw);
    return Number.isFinite(ms) ? ms : 0;
  }
  if (typeof raw === "object") {
    if (typeof raw.toMillis === "function") return raw.toMillis();
    if (typeof raw.seconds === "number") return raw.seconds * 1000;
  }
  return 0;
}

export function classifyPaymentVerificationBucket(paymentStatus = "") {
  const norm = String(paymentStatus || "").trim().toLowerCase();
  if (norm === "payment verified" || norm === "fully paid" || norm === "partially paid") {
    return "verified";
  }
  if (norm === "payment rejected" || norm === "rejected") {
    return "rejected";
  }
  if (norm === "refund requested" || norm === "refund processed") {
    return "refunded";
  }
  return "pending";
}

/**
 * Normalizes a customer's Firestore UPI payment records for the scrollable Payment History list.
 * Strictly enforces customer UID isolation and strips all admin-only internal fields.
 */
export function normalizeUpiPaymentHistoryItems(payments = [], currentUserUid = "") {
  const owned = filterCustomerPayments(payments, currentUserUid);
  return owned
    .map((p) => {
      const utrNumber = String(p.utrNumber || p.utrKey || "").trim();
      const verifiedAmt = Number(p.verifiedAmount || 0);
      const paidAmt = Number(p.paidAmount || 0);
      const txAmt = Number(p.transactionAmount || 0);
      const advanceAmt = Number(p.requiredAdvance || 0);
      const totalAmt = Number(p.totalPrice || 0);

      let amount = 0;
      let amountContext = "due";
      if (verifiedAmt > 0) {
        amount = verifiedAmt;
        amountContext = "verified";
      } else if (paidAmt > 0) {
        amount = paidAmt;
        amountContext = "paid";
      } else if (txAmt > 0) {
        amount = txAmt;
        amountContext = "submitted";
      } else if (advanceAmt > 0) {
        amount = advanceAmt;
        amountContext = "advance";
      } else if (totalAmt > 0) {
        amount = totalAmt;
        amountContext = "due";
      }

      const rawStatus = String(p.paymentStatus || "Payment Pending").trim();
      const verificationBucket = classifyPaymentVerificationBucket(rawStatus);
      const dateValue =
        p.submittedAt ||
        p.verifiedAt ||
        p.transactionCreatedAt ||
        p.updatedAt ||
        p.createdAt ||
        p.shootDate ||
        "";
      const sortTimestampMs = Math.max(
        resolvePaymentHistoryTimestampMs(p.submittedAt),
        resolvePaymentHistoryTimestampMs(p.verifiedAt),
        resolvePaymentHistoryTimestampMs(p.transactionCreatedAt),
        resolvePaymentHistoryTimestampMs(p.updatedAt),
        resolvePaymentHistoryTimestampMs(p.createdAt)
      );
      const title =
        p.paymentCategory === "wedding_booking"
          ? String(p.eventType || p.serviceType || "Wedding / Event Booking").trim()
          : `${String(p.modelName || "Studio Model").trim()} — ${String(p.serviceType || "Model Shoot").trim()}`;

      return {
        id: String(p.id || ""),
        utrNumber,
        hasUtr: utrNumber.length > 0,
        amount,
        amountContext,
        totalPrice: totalAmt,
        paidAmount: paidAmt,
        verifiedAmount: verifiedAmt,
        dateValue,
        sortTimestampMs,
        shootDate: String(p.shootDate || "").trim(),
        paymentStatus: rawStatus,
        verificationBucket,
        statusClass: getPaymentStatusClass(rawStatus),
        paymentCategory: p.paymentCategory === "wedding_booking" ? "wedding_booking" : "model_booking",
        title,
        rejectionReason: String(p.rejectionReason || "").trim(),
        receiptNumber: String(p.receiptNumber || "").trim()
      };
    })
    .sort((a, b) => b.sortTimestampMs - a.sortTimestampMs);
}

export function filterPaymentHistoryByStatus(items = [], statusFilter = "all") {
  const safeFilter = PAYMENT_HISTORY_STATUS_FILTERS.includes(statusFilter) ? statusFilter : "all";
  if (safeFilter === "all") return items;
  return items.filter((item) => item.verificationBucket === safeFilter);
}

/**
 * Renders the Customer Portal 'Payment History' section displaying all past UPI payments
 * fetched from Firestore in a clean scrollable list with Transaction UTR, Amount, Date,
 * and Verification Status.
 */
export function renderCustomerPaymentHistoryHtml({
  payments = [],
  currentUserUid = "",
  statusFilter = "all",
  isLoading = false,
  errorMessage = "",
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  if (!String(currentUserUid || "").trim()) {
    return `<div class="portal-empty-card"><p>${escapeHtml(t("portalSignInRequired"))}</p></div>`;
  }

  const allItems = normalizeUpiPaymentHistoryItems(payments, currentUserUid);
  const safeFilter = PAYMENT_HISTORY_STATUS_FILTERS.includes(statusFilter) ? statusFilter : "all";
  const visibleItems = filterPaymentHistoryByStatus(allItems, safeFilter);

  const verifiedItems = allItems.filter((i) => i.verificationBucket === "verified");
  const pendingItems = allItems.filter((i) => i.verificationBucket === "pending");
  const rejectedItems = allItems.filter((i) => i.verificationBucket === "rejected");
  const totalVerifiedPaid = verifiedItems.reduce((sum, i) => sum + Number(i.verifiedAmount || i.paidAmount || i.amount || 0), 0);

  const filterLabelMap = {
    all: `${t("portalPayHistoryFilterAll")} (${allItems.length})`,
    verified: `${t("portalPayHistoryFilterVerified")} (${verifiedItems.length})`,
    pending: `${t("portalPayHistoryFilterPending")} (${pendingItems.length})`,
    rejected: `${t("portalPayHistoryFilterRejected")} (${rejectedItems.length})`
  };

  const filterChipsHtml = PAYMENT_HISTORY_STATUS_FILTERS.map(
    (key) => `
      <button
        type="button"
        class="chip-btn ${safeFilter === key ? "active" : ""}"
        data-payment-history-filter="${escapeHtml(key)}"
        aria-pressed="${safeFilter === key ? "true" : "false"}"
      >
        ${escapeHtml(filterLabelMap[key])}
      </button>
    `
  ).join("");

  const loadingHtml = isLoading
    ? `<div class="payment-history-loading-bar" role="status" aria-live="polite">⏳ ${escapeHtml(t("portalPayHistoryLoading"))}</div>`
    : "";

  const errorHtml = errorMessage
    ? `<div class="payment-utr-error" role="alert" aria-live="assertive">⚠ ${escapeHtml(errorMessage)}</div>`
    : "";

  const rowsHtml = visibleItems.length
    ? visibleItems
        .map((item) => {
          const formattedDate = item.dateValue
            ? formatNetworkTimestamp(item.dateValue, lang)
            : "—";
          const amountDisplay = item.amount > 0 ? formatInrAmount(item.amount) : t("payAwaitingAdminPrice");
          const amountSubLabel =
            item.amountContext === "verified" || item.amountContext === "paid"
              ? t("portalPayHistoryAmtPaidLabel")
              : item.amountContext === "submitted"
              ? t("portalPayHistoryAmtSubmittedLabel")
              : t("portalPayHistoryAmtDueLabel");

          return `
            <article
              class="payment-history-row"
              role="listitem"
              data-payment-history-id="${escapeHtml(item.id)}"
              data-payment-history-bucket="${escapeHtml(item.verificationBucket)}"
            >
              <div class="payment-history-row-head">
                <div class="payment-history-title-wrap">
                  <span class="payment-cat-tag">${escapeHtml(item.paymentCategory === "wedding_booking" ? t("payWeddingCategory") : t("payModelCategory"))}</span>
                  <h4 class="payment-history-title">${escapeHtml(item.title)}</h4>
                </div>
                <span
                  class="payment-status-pill ${escapeHtml(item.statusClass)}"
                  data-payment-history-status="${escapeHtml(item.paymentStatus)}"
                >
                  ${escapeHtml(item.paymentStatus)}
                </span>
              </div>

              <div class="payment-history-fields-grid">
                <div class="payment-history-field">
                  <span class="payment-history-field-label">${escapeHtml(t("portalPayHistoryUtrCol"))}</span>
                  <div class="payment-history-utr-wrap">
                    <strong class="payment-history-utr-value" data-payment-history-utr="${escapeHtml(item.utrNumber)}">
                      ${escapeHtml(item.hasUtr ? item.utrNumber : t("portalPayHistoryNoUtr"))}
                    </strong>
                    ${
                      item.hasUtr
                        ? `<button
                             type="button"
                             class="payment-history-copy-btn"
                             data-copy-history-utr="${escapeHtml(item.utrNumber)}"
                             aria-label="${escapeHtml(t("portalPayHistoryCopyUtrAria"))}"
                             title="${escapeHtml(t("portalPayHistoryCopyUtrAria"))}"
                           >📋</button>`
                        : ""
                    }
                  </div>
                </div>

                <div class="payment-history-field">
                  <span class="payment-history-field-label">${escapeHtml(t("portalPayHistoryAmountCol"))}</span>
                  <div class="payment-history-amount-wrap">
                    <strong class="payment-history-amount-value" data-payment-history-amount="${escapeHtml(String(item.amount))}">
                      ${escapeHtml(amountDisplay)}
                    </strong>
                    <small class="payment-history-amount-sub">${escapeHtml(amountSubLabel)}</small>
                  </div>
                </div>

                <div class="payment-history-field">
                  <span class="payment-history-field-label">${escapeHtml(t("portalPayHistoryDateCol"))}</span>
                  <strong class="payment-history-date-value" data-payment-history-date="${escapeHtml(formattedDate)}">
                    ${escapeHtml(formattedDate)}
                  </strong>
                  ${item.shootDate ? `<small class="payment-history-amount-sub">📅 ${escapeHtml(item.shootDate)}</small>` : ""}
                </div>

                <div class="payment-history-field">
                  <span class="payment-history-field-label">${escapeHtml(t("portalPayHistoryStatusCol"))}</span>
                  <strong class="payment-history-verify-text ${escapeHtml(item.statusClass)}">
                    ${escapeHtml(item.paymentStatus)}
                  </strong>
                </div>
              </div>

              ${
                item.rejectionReason
                  ? `<div class="payment-reject-note" style="margin-top:8px;">⚠ ${escapeHtml(t("payRejectionReasonLabel"))}: ${escapeHtml(item.rejectionReason)}</div>`
                  : ""
              }

              <div class="payment-history-row-footer">
                <span class="payment-history-ref-id">ID: ${escapeHtml(item.receiptNumber || item.id)}</span>
                <button
                  type="button"
                  class="btn-outline btn-sm"
                  data-print-payment-receipt="${escapeHtml(item.id)}"
                >
                  🖨 ${escapeHtml(t("payDownloadReceiptBtn"))}
                </button>
              </div>
            </article>
          `;
        })
        .join("")
    : `
      <div class="studio-card portal-empty-card payment-history-empty">
        <p style="margin:0 0 12px;color:var(--text-secondary);">
          ${escapeHtml(allItems.length ? t("portalPayHistoryNoFilterMatch") : t("portalPayHistoryEmpty"))}
        </p>
        <div class="payment-history-empty-actions">
          <button type="button" class="btn-gold btn-sm" data-open-portal-qr-scanner="true">
            📷 ${escapeHtml(t("portalScanQrToPayBtn"))}
          </button>
          <button type="button" class="btn-outline btn-sm" data-portal-tab="my-payments">
            💳 ${escapeHtml(t("portalTabMyPayments"))}
          </button>
        </div>
      </div>
    `;

  return `
    <div class="portal-payment-history-panel" data-portal-payment-history="true">
      <div class="portal-payment-history-header">
        <div>
          <h3 style="margin:0;color:var(--gold);">🧾 ${escapeHtml(t("portalTabPaymentHistory"))}</h3>
          <p style="margin:4px 0 0;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("portalPayHistorySubtitle"))}</p>
        </div>
        <button
          id="portalRefreshPaymentHistoryBtn"
          type="button"
          class="btn-outline btn-sm"
          data-refresh-payment-history="true"
          ${isLoading ? "disabled" : ""}
        >
          ↻ ${escapeHtml(t("portalPayHistoryRefreshBtn"))}
        </button>
      </div>

      <div class="payment-history-kpi-grid">
        <div class="payment-history-kpi-card">
          <span>${escapeHtml(t("portalPayHistoryKpiTotalCount"))}</span>
          <strong>${allItems.length}</strong>
        </div>
        <div class="payment-history-kpi-card is-verified">
          <span>${escapeHtml(t("portalPayHistoryKpiVerifiedPaid"))}</span>
          <strong>${escapeHtml(formatInrAmount(totalVerifiedPaid))}</strong>
        </div>
        <div class="payment-history-kpi-card is-pending">
          <span>${escapeHtml(t("portalPayHistoryKpiPending"))}</span>
          <strong>${pendingItems.length}</strong>
        </div>
      </div>

      <div class="payment-history-filters" role="group" aria-label="${escapeHtml(t("portalTabPaymentHistory"))}">
        ${filterChipsHtml}
      </div>

      ${loadingHtml}
      ${errorHtml}

      <div
        id="portalPaymentHistoryList"
        class="portal-payment-history-scroll"
        role="list"
        aria-label="${escapeHtml(t("portalTabPaymentHistory"))}"
      >
        ${rowsHtml}
      </div>
    </div>
  `;
}

