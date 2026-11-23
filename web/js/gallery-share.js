/**
 * Filter-bar category count helpers and Web Share API lightbox sharing for STAR ARTIST STUDIO.
 */
import { buildZipArchive } from "./zip-builder.js";

export function countCatalogByCategory(services = [], searchQuery = "") {
  const q = String(searchQuery || "").trim().toLowerCase();
  const matching = services.filter((pkg) => {
    if (!q) return true;
    const text = `${pkg.titleEn || ""} ${pkg.titleHi || ""} ${pkg.summaryEn || ""} ${pkg.tagEn || ""} ${pkg.recommendedServiceType || ""}`.toLowerCase();
    return text.includes(q);
  });

  const counts = {
    all: matching.length,
    photography: 0,
    modeling: 0,
    production: 0
  };

  for (const pkg of matching) {
    const key = String(pkg.category || "").toLowerCase();
    if (Object.prototype.hasOwnProperty.call(counts, key)) {
      counts[key] += 1;
    }
  }
  return counts;
}

export function countGalleryByCategory(items = [], categories = []) {
  const counts = {};
  for (const cat of categories) {
    counts[cat] = 0;
  }
  counts.All = items.length;
  for (const item of items) {
    const cat = String(item?.category || "").trim();
    if (!cat) continue;
    counts[cat] = (counts[cat] || 0) + 1;
  }
  return counts;
}

export function buildGallerySharePayload(
  item,
  baseUrl = "https://gen-lang-client-0548683163.web.app/"
) {
  const title = String(item?.title || "Studio Showcase").trim();
  const category = String(item?.category || "Portfolio").trim();
  const rawImage = String(item?.mediaUrl || item?.thumbnailUrl || "").trim();

  let imageUrl = "";
  if (rawImage) {
    try {
      imageUrl = new URL(rawImage, baseUrl).href;
    } catch {
      imageUrl = rawImage;
    }
  }

  let pageUrl = baseUrl;
  try {
    const parsed = new URL(baseUrl);
    parsed.hash = "gallery";
    pageUrl = parsed.href;
  } catch {
    pageUrl = imageUrl || "https://gen-lang-client-0548683163.web.app/#gallery";
  }

  const slug = title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "") || "star-artist-studio";

  return {
    title: `${title} — STAR ARTIST STUDIO`,
    text: `Check out "${title}" (${category}) from STAR ARTIST STUDIO, Sirsa, Haryana.`,
    url: imageUrl || pageUrl,
    pageUrl,
    imageUrl,
    fileName: `${slug}.jpg`
  };
}

/**
 * Shares a gallery studio image via the Web Share API (with image File attachment when supported,
 * otherwise native URL/text share), falling back gracefully to clipboard copy on desktop browsers.
 */
export async function shareGalleryImage(
  item,
  {
    nav = typeof navigator !== "undefined" ? navigator : null,
    fetchImpl = typeof fetch === "function" ? fetch : null,
    FileCtor = typeof File === "function" ? File : null,
    baseUrl = typeof window !== "undefined" ? window.location.href : "https://gen-lang-client-0548683163.web.app/"
  } = {}
) {
  const payload = buildGallerySharePayload(item, baseUrl);

  if (nav && typeof nav.share === "function") {
    // Attempt Web Share Level 2 image file sharing when supported and image is fetchable
    if (payload.imageUrl && typeof nav.canShare === "function" && fetchImpl && FileCtor) {
      try {
        const res = await fetchImpl(payload.imageUrl);
        if (res && res.ok) {
          const blob = await res.blob();
          const mime = blob.type && blob.type.startsWith("image/") ? blob.type : "image/jpeg";
          const ext = mime.includes("png") ? "png" : mime.includes("webp") ? "webp" : "jpg";
          const fileName = payload.fileName.replace(/\.jpg$/, `.${ext}`);
          const imageFile = new FileCtor([blob], fileName, { type: mime });
          if (nav.canShare({ files: [imageFile] })) {
            await nav.share({
              title: payload.title,
              text: payload.text,
              url: payload.pageUrl,
              files: [imageFile]
            });
            return { status: "shared-file", payload };
          }
        }
      } catch (err) {
        if (err?.name === "AbortError") {
          return { status: "cancelled", payload };
        }
        // Fall through to standard Web Share link/text sharing
      }
    }

    try {
      await nav.share({
        title: payload.title,
        text: payload.text,
        url: payload.url
      });
      return { status: "shared-link", payload };
    } catch (err) {
      if (err?.name === "AbortError") {
        return { status: "cancelled", payload };
      }
      // Fall through to clipboard fallback if Web Share fails
    }
  }

  if (nav?.clipboard && typeof nav.clipboard.writeText === "function") {
    try {
      await nav.clipboard.writeText(`${payload.title}\n${payload.text}\n${payload.url}`);
      return { status: "copied", payload };
    } catch {
      return { status: "unsupported", payload };
    }
  }

  return { status: "unsupported", payload };
}

