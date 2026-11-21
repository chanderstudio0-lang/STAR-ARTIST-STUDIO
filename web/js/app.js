import { TRANSLATIONS } from "./i18n.js";
import { STUDIO_CONTACT, STUDIO_SERVICES, SHOWCASE_GALLERY_SEEDS, FIREBASE_PROJECT_DEFAULTS } from "./data.js";
import {
  getBookingStatusMeta,
  summarizeBookingStatuses,
  filterBookingsByStatus,
  renderBookingStatusBadgeHtml
} from "./booking-status.js";
import {
  catalogReviewBookingId,
  validateCatalogReview,
  renderCatalogFeedbackHtml
} from "./catalog-reviews.js";
import {
  validateManualModelVerification,
  renderAdminModelsSectionHtml
} from "./admin-models.js";
import {
  isAuthorizedAdminUser,
  isAdminUrlAttempt,
  extractRequestedRoute,
  resolveSafeRoute,
  syncAdminUiVisibility
} from "./admin-guard.js";
import { renderCatalogTierSectionHtml } from "./tier-info.js";
import {
  renderLazyBlurImageHtml,
  initLazyBlurImages
} from "./lazy-images.js";
import {
  evaluateNewsletterEmailInput,
  loadNewsletterSubscribers,
  saveNewsletterSubscription,
  showTemporaryNewsletterSuccess
} from "./newsletter.js";
import {
  buildCarouselTestimonials,
  getNextSlideIndex,
  renderTestimonialCarouselHtml
} from "./testimonials.js";
import {
  validateOfflineBookingPayload,
  createQueuedBookingRecord,
  isLikelyOfflineError,
  enqueueOfflineBooking,
  getQueuedBookings,
  removeQueuedBooking,
  registerBookingBackgroundSync,
  flushQueuedBookingsFromClient,
  renderOfflineQueueHtml
} from "./offline-sync.js";
import {
  countCatalogByCategory,
  countGalleryByCategory,
  shareGalleryImage,
  downloadGalleryImage,
  downloadGalleryFolderAsZip,
  applyLightboxFilterToElement,
  getVisibleGalleryItems,
  getNextLightboxIndex,
  formatLightboxSlideCounter,
  resolveLightboxKeyAction,
  createLightboxSlideshowController,
  applyLightboxZoomTransform,
  createLightboxZoomController,
  LIGHTBOX_MAX_ZOOM,
  renderLightboxMetadataHtml
} from "./gallery-share.js";
import {
  validateGallerySubmission,
  suggestTitleFromFileName,
  encodeImageFileToStudioPayloads,
  createUploadProgressController
} from "./image-upload.js";
import {
  readExifFromImageUrl,
  hasMeaningfulExif,
  sanitizeExifMetadata
} from "./exif-reader.js";
import {
  subscribeAuthState,
  signInWithGooglePopup,
  signInWithEmailPassword,
  registerWithEmailPassword,
  signOutFirebase,
  fetchApprovedGallery,
  fetchMyBookings,
  createShootBooking,
  getBookingSyncContext,
  cancelMyBooking,
  fetchMyModels,
  submitModelRegistration,
  submitGalleryUpload,
  uploadGalleryImageToStorage,
  fetchServiceReviews,
  submitCatalogReview,
  adminFetchDashboardData,
  adminUpdateBookingStatus,
  adminDeleteBooking,
  adminUpdateModelStatus,
  adminUpdateGalleryStatus,
  adminDeleteGalleryItem,
  adminPublishAnnouncement,
  saveManualWebConfig,
  initFirebaseClient,
  syncWorkshopNewsletterOptIn
} from "./firebase.js";

const state = {
  lang: localStorage.getItem("star_artist_lang") === "hi" ? "hi" : "en",
  theme: localStorage.getItem("star_artist_theme") === "light" ? "light" : "dark",
  route: "home",
  user: null,
  isAdmin: false,
  authResolved: false,
  pendingAdminRoute: false,
  firebaseReady: false,
  deferredInstallPrompt: null,
  catalogCategory: "all",
  catalogSearch: "",
  selectedTiers: {},
  bookingStatusFilter: "all",
  galleryFilter: "All",
  adminTab: "bookings",
  adminModelFilter: "pending",
  adminModelSearch: "",
  adminModelDrafts: {},
  galleryItems: [...SHOWCASE_GALLERY_SEEDS],
  myBookings: [],
  queuedOfflineBookings: [],
  myModels: [],
  reviews: [],
  expandedCatalogReviews: {},
  expandedTierGuides: {},
  catalogReviewDrafts: {},
  isSubmittingReview: false,
  testimonialIndex: 0,
  testimonialAutoplay: true,
  testimonialTimer: null,
  activeLightboxItem: null,
  activeLightboxItems: [],
  activeLightboxFolder: "All",
  activeLightboxIndex: -1,
  activeLightboxFilter: "original",
  lightboxSlideshowPlaying: false,
  isPackingGalleryZip: false,
  exifCache: {},
  uploadedGalleryMedia: null,
  isEncodingGalleryImage: false,
  adminData: { bookings: [], models: [], gallery: [], announcements: [] }
};

