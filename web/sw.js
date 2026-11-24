const CACHE_VERSION = "star-artist-pwa-v44";
const SHELL_CACHE = `${CACHE_VERSION}-shell`;
const MEDIA_CACHE = `${CACHE_VERSION}-media`;

const BOOKING_SYNC_TAG = "star-artist-sync-bookings";
const OFFLINE_DB_NAME = "star-artist-offline-sync-v1";
const OFFLINE_DB_VERSION = 1;
const OFFLINE_BOOKINGS_STORE = "pending_bookings";
const DEFAULT_PROJECT_ID = "gen-lang-client-0548683163";
const DEFAULT_DATABASE_ID = "ai-studio-android-stararti-4ffde0d6-02fd-4194-8e31-3359a6b9588b";

const APP_SHELL_URLS = [
  "/",
  "/index.html",
  "/styles.css",
  "/manifest.webmanifest",
  "/manifest.json",
  "/favicon.ico",
  "/firebase-config.json",
  "/js/admin-booking-tracker.js",
  "/js/admin-bookings-export.js",
  "/js/admin-email-preview.js",
  "/js/admin-guard.js",
  "/js/admin-model-experience.js",
  "/js/admin-model-growth.js",
  "/js/admin-models.js",
  "/js/app.js",
  "/js/booking-status.js",
  "/js/catalog-reviews.js",
  "/js/company-booking.js",
  "/js/data.js",
  "/js/exif-reader.js",
  "/js/firebase.js",
  "/js/gallery-share.js",
  "/js/i18n.js",
  "/js/image-upload.js",
  "/js/lazy-images.js",
  "/js/model-discovery.js",
  "/js/model-portal.js",
  "/js/newsletter.js",
  "/js/offline-sync.js",
  "/js/testimonials.js",
  "/js/tier-info.js",
  "/js/wedding-packages.js",
  "/js/zip-builder.js",
  "/icons/favicon-16x16.png",
  "/icons/favicon-32x32.png",
  "/icons/favicon-48x48.png",
  "/icons/icon-192.png",
  "/icons/icon-maskable-192.png",
  "/icons/icon-512.png",
  "/icons/icon-maskable-512.png",
  "/icons/apple-touch-icon.png",
  "/images/hero-banner.jpg",
  "/images/model-portrait.jpg",
  "/images/gallery-fashion.jpg",
  "/images/gallery-portrait.jpg",
  "/images/gallery-bridal.jpg",
  "/images/gallery-commercial.jpg"
];

const BYPASS_ORIGINS = [
  "firestore.googleapis.com",
  "firebasestorage.googleapis.com",
  "identitytoolkit.googleapis.com",
  "securetoken.googleapis.com",
  "www.googleapis.com",
  "firebase.googleapis.com",
  "firebaseinstallations.googleapis.com",
  "content-firebaseappcheck.googleapis.com",
  "www.gstatic.com",
  "www.google.com",
  "esm.sh"
];

// ============================================================================
// IndexedDB Offline Booking Queue Helpers (Shared with /js/offline-sync.js)
// ============================================================================

function openOfflineSyncDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(OFFLINE_DB_NAME, OFFLINE_DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(OFFLINE_BOOKINGS_STORE)) {
        db.createObjectStore(OFFLINE_BOOKINGS_STORE, { keyPath: "id" });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error || new Error("Unable to open offline sync IndexedDB."));
  });
}

async function readAllPendingBookings() {
  const db = await openOfflineSyncDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(OFFLINE_BOOKINGS_STORE, "readonly");
    const req = tx.objectStore(OFFLINE_BOOKINGS_STORE).getAll();
    req.onsuccess = () => {
      db.close();
      resolve(Array.isArray(req.result) ? req.result : []);
    };
    req.onerror = () => {
      db.close();
      reject(req.error || new Error("Unable to read pending bookings."));
    };
  });
}