export const LIGHTBOX_SLIDESHOW_INTERVAL_MS = 4000;

export function getVisibleGalleryItems(items = [], activeFilter = "All") {
  if (!Array.isArray(items)) return [];
  const filter = String(activeFilter || "All").trim();
  if (!filter || filter === "All") return [...items];
  return items.filter((item) => item?.category === filter);
}

export function getNextLightboxIndex(currentIndex, total, delta = 1) {
  const count = Number(total) || 0;
  if (count <= 0) return -1;
  if (count === 1) return 0;
  const cur = Number.isInteger(currentIndex) && currentIndex >= 0 ? currentIndex : 0;
  const step = Number(delta) || 0;
  return (((cur + step) % count) + count) % count;
}

export function formatLightboxSlideCounter(index, total) {
  const count = Number(total) || 0;
  if (count <= 0) return "";
  const safeIndex = Math.max(0, Math.min(count - 1, Number(index) || 0));
  return `${safeIndex + 1} / ${count}`;
}

export function resolveLightboxKeyAction(key) {
  switch (String(key || "")) {
    case "ArrowLeft":
      return "prev";
    case "ArrowRight":
      return "next";
    case " ":
    case "Spacebar":
      return "toggle-play";
    case "Escape":
      return "close";
    case "f":
    case "F":
      return "toggle-fullscreen";
    default:
      return null;
  }
}

export function createLightboxSlideshowController({
  intervalMs = LIGHTBOX_SLIDESHOW_INTERVAL_MS,
  onStep = () => {},
  onStateChange = () => {},
  setIntervalFn = (fn, ms) => setInterval(fn, ms),
  clearIntervalFn = (id) => clearInterval(id)
} = {}) {
  let timerId = null;
  let playing = false;

  function clearTimer() {
    if (timerId !== null) {
      clearIntervalFn(timerId);
      timerId = null;
    }
  }

  function start() {
    clearTimer();
    playing = true;
    timerId = setIntervalFn(() => {
      onStep();
    }, intervalMs);
    onStateChange(true);
    return true;
  }

  function stop() {
    const wasPlaying = playing || timerId !== null;
    clearTimer();
    playing = false;
    if (wasPlaying) {
      onStateChange(false);
    }
    return false;
  }

  function toggle() {
    return playing ? stop() : start();
  }

  function reset() {
    if (!playing) return false;
    clearTimer();
    timerId = setIntervalFn(() => {
      onStep();
    }, intervalMs);
    return true;
  }

  function isPlaying() {
    return playing;
  }

  return { start, stop, toggle, reset, isPlaying };
}

export const LIGHTBOX_MIN_ZOOM = 1;
export const LIGHTBOX_MAX_ZOOM = 4;
export const LIGHTBOX_DOUBLE_TAP_ZOOM = 2.25;
export const LIGHTBOX_ZOOM_STEP = 0.5;

export function clampZoomScale(scale, min = LIGHTBOX_MIN_ZOOM, max = LIGHTBOX_MAX_ZOOM) {
  const num = Number(scale);
  if (!Number.isFinite(num)) return min;
  return Math.max(min, Math.min(max, num));
}

export function getTouchDistance(touchA, touchB) {
  if (!touchA || !touchB) return 0;
  const dx = Number(touchB.clientX || 0) - Number(touchA.clientX || 0);
  const dy = Number(touchB.clientY || 0) - Number(touchA.clientY || 0);
  return Math.hypot(dx, dy);
}

export function getTouchMidpoint(touchA, touchB) {
  if (!touchA || !touchB) return { x: 0, y: 0 };
  return {
    x: (Number(touchA.clientX || 0) + Number(touchB.clientX || 0)) / 2,
    y: (Number(touchA.clientY || 0) + Number(touchB.clientY || 0)) / 2
  };
}

export function clampZoomPan(x, y, scale, containerWidth = 0, containerHeight = 0) {
  const safeScale = clampZoomScale(scale);
  if (safeScale <= 1.02) {
    return { x: 0, y: 0 };
  }
  const w = Math.max(0, Number(containerWidth) || 0);
  const h = Math.max(0, Number(containerHeight) || 0);
  const maxX = w > 0 ? ((safeScale - 1) * w) / 2 : 0;
  const maxY = h > 0 ? ((safeScale - 1) * h) / 2 : 0;
  return {
    x: Math.max(-maxX, Math.min(maxX, Number(x) || 0)),
    y: Math.max(-maxY, Math.min(maxY, Number(y) || 0))
  };
}

