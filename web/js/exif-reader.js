/**
 * Zero-dependency binary EXIF metadata reader for STAR ARTIST STUDIO.
 * Parses JPEG (APP1 Exif), PNG (eXIf chunk), and WebP (EXIF chunk) TIFF headers
 * to automatically extract Camera, Lens, Aperture, Shutter Speed, ISO, and Date Taken.
 */

const EXIF_HEADER_SLICE_BYTES = 262144;

const IFD0_TAGS = {
  MAKE: 0x010f,
  MODEL: 0x0110,
  DATETIME: 0x0132,
  EXIF_IFD_POINTER: 0x8769
};

const EXIF_TAGS = {
  EXPOSURE_TIME: 0x829a,
  F_NUMBER: 0x829d,
  ISO_SPEED: 0x8827,
  PHOTOGRAPHIC_SENSITIVITY: 0x8833,
  DATETIME_ORIGINAL: 0x9003,
  CREATE_DATE: 0x9004,
  FOCAL_LENGTH: 0x920a,
  LENS_MODEL: 0xa434
};

const TYPE_BYTE_SIZES = {
  1: 1, // BYTE
  2: 1, // ASCII
  3: 2, // SHORT
  4: 4, // LONG
  5: 8, // RATIONAL
  7: 1, // UNDEFINED
  9: 4, // SLONG
  10: 8 // SRATIONAL
};

function readAscii(view, start, length) {
  const end = Math.min(view.byteLength, start + length);
  let out = "";
  for (let i = start; i < end; i++) {
    const code = view.getUint8(i);
    if (code === 0) break;
    if (code >= 32 && code <= 126) {
      out += String.fromCharCode(code);
    }
  }
  return out.trim();
}

function readTagValue(view, tiffStart, entryOffset, littleEndian) {
  if (entryOffset + 12 > view.byteLength) return null;
  const type = view.getUint16(entryOffset + 2, littleEndian);
  const count = view.getUint32(entryOffset + 4, littleEndian);
  const unitSize = TYPE_BYTE_SIZES[type];
  if (!unitSize || count <= 0 || count > 4096) return null;

  const byteLen = unitSize * count;
  const valueOffset =
    byteLen <= 4
      ? entryOffset + 8
      : tiffStart + view.getUint32(entryOffset + 8, littleEndian);

  if (valueOffset < 0 || valueOffset + byteLen > view.byteLength) return null;

  if (type === 2) {
    return readAscii(view, valueOffset, count);
  }
  if (type === 3) {
    return view.getUint16(valueOffset, littleEndian);
  }
  if (type === 4) {
    return view.getUint32(valueOffset, littleEndian);
  }
  if (type === 5) {
    const num = view.getUint32(valueOffset, littleEndian);
    const den = view.getUint32(valueOffset + 4, littleEndian);
    if (!den) return null;
    return { num, den, value: num / den };
  }
  if (type === 9) {
    return view.getInt32(valueOffset, littleEndian);
  }
  if (type === 10) {
    const num = view.getInt32(valueOffset, littleEndian);
    const den = view.getInt32(valueOffset + 4, littleEndian);
    if (!den) return null;
    return { num, den, value: num / den };
  }
  return null;
}

function parseIfd(view, tiffStart, ifdOffset, littleEndian, wantedTagSet) {
  const result = {};
  const absOffset = tiffStart + ifdOffset;
  if (absOffset < tiffStart || absOffset + 2 > view.byteLength) return result;
  const entryCount = view.getUint16(absOffset, littleEndian);
  const safeCount = Math.min(entryCount, 256);

  for (let i = 0; i < safeCount; i++) {
    const entryOffset = absOffset + 2 + i * 12;
    if (entryOffset + 12 > view.byteLength) break;
    const tagId = view.getUint16(entryOffset, littleEndian);
    if (wantedTagSet.has(tagId)) {
      const val = readTagValue(view, tiffStart, entryOffset, littleEndian);
      if (val !== null && val !== "") {
        result[tagId] = val;
      }
    }
  }
  return result;
}