async function putPendingBooking(record) {
  const db = await openOfflineSyncDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(OFFLINE_BOOKINGS_STORE, "readwrite");
    tx.objectStore(OFFLINE_BOOKINGS_STORE).put(record);
    tx.oncomplete = () => {
      db.close();
      resolve(record);
    };
    tx.onerror = () => {
      db.close();
      reject(tx.error || new Error("Unable to persist pending booking."));
    };
  });
}

async function deletePendingBooking(id) {
  const db = await openOfflineSyncDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(OFFLINE_BOOKINGS_STORE, "readwrite");
    tx.objectStore(OFFLINE_BOOKINGS_STORE).delete(id);
    tx.oncomplete = () => {
      db.close();
      resolve(true);
    };
    tx.onerror = () => {
      db.close();
      reject(tx.error || new Error("Unable to remove synced booking."));
    };
  });
}

function buildFirestoreBookingDocument(record) {
  const p = record?.payload || {};
  const nowIso = new Date().toISOString();
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
      createdAt: { timestampValue: nowIso },
      updatedAt: { timestampValue: nowIso }
    }
  };
}

async function pushPendingBookingToFirestore(record) {
  const projectId = encodeURIComponent(String(record?.projectId || DEFAULT_PROJECT_ID).trim());
  const databaseId = encodeURIComponent(String(record?.databaseId || DEFAULT_DATABASE_ID).trim());
  const authToken = String(record?.authToken || "").trim();

  if (!authToken || !String(record?.payload?.userId || record?.userId || "").trim()) {
    return { ok: false, retryable: false, needsReauth: true, status: 401 };
  }

  const endpoint = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/${databaseId}/documents/bookings`;
  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${authToken}`
    },
    body: JSON.stringify(buildFirestoreBookingDocument(record))
  });

  if (response.ok) {
    return { ok: true, retryable: false, needsReauth: false, status: response.status };
  }

  if (response.status === 401 || response.status === 403) {
    return { ok: false, retryable: false, needsReauth: true, status: response.status };
  }

  // 5xx or 429 are transient and should be retried by the Background Sync scheduler
  const retryable = response.status >= 500 || response.status === 429 || response.status === 408;
  return { ok: false, retryable, needsReauth: false, status: response.status };
}

async function notifyClients(message) {
  const windowClients = await self.clients.matchAll({
    includeUncontrolled: true,
    type: "window"
  });
  for (const client of windowClients) {
    client.postMessage(message);
  }
}

async function syncPendingBookingsInBackground() {
  const pending = await readAllPendingBookings();
  if (!pending.length) {
    return;
  }

  const syncedIds = [];
  let retryableFailures = 0;
  let needsReauthCount = 0;

  for (const record of pending) {
    try {
      const result = await pushPendingBookingToFirestore(record);
      if (result.ok) {
        await deletePendingBooking(record.id);
        syncedIds.push(record.id);
      } else if (result.needsReauth) {
        needsReauthCount += 1;
        await putPendingBooking({
          ...record,
          attempts: (record.attempts || 0) + 1,
          needsReauth: true,
          lastError: `HTTP ${result.status}`
        });
      } else if (result.retryable) {
        retryableFailures += 1;
        await putPendingBooking({
          ...record,
          attempts: (record.attempts || 0) + 1,
          lastError: `HTTP ${result.status}`
        });
      } else {
        await putPendingBooking({
          ...record,
          attempts: (record.attempts || 0) + 1,
          lastError: `HTTP ${result.status}`
        });
      }
    } catch (err) {
      // Network is still unreachable — let Background Sync retry automatically
      retryableFailures += 1;
      await putPendingBooking({
        ...record,
        attempts: (record.attempts || 0) + 1,
        lastError: String(err?.message || "Network unavailable")
      });
    }
  }

  const remaining = await readAllPendingBookings();

  if (syncedIds.length > 0) {
    await notifyClients({
      type: "BOOKING_SYNC_COMPLETE",
      syncedCount: syncedIds.length,
      syncedIds,
      remainingCount: remaining.length
    });
  }

  if (needsReauthCount > 0) {
    await notifyClients({
      type: "BOOKING_SYNC_REAUTH_REQUIRED",
      remainingCount: remaining.length
    });
  }

  // Throwing rejects event.waitUntil(), telling the browser to schedule an automatic retry
  if (retryableFailures > 0) {
    throw new Error(`Background Sync deferred: ${retryableFailures} booking(s) still awaiting connection.`);
  }
}