export function applyLightboxZoomTransform(imgEl, { scale = 1, x = 0, y = 0 } = {}) {
  const safeScale = clampZoomScale(scale);
  const isZoomed = safeScale > 1.02;
  const tx = isZoomed ? Number(x) || 0 : 0;
  const ty = isZoomed ? Number(y) || 0 : 0;

  if (imgEl && imgEl.style) {
    imgEl.style.transform = isZoomed
      ? `translate3d(${tx.toFixed(1)}px, ${ty.toFixed(1)}px, 0) scale(${safeScale.toFixed(3)})`
      : "translate3d(0px, 0px, 0) scale(1)";
  }
  if (imgEl && typeof imgEl.setAttribute === "function") {
    imgEl.setAttribute("data-zoom-scale", safeScale.toFixed(2));
  }
  if (imgEl?.classList && typeof imgEl.classList.toggle === "function") {
    imgEl.classList.toggle("is-zoomed", isZoomed);
  }
  return { scale: safeScale, x: tx, y: ty, isZoomed };
}

export function createLightboxZoomController({
  minZoom = LIGHTBOX_MIN_ZOOM,
  maxZoom = LIGHTBOX_MAX_ZOOM,
  doubleTapZoom = LIGHTBOX_DOUBLE_TAP_ZOOM,
  zoomStep = LIGHTBOX_ZOOM_STEP,
  onChange = () => {}
} = {}) {
  let state = { scale: minZoom, x: 0, y: 0 };
  let pinchStartDistance = 0;
  let pinchStartScale = minZoom;
  let pinchLastMid = { x: 0, y: 0 };
  let panStartPoint = null;
  let panStartOffset = { x: 0, y: 0 };

  function emit() {
    const snapshot = {
      scale: state.scale,
      x: state.x,
      y: state.y,
      isZoomed: state.scale > 1.02
    };
    onChange(snapshot);
    return snapshot;
  }

  function setZoom(nextScale, nextX = state.x, nextY = state.y, containerWidth = 0, containerHeight = 0) {
    const clampedScale = clampZoomScale(nextScale, minZoom, maxZoom);
    const clampedPan = clampZoomPan(nextX, nextY, clampedScale, containerWidth, containerHeight);
    state = { scale: clampedScale, x: clampedPan.x, y: clampedPan.y };
    return emit();
  }

  function startPinch(touchA, touchB) {
    pinchStartDistance = getTouchDistance(touchA, touchB);
    pinchStartScale = state.scale;
    pinchLastMid = getTouchMidpoint(touchA, touchB);
    panStartPoint = null;
  }

  function movePinch(touchA, touchB, containerWidth = 0, containerHeight = 0) {
    const currentDistance = getTouchDistance(touchA, touchB);
    if (pinchStartDistance <= 0 || currentDistance <= 0) return getState();
    const mid = getTouchMidpoint(touchA, touchB);
    const ratio = currentDistance / pinchStartDistance;
    const targetScale = clampZoomScale(pinchStartScale * ratio, minZoom, maxZoom);
    const dx = mid.x - pinchLastMid.x;
    const dy = mid.y - pinchLastMid.y;
    pinchLastMid = mid;
    return setZoom(targetScale, state.x + dx, state.y + dy, containerWidth, containerHeight);
  }

  function endPinch() {
    pinchStartDistance = 0;
    if (state.scale <= 1.05) {
      return reset();
    }
    return getState();
  }

  function startPan(clientX, clientY) {
    if (state.scale <= 1.02) return false;
    panStartPoint = { x: Number(clientX) || 0, y: Number(clientY) || 0 };
    panStartOffset = { x: state.x, y: state.y };
    return true;
  }

  function movePan(clientX, clientY, containerWidth = 0, containerHeight = 0) {
    if (!panStartPoint || state.scale <= 1.02) return getState();
    const dx = (Number(clientX) || 0) - panStartPoint.x;
    const dy = (Number(clientY) || 0) - panStartPoint.y;
    return setZoom(state.scale, panStartOffset.x + dx, panStartOffset.y + dy, containerWidth, containerHeight);
  }

  function endPan() {
    panStartPoint = null;
  }

  function toggleDoubleTap(containerWidth = 0, containerHeight = 0) {
    if (state.scale > 1.05) {
      return reset();
    }
    return setZoom(doubleTapZoom, 0, 0, containerWidth, containerHeight);
  }

  function zoomIn(containerWidth = 0, containerHeight = 0) {
    return setZoom(state.scale + zoomStep, state.x, state.y, containerWidth, containerHeight);
  }

  function zoomOut(containerWidth = 0, containerHeight = 0) {
    const next = state.scale - zoomStep;
    if (next <= 1.05) return reset();
    return setZoom(next, state.x, state.y, containerWidth, containerHeight);
  }

  function reset() {
    pinchStartDistance = 0;
    panStartPoint = null;
    state = { scale: minZoom, x: 0, y: 0 };
    return emit();
  }

  function getState() {
    return {
      scale: state.scale,
      x: state.x,
      y: state.y,
      isZoomed: state.scale > 1.02
    };
  }

  function isZoomed() {
    return state.scale > 1.02;
  }

  return {
    startPinch,
    movePinch,
    endPinch,
    startPan,
    movePan,
    endPan,
    toggleDoubleTap,
    zoomIn,
    zoomOut,
    setZoom,
    reset,
    getState,
    isZoomed
  };
}

function defaultEscapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function formatCaptureDate(rawDate, lang = "en") {
  if (!rawDate) return "";
  let dateObj = null;

  if (rawDate instanceof Date) {
    dateObj = rawDate;
  } else if (typeof rawDate === "object" && typeof rawDate.toDate === "function") {
    dateObj = rawDate.toDate();
  } else if (typeof rawDate === "object" && Number.isFinite(Number(rawDate.seconds))) {
    dateObj = new Date(Number(rawDate.seconds) * 1000);
  } else if (typeof rawDate === "number" && Number.isFinite(rawDate)) {
    dateObj = new Date(rawDate);
  } else if (typeof rawDate === "string") {
    const clean = rawDate.trim();
    if (!clean) return "";
    if (/^\d{4}-\d{2}-\d{2}$/.test(clean)) {
      const [y, m, d] = clean.split("-").map(Number);
      dateObj = new Date(Date.UTC(y, m - 1, d, 12, 0, 0));
    } else {
      const parsed = new Date(clean);
      if (!Number.isNaN(parsed.getTime())) {
        dateObj = parsed;
      } else {
        return clean;
      }
    }
  }

  if (!dateObj || Number.isNaN(dateObj.getTime())) return "";

  try {
    return new Intl.DateTimeFormat(lang === "hi" ? "hi-IN" : "en-IN", {
      day: "numeric",
      month: "short",
      year: "numeric",
      timeZone: "UTC"
    }).format(dateObj);
  } catch {
    return dateObj.toISOString().slice(0, 10);
  }
}

export function extractLightboxImageMetadata(item, lang = "en") {
  const src = item?.metadata || item?.exif || {};
  const camera = String(src.camera || item?.camera || "").trim();
  const lens = String(src.lens || item?.lens || "").trim();

  let aperture = String(src.aperture || item?.aperture || "").trim();
  if (aperture && !/^f\//i.test(aperture) && /^\d/.test(aperture)) {
    aperture = `f/${aperture}`;
  }

  const shutterSpeed = String(src.shutterSpeed || src.shutter || item?.shutterSpeed || "").trim();

  let iso = String(src.iso ?? item?.iso ?? "").trim();
  if (iso && !/^iso\b/i.test(iso)) {
    iso = `ISO ${iso}`;
  }

  const rawDate = src.dateTaken || item?.dateTaken || item?.createdAt || "";
  const dateTaken = formatCaptureDate(rawDate, lang);

  return {
    camera,
    lens,
    aperture,
    shutterSpeed,
    iso,
    dateTaken
  };
}

export function renderLightboxMetadataHtml(
  item,
  { lang = "en", t = (k) => k, escapeHtml = defaultEscapeHtml, autoExtracted = false, isReadingExif = false } = {}
) {
  const meta = extractLightboxImageMetadata(item, lang);
  const chips = [];

  if (meta.camera || meta.lens) {
    const gearText = [meta.camera, meta.lens].filter(Boolean).join(" • ");
    chips.push({
      field: "camera",
      label: t("metaCameraLabel"),
      value: gearText,
      icon: `<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path><circle cx="12" cy="13" r="4"></circle></svg>`
    });
  }

  if (meta.aperture) {
    chips.push({
      field: "aperture",
      label: t("metaApertureLabel"),
      value: meta.aperture,
      icon: `<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><line x1="14.31" y1="8" x2="20.05" y2="17.94"></line><line x1="9.69" y1="8" x2="21.17" y2="8"></line><line x1="7.38" y1="12" x2="13.12" y2="2.06"></line><line x1="9.69" y1="16" x2="3.95" y2="6.06"></line><line x1="14.31" y1="16" x2="2.83" y2="16"></line><line x1="16.62" y1="12" x2="10.88" y2="21.94"></line></svg>`
    });
  }

  if (meta.shutterSpeed) {
    chips.push({
      field: "shutter",
      label: t("metaShutterLabel"),
      value: meta.shutterSpeed,
      icon: `<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>`
    });
  }

  if (meta.iso) {
    chips.push({
      field: "iso",
      label: t("metaIsoLabel"),
      value: meta.iso,
      icon: `<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line></svg>`
    });
  }

  if (meta.dateTaken) {
    chips.push({
      field: "date",
      label: t("metaDateLabel"),
      value: meta.dateTaken,
      icon: `<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>`
    });
  }

  if (!chips.length) {
    chips.push({
      field: "camera",
      label: t("metaCameraLabel"),
      value: t("metaStudioArchiveValue"),
      icon: `<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path><circle cx="12" cy="13" r="4"></circle></svg>`
    });
  }

  const badgeHtml = isReadingExif
    ? `<span class="lightbox-exif-badge is-reading">${escapeHtml(t("metaExifReading"))}</span>`
    : autoExtracted
      ? `<span class="lightbox-exif-badge is-auto">${escapeHtml(t("metaExifAutoBadge"))}</span>`
      : "";

  return `
    <div class="lightbox-exif-header">
      <span class="lightbox-exif-title">${escapeHtml(t("metaExifHeading"))}</span>
      ${badgeHtml}
    </div>
    <dl class="lightbox-exif-grid">
      ${chips
        .map(
          (chip) => `
        <div class="lightbox-exif-chip" data-exif-field="${escapeHtml(chip.field)}">
          <dt class="lightbox-exif-dt">${chip.icon}<span>${escapeHtml(chip.label)}</span></dt>
          <dd class="lightbox-exif-dd">${escapeHtml(chip.value)}</dd>
        </div>`
        )
        .join("")}
    </dl>`;
}