export function formatExifAperture(fNumber) {
  const num = typeof fNumber === "object" ? fNumber?.value : Number(fNumber);
  if (!Number.isFinite(num) || num <= 0 || num > 128) return "";
  const rounded = Math.round(num * 10) / 10;
  return `f/${Number.isInteger(rounded) ? `${rounded}.0` : rounded}`;
}

export function formatExifShutterSpeed(exposure) {
  const val = typeof exposure === "object" ? exposure?.value : Number(exposure);
  if (!Number.isFinite(val) || val <= 0 || val > 3600) return "";
  if (val >= 1) {
    const rounded = Math.round(val * 10) / 10;
    return `${rounded}s`;
  }
  const denom = Math.round(1 / val);
  if (denom <= 1) return "1s";
  return `1/${denom}s`;
}

export function formatExifIso(isoVal) {
  const num = Math.round(Number(isoVal));
  if (!Number.isFinite(num) || num <= 0 || num > 4096000) return "";
  return `ISO ${num}`;
}

export function formatExifFocalLength(focal) {
  const val = typeof focal === "object" ? focal?.value : Number(focal);
  if (!Number.isFinite(val) || val <= 0 || val > 5000) return "";
  const rounded = Math.round(val);
  return `${rounded}mm`;
}

export function normalizeExifDateString(rawDateStr) {
  const clean = String(rawDateStr || "").trim();
  if (!clean) return "";
  // Standard EXIF format: "YYYY:MM:DD HH:MM:SS"
  const match = clean.match(/^(\d{4})[:\-/](\d{2})[:\-/](\d{2})/);
  if (!match) return "";
  const [, year, month, day] = match;
  if (Number(year) < 1970 || Number(month) < 1 || Number(month) > 12 || Number(day) < 1 || Number(day) > 31) {
    return "";
  }
  return `${year}-${month}-${day}`;
}

function combineCameraMakeModel(make, model) {
  const cleanMake = String(make || "").trim();
  const cleanModel = String(model || "").trim();
  if (!cleanMake) return cleanModel;
  if (!cleanModel) return cleanMake;
  if (cleanModel.toLowerCase().startsWith(cleanMake.toLowerCase())) {
    return cleanModel;
  }
  return `${cleanMake} ${cleanModel}`;
}

function parseTiffExifBlock(view, tiffStart) {
  if (tiffStart < 0 || tiffStart + 8 > view.byteLength) return null;
  const endian = view.getUint16(tiffStart, false);
  let littleEndian;
  if (endian === 0x4949) {
    littleEndian = true; // "II" Intel
  } else if (endian === 0x4d4d) {
    littleEndian = false; // "MM" Motorola
  } else {
    return null;
  }

  if (view.getUint16(tiffStart + 2, littleEndian) !== 0x002a) {
    return null;
  }

  const firstIfdOffset = view.getUint32(tiffStart + 4, littleEndian);
  const ifd0Wanted = new Set(Object.values(IFD0_TAGS));
  const ifd0 = parseIfd(view, tiffStart, firstIfdOffset, littleEndian, ifd0Wanted);

  let exifIfd = {};
  if (Number.isInteger(ifd0[IFD0_TAGS.EXIF_IFD_POINTER])) {
    const exifWanted = new Set(Object.values(EXIF_TAGS));
    exifIfd = parseIfd(
      view,
      tiffStart,
      ifd0[IFD0_TAGS.EXIF_IFD_POINTER],
      littleEndian,
      exifWanted
    );
  }

  const camera = combineCameraMakeModel(ifd0[IFD0_TAGS.MAKE], ifd0[IFD0_TAGS.MODEL]);
  const lensModel = String(exifIfd[EXIF_TAGS.LENS_MODEL] || "").trim();
  const focalFallback = formatExifFocalLength(exifIfd[EXIF_TAGS.FOCAL_LENGTH]);
  const lens = lensModel || focalFallback;
  const aperture = formatExifAperture(exifIfd[EXIF_TAGS.F_NUMBER]);
  const shutterSpeed = formatExifShutterSpeed(exifIfd[EXIF_TAGS.EXPOSURE_TIME]);
  const iso = formatExifIso(
    exifIfd[EXIF_TAGS.ISO_SPEED] ?? exifIfd[EXIF_TAGS.PHOTOGRAPHIC_SENSITIVITY]
  );
  const dateTaken = normalizeExifDateString(
    exifIfd[EXIF_TAGS.DATETIME_ORIGINAL] ||
      exifIfd[EXIF_TAGS.CREATE_DATE] ||
      ifd0[IFD0_TAGS.DATETIME]
  );

  const meta = {
    camera,
    lens,
    aperture,
    shutterSpeed,
    iso,
    dateTaken
  };
  return {
    ...meta,
    hasExif: hasMeaningfulExif(meta)
  };
}

