/**
 * STAR ARTIST STUDIO — Customer Portal Camera QR Scanner & UPI Pre-Fill Utility
 *
 * Security & Safety Guarantees:
 *  - Only accepts strict `upi://pay` QR payloads and blocks arbitrary web/script URLs.
 *  - Verifies scanned payee address (`pa`) matches the mandated official Studio UPI ID (9812890081@ybl).
 *  - Sanitizes and bounds extracted amount (`am`), payee name (`pn`), note (`tn`), and ref (`tr`).
 *  - Always releases camera MediaStreamTracks immediately on detection, modal close, or sign-out.
 *  - Never auto-verifies payments; Admin still manually verifies submitted UTR numbers.
 */

import {
  DEFAULT_PAYMENT_CONFIG,
  OFFICIAL_STUDIO_UPI_ID,
  calculateAdvanceAndBalance,
  decodeQrFromImageData,
  formatInrAmount,
  isValidUpiId,
  loadQrDecoderRuntime,
  verifyQrCode
} from "./payment-system.js";

const MAX_UPI_SCAN_AMOUNT_INR = 100000000;
export const CAMERA_PERMISSION_ACK_STORAGE_KEY = "star_artist_qr_camera_rationale_ack_v1";
const ACTIVE_PAYABLE_STATUSES = new Set([
  "Payment Pending",
  "Payment Rejected",
  "Partially Paid"
]);

/**
 * Checks whether the user has already seen and acknowledged the camera permission
 * rationale notice before scanning a studio payment QR code.
 */
export function hasAcknowledgedQrCameraPermission(storage = typeof localStorage !== "undefined" ? localStorage : null) {
  try {
    return Boolean(storage && storage.getItem(CAMERA_PERMISSION_ACK_STORAGE_KEY) === "1");
  } catch {
    return false;
  }
}

/**
 * Records that the user acknowledged the single-purpose camera usage notice.
 */
export function markQrCameraPermissionAcknowledged(storage = typeof localStorage !== "undefined" ? localStorage : null) {
  try {
    if (storage && typeof storage.setItem === "function") {
      storage.setItem(CAMERA_PERMISSION_ACK_STORAGE_KEY, "1");
      return true;
    }
  } catch {
    // Ignore storage quota or private-mode errors
  }
  return false;
}

/**
 * Renders the descriptive first-time camera permission request UI component explaining
 * that camera access is used exclusively to scan STAR ARTIST STUDIO payment QR codes.
 */