export const LIGHTBOX_FILTER_PRESETS = [
  {
    id: "original",
    i18nKey: "filterPresetOriginal",
    label: "Original",
    cssFilter: "none"
  },
  {
    id: "grayscale",
    i18nKey: "filterPresetGrayscale",
    label: "Grayscale",
    cssFilter: "grayscale(100%) contrast(1.08)"
  },
  {
    id: "sepia",
    i18nKey: "filterPresetSepia",
    label: "Sepia",
    cssFilter: "sepia(82%) contrast(1.04) brightness(0.98)"
  },
  {
    id: "vivid",
    i18nKey: "filterPresetVivid",
    label: "Vivid",
    cssFilter: "saturate(1.48) contrast(1.12) brightness(1.03)"
  },
  {
    id: "noir",
    i18nKey: "filterPresetNoir",
    label: "Noir",
    cssFilter: "grayscale(100%) contrast(1.38) brightness(0.92)"
  },
  {
    id: "golden",
    i18nKey: "filterPresetGolden",
    label: "Golden",
    cssFilter: "sepia(32%) saturate(1.32) contrast(1.06) brightness(1.04)"
  }
];

export function getLightboxFilterPreset(presetId = "original") {
  const cleanId = String(presetId || "original").trim().toLowerCase();
  return (
    LIGHTBOX_FILTER_PRESETS.find((preset) => preset.id === cleanId) ||
    LIGHTBOX_FILTER_PRESETS[0]
  );
}

export function applyLightboxFilterToElement(imgEl, presetId = "original") {
  const preset = getLightboxFilterPreset(presetId);
  if (imgEl && imgEl.style) {
    imgEl.style.filter = preset.cssFilter;
  }
  if (imgEl && typeof imgEl.setAttribute === "function") {
    imgEl.setAttribute("data-active-filter", preset.id);
  }
  return preset;
}

export function buildFilteredFileName(baseFileName, presetId = "original") {
  const preset = getLightboxFilterPreset(presetId);
  if (preset.id === "original") return baseFileName;
  const dotIdx = baseFileName.lastIndexOf(".");
  if (dotIdx > 0) {
    return `${baseFileName.slice(0, dotIdx)}-${preset.id}${baseFileName.slice(dotIdx)}`;
  }
  return `${baseFileName}-${preset.id}`;
}

function loadBlobAsImage(blob, urlApi, ImageCtor) {
  return new Promise((resolve, reject) => {
    const objectUrl = urlApi.createObjectURL(blob);
    const img = new ImageCtor();
    img.onload = () => {
      if (typeof urlApi.revokeObjectURL === "function") {
        urlApi.revokeObjectURL(objectUrl);
      }
      resolve(img);
    };
    img.onerror = () => {
      if (typeof urlApi.revokeObjectURL === "function") {
        urlApi.revokeObjectURL(objectUrl);
      }
      reject(new Error("Unable to decode image for filtered export."));
    };
    img.src = objectUrl;
  });
}

