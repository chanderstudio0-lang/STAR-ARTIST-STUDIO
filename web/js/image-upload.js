import { readExifFromFile, sanitizeExifMetadata } from "./exif-reader.js";

export const FIRESTORE_MEDIA_URL_MAX = 850000;
export const FIRESTORE_THUMB_URL_MAX = 200000;
export const MAX_MEDIA_DATA_URL_CHARS = 800000;
export const MAX_THUMB_DATA_URL_CHARS = 180000;
export const MAX_UPLOAD_FILE_BYTES = 15 * 1024 * 1024;

export const ALLOWED_IMAGE_MIME_TYPES = new Set([
  "image/jpeg",
  "image/jpg",
  "image/png",
  "image/webp",
  "image/avif",
  "image/gif"
]);

const IMAGE_EXT_REGEX = /\.(jpe?g|png|webp|avif|gif)$/i;

export function validateGalleryImageFile(file) {
  if (!file) {
    return { ok: false, error: "Please select an image file from your device." };
  }
  const mime = String(file.type || "").toLowerCase().trim();
  const name = String(file.name || "").trim();
  const mimeOk = mime ? mime.startsWith("image/") && (ALLOWED_IMAGE_MIME_TYPES.has(mime) || mime === "image/heic" || mime === "image/heif") : IMAGE_EXT_REGEX.test(name);
  if (!mimeOk) {
    return { ok: false, error: "Unsupported file format. Please choose a JPG, PNG, or WebP photo." };
  }
  const size = Number(file.size || 0);
  if (!Number.isFinite(size) || size <= 0) {
    return { ok: false, error: "Selected image file appears to be empty." };
  }
  if (size > MAX_UPLOAD_FILE_BYTES) {
    return { ok: false, error: "Image is larger than 15 MB. Please select a smaller photo." };
  }
  return { ok: true, error: "" };
}

export function computeScaledDimensions(width, height, maxDimension = 1280) {
  const safeW = Math.max(1, Math.round(Number(width) || 1));
  const safeH = Math.max(1, Math.round(Number(height) || 1));
  const limit = Math.max(64, Math.round(Number(maxDimension) || 1280));
  const longest = Math.max(safeW, safeH);
  if (longest <= limit) {
    return { width: safeW, height: safeH };
  }
  const scale = limit / longest;
  return {
    width: Math.max(1, Math.round(safeW * scale)),
    height: Math.max(1, Math.round(safeH * scale))
  };
}

export function suggestTitleFromFileName(fileName) {
  const clean = String(fileName || "")
    .replace(/\.[^.]+$/, "")
    .replace(/[_-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
  if (!clean) return "";
  return clean.slice(0, 120);
}

export function validateGallerySubmission({ title, mediaUrl, thumbnailUrl = "" }) {
  const cleanTitle = String(title || "").trim();
  if (!cleanTitle || cleanTitle.length > 120) {
    return "Please enter an artwork title (1–120 characters).";
  }

  const cleanMedia = String(mediaUrl || "").trim();
  if (!cleanMedia) {
    return "Please upload a photo from your device or paste an image/video URL.";
  }

  const validSource =
    /^https?:\/\//i.test(cleanMedia) || /^data:image\/[a-z0-9.+-]+;base64,/i.test(cleanMedia);
  if (!validSource) {
    return "Media source must be a valid uploaded photo or an https:// URL.";
  }

  if (cleanMedia.length > FIRESTORE_MEDIA_URL_MAX) {
    return "Uploaded image exceeds the free cloud size limit. Please try a smaller photo.";
  }

  const cleanThumb = String(thumbnailUrl || "").trim();
  if (cleanThumb.length > FIRESTORE_THUMB_URL_MAX) {
    return "Generated thumbnail exceeds the free cloud size limit.";
  }

  return null;
}

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ""));
    reader.onerror = () => reject(new Error("Could not read the selected image file."));
    reader.readAsDataURL(file);
  });
}

function loadImageElement(src) {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error("Could not decode the selected photo. Try JPG, PNG, or WebP."));
    img.src = src;
  });
}

function renderCompressedJpeg(img, maxDim, maxChars, startQuality = 0.84, minQuality = 0.52) {
  let targetDim = maxDim;
  for (let pass = 0; pass < 4; pass++) {
    const dims = computeScaledDimensions(img.naturalWidth || img.width, img.naturalHeight || img.height, targetDim);
    const canvas = document.createElement("canvas");
    canvas.width = dims.width;
    canvas.height = dims.height;
    const ctx = canvas.getContext("2d");
    if (!ctx) throw new Error("Browser image canvas is unavailable.");
    ctx.fillStyle = "#0C0C0C";
    ctx.fillRect(0, 0, dims.width, dims.height);
    ctx.drawImage(img, 0, 0, dims.width, dims.height);

    let quality = startQuality;
    while (quality >= minQuality - 0.001) {
      const dataUrl = canvas.toDataURL("image/jpeg", Number(quality.toFixed(2)));
      if (dataUrl.length <= maxChars) {
        return { dataUrl, width: dims.width, height: dims.height };
      }
      quality -= 0.08;
    }
    targetDim = Math.max(240, Math.round(targetDim * 0.78));
  }
  throw new Error("Photo is too detailed for free-tier cloud sync. Please pick a smaller image.");
}

/**
 * Compresses a user-selected device image client-side into Firestore-safe JPEG Data URLs
 * (mediaUrl <= 800KB, thumbnailUrl <= 180KB) so uploads stay 100% free on the Firebase Spark plan.
 */