export function renderCameraPermissionNoticeHtml({
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  return `
    <div class="portal-qr-permission-card" data-qr-permission-notice="true">
      <div class="portal-qr-permission-icon" aria-hidden="true">📷</div>
      <h4 id="portalQrPermissionHeading" class="portal-qr-permission-title">
        ${escapeHtml(t("portalScanQrPermTitle"))}
      </h4>
      <p class="portal-qr-permission-desc">
        ${escapeHtml(t("portalScanQrPermDesc"))}
      </p>
      <ul class="portal-qr-permission-points">
        <li>
          <span class="portal-qr-perm-check" aria-hidden="true">✓</span>
          <span>${escapeHtml(t("portalScanQrPermPoint1"))}</span>
        </li>
        <li>
          <span class="portal-qr-perm-check" aria-hidden="true">✓</span>
          <span>${escapeHtml(t("portalScanQrPermPoint2"))}</span>
        </li>
        <li>
          <span class="portal-qr-perm-check" aria-hidden="true">✓</span>
          <span>${escapeHtml(t("portalScanQrPermPoint3"))}</span>
        </li>
      </ul>
      <div class="portal-qr-permission-actions">
        <button
          id="portalQrConfirmCameraBtn"
          type="button"
          class="btn-gold portal-qr-perm-allow-btn"
          data-confirm-qr-camera="true"
        >
          ${escapeHtml(t("portalScanQrPermAllowBtn"))}
        </button>
        <div class="portal-qr-permission-secondary">
          <label class="btn-outline btn-sm portal-qr-upload-label" for="portalQrFallbackFile">
            <span aria-hidden="true">🖼</span>
            <span>${escapeHtml(t("portalScanQrUploadFallbackBtn"))}</span>
          </label>
          <button
            type="button"
            class="btn-outline btn-sm"
            data-cancel-qr-camera="true"
          >
            ${escapeHtml(t("portalScanQrPermCancelBtn"))}
          </button>
        </div>
      </div>
    </div>
  `;
}

function defaultEscapeHtml(str) {
  return String(str ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function getCaseInsensitiveParam(params, keyName) {
  const exact = params.get(keyName);
  if (exact !== null) return exact;
  const target = keyName.toLowerCase();
  for (const [k, v] of params.entries()) {
    if (String(k || "").trim().toLowerCase() === target) {
      return v;
    }
  }
  return null;
}

/**
 * Parses a scanned UPI payment QR payload (`upi://pay?...`), extracts amount (`am`) and
 * payment metadata, and enforces the official Studio UPI ID (`9812890081@ybl`).
 */
export function parseUpiPaymentQrDetails(
  rawText = "",
  { expectedUpiId = OFFICIAL_STUDIO_UPI_ID, requireOfficialUpi = true } = {}
) {
  const rawPayload = String(rawText || "").trim();
  const expectedClean = String(expectedUpiId || OFFICIAL_STUDIO_UPI_ID).trim().toLowerCase();

  const fail = (error, extra = {}) => ({
    valid: false,
    upiId: "",
    payeeName: "",
    amount: 0,
    hasExplicitAmount: false,
    currency: "INR",
    note: "",
    transactionRef: "",
    rawPayload,
    isOfficialStudioUpi: false,
    ...extra,
    error
  });

  if (!rawPayload) {
    return fail("No QR code data was detected. Point the camera clearly at a UPI payment QR code.");
  }

  if (!/^upi:\/\/pay(?:\?|$)/i.test(rawPayload)) {
    return fail("Scanned QR code is not a valid UPI payment link (expected upi://pay).");
  }

  const qIdx = rawPayload.indexOf("?");
  const queryPart = qIdx >= 0 ? rawPayload.slice(qIdx + 1) : "";
  const params = new URLSearchParams(queryPart);

  const upiId = String(getCaseInsensitiveParam(params, "pa") || "").trim().toLowerCase();
  if (!upiId || !isValidUpiId(upiId)) {
    return fail("Scanned UPI QR code does not contain a valid payee UPI ID.");
  }

  const isOfficialStudioUpi = upiId === expectedClean;
  if (requireOfficialUpi && !isOfficialStudioUpi) {
    return fail(
      `Security alert: Scanned QR points to "${upiId}", not the official STAR ARTIST STUDIO UPI ID (${expectedClean}). Payment was blocked.`,
      { upiId, isOfficialStudioUpi: false }
    );
  }

  const payeeName = String(getCaseInsensitiveParam(params, "pn") || "STAR ARTIST STUDIO")
    .replace(/[\r\n\t]+/g, " ")
    .trim()
    .slice(0, 120) || "STAR ARTIST STUDIO";

  const rawCurrency = String(getCaseInsensitiveParam(params, "cu") || "INR").trim().toUpperCase();
  if (rawCurrency && rawCurrency !== "INR") {
    return fail(`Unsupported UPI currency "${rawCurrency}". Only INR payments are accepted.`);
  }

  const rawAmountStr = String(getCaseInsensitiveParam(params, "am") ?? "").trim();
  let amount = 0;
  let hasExplicitAmount = false;
  if (rawAmountStr !== "") {
    if (!/^\d+(?:\.\d{1,2})?$/.test(rawAmountStr)) {
      return fail("Scanned QR code contains an invalid payment amount.");
    }
    const parsedAmt = Number(rawAmountStr);
    if (!Number.isFinite(parsedAmt) || parsedAmt < 0 || parsedAmt > MAX_UPI_SCAN_AMOUNT_INR) {
      return fail("Scanned QR code payment amount is out of allowed bounds.");
    }
    amount = Math.round(parsedAmt * 100) / 100;
    hasExplicitAmount = amount > 0;
  }

  const note = String(getCaseInsensitiveParam(params, "tn") || "")
    .replace(/[\r\n\t]+/g, " ")
    .trim()
    .slice(0, 120);

  const transactionRef = String(getCaseInsensitiveParam(params, "tr") || "")
    .replace(/[\r\n\t]+/g, " ")
    .trim()
    .slice(0, 80);

  return {
    valid: true,
    upiId,
    payeeName,
    amount,
    hasExplicitAmount,
    currency: "INR",
    note,
    transactionRef,
    rawPayload,
    isOfficialStudioUpi
  };
}

/**
 * Builds a strictly sanitized `upi://pay` deep link for opening installed UPI apps.
 */
export function buildSafeUpiPayLink({
  upiId = OFFICIAL_STUDIO_UPI_ID,
  payeeName = "STAR ARTIST STUDIO",
  amount = 0,
  note = "",
  transactionRef = ""
} = {}) {
  const cleanUpi = String(upiId || "").trim().toLowerCase();
  if (!isValidUpiId(cleanUpi)) return "";

  const params = new URLSearchParams();
  params.set("pa", cleanUpi);
  params.set("pn", String(payeeName || "STAR ARTIST STUDIO").trim().slice(0, 120) || "STAR ARTIST STUDIO");
  const numAmount = Number(amount);
  if (Number.isFinite(numAmount) && numAmount > 0 && numAmount <= MAX_UPI_SCAN_AMOUNT_INR) {
    const rounded = Math.round(numAmount * 100) / 100;
    params.set("am", Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(2));
  }
  params.set("cu", "INR");
  const cleanNote = String(note || "").replace(/[\r\n\t]+/g, " ").trim().slice(0, 120);
  if (cleanNote) params.set("tn", cleanNote);
  const cleanTr = String(transactionRef || "").replace(/[\r\n\t]+/g, " ").trim().slice(0, 80);
  if (cleanTr) params.set("tr", cleanTr);

  return `upi://pay?${params.toString()}`;
}

/**
 * Finds the best matching active payable booking payment record for the customer
 * and builds the complete pre-filled UPI payment details object.
 */
export function buildPrefilledUpiPaymentDetails(
  parsedQr,
  { customerPayments = [], fallbackConfig = DEFAULT_PAYMENT_CONFIG } = {}
) {
  if (!parsedQr?.valid) return null;

  const activePayable = (Array.isArray(customerPayments) ? customerPayments : []).filter(
    (p) =>
      p &&
      p.priceSetByAdmin === true &&
      Number(p.totalPrice || 0) > 0 &&
      ACTIVE_PAYABLE_STATUSES.has(String(p.paymentStatus || ""))
  );

  let matchedPayment = null;
  if (parsedQr.hasExplicitAmount && activePayable.length > 0) {
    matchedPayment =
      activePayable.find((p) => {
        const calc = calculateAdvanceAndBalance({
          totalPrice: p.totalPrice,
          mode: p.chargeMode || "percentage",
          value: p.chargeValue || 0,
          paidAmount: p.paidAmount
        });
        const targets = [
          Math.round(Number(p.requiredAdvance || 0)),
          Math.round(Number(p.remainingBalance || 0)),
          Math.round(Number(calc.payableNow || 0))
        ];
        return targets.includes(Math.round(parsedQr.amount));
      }) || null;
  }
  if (!matchedPayment && activePayable.length === 1) {
    matchedPayment = activePayable[0];
  }

  let effectiveAmount = parsedQr.hasExplicitAmount ? parsedQr.amount : 0;
  let amountSource = parsedQr.hasExplicitAmount ? "qr" : "manual";

  if (!parsedQr.hasExplicitAmount && matchedPayment) {
    const calc = calculateAdvanceAndBalance({
      totalPrice: matchedPayment.totalPrice,
      mode: matchedPayment.chargeMode || "percentage",
      value: matchedPayment.chargeValue || 0,
      paidAmount: matchedPayment.paidAmount
    });
    const fallbackAmt = calc.payableNow > 0 ? calc.payableNow : Number(matchedPayment.requiredAdvance || matchedPayment.remainingBalance || 0);
    if (fallbackAmt > 0) {
      effectiveAmount = Math.round(fallbackAmt);
      amountSource = "booking";
    }
  }

  const matchedLabel = matchedPayment
    ? matchedPayment.paymentCategory === "wedding_booking"
      ? `${matchedPayment.eventType || matchedPayment.serviceType || "Wedding Booking"} (${matchedPayment.shootDate || "—"})`
      : `${matchedPayment.modelName || "Model"} — ${matchedPayment.serviceType || "Booking"} (${matchedPayment.shootDate || "—"})`
    : "";

  const upiId = parsedQr.upiId || String(fallbackConfig?.upiId || OFFICIAL_STUDIO_UPI_ID).trim().toLowerCase();
  const payeeName = parsedQr.payeeName || String(fallbackConfig?.payeeName || "STAR ARTIST STUDIO").trim();
  const note =
    parsedQr.note ||
    (matchedLabel ? `Star Artist Studio - ${matchedLabel}`.slice(0, 120) : "Star Artist Studio UPI Payment");
  const transactionRef = parsedQr.transactionRef || "";

  return {
    upiId,
    payeeName,
    amount: effectiveAmount,
    amountSource,
    currency: "INR",
    note,
    transactionRef,
    isOfficialStudioUpi: Boolean(parsedQr.isOfficialStudioUpi),
    matchedPaymentId: matchedPayment?.id ? String(matchedPayment.id) : "",
    matchedPaymentLabel: matchedLabel,
    upiDeepLink: buildSafeUpiPayLink({
      upiId,
      payeeName,
      amount: effectiveAmount,
      note,
      transactionRef
    }),
    scannedAtIso: new Date().toISOString()
  };
}

/**
 * Decodes an uploaded/captured QR image file fallback into verified UPI payment details.
 */
export async function decodeQrImageFileForPayment(file, options = {}) {
  const verifyRes = await verifyQrCode(file, options);
  if (!verifyRes.valid) {
    return {
      valid: false,
      error: verifyRes.error || "Could not decode a valid official Studio UPI QR code."
    };
  }
  return parseUpiPaymentQrDetails(verifyRes.rawPayload, {
    expectedUpiId: options.expectedUpiId || OFFICIAL_STUDIO_UPI_ID,
    requireOfficialUpi: options.requireOfficialUpi !== false
  });
}

/**
 * Renders the pre-filled UPI Payment Details card inside My Portal after a QR code is scanned.
 */
export function renderScannedUpiPrefillPanelHtml(
  prefill,
  { t = (k) => k, escapeHtml = defaultEscapeHtml } = {}
) {
  if (!prefill || !prefill.upiId) return "";

  const amountVal = Number(prefill.amount || 0);
  const amountDisplay = amountVal > 0 ? formatInrAmount(amountVal) : t("portalScanQrEnterAmountHint");
  const amountSourceBadge =
    prefill.amountSource === "qr"
      ? t("portalScanQrAmountFromQr")
      : prefill.amountSource === "booking"
      ? t("portalScanQrAmountFromBooking")
      : t("portalScanQrAmountManual");
  const safeDeepLink = buildSafeUpiPayLink({
    upiId: prefill.upiId,
    payeeName: prefill.payeeName,
    amount: amountVal,
    note: prefill.note,
    transactionRef: prefill.transactionRef
  });

  return `
    <section
      id="portalScannedUpiCard"
      class="studio-card portal-scanned-upi-card"
      data-scanned-upi-prefill="true"
      aria-labelledby="portalScannedUpiHeading"
    >
      <div class="portal-scanned-upi-head">
        <div>
          <span class="portal-scanned-verified-pill">✓ ${escapeHtml(t("portalScanQrVerifiedBadge"))}</span>
          <h4 id="portalScannedUpiHeading" style="margin:6px 0 2px;color:var(--gold-light);">${escapeHtml(t("portalScanQrPrefillTitle"))}</h4>
          <p style="margin:0;font-size:0.84rem;color:var(--text-secondary);">${escapeHtml(t("portalScanQrPrefillSub"))}</p>
        </div>
        <button type="button" class="icon-btn" data-clear-scanned-upi="true" aria-label="${escapeHtml(t("portalScanQrClearBtn"))}" title="${escapeHtml(t("portalScanQrClearBtn"))}">✕</button>
      </div>

      <div class="portal-scanned-upi-grid">
        <div class="portal-scanned-field">
          <label for="portalScannedUpiId">${escapeHtml(t("portalScanQrPayeeUpiLabel"))}</label>
          <div class="portal-scanned-upi-copy-row">
            <input id="portalScannedUpiId" type="text" class="input" readonly value="${escapeHtml(prefill.upiId)}" />
            <button type="button" class="btn-outline btn-sm" data-copy-upi="${escapeHtml(prefill.upiId)}">${escapeHtml(t("payCopyUpiBtn"))}</button>
          </div>
        </div>
        <div class="portal-scanned-field">
          <label for="portalScannedPayeeName">${escapeHtml(t("portalScanQrPayeeNameLabel"))}</label>
          <input id="portalScannedPayeeName" type="text" class="input" readonly value="${escapeHtml(prefill.payeeName)}" />
        </div>
        <div class="portal-scanned-field">
          <label for="portalScannedPayAmount">
            ${escapeHtml(t("portalScanQrAmountLabel"))}
            <span class="portal-scanned-source-tag">${escapeHtml(amountSourceBadge)}</span>
          </label>
          <input
            id="portalScannedPayAmount"
            type="number"
            class="input"
            min="1"
            max="${MAX_UPI_SCAN_AMOUNT_INR}"
            step="0.01"
            placeholder="0"
            value="${amountVal > 0 ? escapeHtml(amountVal) : ""}"
          />
        </div>
        <div class="portal-scanned-field">
          <label for="portalScannedPayNote">${escapeHtml(t("portalScanQrNoteLabel"))}</label>
          <input
            id="portalScannedPayNote"
            type="text"
            class="input"
            maxlength="120"
            value="${escapeHtml(prefill.note || "")}"
            placeholder="${escapeHtml(t("portalScanQrNotePlaceholder"))}"
          />
        </div>
      </div>

      ${
        prefill.matchedPaymentLabel
          ? `<div class="portal-scanned-booking-match">
              <span>📌 ${escapeHtml(t("portalScanQrMatchedBooking"))}: <strong>${escapeHtml(prefill.matchedPaymentLabel)}</strong></span>
              <button type="button" class="btn-outline btn-sm" data-jump-utr-payment-id="${escapeHtml(prefill.matchedPaymentId)}">${escapeHtml(t("portalScanQrJumpUtrBtn"))}</button>
            </div>`
          : ""
      }

      <div class="portal-scanned-upi-actions">
        <a
          id="portalScannedUpiPayLink"
          href="${escapeHtml(safeDeepLink)}"
          class="btn-gold portal-scanned-pay-cta"
          data-upi-pay-deep-link="true"
        >
          ⚡ ${escapeHtml(t("portalScanQrOpenUpiAppBtn"))}${amountVal > 0 ? ` (${escapeHtml(amountDisplay)})` : ""}
        </a>
        <button type="button" class="btn-outline btn-sm" data-open-portal-qr-scanner="true">
          📷 ${escapeHtml(t("portalScanQrRescanBtn"))}
        </button>
      </div>
      <p class="upi-security-note" style="margin:0;">🔒 ${escapeHtml(t("portalScanQrAfterPayNote"))}</p>
    </section>
  `;
}

/**
 * Live camera QR scanner controller using MediaDevices + BarcodeDetector / jsQR.
 * Guarantees all MediaStreamTracks are stopped immediately on detection or close.
 */
export function createCameraQrScannerController({
  videoEl = null,
  canvasEl = null,
  onDetected = () => {},
  onError = () => {},
  onStatus = () => {},
  getUserMedia = null,
  barcodeDetector = null,
  customDecoder = null,
  customLoader = null,
  scanIntervalMs = 180
} = {}) {
  let activeStream = null;
  let scanTimer = null;
  let running = false;
  let scanningFrame = false;

  const stopTracks = () => {
    if (activeStream && typeof activeStream.getTracks === "function") {
      for (const track of activeStream.getTracks()) {
        try {
          track.stop();
        } catch {
          // Ignore track stop errors
        }
      }
    }
    activeStream = null;
    if (videoEl) {
      try {
        videoEl.pause?.();
        videoEl.srcObject = null;
      } catch {
        // Ignore video cleanup errors
      }
    }
  };

  const stop = () => {
    running = false;
    scanningFrame = false;
    if (scanTimer !== null) {
      clearTimeout(scanTimer);
      scanTimer = null;
    }
    stopTracks();
  };

  const scanSingleFrame = async ({ decoder, detector }) => {
    if (!running || !videoEl) return "";

    if (detector && typeof detector.detect === "function") {
      try {
        const found = await detector.detect(videoEl);
        const val = String(found?.[0]?.rawValue || "").trim();
        if (val) return val;
      } catch {
        // Fall through to canvas + jsQR decoding
      }
    }

    if (!decoder || !canvasEl) return "";
    const vw = Number(videoEl.videoWidth || videoEl.width || 0);
    const vh = Number(videoEl.videoHeight || videoEl.height || 0);
    if (vw <= 0 || vh <= 0) return "";

    const scale = Math.min(1, 720 / Math.max(vw, vh));
    canvasEl.width = Math.max(1, Math.round(vw * scale));
    canvasEl.height = Math.max(1, Math.round(vh * scale));
    const ctx2d = canvasEl.getContext?.("2d", { willReadFrequently: true });
    if (!ctx2d) return "";
    ctx2d.drawImage(videoEl, 0, 0, canvasEl.width, canvasEl.height);
    const imageData = ctx2d.getImageData(0, 0, canvasEl.width, canvasEl.height);
    return decodeQrFromImageData(imageData, { customDecoder: decoder });
  };

  const start = async () => {
    stop();
    const requestMedia =
      typeof getUserMedia === "function"
        ? getUserMedia
        : typeof navigator !== "undefined" && navigator.mediaDevices?.getUserMedia
        ? (constraints) => navigator.mediaDevices.getUserMedia(constraints)
        : null;

    if (!requestMedia) {
      onError("Camera access is unavailable on this browser or device. Use the 'Upload QR Image' option below instead.");
      return false;
    }

    running = true;
    onStatus("Starting camera...");

    try {
      activeStream = await requestMedia({
        video: { facingMode: { ideal: "environment" } },
        audio: false
      });
    } catch (err) {
      running = false;
      const errName = String(err?.name || "");
      const friendly =
        errName === "NotAllowedError" || errName === "PermissionDeniedError"
          ? "Camera permission was denied. Allow camera access in your browser settings or upload a QR image below."
          : errName === "NotFoundError" || errName === "DevicesNotFoundError"
          ? "No camera was found on this device. You can upload a QR image below instead."
          : err?.message || "Unable to open the camera. You can upload a QR image below instead.";
      onError(friendly);
      return false;
    }

    if (!running) {
      stopTracks();
      return false;
    }

    if (videoEl) {
      videoEl.srcObject = activeStream;
      videoEl.setAttribute?.("playsinline", "true");
      videoEl.muted = true;
      try {
        await videoEl.play?.();
      } catch {
        // Some browsers resolve play asynchronously after metadata loads
      }
    }

    const detector =
      barcodeDetector ||
      (typeof globalThis !== "undefined" && typeof globalThis.BarcodeDetector === "function"
        ? new globalThis.BarcodeDetector({ formats: ["qr_code"] })
        : null);

    let decoder = typeof customDecoder === "function" ? customDecoder : null;
    if (!decoder && !detector) {
      try {
        decoder = await loadQrDecoderRuntime({ customLoader });
      } catch {
        // Will fail gracefully below if neither decoder nor detector is ready
      }
    } else if (!decoder) {
      // Preload jsQR lazily in background as backup for BarcodeDetector
      loadQrDecoderRuntime({ customLoader })
        .then((fn) => {
          decoder = fn;
        })
        .catch(() => {});
    }

    if (!detector && !decoder) {
      stop();
      onError("QR scanner engine could not load. Check your connection or upload a QR image below.");
      return false;
    }

    onStatus("Point your camera at the STAR ARTIST STUDIO UPI QR code...");

    const tick = async () => {
      if (!running) return;
      if (!scanningFrame) {
        scanningFrame = true;
        try {
          const decoded = await scanSingleFrame({ decoder, detector });
          if (decoded && running) {
            stop();
            onDetected(decoded);
            return;
          }
        } catch {
          // Keep scanning next frame
        } finally {
          scanningFrame = false;
        }
      }
      if (running) {
        scanTimer = setTimeout(tick, Math.max(60, Number(scanIntervalMs) || 180));
      }
    };

    scanTimer = setTimeout(tick, 120);
    return true;
  };

  return {
    start,
    stop,
    isRunning: () => running
  };
}