function showToast(msg) {
  const el = document.getElementById("toast");
  if (!el) return;
  el.textContent = msg;
  el.classList.remove("hidden");
  clearTimeout(showToast._timer);
  showToast._timer = setTimeout(() => el.classList.add("hidden"), 3400);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function t(key) {
  return TRANSLATIONS[state.lang]?.[key] || TRANSLATIONS.en[key] || key;
}

function applyTranslations() {
  document.documentElement.lang = state.lang;
  document.querySelectorAll("[data-i18n]").forEach((el) => {
    const key = el.getAttribute("data-i18n");
    if (key && TRANSLATIONS[state.lang]?.[key]) {
      el.textContent = TRANSLATIONS[state.lang][key];
    }
  });
  const searchInput = document.getElementById("catalogSearchInput");
  if (searchInput) searchInput.placeholder = t("catalogSearchPlaceholder");

  const newsletterInput = document.getElementById("newsletterEmail");
  if (newsletterInput) newsletterInput.placeholder = t("newsletterEmailPlaceholder");

  const themeBtn = document.getElementById("themeToggleBtn");
  if (themeBtn) themeBtn.textContent = state.theme === "dark" ? t("themeLight") : t("themeDark");

  const authBtn = document.getElementById("authActionBtn");
  if (authBtn) authBtn.textContent = state.user ? t("signOutBtn") : t("signInBtn");

  if (state.activeLightboxItem) {
    const exifEl = document.getElementById("lightboxExifPanel");
    if (exifEl) {
      const cacheKey = state.activeLightboxItem.id || state.activeLightboxItem.mediaUrl || "";
      const cached = state.exifCache[cacheKey];
      const mergedItem = cached?.hasExif
        ? { ...state.activeLightboxItem, metadata: { ...cached, ...(state.activeLightboxItem.metadata || {}) } }
        : state.activeLightboxItem;
      exifEl.innerHTML = renderLightboxMetadataHtml(mergedItem, {
        lang: state.lang,
        t,
        escapeHtml,
        autoExtracted: Boolean(cached?.hasExif)
      });
    }
  }
}

function applyTheme() {
  document.documentElement.setAttribute("data-theme", state.theme);
  const metaTheme = document.querySelector('meta[name="theme-color"]');
  if (metaTheme) {
    metaTheme.setAttribute("content", state.theme === "light" ? "#F8F9FA" : "#0C0C0C");
  }
}

function hasVerifiedAdminSession() {
  return Boolean(state.isAdmin && isAuthorizedAdminUser(state.user));
}

function openAuthModal({ adminNotice = false } = {}) {
  const hint = document.getElementById("authAdminHint");
  if (hint) {
    hint.classList.toggle("hidden", !adminNotice);
  }
  document.getElementById("authModal")?.classList.remove("hidden");
}

function closeAuthModal() {
  document.getElementById("authAdminHint")?.classList.add("hidden");
  document.getElementById("authModal")?.classList.add("hidden");
}

function navigateTo(route, { silentBlock = false } = {}) {
  const validRoutes = ["home", "catalog", "booking", "models", "gallery", "admin"];
  const isAdminNow = hasVerifiedAdminSession();
  const resolution = resolveSafeRoute({
    requestedRoute: route,
    pathname: window.location.pathname,
    hash: window.location.hash,
    search: window.location.search,
    isAdmin: isAdminNow
  });

  if (resolution.blockedAdminAttempt) {
    syncAdminUiVisibility(document, { isAdmin: false });
    if (resolution.shouldNormalizeUrl) {
      history.replaceState(null, "", "/#home");
    }
    if (!state.authResolved) {
      state.pendingAdminRoute = true;
    } else if (!silentBlock) {
      if (state.user) {
        showToast(t("adminAccessDeniedToast"));
      } else {
        state.pendingAdminRoute = true;
        showToast(t("adminSignInRequiredToast"));
        if (state.firebaseReady) {
          openAuthModal({ adminNotice: true });
        } else {
          document.getElementById("firebaseSetupModal")?.classList.remove("hidden");
        }
      }
    }
  }

  const target = resolution.route;
  state.route = target;
  if (resolution.shouldNormalizeUrl || window.location.hash !== `#${target}`) {
    history.replaceState(null, "", `/#${target}`);
  }

  validRoutes.forEach((r) => {
    const sec = document.getElementById(`view-${r}`);
    if (!sec) return;
    const show = r === target && (r !== "admin" || isAdminNow);
    sec.classList.toggle("hidden", !show);
    sec.hidden = !show;
    if (r === "admin") {
      sec.setAttribute("aria-hidden", show ? "false" : "true");
    }
  });

  document.querySelectorAll("[data-route]").forEach((btn) => {
    btn.classList.toggle("active", btn.getAttribute("data-route") === target);
  });

  if (target === "admin" && isAdminNow) {
    loadAdminConsole();
  }
}

function renderServiceCards(containerId, services) {
  const container = document.getElementById(containerId);
  if (!container) return;

  if (!services.length) {
    container.innerHTML = `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">No studio packages match this filter.</p></div>`;
    return;
  }

  const interactiveFeedback = containerId === "catalogGrid";

  container.innerHTML = services
    .map((pkg) => {
      const selectedTierId =
        state.selectedTiers[pkg.id] ||
        pkg.tiers.find((x) => x.isRecommended)?.id ||
        pkg.tiers[0].id;
      const tier = pkg.tiers.find((x) => x.id === selectedTierId) || pkg.tiers[0];
      const title = state.lang === "hi" ? pkg.titleHi : pkg.titleEn;
      const summary = state.lang === "hi" ? pkg.summaryHi : pkg.summaryEn;
      const tag = state.lang === "hi" ? pkg.tagHi : pkg.tagEn;

      const targetBookingId = catalogReviewBookingId(pkg.id).toLowerCase();
      const existingReview = state.user
        ? state.reviews.find(
            (r) =>
              r.userId === state.user.uid &&
              String(r.bookingId || "").trim().toLowerCase() === targetBookingId
          ) || null
        : null;
      const draft = state.catalogReviewDrafts[pkg.id] || {};
      const feedbackHtml = renderCatalogFeedbackHtml({
        pkg,
        reviews: state.reviews,
        existingReview,
        isExpanded: Boolean(state.expandedCatalogReviews[pkg.id]),
        draftRating: draft.rating ?? null,
        draftComment: draft.comment ?? null,
        isSubmitting: state.isSubmittingReview,
        lang: state.lang,
        t,
        interactive: interactiveFeedback
      });

      return `
      <article class="studio-card" data-pkg-card="${escapeHtml(pkg.id)}">
        ${
          pkg.imageUrl
            ? renderLazyBlurImageHtml({
                src: pkg.imageUrl,
                alt: `${title} — STAR ARTIST STUDIO`,
                aspectClass: "is-catalog-cover",
                escapeHtml
              })
            : ""
        }
        <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;">
          <span class="badge">${escapeHtml(tag)}</span>
          <strong style="color:var(--gold);font-size:1.15rem;">${escapeHtml(tier.priceFormatted)}</strong>
        </div>
        <h3 style="margin:0;color:var(--text-primary);">${escapeHtml(title)}</h3>
        <p style="margin:0;font-size:0.88rem;color:var(--text-secondary);">${escapeHtml(summary)}</p>
        ${renderCatalogTierSectionHtml({
          pkg,
          activeTier: tier,
          isGuideExpanded: Boolean(state.expandedTierGuides[pkg.id]),
          lang: state.lang,
          t,
          escapeHtml
        })}
        <div style="font-size:0.8rem;color:var(--text-muted);">${escapeHtml(tier.durationEn)} • ${escapeHtml(tier.turnaroundEn)}</div>
        <ul style="margin:0;padding-left:18px;font-size:0.84rem;color:var(--text-secondary);">
          ${tier.deliverablesEn.map((d) => `<li>${escapeHtml(d)}</li>`).join("")}
        </ul>
        ${feedbackHtml}
        <button type="button" class="btn-gold" style="margin-top:auto;" data-book-service="${escapeHtml(`${pkg.recommendedServiceType} - ${tier.tierNameEn}`)}">
          ${escapeHtml(t("bookTierBtn"))}
        </button>
      </article>`;
    })
    .join("");

  initLazyBlurImages(container);
}

function renderCatalog() {
  const q = state.catalogSearch.trim().toLowerCase();
  const catCounts = countCatalogByCategory(STUDIO_SERVICES, q);
  document.querySelectorAll("[data-cat-count]").forEach((badge) => {
    const key = badge.getAttribute("data-cat-count");
    if (key && Object.prototype.hasOwnProperty.call(catCounts, key)) {
      badge.textContent = String(catCounts[key]);
      badge.setAttribute("aria-label", `${catCounts[key]} items`);
    }
  });

  const filtered = STUDIO_SERVICES.filter((pkg) => {
    const catOk = state.catalogCategory === "all" || pkg.category === state.catalogCategory;
    const text = `${pkg.titleEn} ${pkg.titleHi} ${pkg.summaryEn} ${pkg.tagEn} ${pkg.recommendedServiceType}`.toLowerCase();
    return catOk && (!q || text.includes(q));
  });
  renderServiceCards("homeFeaturedServicesGrid", STUDIO_SERVICES.filter((p) => p.isPopular).slice(0, 3));
  renderServiceCards("catalogGrid", filtered);
}

function renderGallery() {
  const categories = ["All", "Fashion", "Editorial", "Portrait", "Bridal", "Commercial", "Video Reel"];
  const galCounts = countGalleryByCategory(state.galleryItems, categories);
  const filterBar = document.getElementById("galleryFilterBar");
  if (filterBar) {
    filterBar.innerHTML = categories
      .map((cat) => {
        const count = galCounts[cat] ?? 0;
        return `<button type="button" class="chip-btn ${state.galleryFilter === cat ? "active" : ""}" data-gal-cat="${escapeHtml(cat)}"><span>${escapeHtml(cat)}</span><span class="filter-count-badge" data-gal-count="${escapeHtml(cat)}" aria-label="${count} items">${count}</span></button>`;
      })
      .join("");
  }

  const visible = state.galleryItems.filter(
    (item) => state.galleryFilter === "All" || item.category === state.galleryFilter
  );

  const renderGrid = (elId, list) => {
    const el = document.getElementById(elId);
    if (!el) return;
    el.innerHTML = list
      .map(
        (item) => `
      <article class="studio-card gallery-card" data-lightbox-id="${escapeHtml(item.id)}">
        ${renderLazyBlurImageHtml({
          src: item.thumbnailUrl || item.mediaUrl,
          alt: item.title,
          aspectClass: "is-gallery-cover",
          escapeHtml
        })}
        <div class="gallery-card-body">
          <div>
            <strong style="display:block;">${escapeHtml(item.title)}</strong>
            <span style="font-size:0.8rem;color:var(--text-secondary);">${escapeHtml(item.category)} • ${escapeHtml(item.mediaType || "photo")}</span>
          </div>
          <span class="badge">${item.status === "approved" ? "Approved" : escapeHtml(item.status || "Showcase")}</span>
        </div>
      </article>`
      )
      .join("");
    initLazyBlurImages(el);
  };

  renderGrid("homeShowcaseGrid", state.galleryItems.slice(0, 3));
  renderGrid("galleryGrid", visible);
}

function getActiveTestimonials() {
  return buildCarouselTestimonials(state.reviews, state.lang);
}

function renderTestimonialCarousel() {
  const mount = document.getElementById("homeTestimonialCarousel");
  if (!mount) return;
  const items = getActiveTestimonials();
  if (!items.length) return;
  state.testimonialIndex = getNextSlideIndex(state.testimonialIndex, items.length, 0);
  mount.innerHTML = renderTestimonialCarouselHtml({
    items,
    activeIndex: state.testimonialIndex,
    autoplay: state.testimonialAutoplay,
    t,
    escapeHtml
  });
}

function stepTestimonialCarousel(delta) {
  const items = getActiveTestimonials();
  if (!items.length) return;
  state.testimonialIndex = getNextSlideIndex(state.testimonialIndex, items.length, delta);
  renderTestimonialCarousel();
}

function syncTestimonialAutoplayTimer() {
  if (state.testimonialTimer) {
    clearInterval(state.testimonialTimer);
    state.testimonialTimer = null;
  }
  const prefersReduced =
    typeof window !== "undefined" &&
    typeof window.matchMedia === "function" &&
    window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (!state.testimonialAutoplay || prefersReduced) return;
  state.testimonialTimer = setInterval(() => {
    if (state.route === "home" && !document.hidden) {
      stepTestimonialCarousel(1);
    }
  }, 6000);
}

function renderOfflineBookingQueue() {
  const box = document.getElementById("offlineBookingsQueueBox");
  if (!box) return;
  box.innerHTML = renderOfflineQueueHtml({
    queuedBookings: state.queuedOfflineBookings,
    isOnline: typeof navigator === "undefined" ? true : navigator.onLine,
    t,
    escapeHtml
  });
}

async function refreshOfflineBookingQueue() {
  state.queuedOfflineBookings = await getQueuedBookings();
  renderOfflineBookingQueue();
}

async function syncQueuedBookings({ notifyIfNone = false } = {}) {
  await refreshOfflineBookingQueue();
  if (!state.queuedOfflineBookings.length) {
    if (notifyIfNone) {
      showToast("All bookings are already synced.");
    }
    return;
  }

  if (typeof navigator !== "undefined" && !navigator.onLine) {
    showToast(t("offlineQueueWaitingNet"));
    return;
  }

  // Trigger Service Worker Background Sync if available
  await registerBookingBackgroundSync();

  // Also flush via Firebase SDK when authenticated online (handles Safari/Firefox & refreshed tokens)
  if (state.user && state.firebaseReady) {
    const result = await flushQueuedBookingsFromClient({
      createBookingFn: createShootBooking,
      isOnline: typeof navigator === "undefined" ? true : navigator.onLine
    });
    await refreshOfflineBookingQueue();
    if (result.syncedCount > 0) {
      showToast(t("offlineBookingSyncedToast"));
      await refreshCloudData();
    }
  } else if (notifyIfNone) {
    showToast(t("offlineBookingReauthToast"));
    if (state.firebaseReady) {
      document.getElementById("authModal")?.classList.remove("hidden");
    }
  }
}

function renderMyBookings() {
  renderOfflineBookingQueue();
  const el = document.getElementById("myBookingsList");
  const statusBar = document.getElementById("myBookingsStatusBar");
  if (!el) return;
  if (!state.user) {
    statusBar?.classList.add("hidden");
    el.innerHTML = `<p style="color:var(--text-secondary);margin:0;">${escapeHtml(t("authRequiredNotice"))}</p>`;
    return;
  }
  if (!state.myBookings.length) {
    statusBar?.classList.add("hidden");
    el.innerHTML = `<p style="color:var(--text-secondary);margin:0;">No shoot bookings found for this account yet.</p>`;
    return;
  }

  if (statusBar) {
    const counts = summarizeBookingStatuses(state.myBookings);
    const pills = [
      { key: "all", label: t("statusAll"), count: counts.total },
      { key: "confirmed", label: t("statusConfirmed"), count: counts.confirmed },
      { key: "pending", label: t("statusPending"), count: counts.pending },
      { key: "completed", label: t("statusCompleted"), count: counts.completed },
      { key: "cancelled", label: t("statusCancelled"), count: counts.cancelled }
    ];
    statusBar.classList.remove("hidden");
    statusBar.innerHTML = pills
      .map(
        (pill) => `
      <button
        type="button"
        role="tab"
        aria-selected="${state.bookingStatusFilter === pill.key ? "true" : "false"}"
        class="booking-status-pill ${state.bookingStatusFilter === pill.key ? "active" : ""}"
        data-booking-status-filter="${pill.key}"
      >
        <span>${escapeHtml(pill.label)}</span>
        <span class="pill-count">${pill.count}</span>
      </button>`
      )
      .join("");
  }

  const visibleBookings = filterBookingsByStatus(state.myBookings, state.bookingStatusFilter);
  if (!visibleBookings.length) {
    el.innerHTML = `<p style="color:var(--text-secondary);margin:0;">${escapeHtml(t("emptyFilteredBookings"))}</p>`;
    return;
  }

  el.innerHTML = visibleBookings
    .map((b) => {
      const statusMeta = getBookingStatusMeta(b.status, state.lang);
      const badgeHtml = renderBookingStatusBadgeHtml(b.status, state.lang);
      const canCancel = statusMeta.key === "pending";
      const notesBlock = b.notes
        ? `<div class="booking-card-notes">${escapeHtml(b.notes)}</div>`
        : "";
      return `
      <article class="studio-card booking-item-card ${statusMeta.cardClass}" data-booking-id="${escapeHtml(b.id)}">
        <div class="booking-card-header">
          <strong>${escapeHtml(b.serviceType)}</strong>
          ${badgeHtml}
        </div>
        <div class="booking-card-meta">
          ${escapeHtml(b.preferredDate)} • ${escapeHtml(b.preferredTime || "")} • ${escapeHtml(b.location || "")}
        </div>
        ${notesBlock}
        ${
          canCancel
            ? `<div><button type="button" class="btn-danger" data-cancel-booking="${escapeHtml(b.id)}">${escapeHtml(t("cancelBookingBtn"))}</button></div>`
            : ""
        }
      </article>`;
    })
    .join("");
}

function renderMyModels() {
  const el = document.getElementById("myModelsList");
  if (!el) return;
  if (!state.user) {
    el.innerHTML = `<p style="color:var(--text-secondary);margin:0;">${escapeHtml(t("authRequiredNotice"))}</p>`;
    return;
  }
  if (!state.myModels.length) {
    el.innerHTML = `<p style="color:var(--text-secondary);margin:0;">No 18+ model applications submitted yet.</p>`;
    return;
  }
  el.innerHTML = state.myModels
    .map((m) => {
      const normalized = String(m.status || "pending").toLowerCase();
      const statusCls = `status-${normalized}`;
      const statusLabel =
        normalized === "approved"
          ? t("rosterBadge")
          : normalized === "rejected"
          ? "Rejected"
          : "Pending Review";
      const adminNoteHtml = m.adminNotes
        ? `<div class="booking-card-notes"><strong>Studio Admin Note:</strong> ${escapeHtml(m.adminNotes)}</div>`
        : "";
      return `
      <div class="studio-card" style="padding:14px;gap:8px;">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;">
          <strong>${escapeHtml(m.fullName)} (${escapeHtml(m.age)} yrs)</strong>
          <span class="badge ${statusCls}">${escapeHtml(statusLabel)}</span>
        </div>
        <div style="font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(m.city)} • ${escapeHtml(m.skills || "")}</div>
        ${adminNoteHtml}
      </div>`;
    })
    .join("");
}

async function loadAdminConsole() {
  if (!hasVerifiedAdminSession()) {
    state.adminData = { bookings: [], models: [], gallery: [], announcements: [] };
    syncAdminUiVisibility(document, { isAdmin: false });
    if (state.route === "admin") {
      navigateTo("home", { silentBlock: true });
    }
    return;
  }
  syncAdminUiVisibility(document, { isAdmin: true, adminEmail: state.user?.email || "" });

  try {
    state.adminData = await adminFetchDashboardData();
    renderAdminConsole();
  } catch (err) {
    showToast(err.message || "Unable to load Admin Dashboard");
  }
}

function renderAdminConsole() {
  if (!hasVerifiedAdminSession()) {
    syncAdminUiVisibility(document, { isAdmin: false });
    return;
  }
  const stats = document.getElementById("adminStatsGrid");
  if (stats) {
    stats.innerHTML = `
      <div class="studio-card"><div class="stat-value">${state.adminData.bookings.length}</div><div class="stat-label">Total Bookings</div></div>
      <div class="studio-card"><div class="stat-value">${state.adminData.models.length}</div><div class="stat-label">18+ Models</div></div>
      <div class="studio-card"><div class="stat-value">${state.adminData.gallery.length}</div><div class="stat-label">Gallery Submissions</div></div>
      <div class="studio-card"><div class="stat-value">${state.adminData.announcements.length}</div><div class="stat-label">Announcements</div></div>`;
  }

  const adminCounts = {
    bookings: state.adminData.bookings.length,
    models: state.adminData.models.length,
    gallery: state.adminData.gallery.length,
    announcements: state.adminData.announcements.length
  };
  document.querySelectorAll("[data-admin-count]").forEach((badge) => {
    const key = badge.getAttribute("data-admin-count");
    if (key && Object.prototype.hasOwnProperty.call(adminCounts, key)) {
      badge.textContent = String(adminCounts[key]);
      badge.setAttribute("aria-label", `${adminCounts[key]} items`);
    }
  });

  const body = document.getElementById("adminTabBody");
  if (!body) return;

  if (state.adminTab === "bookings") {
    body.innerHTML = state.adminData.bookings
      .map(
        (b) => `
      <div class="studio-card">
        <div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:8px;">
          <strong>${escapeHtml(b.clientName)} — ${escapeHtml(b.serviceType)}</strong>
          <span class="badge status-${String(b.status || "pending").toLowerCase()}">${escapeHtml(b.status)}</span>
        </div>
        <div style="font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(b.preferredDate)} ${escapeHtml(b.preferredTime || "")} • ${escapeHtml(b.contactNumber)}</div>
        <div style="display:flex;flex-wrap:wrap;gap:8px;">
          <button type="button" class="chip-btn" data-adm-bk-id="${escapeHtml(b.id)}" data-adm-bk-st="Confirmed">Confirm</button>
          <button type="button" class="chip-btn" data-adm-bk-id="${escapeHtml(b.id)}" data-adm-bk-st="Completed">Complete</button>
          <button type="button" class="chip-btn" data-adm-bk-id="${escapeHtml(b.id)}" data-adm-bk-st="Cancelled">Cancel</button>
          <button type="button" class="btn-danger" data-adm-bk-del="${escapeHtml(b.id)}">Delete</button>
        </div>
      </div>`
      )
      .join("");
  } else if (state.adminTab === "models") {
    body.innerHTML = renderAdminModelsSectionHtml({
      models: state.adminData.models,
      filter: state.adminModelFilter,
      searchQuery: state.adminModelSearch,
      drafts: state.adminModelDrafts,
      lang: state.lang,
      t
    });
  } else if (state.adminTab === "gallery") {
    body.innerHTML = state.adminData.gallery
      .map(
        (g) => `
      <div class="studio-card">
        <div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:8px;">
          <strong>${escapeHtml(g.title)} (${escapeHtml(g.category)})</strong>
          <span class="badge status-${String(g.status || "pending").toLowerCase()}">${escapeHtml(g.status)}</span>
        </div>
        <div style="display:flex;gap:8px;flex-wrap:wrap;">
          <button type="button" class="chip-btn" data-adm-gal-id="${escapeHtml(g.id)}" data-adm-gal-st="approved">Approve</button>
          <button type="button" class="chip-btn" data-adm-gal-id="${escapeHtml(g.id)}" data-adm-gal-st="rejected">Reject</button>
          <button type="button" class="btn-danger" data-adm-gal-del="${escapeHtml(g.id)}">Delete</button>
        </div>
      </div>`
      )
      .join("");
  } else {
    body.innerHTML = `
      <form id="adminAnnouncementForm" class="studio-card form-grid">
        <div class="field">
          <label for="annTitle">Announcement Title</label>
          <input id="annTitle" class="input" required minlength="3" maxlength="140" />
        </div>
        <div class="field">
          <label for="annMessage">Message</label>
          <textarea id="annMessage" class="input" rows="3" required minlength="5" maxlength="2000"></textarea>
        </div>
        <button type="submit" class="btn-gold">${escapeHtml(t("publishAnnouncementBtn"))}</button>
      </form>
      ${state.adminData.announcements
        .map(
          (a) => `<div class="studio-card"><strong>${escapeHtml(a.title)}</strong><p style="margin:0;color:var(--text-secondary);">${escapeHtml(a.message)}</p></div>`
        )
        .join("")}`;
  }
}

async function refreshCloudData() {
  if (!state.firebaseReady) return;
  try {
    const cloudGallery = await fetchApprovedGallery();
    const seen = new Set(cloudGallery.map((g) => g.id));
    state.galleryItems = [
      ...cloudGallery,
      ...SHOWCASE_GALLERY_SEEDS.filter((seed) => !seen.has(seed.id))
    ];
    renderGallery();

    if (state.user) {
      const [bookingsRes, modelsRes, reviewsRes] = await Promise.allSettled([
        fetchMyBookings(state.user.uid),
        fetchMyModels(state.user.uid),
        fetchServiceReviews()
      ]);
      if (bookingsRes.status === "fulfilled") state.myBookings = bookingsRes.value;
      if (modelsRes.status === "fulfilled") state.myModels = modelsRes.value;
      if (reviewsRes.status === "fulfilled") state.reviews = reviewsRes.value;
      renderMyBookings();
      renderMyModels();
      renderCatalog();
      renderTestimonialCarousel();
      if (state.isAdmin && state.route === "admin") {
        await loadAdminConsole();
      }
    } else {
      state.reviews = [];
      renderCatalog();
      renderTestimonialCarousel();
    }
  } catch (err) {
    console.warn("Cloud refresh warning:", err?.message || err);
  }
}

function updateFirebaseUiStatus() {
  const dot = document.getElementById("firebaseStatusDot");
  const txt = document.getElementById("firebaseStatusText");
  const userLabel = document.getElementById("signedInUserLabel");
  const isAdminNow = hasVerifiedAdminSession();

  if (dot) dot.classList.toggle("ready", state.firebaseReady);
  if (txt) txt.textContent = state.firebaseReady ? t("firebaseStatusReady") : t("firebaseStatusOffline");
  if (userLabel) {
    userLabel.textContent = state.user?.email || state.user?.displayName || "";
    userLabel.classList.toggle("hidden", !state.user);
  }
  syncAdminUiVisibility(document, {
    isAdmin: isAdminNow,
    adminEmail: state.user?.email || ""
  });
  applyTranslations();
}

function populateBookingServicesDropdown() {
  const select = document.getElementById("bookingService");
  if (!select) return;
  const options = [];
  STUDIO_SERVICES.forEach((pkg) => {
    pkg.tiers.forEach((tier) => {
      options.push(`${pkg.recommendedServiceType} - ${tier.tierNameEn}`.slice(0, 100));
    });
  });
  select.innerHTML = options.map((opt) => `<option value="${escapeHtml(opt)}">${escapeHtml(opt)}</option>`).join("");

  const dateEl = document.getElementById("bookingDate");
  if (dateEl && !dateEl.value) {
    const tomorrow = new Date(Date.now() + 86400000);
    dateEl.min = tomorrow.toISOString().split("T")[0];
    dateEl.value = tomorrow.toISOString().split("T")[0];
  }
}

function setupPwaInstall() {
  const installBtn = document.getElementById("installAppBtn");
  const banner = document.getElementById("installPromptBanner");
  const bannerCta = document.getElementById("bannerInstallCtaBtn");

  const isStandalone =
    window.matchMedia("(display-mode: standalone)").matches ||
    window.navigator.standalone === true;

  if (!isStandalone && banner) {
    banner.classList.remove("hidden");
  }

  window.addEventListener("beforeinstallprompt", (e) => {
    e.preventDefault();
    state.deferredInstallPrompt = e;
    installBtn?.classList.remove("hidden");
    banner?.classList.remove("hidden");
  });

  window.addEventListener("appinstalled", () => {
    state.deferredInstallPrompt = null;
    installBtn?.classList.add("hidden");
    banner?.classList.add("hidden");
    showToast("STAR ARTIST STUDIO installed to Home Screen!");
  });

  const handleInstallClick = async () => {
    if (state.deferredInstallPrompt) {
      state.deferredInstallPrompt.prompt();
      await state.deferredInstallPrompt.userChoice;
      state.deferredInstallPrompt = null;
      installBtn?.classList.add("hidden");
    } else {
      showToast(t("installHowTo"));
    }
  };

  installBtn?.addEventListener("click", handleInstallClick);
  bannerCta?.addEventListener("click", handleInstallClick);

  if ("serviceWorker" in navigator) {
    window.addEventListener("load", () => {
      navigator.serviceWorker.register("/sw.js").catch((err) => {
        console.warn("Service worker registration failed:", err?.message || err);
      });
    });

    navigator.serviceWorker.addEventListener("message", async (event) => {
      const data = event.data || {};
      if (data.type === "BOOKING_SYNC_COMPLETE") {
        await refreshOfflineBookingQueue();
        showToast(t("offlineBookingSyncedToast"));
        await refreshCloudData();
      } else if (data.type === "BOOKING_SYNC_REAUTH_REQUIRED") {
        await refreshOfflineBookingQueue();
        if (state.user && state.firebaseReady) {
          await syncQueuedBookings();
        } else {
          showToast(t("offlineBookingReauthToast"));
        }
      }
    });
  }

  window.addEventListener("online", () => {
    renderOfflineBookingQueue();
    syncQueuedBookings();
  });

  window.addEventListener("offline", () => {
    renderOfflineBookingQueue();
  });
}

function wireEvents() {
  document.getElementById("openMapsLink")?.setAttribute("href", STUDIO_CONTACT.mapsUrl);
  document.getElementById("socialInstagramLink")?.setAttribute("href", STUDIO_CONTACT.socialLinks.instagram);
  document.getElementById("socialFacebookLink")?.setAttribute("href", STUDIO_CONTACT.socialLinks.facebook);
  document.getElementById("socialLinkedinLink")?.setAttribute("href", STUDIO_CONTACT.socialLinks.linkedin);

  document.addEventListener("click", async (e) => {
    const routeBtn = e.target.closest("[data-route]");
    if (routeBtn) {
      e.preventDefault();
      navigateTo(routeBtn.getAttribute("data-route"));
      return;
    }

    const tierBtn = e.target.closest("[data-tier-id]");
    if (tierBtn) {
      state.selectedTiers[tierBtn.getAttribute("data-pkg-id")] = tierBtn.getAttribute("data-tier-id");
      renderCatalog();
      return;
    }

    const tierGuideBtn = e.target.closest("[data-tier-guide-pkg]");
    if (tierGuideBtn) {
      const pkgId = tierGuideBtn.getAttribute("data-tier-guide-pkg");
      state.expandedTierGuides[pkgId] = !state.expandedTierGuides[pkgId];
      renderCatalog();
      return;
    }

    if (e.target.closest("[data-testimonial-prev]")) {
      stepTestimonialCarousel(-1);
      syncTestimonialAutoplayTimer();
      return;
    }

    if (e.target.closest("[data-testimonial-next]")) {
      stepTestimonialCarousel(1);
      syncTestimonialAutoplayTimer();
      return;
    }

    const testimonialDotBtn = e.target.closest("[data-testimonial-dot]");
    if (testimonialDotBtn) {
      const idx = Number.parseInt(testimonialDotBtn.getAttribute("data-testimonial-dot"), 10);
      if (Number.isInteger(idx)) {
        state.testimonialIndex = idx;
        renderTestimonialCarousel();
        syncTestimonialAutoplayTimer();
      }
      return;
    }

    if (e.target.closest("[data-testimonial-autoplay]")) {
      state.testimonialAutoplay = !state.testimonialAutoplay;
      renderTestimonialCarousel();
      syncTestimonialAutoplayTimer();
      return;
    }

    const bookBtn = e.target.closest("[data-book-service]");
    if (bookBtn) {
      const svc = bookBtn.getAttribute("data-book-service");
      const select = document.getElementById("bookingService");
      if (select && svc) select.value = svc;
      navigateTo("booking");
      return;
    }

    const catBtn = e.target.closest("[data-cat-filter]");
    if (catBtn) {
      state.catalogCategory = catBtn.getAttribute("data-cat-filter") || "all";
      document.querySelectorAll("[data-cat-filter]").forEach((b) => b.classList.toggle("active", b === catBtn));
      renderCatalog();
      return;
    }

    const galCatBtn = e.target.closest("[data-gal-cat]");
    if (galCatBtn) {
      state.galleryFilter = galCatBtn.getAttribute("data-gal-cat") || "All";
      renderGallery();
      return;
    }

    const cardLightbox = e.target.closest("[data-lightbox-id]");
    if (cardLightbox) {
      const id = cardLightbox.getAttribute("data-lightbox-id");
      const fromGalleryGrid = Boolean(cardLightbox.closest("#galleryGrid"));
      const folderName = fromGalleryGrid ? state.galleryFilter : "All";
      const pool = fromGalleryGrid
        ? getVisibleGalleryItems(state.galleryItems, state.galleryFilter)
        : [...state.galleryItems];
      const idx = pool.findIndex((g) => g.id === id);
      if (idx >= 0) {
        openLightboxAt(pool, idx, folderName);
      } else {
        const fallbackIdx = state.galleryItems.findIndex((g) => g.id === id);
        if (fallbackIdx >= 0) openLightboxAt([...state.galleryItems], fallbackIdx, "All");
      }
      return;
    }

    const toggleReviewsBtn = e.target.closest("[data-toggle-catalog-reviews]");
    if (toggleReviewsBtn) {
      const pkgId = toggleReviewsBtn.getAttribute("data-toggle-catalog-reviews");
      state.expandedCatalogReviews[pkgId] = !state.expandedCatalogReviews[pkgId];
      renderCatalog();
      return;
    }

    const featuredStrip = e.target.closest(".catalog-rating-strip");
    if (featuredStrip) {
      const card = featuredStrip.closest("[data-pkg-card]");
      const pkgId = card?.getAttribute("data-pkg-card");
      if (pkgId) {
        state.expandedCatalogReviews[pkgId] = true;
      }
      navigateTo("catalog");
      renderCatalog();
      return;
    }

    const rateStarBtn = e.target.closest("[data-rate-pkg]");
    if (rateStarBtn) {
      const pkgId = rateStarBtn.getAttribute("data-rate-pkg");
      const starValue = Number(rateStarBtn.getAttribute("data-rate-value")) || 5;
      const box = rateStarBtn.closest("[data-catalog-feedback]");
      const textarea = box?.querySelector(`[data-review-comment="${pkgId}"]`);
      state.catalogReviewDrafts[pkgId] = {
        rating: starValue,
        comment: textarea ? textarea.value : state.catalogReviewDrafts[pkgId]?.comment
      };
      renderCatalog();
      return;
    }

    const submitReviewBtn = e.target.closest("[data-submit-catalog-review]");
    if (submitReviewBtn) {
      const pkgId = submitReviewBtn.getAttribute("data-submit-catalog-review");
      const pkg = STUDIO_SERVICES.find((s) => s.id === pkgId);
      if (!pkg) return;

      if (!state.user) {
        showToast("Please sign in first to post a star rating and comment.");
        if (state.firebaseReady) {
          document.getElementById("authModal")?.classList.remove("hidden");
        } else {
          document.getElementById("firebaseSetupModal")?.classList.remove("hidden");
        }
        return;
      }

      const box = submitReviewBtn.closest("[data-catalog-feedback]");
      const textarea = box?.querySelector(`[data-review-comment="${pkgId}"]`);
      const targetBookingId = catalogReviewBookingId(pkg.id).toLowerCase();
      const existingReview = state.reviews.find(
        (r) =>
          r.userId === state.user.uid &&
          String(r.bookingId || "").trim().toLowerCase() === targetBookingId
      );
      const draft = state.catalogReviewDrafts[pkgId] || {};
      const rating = Number(draft.rating ?? existingReview?.rating ?? 5);
      const comment = textarea ? textarea.value : draft.comment ?? existingReview?.comment ?? "";

      const validationError = validateCatalogReview(rating, comment, state.lang);
      if (validationError) {
        showToast(validationError);
        return;
      }

      state.isSubmittingReview = true;
      renderCatalog();
      try {
        await submitCatalogReview({
          packageId: pkg.id,
          serviceType: pkg.recommendedServiceType,
          rating,
          comment
        });
        delete state.catalogReviewDrafts[pkgId];
        state.expandedCatalogReviews[pkgId] = true;
        showToast(existingReview ? "Rating & comment updated!" : "Thank you for rating this service!");
        await refreshCloudData();
      } catch (err) {
        showToast(err.message || "Unable to submit feedback.");
      } finally {
        state.isSubmittingReview = false;
        renderCatalog();
      }
      return;
    }

    const bookingStatusBtn = e.target.closest("[data-booking-status-filter]");
    if (bookingStatusBtn) {
      state.bookingStatusFilter = bookingStatusBtn.getAttribute("data-booking-status-filter") || "all";
      renderMyBookings();
      return;
    }

    const cancelBk = e.target.closest("[data-cancel-booking]");
    if (cancelBk) {
      try {
        await cancelMyBooking(cancelBk.getAttribute("data-cancel-booking"));
        showToast("Booking cancelled.");
        await refreshCloudData();
      } catch (err) {
        showToast(err.message);
      }
      return;
    }

    if (e.target.closest("[data-sync-offline-bookings]")) {
      await syncQueuedBookings({ notifyIfNone: true });
      return;
    }

    const removeOfflineBtn = e.target.closest("[data-remove-offline-booking]");
    if (removeOfflineBtn) {
      const offlineId = removeOfflineBtn.getAttribute("data-remove-offline-booking");
      try {
        await removeQueuedBooking(offlineId);
        await refreshOfflineBookingQueue();
        showToast("Offline booking draft removed.");
      } catch (err) {
        showToast(err.message);
      }
      return;
    }

    const adminTabBtn = e.target.closest("[data-admin-tab]");
    if (adminTabBtn) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      state.adminTab = adminTabBtn.getAttribute("data-admin-tab");
      document.querySelectorAll("[data-admin-tab]").forEach((b) => b.classList.toggle("active", b === adminTabBtn));
      renderAdminConsole();
      return;
    }

    const admModelFilterBtn = e.target.closest("[data-adm-model-filter]");
    if (admModelFilterBtn) {
      if (!hasVerifiedAdminSession()) return;
      state.adminModelFilter = admModelFilterBtn.getAttribute("data-adm-model-filter") || "pending";
      renderAdminConsole();
      return;
    }

    const admQuickNoteBtn = e.target.closest("[data-adm-model-quicknote]");
    if (admQuickNoteBtn) {
      if (!hasVerifiedAdminSession()) return;
      const modelId = admQuickNoteBtn.getAttribute("data-adm-model-quicknote");
      const val = admQuickNoteBtn.getAttribute("data-adm-quicknote-val") || "";
      state.adminModelDrafts[modelId] = {
        ...(state.adminModelDrafts[modelId] || {}),
        notes: val
      };
      renderAdminConsole();
      return;
    }

    if (e.target.matches("[data-adm-bk-id]")) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      await adminUpdateBookingStatus(e.target.getAttribute("data-adm-bk-id"), e.target.getAttribute("data-adm-bk-st"));
      showToast("Booking status updated.");
      await loadAdminConsole();
    } else if (e.target.matches("[data-adm-bk-del]")) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      await adminDeleteBooking(e.target.getAttribute("data-adm-bk-del"));
      showToast("Booking removed.");
      await loadAdminConsole();
    } else if (e.target.matches("[data-adm-mdl-id]")) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const modelId = e.target.getAttribute("data-adm-mdl-id");
      const nextStatus = e.target.getAttribute("data-adm-mdl-st");
      const targetModel = state.adminData.models.find((m) => m.id === modelId);
      const card = e.target.closest("[data-adm-model-card]");
      const checkInput = card?.querySelector(`[data-adm-model-check="${modelId}"]`);
      const notesInput = card?.querySelector(`[data-adm-model-notes="${modelId}"]`);
      const draft = state.adminModelDrafts[modelId] || {};
      const checklistConfirmed =
        checkInput ? checkInput.checked : Boolean(draft.checked ?? targetModel?.status === "approved");
      const adminNotes = notesInput ? notesInput.value : draft.notes ?? targetModel?.adminNotes ?? "";

      const err = validateManualModelVerification({
        status: nextStatus,
        age: targetModel?.age ?? 18,
        consentGiven: Boolean(targetModel?.consentGiven),
        checklistConfirmed,
        adminNotes,
        lang: state.lang
      });
      if (err) {
        showToast(err);
        return;
      }

      try {
        await adminUpdateModelStatus(modelId, nextStatus, adminNotes);
        delete state.adminModelDrafts[modelId];
        showToast(
          nextStatus === "approved"
            ? "Verified and added to Official Studio Roster!"
            : nextStatus === "rejected"
            ? "Model application rejected with review note."
            : "Moved back to verification queue."
        );
        await loadAdminConsole();
      } catch (updateErr) {
        showToast(updateErr.message || "Unable to update model verification status.");
      }
    } else if (e.target.matches("[data-adm-gal-id]")) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      await adminUpdateGalleryStatus(e.target.getAttribute("data-adm-gal-id"), e.target.getAttribute("data-adm-gal-st"));
      showToast("Gallery item updated.");
      await loadAdminConsole();
    } else if (e.target.matches("[data-adm-gal-del]")) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      await adminDeleteGalleryItem(e.target.getAttribute("data-adm-gal-del"));
      showToast("Gallery item deleted.");
      await loadAdminConsole();
    }
  });

  document.addEventListener("change", (e) => {
    const checkInput = e.target.closest("[data-adm-model-check]");
    if (!checkInput) return;
    const modelId = checkInput.getAttribute("data-adm-model-check");
    state.adminModelDrafts[modelId] = {
      ...(state.adminModelDrafts[modelId] || {}),
      checked: checkInput.checked
    };
  });

  document.addEventListener("input", (e) => {
    const admSearchInput = e.target.closest("[data-adm-model-search]");
    if (admSearchInput) {
      state.adminModelSearch = admSearchInput.value || "";
      renderAdminConsole();
      const restored = document.querySelector("[data-adm-model-search]");
      if (restored) {
        restored.focus();
        const len = restored.value.length;
        restored.setSelectionRange?.(len, len);
      }
      return;
    }

    const admNotesInput = e.target.closest("[data-adm-model-notes]");
    if (admNotesInput) {
      const modelId = admNotesInput.getAttribute("data-adm-model-notes");
      state.adminModelDrafts[modelId] = {
        ...(state.adminModelDrafts[modelId] || {}),
        notes: admNotesInput.value
      };
      return;
    }

    const reviewInput = e.target.closest("[data-review-comment]");
    if (!reviewInput) return;
    const pkgId = reviewInput.getAttribute("data-review-comment");
    const current = state.catalogReviewDrafts[pkgId] || {};
    state.catalogReviewDrafts[pkgId] = {
      ...current,
      comment: reviewInput.value
    };
    const form = reviewInput.closest(".catalog-review-form");
    const counter = form?.querySelector(".catalog-char-count");
    if (counter) {
      counter.textContent = `${reviewInput.value.length}/1000`;
    }
  });

  document.getElementById("catalogSearchInput")?.addEventListener("input", (e) => {
    state.catalogSearch = e.target.value || "";
    renderCatalog();
  });

  document.getElementById("langToggleBtn")?.addEventListener("click", () => {
    state.lang = state.lang === "en" ? "hi" : "en";
    localStorage.setItem("star_artist_lang", state.lang);
    applyTranslations();
    renderCatalog();
    renderTestimonialCarousel();
    renderMyBookings();
    updateNewsletterRealtimeValidation({ showEmptyError: false });
  });

  const testimonialSec = document.getElementById("homeTestimonialsSection");
  if (testimonialSec) {
    let touchStartX = null;
    testimonialSec.addEventListener("keydown", (e) => {
      if (e.key === "ArrowLeft") {
        e.preventDefault();
        stepTestimonialCarousel(-1);
        syncTestimonialAutoplayTimer();
      } else if (e.key === "ArrowRight") {
        e.preventDefault();
        stepTestimonialCarousel(1);
        syncTestimonialAutoplayTimer();
      }
    });
    testimonialSec.addEventListener(
      "touchstart",
      (e) => {
        touchStartX = e.changedTouches?.[0]?.clientX ?? null;
      },
      { passive: true }
    );
    testimonialSec.addEventListener(
      "touchend",
      (e) => {
        if (touchStartX === null) return;
        const endX = e.changedTouches?.[0]?.clientX ?? touchStartX;
        const dx = endX - touchStartX;
        touchStartX = null;
        if (Math.abs(dx) >= 40) {
          stepTestimonialCarousel(dx < 0 ? 1 : -1);
          syncTestimonialAutoplayTimer();
        }
      },
      { passive: true }
    );
  }

  document.getElementById("themeToggleBtn")?.addEventListener("click", () => {
    state.theme = state.theme === "dark" ? "light" : "dark";
    localStorage.setItem("star_artist_theme", state.theme);
    applyTheme();
    applyTranslations();
  });

  document.getElementById("authActionBtn")?.addEventListener("click", async () => {
    if (state.user) {
      await signOutFirebase();
      showToast("Signed out.");
    } else if (!state.firebaseReady) {
      document.getElementById("firebaseSetupModal")?.classList.remove("hidden");
    } else {
      openAuthModal({ adminNotice: false });
    }
  });

  document.getElementById("adminSignOutBtn")?.addEventListener("click", async () => {
    await signOutFirebase();
    showToast("Signed out.");
  });

  document.getElementById("closeAuthModalBtn")?.addEventListener("click", () => {
    state.pendingAdminRoute = false;
    closeAuthModal();
  });

  document.getElementById("openFirebaseSetupBtn")?.addEventListener("click", () => {
    document.getElementById("firebaseSetupModal")?.classList.remove("hidden");
  });

  document.getElementById("closeFirebaseSetupBtn")?.addEventListener("click", () => {
    document.getElementById("firebaseSetupModal")?.classList.add("hidden");
  });

  function setActiveLightboxFilter(presetId = "original") {
    const imgEl = document.getElementById("lightboxImg");
    const applied = applyLightboxFilterToElement(imgEl, presetId);
    state.activeLightboxFilter = applied.id;
    document.querySelectorAll("#lightboxFilterBar [data-lightbox-filter]").forEach((btn) => {
      const isMatch = btn.getAttribute("data-lightbox-filter") === applied.id;
      btn.classList.toggle("active", isMatch);
      btn.setAttribute("aria-pressed", isMatch ? "true" : "false");
    });
  }

  function syncLightboxSlideshowUi() {
    const playing = state.lightboxSlideshowPlaying;
    const total = state.activeLightboxItems.length;
    const btn = document.getElementById("lightboxSlideshowBtn");
    const iconEl = document.getElementById("lightboxSlideshowIcon");
    const labelEl = document.getElementById("lightboxSlideshowLabel");
    const prevBtn = document.getElementById("lightboxPrevBtn");
    const nextBtn = document.getElementById("lightboxNextBtn");
    const canCycle = total > 1;

    if (prevBtn) {
      prevBtn.disabled = !canCycle;
      prevBtn.setAttribute("aria-label", t("lightboxPrevLabel"));
    }
    if (nextBtn) {
      nextBtn.disabled = !canCycle;
      nextBtn.setAttribute("aria-label", t("lightboxNextLabel"));
    }
    if (btn) {
      btn.disabled = !canCycle;
      btn.classList.toggle("is-playing", playing && canCycle);
      btn.setAttribute("aria-pressed", playing && canCycle ? "true" : "false");
    }
    if (iconEl) iconEl.textContent = playing && canCycle ? "⏸" : "▶";
    if (labelEl) {
      labelEl.textContent = playing && canCycle ? t("lightboxPauseSlideshow") : t("lightboxPlaySlideshow");
    }
    const downloadAllBtn = document.getElementById("lightboxDownloadAllBtn");
    const downloadAllCountEl = document.getElementById("lightboxDownloadAllCount");
    if (downloadAllCountEl) {
      downloadAllCountEl.textContent = String(total);
    }
    if (downloadAllBtn) {
      downloadAllBtn.disabled = total <= 0 || state.isPackingGalleryZip;
    }
  }

  function getLightboxStageSize() {
    const stageEl = document.getElementById("lightboxStage");
    const rect = stageEl?.getBoundingClientRect?.();
    if (rect && rect.width > 0 && rect.height > 0) {
      return { width: rect.width, height: rect.height };
    }
    return { width: 600, height: 400 };
  }

  function syncLightboxZoomUi(snapshot = { scale: 1, x: 0, y: 0, isZoomed: false }) {
    const outBtn = document.getElementById("lightboxZoomOutBtn");
    const resetBtn = document.getElementById("lightboxZoomResetBtn");
    const inBtn = document.getElementById("lightboxZoomInBtn");
    const scale = Number(snapshot?.scale || 1);
    const zoomed = Boolean(snapshot?.isZoomed);

    if (resetBtn) {
      resetBtn.textContent = `${scale.toFixed(1)}×`;
      resetBtn.classList.toggle("is-active", zoomed);
      resetBtn.setAttribute("aria-label", t("lightboxZoomResetLabel"));
      resetBtn.setAttribute("title", t("lightboxZoomResetLabel"));
    }
    if (outBtn) {
      outBtn.disabled = !zoomed;
      outBtn.setAttribute("aria-label", t("lightboxZoomOutLabel"));
      outBtn.setAttribute("title", t("lightboxZoomOutLabel"));
    }
    if (inBtn) {
      inBtn.disabled = scale >= LIGHTBOX_MAX_ZOOM - 0.01;
      inBtn.setAttribute("aria-label", t("lightboxZoomInLabel"));
      inBtn.setAttribute("title", t("lightboxZoomInLabel"));
    }
  }

  const lightboxZoom = createLightboxZoomController({
    onChange: (snapshot) => {
      const imgEl = document.getElementById("lightboxImg");
      applyLightboxZoomTransform(imgEl, snapshot);
      syncLightboxZoomUi(snapshot);
    }
  });

  const lightboxSlideshow = createLightboxSlideshowController({
    onStep: () => {
      if (state.activeLightboxItems.length > 1 && !lightboxZoom.isZoomed()) {
        stepLightboxSlide(1, { fromAutoplay: true });
      }
    },
    onStateChange: (isPlaying) => {
      state.lightboxSlideshowPlaying = isPlaying;
      syncLightboxSlideshowUi();
    }
  });

  function renderLightboxSlide() {
    const total = state.activeLightboxItems.length;
    if (total <= 0 || state.activeLightboxIndex < 0 || state.activeLightboxIndex >= total) return;
    const item = state.activeLightboxItems[state.activeLightboxIndex];
    state.activeLightboxItem = item;
    setActiveLightboxFilter("original");
    lightboxZoom.reset();
    const titleEl = document.getElementById("lightboxTitle");
    const imgEl = document.getElementById("lightboxImg");
    const metaEl = document.getElementById("lightboxMeta");
    const counterEl = document.getElementById("lightboxCounter");
    const exifEl = document.getElementById("lightboxExifPanel");
    if (titleEl) titleEl.textContent = item.title;
    if (imgEl) {
      imgEl.classList.remove("is-pinching");
      imgEl.src = item.mediaUrl || item.thumbnailUrl;
      imgEl.alt = item.title || "Gallery preview";
    }
    if (metaEl) metaEl.textContent = `${item.category} • ${item.mediaType || "photo"}`;
    if (counterEl) counterEl.textContent = formatLightboxSlideCounter(state.activeLightboxIndex, total);
    if (exifEl) {
      const imgSrc = item.mediaUrl || item.thumbnailUrl || "";
      const cacheKey = item.id || imgSrc;
      const existingMeta = item.metadata || item.exif || null;
      const hasPresetCameraExif = Boolean(
        existingMeta && (existingMeta.camera || existingMeta.aperture || existingMeta.iso || existingMeta.shutterSpeed)
      );
      const cachedExif = state.exifCache[cacheKey];
      const shouldAutoRead = !hasPresetCameraExif && !cachedExif && Boolean(imgSrc);
      const mergedItem = cachedExif?.hasExif
        ? { ...item, metadata: { ...cachedExif, ...(existingMeta || {}) } }
        : item;

      exifEl.innerHTML = renderLightboxMetadataHtml(mergedItem, {
        lang: state.lang,
        t,
        escapeHtml,
        autoExtracted: Boolean(cachedExif?.hasExif),
        isReadingExif: shouldAutoRead
      });

      if (shouldAutoRead) {
        readExifFromImageUrl(imgSrc).then((parsed) => {
          state.exifCache[cacheKey] = parsed;
          if (state.activeLightboxItem?.id !== item.id) return;
          const updatedItem = parsed.hasExif
            ? { ...item, metadata: { ...parsed, ...(existingMeta || {}) } }
            : item;
          exifEl.innerHTML = renderLightboxMetadataHtml(updatedItem, {
            lang: state.lang,
            t,
            escapeHtml,
            autoExtracted: Boolean(parsed.hasExif),
            isReadingExif: false
          });
        });
      }
    }
    syncLightboxSlideshowUi();
  }

  function openLightboxAt(items, index = 0, folderName = "All") {
    if (!Array.isArray(items) || !items.length) return;
    lightboxSlideshow.stop();
    state.activeLightboxItems = items;
    state.activeLightboxFolder = folderName || "All";
    state.activeLightboxIndex = Math.max(0, Math.min(items.length - 1, index));
    renderLightboxSlide();
    document.getElementById("lightboxModal")?.classList.remove("hidden");
  }

  function stepLightboxSlide(delta = 1, { fromAutoplay = false } = {}) {
    const total = state.activeLightboxItems.length;
    if (total <= 1) return;
    const nextIdx = getNextLightboxIndex(state.activeLightboxIndex, total, delta);
    if (nextIdx < 0) return;
    state.activeLightboxIndex = nextIdx;
    renderLightboxSlide();
    if (!fromAutoplay && lightboxSlideshow.isPlaying()) {
      lightboxSlideshow.reset();
    }
  }

  function closeLightbox() {
    lightboxSlideshow.stop();
    lightboxZoom.reset();
    document.getElementById("lightboxImg")?.classList.remove("is-pinching");
    state.activeLightboxItem = null;
    state.activeLightboxItems = [];
    state.activeLightboxFolder = "All";
    state.activeLightboxIndex = -1;
    state.lightboxSlideshowPlaying = false;
    setActiveLightboxFilter("original");
    document.getElementById("lightboxModal")?.classList.add("hidden");
  }

  document.getElementById("lightboxFilterBar")?.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-lightbox-filter]");
    if (!btn) return;
    const requested = btn.getAttribute("data-lightbox-filter") || "original";
    const nextPreset = requested !== "original" && state.activeLightboxFilter === requested
      ? "original"
      : requested;
    setActiveLightboxFilter(nextPreset);
  });

  document.getElementById("lightboxPrevBtn")?.addEventListener("click", () => {
    stepLightboxSlide(-1);
  });

  document.getElementById("lightboxNextBtn")?.addEventListener("click", () => {
    stepLightboxSlide(1);
  });

  document.getElementById("lightboxZoomInBtn")?.addEventListener("click", () => {
    const { width, height } = getLightboxStageSize();
    lightboxZoom.zoomIn(width, height);
  });

  document.getElementById("lightboxZoomOutBtn")?.addEventListener("click", () => {
    const { width, height } = getLightboxStageSize();
    lightboxZoom.zoomOut(width, height);
  });

  document.getElementById("lightboxZoomResetBtn")?.addEventListener("click", () => {
    if (lightboxZoom.isZoomed()) {
      lightboxZoom.reset();
    } else {
      const { width, height } = getLightboxStageSize();
      lightboxZoom.toggleDoubleTap(width, height);
    }
  });

  document.getElementById("lightboxSlideshowBtn")?.addEventListener("click", () => {
    if (state.activeLightboxItems.length <= 1) return;
    const nowPlaying = lightboxSlideshow.toggle();
    showToast(nowPlaying ? t("lightboxSlideshowStartedToast") : t("lightboxSlideshowPausedToast"));
  });

  document.getElementById("closeLightboxBtn")?.addEventListener("click", () => {
    closeLightbox();
  });

  document.getElementById("lightboxModal")?.addEventListener("click", (e) => {
    if (e.target === e.currentTarget) {
      closeLightbox();
    }
  });

  const lightboxStageEl = document.getElementById("lightboxStage");
  const lightboxImgEl = document.getElementById("lightboxImg");
  let lightboxTouchStartX = null;
  let lightboxTouchStartY = null;
  let isPinchingLightbox = false;
  let isPanningLightbox = false;
  let hadMultiTouchInGesture = false;
  let lastLightboxTapTime = 0;
  let lastLightboxTapPos = { x: 0, y: 0 };

  if (lightboxStageEl) {
    lightboxStageEl.addEventListener("touchstart", (e) => {
      if (e.target.closest(".lightbox-nav-btn")) return;

      if (e.touches.length === 2) {
        hadMultiTouchInGesture = true;
        isPinchingLightbox = true;
        isPanningLightbox = false;
        lightboxTouchStartX = null;
        lightboxTouchStartY = null;
        lightboxImgEl?.classList.add("is-pinching");
        lightboxZoom.startPinch(e.touches[0], e.touches[1]);
        return;
      }

      if (e.touches.length === 1) {
        const touch = e.touches[0];
        if (lightboxZoom.isZoomed()) {
          isPanningLightbox = lightboxZoom.startPan(touch.clientX, touch.clientY);
          lightboxImgEl?.classList.add("is-pinching");
          lightboxTouchStartX = touch.clientX;
          lightboxTouchStartY = touch.clientY;
        } else {
          hadMultiTouchInGesture = false;
          isPanningLightbox = false;
          lightboxTouchStartX = touch.clientX;
          lightboxTouchStartY = touch.clientY;
        }
      }
    }, { passive: true });

    lightboxStageEl.addEventListener("touchmove", (e) => {
      if (isPinchingLightbox && e.touches.length >= 2) {
        e.preventDefault();
        const { width, height } = getLightboxStageSize();
        lightboxZoom.movePinch(e.touches[0], e.touches[1], width, height);
        return;
      }

      if (isPanningLightbox && e.touches.length === 1 && lightboxZoom.isZoomed()) {
        e.preventDefault();
        const touch = e.touches[0];
        const { width, height } = getLightboxStageSize();
        lightboxZoom.movePan(touch.clientX, touch.clientY, width, height);
      }
    }, { passive: false });

    const finishLightboxTouch = (e) => {
      if (isPinchingLightbox && e.touches.length < 2) {
        isPinchingLightbox = false;
        lightboxZoom.endPinch();
        if (e.touches.length === 1 && lightboxZoom.isZoomed()) {
          isPanningLightbox = lightboxZoom.startPan(e.touches[0].clientX, e.touches[0].clientY);
          return;
        }
        lightboxImgEl?.classList.remove("is-pinching");
      }

      if (e.touches.length === 0) {
        lightboxImgEl?.classList.remove("is-pinching");
        const wasPanning = isPanningLightbox;
        if (isPanningLightbox) {
          isPanningLightbox = false;
          lightboxZoom.endPan();
        }

        const changed = e.changedTouches?.[0];
        if (!hadMultiTouchInGesture && lightboxTouchStartX !== null && changed) {
          const dx = changed.clientX - lightboxTouchStartX;
          const dy = changed.clientY - (lightboxTouchStartY ?? changed.clientY);
          lightboxTouchStartX = null;
          lightboxTouchStartY = null;

          if (!wasPanning && !lightboxZoom.isZoomed() && Math.abs(dx) >= 45 && Math.abs(dx) > Math.abs(dy)) {
            lastLightboxTapTime = 0;
            stepLightboxSlide(dx < 0 ? 1 : -1);
            return;
          }

          if (Math.abs(dx) < 14 && Math.abs(dy) < 14 && !e.target.closest(".lightbox-nav-btn")) {
            const now = Date.now();
            const tapDist = Math.hypot(changed.clientX - lastLightboxTapPos.x, changed.clientY - lastLightboxTapPos.y);
            if (now - lastLightboxTapTime <= 300 && tapDist < 36) {
              lastLightboxTapTime = 0;
              const { width, height } = getLightboxStageSize();
              lightboxZoom.toggleDoubleTap(width, height);
            } else {
              lastLightboxTapTime = now;
              lastLightboxTapPos = { x: changed.clientX, y: changed.clientY };
            }
          }
          return;
        }

        lightboxTouchStartX = null;
        lightboxTouchStartY = null;
        hadMultiTouchInGesture = false;
      }
    };

    lightboxStageEl.addEventListener("touchend", finishLightboxTouch, { passive: true });
    lightboxStageEl.addEventListener("touchcancel", finishLightboxTouch, { passive: true });

    // Desktop trackpad pinch / wheel zoom
    lightboxStageEl.addEventListener("wheel", (e) => {
      if (!state.activeLightboxItem) return;
      if (!e.ctrlKey && !lightboxZoom.isZoomed()) return;
      e.preventDefault();
      const { width, height } = getLightboxStageSize();
      const current = lightboxZoom.getState();
      const step = e.deltaY < 0 ? 0.35 : -0.35;
      const nextScale = current.scale + step;
      if (nextScale <= 1.05) {
        lightboxZoom.reset();
      } else {
        lightboxZoom.setZoom(nextScale, current.x, current.y, width, height);
      }
    }, { passive: false });
  }

  // Desktop double-click toggle & mouse-drag pan when zoomed
  if (lightboxImgEl) {
    lightboxImgEl.addEventListener("dblclick", () => {
      const { width, height } = getLightboxStageSize();
      lightboxZoom.toggleDoubleTap(width, height);
    });

    let isMousePanning = false;
    lightboxImgEl.addEventListener("mousedown", (e) => {
      if (!lightboxZoom.isZoomed() || e.button !== 0) return;
      e.preventDefault();
      isMousePanning = lightboxZoom.startPan(e.clientX, e.clientY);
      lightboxImgEl.classList.add("is-pinching");
    });

    window.addEventListener("mousemove", (e) => {
      if (!isMousePanning) return;
      const { width, height } = getLightboxStageSize();
      lightboxZoom.movePan(e.clientX, e.clientY, width, height);
    });

    window.addEventListener("mouseup", () => {
      if (!isMousePanning) return;
      isMousePanning = false;
      lightboxZoom.endPan();
      lightboxImgEl.classList.remove("is-pinching");
    });
  }

  window.addEventListener("keydown", (e) => {
    const modal = document.getElementById("lightboxModal");
    if (!state.activeLightboxItem || !modal || modal.classList.contains("hidden")) return;
    const tag = String(e.target?.tagName || "").toUpperCase();
    if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return;

    const action = resolveLightboxKeyAction(e.key);
    if (!action) return;

    if (action === "prev") {
      e.preventDefault();
      stepLightboxSlide(-1);
    } else if (action === "next") {
      e.preventDefault();
      stepLightboxSlide(1);
    } else if (action === "toggle-play") {
      e.preventDefault();
      if (state.activeLightboxItems.length > 1) {
        const nowPlaying = lightboxSlideshow.toggle();
        showToast(nowPlaying ? t("lightboxSlideshowStartedToast") : t("lightboxSlideshowPausedToast"));
      }
    } else if (action === "close") {
      e.preventDefault();
      closeLightbox();
    }
  });

  document.getElementById("lightboxDownloadBtn")?.addEventListener("click", async () => {
    if (!state.activeLightboxItem) return;
    const result = await downloadGalleryImage(state.activeLightboxItem, {
      baseUrl: window.location.href,
      filterPresetId: state.activeLightboxFilter
    });
    if (result.status === "downloaded-filtered") {
      showToast(t("downloadFilteredSuccessToast"));
    } else if (result.status === "downloaded-blob" || result.status === "downloaded-link") {
      showToast(t("downloadImageSuccessToast"));
    } else {
      showToast(t("downloadImageErrorToast"));
    }
  });

  document.getElementById("lightboxDownloadAllBtn")?.addEventListener("click", async () => {
    if (state.isPackingGalleryZip || !state.activeLightboxItems.length) return;
    const btn = document.getElementById("lightboxDownloadAllBtn");
    const labelEl = document.getElementById("lightboxDownloadAllLabel");
    state.isPackingGalleryZip = true;
    if (btn) {
      btn.disabled = true;
      btn.classList.add("is-packing");
    }
    if (labelEl) {
      labelEl.textContent = t("downloadAllPackingLabel");
    }

    try {
      const itemsToPack = [...state.activeLightboxItems];
      const folderName = state.activeLightboxFolder || "All";
      const result = await downloadGalleryFolderAsZip(itemsToPack, {
        folderName,
        baseUrl: window.location.href,
        onProgress: ({ current, total }) => {
          if (labelEl) {
            labelEl.textContent = `${t("downloadAllPackingLabel")} (${current}/${total})`;
          }
        }
      });

      if (result.status === "downloaded-zip") {
        showToast(t("downloadAllSuccessToast").replace("{count}", String(result.count)));
      } else if (result.status === "downloaded-partial") {
        showToast(
          t("downloadAllPartialToast")
            .replace("{count}", String(result.count))
            .replace("{total}", String(result.total))
        );
      } else {
        showToast(t("downloadAllErrorToast"));
      }
    } finally {
      state.isPackingGalleryZip = false;
      if (btn) {
        btn.disabled = state.activeLightboxItems.length <= 0;
        btn.classList.remove("is-packing");
      }
      if (labelEl) {
        labelEl.textContent = t("downloadAllImagesBtn");
      }
    }
  });

  document.getElementById("lightboxShareBtn")?.addEventListener("click", async () => {
    if (!state.activeLightboxItem) return;
    const result = await shareGalleryImage(state.activeLightboxItem, {
      baseUrl: window.location.href
    });
    if (result.status === "shared-file" || result.status === "shared-link") {
      showToast(t("shareImageSuccessToast"));
    } else if (result.status === "copied") {
      showToast(t("shareImageCopiedToast"));
    } else if (result.status === "unsupported") {
      showToast(t("shareImageUnsupportedToast"));
    }
  });

  document.getElementById("googleSignInBtn")?.addEventListener("click", async () => {
    try {
      await signInWithGooglePopup();
      closeAuthModal();
      showToast("Signed in with Google!");
    } catch (err) {
      showToast(err.message || "Google Sign-In failed");
    }
  });

  document.getElementById("emailAuthForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      await signInWithEmailPassword(
        document.getElementById("authEmail").value,
        document.getElementById("authPassword").value
      );
      closeAuthModal();
      showToast("Signed in!");
    } catch (err) {
      showToast(err.message);
    }
  });

  document.getElementById("emailRegisterBtn")?.addEventListener("click", async () => {
    try {
      await registerWithEmailPassword(
        document.getElementById("authEmail").value,
        document.getElementById("authPassword").value
      );
      closeAuthModal();
      showToast("Account created! Verification email sent.");
    } catch (err) {
      showToast(err.message);
    }
  });

  document.getElementById("firebaseConfigForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      saveManualWebConfig({
        projectId: document.getElementById("cfgProjectId").value,
        databaseId: document.getElementById("cfgDatabaseId").value,
        apiKey: document.getElementById("cfgApiKey").value,
        appId: document.getElementById("cfgAppId").value,
        recaptchaSiteKey: document.getElementById("cfgRecaptcha").value,
        messagingSenderId: FIREBASE_PROJECT_DEFAULTS.messagingSenderId,
        storageBucket: FIREBASE_PROJECT_DEFAULTS.storageBucket
      });
      window.location.reload();
    } catch (err) {
      showToast(err.message);
    }
  });

  document.getElementById("bookingForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
      clientName: document.getElementById("bookingName").value,
      contactNumber: document.getElementById("bookingPhone").value,
      serviceType: document.getElementById("bookingService").value,
      preferredDate: document.getElementById("bookingDate").value,
      preferredTime: document.getElementById("bookingTime").value,
      location: document.getElementById("bookingLocation").value,
      notes: document.getElementById("bookingNotes").value
    };

    const validationError = validateOfflineBookingPayload(payload);
    if (validationError) {
      showToast(validationError);
      return;
    }

    const queueBookingOffline = async () => {
      const syncCtx = await getBookingSyncContext();
      const record = createQueuedBookingRecord({
        payload,
        userId: syncCtx.uid || state.user?.uid || "",
        authToken: syncCtx.idToken || "",
        projectId: syncCtx.projectId,
        databaseId: syncCtx.databaseId
      });
      await enqueueOfflineBooking(record);
      await registerBookingBackgroundSync();
      await refreshOfflineBookingQueue();
      e.target.reset();
      populateBookingServicesDropdown();
      showToast(t("offlineBookingSavedToast"));
    };

    if (typeof navigator !== "undefined" && !navigator.onLine) {
      try {
        await queueBookingOffline();
      } catch (queueErr) {
        showToast(queueErr.message || "Could not save offline booking.");
      }
      return;
    }

    try {
      await createShootBooking(payload);
      e.target.reset();
      populateBookingServicesDropdown();
      showToast("Shoot booking submitted!");
      await refreshCloudData();
    } catch (err) {
      if (isLikelyOfflineError(err, typeof navigator === "undefined" ? true : navigator.onLine)) {
        try {
          await queueBookingOffline();
          return;
        } catch (queueErr) {
          showToast(queueErr.message || err.message);
          return;
        }
      }
      showToast(err.message);
      if (!state.user && state.firebaseReady) {
        document.getElementById("authModal")?.classList.remove("hidden");
      }
    }
  });

  document.getElementById("modelForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      await submitModelRegistration({
        fullName: document.getElementById("modelName").value,
        age: document.getElementById("modelAge").value,
        gender: document.getElementById("modelGender").value,
        city: document.getElementById("modelCity").value,
        phoneNumber: document.getElementById("modelPhone").value,
        skills: document.getElementById("modelSkills").value,
        portfolioUrls: document.getElementById("modelPortfolio").value,
        consentGiven: document.getElementById("modelConsent").checked
      });
      e.target.reset();
      showToast("18+ Model registration submitted!");
      await refreshCloudData();
    } catch (err) {
      showToast(err.message);
      if (!state.user && state.firebaseReady) {
        document.getElementById("authModal")?.classList.remove("hidden");
      }
    }
  });

  const galleryFileInput = document.getElementById("galleryImageFile");
  const galleryDropzone = document.getElementById("galleryUploadDropzone");
  const pickGalleryBtn = document.getElementById("pickGalleryImageBtn");
  const clearGalleryBtn = document.getElementById("clearGalleryImageBtn");
  const previewBox = document.getElementById("galleryUploadPreviewBox");
  const previewImg = document.getElementById("galleryUploadPreviewImg");
  const previewName = document.getElementById("galleryUploadPreviewName");
  const previewMeta = document.getElementById("galleryUploadPreviewMeta");

  const uploadProgress = createUploadProgressController({
    dropzoneEl: galleryDropzone,
    wrapEl: document.getElementById("galleryUploadProgressWrap"),
    barEl: document.getElementById("galleryUploadProgressBar"),
    fillEl: document.getElementById("galleryUploadProgressFill"),
    labelEl: document.getElementById("galleryUploadProgressLabel"),
    pctEl: document.getElementById("galleryUploadProgressPct"),
    bytesEl: document.getElementById("galleryUploadProgressBytes")
  });
  let uploadProgressClearTimer = null;
  let isUploadingGalleryFile = false;

  function clearGalleryUploadSelection({ keepProgress = false } = {}) {
    state.uploadedGalleryMedia = null;
    state.isEncodingGalleryImage = false;
    if (galleryFileInput) galleryFileInput.value = "";
    if (previewImg) previewImg.removeAttribute("src");
    if (previewName) previewName.textContent = "";
    if (previewMeta) previewMeta.textContent = "";
    previewBox?.classList.add("hidden");
    if (!keepProgress) {
      clearTimeout(uploadProgressClearTimer);
      uploadProgress.reset();
    }
  }

  async function handleGalleryImageFile(file) {
    if (!file || isUploadingGalleryFile) return;
    clearTimeout(uploadProgressClearTimer);
    state.isEncodingGalleryImage = true;
    if (previewBox) previewBox.classList.remove("hidden");
    if (previewName) previewName.textContent = file.name || "studio-photo.jpg";
    if (previewMeta) previewMeta.textContent = t("uploadOptimizingMsg");
    uploadProgress.update({
      percent: 25,
      bytesTransferred: 0,
      totalBytes: Number(file.size || 0),
      label: t("uploadProgressPreparing"),
      status: "uploading"
    });

    try {
      const encoded = await encodeImageFileToStudioPayloads(file);
      state.uploadedGalleryMedia = encoded;
      if (previewImg) previewImg.src = encoded.thumbnailUrl || encoded.mediaUrl;
      if (previewName) previewName.textContent = encoded.originalName;
      if (previewMeta) {
        const exifParts = encoded.metadata
          ? [encoded.metadata.camera, encoded.metadata.aperture, encoded.metadata.iso, encoded.metadata.dateTaken].filter(Boolean)
          : [];
        const baseInfo = `${encoded.width}×${encoded.height}px • ~${encoded.approxKb} KB`;
        previewMeta.textContent = exifParts.length
          ? `${baseInfo} • EXIF: ${exifParts.join(" · ")}`
          : baseInfo;
      }
      const mediaTypeEl = document.getElementById("galleryMediaType");
      if (mediaTypeEl) mediaTypeEl.value = "photo";
      const titleEl = document.getElementById("galleryTitle");
      if (titleEl && !titleEl.value.trim()) {
        titleEl.value = suggestTitleFromFileName(encoded.originalName);
      }
      uploadProgress.reset();
      showToast(t("uploadReadyToast"));
    } catch (err) {
      clearGalleryUploadSelection();
      uploadProgress.error(err.message || t("uploadProgressFailed"));
      showToast(err.message || "Could not process selected photo.");
    } finally {
      state.isEncodingGalleryImage = false;
    }
  }

  pickGalleryBtn?.addEventListener("click", (e) => {
    e.stopPropagation();
    if (isUploadingGalleryFile) return;
    galleryFileInput?.click();
  });

  galleryDropzone?.addEventListener("click", (e) => {
    if (isUploadingGalleryFile || e.target === pickGalleryBtn) return;
    galleryFileInput?.click();
  });

  galleryDropzone?.addEventListener("keydown", (e) => {
    if (isUploadingGalleryFile) return;
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      galleryFileInput?.click();
    }
  });

  galleryDropzone?.addEventListener("dragover", (e) => {
    e.preventDefault();
    if (!isUploadingGalleryFile) {
      galleryDropzone.classList.add("is-dragover");
    }
  });

  galleryDropzone?.addEventListener("dragleave", () => {
    galleryDropzone.classList.remove("is-dragover");
  });

  galleryDropzone?.addEventListener("drop", (e) => {
    e.preventDefault();
    galleryDropzone.classList.remove("is-dragover");
    if (isUploadingGalleryFile) return;
    const droppedFile = e.dataTransfer?.files?.[0];
    if (droppedFile) handleGalleryImageFile(droppedFile);
  });

  galleryFileInput?.addEventListener("change", (e) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) handleGalleryImageFile(selectedFile);
  });

  clearGalleryBtn?.addEventListener("click", () => {
    if (isUploadingGalleryFile) return;
    clearGalleryUploadSelection();
  });

  document.getElementById("galleryUploadForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (state.isEncodingGalleryImage) {
      showToast(t("uploadOptimizingMsg"));
      return;
    }
    if (isUploadingGalleryFile) return;

    const submitBtn = document.getElementById("submitGalleryBtn");
    const title = document.getElementById("galleryTitle")?.value || "";
    const category = document.getElementById("galleryCategory")?.value || "Fashion";
    const mediaType = document.getElementById("galleryMediaType")?.value || "photo";
    const pastedUrl = (document.getElementById("galleryMediaUrl")?.value || "").trim();

    let mediaUrl = state.uploadedGalleryMedia?.mediaUrl || pastedUrl;
    const thumbnailUrl = state.uploadedGalleryMedia?.thumbnailUrl || "";
    let extractedMetadata = state.uploadedGalleryMedia?.metadata || null;
    const selectedFile = state.uploadedGalleryMedia?.file || null;

    const validationError = validateGallerySubmission({ title, mediaUrl, thumbnailUrl });
    if (validationError) {
      showToast(validationError);
      return;
    }

    clearTimeout(uploadProgressClearTimer);
    isUploadingGalleryFile = true;
    if (submitBtn) submitBtn.disabled = true;
    try {
      if (selectedFile && state.firebaseReady && state.user) {
        uploadProgress.update({
          percent: 0,
          bytesTransferred: 0,
          totalBytes: Number(selectedFile.size || 0),
          label: t("uploadProgressUploading"),
          status: "uploading"
        });
        try {
          const storageResult = await uploadGalleryImageToStorage(selectedFile, {
            originalName: state.uploadedGalleryMedia?.originalName || selectedFile.name,
            onProgress: ({ percent, bytesTransferred, totalBytes }) => {
              uploadProgress.update({
                percent: Math.min(95, percent),
                bytesTransferred,
                totalBytes,
                label: t("uploadProgressUploading"),
                status: "uploading"
              });
            }
          });
          if (storageResult?.downloadUrl) {
            mediaUrl = storageResult.downloadUrl;
          }
        } catch (storageErr) {
          // Graceful 100% free Spark-plan fallback when Storage bucket is unprovisioned or restricted
          console.warn("Firebase Storage unavailable on Spark plan; falling back to free Firestore upload:", storageErr?.message || storageErr);
          uploadProgress.update({
            percent: 78,
            bytesTransferred: Number(selectedFile.size || 0),
            totalBytes: Number(selectedFile.size || 0),
            label: t("uploadProgressSavingCloud"),
            status: "uploading"
          });
        }
      } else if (selectedFile) {
        uploadProgress.update({
          percent: 55,
          bytesTransferred: 0,
          totalBytes: Number(selectedFile.size || 0),
          label: t("uploadProgressSavingCloud"),
          status: "uploading"
        });
      }

      if (!extractedMetadata && pastedUrl && mediaType === "photo") {
        const remoteExif = await readExifFromImageUrl(pastedUrl);
        if (hasMeaningfulExif(remoteExif)) {
          extractedMetadata = sanitizeExifMetadata(remoteExif);
        }
      }

      if (selectedFile) {
        uploadProgress.update({
          percent: 96,
          label: t("uploadProgressSavingCloud"),
          status: "uploading"
        });
      }

      await submitGalleryUpload(
        {
          title,
          category,
          mediaType,
          mediaUrl,
          thumbnailUrl,
          metadata: extractedMetadata
        },
        state.isAdmin
      );

      if (selectedFile) {
        uploadProgress.complete(t("uploadProgressComplete"));
        uploadProgressClearTimer = setTimeout(() => {
          uploadProgress.reset();
        }, 2000);
      }

      e.target.reset();
      clearGalleryUploadSelection({ keepProgress: Boolean(selectedFile) });
      showToast(state.isAdmin ? "Artwork published to gallery!" : "Artwork submitted for admin review!");
      await refreshCloudData();
    } catch (err) {
      if (selectedFile) {
        uploadProgress.error(t("uploadProgressFailed"));
      }
      showToast(err.message);
      if (!state.user && state.firebaseReady) {
        document.getElementById("authModal")?.classList.remove("hidden");
      }
    } finally {
      isUploadingGalleryFile = false;
      if (submitBtn) submitBtn.disabled = false;
    }
  });

  const newsletterEmailInput = document.getElementById("newsletterEmail");
  newsletterEmailInput?.addEventListener("input", () => {
    updateNewsletterRealtimeValidation({ showEmptyError: false });
  });
  newsletterEmailInput?.addEventListener("blur", () => {
    if ((newsletterEmailInput.value || "").trim().length > 0) {
      updateNewsletterRealtimeValidation({ showEmptyError: false });
    }
  });

  document.getElementById("newsletterForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const emailInput = document.getElementById("newsletterEmail");
    const statusEl = document.getElementById("newsletterStatusMsg");
    const errorEl = document.getElementById("newsletterErrorMsg");
    const emailVal = emailInput?.value || "";
    const validation = updateNewsletterRealtimeValidation({ showEmptyError: true });
    if (!validation.isValid) {
      showToast(validation.error || t("newsletterInvalidEmail"));
      emailInput?.focus();
      return;
    }

    const result = saveNewsletterSubscription({ email: emailVal, lang: state.lang });
    if (!result.ok) {
      if (errorEl) {
        errorEl.textContent = result.error || t("newsletterInvalidEmail");
        errorEl.classList.remove("hidden");
      }
      showToast(result.error || t("newsletterInvalidEmail"));
      emailInput?.focus();
      return;
    }

    if (state.user && state.firebaseReady) {
      syncWorkshopNewsletterOptIn();
    }

    const message = result.alreadySubscribed
      ? `${t("newsletterAlready")} (${result.entry.email})`
      : `${t("newsletterSuccess")} (${result.entry.email})`;
    if (errorEl) {
      errorEl.textContent = "";
      errorEl.classList.add("hidden");
    }
    if (emailInput) {
      emailInput.classList.remove("is-invalid", "is-valid");
      emailInput.setAttribute("aria-invalid", "false");
      emailInput.value = "";
    }
    if (statusEl) {
      statusEl.textContent = message;
      statusEl.className = "newsletter-status is-success";
    }

    const fieldsEl = document.getElementById("newsletterFormFields");
    const successEl = document.getElementById("newsletterSuccessNotice");
    const detailEl = document.getElementById("newsletterSuccessDetail");
    const detailText = `${t("newsletterThankYouDetail")} (${result.entry.email})`;
    showTemporaryNewsletterSuccess({
      fieldsEl,
      successEl,
      detailEl,
      detailText,
      onRestore: () => {
        renderSavedNewsletterStatus();
      }
    });
    successEl?.focus?.();
    showToast(t("newsletterThankYouHeading"));
  });

  document.addEventListener("submit", async (e) => {
    if (e.target.id === "adminAnnouncementForm") {
      e.preventDefault();
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      try {
        await adminPublishAnnouncement({
          title: document.getElementById("annTitle").value,
          message: document.getElementById("annMessage").value,
          category: "Studio Update",
          priority: "normal"
        });
        showToast("Studio announcement published!");
        await loadAdminConsole();
      } catch (err) {
        showToast(err.message);
      }
    }
  });

  const handleBrowserUrlChange = () => {
    const requested = extractRequestedRoute({
      pathname: window.location.pathname,
      hash: window.location.hash,
      search: window.location.search
    });
    if (requested !== state.route || isAdminUrlAttempt({
      pathname: window.location.pathname,
      hash: window.location.hash,
      search: window.location.search
    })) {
      navigateTo(requested);
    }
  };
  window.addEventListener("hashchange", handleBrowserUrlChange);
  window.addEventListener("popstate", handleBrowserUrlChange);
}

