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
  subscribeAuthState,
  signInWithGooglePopup,
  signInWithEmailPassword,
  registerWithEmailPassword,
  signOutFirebase,
  fetchApprovedGallery,
  fetchMyBookings,
  createShootBooking,
  cancelMyBooking,
  fetchMyModels,
  submitModelRegistration,
  submitGalleryUpload,
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
  myModels: [],
  reviews: [],
  expandedCatalogReviews: {},
  expandedTierGuides: {},
  catalogReviewDrafts: {},
  isSubmittingReview: false,
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
}

function applyTheme() {
  document.documentElement.setAttribute("data-theme", state.theme);
  const metaTheme = document.querySelector('meta[name="theme-color"]');
  if (metaTheme) {
    metaTheme.setAttribute("content", state.theme === "light" ? "#F8F9FA" : "#0C0C0C");
  }
}

function navigateTo(route) {
  const validRoutes = ["home", "catalog", "booking", "models", "gallery", "admin"];
  const target = validRoutes.includes(route) ? route : "home";
  state.route = target;
  if (window.location.hash !== `#${target}`) {
    history.replaceState(null, "", `#${target}`);
  }

  validRoutes.forEach((r) => {
    const sec = document.getElementById(`view-${r}`);
    if (sec) sec.classList.toggle("hidden", r !== target);
  });

  document.querySelectorAll("[data-route]").forEach((btn) => {
    btn.classList.toggle("active", btn.getAttribute("data-route") === target);
  });

  if (target === "admin" && state.isAdmin) {
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
  const filterBar = document.getElementById("galleryFilterBar");
  if (filterBar) {
    filterBar.innerHTML = categories
      .map(
        (cat) =>
          `<button type="button" class="chip-btn ${state.galleryFilter === cat ? "active" : ""}" data-gal-cat="${cat}">${cat}</button>`
      )
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

function renderMyBookings() {
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
  const locked = document.getElementById("adminLockedPanel");
  const content = document.getElementById("adminConsoleContent");
  if (!state.isAdmin) {
    locked?.classList.remove("hidden");
    content?.classList.add("hidden");
    return;
  }
  locked?.classList.add("hidden");
  content?.classList.remove("hidden");

  try {
    state.adminData = await adminFetchDashboardData();
    renderAdminConsole();
  } catch (err) {
    showToast(err.message || "Unable to load Admin Dashboard");
  }
}

function renderAdminConsole() {
  const stats = document.getElementById("adminStatsGrid");
  if (stats) {
    stats.innerHTML = `
      <div class="studio-card"><div class="stat-value">${state.adminData.bookings.length}</div><div class="stat-label">Total Bookings</div></div>
      <div class="studio-card"><div class="stat-value">${state.adminData.models.length}</div><div class="stat-label">18+ Models</div></div>
      <div class="studio-card"><div class="stat-value">${state.adminData.gallery.length}</div><div class="stat-label">Gallery Submissions</div></div>
      <div class="studio-card"><div class="stat-value">${state.adminData.announcements.length}</div><div class="stat-label">Announcements</div></div>`;
  }

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
      if (state.isAdmin && state.route === "admin") {
        await loadAdminConsole();
      }
    } else {
      state.reviews = [];
      renderCatalog();
    }
  } catch (err) {
    console.warn("Cloud refresh warning:", err?.message || err);
  }
}

function updateFirebaseUiStatus() {
  const dot = document.getElementById("firebaseStatusDot");
  const txt = document.getElementById("firebaseStatusText");
  const userLabel = document.getElementById("signedInUserLabel");
  const adminNav = document.getElementById("desktopAdminNavBtn");
  const adminQuick = document.getElementById("openAdminConsoleQuickBtn");

  if (dot) dot.classList.toggle("ready", state.firebaseReady);
  if (txt) txt.textContent = state.firebaseReady ? t("firebaseStatusReady") : t("firebaseStatusOffline");
  if (userLabel) {
    userLabel.textContent = state.user?.email || state.user?.displayName || "";
    userLabel.classList.toggle("hidden", !state.user);
  }
  adminNav?.classList.toggle("hidden", !state.isAdmin);
  adminQuick?.classList.toggle("hidden", !state.isAdmin);
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
  }
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
      const item = state.galleryItems.find((g) => g.id === id);
      if (item) {
        document.getElementById("lightboxTitle").textContent = item.title;
        document.getElementById("lightboxImg").src = item.mediaUrl || item.thumbnailUrl;
        document.getElementById("lightboxMeta").textContent = `${item.category} • ${item.mediaType || "photo"}`;
        document.getElementById("lightboxModal").classList.remove("hidden");
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

    const adminTabBtn = e.target.closest("[data-admin-tab]");
    if (adminTabBtn) {
      state.adminTab = adminTabBtn.getAttribute("data-admin-tab");
      document.querySelectorAll("[data-admin-tab]").forEach((b) => b.classList.toggle("active", b === adminTabBtn));
      renderAdminConsole();
      return;
    }

    const admModelFilterBtn = e.target.closest("[data-adm-model-filter]");
    if (admModelFilterBtn) {
      state.adminModelFilter = admModelFilterBtn.getAttribute("data-adm-model-filter") || "pending";
      renderAdminConsole();
      return;
    }

    const admQuickNoteBtn = e.target.closest("[data-adm-model-quicknote]");
    if (admQuickNoteBtn) {
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
      await adminUpdateBookingStatus(e.target.getAttribute("data-adm-bk-id"), e.target.getAttribute("data-adm-bk-st"));
      showToast("Booking status updated.");
      await loadAdminConsole();
    } else if (e.target.matches("[data-adm-bk-del]")) {
      await adminDeleteBooking(e.target.getAttribute("data-adm-bk-del"));
      showToast("Booking removed.");
      await loadAdminConsole();
    } else if (e.target.matches("[data-adm-mdl-id]")) {
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
      await adminUpdateGalleryStatus(e.target.getAttribute("data-adm-gal-id"), e.target.getAttribute("data-adm-gal-st"));
      showToast("Gallery item updated.");
      await loadAdminConsole();
    } else if (e.target.matches("[data-adm-gal-del]")) {
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
    renderMyBookings();
    updateNewsletterRealtimeValidation({ showEmptyError: false });
  });

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
      document.getElementById("authModal")?.classList.remove("hidden");
    }
  });

  document.getElementById("closeAuthModalBtn")?.addEventListener("click", () => {
    document.getElementById("authModal")?.classList.add("hidden");
  });

  document.getElementById("openFirebaseSetupBtn")?.addEventListener("click", () => {
    document.getElementById("firebaseSetupModal")?.classList.remove("hidden");
  });

  document.getElementById("closeFirebaseSetupBtn")?.addEventListener("click", () => {
    document.getElementById("firebaseSetupModal")?.classList.add("hidden");
  });

  document.getElementById("closeLightboxBtn")?.addEventListener("click", () => {
    document.getElementById("lightboxModal")?.classList.add("hidden");
  });

  document.getElementById("googleSignInBtn")?.addEventListener("click", async () => {
    try {
      await signInWithGooglePopup();
      document.getElementById("authModal")?.classList.add("hidden");
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
      document.getElementById("authModal")?.classList.add("hidden");
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
      document.getElementById("authModal")?.classList.add("hidden");
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
    try {
      await createShootBooking({
        clientName: document.getElementById("bookingName").value,
        contactNumber: document.getElementById("bookingPhone").value,
        serviceType: document.getElementById("bookingService").value,
        preferredDate: document.getElementById("bookingDate").value,
        preferredTime: document.getElementById("bookingTime").value,
        location: document.getElementById("bookingLocation").value,
        notes: document.getElementById("bookingNotes").value
      });
      e.target.reset();
      populateBookingServicesDropdown();
      showToast("Shoot booking submitted!");
      await refreshCloudData();
    } catch (err) {
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

  document.getElementById("galleryUploadForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      await submitGalleryUpload(
        {
          title: document.getElementById("galleryTitle").value,
          category: document.getElementById("galleryCategory").value,
          mediaType: document.getElementById("galleryMediaType").value,
          mediaUrl: document.getElementById("galleryMediaUrl").value
        },
        state.isAdmin
      );
      e.target.reset();
      showToast(state.isAdmin ? "Artwork published to gallery!" : "Artwork submitted for admin review!");
      await refreshCloudData();
    } catch (err) {
      showToast(err.message);
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
  renderMyBookings();
  renderMyModels();
  renderSavedNewsletterStatus();
  setupPwaInstall();
  wireEvents();

  const initialRoute = window.location.hash.replace("#", "").trim() || "home";
  navigateTo(initialRoute);

  const clientStatus = await initFirebaseClient();
  state.firebaseReady = Boolean(clientStatus.ready);
  updateFirebaseUiStatus();

  if (state.firebaseReady) {
    await subscribeAuthState(async ({ user, isAdmin, firebaseReady }) => {
      state.user = user;
      state.isAdmin = isAdmin;
      state.firebaseReady = firebaseReady;
      updateFirebaseUiStatus();
      renderMyBookings();
      renderMyModels();
      await refreshCloudData();
    });
  }
}

bootstrap();