export async function renderFilteredImageBlob(
  blob,
  presetId,
  {
    doc = typeof document !== "undefined" ? document : null,
    urlApi = typeof URL !== "undefined" ? URL : null,
    ImageCtor = typeof Image === "function" ? Image : null,
    loadImageFn = null
  } = {}
) {
  const preset = getLightboxFilterPreset(presetId);
  if (preset.id === "original" || !doc || typeof doc.createElement !== "function") {
    return null;
  }

  let img;
  if (typeof loadImageFn === "function") {
    img = await loadImageFn(blob);
  } else if (urlApi && typeof urlApi.createObjectURL === "function" && ImageCtor) {
    img = await loadBlobAsImage(blob, urlApi, ImageCtor);
  } else {
    return null;
  }

  const width = Math.max(1, Number(img.naturalWidth || img.width || 0));
  const height = Math.max(1, Number(img.naturalHeight || img.height || 0));
  if (!width || !height) return null;

  const canvas = doc.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const ctx = typeof canvas.getContext === "function" ? canvas.getContext("2d") : null;
  if (!ctx) return null;

  ctx.filter = preset.cssFilter;
  ctx.drawImage(img, 0, 0, width, height);

  const mime =
    blob?.type && (blob.type.includes("png") || blob.type.includes("webp"))
      ? blob.type
      : "image/jpeg";

  if (typeof canvas.toBlob === "function") {
    const outBlob = await new Promise((resolve) => {
      canvas.toBlob((result) => resolve(result), mime, 0.92);
    });
    if (outBlob) return outBlob;
  }
  return null;
}

function inferImageExtension(mime = "", imageUrl = "") {
  const cleanMime = String(mime || "").toLowerCase();
  if (cleanMime.includes("png")) return "png";
  if (cleanMime.includes("webp")) return "webp";
  if (cleanMime.includes("gif")) return "gif";
  if (cleanMime.includes("jpeg") || cleanMime.includes("jpg")) return "jpg";

  const cleanUrl = String(imageUrl || "").split(/[?#]/)[0].toLowerCase();
  if (cleanUrl.endsWith(".png")) return "png";
  if (cleanUrl.endsWith(".webp")) return "webp";
  if (cleanUrl.endsWith(".gif")) return "gif";
  return "jpg";
}

function triggerAnchorDownload(doc, href, fileName, openInNewTab = false) {
  const link = doc.createElement("a");
  link.href = href;
  link.download = fileName;
  link.style.display = "none";
  if (openInNewTab) {
    link.target = "_blank";
    link.rel = "noopener noreferrer";
  }
  doc.body?.appendChild(link);
  link.click();
  if (typeof link.remove === "function") {
    link.remove();
  } else {
    doc.body?.removeChild(link);
  }
}

/**
 * Triggers a direct browser download of a gallery image (preferring a Blob Object URL so the
 * browser saves the file directly with a clean portfolio filename). When a non-original CSS
 * filter preset is active, renders the filtered image on an offscreen canvas before saving.
 */
export async function downloadGalleryImage(
  item,
  {
    doc = typeof document !== "undefined" ? document : null,
    fetchImpl = typeof fetch === "function" ? fetch : null,
    urlApi = typeof URL !== "undefined" ? URL : null,
    ImageCtor = typeof Image === "function" ? Image : null,
    loadImageFn = null,
    filterPresetId = "original",
    baseUrl = typeof window !== "undefined" ? window.location.href : "https://gen-lang-client-0548683163.web.app/"
  } = {}
) {
  const payload = buildGallerySharePayload(item, baseUrl);
  const preset = getLightboxFilterPreset(filterPresetId);
  if (!payload.imageUrl || !doc) {
    return { status: "failed", filterId: preset.id, fileName: payload.fileName, payload };
  }

  if (fetchImpl && urlApi && typeof urlApi.createObjectURL === "function") {
    try {
      const res = await fetchImpl(payload.imageUrl);
      if (res && res.ok) {
        const sourceBlob = await res.blob();
        let finalBlob = sourceBlob;
        let isFiltered = false;

        if (preset.id !== "original") {
          try {
            const styledBlob = await renderFilteredImageBlob(sourceBlob, preset.id, {
              doc,
              urlApi,
              ImageCtor,
              loadImageFn
            });
            if (styledBlob) {
              finalBlob = styledBlob;
              isFiltered = true;
            }
          } catch {
            // Fall back cleanly to saving the original fetched blob
          }
        }

        const ext = inferImageExtension(finalBlob?.type, payload.imageUrl);
        const baseFileName = payload.fileName.replace(/\.jpg$/, `.${ext}`);
        const fileName = isFiltered ? buildFilteredFileName(baseFileName, preset.id) : baseFileName;
        const blobUrl = urlApi.createObjectURL(finalBlob);
        try {
          triggerAnchorDownload(doc, blobUrl, fileName, false);
        } finally {
          if (typeof urlApi.revokeObjectURL === "function") {
            setTimeout(() => urlApi.revokeObjectURL(blobUrl), 1000);
          }
        }
        return {
          status: isFiltered ? "downloaded-filtered" : "downloaded-blob",
          filterId: isFiltered ? preset.id : "original",
          fileName,
          payload
        };
      }
    } catch {
      // Fall back to direct anchor download if cross-origin CORS blocks fetch
    }
  }

  try {
    const ext = inferImageExtension("", payload.imageUrl);
    const fileName = payload.fileName.replace(/\.jpg$/, `.${ext}`);
    triggerAnchorDownload(doc, payload.imageUrl, fileName, true);
    return { status: "downloaded-link", filterId: "original", fileName, payload };
  } catch {
    return { status: "failed", filterId: preset.id, fileName: payload.fileName, payload };
  }
}

export function buildFolderZipFileName(folderName = "All") {
  const slug = String(folderName || "all")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "") || "all";
  return `star-artist-studio-${slug}-gallery.zip`;
}

export function decodeDataUrlToBytes(dataUrl) {
  const str = String(dataUrl || "");
  const match = /^data:([^;,]+)?(;base64)?,(.*)$/i.exec(str);
  if (!match) return null;
  const mime = (match[1] || "application/octet-stream").toLowerCase();
  const isBase64 = Boolean(match[2]);
  const payload = match[3] || "";

  try {
    if (isBase64) {
      if (typeof atob === "function") {
        const binary = atob(payload);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
          bytes[i] = binary.charCodeAt(i);
        }
        return { bytes, mime };
      }
      if (typeof Buffer !== "undefined") {
        const buf = Buffer.from(payload, "base64");
        return { bytes: new Uint8Array(buf.buffer, buf.byteOffset, buf.byteLength), mime };
      }
      return null;
    }
    const decoded = decodeURIComponent(payload);
    const bytes = new Uint8Array(decoded.length);
    for (let i = 0; i < decoded.length; i++) {
      bytes[i] = decoded.charCodeAt(i) & 0xff;
    }
    return { bytes, mime };
  } catch {
    return null;
  }
}