function locateTiffStartInJpeg(view) {
  if (view.byteLength < 4 || view.getUint16(0, false) !== 0xffd8) return -1;
  let offset = 2;
  while (offset + 4 <= view.byteLength) {
    if (view.getUint8(offset) !== 0xff) {
      offset += 1;
      continue;
    }
    const marker = view.getUint8(offset + 1);
    if (marker === 0xd9 || marker === 0xda) break; // EOI or SOS
    if (marker === 0x01 || (marker >= 0xd0 && marker <= 0xd8)) {
      offset += 2;
      continue;
    }
    if (offset + 4 > view.byteLength) break;
    const segmentLen = view.getUint16(offset + 2, false);
    if (segmentLen < 2) break;

    if (marker === 0xe1 && offset + 10 <= view.byteLength) {
      const header = readAscii(view, offset + 4, 6);
      if (header === "Exif") {
        return offset + 10;
      }
    }
    offset += 2 + segmentLen;
  }
  return -1;
}

function locateTiffStartInPng(view) {
  // PNG signature: 89 50 4E 47 0D 0A 1A 0A
  if (
    view.byteLength < 16 ||
    view.getUint32(0, false) !== 0x89504e47 ||
    view.getUint32(4, false) !== 0x0d0a1a0a
  ) {
    return -1;
  }
  let offset = 8;
  while (offset + 12 <= view.byteLength) {
    const chunkLen = view.getUint32(offset, false);
    const chunkType = readAscii(view, offset + 4, 4);
    if (chunkType === "eXIf" && offset + 8 + chunkLen <= view.byteLength) {
      return offset + 8;
    }
    if (chunkType === "IEND") break;
    offset += 12 + chunkLen;
  }
  return -1;
}

function locateTiffStartInWebp(view) {
  if (view.byteLength < 16) return -1;
  if (readAscii(view, 0, 4) !== "RIFF" || readAscii(view, 8, 4) !== "WEBP") return -1;
  let offset = 12;
  while (offset + 8 <= view.byteLength) {
    const chunkId = readAscii(view, offset, 4);
    const chunkSize = view.getUint32(offset + 4, true);
    if (chunkId === "EXIF") {
      const payloadStart = offset + 8;
      if (payloadStart + 6 <= view.byteLength && readAscii(view, payloadStart, 6) === "Exif") {
        return payloadStart + 6;
      }
      return payloadStart;
    }
    offset += 8 + chunkSize + (chunkSize % 2);
  }
  return -1;
}

export function hasMeaningfulExif(meta) {
  if (!meta || typeof meta !== "object") return false;
  return Boolean(
    meta.camera ||
      meta.lens ||
      meta.aperture ||
      meta.shutterSpeed ||
      meta.iso ||
      meta.dateTaken
  );
}

export function sanitizeExifMetadata(meta) {
  if (!meta || typeof meta !== "object") return null;
  const clean = {
    camera: String(meta.camera || "").trim().slice(0, 100),
    lens: String(meta.lens || "").trim().slice(0, 100),
    aperture: String(meta.aperture || "").trim().slice(0, 32),
    shutterSpeed: String(meta.shutterSpeed || "").trim().slice(0, 32),
    iso: String(meta.iso || "").trim().slice(0, 32),
    dateTaken: String(meta.dateTaken || "").trim().slice(0, 40)
  };
  return hasMeaningfulExif(clean) ? clean : null;
}

/**
 * Extracts EXIF metadata directly from an ArrayBuffer or Uint8Array (JPEG, PNG, or WebP).
 */
