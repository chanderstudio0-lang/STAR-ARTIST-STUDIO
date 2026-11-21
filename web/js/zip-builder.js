/**
 * Zero-dependency PKZIP (STORE / method 0) archive builder for Star Artist Studio PWA.
 * Produces standard .zip files compatible with Android Files, iOS Files, Windows, macOS, and Linux
 * while working 100% offline inside the Service Worker cache on Firebase's free Spark plan.
 */

const CRC32_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let i = 0; i < 256; i++) {
    let c = i;
    for (let k = 0; k < 8; k++) {
      c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
    }
    table[i] = c >>> 0;
  }
  return table;
})();

export function crc32(bytes) {
  const data = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes || 0);
  let crc = 0xffffffff;
  for (let i = 0; i < data.length; i++) {
    crc = CRC32_TABLE[(crc ^ data[i]) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

export function sanitizeZipEntryName(rawName, fallback = "studio-image.jpg") {
  const cleaned = String(rawName || "")
    .replaceAll("\\", "/")
    .split("/")
    .pop()
    .replace(/[\u0000-\u001f<>:"|?*]/g, "")
    .replace(/^\.+/, "")
    .trim();

  if (!cleaned) return fallback;
  return cleaned.slice(0, 120);
}

export function makeUniqueZipEntryName(desiredName, usedLowerSet) {
  const safe = sanitizeZipEntryName(desiredName);
  const lower = safe.toLowerCase();
  if (!usedLowerSet.has(lower)) {
    usedLowerSet.add(lower);
    return safe;
  }

  const dotIdx = safe.lastIndexOf(".");
  const stem = dotIdx > 0 ? safe.slice(0, dotIdx) : safe;
  const ext = dotIdx > 0 ? safe.slice(dotIdx) : "";

  let counter = 2;
  while (counter < 10000) {
    const candidate = `${stem}-${counter}${ext}`;
    const candidateLower = candidate.toLowerCase();
    if (!usedLowerSet.has(candidateLower)) {
      usedLowerSet.add(candidateLower);
      return candidate;
    }
    counter += 1;
  }
  const fallback = `${stem}-${Date.now()}${ext}`;
  usedLowerSet.add(fallback.toLowerCase());
  return fallback;
}

function encodeDosDateTime(dateInput) {
  const d = dateInput instanceof Date && !Number.isNaN(dateInput.getTime()) ? dateInput : new Date();
  const year = Math.max(1980, Math.min(2107, d.getFullYear()));
  const dosTime =
    ((d.getHours() & 0x1f) << 11) |
    ((d.getMinutes() & 0x3f) << 5) |
    ((Math.floor(d.getSeconds() / 2)) & 0x1f);
  const dosDate =
    (((year - 1980) & 0x7f) << 9) |
    (((d.getMonth() + 1) & 0x0f) << 5) |
    (d.getDate() & 0x1f);
  return { dosTime, dosDate };
}

function utf8Encode(str) {
  if (typeof TextEncoder === "function") {
    return new TextEncoder().encode(str);
  }
  const out = [];
  for (let i = 0; i < str.length; i++) {
    out.push(str.charCodeAt(i) & 0xff);
  }
  return new Uint8Array(out);
}

/**
 * Builds a valid PKZIP archive (STORE method, UTF-8 names) from an array of { name, data } entries.
 * @param {Array<{name: string, data: Uint8Array|ArrayBuffer}>} entries
 * @param {{ date?: Date }} [options]
 * @returns {Uint8Array}
 */
export function buildZipArchive(entries, { date = new Date() } = {}) {
  const safeEntries = Array.isArray(entries) ? entries : [];
  const { dosTime, dosDate } = encodeDosDateTime(date);
  const usedNames = new Set();

  const prepared = safeEntries.map((entry, idx) => {
    const uniqueName = makeUniqueZipEntryName(entry?.name || `image-${idx + 1}.jpg`, usedNames);
    const nameBytes = utf8Encode(uniqueName);
    const dataBytes =
      entry?.data instanceof Uint8Array
        ? entry.data
        : entry?.data instanceof ArrayBuffer
          ? new Uint8Array(entry.data)
          : new Uint8Array(entry?.data || 0);
    return {
      name: uniqueName,
      nameBytes,
      dataBytes,
      crc: crc32(dataBytes),
      localHeaderOffset: 0
    };
  });

  // Calculate total archive size
  let localSectionSize = 0;
  let centralDirSize = 0;
  for (const item of prepared) {
    item.localHeaderOffset = localSectionSize;
    localSectionSize += 30 + item.nameBytes.length + item.dataBytes.length;
    centralDirSize += 46 + item.nameBytes.length;
  }

  const totalSize = localSectionSize + centralDirSize + 22;
  const output = new Uint8Array(totalSize);
  const view = new DataView(output.buffer);

  // 1. Local file headers + file payloads
  let offset = 0;
  for (const item of prepared) {
    view.setUint32(offset, 0x04034b50, true); // Local file header signature
    view.setUint16(offset + 4, 20, true); // Version needed to extract (2.0)
    view.setUint16(offset + 6, 0x0800, true); // General purpose bit flag (UTF-8 filenames)
    view.setUint16(offset + 8, 0, true); // Compression method: 0 (STORE)
    view.setUint16(offset + 10, dosTime, true);
    view.setUint16(offset + 12, dosDate, true);
    view.setUint32(offset + 14, item.crc, true);
    view.setUint32(offset + 18, item.dataBytes.length, true); // Compressed size
    view.setUint32(offset + 22, item.dataBytes.length, true); // Uncompressed size
    view.setUint16(offset + 26, item.nameBytes.length, true); // File name length
    view.setUint16(offset + 28, 0, true); // Extra field length
    offset += 30;

    output.set(item.nameBytes, offset);
    offset += item.nameBytes.length;

    output.set(item.dataBytes, offset);
    offset += item.dataBytes.length;
  }

  // 2. Central directory file headers
  const centralDirOffset = offset;
  for (const item of prepared) {
    view.setUint32(offset, 0x02014b50, true); // Central directory header signature
    view.setUint16(offset + 4, 20, true); // Version made by
    view.setUint16(offset + 6, 20, true); // Version needed to extract
    view.setUint16(offset + 8, 0x0800, true); // UTF-8 filename flag
    view.setUint16(offset + 10, 0, true); // Compression method: 0 (STORE)
    view.setUint16(offset + 12, dosTime, true);
    view.setUint16(offset + 14, dosDate, true);
    view.setUint32(offset + 16, item.crc, true);
    view.setUint32(offset + 20, item.dataBytes.length, true);
    view.setUint32(offset + 24, item.dataBytes.length, true);
    view.setUint16(offset + 28, item.nameBytes.length, true);
    view.setUint16(offset + 30, 0, true); // Extra field length
    view.setUint16(offset + 32, 0, true); // File comment length
    view.setUint16(offset + 34, 0, true); // Disk number start
    view.setUint16(offset + 36, 0, true); // Internal file attributes
    view.setUint32(offset + 38, 0, true); // External file attributes
    view.setUint32(offset + 42, item.localHeaderOffset, true);
    offset += 46;

    output.set(item.nameBytes, offset);
    offset += item.nameBytes.length;
  }

  // 3. End of central directory record (EOCD)
  view.setUint32(offset, 0x06054b50, true); // EOCD signature
  view.setUint16(offset + 4, 0, true); // Number of this disk
  view.setUint16(offset + 6, 0, true); // Disk where central directory starts
  view.setUint16(offset + 8, prepared.length, true); // Entries on this disk
  view.setUint16(offset + 10, prepared.length, true); // Total entries
  view.setUint32(offset + 12, centralDirSize, true); // Size of central directory
  view.setUint32(offset + 16, centralDirOffset, true); // Offset of start of central directory
  view.setUint16(offset + 20, 0, true); // Comment length

  return output;
}