function updateNewsletterRealtimeValidation({ showEmptyError = false } = {}) {
  const emailInput = document.getElementById("newsletterEmail");
  const errorEl = document.getElementById("newsletterErrorMsg");
  const statusEl = document.getElementById("newsletterStatusMsg");
  if (!emailInput) {
    return { state: "idle", isValid: false, error: "" };
  }

  const evaluation = evaluateNewsletterEmailInput(emailInput.value, state.lang, {
    allowEmpty: !showEmptyError
  });

  if (evaluation.state === "invalid") {
    emailInput.classList.add("is-invalid");
    emailInput.classList.remove("is-valid");
    emailInput.setAttribute("aria-invalid", "true");
    if (errorEl) {
      errorEl.textContent = evaluation.error;
      errorEl.classList.remove("hidden");
    }
    if (statusEl && statusEl.classList.contains("is-valid-hint")) {
      statusEl.textContent = "";
      statusEl.className = "newsletter-status";
    }
  } else if (evaluation.state === "valid") {
    emailInput.classList.remove("is-invalid");
    emailInput.classList.add("is-valid");
    emailInput.setAttribute("aria-invalid", "false");
    if (errorEl) {
      errorEl.textContent = "";
      errorEl.classList.add("hidden");
    }
    if (statusEl) {
      const existing = loadNewsletterSubscribers().some(
        (item) => item?.email === evaluation.normalizedEmail
      );
      if (existing) {
        statusEl.textContent = `${t("newsletterAlready")} (${evaluation.normalizedEmail})`;
        statusEl.className = "newsletter-status is-success";
      } else {
        statusEl.textContent = t("newsletterValidReady");
        statusEl.className = "newsletter-status is-valid-hint";
      }
    }
  } else {
    emailInput.classList.remove("is-invalid", "is-valid");
    emailInput.setAttribute("aria-invalid", "false");
    if (errorEl) {
      errorEl.textContent = "";
      errorEl.classList.add("hidden");
    }
    renderSavedNewsletterStatus();
  }

  return evaluation;
}

