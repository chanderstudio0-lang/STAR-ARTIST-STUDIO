import { FIREBASE_PROJECT_DEFAULTS } from "./data.js";

export const BOOKING_SYNC_TAG = "star-artist-sync-bookings";
export const OFFLINE_DB_NAME = "star-artist-offline-sync-v1";
export const OFFLINE_DB_VERSION = 1;
export const OFFLINE_BOOKINGS_STORE = "pending_bookings";

/**
 * Validates booking fields locally using the same constraints enforced by firestore.rules isValidBooking().
 * Returns an error string if invalid, or null if valid.
 */
export function validateOfflineBookingPayload(payload) {
  const clientName = String(payload?.clientName || "").trim();
  const contactNumber = String(payload?.contactNumber || "").trim();
  const serviceType = String(payload?.serviceType || "").trim();
  const preferredDate = String(payload?.preferredDate || "").trim();
  const preferredTime = String(payload?.preferredTime || "").trim();
  const location = String(payload?.location || "").trim();
  const notes = String(payload?.notes || "").trim();

  if (clientName.length < 2 || clientName.length > 100) {
    return "Client name must be between 2 and 100 characters.";
  }
  if (contactNumber.length < 8 || contactNumber.length > 20) {
    return "Contact number must be between 8 and 20 characters.";
  }
  if (serviceType.length < 1 || serviceType.length > 100) {
    return "Please select a valid service package.";
  }
  if (preferredDate.length < 1 || preferredDate.length > 50) {
    return "Please choose a preferred shoot date.";
  }
  if (preferredTime.length > 50) {
    return "Preferred time is too long.";
  }
  if (location.length > 200) {
    return "Shoot location must be 200 characters or fewer.";
  }
  if (notes.length > 1000) {
    return "Notes must be 1000 characters or fewer.";
  }
  return null;
}

/**
 * Builds the Firestore REST v1 endpoint for the project's named bookings collection.
 */
export function buildFirestoreBookingsEndpoint(
  projectId = FIREBASE_PROJECT_DEFAULTS.projectId,
  databaseId = FIREBASE_PROJECT_DEFAULTS.databaseId
) {
  const safeProject = encodeURIComponent(String(projectId || FIREBASE_PROJECT_DEFAULTS.projectId).trim());
  const safeDb = encodeURIComponent(String(databaseId || FIREBASE_PROJECT_DEFAULTS.databaseId).trim());
  return `https://firestore.googleapis.com/v1/projects/${safeProject}/databases/${safeDb}/documents/bookings`;
}

/**
 * Converts a queued booking record into a Firestore REST v1 document matching firestore.rules.
 */
export function buildFirestoreBookingDocument(record, nowIso = new Date().toISOString()) {
  const p = record?.payload || {};
  const timestamp = String(nowIso || new Date().toISOString());
  return {
    fields: {
      userId: { stringValue: String(p.userId || record?.userId || "").trim() },
      clientName: { stringValue: String(p.clientName || "").trim() },
      contactNumber: { stringValue: String(p.contactNumber || "").trim() },
      serviceType: { stringValue: String(p.serviceType || "").trim() },
      preferredDate: { stringValue: String(p.preferredDate || "").trim() },
      preferredTime: { stringValue: String(p.preferredTime || "11:00 AM").trim() || "11:00 AM" },
      location: { stringValue: String(p.location || "Studio (Talwara Khurd)").trim() || "Studio (Talwara Khurd)" },
      notes: { stringValue: String(p.notes || "").trim() },
      status: { stringValue: "Pending" },
      fcmToken: { stringValue: "" },
      createdAt: { timestampValue: timestamp },
      updatedAt: { timestampValue: timestamp }
    }
  };
}

/**
 * Creates a normalized offline booking queue entry ready for IndexedDB + Service Worker Background Sync.
 */
