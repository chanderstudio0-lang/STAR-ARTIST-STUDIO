const CACHE_VERSION = "star-artist-pwa-v11";
const SHELL_CACHE = `${CACHE_VERSION}-shell`;
const MEDIA_CACHE = `${CACHE_VERSION}-media`;

const APP_SHELL_URLS = [
  "/",
  "/index.html",
  "/styles.css",
  "/manifest.webmanifest",
  "/js/admin-models.js",
  "/js/app.js",
  "/js/booking-status.js",
  "/js/catalog-reviews.js",
  "/js/data.js",
  "/js/firebase.js",
  "/js/i18n.js",
  "/js/lazy-images.js",
  "/js/newsletter.js",
  "/js/tier-info.js",
  "/icons/icon-192.png",
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
  "identitytoolkit.googleapis.com",
  "securetoken.googleapis.com",
  "www.googleapis.com",
  "firebase.googleapis.com",
  "firebaseinstallations.googleapis.com",
  "content-firebaseappcheck.googleapis.com",
  "www.gstatic.com",
  "www.google.com"
];

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
  );
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
      fetch(request)
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

  // Cache-first for static studio shell (index.html, styles.css, js/app.js, modules) & local images
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