// ============================================================================
// Service Worker Lifecycle, Caching & Background Sync Listeners
// ============================================================================

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches
      .open(SHELL_CACHE)
      .then((cache) => cache.addAll(APP_SHELL_URLS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((key) => !key.startsWith(CACHE_VERSION))
            .map((key) => caches.delete(key))
        )
      )
      .then(() => self.clients.claim())
      .then(() => notifyClients({ type: "SW_UPDATED", version: CACHE_VERSION }))
  );
});

self.addEventListener("sync", (event) => {
  if (event.tag === BOOKING_SYNC_TAG) {
    event.waitUntil(syncPendingBookingsInBackground());
  }
});

self.addEventListener("message", (event) => {
  const data = event.data || {};
  if (data.type === "SKIP_WAITING") {
    self.skipWaiting();
    return;
  }
  if (data.type === "ENQUEUE_OFFLINE_BOOKING" && data.record) {
    event.waitUntil(putPendingBooking(data.record));
    return;
  }
  if (data.type === "TRIGGER_BOOKING_SYNC") {
    event.waitUntil(
      syncPendingBookingsInBackground().catch(() => {
        // Ignore transient failure on manual message trigger; Background Sync will retry
      })
    );
  }
});

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;

  const url = new URL(request.url);

  // Never intercept Firebase Auth, Firestore, App Check, or Firebase Hosting init endpoints
  if (
    BYPASS_ORIGINS.some((host) => url.hostname.endsWith(host)) ||
    url.pathname.startsWith("/__/")
  ) {
    return;
  }

  // Only handle same-origin HTTP(S) assets
  if (url.origin !== self.location.origin) {
    return;
  }

  // Network-first with cached shell fallback for HTML navigations
  if (request.mode === "navigate") {
    event.respondWith(
      fetch(request, { cache: "no-store" })
        .then((response) => {
          if (response && response.ok) {
            const copy = response.clone();
            caches.open(SHELL_CACHE).then((cache) => cache.put("/index.html", copy));
          }
          return response;
        })
        .catch(async () => (await caches.match("/index.html")) || (await caches.match("/")))
    );
    return;
  }

  // Network-first with cache fallback for JS, CSS, manifest, and shared Firebase config so mobile browsers
  // always receive the latest production build immediately without stale cached popups or scripts.
  const isFreshCodeAsset =
    url.pathname === "/index.html" ||
    url.pathname === "/styles.css" ||
    url.pathname === "/firebase-config.json" ||
    url.pathname === "/manifest.webmanifest" ||
    url.pathname === "/manifest.json" ||
    url.pathname === "/favicon.ico" ||
    url.pathname.startsWith("/icons/") ||
    url.pathname.startsWith("/js/");

  if (isFreshCodeAsset) {
    event.respondWith(
      fetch(request, { cache: "no-cache" })
        .then((response) => {
          if (response && response.ok && response.type === "basic") {
            const copy = response.clone();
            caches.open(SHELL_CACHE).then((cache) => cache.put(request, copy));
          }
          return response;
        })
        .catch(() => caches.match(request, { ignoreSearch: true }))
    );
    return;
  }

  // Cache-first for static studio images & icons
  event.respondWith(
    caches.match(request, { ignoreSearch: true }).then((cached) => {
      if (cached) {
        return cached;
      }
      return fetch(request)
        .then((response) => {
          if (response && response.ok && response.type === "basic") {
            const targetCache = url.pathname.startsWith("/images/") ? MEDIA_CACHE : SHELL_CACHE;
            const copy = response.clone();
            caches.open(targetCache).then((cache) => cache.put(request, copy));
          }
          return response;
        })
        .catch(() => caches.match(request));
    })
  );
});
