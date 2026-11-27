import {
  MAX_MEDIA_DATA_URL_CHARS,
  MAX_THUMB_DATA_URL_CHARS,
  computeScaledDimensions
} from "./image-upload.js";

export const MIN_BRIGHTNESS = 50;
export const MAX_BRIGHTNESS = 150;
export const DEFAULT_BRIGHTNESS = 100;

export const MIN_CROP_ZOOM = 1;
export const MAX_CROP_ZOOM = 3;
export const DEFAULT_CROP_ZOOM = 1;

export const CROP_ASPECT_PRESETS = Object.freeze(["original", "1:1", "4:3", "4:5", "16:9", "3:4", "9:16"]);

export function clampBrightness(value) {
  const num = Number(value);
  if (!Number.isFinite(num)) return DEFAULT_BRIGHTNESS;
  return Math.max(MIN_BRIGHTNESS, Math.min(MAX_BRIGHTNESS, Math.round(num)));
}

export function clampCropZoom(value) {
  const num = Number(value);
  if (!Number.isFinite(num)) return DEFAULT_CROP_ZOOM;
  const clamped = Math.max(MIN_CROP_ZOOM, Math.min(MAX_CROP_ZOOM, num));
  return Number(clamped.toFixed(2));
}

export function clampOffset(value) {
  const num = Number(value);
  if (!Number.isFinite(num)) return 0;
  const clamped = Math.max(-1, Math.min(1, num));
  return Number(clamped.toFixed(4));
}

export function normalizeAspectPreset(aspect) {
  const clean = String(aspect || "original").trim();
  return CROP_ASPECT_PRESETS.includes(clean) ? clean : "original";
}

export function createDefaultEditorState() {
  return {
    aspectRatio: "original",
    zoom: DEFAULT_CROP_ZOOM,
    offsetX: 0,
    offsetY: 0,
    brightness: DEFAULT_BRIGHTNESS
  };
}

export function normalizeEditorState(raw = {}) {
  return {
    aspectRatio: normalizeAspectPreset(raw.aspectRatio),
    zoom: clampCropZoom(raw.zoom),
    offsetX: clampOffset(raw.offsetX),
    offsetY: clampOffset(raw.offsetY),
    brightness: clampBrightness(raw.brightness)
  };
}

export function hasNonDefaultEdits(edits = {}) {
  const clean = normalizeEditorState(edits);
  return (
    clean.aspectRatio !== "original" ||
    clean.zoom > DEFAULT_CROP_ZOOM + 0.001 ||
    clean.brightness !== DEFAULT_BRIGHTNESS ||
    Math.abs(clean.offsetX) > 0.001 ||
    Math.abs(clean.offsetY) > 0.001
  );
}

export function parseAspectRatio(aspect, imageWidth, imageHeight) {
  const safeW = Math.max(1, Number(imageWidth) || 1);
  const safeH = Math.max(1, Number(imageHeight) || 1);
  const preset = normalizeAspectPreset(aspect);
  if (preset === "1:1") return 1;
  if (preset === "4:3") return 4 / 3;
  if (preset === "4:5") return 4 / 5;
  if (preset === "16:9") return 16 / 9;
  if (preset === "3:4") return 3 / 4;
  if (preset === "9:16") return 9 / 16;
  return safeW / safeH;
}

/**
 * Computes the clamped source crop rectangle (sx, sy, sw, sh) inside an image
 * for a given aspect ratio preset, zoom factor (1x–3x), and normalized pan offset (-1..1).
 */