function renderSavedNewsletterStatus() {
  const statusEl = document.getElementById("newsletterStatusMsg");
  if (!statusEl) return;
  const saved = loadNewsletterSubscribers();
  if (saved.length > 0) {
    const latest = saved[saved.length - 1];
    statusEl.textContent = `${t("newsletterSuccess")} (${latest.email})`;
    statusEl.className = "newsletter-status is-success";
  } else {
    statusEl.textContent = "";
    statusEl.className = "newsletter-status";
  }
}

async function bootstrap() {
  applyTheme();
  applyTranslations();
  populateBookingServicesDropdown();
  renderCatalog();
  renderGallery();
  renderTestimonialCarousel();
  syncTestimonialAutoplayTimer();
  renderMyBookings();
  renderMyModels();
  renderSavedNewsletterStatus();
  syncAdminUiVisibility(document, { isAdmin: false });
  await refreshOfflineBookingQueue();
  setupPwaInstall();
  wireEvents();

  const initialRoute = extractRequestedRoute({
    pathname: window.location.pathname,
    hash: window.location.hash,
    search: window.location.search
  });
  navigateTo(initialRoute, { silentBlock: true });

  const clientStatus = await initFirebaseClient();
  state.firebaseReady = Boolean(clientStatus.ready);
  updateFirebaseUiStatus();

  if (state.firebaseReady) {
    await subscribeAuthState(async ({ user, isAdmin, firebaseReady }) => {
      state.user = user;
      state.isAdmin = Boolean(isAdmin && isAuthorizedAdminUser(user));
      state.authResolved = true;
      state.firebaseReady = firebaseReady;

      if (!state.isAdmin) {
        state.adminData = { bookings: [], models: [], gallery: [], announcements: [] };
      }

      updateFirebaseUiStatus();

      if (state.pendingAdminRoute) {
        state.pendingAdminRoute = false;
        if (state.isAdmin) {
          closeAuthModal();
          navigateTo("admin");
        } else if (user) {
          closeAuthModal();
          navigateTo("home", { silentBlock: true });
          showToast(t("adminAccessDeniedToast"));
        } else {
          navigateTo("home", { silentBlock: true });
          showToast(t("adminSignInRequiredToast"));
          openAuthModal({ adminNotice: true });
        }
      } else if (state.route === "admin" && !state.isAdmin) {
        navigateTo("home", { silentBlock: true });
      }

      renderMyBookings();
      renderMyModels();
      if (user) {
        await syncQueuedBookings();
      }
      await refreshCloudData();
    });
  } else {
    state.authResolved = true;
    if (state.pendingAdminRoute) {
      state.pendingAdminRoute = false;
      showToast(t("adminSignInRequiredToast"));
      document.getElementById("firebaseSetupModal")?.classList.remove("hidden");
    }
  }
}

bootstrap();