async function fetchImageBytesForZip(imageUrl, fetchImpl) {
  if (String(imageUrl || "").startsWith("data:")) {
    return decodeDataUrlToBytes(imageUrl);
  }
  if (typeof fetchImpl !== "function") return null;

  const res = await fetchImpl(imageUrl);
  if (!res || !res.ok) return null;

  const headerMime = typeof res.headers?.get === "function" ? (res.headers.get("content-type") || "") : "";
  if (typeof res.arrayBuffer === "function") {
    const ab = await res.arrayBuffer();
    return { bytes: new Uint8Array(ab), mime: headerMime };
  }
  if (typeof res.blob === "function") {
    const blob = await res.blob();
    const mime = blob?.type || headerMime;
    if (typeof blob?.arrayBuffer === "function") {
      const ab = await blob.arrayBuffer();
      return { bytes: new Uint8Array(ab), mime };
    }
  }
  return null;
}

/**
 * Packages all images in the currently active gallery folder into a single PKZIP file
 * and triggers a direct browser download.
 */
export async function downloadGalleryFolderAsZip(
  items = [],
  {
    folderName = "All",
    doc = typeof document !== "undefined" ? document : null,
    fetchImpl = typeof fetch === "function" ? fetch : null,
    urlApi = typeof URL !== "undefined" ? URL : null,
    BlobCtor = typeof Blob === "function" ? Blob : null,
    baseUrl = typeof window !== "undefined" ? window.location.href : "https://gen-lang-client-0548683163.web.app/",
    onProgress = null
  } = {}
) {
  const validItems = (Array.isArray(items) ? items : []).filter((item) => Boolean(item?.mediaUrl || item?.thumbnailUrl));
  const fileName = buildFolderZipFileName(folderName);

  if (!validItems.length || !doc || !urlApi || typeof urlApi.createObjectURL !== "function" || !BlobCtor) {
    return { status: "failed", count: 0, total: validItems.length, failedCount: validItems.length, fileName };
  }

  const entries = [];
  let failedCount = 0;

  for (let i = 0; i < validItems.length; i++) {
    const item = validItems[i];
    if (typeof onProgress === "function") {
      onProgress({ current: i + 1, total: validItems.length, item });
    }
    const rawSrc = item.mediaUrl || item.thumbnailUrl || "";
    const payload = buildGallerySharePayload(item, baseUrl);
    const sourceUrl = rawSrc.startsWith("data:") ? rawSrc : payload.imageUrl;

    try {
      const fetched = await fetchImageBytesForZip(sourceUrl, fetchImpl);
      if (!fetched || !fetched.bytes || fetched.bytes.byteLength === 0) {
        failedCount += 1;
        continue;
      }
      const ext = inferImageExtension(fetched.mime, sourceUrl);
      const entryName = payload.fileName.replace(/\.jpg$/i, `.${ext}`);
      entries.push({ name: entryName, data: fetched.bytes });
    } catch {
      failedCount += 1;
    }
  }

  if (!entries.length) {
    return { status: "failed", count: 0, total: validItems.length, failedCount, fileName };
  }

  const zipBytes = buildZipArchive(entries);
  const zipBlob = new BlobCtor([zipBytes], { type: "application/zip" });
  const blobUrl = urlApi.createObjectURL(zipBlob);

  try {
    triggerAnchorDownload(doc, blobUrl, fileName, false);
  } finally {
    if (typeof urlApi.revokeObjectURL === "function") {
      setTimeout(() => urlApi.revokeObjectURL(blobUrl), 1500);
    }
  }

  return {
    status: failedCount > 0 ? "downloaded-partial" : "downloaded-zip",
    count: entries.length,
    total: validItems.length,
    failedCount,
    fileName,
    byteLength: zipBytes.byteLength,
    zipBytes
  };
}