export function parseExifFromArrayBuffer(input) {
  const empty = {
    camera: "",
    lens: "",
    aperture: "",
    shutterSpeed: "",
    iso: "",
    dateTaken: "",
    hasExif: false
  };
  if (!input) return empty;

  let view;
  if (input instanceof ArrayBuffer) {
    view = new DataView(input);
  } else if (ArrayBuffer.isView(input)) {
    view = new DataView(input.buffer, input.byteOffset, input.byteLength);
  } else {
    return empty;
  }

  let tiffStart = locateTiffStartInJpeg(view);
  if (tiffStart < 0) tiffStart = locateTiffStartInPng(view);
  if (tiffStart < 0) tiffStart = locateTiffStartInWebp(view);
  if (tiffStart < 0) return empty;

  return parseTiffExifBlock(view, tiffStart) || empty;
}

function decodeBase64DataUrlPrefix(dataUrl, maxBytes = EXIF_HEADER_SLICE_BYTES) {
  const commaIdx = dataUrl.indexOf(",");
  if (commaIdx < 0) return null;
  const header = dataUrl.slice(0, commaIdx).toLowerCase();
  if (!header.includes(";base64")) return null;
  const maxB64Chars = Math.ceil(maxBytes / 3) * 4;
  const b64 = dataUrl.slice(commaIdx + 1, commaIdx + 1 + maxB64Chars).replace(/\s+/g, "");
  const paddedLen = Math.floor(b64.length / 4) * 4;
  if (paddedLen < 4) return null;
  const slice = b64.slice(0, paddedLen);

  if (typeof atob === "function") {
    const binary = atob(slice);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes.buffer;
  }
  if (typeof Buffer !== "undefined") {
    const buf = Buffer.from(slice, "base64");
    return buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength);
  }
  return null;
}

/**
 * Reads EXIF metadata directly from a browser File or Blob before any canvas compression.
 */
export async function readExifFromFile(file) {
  const empty = {
    camera: "",
    lens: "",
    aperture: "",
    shutterSpeed: "",
    iso: "",
    dateTaken: "",
    hasExif: false
  };
  if (!file) return empty;

  try {
    const slice = typeof file.slice === "function" ? file.slice(0, EXIF_HEADER_SLICE_BYTES) : file;
    let buffer = null;
    if (typeof slice.arrayBuffer === "function") {
      buffer = await slice.arrayBuffer();
    } else if (typeof FileReader !== "undefined") {
      buffer = await new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = () => reject(new Error("EXIF file read failed"));
        reader.readAsArrayBuffer(slice);
      });
    }
    const parsed = parseExifFromArrayBuffer(buffer);
    if (!parsed.dateTaken && Number.isFinite(Number(file.lastModified)) && Number(file.lastModified) > 0) {
      const dt = new Date(Number(file.lastModified));
      if (!Number.isNaN(dt.getTime())) {
        parsed.dateTaken = dt.toISOString().slice(0, 10);
        parsed.hasExif = hasMeaningfulExif(parsed);
      }
    }
    return parsed;
  } catch {
    return empty;
  }
}

/**
 * Reads EXIF metadata directly from a Data URL or fetchable image URL.
 */
export async function readExifFromImageUrl(
  imageUrl,
  { fetchImpl = typeof fetch === "function" ? fetch : null } = {}
) {
  const empty = {
    camera: "",
    lens: "",
    aperture: "",
    shutterSpeed: "",
    iso: "",
    dateTaken: "",
    hasExif: false
  };
  const cleanUrl = String(imageUrl || "").trim();
  if (!cleanUrl) return empty;

  try {
    if (/^data:image\//i.test(cleanUrl)) {
      const buf = decodeBase64DataUrlPrefix(cleanUrl);
      return parseExifFromArrayBuffer(buf);
    }
    if (!fetchImpl) return empty;
    const res = await fetchImpl(cleanUrl);
    if (!res || !res.ok || typeof res.arrayBuffer !== "function") return empty;
    const buffer = await res.arrayBuffer();
    return parseExifFromArrayBuffer(buffer);
  } catch {
    return empty;
  }
}