export function computeCropSourceRect({
  imageWidth,
  imageHeight,
  aspectRatio = "original",
  zoom = DEFAULT_CROP_ZOOM,
  offsetX = 0,
  offsetY = 0
} = {}) {
  const imgW = Math.max(1, Number(imageWidth) || 1);
  const imgH = Math.max(1, Number(imageHeight) || 1);
  const safeZoom = clampCropZoom(zoom);
  const safeOffsetX = clampOffset(offsetX);
  const safeOffsetY = clampOffset(offsetY);

  const imgAspect = imgW / imgH;
  const targetAspect = parseAspectRatio(aspectRatio, imgW, imgH);

  let baseW = imgW;
  let baseH = imgH;
  if (targetAspect > imgAspect) {
    baseW = imgW;
    baseH = imgW / targetAspect;
  } else if (targetAspect < imgAspect) {
    baseH = imgH;
    baseW = imgH * targetAspect;
  }

  const sw = Math.max(1, Math.min(imgW, baseW / safeZoom));
  const sh = Math.max(1, Math.min(imgH, baseH / safeZoom));

  const maxPanX = Math.max(0, (imgW - sw) / 2);
  const maxPanY = Math.max(0, (imgH - sh) / 2);

  const centerX = imgW / 2 + safeOffsetX * maxPanX;
  const centerY = imgH / 2 + safeOffsetY * maxPanY;

  const sx = Math.max(0, Math.min(imgW - sw, centerX - sw / 2));
  const sy = Math.max(0, Math.min(imgH - sh, centerY - sh / 2));

  return {
    sx: Math.round(sx),
    sy: Math.round(sy),
    sw: Math.max(1, Math.round(sw)),
    sh: Math.max(1, Math.round(sh)),
    targetAspect,
    maxPanX,
    maxPanY
  };
}

/**
 * Pixel-level RGBA brightness adjustment fallback for environments without Canvas 2D ctx.filter.
 */
export function applyBrightnessToRgbaPixels(rgbaData, brightnessPct = DEFAULT_BRIGHTNESS) {
  if (!rgbaData || typeof rgbaData.length !== "number") return rgbaData;
  const pct = clampBrightness(brightnessPct);
  if (pct === DEFAULT_BRIGHTNESS) return rgbaData;
  const factor = pct / 100;
  for (let i = 0; i < rgbaData.length; i += 4) {
    rgbaData[i] = Math.max(0, Math.min(255, Math.round(rgbaData[i] * factor)));
    rgbaData[i + 1] = Math.max(0, Math.min(255, Math.round(rgbaData[i + 1] * factor)));
    rgbaData[i + 2] = Math.max(0, Math.min(255, Math.round(rgbaData[i + 2] * factor)));
  }
  return rgbaData;
}

/**
 * Renders the cropped and brightness-adjusted image onto an HTML Canvas 2D context.
 * Optionally overlays a rule-of-thirds studio framing grid on interactive previews.
 */
export function drawEditedImageToCanvas(
  canvas,
  img,
  edits = {},
  { maxDim = 720, showGrid = false, backgroundColor = "#0C0C0C" } = {}
) {
  if (!canvas || !img) {
    throw new Error("Canvas and source image are required for rendering.");
  }
  const cleanEdits = normalizeEditorState(edits);
  const imageWidth = Math.max(1, Number(img.naturalWidth || img.width || 1));
  const imageHeight = Math.max(1, Number(img.naturalHeight || img.height || 1));
  const crop = computeCropSourceRect({
    imageWidth,
    imageHeight,
    aspectRatio: cleanEdits.aspectRatio,
    zoom: cleanEdits.zoom,
    offsetX: cleanEdits.offsetX,
    offsetY: cleanEdits.offsetY
  });

  const dims = computeScaledDimensions(crop.sw, crop.sh, maxDim);
  canvas.width = dims.width;
  canvas.height = dims.height;

  const ctx = canvas.getContext("2d");
  if (!ctx) {
    throw new Error("Browser HTML Canvas 2D context is unavailable.");
  }

  ctx.save?.();
  ctx.fillStyle = backgroundColor;
  ctx.fillRect(0, 0, dims.width, dims.height);

  let usedCanvasFilter = false;
  if ("filter" in ctx) {
    const filterExpr = cleanEdits.brightness === DEFAULT_BRIGHTNESS
      ? "none"
      : `brightness(${cleanEdits.brightness}%)`;
    ctx.filter = filterExpr;
    usedCanvasFilter =
      cleanEdits.brightness === DEFAULT_BRIGHTNESS ||
      String(ctx.filter || "").includes("brightness");
  }

  ctx.drawImage(img, crop.sx, crop.sy, crop.sw, crop.sh, 0, 0, dims.width, dims.height);
  if ("filter" in ctx) {
    ctx.filter = "none";
  }

  if (
    cleanEdits.brightness !== DEFAULT_BRIGHTNESS &&
    !usedCanvasFilter &&
    typeof ctx.getImageData === "function" &&
    typeof ctx.putImageData === "function"
  ) {
    const imageData = ctx.getImageData(0, 0, dims.width, dims.height);
    applyBrightnessToRgbaPixels(imageData.data, cleanEdits.brightness);
    ctx.putImageData(imageData, 0, 0);
  }

  if (showGrid && typeof ctx.beginPath === "function") {
    ctx.strokeStyle = "rgba(212, 175, 55, 0.55)";
    ctx.lineWidth = Math.max(1, Math.round(Math.min(dims.width, dims.height) / 360));
    const thirdsX = [dims.width / 3, (dims.width * 2) / 3];
    const thirdsY = [dims.height / 3, (dims.height * 2) / 3];
    ctx.beginPath();
    for (const x of thirdsX) {
      ctx.moveTo(x, 0);
      ctx.lineTo(x, dims.height);
    }
    for (const y of thirdsY) {
      ctx.moveTo(0, y);
      ctx.lineTo(dims.width, y);
    }
    ctx.stroke();

    if (typeof ctx.strokeRect === "function") {
      ctx.strokeStyle = "rgba(212, 175, 55, 0.85)";
      ctx.lineWidth = Math.max(2, Math.round(Math.min(dims.width, dims.height) / 220));
      ctx.strokeRect(1, 1, Math.max(1, dims.width - 2), Math.max(1, dims.height - 2));
    }
  }

  ctx.restore?.();

  return {
    width: dims.width,
    height: dims.height,
    crop,
    edits: cleanEdits
  };
}