export function getFullscreenElement(doc = globalThis.document) {
  if (!doc) return null;
  return (
    doc.fullscreenElement ||
    doc.webkitFullscreenElement ||
    doc.mozFullScreenElement ||
    doc.msFullscreenElement ||
    null
  );
}

export function isFullscreenApiSupported(targetEl, doc = globalThis.document) {
  if (!targetEl) return false;
  if (doc && doc.fullscreenEnabled === false && doc.webkitFullscreenEnabled !== true) {
    return false;
  }
  return Boolean(
    typeof targetEl.requestFullscreen === "function" ||
      typeof targetEl.webkitRequestFullscreen === "function" ||
      typeof targetEl.mozRequestFullScreen === "function" ||
      typeof targetEl.msRequestFullscreen === "function"
  );
}

export function createLightboxFullscreenController({
  getTargetEl = () => null,
  doc = globalThis.document,
  onStateChange = () => {}
} = {}) {
  let pseudoFullscreen = false;
  let lastNotifiedActive = false;

  function isNativeFullscreen() {
    const target = getTargetEl?.();
    const activeEl = getFullscreenElement(doc);
    if (!target || !activeEl) return false;
    return activeEl === target || (typeof activeEl.contains === "function" && activeEl.contains(target));
  }

  function isFullscreen() {
    return isNativeFullscreen() || pseudoFullscreen;
  }

  function notify(force = false) {
    const nativeActive = isNativeFullscreen();
    if (nativeActive && pseudoFullscreen) {
      pseudoFullscreen = false;
    }
    const active = nativeActive || pseudoFullscreen;
    const mode = nativeActive ? "native" : pseudoFullscreen ? "fallback" : "none";
    if (force || active !== lastNotifiedActive) {
      lastNotifiedActive = active;
      onStateChange({ isFullscreen: active, isNative: nativeActive, mode });
    }
    return { active, isNative: nativeActive, mode };
  }

  async function enter() {
    const target = getTargetEl?.();
    if (!target) {
      return { active: false, isNative: false, mode: "unavailable" };
    }
    if (isNativeFullscreen()) {
      return notify(true);
    }

    const requestMethod =
      target.requestFullscreen ||
      target.webkitRequestFullscreen ||
      target.mozRequestFullScreen ||
      target.msRequestFullscreen;

    if (typeof requestMethod === "function" && isFullscreenApiSupported(target, doc)) {
      try {
        const result = requestMethod.call(target);
        if (result && typeof result.then === "function") {
          await result;
        }
        pseudoFullscreen = false;
        const activeAfterRequest = isNativeFullscreen();
        if (activeAfterRequest) {
          return notify(true);
        }
        lastNotifiedActive = true;
        onStateChange({ isFullscreen: true, isNative: true, mode: "native" });
        return { active: true, isNative: true, mode: "native" };
      } catch {
        // Fall through to CSS viewport fullscreen fallback if native Fullscreen API rejects
      }
    }

    pseudoFullscreen = true;
    return notify(true);
  }

  async function exit() {
    if (isNativeFullscreen() && doc) {
      const exitMethod =
        doc.exitFullscreen ||
        doc.webkitExitFullscreen ||
        doc.mozCancelFullScreen ||
        doc.msExitFullscreen;
      if (typeof exitMethod === "function") {
        try {
          const result = exitMethod.call(doc);
          if (result && typeof result.then === "function") {
            await result;
          }
        } catch {
          // Ensure local state still resets cleanly
        }
      }
    }

    pseudoFullscreen = false;
    return notify(true);
  }

  async function toggle() {
    if (isFullscreen()) {
      return exit();
    }
    return enter();
  }

  function syncFromDocument() {
    return notify(false);
  }

  return {
    enter,
    exit,
    toggle,
    isFullscreen,
    isNativeFullscreen,
    syncFromDocument
  };
}