export async function encodeImageFileToStudioPayloads(
  file,
  { maxMainDim = 1280, maxThumbDim = 480 } = {}
) {
  const check = validateGalleryImageFile(file);
  if (!check.ok) {
    throw new Error(check.error);
  }

  const extractedExif = await readExifFromFile(file);
  const metadata = sanitizeExifMetadata(extractedExif);

  const rawDataUrl = await readFileAsDataUrl(file);
  const img = await loadImageElement(rawDataUrl);

  const main = renderCompressedJpeg(img, maxMainDim, MAX_MEDIA_DATA_URL_CHARS, 0.84, 0.52);
  const thumb = renderCompressedJpeg(img, maxThumbDim, MAX_THUMB_DATA_URL_CHARS, 0.74, 0.48);

  const approxKb = Math.max(1, Math.round((main.dataUrl.length * 0.75) / 1024));
  return {
    file,
    mediaUrl: main.dataUrl,
    thumbnailUrl: thumb.dataUrl,
    width: main.width,
    height: main.height,
    originalName: String(file.name || "studio-photo.jpg"),
    approxKb,
    metadata
  };
}

export function formatUploadBytes(bytes) {
  const num = Number(bytes);
  if (!Number.isFinite(num) || num <= 0) return "0 B";
  if (num < 1024) return `${Math.round(num)} B`;
  const kb = num / 1024;
  if (kb < 1024) return `${kb >= 100 ? Math.round(kb) : Number(kb.toFixed(1))} KB`;
  const mb = kb / 1024;
  return `${Number(mb.toFixed(2))} MB`;
}

export function computeUploadProgress(bytesTransferred, totalBytes) {
  const transferred = Number(bytesTransferred);
  const total = Number(totalBytes);
  if (!Number.isFinite(transferred) || !Number.isFinite(total) || total <= 0) {
    return 0;
  }
  const pct = Math.round((transferred / total) * 100);
  return Math.max(0, Math.min(100, pct));
}

export function buildStorageObjectPath(uid, originalName = "studio-photo.jpg", timestamp = Date.now()) {
  const safeUid = String(uid || "member").replace(/[^a-zA-Z0-9_-]/g, "").slice(0, 64) || "member";
  const baseName = String(originalName || "studio-photo.jpg")
    .replaceAll("\\", "/")
    .split("/")
    .pop()
    .replace(/[^a-zA-Z0-9._-]/g, "-")
    .replace(/-+/g, "-")
    .replace(/^\.+/, "")
    .slice(0, 80) || "studio-photo.jpg";
  const safeTs = Number.isFinite(Number(timestamp)) ? Math.floor(Number(timestamp)) : Date.now();
  return `gallery_uploads/${safeUid}/${safeTs}-${baseName}`;
}

/**
 * Controls the real-time upload progress bar embedded inside #galleryUploadDropzone.
 */
export function createUploadProgressController({
  dropzoneEl = null,
  wrapEl = null,
  barEl = null,
  fillEl = null,
  labelEl = null,
  pctEl = null,
  bytesEl = null
} = {}) {
  let currentState = {
    visible: false,
    percent: 0,
    bytesTransferred: 0,
    totalBytes: 0,
    label: "",
    status: "idle"
  };

  function applyDom() {
    if (wrapEl?.classList) {
      wrapEl.classList.toggle("hidden", !currentState.visible);
      wrapEl.classList.toggle("is-uploading", currentState.status === "uploading");
      wrapEl.classList.toggle("is-complete", currentState.status === "complete");
      wrapEl.classList.toggle("is-error", currentState.status === "error");
    }
    if (dropzoneEl?.classList) {
      dropzoneEl.classList.toggle("is-uploading", currentState.status === "uploading");
    }
    if (fillEl?.style) {
      fillEl.style.width = `${currentState.percent}%`;
    }
    if (barEl && typeof barEl.setAttribute === "function") {
      barEl.setAttribute("aria-valuenow", String(currentState.percent));
      barEl.setAttribute(
        "aria-valuetext",
        currentState.label ? `${currentState.label} ${currentState.percent}%` : `${currentState.percent}%`
      );
    }
    if (labelEl) {
      labelEl.textContent = currentState.label;
    }
    if (pctEl) {
      pctEl.textContent = `${currentState.percent}%`;
    }
    if (bytesEl) {
      bytesEl.textContent =
        currentState.totalBytes > 0
          ? `${formatUploadBytes(currentState.bytesTransferred)} / ${formatUploadBytes(currentState.totalBytes)}`
          : "";
    }
  }

  function update({
    percent = currentState.percent,
    bytesTransferred = currentState.bytesTransferred,
    totalBytes = currentState.totalBytes,
    label = currentState.label,
    status = "uploading"
  } = {}) {
    const safePct = Math.max(0, Math.min(100, Math.round(Number(percent) || 0)));
    currentState = {
      visible: true,
      percent: safePct,
      bytesTransferred: Math.max(0, Number(bytesTransferred) || 0),
      totalBytes: Math.max(0, Number(totalBytes) || 0),
      label: String(label || ""),
      status
    };
    applyDom();
    return { ...currentState };
  }

  function complete(label = "Upload complete!") {
    return update({
      percent: 100,
      bytesTransferred: currentState.totalBytes || currentState.bytesTransferred,
      totalBytes: currentState.totalBytes,
      label,
      status: "complete"
    });
  }

  function error(label = "Upload failed.") {
    return update({
      percent: currentState.percent,
      bytesTransferred: currentState.bytesTransferred,
      totalBytes: currentState.totalBytes,
      label,
      status: "error"
    });
  }

  function reset() {
    currentState = {
      visible: false,
      percent: 0,
      bytesTransferred: 0,
      totalBytes: 0,
      label: "",
      status: "idle"
    };
    applyDom();
    return { ...currentState };
  }

  function getState() {
    return { ...currentState };
  }

  return { update, complete, error, reset, getState };
}