/**
 * Renders the full source image onto an interactive HTML Canvas workspace with a
 * darkened outer mask, crisp gold crop box, corner handles, and rule-of-thirds grid
 * so users can visually frame and reposition their crop before uploading.
 */
export function drawCropWorkspaceToCanvas(
  canvas,
  img,
  edits = {},
  { maxDim = 680, backgroundColor = "#0C0C0C" } = {}
) {
  if (!canvas || !img) {
    throw new Error("Canvas and source image are required for crop workspace rendering.");
  }
  const cleanEdits = normalizeEditorState(edits);
  const imageWidth = Math.max(1, Number(img.naturalWidth || img.width || 1));
  const imageHeight = Math.max(1, Number(img.naturalHeight || img.height || 1));
  const crop = computeCropSourceRect({
    imageWidth,
    imageHeight,
    aspectRatio: cleanEdits.aspectRatio,
    zoom: cleanEdits.zoom,
    offsetX: cleanEdits.offsetX,
    offsetY: cleanEdits.offsetY
  });

  const dims = computeScaledDimensions(imageWidth, imageHeight, maxDim);
  canvas.width = dims.width;
  canvas.height = dims.height;

  const ctx = canvas.getContext("2d");
  if (!ctx) {
    throw new Error("Browser HTML Canvas 2D context is unavailable.");
  }

  ctx.save?.();
  ctx.fillStyle = backgroundColor;
  ctx.fillRect(0, 0, dims.width, dims.height);

  let usedCanvasFilter = false;
  if ("filter" in ctx) {
    const filterExpr = cleanEdits.brightness === DEFAULT_BRIGHTNESS
      ? "none"
      : `brightness(${cleanEdits.brightness}%)`;
    ctx.filter = filterExpr;
    usedCanvasFilter =
      cleanEdits.brightness === DEFAULT_BRIGHTNESS ||
      String(ctx.filter || "").includes("brightness");
  }

  ctx.drawImage(img, 0, 0, imageWidth, imageHeight, 0, 0, dims.width, dims.height);
  if ("filter" in ctx) {
    ctx.filter = "none";
  }

  if (
    cleanEdits.brightness !== DEFAULT_BRIGHTNESS &&
    !usedCanvasFilter &&
    typeof ctx.getImageData === "function" &&
    typeof ctx.putImageData === "function"
  ) {
    const imageData = ctx.getImageData(0, 0, dims.width, dims.height);
    applyBrightnessToRgbaPixels(imageData.data, cleanEdits.brightness);
    ctx.putImageData(imageData, 0, 0);
  }

  const scaleX = dims.width / imageWidth;
  const scaleY = dims.height / imageHeight;
  const rx = Math.round(crop.sx * scaleX);
  const ry = Math.round(crop.sy * scaleY);
  const rw = Math.max(2, Math.min(dims.width - rx, Math.round(crop.sw * scaleX)));
  const rh = Math.max(2, Math.min(dims.height - ry, Math.round(crop.sh * scaleY)));

  // Darken regions outside the active crop rectangle
  ctx.fillStyle = "rgba(0, 0, 0, 0.58)";
  if (ry > 0) ctx.fillRect(0, 0, dims.width, ry);
  if (ry + rh < dims.height) ctx.fillRect(0, ry + rh, dims.width, dims.height - (ry + rh));
  if (rx > 0) ctx.fillRect(0, ry, rx, rh);
  if (rx + rw < dims.width) ctx.fillRect(rx + rw, ry, dims.width - (rx + rw), rh);

  // Rule-of-thirds framing lines + gold border + corner handles
  if (typeof ctx.beginPath === "function") {
    ctx.strokeStyle = "rgba(212, 175, 55, 0.45)";
    ctx.lineWidth = Math.max(1, Math.round(Math.min(dims.width, dims.height) / 420));
    ctx.beginPath();
    ctx.moveTo(rx + rw / 3, ry);
    ctx.lineTo(rx + rw / 3, ry + rh);
    ctx.moveTo(rx + (rw * 2) / 3, ry);
    ctx.lineTo(rx + (rw * 2) / 3, ry + rh);
    ctx.moveTo(rx, ry + rh / 3);
    ctx.lineTo(rx + rw, ry + rh / 3);
    ctx.moveTo(rx, ry + (rh * 2) / 3);
    ctx.lineTo(rx + rw, ry + (rh * 2) / 3);
    ctx.stroke();

    if (typeof ctx.strokeRect === "function") {
      ctx.strokeStyle = "#D4AF37";
      ctx.lineWidth = Math.max(2, Math.round(Math.min(dims.width, dims.height) / 240));
      ctx.strokeRect(rx + 1, ry + 1, Math.max(1, rw - 2), Math.max(1, rh - 2));
    }

    const handleLen = Math.max(10, Math.min(22, Math.round(Math.min(rw, rh) * 0.14)));
    ctx.strokeStyle = "#F5D77A";
    ctx.lineWidth = Math.max(3, Math.round(Math.min(dims.width, dims.height) / 170));
    ctx.beginPath();
    // Top-left
    ctx.moveTo(rx + 1, ry + handleLen);
    ctx.lineTo(rx + 1, ry + 1);
    ctx.lineTo(rx + handleLen, ry + 1);
    // Top-right
    ctx.moveTo(rx + rw - handleLen, ry + 1);
    ctx.lineTo(rx + rw - 1, ry + 1);
    ctx.lineTo(rx + rw - 1, ry + handleLen);
    // Bottom-left
    ctx.moveTo(rx + 1, ry + rh - handleLen);
    ctx.lineTo(rx + 1, ry + rh - 1);
    ctx.lineTo(rx + handleLen, ry + rh - 1);
    // Bottom-right
    ctx.moveTo(rx + rw - handleLen, ry + rh - 1);
    ctx.lineTo(rx + rw - 1, ry + rh - 1);
    ctx.lineTo(rx + rw - 1, ry + rh - handleLen);
    ctx.stroke();
  }

  ctx.restore?.();

  return {
    width: dims.width,
    height: dims.height,
    crop,
    frameRect: { x: rx, y: ry, width: rw, height: rh },
    edits: cleanEdits
  };
}