export function createQueuedBookingRecord({
  payload,
  userId = "",
  authToken = "",
  projectId = FIREBASE_PROJECT_DEFAULTS.projectId,
  databaseId = FIREBASE_PROJECT_DEFAULTS.databaseId
}) {
  const id = `offline_bk_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const cleanUserId = String(userId || payload?.userId || "").trim();
  return {
    id,
    queuedAt: new Date().toISOString(),
    projectId: String(projectId || FIREBASE_PROJECT_DEFAULTS.projectId).trim(),
    databaseId: String(databaseId || FIREBASE_PROJECT_DEFAULTS.databaseId).trim(),
    authToken: String(authToken || "").trim(),
    userId: cleanUserId,
    payload: {
      userId: cleanUserId,
      clientName: String(payload?.clientName || "").trim(),
      contactNumber: String(payload?.contactNumber || "").trim(),
      serviceType: String(payload?.serviceType || "").trim(),
      preferredDate: String(payload?.preferredDate || "").trim(),
      preferredTime: String(payload?.preferredTime || "11:00 AM").trim() || "11:00 AM",
      location: String(payload?.location || "Studio (Talwara Khurd)").trim() || "Studio (Talwara Khurd)",
      notes: String(payload?.notes || "").trim(),
      status: "Pending",
      fcmToken: ""
    },
    attempts: 0,
    needsReauth: false,
    lastError: ""
  };
}

/**
 * Determines whether a submission error indicates an offline / unreachable network condition.
 */
export function isLikelyOfflineError(err, isNavigatorOnline = true) {
  if (!isNavigatorOnline) return true;
  if (!err) return false;
  const code = String(err.code || "").toLowerCase();
  const msg = String(err.message || err || "").toLowerCase();
  return (
    code.includes("unavailable") ||
    code.includes("network-request-failed") ||
    msg.includes("offline") ||
    msg.includes("failed to fetch") ||
    msg.includes("networkerror") ||
    msg.includes("network error") ||
    msg.includes("load failed") ||
    msg.includes("client is offline")
  );
}

export function openOfflineBookingDb(idb = typeof indexedDB !== "undefined" ? indexedDB : null) {
  return new Promise((resolve, reject) => {
    if (!idb) {
      reject(new Error("IndexedDB is not available in this environment."));
      return;
    }
    const req = idb.open(OFFLINE_DB_NAME, OFFLINE_DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(OFFLINE_BOOKINGS_STORE)) {
        db.createObjectStore(OFFLINE_BOOKINGS_STORE, { keyPath: "id" });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error || new Error("Failed to open offline sync database."));
  });
}

export async function enqueueOfflineBooking(record, idb) {
  const db = await openOfflineBookingDb(idb);
  return new Promise((resolve, reject) => {
    const tx = db.transaction(OFFLINE_BOOKINGS_STORE, "readwrite");
    tx.objectStore(OFFLINE_BOOKINGS_STORE).put(record);
    tx.oncomplete = () => {
      db.close();
      resolve(record);
    };
    tx.onerror = () => {
      db.close();
      reject(tx.error || new Error("Failed to save offline booking."));
    };
  });
}

export async function getQueuedBookings(idb) {
  try {
    const db = await openOfflineBookingDb(idb);
    return await new Promise((resolve, reject) => {
      const tx = db.transaction(OFFLINE_BOOKINGS_STORE, "readonly");
      const req = tx.objectStore(OFFLINE_BOOKINGS_STORE).getAll();
      req.onsuccess = () => {
        const items = Array.isArray(req.result) ? req.result : [];
        items.sort((a, b) => String(b.queuedAt || "").localeCompare(String(a.queuedAt || "")));
        db.close();
        resolve(items);
      };
      req.onerror = () => {
        db.close();
        reject(req.error || new Error("Failed to read offline bookings."));
      };
    });
  } catch {
    return [];
  }
}

export async function removeQueuedBooking(id, idb) {
  const db = await openOfflineBookingDb(idb);
  return new Promise((resolve, reject) => {
    const tx = db.transaction(OFFLINE_BOOKINGS_STORE, "readwrite");
    tx.objectStore(OFFLINE_BOOKINGS_STORE).delete(id);
    tx.oncomplete = () => {
      db.close();
      resolve(true);
    };
    tx.onerror = () => {
      db.close();
      reject(tx.error || new Error("Failed to remove queued booking."));
    };
  });
}

/**
 * Registers the Service Worker Background Sync tag so the browser wakes sw.js once connectivity returns.
 * Returns { registered: boolean, mode: "background-sync" | "sw-message" | "online-fallback" }.
 */
export async function registerBookingBackgroundSync(
  swContainer = typeof navigator !== "undefined" ? navigator.serviceWorker : null
) {
  if (!swContainer || !swContainer.ready) {
    return { registered: false, mode: "online-fallback" };
  }
  try {
    const registration = await swContainer.ready;
    if (registration && registration.sync && typeof registration.sync.register === "function") {
      await registration.sync.register(BOOKING_SYNC_TAG);
      return { registered: true, mode: "background-sync" };
    }
    if (registration?.active && typeof registration.active.postMessage === "function") {
      registration.active.postMessage({ type: "TRIGGER_BOOKING_SYNC" });
      return { registered: true, mode: "sw-message" };
    }
  } catch {
    // Graceful fallback when Background Sync is blocked or unsupported (e.g. Safari/private mode)
  }
  return { registered: false, mode: "online-fallback" };
}

/**
 * Client-side fallback flush for browsers without Background Sync or bookings that needed fresh auth.
 */
export async function flushQueuedBookingsFromClient({
  queuedRecords = null,
  createBookingFn,
  removeFn = removeQueuedBooking,
  isOnline = typeof navigator !== "undefined" ? navigator.onLine : true
}) {
  if (!isOnline || typeof createBookingFn !== "function") {
    return { syncedCount: 0, remainingCount: Array.isArray(queuedRecords) ? queuedRecords.length : 0, syncedIds: [] };
  }
  const items = Array.isArray(queuedRecords) ? [...queuedRecords] : await getQueuedBookings();
  if (!items.length) {
    return { syncedCount: 0, remainingCount: 0, syncedIds: [] };
  }

  let syncedCount = 0;
  const syncedIds = [];
  for (const item of items) {
    try {
      await createBookingFn(item.payload);
      await removeFn(item.id);
      syncedCount += 1;
      syncedIds.push(item.id);
    } catch (err) {
      if (isLikelyOfflineError(err, isOnline)) {
        break;
      }
      // Keep item queued if sign-in is required so it succeeds right after authentication
    }
  }
  return {
    syncedCount,
    remainingCount: Math.max(0, items.length - syncedCount),
    syncedIds
  };
}

/**
 * Renders the Offline Sync Queue panel inside the My Bookings card.
 */
export function renderOfflineQueueHtml({ queuedBookings, isOnline, t, escapeHtml }) {
  if (!Array.isArray(queuedBookings) || queuedBookings.length === 0) {
    return "";
  }
  const count = queuedBookings.length;
  const statusCopy = isOnline
    ? t("offlineQueueSyncReady")
    : t("offlineQueueWaitingNet");

  const itemsHtml = queuedBookings
    .map((item) => {
      const p = item.payload || {};
      const notesHtml = p.notes
        ? `<div class="booking-card-notes">${escapeHtml(p.notes)}</div>`
        : "";
      return `
      <article class="offline-queue-item" data-offline-booking-id="${escapeHtml(item.id)}">
        <div class="booking-card-header">
          <strong>${escapeHtml(p.serviceType || "Studio Shoot")}</strong>
          <span class="offline-sync-pill">${escapeHtml(t("offlineQueueBadge"))}</span>
        </div>
        <div class="booking-card-meta">
          ${escapeHtml(p.preferredDate || "")} • ${escapeHtml(p.preferredTime || "")} • ${escapeHtml(p.location || "")}
        </div>
        ${notesHtml}
        <div class="offline-queue-actions">
          <button type="button" class="btn-outline btn-sm" data-remove-offline-booking="${escapeHtml(item.id)}">
            ${escapeHtml(t("offlineQueueRemoveBtn"))}
          </button>
        </div>
      </article>`;
    })
    .join("");

  return `
    <div class="offline-queue-panel" role="status" aria-live="polite">
      <div class="offline-queue-header">
        <div>
          <strong>${escapeHtml(t("offlineQueueTitle"))} (${count})</strong>
          <p>${escapeHtml(statusCopy)}</p>
        </div>
        ${
          isOnline
            ? `<button type="button" class="btn-gold btn-sm" data-sync-offline-bookings="true">${escapeHtml(t("offlineQueueSyncNowBtn"))}</button>`
            : `<span class="offline-net-chip">${escapeHtml(t("offlineModeChip"))}</span>`
        }
      </div>
      <div class="offline-queue-list">${itemsHtml}</div>
    </div>
  `;
}