/**
 * Converts a base64 JPEG Data URL into a File or Blob for Storage upload.
 */
export function dataUrlToImageBlob(dataUrl, fileName = "studio-photo.jpg") {
  const raw = String(dataUrl || "");
  const commaIdx = raw.indexOf(",");
  if (commaIdx === -1) return null;
  const header = raw.slice(0, commaIdx);
  const b64 = raw.slice(commaIdx + 1);
  const mimeMatch = /data:([^;]+);base64/i.exec(header);
  const mime = mimeMatch ? mimeMatch[1] : "image/jpeg";

  let bytes;
  if (typeof atob === "function") {
    const binary = atob(b64);
    bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
  } else if (typeof Buffer !== "undefined") {
    bytes = new Uint8Array(Buffer.from(b64, "base64"));
  } else {
    return null;
  }

  const safeName = String(fileName || "studio-photo.jpg")
    .replace(/\.[^.]+$/, "") + ".jpg";
  if (typeof File === "function") {
    try {
      return new File([bytes], safeName, { type: mime });
    } catch {
      // Fallback to Blob when File constructor is restricted
    }
  }
  if (typeof Blob === "function") {
    return new Blob([bytes], { type: mime });
  }
  return null;
}

function renderEditedCompressedJpeg(
  img,
  edits,
  maxDim,
  maxChars,
  startQuality,
  minQuality,
  createCanvas
) {
  let targetDim = maxDim;
  for (let pass = 0; pass < 4; pass++) {
    const canvas = createCanvas();
    const rendered = drawEditedImageToCanvas(canvas, img, edits, {
      maxDim: targetDim,
      showGrid: false
    });
    let quality = startQuality;
    while (quality >= minQuality - 0.001) {
      const dataUrl = canvas.toDataURL("image/jpeg", Number(quality.toFixed(2)));
      if (dataUrl.length <= maxChars) {
        return {
          dataUrl,
          width: rendered.width,
          height: rendered.height,
          crop: rendered.crop
        };
      }
      quality -= 0.08;
    }
    targetDim = Math.max(240, Math.round(targetDim * 0.78));
  }
  throw new Error("Edited photo is too detailed for free-tier cloud sync. Please crop tighter or choose a smaller image.");
}

/**
 * Exports the cropped & brightness-adjusted image using HTML Canvas API
 * into Firestore-safe compressed JPEG Data URLs + Blob/File for Storage upload.
 */
export function exportEditedCanvasPayloads({
  img,
  edits = {},
  originalName = "studio-photo.jpg",
  metadata = null,
  rawDataUrl = "",
  maxMainDim = 1280,
  maxThumbDim = 480,
  createCanvas = () => document.createElement("canvas")
} = {}) {
  const cleanEdits = normalizeEditorState(edits);
  const main = renderEditedCompressedJpeg(
    img,
    cleanEdits,
    maxMainDim,
    MAX_MEDIA_DATA_URL_CHARS,
    0.84,
    0.52,
    createCanvas
  );
  const thumb = renderEditedCompressedJpeg(
    img,
    cleanEdits,
    maxThumbDim,
    MAX_THUMB_DATA_URL_CHARS,
    0.74,
    0.48,
    createCanvas
  );
  const approxKb = Math.max(1, Math.round((main.dataUrl.length * 0.75) / 1024));
  const editedFile = dataUrlToImageBlob(main.dataUrl, originalName);

  return {
    file: editedFile,
    mediaUrl: main.dataUrl,
    thumbnailUrl: thumb.dataUrl,
    rawDataUrl,
    width: main.width,
    height: main.height,
    originalName: String(originalName || "studio-photo.jpg"),
    approxKb,
    metadata,
    edits: cleanEdits,
    isEdited: hasNonDefaultEdits(cleanEdits)
  };
}

export function formatEditorSummary(edits = {}) {
  const clean = normalizeEditorState(edits);
  const parts = [];
  if (clean.aspectRatio !== "original") {
    parts.push(`Crop ${clean.aspectRatio}`);
  }
  if (clean.zoom > DEFAULT_CROP_ZOOM + 0.001) {
    parts.push(`Zoom ${clean.zoom.toFixed(1)}×`);
  }
  if (clean.brightness !== DEFAULT_BRIGHTNESS) {
    parts.push(`Brightness ${clean.brightness}%`);
  }
  return parts.join(" • ");
}
