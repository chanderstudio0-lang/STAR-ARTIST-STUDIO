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
  filterModelApplications,
  validateManualModelVerification,
  evaluateBulkModelUpdate,
  renderAdminModelsSectionHtml
} from "./admin-models.js";
import {
  isAuthorizedAdminUser,
  isAdminUrlAttempt,
  extractRequestedRoute,
  resolveSafeRoute,
  syncAdminUiVisibility
} from "./admin-guard.js";
import {
  isAuthenticatedMember,
  extractAuthRouteFromUrl,
  resolveAuthenticatedAccess,
  validateRegistrationInput,
  validateLoginInput,
  validatePasswordResetInput,
  mapFirebaseAuthError,
  syncAuthGateDom
} from "./auth-gate.js";
import {
  mountModelGrowthChart,
  unmountModelGrowthChart
} from "./admin-model-growth.js";
import {
  mountModelExperienceChart,
  unmountModelExperienceChart
} from "./admin-model-experience.js";
import {
  PAYMENT_TRENDS_DEFAULT_DAYS,
  mountPaymentTrendsChart,
  unmountPaymentTrendsChart
} from "./admin-payment-trends.js";
import {
  exportBookingsToCsv,
  applyAdminCsvButtonState,
  runAdminBookingsCsvExport,
  renderAdminBookingsToolbarHtml
} from "./admin-bookings-export.js";
import {
  createRealtimeBookingTracker,
  renderAdminBookingTrackerHtml,
  renderBookingTimelineChipHtml
} from "./admin-booking-tracker.js";
import {
  buildAdminEmailPreview,
  buildAdminTestEmailRequestPayload,
  getPreviewRecordsForType,
  renderAdminEmailPreviewSectionHtml
} from "./admin-email-preview.js";
import {
  DEFAULT_ADMIN_EMAIL_ALERT_SETTINGS,
  normalizeAdminEmailAlertSettings,
  detectNewAdminAlertItems,
  buildAdminEmailAlertRecord,
  summarizeAdminEmailAlerts,
  renderAdminEmailAlertsSectionHtml
} from "./admin-email-alerts.js";
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
  createLightboxFullscreenController,
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
  createDefaultEditorState,
  clampBrightness,
  clampCropZoom,
  clampOffset,
  normalizeAspectPreset,
  drawCropWorkspaceToCanvas,
  drawEditedImageToCanvas,
  exportEditedCanvasPayloads,
  formatEditorSummary
} from "./image-editor.js";
import {
  readExifFromImageUrl,
  hasMeaningfulExif,
  sanitizeExifMetadata
} from "./exif-reader.js";
import {
  mergePublicModels,
  filterPublicModels,
  validateCompanyRegistration,
  validateCompanyModelBooking,
  renderPublicModelPortfolioCardsHtml,
  renderSelectedRequestedModelsHtml,
  renderMyCompanyBookingsHtml,
  renderAdminCompanyRequestsSectionHtml
} from "./company-booking.js";
import {
  DEFAULT_MODEL_DISCOVERY_FILTERS,
  getAvailableStatesFromModels,
  getAvailableCitiesFromModels,
  countActiveDiscoveryFilters,
  renderModelProfileModalHtml,
  buildModelProfileShareUrl,
  extractSharedModelIdFromUrl
} from "./model-discovery.js";
import {
  MAX_PORTFOLIO_PHOTOS,
  calculateAgeFromDob,
  encodePortfolioPhotoFile,
  sanitizePortfolioPhotoList,
  validateModelSelfRegistration,
  renderPortfolioPhotoStripHtml,
  renderModelDashboardHtml
} from "./model-portal.js";
import {
  WEDDING_PACKAGE_ID,
  WEDDING_BOOKING_ADDONS,
  formatInrCurrency,
  getServiceTierBookingValue,
  mergeWeddingTierConfigs,
  findWeddingTierByBookingValue,
  calculateWeddingBookingTotal,
  buildWeddingBookingNotesWithPricing,
  updateBookingWeddingTotalDom,
  renderBookingWeddingSummaryHtml,
  renderAdminWeddingPackagesSectionHtml
} from "./wedding-packages.js";
import {
  subscribeAuthState,
  signInWithGooglePopup,
  signInWithEmailPassword,
  registerWithEmailPassword,
  sendPasswordReset,
  signOutFirebase,
  fetchApprovedGallery,
  fetchMyBookings,
  createShootBooking,
  getBookingSyncContext,
  cancelMyBooking,
  fetchMyModels,
  fetchMyNotifications,
  markMyNotificationRead,
  submitModelRegistration,
  updateMyModelProfile,
  submitGalleryUpload,
  uploadGalleryImageToStorage,
  fetchServiceReviews,
  submitCatalogReview,
  fetchPublicApprovedModels,
  fetchMyCompanyProfile,
  saveCompanyRegistration,
  fetchMyCompanyBookings,
  submitCompanyModelBooking,
  cancelMyCompanyBooking,
  adminFetchDashboardData,
  adminUpdateBookingStatus,
  adminAutoCompleteExpiredBookings,
  adminSubscribeBookingsRealtime,
  adminDeleteBooking,
  adminUpdateModelStatus,
  adminSetModelDisabled,
  adminDeleteModelProfile,
  adminUpdateCompanyBookingStatus,
  adminDeleteCompanyBooking,
  adminUpdateGalleryStatus,
  adminDeleteGalleryItem,
  adminPublishAnnouncement,
  adminRequestTestEmail,
  adminSubscribeTestEmailStatus,
  adminSubscribeModelsRealtime,
  adminFetchEmailAlertSettings,
  adminSaveEmailAlertSettings,
  adminFetchEmailAlerts,
  adminSubscribeEmailAlertsRealtime,
  adminLogEmailAlert,
  adminMarkEmailAlertRead,
  adminMarkAllEmailAlertsRead,
  fetchWeddingPackageConfigs,
  adminSaveWeddingPackageConfig,
  adminResetWeddingPackageConfig,
  initFirebaseClient,
  shouldShowManualFirebaseSetup,
  saveLocalDevFirebaseConfig,
  syncWorkshopNewsletterOptIn,
  fetchApprovedPublicProfessionals,
  submitProfessionalRegistration,
  fetchMyProfessionalProfiles,
  submitHiringRequest,
  fetchMyHiringRequests,
  fetchAssignedJobsForProfessional,
  respondToHiringProposal,
  cancelMyHiringRequest,
  submitOutsourcingRequest,
  fetchMyOutsourcingRequests,
  cancelMyOutsourcingRequest,
  adminFetchProfessionalNetworkData,
  adminUpdateProfessionalStatus,
  adminProposeProfessionalForHiring,
  adminUpdateHiringRequestStatus,
  adminUpdateOutsourcingRequest,
  fetchMyChatBlockStatus,
  adminSetChatUserBlocked,
  openOrCreatePrivateChatThread,
  sendPrivateChatMessage,
  markPrivateChatRead,
  markPrivateChatMessagesRead,
  subscribeMyPrivateChats,
  adminSubscribePrivateChats,
  subscribePrivateChatMessages,
  fetchMyUserProfile,
  fetchMyCustomerCatalogs,
  fetchMyCustomerSlots,
  adminFetchCustomerPortalData,
  adminAssignCustomerCatalog,
  adminDeleteCustomerCatalog,
  adminAssignCustomerSlot,
  adminUpdateCustomerSlotStatus,
  adminDeleteCustomerSlot,
  fetchStudioBookedSlots,
  fetchPaymentSettings,
  adminSavePaymentSettings,
  fetchMyBookingPayments,
  fetchMyUpiPaymentHistory,
  createModelAdvancePaymentRequest,
  createWeddingBookingPaymentRequest,
  submitCustomerPaymentUtr,
  requestCustomerPaymentRefund,
  adminFetchAllBookingPayments,
  adminSetBookingPaymentPricing,
  adminVerifyBookingPayment,
  adminRejectBookingPayment,
  adminProcessBookingPaymentRefund
} from "./firebase.js";
import {
  renderApprovedProfessionalsDirectoryHtml,
  renderMyProApplicationsHtml,
  renderMyHiringBookingsHtml
} from "./pro-network.js";
import {
  getUnreadIncomingMessageIds,
  countUnreadChatsForUser,
  countUnreadChatsForAdmin,
  renderUserPrivateChatHtml,
  renderAdminPrivateMessagesHtml
} from "./private-chat.js";
import {
  renderAdminProRegistrationsHtml,
  renderAdminHiringRequestsHtml,
  renderAdminOutsourcingRequestsHtml,
  renderAdminAssignmentsHtml,
  renderAdminBookingHistoryHtml
} from "./admin-pro-network.js";
import {
  CUSTOMER_PORTAL_TABS,
  extractPortalQueryParams,
  resolvePortalDeepLinkAccess,
  validateCustomerCatalogPayload,
  validateCustomerSlotPayload,
  renderCustomerPortalHtml,
  renderAdminCustomerPortalTabHtml
} from "./customer-portal.js";
import {
  buildBookedSlotIndex,
  getFirstAvailableTimeForDate,
  isStudioSlotBooked,
  renderBookingCalendarHtml
} from "./booking-calendar.js";
import {
  DEFAULT_PAYMENT_CONFIG,
  OFFICIAL_STUDIO_UPI_ID,
  buildPrintablePaymentReceiptHtml,
  formatInrAmount,
  isAllowedPaymentImageFileMeta,
  isDuplicateUtrError,
  isUploadedQrImageDataUrl,
  renderAdminPaymentDashboardHtml,
  renderDuplicateUtrAlertHtml,
  verifyQrCode
} from "./payment-system.js";
import {
  buildPrefilledUpiPaymentDetails,
  buildSafeUpiPayLink,
  createCameraQrScannerController,
  decodeQrImageFileForPayment,
  hasAcknowledgedQrCameraPermission,
  markQrCameraPermissionAcknowledged,
  parseUpiPaymentQrDetails,
  renderCameraPermissionNoticeHtml
} from "./qr-pay-scanner.js";

const state = {
  lang: localStorage.getItem("star_artist_lang") === "hi" ? "hi" : "en",
  theme: localStorage.getItem("star_artist_theme") === "light" ? "light" : "dark",
  route: "login",
  authMode: "login",
  postAuthRoute: "home",
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
  adminQrUploadState: { status: "idle", previewDataUrl: "", fileName: "", error: "", approxKb: 0 },
  adminGrowthChartType: "area",
  adminPaymentTrendsDays: PAYMENT_TRENDS_DEFAULT_DAYS,
  adminModelExperienceScope: "all",
  adminModelFilter: "pending",
  adminModelSearch: "",
  adminModelDrafts: {},
  adminSelectedModelIds: [],
  adminBulkModelNote: "",
  adminBulkUpdating: false,
  adminCompanyFilter: "all",
  adminCompanySearch: "",
  adminCompanyNoteDrafts: {},
  adminEmailPreview: {
    type: "booking",
    recordId: "__sample__",
    viewMode: "html",
    viewport: "desktop",
    overrides: {},
    testSend: {
      status: "idle",
      testId: "",
      recipient: STUDIO_CONTACT.adminEmail,
      messageId: "",
      smtpHost: "",
      smtpPort: "",
      error: ""
    }
  },
  adminEmailAlertSettings: { ...DEFAULT_ADMIN_EMAIL_ALERT_SETTINGS },
  adminEmailAlerts: [],
  adminEmailAlertFilter: "all",
  isSavingEmailAlertSettings: false,
  dispatchingEmailAlertKey: "",
  _knownBookingIds: null,
  _knownModelIds: null,
  publicModels: [],
  publicModelFilters: { ...DEFAULT_MODEL_DISCOVERY_FILTERS, skills: [] },
  activeProfileModelId: null,
  pendingSharedModelId: "",
  myCompanyProfile: null,
  myCompanyBookings: [],
  selectedRequestedModels: [],
  galleryItems: [],
  myBookings: [],
  queuedOfflineBookings: [],
  myModels: [],
  myNotifications: [],
  myUserProfile: null,
  myCustomerCatalogs: [],
  myCustomerSlots: [],
  portalTab: "my-catalog",
  portalDeniedNotice: "",
  portalHighlightedBookingId: "",
  adminPortalData: { catalogs: [], slots: [] },
  studioBookedSlots: [],
  bookingCalYear: new Date().getFullYear(),
  bookingCalMonth: new Date().getMonth(),
  paymentConfig: { ...DEFAULT_PAYMENT_CONFIG },
  myBookingPayments: [],
  activePaymentForm: "model",
  portalScannedUpiPayment: null,
  portalPaymentHistoryFilter: "all",
  portalPaymentHistoryLoading: false,
  portalPaymentHistoryError: "",
  adminBookingPayments: [],
  adminPaymentFilter: "all",
  modelRegPortfolioPhotos: [],
  editingModelId: null,
  editModelDraftPhotos: [],
  weddingPackageConfigs: [],
  savingWeddingTierId: null,
  selectedWeddingAddonIds: [],
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
  adminCsvExportStatus: "idle",
  adminData: { bookings: [], models: [], gallery: [], announcements: [], companies: [], companyBookings: [] },
  proTab: "join",
  proDirCategory: "all",
  proDirSearch: "",
  publicProfessionals: [],
  myProfessionals: [],
  myHiringRequests: [],
  myAssignedJobs: [],
  myOutsourcingRequests: [],
  proRegPhotos: [],
  hireRefPhotos: [],
  outRefPhotos: [],
  myChatThreads: [],
  activeUserChatId: "",
  activeUserChatMessages: [],
  userChatDraftText: "",
  userChatDraftImage: "",
  userChatSending: false,
  myChatBlockStatus: { isBlocked: false, reason: "" },
  _prevUserChatUnread: null,
  _prevAdminChatUnread: null,
  adminProData: { professionals: [], hiringRequests: [], outsourcingRequests: [], blockedMap: {} },
  adminProFilter: "pending",
  adminProSearch: "",
  adminHiringFilter: "all",
  adminChatThreads: [],
  activeAdminChatId: "",
  activeAdminChatMessages: [],
  adminChatDraftText: "",
  adminChatDraftImage: "",
  adminChatSending: false
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
  document.querySelectorAll("[data-i18n-placeholder]").forEach((el) => {
    const key = el.getAttribute("data-i18n-placeholder");
    if (key && TRANSLATIONS[state.lang]?.[key]) {
      el.placeholder = TRANSLATIONS[state.lang][key];
    }
  });
  const searchInput = document.getElementById("catalogSearchInput");
  if (searchInput) searchInput.placeholder = t("catalogSearchPlaceholder");

  const publicModelSearchInput = document.getElementById("publicModelsSearchInput");
  if (publicModelSearchInput) publicModelSearchInput.placeholder = t("companySearchPlaceholder");

  const castingCityInput = document.getElementById("castingCityInput");
  if (castingCityInput) castingCityInput.placeholder = t("castingCityPlaceholder");

  const castingMinAgeInput = document.getElementById("castingMinAge");
  if (castingMinAgeInput) castingMinAgeInput.placeholder = t("castingMinAgePlaceholder");

  const castingMaxAgeInput = document.getElementById("castingMaxAge");
  if (castingMaxAgeInput) castingMaxAgeInput.placeholder = t("castingMaxAgePlaceholder");

  if (state.activeProfileModelId) {
    renderActiveModelProfileModal();
  }

  const newsletterInput = document.getElementById("newsletterEmail");
  if (newsletterInput) newsletterInput.placeholder = t("newsletterEmailPlaceholder");

  const themeBtn = document.getElementById("themeToggleBtn");
  const themeLabel = document.getElementById("themeToggleLabel");
  const themeIcon = document.getElementById("themeToggleIcon");
  const nextThemeLabel = state.theme === "dark" ? t("themeLight") : t("themeDark");
  if (themeLabel) {
    themeLabel.textContent = nextThemeLabel;
  } else if (themeBtn) {
    themeBtn.textContent = nextThemeLabel;
  }
  if (themeIcon) {
    themeIcon.textContent = state.theme === "dark" ? "☀" : "☾";
  }

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

  renderBookingWeddingSummary();

  if (isAuthenticatedMember(state.user)) {
    renderProNetworkView();
    renderCustomerPortalView();
  }

  if (hasVerifiedAdminSession() && state.route === "admin") {
    renderAdminBookingTracker();
    renderAdminModelGrowthChart();
    renderAdminPaymentTrendsChart();
    renderAdminConsole();
  }
}

function applyTheme() {
  document.documentElement.setAttribute("data-theme", state.theme);
  const metaTheme = document.querySelector('meta[name="theme-color"]');
  if (metaTheme) {
    metaTheme.setAttribute("content", state.theme === "light" ? "#F8F9FA" : "#0C0C0C");
  }
  if (hasVerifiedAdminSession() && state.route === "admin") {
    renderAdminModelGrowthChart();
    renderAdminPaymentTrendsChart();
    if (state.adminTab === "models") {
      renderAdminModelExperienceChart();
    }
  }
}

function hasVerifiedAdminSession() {
  return Boolean(state.isAdmin && isAuthorizedAdminUser(state.user));
}

function setAuthGateAlert(message = "", { isInfo = false } = {}) {
  const alertEl = document.getElementById("authGateAlert");
  if (!alertEl) return;
  const clean = String(message || "").trim();
  alertEl.textContent = clean;
  alertEl.classList.toggle("hidden", !clean);
  alertEl.classList.toggle("is-info", Boolean(clean && isInfo));
}

function setAuthMode(mode = "login", { updateUrl = true, clearAlert = true, adminNotice = false } = {}) {
  const normalized = mode === "signup" ? "signup" : mode === "reset" ? "reset" : "login";
  state.authMode = normalized;
  if (clearAlert) {
    setAuthGateAlert("");
  }
  const adminGateHint = document.getElementById("authAdminGateHint");
  if (adminGateHint) {
    adminGateHint.classList.toggle("hidden", !adminNotice);
  }
  syncAuthGateDom(document, {
    isAuthenticated: isAuthenticatedMember(state.user),
    authMode: normalized,
    authResolved: state.authResolved
  });
  if (updateUrl && !isAuthenticatedMember(state.user)) {
    const targetHash = `#${normalized}`;
    if (window.location.hash !== targetHash) {
      history.replaceState(null, "", `/${targetHash}`);
    }
  }
}

function openAuthModal({ adminNotice = false, mode = "login" } = {}) {
  if (!isAuthenticatedMember(state.user)) {
    setAuthMode(mode, { updateUrl: true, clearAlert: false, adminNotice });
    window.scrollTo({ top: 0, behavior: "smooth" });
    return;
  }
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
  if (route !== "portal") {
    closePortalQrScannerModal();
  }
  const validRoutes = ["home", "portal", "catalog", "booking", "models", "companies", "network", "gallery", "admin"];
  const authenticated = isAuthenticatedMember(state.user);
  const isAdminNow = hasVerifiedAdminSession();
  const access = resolveAuthenticatedAccess({
    requestedRoute: route,
    pathname: window.location.pathname,
    hash: window.location.hash,
    search: window.location.search,
    user: state.user,
    isAdmin: isAdminNow,
    postAuthRoute: state.postAuthRoute
  });

  if (!access.authenticated) {
    if (
      access.postAuthRoute &&
      access.postAuthRoute !== "login" &&
      access.postAuthRoute !== "signup" &&
      access.postAuthRoute !== "reset"
    ) {
      state.postAuthRoute = access.postAuthRoute;
    }
    if (access.blockedAdminAttempt) {
      syncAdminUiVisibility(document, { isAdmin: false });
      state.pendingAdminRoute = true;
    }
    state.route = access.route;
    state.authMode = access.authMode;

    validRoutes.forEach((r) => {
      const sec = document.getElementById(`view-${r}`);
      if (!sec) return;
      sec.classList.add("hidden");
      sec.hidden = true;
      sec.setAttribute("aria-hidden", "true");
    });

    syncAuthGateDom(document, {
      isAuthenticated: false,
      authMode: access.authMode,
      authResolved: state.authResolved
    });

    const adminGateHint = document.getElementById("authAdminGateHint");
    if (adminGateHint) {
      adminGateHint.classList.toggle("hidden", !access.blockedAdminAttempt);
    }

    const authHash = `#${access.authMode}`;
    if (access.shouldNormalizeUrl || window.location.hash !== authHash) {
      history.replaceState(null, "", `/${authHash}`);
    }

    if (
      !silentBlock &&
      state.authResolved &&
      route &&
      route !== "login" &&
      route !== "signup" &&
      route !== "reset"
    ) {
      showToast(access.blockedAdminAttempt ? t("adminSignInRequiredToast") : t("authMustSignInToast"));
    }
    return;
  }

  syncAuthGateDom(document, {
    isAuthenticated: true,
    authMode: state.authMode,
    authResolved: state.authResolved
  });

  if (access.blockedAdminAttempt) {
    syncAdminUiVisibility(document, { isAdmin: false });
    if (access.shouldNormalizeUrl) {
      history.replaceState(null, "", "/#home");
    }
    if (!silentBlock && state.authResolved) {
      showToast(t("adminAccessDeniedToast"));
    }
  }

  const target = access.route;
  state.route = target;
  state.postAuthRoute = target;
  const preservedModelId =
    target === "companies" ? (state.activeProfileModelId || state.pendingSharedModelId) : "";
  const desiredHash = preservedModelId
    ? `#companies?model=${encodeURIComponent(preservedModelId)}`
    : `#${target}`;
  if (access.shouldNormalizeUrl || window.location.hash !== desiredHash) {
    history.replaceState(null, "", `/${desiredHash}`);
  }

  validRoutes.forEach((r) => {
    const sec = document.getElementById(`view-${r}`);
    if (!sec) return;
    const show = authenticated && r === target && (r !== "admin" || isAdminNow);
    sec.classList.toggle("hidden", !show);
    sec.hidden = !show;
    sec.setAttribute("aria-hidden", show ? "false" : "true");
  });

  document.querySelectorAll("[data-route]").forEach((btn) => {
    btn.classList.toggle("active", btn.getAttribute("data-route") === target);
  });

  if (target === "portal") {
    renderCustomerPortalView({ checkUrlDeepLink: true });
  }
  if (target === "network") {
    renderProNetworkView();
  }
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
        <button type="button" class="btn-gold" style="margin-top:auto;" data-book-service="${escapeHtml(getServiceTierBookingValue(pkg, tier))}">
          ${escapeHtml(t("bookTierBtn"))}
        </button>
      </article>`;
    })
    .join("");

  initLazyBlurImages(container);
}

function getEffectiveStudioServices() {
  return mergeWeddingTierConfigs(STUDIO_SERVICES, state.weddingPackageConfigs);
}

function renderCatalog() {
  const effectiveServices = getEffectiveStudioServices();
  const q = state.catalogSearch.trim().toLowerCase();
  const catCounts = countCatalogByCategory(effectiveServices, q);
  document.querySelectorAll("[data-cat-count]").forEach((badge) => {
    const key = badge.getAttribute("data-cat-count");
    if (key && Object.prototype.hasOwnProperty.call(catCounts, key)) {
      badge.textContent = String(catCounts[key]);
      badge.setAttribute("aria-label", `${catCounts[key]} items`);
    }
  });

  const filtered = effectiveServices.filter((pkg) => {
    const catOk = state.catalogCategory === "all" || pkg.category === state.catalogCategory;
    const tierNames = (pkg.tiers || []).map((tr) => `${tr.tierNameEn || ""} ${tr.tierNameHi || ""}`).join(" ");
    const text = `${pkg.titleEn} ${pkg.titleHi} ${pkg.summaryEn} ${pkg.tagEn} ${pkg.recommendedServiceType} ${tierNames}`.toLowerCase();
    return catOk && (!q || text.includes(q));
  });
  renderServiceCards("homeFeaturedServicesGrid", effectiveServices.filter((p) => p.isPopular).slice(0, 3));
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

function renderModelRegPortfolioPreview() {
  const el = document.getElementById("modelRegPortfolioPreview");
  if (!el) return;
  el.innerHTML = renderPortfolioPhotoStripHtml(state.modelRegPortfolioPhotos, {
    editable: true,
    scope: "reg",
    t,
    escapeHtml
  });
}

function renderMyModels() {
  const el = document.getElementById("myModelsList");
  if (!el) return;
  const emailInput = document.getElementById("modelContactEmail");
  if (emailInput && !emailInput.value && state.user?.email) {
    emailInput.value = state.user.email;
  }
  el.innerHTML = renderModelDashboardHtml({
    user: state.user,
    models: state.myModels,
    notifications: state.myNotifications,
    editingModelId: state.editingModelId,
    editDraftPhotos: state.editModelDraftPhotos,
    t,
    escapeHtml
  });
  renderModelRegPortfolioPreview();
}

function syncCastingLocationControls() {
  const stateSelect = document.getElementById("castingStateSelect");
  if (stateSelect) {
    const states = getAvailableStatesFromModels(state.publicModels);
    const currentVal = state.publicModelFilters.state || "all";
    stateSelect.innerHTML = [
      `<option value="all">${escapeHtml(t("castingAllStates"))}</option>`,
      ...states.map((st) => `<option value="${escapeHtml(st)}">${escapeHtml(st)}</option>`)
    ].join("");
    stateSelect.value = states.includes(currentVal) ? currentVal : "all";
  }

  const cityDatalist = document.getElementById("castingCityDatalist");
  if (cityDatalist) {
    const cities = getAvailableCitiesFromModels(state.publicModels, state.publicModelFilters.state);
    cityDatalist.innerHTML = cities.map((c) => `<option value="${escapeHtml(c)}"></option>`).join("");
  }
}

function renderPublicModels() {
  const grid = document.getElementById("publicModelsGrid");
  if (!grid) return;

  syncCastingLocationControls();

  const visible = filterPublicModels(state.publicModels, state.publicModelFilters);
  const selectedIds = state.selectedRequestedModels.map((m) => m.id);
  grid.innerHTML = renderPublicModelPortfolioCardsHtml({
    models: visible,
    selectedModelIds: selectedIds,
    t,
    escapeHtml
  });

  const countEl = document.getElementById("castingResultsCount");
  if (countEl) {
    countEl.textContent = t("castingResultsLabel")
      .replace("{count}", String(visible.length))
      .replace("{total}", String(state.publicModels.length));
  }

  const resetBtn = document.getElementById("castingResetFiltersBtn");
  if (resetBtn) {
    const activeCount = countActiveDiscoveryFilters(state.publicModelFilters);
    resetBtn.textContent = activeCount > 0 ? `${t("castingResetFiltersBtn")} (${activeCount})` : t("castingResetFiltersBtn");
    resetBtn.disabled = activeCount === 0;
  }

  document.querySelectorAll("[data-public-model-gender]").forEach((btn) => {
    const isActive = (btn.getAttribute("data-public-model-gender") || "all") === state.publicModelFilters.gender;
    btn.classList.toggle("active", isActive);
    btn.setAttribute("aria-pressed", isActive ? "true" : "false");
  });

  const activeSkills = new Set(state.publicModelFilters.skills || []);
  document.querySelectorAll("[data-casting-skill]").forEach((btn) => {
    const skill = btn.getAttribute("data-casting-skill") || "";
    const isActive = activeSkills.has(skill);
    btn.classList.toggle("active", isActive);
    btn.setAttribute("aria-pressed", isActive ? "true" : "false");
  });
}

function renderActiveModelProfileModal() {
  const body = document.getElementById("modelProfileModalBody");
  if (!body || !state.activeProfileModelId) return;
  const model = state.publicModels.find((m) => m.id === state.activeProfileModelId);
  if (!model) {
    closeModelProfileModal();
    return;
  }
  const isSelected = state.selectedRequestedModels.some((m) => m.id === model.id);
  body.innerHTML = renderModelProfileModalHtml(model, {
    isSelected,
    t,
    escapeHtml
  });
}

function openModelProfileModal(modelId, { updateUrl = true } = {}) {
  const modal = document.getElementById("modelProfileModal");
  if (!modal) return;
  const model = state.publicModels.find((m) => m.id === modelId);
  if (!model) return;
  state.activeProfileModelId = model.id;
  state.pendingSharedModelId = "";
  renderActiveModelProfileModal();
  modal.classList.remove("hidden");
  if (updateUrl && state.route === "companies") {
    const shareHash = `#companies?model=${encodeURIComponent(model.id)}`;
    if (window.location.hash !== shareHash) {
      history.replaceState(null, "", `/${shareHash}`);
    }
  }
}

function closeModelProfileModal() {
  state.activeProfileModelId = null;
  state.pendingSharedModelId = "";
  document.getElementById("modelProfileModal")?.classList.add("hidden");
  if (window.location.hash.includes("?model=")) {
    history.replaceState(null, "", `/#${state.route || "companies"}`);
  }
}

function tryOpenPendingSharedModelProfile() {
  const targetId = String(state.pendingSharedModelId || "").trim();
  if (!targetId) return false;
  const match = state.publicModels.find((m) => m.id === targetId);
  if (!match) return false;
  if (state.route !== "companies") {
    navigateTo("companies", { silentBlock: true });
  }
  openModelProfileModal(match.id, { updateUrl: true });
  return true;
}

async function copyModelProfileLink(modelId, modelName, triggerBtn = null) {
  const shareUrl = buildModelProfileShareUrl(modelId, {
    origin: window.location.origin
  });
  if (!shareUrl) return;

  let copied = false;
  if (navigator.clipboard && typeof navigator.clipboard.writeText === "function") {
    try {
      await navigator.clipboard.writeText(shareUrl);
      copied = true;
    } catch {
      copied = false;
    }
  }

  if (!copied) {
    try {
      const textarea = document.createElement("textarea");
      textarea.value = shareUrl;
      textarea.setAttribute("readonly", "");
      textarea.style.position = "fixed";
      textarea.style.top = "-9999px";
      textarea.style.opacity = "0";
      document.body.appendChild(textarea);
      textarea.select();
      textarea.setSelectionRange(0, shareUrl.length);
      copied = document.execCommand("copy");
      document.body.removeChild(textarea);
    } catch {
      copied = false;
    }
  }

  const resolvedName =
    modelName ||
    state.publicModels.find((m) => m.id === modelId)?.fullName ||
    "Roster Model";

  if (copied) {
    showToast(t("castingShareCopiedToast").replace("{name}", resolvedName));
    if (triggerBtn) {
      triggerBtn.classList.add("is-copied");
      const labelEl = triggerBtn.querySelector(".share-btn-label");
      if (labelEl) {
        labelEl.textContent = `✓ ${t("castingShareCopiedBtn")}`;
      }
      if (triggerBtn._copiedResetTimer) {
        clearTimeout(triggerBtn._copiedResetTimer);
      }
      triggerBtn._copiedResetTimer = setTimeout(() => {
        triggerBtn.classList.remove("is-copied");
        const activeLabel = triggerBtn.querySelector(".share-btn-label");
        if (activeLabel) {
          activeLabel.textContent = t("castingShareBtn");
        }
      }, 2200);
    }
  } else {
    showToast(t("castingShareFailedToast"));
  }
}

function resetModelDiscoveryFilters() {
  state.publicModelFilters = { ...DEFAULT_MODEL_DISCOVERY_FILTERS, skills: [] };
  const setVal = (id, val) => {
    const el = document.getElementById(id);
    if (el) el.value = val;
  };
  setVal("publicModelsSearchInput", "");
  setVal("castingStateSelect", "all");
  setVal("castingCityInput", "");
  setVal("castingMinAge", "");
  setVal("castingMaxAge", "");
  setVal("castingMinHeightSelect", "");
  setVal("castingMaxHeightSelect", "");
  setVal("castingExperienceSelect", "all");
  const portfolioCb = document.getElementById("castingPortfolioOnlyCheckbox");
  if (portfolioCb) portfolioCb.checked = false;
  renderPublicModels();
}

function startCastingInquiryForModel(modelId, fullName) {
  const model = state.publicModels.find((m) => m.id === modelId);
  const resolvedName = fullName || model?.fullName || "Roster Model";
  if (!state.selectedRequestedModels.some((m) => m.id === modelId)) {
    if (state.selectedRequestedModels.length >= 20) {
      showToast("You can attach up to 20 specific models per request.");
      return;
    }
    state.selectedRequestedModels.push({ id: modelId, fullName: resolvedName });
  }
  if (model && state.selectedRequestedModels.length === 1) {
    const reqSel = document.getElementById("compBookRequirement");
    if (reqSel) {
      const g = String(model.gender || "").toLowerCase();
      if (g === "female") reqSel.value = "Female Models";
      else if (g === "male") reqSel.value = "Male Models";
    }
  }
  closeModelProfileModal();
  renderPublicModels();
  renderSelectedRequestedModels();
  showToast(t("castingInquiryAddedToast").replace("{name}", resolvedName));
  document.getElementById("companyBookingForm")?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function startAuditionRequestForModel(modelId, fullName) {
  const model = state.publicModels.find((m) => m.id === modelId);
  const resolvedName = fullName || model?.fullName || "Roster Model";
  if (!state.selectedRequestedModels.some((m) => m.id === modelId)) {
    if (state.selectedRequestedModels.length < 20) {
      state.selectedRequestedModels.push({ id: modelId, fullName: resolvedName });
    }
  }
  if (model && state.selectedRequestedModels.length === 1) {
    const reqSel = document.getElementById("compBookRequirement");
    if (reqSel) {
      const g = String(model.gender || "").toLowerCase();
      if (g === "female") reqSel.value = "Female Models";
      else if (g === "male") reqSel.value = "Male Models";
    }
  }
  const detailsEl = document.getElementById("compBookDetails");
  if (detailsEl) {
    const auditionHeader = `[AUDITION REQUEST] Requesting audition & look-test for approved model: ${resolvedName}${model?.city ? ` (${model.city})` : ""}. Please share audition slot availability.`;
    if (!detailsEl.value.includes("[AUDITION REQUEST]") || !detailsEl.value.includes(resolvedName)) {
      detailsEl.value = detailsEl.value.trim() ? `${auditionHeader}\n${detailsEl.value.trim()}` : auditionHeader;
    }
  }
  closeModelProfileModal();
  renderPublicModels();
  renderSelectedRequestedModels();
  showToast(t("castingAuditionPrefillToast").replace("{name}", resolvedName));
  document.getElementById("companyBookingForm")?.scrollIntoView({ behavior: "smooth", block: "start" });
  document.getElementById("compBookEventDate")?.focus();
}

function startShootBookingForModel(modelId, fullName) {
  const model = state.publicModels.find((m) => m.id === modelId);
  const resolvedName = fullName || model?.fullName || "Roster Model";
  closeModelProfileModal();
  setRoute("booking");
  const notesEl = document.getElementById("bookingNotes");
  if (notesEl) {
    const locationStr = model ? [model.cityName || model.city, model.state].filter(Boolean).join(", ") : "";
    const briefLine = `Casting Shoot Booking Request — Approved Model: ${resolvedName}${locationStr ? ` (${locationStr})` : ""}${model?.category ? ` • ${model.category}` : ""}`;
    if (!notesEl.value.includes(resolvedName)) {
      notesEl.value = notesEl.value.trim() ? `${briefLine}\n${notesEl.value.trim()}` : briefLine;
    }
  }
  showToast(t("castingShootPrefillToast").replace("{name}", resolvedName));
  document.getElementById("bookingDate")?.focus();
}

function renderSelectedRequestedModels() {
  const bar = document.getElementById("selectedRequestedModelsBar");
  if (!bar) return;
  bar.innerHTML = renderSelectedRequestedModelsHtml({
    selectedModels: state.selectedRequestedModels,
    t,
    escapeHtml
  });
}

function prefillCompanyFormsFromProfile(profile) {
  const badge = document.getElementById("companyProfileStatusBadge");
  if (badge) {
    if (profile?.companyName) {
      badge.textContent = `${t("companyProfileRegisteredBadge")} • ${profile.companyName}`;
      badge.classList.remove("hidden");
    } else {
      badge.classList.add("hidden");
    }
  }
  if (!profile) return;

  const setIfPresent = (id, val) => {
    const el = document.getElementById(id);
    if (el && val) el.value = val;
  };
  setIfPresent("compRegName", profile.companyName);
  setIfPresent("compRegType", profile.organizationType);
  setIfPresent("compRegContactPerson", profile.contactPerson);
  setIfPresent("compRegMobile", profile.mobileNumber);
  setIfPresent("compRegEmail", profile.email);
  setIfPresent("compRegAddress", profile.address);

  setIfPresent("compBookName", profile.companyName);
  setIfPresent("compBookType", profile.organizationType);
  setIfPresent("compBookContactPerson", profile.contactPerson);
  setIfPresent("compBookMobile", profile.mobileNumber);
  setIfPresent("compBookEmail", profile.email);
  setIfPresent("compBookAddress", profile.address);
}

function renderMyCompanyBookings() {
  const list = document.getElementById("myCompanyBookingsList");
  if (!list) return;
  list.innerHTML = renderMyCompanyBookingsHtml({
    requests: state.myCompanyBookings,
    signedIn: Boolean(state.user),
    t,
    escapeHtml
  });
}

function renderAdminModelGrowthChart() {
  const chartHost = document.getElementById("adminModelGrowthChartMount");
  if (!hasVerifiedAdminSession()) {
    unmountModelGrowthChart(chartHost);
    return;
  }

  document.querySelectorAll("[data-growth-chart-type]").forEach((btn) => {
    const isActive = btn.getAttribute("data-growth-chart-type") === state.adminGrowthChartType;
    btn.classList.toggle("active", isActive);
    btn.setAttribute("aria-pressed", isActive ? "true" : "false");
  });

  mountModelGrowthChart(chartHost, {
    models: state.adminData.models,
    metricsEl: document.getElementById("adminModelGrowthMetrics"),
    statusEl: document.getElementById("adminModelGrowthStatus"),
    chartType: state.adminGrowthChartType,
    theme: state.theme,
    lang: state.lang,
    t,
    escapeHtml
  });
}

function renderAdminPaymentTrendsChart() {
  const chartHost = document.getElementById("adminPaymentTrendsChartMount");
  if (!hasVerifiedAdminSession()) {
    unmountPaymentTrendsChart(chartHost);
    return;
  }

  const activeDays = Number(state.adminPaymentTrendsDays) || PAYMENT_TRENDS_DEFAULT_DAYS;
  document.querySelectorAll("[data-payment-trends-days]").forEach((btn) => {
    const isActive = Number(btn.getAttribute("data-payment-trends-days")) === activeDays;
    btn.classList.toggle("active", isActive);
    btn.setAttribute("aria-pressed", isActive ? "true" : "false");
  });

  mountPaymentTrendsChart(chartHost, {
    payments: state.adminBookingPayments || [],
    metricsEl: document.getElementById("adminPaymentTrendsMetrics"),
    statusEl: document.getElementById("adminPaymentTrendsStatus"),
    days: activeDays,
    theme: state.theme,
    lang: state.lang,
    t,
    escapeHtml
  });
}

function renderAdminModelExperienceChart() {
  const chartHost = document.getElementById("adminModelExperienceChartMount");
  if (!hasVerifiedAdminSession()) {
    unmountModelExperienceChart(chartHost);
    return;
  }

  document.querySelectorAll("[data-exp-chart-scope]").forEach((btn) => {
    const isActive = btn.getAttribute("data-exp-chart-scope") === state.adminModelExperienceScope;
    btn.classList.toggle("active", isActive);
    btn.setAttribute("aria-pressed", isActive ? "true" : "false");
  });

  if (!chartHost) return;

  mountModelExperienceChart(chartHost, {
    models: state.adminData.models,
    summaryEl: document.getElementById("adminModelExperienceSummary"),
    statusEl: document.getElementById("adminModelExperienceStatus"),
    scope: state.adminModelExperienceScope,
    theme: state.theme,
    t,
    escapeHtml
  });
}

let adminBookingsUnsubscribe = null;
let adminModelsUnsubscribe = null;
let adminEmailAlertsUnsubscribe = null;

const adminBookingTracker = createRealtimeBookingTracker({
  getBookings: () => state.adminData.bookings,
  onAutoComplete: async (dueBookings) => {
    if (!hasVerifiedAdminSession()) {
      return { succeededIds: [], failedIds: [] };
    }
    const ids = dueBookings.map((b) => b.id);
    const res = await adminAutoCompleteExpiredBookings(ids);
    if (res.succeededIds.length > 0) {
      const doneSet = new Set(res.succeededIds);
      state.adminData.bookings = state.adminData.bookings.map((b) =>
        doneSet.has(b.id) ? { ...b, status: "Completed" } : b
      );
      showToast(t("adminTrackerAutoCompletedToast").replace("{count}", String(res.succeededIds.length)));
      if (state.route === "admin") {
        renderAdminConsole();
      }
    }
    return res;
  },
  onStateChange: () => {
    if (hasVerifiedAdminSession() && state.route === "admin") {
      renderAdminBookingTracker();
    }
  },
  intervalMs: 60000
});

function stopAdminRealtimeTracker() {
  if (typeof adminBookingsUnsubscribe === "function") {
    try {
      adminBookingsUnsubscribe();
    } catch {
      // Ignore cleanup error
    }
    adminBookingsUnsubscribe = null;
  }
  if (typeof adminModelsUnsubscribe === "function") {
    try {
      adminModelsUnsubscribe();
    } catch {
      // Ignore cleanup error
    }
    adminModelsUnsubscribe = null;
  }
  if (typeof adminEmailAlertsUnsubscribe === "function") {
    try {
      adminEmailAlertsUnsubscribe();
    } catch {
      // Ignore cleanup error
    }
    adminEmailAlertsUnsubscribe = null;
  }
  stopAdminChatSubscriptions();
  state._knownBookingIds = null;
  state._knownModelIds = null;
  state._prevAdminChatUnread = null;
  adminBookingTracker.stop();
}

async function handleLiveAdminSubmissionAlerts(type, newItems = []) {
  if (!hasVerifiedAdminSession() || !Array.isArray(newItems) || !newItems.length) return;
  const cfg = normalizeAdminEmailAlertSettings(state.adminEmailAlertSettings);
  const enabled = type === "model" ? cfg.modelAlertsEnabled : cfg.bookingAlertsEnabled;
  if (!enabled) return;

  for (const item of newItems) {
    const recordId = String(item?.id || "").trim();
    if (!recordId) continue;
    const alreadyLogged = (state.adminEmailAlerts || []).some(
      (a) => a?.type === type && String(a?.recordId || "") === recordId
    );
    const alertRecord = buildAdminEmailAlertRecord({
      type,
      record: item,
      status: "queued",
      source: "dashboard-live"
    });
    if (!alreadyLogged) {
      try {
        await adminLogEmailAlert(alertRecord);
      } catch (err) {
        console.warn("Live admin alert log warning:", err?.message || err);
      }
    }
    if (cfg.browserAlertsEnabled) {
      const displayName = type === "model" ? item.fullName || "Applicant" : item.clientName || "Client";
      const toastKey = type === "model" ? "adminEmailAlertLiveModelToast" : "adminEmailAlertLiveBookingToast";
      showToast(t(toastKey).replace("{name}", displayName));
    }
  }
}

async function ensureAdminRealtimeTracker() {
  if (!hasVerifiedAdminSession()) {
    stopAdminRealtimeTracker();
    return;
  }
  adminBookingTracker.start();
  if (!adminBookingsUnsubscribe && state.firebaseReady) {
    try {
      adminBookingsUnsubscribe = await adminSubscribeBookingsRealtime({
        onBookings: (liveBookings) => {
          if (!hasVerifiedAdminSession()) {
            stopAdminRealtimeTracker();
            return;
          }
          const detected = detectNewAdminAlertItems({
            prevIds: state._knownBookingIds,
            items: liveBookings
          });
          state._knownBookingIds = detected.nextIds;
          state.adminData.bookings = liveBookings;
          adminBookingTracker.setLiveConnected(true);
          if (!detected.initialized && detected.newItems.length > 0) {
            handleLiveAdminSubmissionAlerts("booking", detected.newItems);
          }
          if (state.route === "admin") {
            renderAdminConsole();
          }
          adminBookingTracker.evaluateNow();
        },
        onError: (err) => {
          adminBookingTracker.setLiveConnected(false);
          console.warn("Realtime bookings tracker listener warning:", err?.message || err);
        }
      });
    } catch (err) {
      adminBookingTracker.setLiveConnected(false);
      console.warn("Unable to start real-time bookings listener:", err?.message || err);
    }
  }

  if (!adminModelsUnsubscribe && state.firebaseReady) {
    try {
      adminModelsUnsubscribe = await adminSubscribeModelsRealtime({
        onModels: (liveModels) => {
          if (!hasVerifiedAdminSession()) {
            stopAdminRealtimeTracker();
            return;
          }
          const detected = detectNewAdminAlertItems({
            prevIds: state._knownModelIds,
            items: liveModels
          });
          state._knownModelIds = detected.nextIds;
          state.adminData.models = liveModels;
          if (!detected.initialized && detected.newItems.length > 0) {
            handleLiveAdminSubmissionAlerts("model", detected.newItems);
          }
          if (state.route === "admin") {
            renderAdminConsole();
          }
        },
        onError: (err) => {
          console.warn("Realtime models alert listener warning:", err?.message || err);
        }
      });
    } catch (err) {
      console.warn("Unable to start real-time models alert listener:", err?.message || err);
    }
  }

  if (!adminEmailAlertsUnsubscribe && state.firebaseReady) {
    try {
      adminEmailAlertsUnsubscribe = await adminSubscribeEmailAlertsRealtime({
        onAlerts: (liveAlerts) => {
          if (!hasVerifiedAdminSession()) {
            stopAdminRealtimeTracker();
            return;
          }
          state.adminEmailAlerts = liveAlerts;
          if (state.route === "admin") {
            renderAdminConsole();
          }
        },
        onError: (err) => {
          console.warn("Realtime email alerts listener warning:", err?.message || err);
        }
      });
    } catch (err) {
      console.warn("Unable to start real-time email alerts listener:", err?.message || err);
    }
  }

  await ensureAdminChatSubscription();
}

function renderAdminBookingTracker() {
  const host = document.getElementById("adminBookingTrackerCard");
  if (!host) return;
  if (!hasVerifiedAdminSession()) {
    host.innerHTML = "";
    return;
  }
  host.innerHTML = renderAdminBookingTrackerHtml({
    bookings: state.adminData.bookings,
    trackerState: adminBookingTracker.getSnapshot(),
    lang: state.lang,
    t,
    escapeHtml
  });
}

async function loadAdminConsole() {
  if (!hasVerifiedAdminSession()) {
    stopAdminRealtimeTracker();
    state.adminData = { bookings: [], models: [], gallery: [], announcements: [], companies: [], companyBookings: [] };
    state.adminEmailAlerts = [];
    unmountModelGrowthChart(document.getElementById("adminModelGrowthChartMount"));
    unmountPaymentTrendsChart(document.getElementById("adminPaymentTrendsChartMount"));
    unmountModelExperienceChart(document.getElementById("adminModelExperienceChartMount"));
    syncAdminUiVisibility(document, { isAdmin: false });
    if (state.route === "admin") {
      navigateTo("home", { silentBlock: true });
    }
    return;
  }
  syncAdminUiVisibility(document, { isAdmin: true, adminEmail: state.user?.email || "" });

  try {
    const [dashData, weddingConfigs, alertSettings, alertLogs, proNetData, portalAdminData, payConfig, allPayments] = await Promise.all([
      adminFetchDashboardData(),
      fetchWeddingPackageConfigs(),
      adminFetchEmailAlertSettings(),
      adminFetchEmailAlerts(),
      adminFetchProfessionalNetworkData().catch(() => ({
        professionals: [],
        hiringRequests: [],
        outsourcingRequests: [],
        blockedMap: {}
      })),
      adminFetchCustomerPortalData().catch(() => ({
        catalogs: [],
        slots: []
      })),
      fetchPaymentSettings({ throwOnError: true }).catch(() => state.paymentConfig || { ...DEFAULT_PAYMENT_CONFIG }),
      adminFetchAllBookingPayments().catch(() => [])
    ]);
    state.adminData = dashData;
    state.adminProData = proNetData;
    state.adminPortalData = portalAdminData;
    state.paymentConfig = payConfig;
    state.adminBookingPayments = Array.isArray(allPayments) ? allPayments : [];
    state.weddingPackageConfigs = weddingConfigs;
    state.adminEmailAlertSettings = normalizeAdminEmailAlertSettings(alertSettings);
    state.adminEmailAlerts = Array.isArray(alertLogs) ? alertLogs : [];
    if (!(state._knownBookingIds instanceof Set)) {
      state._knownBookingIds = new Set((dashData.bookings || []).map((b) => String(b?.id || "")).filter(Boolean));
    }
    if (!(state._knownModelIds instanceof Set)) {
      state._knownModelIds = new Set((dashData.models || []).map((m) => String(m?.id || "")).filter(Boolean));
    }
    renderCatalog();
    populateBookingServicesDropdown();
    renderAdminConsole();
    await ensureAdminRealtimeTracker();
    await adminBookingTracker.evaluateNow();
  } catch (err) {
    showToast(err.message || "Unable to load Admin Dashboard");
  }
}

function renderAdminConsole() {
  if (!hasVerifiedAdminSession()) {
    stopAdminRealtimeTracker();
    unmountModelGrowthChart(document.getElementById("adminModelGrowthChartMount"));
    unmountPaymentTrendsChart(document.getElementById("adminPaymentTrendsChartMount"));
    unmountModelExperienceChart(document.getElementById("adminModelExperienceChartMount"));
    syncAdminUiVisibility(document, { isAdmin: false });
    return;
  }
  const companyRequests = Array.isArray(state.adminData.companyBookings) ? state.adminData.companyBookings : [];
  const registeredCompanies = Array.isArray(state.adminData.companies) ? state.adminData.companies : [];
  const stats = document.getElementById("adminStatsGrid");
  if (stats) {
    stats.innerHTML = `
      <div class="studio-card"><div class="stat-value">${state.adminData.bookings.length}</div><div class="stat-label">Total Bookings</div></div>
      <div class="studio-card"><div class="stat-value">${state.adminData.models.length}</div><div class="stat-label">18+ Models</div></div>
      <div class="studio-card"><div class="stat-value">${companyRequests.length}</div><div class="stat-label">Company Requests</div></div>
      <div class="studio-card"><div class="stat-value">${state.adminData.gallery.length}</div><div class="stat-label">Gallery Submissions</div></div>
      <div class="studio-card"><div class="stat-value">${state.adminData.announcements.length}</div><div class="stat-label">Announcements</div></div>`;
  }

  renderAdminBookingTracker();
  renderAdminModelGrowthChart();
  renderAdminPaymentTrendsChart();

  const emailAlertSummary = summarizeAdminEmailAlerts({
    alerts: state.adminEmailAlerts,
    settings: state.adminEmailAlertSettings
  });
  const proList = Array.isArray(state.adminProData?.professionals) ? state.adminProData.professionals : [];
  const hiringList = Array.isArray(state.adminProData?.hiringRequests) ? state.adminProData.hiringRequests : [];
  const outsourcingList = Array.isArray(state.adminProData?.outsourcingRequests) ? state.adminProData.outsourcingRequests : [];
  const assignmentsCount =
    hiringList.filter((h) => h.assignedProfessionalId || h.assignedProfessionalName).length +
    outsourcingList.filter((o) => o.assignedProfessionalId || o.assignedProfessionalName).length;
  const adminChatUnread = countUnreadChatsForAdmin(state.adminChatThreads);
  const adminCounts = {
    bookings: state.adminData.bookings.length,
    models: state.adminData.models.length,
    companies: companyRequests.length,
    gallery: state.adminData.gallery.length,
    announcements: state.adminData.announcements.length,
    "wedding-packages": 3,
    "email-alerts": emailAlertSummary.unread > 0 ? emailAlertSummary.unread : emailAlertSummary.total,
    "pro-registrations": proList.length,
    "pro-hiring": hiringList.length,
    "pro-outsourcing": outsourcingList.length,
    "pro-assignments": assignmentsCount,
    "private-messages": adminChatUnread > 0 ? adminChatUnread : state.adminChatThreads.length,
    "pro-booking-history": state.adminData.bookings.length + hiringList.length + outsourcingList.length,
    "customer-portal":
      (Array.isArray(state.adminPortalData?.catalogs) ? state.adminPortalData.catalogs.length : 0) +
      (Array.isArray(state.adminPortalData?.slots) ? state.adminPortalData.slots.length : 0),
    payments: Array.isArray(state.adminBookingPayments) ? state.adminBookingPayments.length : 0
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

  // Always unmount any active React PieChart root before replacing #adminTabBody HTML
  unmountModelExperienceChart(document.getElementById("adminModelExperienceChartMount"));

  if (state.adminTab === "bookings") {
    const toolbarHtml = renderAdminBookingsToolbarHtml({
      count: state.adminData.bookings.length,
      status: state.adminCsvExportStatus,
      t,
      escapeHtml
    });
    const bookingsCardsHtml = state.adminData.bookings.length
      ? state.adminData.bookings
          .map(
            (b) => `
      <div class="studio-card">
        <div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:8px;align-items:center;">
          <strong>${escapeHtml(b.clientName)} — ${escapeHtml(b.serviceType)}</strong>
          <div style="display:flex;gap:6px;flex-wrap:wrap;align-items:center;">
            ${renderBookingTimelineChipHtml(b, { lang: state.lang, t, escapeHtml })}
            <span class="badge status-${String(b.status || "pending").toLowerCase()}">${escapeHtml(b.status)}</span>
          </div>
        </div>
        <div style="font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(b.preferredDate)} ${escapeHtml(b.preferredTime || "")} • ${escapeHtml(b.contactNumber)}</div>
        <div style="display:flex;flex-wrap:wrap;gap:8px;">
          <button type="button" class="chip-btn" data-adm-bk-id="${escapeHtml(b.id)}" data-adm-bk-st="Approved">Approve</button>
          <button type="button" class="chip-btn" data-adm-bk-id="${escapeHtml(b.id)}" data-adm-bk-st="Confirmed">Confirm</button>
          <button type="button" class="chip-btn" data-adm-bk-id="${escapeHtml(b.id)}" data-adm-bk-st="Rejected">Reject</button>
          <button type="button" class="chip-btn" data-adm-bk-id="${escapeHtml(b.id)}" data-adm-bk-st="Completed">Complete</button>
          <button type="button" class="chip-btn" data-adm-bk-id="${escapeHtml(b.id)}" data-adm-bk-st="Cancelled">Cancel</button>
          <button type="button" class="chip-btn" data-open-email-preview-type="booking" data-open-email-preview-id="${escapeHtml(b.id)}">${escapeHtml(t("adminEmailPreviewQuickBtn"))}</button>
          <button type="button" class="btn-danger" data-adm-bk-del="${escapeHtml(b.id)}">Delete</button>
        </div>
      </div>`
          )
          .join("")
      : `<div class="studio-card"><p style="margin:0;color:var(--text-secondary);">${escapeHtml(t("adminNoBookingsDesc"))}</p></div>`;
    body.innerHTML = `${toolbarHtml}${bookingsCardsHtml}`;
  } else if (state.adminTab === "models") {
    const validIds = new Set((state.adminData.models || []).map((m) => String(m?.id || "")));
    state.adminSelectedModelIds = (state.adminSelectedModelIds || []).filter((id) => validIds.has(String(id)));
    body.innerHTML = renderAdminModelsSectionHtml({
      models: state.adminData.models,
      filter: state.adminModelFilter,
      searchQuery: state.adminModelSearch,
      drafts: state.adminModelDrafts,
      experienceScope: state.adminModelExperienceScope,
      selectedIds: state.adminSelectedModelIds,
      bulkNote: state.adminBulkModelNote,
      bulkBusy: state.adminBulkUpdating,
      lang: state.lang,
      t
    });
    renderAdminModelExperienceChart();
  } else if (state.adminTab === "companies") {
    body.innerHTML = renderAdminCompanyRequestsSectionHtml({
      requests: companyRequests,
      companies: registeredCompanies,
      filter: state.adminCompanyFilter,
      searchQuery: state.adminCompanySearch,
      drafts: state.adminCompanyNoteDrafts,
      t,
      escapeHtml
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
  } else if (state.adminTab === "wedding-packages") {
    body.innerHTML = renderAdminWeddingPackagesSectionHtml({
      services: getEffectiveStudioServices(),
      savingTierId: state.savingWeddingTierId,
      t,
      escapeHtml
    });
  } else if (state.adminTab === "email-alerts") {
    body.innerHTML = renderAdminEmailAlertsSectionHtml({
      settings: state.adminEmailAlertSettings,
      alerts: state.adminEmailAlerts,
      recentBookings: state.adminData.bookings,
      recentModels: state.adminData.models,
      filter: state.adminEmailAlertFilter,
      isSavingSettings: state.isSavingEmailAlertSettings,
      dispatchingKey: state.dispatchingEmailAlertKey,
      t,
      escapeHtml
    });
  } else if (state.adminTab === "email-preview") {
    body.innerHTML = renderAdminEmailPreviewSectionHtml({
      previewState: state.adminEmailPreview,
      adminData: state.adminData,
      t,
      escapeHtml
    });
  } else if (state.adminTab === "pro-registrations") {
    body.innerHTML = renderAdminProRegistrationsHtml({
      professionals: proList,
      filter: state.adminProFilter,
      search: state.adminProSearch,
      lang: state.lang,
      t,
      escapeHtml
    });
  } else if (state.adminTab === "pro-hiring") {
    body.innerHTML = renderAdminHiringRequestsHtml({
      hiringRequests: hiringList,
      approvedProfessionals: proList.filter((p) => String(p.status || "").toLowerCase() === "approved"),
      filter: state.adminHiringFilter,
      lang: state.lang,
      t,
      escapeHtml
    });
  } else if (state.adminTab === "pro-outsourcing") {
    body.innerHTML = renderAdminOutsourcingRequestsHtml({
      outsourcingRequests: outsourcingList,
      approvedProfessionals: proList.filter((p) => String(p.status || "").toLowerCase() === "approved"),
      lang: state.lang,
      t,
      escapeHtml
    });
  } else if (state.adminTab === "pro-assignments") {
    body.innerHTML = renderAdminAssignmentsHtml({
      hiringRequests: hiringList,
      outsourcingRequests: outsourcingList,
      lang: state.lang,
      t,
      escapeHtml
    });
  } else if (state.adminTab === "private-messages") {
    body.innerHTML = renderAdminPrivateMessagesHtml({
      threads: state.adminChatThreads,
      activeChatId: state.activeAdminChatId,
      messages: state.activeAdminChatMessages,
      blockedMap: state.adminProData?.blockedMap || {},
      draftText: state.adminChatDraftText,
      draftImage: state.adminChatDraftImage,
      isSending: state.adminChatSending,
      lang: state.lang,
      t,
      escapeHtml
    });
    const adminTimeline = document.getElementById("adminPrivateChatTimeline");
    if (adminTimeline) adminTimeline.scrollTop = adminTimeline.scrollHeight;
    syncVisibleChatReadReceipts({ asAdmin: true });
  } else if (state.adminTab === "pro-booking-history") {
    body.innerHTML = renderAdminBookingHistoryHtml({
      studioBookings: state.adminData.bookings,
      hiringRequests: hiringList,
      outsourcingRequests: outsourcingList,
      lang: state.lang,
      t,
      escapeHtml
    });
  } else if (state.adminTab === "customer-portal") {
    body.innerHTML = renderAdminCustomerPortalTabHtml({
      bookings: state.adminData.bookings,
      catalogs: state.adminPortalData?.catalogs || [],
      slots: state.adminPortalData?.slots || [],
      lang: state.lang,
      t,
      escapeHtml
    });
  } else if (state.adminTab === "payments") {
    if (state.adminQrUploadState?.status === "uploading") {
      return;
    }
    body.innerHTML = renderAdminPaymentDashboardHtml({
      payments: state.adminBookingPayments || [],
      paymentConfig: state.paymentConfig || DEFAULT_PAYMENT_CONFIG,
      qrUploadState: state.adminQrUploadState || null,
      filterStatus: state.adminPaymentFilter || "all",
      lang: state.lang,
      t,
      escapeHtml
    });
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

function getActiveEmailPreviewPayload() {
  const { type, recordId, overrides } = state.adminEmailPreview;
  const records = getPreviewRecordsForType(type, state.adminData);
  const matched = recordId !== "__sample__" ? records.find((r) => r.id === recordId) || null : null;
  return buildAdminEmailPreview({
    type,
    record: matched,
    recordId: matched?.id || "",
    brandOverrides: overrides
  });
}

function refreshEmailPreviewLiveStage() {
  const preview = getActiveEmailPreviewPayload();
  const subjectEl = document.getElementById("adminEmailPreviewSubjectText");
  if (subjectEl) subjectEl.textContent = preview.subject;
  const frame = document.getElementById("adminEmailPreviewFrame");
  if (frame) frame.setAttribute("srcdoc", preview.html);
  const textBody = document.getElementById("adminEmailPreviewTextBody");
  if (textBody) textBody.textContent = preview.text;
  const srcBody = document.getElementById("adminEmailPreviewSourceBody");
  if (srcBody) srcBody.textContent = preview.html;
}

async function refreshCloudData() {
  if (!state.firebaseReady || !isAuthenticatedMember(state.user)) return;
  try {
    const [cloudGalleryRes, publicModelsRes, weddingConfigsRes] = await Promise.allSettled([
      fetchApprovedGallery(),
      fetchPublicApprovedModels(),
      fetchWeddingPackageConfigs()
    ]);
    if (cloudGalleryRes.status === "fulfilled") {
      const cloudGallery = cloudGalleryRes.value;
      const seen = new Set(cloudGallery.map((g) => g.id));
      state.galleryItems = [
        ...cloudGallery,
        ...SHOWCASE_GALLERY_SEEDS.filter((seed) => !seen.has(seed.id))
      ];
      renderGallery();
    }
    if (publicModelsRes.status === "fulfilled") {
      state.publicModels = mergePublicModels(publicModelsRes.value);
      renderPublicModels();
      tryOpenPendingSharedModelProfile();
    }
    if (weddingConfigsRes.status === "fulfilled") {
      state.weddingPackageConfigs = weddingConfigsRes.value;
      renderCatalog();
      populateBookingServicesDropdown();
    }

    if (state.user) {
      const [
        bookingsRes,
        modelsRes,
        notifsRes,
        reviewsRes,
        companyProfileRes,
        companyBookingsRes,
        publicProsRes,
        myProsRes,
        myHiringRes,
        myAssignedRes,
        myOutsRes,
        myBlockRes,
        myProfileRes,
        myCatalogsRes,
        mySlotsRes,
        bookedSlotsRes,
        paymentSettingsRes,
        myPaymentsRes
      ] = await Promise.allSettled([
        fetchMyBookings(state.user.uid),
        fetchMyModels(state.user.uid),
        fetchMyNotifications(state.user.uid),
        fetchServiceReviews(),
        fetchMyCompanyProfile(state.user.uid),
        fetchMyCompanyBookings(state.user.uid),
        fetchApprovedPublicProfessionals(),
        fetchMyProfessionalProfiles(state.user.uid),
        fetchMyHiringRequests(state.user.uid),
        fetchAssignedJobsForProfessional(state.user.uid),
        fetchMyOutsourcingRequests(state.user.uid),
        fetchMyChatBlockStatus(state.user.uid),
        fetchMyUserProfile(state.user.uid),
        fetchMyCustomerCatalogs(state.user.uid),
        fetchMyCustomerSlots(state.user.uid),
        fetchStudioBookedSlots(),
        fetchPaymentSettings(),
        fetchMyUpiPaymentHistory(state.user.uid).catch(() => fetchMyBookingPayments(state.user.uid))
      ]);
      if (bookingsRes.status === "fulfilled") state.myBookings = bookingsRes.value;
      if (modelsRes.status === "fulfilled") state.myModels = modelsRes.value;
      if (notifsRes.status === "fulfilled") state.myNotifications = notifsRes.value;
      if (reviewsRes.status === "fulfilled") state.reviews = reviewsRes.value;
      if (companyProfileRes.status === "fulfilled") {
        state.myCompanyProfile = companyProfileRes.value;
        prefillCompanyFormsFromProfile(state.myCompanyProfile);
      }
      if (companyBookingsRes.status === "fulfilled") {
        state.myCompanyBookings = companyBookingsRes.value;
      }
      if (publicProsRes.status === "fulfilled") state.publicProfessionals = publicProsRes.value;
      if (myProsRes.status === "fulfilled") state.myProfessionals = myProsRes.value;
      if (myHiringRes.status === "fulfilled") state.myHiringRequests = myHiringRes.value;
      if (myAssignedRes.status === "fulfilled") state.myAssignedJobs = myAssignedRes.value;
      if (myOutsRes.status === "fulfilled") state.myOutsourcingRequests = myOutsRes.value;
      if (myBlockRes.status === "fulfilled") state.myChatBlockStatus = myBlockRes.value;
      if (myProfileRes.status === "fulfilled") state.myUserProfile = myProfileRes.value;
      if (myCatalogsRes.status === "fulfilled") state.myCustomerCatalogs = myCatalogsRes.value;
      if (mySlotsRes.status === "fulfilled") state.myCustomerSlots = mySlotsRes.value;
      if (bookedSlotsRes.status === "fulfilled") state.studioBookedSlots = bookedSlotsRes.value;
      if (paymentSettingsRes.status === "fulfilled") state.paymentConfig = paymentSettingsRes.value;
      if (myPaymentsRes.status === "fulfilled") state.myBookingPayments = myPaymentsRes.value;

      renderMyBookings();
      renderBookingSlotCalendar();
      renderMyModels();
      renderMyCompanyBookings();
      renderCatalog();
      renderTestimonialCarousel();
      renderProNetworkView();
      renderCustomerPortalView({ checkUrlDeepLink: state.route === "portal" });
      await startUserPrivateChatSubscription();
      if (state.isAdmin && state.route === "admin") {
        await loadAdminConsole();
      }
    } else {
      state.reviews = [];
      state.myModels = [];
      state.myNotifications = [];
      state.myUserProfile = null;
      state.myCustomerCatalogs = [];
      state.myCustomerSlots = [];
      state.portalDeniedNotice = "";
      state.portalHighlightedBookingId = "";
      state.myBookingPayments = [];
      state.portalPaymentHistoryFilter = "all";
      state.portalPaymentHistoryLoading = false;
      state.portalPaymentHistoryError = "";
      state.editingModelId = null;
      state.editModelDraftPhotos = [];
      state.myCompanyProfile = null;
      state.myCompanyBookings = [];
      state.publicProfessionals = [];
      state.myProfessionals = [];
      state.myHiringRequests = [];
      state.myAssignedJobs = [];
      state.myOutsourcingRequests = [];
      state.myChatThreads = [];
      state.activeUserChatId = "";
      state.activeUserChatMessages = [];
      stopUserChatSubscriptions();
      prefillCompanyFormsFromProfile(null);
      renderMyModels();
      renderMyCompanyBookings();
      renderCatalog();
      renderTestimonialCarousel();
    }
  } catch (err) {
    console.warn("Cloud refresh warning:", err?.message || err);
  }
}

function syncLocalDevSetupAffordance() {
  const existingBtn = document.getElementById("localDevFirebaseBtn");
  const existingDialog = document.getElementById("localDevFirebaseDialog");
  const allowLocalManualSetup = shouldShowManualFirebaseSetup({
    hostname: window.location?.hostname || "",
    firebaseReady: state.firebaseReady
  });

  // Never show any manual Firebase setup UI on production (.web.app / .firebaseapp.com) or once connected
  if (!allowLocalManualSetup) {
    existingBtn?.remove();
    existingDialog?.remove();
    return;
  }

  if (!existingBtn) {
    const statusStripActions = document.getElementById("openAdminConsoleQuickBtn")?.parentElement;
    if (statusStripActions) {
      const btn = document.createElement("button");
      btn.id = "localDevFirebaseBtn";
      btn.type = "button";
      btn.className = "chip-btn";
      btn.style.minHeight = "36px";
      btn.style.padding = "4px 12px";
      btn.textContent = "Local Dev Cloud Config";
      btn.addEventListener("click", () => {
        openLocalDevFirebaseDialog();
      });
      statusStripActions.appendChild(btn);
    }
  }
}

function openLocalDevFirebaseDialog() {
  if (
    !shouldShowManualFirebaseSetup({
      hostname: window.location?.hostname || "",
      firebaseReady: state.firebaseReady
    })
  ) {
    return;
  }
  if (document.getElementById("localDevFirebaseDialog")) return;

  const backdrop = document.createElement("div");
  backdrop.id = "localDevFirebaseDialog";
  backdrop.className = "modal-backdrop";
  backdrop.setAttribute("role", "dialog");
  backdrop.setAttribute("aria-modal", "true");
  backdrop.innerHTML = `
    <div class="modal-card">
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h3 style="margin:0;color:var(--gold);">Local Development Preview Config</h3>
        <button id="closeLocalDevFirebaseDialogBtn" class="icon-btn" type="button" aria-label="Close">✕</button>
      </div>
      <p style="margin:0;font-size:0.86rem;color:var(--text-secondary);">
        Only shown on localhost when /__/firebase/init.json and /firebase-config.json are unavailable. Production (.web.app / .firebaseapp.com) always auto-connects.
      </p>
      <form id="localDevFirebaseForm" class="form-grid">
        <div class="field">
          <label for="localDevApiKey">Firebase Web API Key</label>
          <input id="localDevApiKey" type="text" class="input" required autocomplete="off" />
        </div>
        <div class="field">
          <label for="localDevAppId">Firebase Web App ID</label>
          <input id="localDevAppId" type="text" class="input" required autocomplete="off" />
        </div>
        <button type="submit" class="btn-gold">Connect Local Preview</button>
      </form>
    </div>
  `;
  document.body.appendChild(backdrop);

  backdrop.querySelector("#closeLocalDevFirebaseDialogBtn")?.addEventListener("click", () => {
    backdrop.remove();
  });
  backdrop.addEventListener("click", (e) => {
    if (e.target === backdrop) backdrop.remove();
  });
  backdrop.querySelector("#localDevFirebaseForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const apiKey = backdrop.querySelector("#localDevApiKey")?.value || "";
    const appId = backdrop.querySelector("#localDevAppId")?.value || "";
    try {
      saveLocalDevFirebaseConfig({ apiKey, appId }, { hostname: window.location?.hostname || "" });
      backdrop.remove();
      const ready = await connectFirebaseCloud();
      if (ready) {
        showToast("Local preview connected to Firebase Cloud.");
      }
    } catch (err) {
      showToast(err?.message || "Invalid local preview configuration.");
    }
  });
}

function updateFirebaseUiStatus() {
  const statusStrip = document.getElementById("studioCloudStatusStrip");
  const dot = document.getElementById("firebaseStatusDot");
  const txt = document.getElementById("firebaseStatusText");
  const userLabel = document.getElementById("signedInUserLabel");
  const isAdminNow = hasVerifiedAdminSession();

  if (statusStrip) {
    statusStrip.classList.add("hidden");
    statusStrip.hidden = true;
    statusStrip.setAttribute("aria-hidden", "true");
  }
  if (dot) dot.classList.toggle("ready", state.firebaseReady);
  if (txt) txt.textContent = "";
  if (userLabel) {
    userLabel.textContent = "";
    userLabel.classList.add("hidden");
    userLabel.hidden = true;
    userLabel.setAttribute("aria-hidden", "true");
  }
  if (!isAdminNow) {
    unmountModelGrowthChart(document.getElementById("adminModelGrowthChartMount"));
    unmountPaymentTrendsChart(document.getElementById("adminPaymentTrendsChartMount"));
    unmountModelExperienceChart(document.getElementById("adminModelExperienceChartMount"));
  }
  syncAdminUiVisibility(document, {
    isAdmin: isAdminNow,
    adminEmail: state.user?.email || ""
  });
  syncLocalDevSetupAffordance();
  applyTranslations();
}

function renderBookingWeddingSummary() {
  const select = document.getElementById("bookingService");
  const box = document.getElementById("bookingWeddingSummary");
  if (!select || !box) return;
  const matchedTier = findWeddingTierByBookingValue(getEffectiveStudioServices(), select.value);
  if (!matchedTier) {
    state.selectedWeddingAddonIds = [];
    box.innerHTML = "";
    box.classList.add("hidden");
    return;
  }
  box.innerHTML = renderBookingWeddingSummaryHtml(matchedTier, {
    selectedAddonIds: state.selectedWeddingAddonIds,
    addons: WEDDING_BOOKING_ADDONS,
    lang: state.lang,
    t,
    escapeHtml
  });
  box.classList.remove("hidden");
}

function populateBookingServicesDropdown() {
  const select = document.getElementById("bookingService");
  if (!select) return;
  const prevValue = select.value;
  const effectiveServices = getEffectiveStudioServices();
  const weddingOptions = [];
  const studioOptions = [];

  effectiveServices.forEach((pkg) => {
    pkg.tiers.forEach((tier) => {
      const val = getServiceTierBookingValue(pkg, tier);
      if (pkg.id === WEDDING_PACKAGE_ID || pkg.isWeddingCollection) {
        weddingOptions.push({
          value: val,
          label: `${tier.tierNameEn} (${tier.priceFormatted})`
        });
      } else {
        studioOptions.push({
          value: val,
          label: val
        });
      }
    });
  });

  const weddingGroupHtml = weddingOptions.length
    ? `<optgroup label="Wedding Packages">${weddingOptions
        .map((opt) => `<option value="${escapeHtml(opt.value)}">${escapeHtml(opt.label)}</option>`)
        .join("")}</optgroup>`
    : "";
  const studioGroupHtml = `<optgroup label="Studio Photography, Modeling &amp; Cinema">${studioOptions
    .map((opt) => `<option value="${escapeHtml(opt.value)}">${escapeHtml(opt.label)}</option>`)
    .join("")}</optgroup>`;

  select.innerHTML = `${weddingGroupHtml}${studioGroupHtml}`;
  if (prevValue && Array.from(select.options).some((o) => o.value === prevValue)) {
    select.value = prevValue;
  }
  renderBookingWeddingSummary();

  const dateEl = document.getElementById("bookingDate");
  if (dateEl && !dateEl.value) {
    const tomorrow = new Date(Date.now() + 86400000);
    dateEl.min = tomorrow.toISOString().split("T")[0];
    dateEl.value = tomorrow.toISOString().split("T")[0];
  }

  const compDateEl = document.getElementById("compBookEventDate");
  if (compDateEl && !compDateEl.value) {
    const nextWeek = new Date(Date.now() + 7 * 86400000);
    const tomorrowStr = new Date(Date.now() + 86400000).toISOString().split("T")[0];
    compDateEl.min = tomorrowStr;
    compDateEl.value = nextWeek.toISOString().split("T")[0];
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
    const hadExistingController = Boolean(navigator.serviceWorker.controller);
    let swRefreshTriggered = false;

    navigator.serviceWorker.addEventListener("controllerchange", () => {
      if (hadExistingController && !swRefreshTriggered) {
        swRefreshTriggered = true;
        window.location.reload();
      }
    });

    window.addEventListener("load", () => {
      navigator.serviceWorker
        .register("/sw.js", { updateViaCache: "none" })
        .then((registration) => {
          registration.update().catch(() => {});
        })
        .catch((err) => {
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
      if (select && svc) {
        select.value = svc;
        renderBookingWeddingSummary();
      }
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
          openAuthModal({ adminNotice: false });
        } else {
          showToast(t("firebaseReconnectingToast"));
          connectFirebaseCloud().then((ready) => {
            if (ready) openAuthModal({ adminNotice: false });
          });
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

    if (e.target.closest("[data-tracker-toggle]")) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const snap = adminBookingTracker.getSnapshot();
      adminBookingTracker.setEnabled(!snap.enabled);
      return;
    }

    if (e.target.closest("[data-tracker-check-now]")) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const res = await adminBookingTracker.evaluateNow({ force: true });
      if (!res.transitioned?.length && !res.error) {
        showToast(t("adminTrackerCheckNoneToast"));
      }
      return;
    }

    if (e.target.closest("[data-adm-export-bookings-csv]")) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      if (state.adminCsvExportStatus === "processing") {
        return;
      }
      const res = await runAdminBookingsCsvExport(state.adminData.bookings, {
        onStateChange: (nextStatus) => {
          state.adminCsvExportStatus = nextStatus;
          applyAdminCsvButtonState(document.getElementById("adminExportBookingsCsvBtn"), nextStatus, {
            count: state.adminData.bookings.length,
            t,
            escapeHtml
          });
        }
      });
      if (res.status === "exported") {
        showToast(t("adminExportBookingsSuccessToast").replace("{count}", String(res.count)));
      } else {
        showToast(t("adminExportBookingsEmptyToast"));
      }
      return;
    }

    const growthChartTypeBtn = e.target.closest("[data-growth-chart-type]");
    if (growthChartTypeBtn) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const nextType = growthChartTypeBtn.getAttribute("data-growth-chart-type") === "bar" ? "bar" : "area";
      state.adminGrowthChartType = nextType;
      renderAdminModelGrowthChart();
      return;
    }

    const payTrendsDaysBtn = e.target.closest("[data-payment-trends-days]");
    if (payTrendsDaysBtn) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const parsedDays = Number(payTrendsDaysBtn.getAttribute("data-payment-trends-days"));
      state.adminPaymentTrendsDays = [7, 14, 30].includes(parsedDays) ? parsedDays : PAYMENT_TRENDS_DEFAULT_DAYS;
      renderAdminPaymentTrendsChart();
      return;
    }

    const expChartScopeBtn = e.target.closest("[data-exp-chart-scope]");
    if (expChartScopeBtn) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const nextScope = expChartScopeBtn.getAttribute("data-exp-chart-scope") === "approved" ? "approved" : "all";
      state.adminModelExperienceScope = nextScope;
      renderAdminModelExperienceChart();
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

    const openPreviewBtn = e.target.closest("[data-open-email-preview-type]");
    if (openPreviewBtn) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      state.adminEmailPreview.type = openPreviewBtn.getAttribute("data-open-email-preview-type") || "booking";
      state.adminEmailPreview.recordId = openPreviewBtn.getAttribute("data-open-email-preview-id") || "__sample__";
      state.adminTab = "email-preview";
      document
        .querySelectorAll("[data-admin-tab]")
        .forEach((b) => b.classList.toggle("active", b.getAttribute("data-admin-tab") === "email-preview"));
      renderAdminConsole();
      document.getElementById("adminEmailPreviewPanel")?.scrollIntoView({ behavior: "smooth", block: "start" });
      return;
    }

    const previewTypeBtn = e.target.closest("[data-email-preview-type]");
    if (previewTypeBtn) {
      if (!hasVerifiedAdminSession()) return;
      state.adminEmailPreview.type = previewTypeBtn.getAttribute("data-email-preview-type") || "booking";
      state.adminEmailPreview.recordId = "__sample__";
      renderAdminConsole();
      return;
    }

    const previewModeBtn = e.target.closest("[data-email-preview-mode]");
    if (previewModeBtn) {
      if (!hasVerifiedAdminSession()) return;
      state.adminEmailPreview.viewMode = previewModeBtn.getAttribute("data-email-preview-mode") || "html";
      renderAdminConsole();
      return;
    }

    const previewViewportBtn = e.target.closest("[data-email-preview-viewport]");
    if (previewViewportBtn) {
      if (!hasVerifiedAdminSession()) return;
      state.adminEmailPreview.viewport = previewViewportBtn.getAttribute("data-email-preview-viewport") || "desktop";
      renderAdminConsole();
      return;
    }

    const previewResetBtn = e.target.closest("[data-email-preview-reset]");
    if (previewResetBtn) {
      if (!hasVerifiedAdminSession()) return;
      state.adminEmailPreview.overrides = {};
      renderAdminConsole();
      showToast(t("adminEmailResetToast"));
      return;
    }

    const previewCopyBtn = e.target.closest("[data-email-preview-copy]");
    if (previewCopyBtn) {
      if (!hasVerifiedAdminSession()) return;
      const copyTarget = previewCopyBtn.getAttribute("data-email-preview-copy");
      const preview = getActiveEmailPreviewPayload();
      const textToCopy = copyTarget === "subject" ? preview.subject : preview.html;
      try {
        await navigator.clipboard?.writeText?.(textToCopy);
        showToast(copyTarget === "subject" ? t("adminEmailCopiedSubjectToast") : t("adminEmailCopiedHtmlToast"));
      } catch {
        showToast("Unable to access clipboard on this browser.");
      }
      return;
    }

    const sendTestEmailBtn = e.target.closest("[data-email-preview-send-test]");
    if (sendTestEmailBtn) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const currentStatus = state.adminEmailPreview.testSend?.status;
      if (currentStatus === "sending" || currentStatus === "queued") {
        return;
      }

      if (typeof state._unsubTestEmailStatus === "function") {
        state._unsubTestEmailStatus();
        state._unsubTestEmailStatus = null;
      }
      if (state._testEmailWorkerTimer) {
        clearTimeout(state._testEmailWorkerTimer);
        state._testEmailWorkerTimer = null;
      }

      const { type, recordId, overrides } = state.adminEmailPreview;
      const liveRecords = getPreviewRecordsForType(type, state.adminData);
      const matchedRecord =
        recordId && recordId !== "__sample__" ? liveRecords.find((item) => item.id === recordId) || null : null;
      const requestPayload = buildAdminTestEmailRequestPayload({
        type,
        record: matchedRecord,
        recordId: matchedRecord?.id || "",
        brandOverrides: overrides
      });

      state.adminEmailPreview.testSend = {
        status: "sending",
        testId: "",
        recipient: requestPayload.recipientEmail,
        messageId: "",
        smtpHost: "",
        smtpPort: "",
        error: ""
      };
      renderAdminConsole();

      try {
        const queued = await adminRequestTestEmail(requestPayload);
        state.adminEmailPreview.testSend = {
          status: "queued",
          testId: queued.testId,
          recipient: queued.recipientEmail,
          messageId: "",
          smtpHost: "",
          smtpPort: "",
          error: ""
        };
        renderAdminConsole();
        showToast(t("adminEmailTestQueuedToast").replace("{email}", queued.recipientEmail));

        state._testEmailWorkerTimer = setTimeout(() => {
          if (
            state.adminEmailPreview.testSend?.testId === queued.testId &&
            state.adminEmailPreview.testSend?.status === "queued"
          ) {
            state.adminEmailPreview.testSend = {
              ...state.adminEmailPreview.testSend,
              status: "awaiting_worker"
            };
            renderAdminConsole();
          }
        }, 12000);

        state._unsubTestEmailStatus = await adminSubscribeTestEmailStatus(queued.testId, {
          onUpdate: (docData) => {
            if (state.adminEmailPreview.testSend?.testId !== queued.testId) return;
            const remoteStatus = String(docData?.status || "");
            if (remoteStatus === "sent") {
              clearTimeout(state._testEmailWorkerTimer);
              state._testEmailWorkerTimer = null;
              state.adminEmailPreview.testSend = {
                status: "sent",
                testId: queued.testId,
                recipient: docData.recipientEmail || queued.recipientEmail,
                messageId: docData.messageId || "",
                smtpHost: docData.smtpHost || "",
                smtpPort: docData.smtpPort || "",
                error: ""
              };
              renderAdminConsole();
              showToast(t("adminEmailTestSentToast").replace("{email}", docData.recipientEmail || queued.recipientEmail));
              if (typeof state._unsubTestEmailStatus === "function") {
                state._unsubTestEmailStatus();
                state._unsubTestEmailStatus = null;
              }
            } else if (remoteStatus === "failed") {
              clearTimeout(state._testEmailWorkerTimer);
              state._testEmailWorkerTimer = null;
              const errText = docData.errorMessage || t("adminEmailTestStatusFailedFallback");
              state.adminEmailPreview.testSend = {
                status: "failed",
                testId: queued.testId,
                recipient: docData.recipientEmail || queued.recipientEmail,
                messageId: "",
                smtpHost: docData.smtpHost || "",
                smtpPort: docData.smtpPort || "",
                error: errText
              };
              renderAdminConsole();
              showToast(t("adminEmailTestFailedToast").replace("{error}", errText));
              if (typeof state._unsubTestEmailStatus === "function") {
                state._unsubTestEmailStatus();
                state._unsubTestEmailStatus = null;
              }
            }
          },
          onError: () => {}
        });
      } catch (err) {
        const errText = err?.message || t("adminEmailTestStatusFailedFallback");
        state.adminEmailPreview.testSend = {
          status: "failed",
          testId: "",
          recipient: requestPayload.recipientEmail,
          messageId: "",
          smtpHost: "",
          smtpPort: "",
          error: errText
        };
        renderAdminConsole();
        showToast(t("adminEmailTestFailedToast").replace("{error}", errText));
      }
      return;
    }

    const alertFilterBtn = e.target.closest("[data-admin-alert-filter]");
    if (alertFilterBtn) {
      if (!hasVerifiedAdminSession()) return;
      state.adminEmailAlertFilter = alertFilterBtn.getAttribute("data-admin-alert-filter") || "all";
      renderAdminConsole();
      return;
    }

    const alertMarkReadBtn = e.target.closest("[data-admin-alert-mark-read]");
    if (alertMarkReadBtn) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const alertId = alertMarkReadBtn.getAttribute("data-admin-alert-mark-read");
      if (alertId) {
        try {
          await adminMarkEmailAlertRead(alertId);
          state.adminEmailAlerts = (state.adminEmailAlerts || []).map((a) =>
            a.id === alertId ? { ...a, isRead: true } : a
          );
          renderAdminConsole();
        } catch (err) {
          showToast(err.message || "Unable to update alert status.");
        }
      }
      return;
    }

    const alertMarkAllReadBtn = e.target.closest("[data-admin-alert-mark-all-read]");
    if (alertMarkAllReadBtn) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const unreadIds = (state.adminEmailAlerts || []).filter((a) => a?.isRead === false && a?.id).map((a) => a.id);
      if (unreadIds.length) {
        try {
          await adminMarkAllEmailAlertsRead(unreadIds);
          const idSet = new Set(unreadIds);
          state.adminEmailAlerts = (state.adminEmailAlerts || []).map((a) =>
            idSet.has(a.id) ? { ...a, isRead: true } : a
          );
          showToast(t("adminEmailAlertMarkedAllReadToast"));
          renderAdminConsole();
        } catch (err) {
          showToast(err.message || "Unable to mark alerts as read.");
        }
      }
      return;
    }

    const alertResendBtn = e.target.closest("[data-admin-alert-resend-type]");
    if (alertResendBtn) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const alertType = alertResendBtn.getAttribute("data-admin-alert-resend-type") || "booking";
      const recordId = alertResendBtn.getAttribute("data-admin-alert-resend-id") || "";
      const dispatchKey = `${alertType}:${recordId}`;
      if (state.dispatchingEmailAlertKey === dispatchKey) return;
      state.dispatchingEmailAlertKey = dispatchKey;
      renderAdminConsole();
      try {
        const liveRecords = getPreviewRecordsForType(alertType, state.adminData);
        const matched = liveRecords.find((r) => String(r?.id || "") === recordId) || null;
        const requestPayload = buildAdminTestEmailRequestPayload({
          type: alertType,
          record: matched,
          recordId: matched?.id || recordId,
          brandOverrides: state.adminEmailPreview.overrides || {}
        });
        const queued = await adminRequestTestEmail(requestPayload);
        const logRecord = buildAdminEmailAlertRecord({
          type: alertType,
          record: matched || { id: recordId },
          status: "queued",
          source: "manual-resend"
        });
        await adminLogEmailAlert(logRecord).catch(() => {});
        showToast(t("adminEmailAlertDispatchedToast").replace("{email}", queued.recipientEmail));
      } catch (err) {
        showToast(err.message || "Unable to dispatch email alert.");
      } finally {
        state.dispatchingEmailAlertKey = "";
        renderAdminConsole();
      }
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

    const admBulkClearBtn = e.target.closest("[data-adm-model-bulk-clear]");
    if (admBulkClearBtn) {
      if (!hasVerifiedAdminSession()) return;
      state.adminSelectedModelIds = [];
      renderAdminConsole();
      return;
    }

    const admBulkActionBtn = e.target.closest("[data-adm-model-bulk-action]");
    if (admBulkActionBtn) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      if (state.adminBulkUpdating) return;
      const nextStatus = admBulkActionBtn.getAttribute("data-adm-model-bulk-action") || "";
      const evaluation = evaluateBulkModelUpdate({
        models: state.adminData.models || [],
        selectedIds: state.adminSelectedModelIds || [],
        nextStatus,
        bulkNote: state.adminBulkModelNote || "",
        drafts: state.adminModelDrafts || {},
        lang: state.lang
      });
      if (!evaluation.ok) {
        showToast(evaluation.error || "Unable to run bulk model action.");
        return;
      }
      const actionVerb = evaluation.status === "approved" ? "Approve" : "Reject";
      if (
        !window.confirm(
          `${actionVerb} ${evaluation.eligible.length} selected model(s) simultaneously?${
            evaluation.skipped.length > 0
              ? ` (${evaluation.skipped.length} ineligible model(s) will be skipped.)`
              : ""
          }`
        )
      ) {
        return;
      }

      state.adminBulkUpdating = true;
      renderAdminConsole();
      try {
        const results = await Promise.allSettled(
          evaluation.eligible.map(({ model, adminNotes }) =>
            adminUpdateModelStatus(model.id, evaluation.status, adminNotes, model)
          )
        );
        let succeeded = 0;
        let failed = 0;
        results.forEach((res, idx) => {
          const item = evaluation.eligible[idx];
          if (res.status === "fulfilled") {
            succeeded += 1;
            if (item?.model?.id) {
              delete state.adminModelDrafts[item.model.id];
            }
          } else {
            failed += 1;
          }
        });
        state.adminSelectedModelIds = [];
        state.adminBulkModelNote = "";
        const label = evaluation.status === "approved" ? "approved" : "rejected";
        const parts = [`${succeeded} ${label}`];
        if (evaluation.skipped.length > 0) parts.push(`${evaluation.skipped.length} skipped`);
        if (failed > 0) parts.push(`${failed} failed`);
        showToast(`Bulk update complete: ${parts.join(", ")}.`);
        await loadAdminConsole();
        await refreshCloudData();
      } catch (bulkErr) {
        showToast(bulkErr?.message || "Bulk model update failed.");
      } finally {
        state.adminBulkUpdating = false;
        renderAdminConsole();
      }
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
        await adminUpdateModelStatus(modelId, nextStatus, adminNotes, targetModel);
        delete state.adminModelDrafts[modelId];
        showToast(
          nextStatus === "approved"
            ? "Verified and added to Official Studio Roster!"
            : nextStatus === "rejected"
            ? "Model application rejected with review note."
            : "Moved back to verification queue."
        );
        await loadAdminConsole();
        await refreshCloudData();
      } catch (updateErr) {
        showToast(updateErr.message || "Unable to update model verification status.");
      }
    } else if (e.target.closest("[data-adm-mdl-disable]")) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const btn = e.target.closest("[data-adm-mdl-disable]");
      const modelId = btn.getAttribute("data-adm-mdl-disable");
      const nextDisabled = btn.getAttribute("data-adm-mdl-disabled-next") === "true";
      const targetModel = state.adminData.models.find((m) => m.id === modelId);
      const card = btn.closest("[data-adm-model-card]");
      const notesInput = card?.querySelector(`[data-adm-model-notes="${modelId}"]`);
      const adminNotes = notesInput ? notesInput.value : targetModel?.adminNotes || "";
      try {
        await adminSetModelDisabled(modelId, nextDisabled, adminNotes, targetModel);
        showToast(nextDisabled ? "Model profile disabled and removed from public catalog." : "Model profile re-enabled.");
        await loadAdminConsole();
        await refreshCloudData();
      } catch (err) {
        showToast(err.message || "Unable to update profile visibility.");
      }
    } else if (e.target.closest("[data-adm-mdl-delete]")) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const btn = e.target.closest("[data-adm-mdl-delete]");
      const modelId = btn.getAttribute("data-adm-mdl-delete");
      const modelName = btn.getAttribute("data-adm-mdl-delete-name") || "this profile";
      if (!window.confirm(`Permanently remove ${modelName} from both Admin and public catalog?`)) return;
      try {
        await adminDeleteModelProfile(modelId);
        showToast("Model profile permanently removed.");
        await loadAdminConsole();
        await refreshCloudData();
      } catch (err) {
        showToast(err.message || "Unable to delete model profile.");
      }
    } else if (e.target.closest("[data-open-auth-modal='true']")) {
      openAuthModal({ adminNotice: false });
    } else if (e.target.closest("[data-start-edit-model]")) {
      const btn = e.target.closest("[data-start-edit-model]");
      const modelId = btn.getAttribute("data-start-edit-model");
      const target = state.myModels.find((m) => m.id === modelId);
      if (target) {
        state.editingModelId = modelId;
        state.editModelDraftPhotos = sanitizePortfolioPhotoList(target.portfolioPhotos || []);
        renderMyModels();
      }
    } else if (e.target.closest("[data-cancel-edit-model]")) {
      state.editingModelId = null;
      state.editModelDraftPhotos = [];
      renderMyModels();
    } else if (e.target.closest("[data-remove-portfolio-photo]")) {
      const btn = e.target.closest("[data-remove-portfolio-photo]");
      const idx = Number.parseInt(btn.getAttribute("data-remove-portfolio-photo") || "-1", 10);
      const scope = btn.getAttribute("data-photo-scope") || "reg";
      if (idx >= 0) {
        if (scope === "edit") {
          state.editModelDraftPhotos.splice(idx, 1);
          renderMyModels();
        } else {
          state.modelRegPortfolioPhotos.splice(idx, 1);
          renderModelRegPortfolioPreview();
        }
      }
    } else if (e.target.closest("[data-mark-model-notif-read]")) {
      const btn = e.target.closest("[data-mark-model-notif-read]");
      const notifId = btn.getAttribute("data-mark-model-notif-read");
      if (state.user && notifId) {
        try {
          await markMyNotificationRead(state.user.uid, notifId);
          state.myNotifications = state.myNotifications.map((n) =>
            n.id === notifId ? { ...n, isRead: true } : n
          );
          renderMyModels();
        } catch (err) {
          showToast(err.message || "Unable to update notification.");
        }
      }
    } else if (e.target.closest("[data-profile-photo-src]")) {
      const btn = e.target.closest("[data-profile-photo-src]");
      const nextSrc = btn.getAttribute("data-profile-photo-src");
      const mainImg = document.getElementById("modelProfileMainImg");
      if (mainImg && nextSrc) {
        mainImg.src = nextSrc;
        btn.closest(".model-profile-thumbs")?.querySelectorAll("[data-profile-photo-src]").forEach((el) => {
          el.classList.toggle("active", el === btn);
        });
      }
    } else if (e.target.closest("[data-audition-request-model-id]")) {
      const btn = e.target.closest("[data-audition-request-model-id]");
      const modelId = btn.getAttribute("data-audition-request-model-id");
      const fullName = btn.getAttribute("data-audition-request-model-name") || "Roster Model";
      if (modelId) startAuditionRequestForModel(modelId, fullName);
    } else if (e.target.closest("[data-public-model-gender]")) {
      const btn = e.target.closest("[data-public-model-gender]");
      state.publicModelFilters.gender = btn.getAttribute("data-public-model-gender") || "all";
      renderPublicModels();
    } else if (e.target.closest("[data-casting-skill]")) {
      const btn = e.target.closest("[data-casting-skill]");
      const skill = btn.getAttribute("data-casting-skill") || "";
      if (!skill) return;
      const list = Array.isArray(state.publicModelFilters.skills) ? [...state.publicModelFilters.skills] : [];
      const idx = list.indexOf(skill);
      if (idx >= 0) list.splice(idx, 1);
      else list.push(skill);
      state.publicModelFilters.skills = list;
      renderPublicModels();
    } else if (e.target.closest("[data-view-model-profile]")) {
      const btn = e.target.closest("[data-view-model-profile]");
      const modelId = btn.getAttribute("data-view-model-profile");
      if (modelId) openModelProfileModal(modelId);
    } else if (e.target.closest("[data-share-model-profile]")) {
      const btn = e.target.closest("[data-share-model-profile]");
      const modelId = btn.getAttribute("data-share-model-profile");
      const fullName = btn.getAttribute("data-share-model-name") || "Roster Model";
      if (modelId) copyModelProfileLink(modelId, fullName, btn);
    } else if (e.target.closest("[data-shoot-booking-model-id]")) {
      const btn = e.target.closest("[data-shoot-booking-model-id]");
      const modelId = btn.getAttribute("data-shoot-booking-model-id");
      const fullName = btn.getAttribute("data-shoot-booking-model-name") || "Roster Model";
      if (modelId) startShootBookingForModel(modelId, fullName);
    } else if (e.target.closest("#modelProfileModal [data-casting-inquiry-model-id]")) {
      const btn = e.target.closest("[data-casting-inquiry-model-id]");
      const modelId = btn.getAttribute("data-casting-inquiry-model-id");
      const fullName = btn.getAttribute("data-casting-inquiry-model-name") || "Roster Model";
      if (modelId) startCastingInquiryForModel(modelId, fullName);
    } else if (e.target.closest("[data-toggle-req-model-id]")) {
      const btn = e.target.closest("[data-toggle-req-model-id]");
      const modelId = btn.getAttribute("data-toggle-req-model-id");
      const fullName = btn.getAttribute("data-toggle-req-model-name") || "Roster Model";
      const existsIdx = state.selectedRequestedModels.findIndex((m) => m.id === modelId);
      if (existsIdx >= 0) {
        state.selectedRequestedModels.splice(existsIdx, 1);
      } else {
        if (state.selectedRequestedModels.length >= 20) {
          showToast("You can attach up to 20 specific models per request.");
          return;
        }
        state.selectedRequestedModels.push({ id: modelId, fullName });
        showToast(t("castingInquiryAddedToast").replace("{name}", fullName));
      }
      renderPublicModels();
      renderSelectedRequestedModels();
    } else if (e.target.closest("[data-remove-req-model-id]")) {
      const btn = e.target.closest("[data-remove-req-model-id]");
      const modelId = btn.getAttribute("data-remove-req-model-id");
      state.selectedRequestedModels = state.selectedRequestedModels.filter((m) => m.id !== modelId);
      renderPublicModels();
      renderSelectedRequestedModels();
    } else if (e.target.closest("[data-model-skill-quick]")) {
      const btn = e.target.closest("[data-model-skill-quick]");
      const skill = btn.getAttribute("data-model-skill-quick") || "";
      const input = document.getElementById("modelSkills");
      if (input && skill) {
        const existing = input.value
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean);
        const lowerIdx = existing.findIndex((s) => s.toLowerCase() === skill.toLowerCase());
        if (lowerIdx >= 0) {
          existing.splice(lowerIdx, 1);
          btn.classList.remove("active");
        } else {
          existing.push(skill);
          btn.classList.add("active");
        }
        input.value = existing.join(", ");
      }
    } else if (e.target.closest("[data-cancel-company-req]")) {
      const btn = e.target.closest("[data-cancel-company-req]");
      const reqId = btn.getAttribute("data-cancel-company-req");
      try {
        await cancelMyCompanyBooking(reqId);
        showToast(t("companyBookingCancelledToast"));
        await refreshCloudData();
      } catch (err) {
        showToast(err.message || "Unable to cancel company booking.");
      }
    } else if (e.target.closest("[data-adm-comp-filter]")) {
      if (!hasVerifiedAdminSession()) return;
      state.adminCompanyFilter = e.target.closest("[data-adm-comp-filter]").getAttribute("data-adm-comp-filter") || "all";
      renderAdminConsole();
    } else if (e.target.matches("[data-adm-comp-id]")) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const reqId = e.target.getAttribute("data-adm-comp-id");
      const nextStatus = e.target.getAttribute("data-adm-comp-st");
      const card = e.target.closest("[data-adm-comp-card]");
      const notesInput = card?.querySelector(`[data-adm-comp-notes="${reqId}"]`);
      const adminNotes = notesInput ? notesInput.value : state.adminCompanyNoteDrafts[reqId] || "";
      try {
        await adminUpdateCompanyBookingStatus(reqId, nextStatus, adminNotes);
        delete state.adminCompanyNoteDrafts[reqId];
        showToast(`Company booking marked ${nextStatus}.`);
        await loadAdminConsole();
      } catch (err) {
        showToast(err.message || "Unable to update company booking status.");
      }
    } else if (e.target.matches("[data-adm-comp-del]")) {
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      try {
        await adminDeleteCompanyBooking(e.target.getAttribute("data-adm-comp-del"));
        showToast("Company booking request removed.");
        await loadAdminConsole();
      } catch (err) {
        showToast(err.message || "Unable to remove company booking.");
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
    const emailRecordSelect = e.target.closest("[data-email-preview-record]");
    if (emailRecordSelect) {
      if (!hasVerifiedAdminSession()) return;
      state.adminEmailPreview.recordId = emailRecordSelect.value || "__sample__";
      renderAdminConsole();
      return;
    }

    const selectAllInput = e.target.closest("[data-adm-model-select-all]");
    if (selectAllInput) {
      if (!hasVerifiedAdminSession()) return;
      const visibleModels = filterModelApplications(
        state.adminData.models || [],
        state.adminModelFilter,
        state.adminModelSearch
      );
      const nextSet = new Set(state.adminSelectedModelIds || []);
      visibleModels.forEach((m) => {
        if (!m?.id) return;
        if (selectAllInput.checked) nextSet.add(String(m.id));
        else nextSet.delete(String(m.id));
      });
      state.adminSelectedModelIds = Array.from(nextSet);
      renderAdminConsole();
      return;
    }

    const selectOneInput = e.target.closest("[data-adm-model-select]");
    if (selectOneInput) {
      if (!hasVerifiedAdminSession()) return;
      const modelId = String(selectOneInput.getAttribute("data-adm-model-select") || "");
      if (!modelId) return;
      const nextSet = new Set(state.adminSelectedModelIds || []);
      if (selectOneInput.checked) nextSet.add(modelId);
      else nextSet.delete(modelId);
      state.adminSelectedModelIds = Array.from(nextSet);
      renderAdminConsole();
      return;
    }

    const checkInput = e.target.closest("[data-adm-model-check]");
    if (!checkInput) return;
    const modelId = checkInput.getAttribute("data-adm-model-check");
    state.adminModelDrafts[modelId] = {
      ...(state.adminModelDrafts[modelId] || {}),
      checked: checkInput.checked
    };
  });

  document.addEventListener("input", (e) => {
    const emailOverrideInput = e.target.closest("[data-email-override-key]");
    if (emailOverrideInput) {
      if (!hasVerifiedAdminSession()) return;
      const key = emailOverrideInput.getAttribute("data-email-override-key");
      if (key) {
        state.adminEmailPreview.overrides[key] = emailOverrideInput.value;
        refreshEmailPreviewLiveStage();
      }
      return;
    }

    const admBulkNoteInput = e.target.closest("[data-adm-model-bulk-note]");
    if (admBulkNoteInput) {
      if (!hasVerifiedAdminSession()) return;
      state.adminBulkModelNote = admBulkNoteInput.value || "";
      return;
    }

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

    const admCompSearchInput = e.target.closest("[data-adm-comp-search]");
    if (admCompSearchInput) {
      state.adminCompanySearch = admCompSearchInput.value || "";
      renderAdminConsole();
      const restored = document.querySelector("[data-adm-comp-search]");
      if (restored) {
        restored.focus();
        const len = restored.value.length;
        restored.setSelectionRange?.(len, len);
      }
      return;
    }

    const admCompNotesInput = e.target.closest("[data-adm-comp-notes]");
    if (admCompNotesInput) {
      const reqId = admCompNotesInput.getAttribute("data-adm-comp-notes");
      state.adminCompanyNoteDrafts[reqId] = admCompNotesInput.value;
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

  document.getElementById("publicModelsSearchInput")?.addEventListener("input", (e) => {
    state.publicModelFilters.search = e.target.value || "";
    renderPublicModels();
  });

  document.getElementById("castingStateSelect")?.addEventListener("change", (e) => {
    state.publicModelFilters.state = e.target.value || "all";
    renderPublicModels();
  });

  document.getElementById("castingCityInput")?.addEventListener("input", (e) => {
    state.publicModelFilters.city = e.target.value || "";
    renderPublicModels();
  });

  document.getElementById("castingMinAge")?.addEventListener("input", (e) => {
    state.publicModelFilters.minAge = e.target.value || "";
    renderPublicModels();
  });

  document.getElementById("castingMaxAge")?.addEventListener("input", (e) => {
    state.publicModelFilters.maxAge = e.target.value || "";
    renderPublicModels();
  });

  document.getElementById("castingMinHeightSelect")?.addEventListener("change", (e) => {
    state.publicModelFilters.minHeightCm = e.target.value || "";
    renderPublicModels();
  });

  document.getElementById("castingMaxHeightSelect")?.addEventListener("change", (e) => {
    state.publicModelFilters.maxHeightCm = e.target.value || "";
    renderPublicModels();
  });

  document.getElementById("castingExperienceSelect")?.addEventListener("change", (e) => {
    state.publicModelFilters.experience = e.target.value || "all";
    renderPublicModels();
  });

  document.getElementById("castingPortfolioOnlyCheckbox")?.addEventListener("change", (e) => {
    state.publicModelFilters.portfolioOnly = Boolean(e.target.checked);
    renderPublicModels();
  });

  document.getElementById("castingResetFiltersBtn")?.addEventListener("click", () => {
    resetModelDiscoveryFilters();
  });

  document.getElementById("closeModelProfileBtn")?.addEventListener("click", () => {
    closeModelProfileModal();
  });

  document.getElementById("modelProfileModal")?.addEventListener("click", (e) => {
    if (e.target === e.currentTarget) {
      closeModelProfileModal();
    }
  });

  document.getElementById("langToggleBtn")?.addEventListener("click", () => {
    state.lang = state.lang === "en" ? "hi" : "en";
    localStorage.setItem("star_artist_lang", state.lang);
    applyTranslations();
    if (isAuthenticatedMember(state.user)) {
      renderCatalog();
      renderTestimonialCarousel();
      renderMyBookings();
      renderPublicModels();
      renderSelectedRequestedModels();
      renderMyCompanyBookings();
      updateNewsletterRealtimeValidation({ showEmptyError: false });
    }
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
    if (isAuthenticatedMember(state.user)) {
      await signOutFirebase();
      showToast("Signed out.");
    } else if (!state.firebaseReady) {
      const ready = await connectFirebaseCloud();
      if (ready) {
        setAuthMode("login", { updateUrl: true });
      } else {
        showToast(t("firebaseReconnectingToast"));
      }
    } else {
      setAuthMode("login", { updateUrl: true });
    }
  });

  document.querySelectorAll("[data-auth-tab]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const requested = btn.getAttribute("data-auth-tab");
      const nextMode = requested === "signup" ? "signup" : requested === "reset" ? "reset" : "login";
      setAuthMode(nextMode, { updateUrl: true, clearAlert: true });
    });
  });

  document.getElementById("adminSignOutBtn")?.addEventListener("click", async () => {
    await signOutFirebase();
    showToast("Signed out.");
  });

  document.getElementById("closeAuthModalBtn")?.addEventListener("click", () => {
    state.pendingAdminRoute = false;
    closeAuthModal();
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

  function syncLightboxFullscreenUi(isFullscreen = false) {
    const modal = document.getElementById("lightboxModal");
    const btn = document.getElementById("lightboxFullscreenBtn");
    const enterIcon = document.getElementById("lightboxFullscreenEnterIcon");
    const exitIcon = document.getElementById("lightboxFullscreenExitIcon");
    const label = isFullscreen ? t("lightboxExitFullscreen") : t("lightboxEnterFullscreen");

    modal?.classList.toggle("is-fullscreen", Boolean(isFullscreen));
    if (btn) {
      btn.classList.toggle("is-active", Boolean(isFullscreen));
      btn.setAttribute("aria-pressed", isFullscreen ? "true" : "false");
      btn.setAttribute("aria-label", label);
      btn.setAttribute("title", `${label} (F)`);
    }
    enterIcon?.classList.toggle("hidden", Boolean(isFullscreen));
    exitIcon?.classList.toggle("hidden", !isFullscreen);
  }

  const lightboxFullscreen = createLightboxFullscreenController({
    getTargetEl: () => document.getElementById("lightboxModal"),
    onStateChange: ({ isFullscreen }) => {
      syncLightboxFullscreenUi(isFullscreen);
      if (lightboxZoom.isZoomed()) {
        lightboxZoom.reset();
      }
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
    syncLightboxFullscreenUi(lightboxFullscreen.isFullscreen());
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
    lightboxFullscreen.exit();
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

  document.getElementById("lightboxFullscreenBtn")?.addEventListener("click", () => {
    lightboxFullscreen.toggle();
  });

  document.addEventListener("fullscreenchange", () => {
    lightboxFullscreen.syncFromDocument();
  });
  document.addEventListener("webkitfullscreenchange", () => {
    lightboxFullscreen.syncFromDocument();
  });

  document.getElementById("closeLightboxBtn")?.addEventListener("click", () => {
    closeLightbox();
  });

  document.getElementById("lightboxModal")?.addEventListener("click", (e) => {
    if (e.target === e.currentTarget && !lightboxFullscreen.isFullscreen()) {
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
    if (e.key === "Escape") {
      const qrModal = document.getElementById("portalQrScannerModal");
      if (qrModal && !qrModal.classList.contains("hidden")) {
        e.preventDefault();
        closePortalQrScannerModal();
        return;
      }
    }
    if (e.key === "Escape" && state.activeProfileModelId) {
      const profileModal = document.getElementById("modelProfileModal");
      if (profileModal && !profileModal.classList.contains("hidden")) {
        e.preventDefault();
        closeModelProfileModal();
        return;
      }
    }
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
    } else if (action === "toggle-fullscreen") {
      e.preventDefault();
      lightboxFullscreen.toggle();
    } else if (action === "close") {
      e.preventDefault();
      if (lightboxFullscreen.isFullscreen()) {
        lightboxFullscreen.exit();
      } else {
        closeLightbox();
      }
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

  const handleGoogleAuth = async (mode = "login") => {
    setAuthGateAlert("");
    try {
      if (!state.firebaseReady) {
        const ready = await connectFirebaseCloud();
        if (!ready) {
          const msg = t("firebaseReconnectingToast");
          setAuthGateAlert(msg);
          showToast(msg);
          return;
        }
      }
      const res = await signInWithGooglePopup({ mode });
      closeAuthModal();
      if (mode === "signup") {
        showToast(res?.alreadyRegistered ? t("authGoogleAlreadyRegisteredToast") : t("authSignupSuccessToast"));
      } else {
        showToast(t("authLoginSuccessToast"));
      }
    } catch (err) {
      const msg = mapFirebaseAuthError(err, { mode, t });
      setAuthGateAlert(msg);
      showToast(msg);
      if (err?.code === "auth/unregistered-account") {
        setAuthMode("signup", { updateUrl: true, clearAlert: false });
      }
    }
  };

  document.getElementById("loginGoogleBtn")?.addEventListener("click", () => handleGoogleAuth("login"));
  document.getElementById("signupGoogleBtn")?.addEventListener("click", () => handleGoogleAuth("signup"));
  document.getElementById("googleSignInBtn")?.addEventListener("click", () => handleGoogleAuth("login"));

  document.getElementById("loginAuthForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setAuthGateAlert("");
    const email = document.getElementById("loginEmail")?.value || "";
    const password = document.getElementById("loginPassword")?.value || "";
    const validation = validateLoginInput({ email, password });
    if (!validation.valid) {
      const msg = t(validation.errorKey);
      setAuthGateAlert(msg);
      showToast(msg);
      return;
    }
    try {
      await signInWithEmailPassword(validation.clean.email, validation.clean.password);
      e.target.reset();
      closeAuthModal();
      showToast(t("authLoginSuccessToast"));
    } catch (err) {
      const msg = mapFirebaseAuthError(err, { mode: "login", t });
      setAuthGateAlert(msg);
      showToast(msg);
    }
  });

  document.getElementById("signupAuthForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setAuthGateAlert("");
    const fullName = document.getElementById("signupFullName")?.value || "";
    const email = document.getElementById("signupEmail")?.value || "";
    const password = document.getElementById("signupPassword")?.value || "";
    const confirmPassword = document.getElementById("signupConfirmPassword")?.value || "";
    const validation = validateRegistrationInput({ fullName, email, password, confirmPassword });
    if (!validation.valid) {
      const msg = t(validation.errorKey);
      setAuthGateAlert(msg);
      showToast(msg);
      return;
    }
    try {
      await registerWithEmailPassword(validation.clean.email, validation.clean.password, {
        fullName: validation.clean.fullName
      });
      e.target.reset();
      closeAuthModal();
      showToast(t("authSignupSuccessToast"));
    } catch (err) {
      const msg = mapFirebaseAuthError(err, { mode: "signup", t });
      setAuthGateAlert(msg);
      showToast(msg);
      if (err?.code === "auth/email-already-in-use") {
        setAuthMode("login", { updateUrl: true, clearAlert: false });
      }
    }
  });

  document.getElementById("resetPasswordForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setAuthGateAlert("");
    const email = document.getElementById("resetEmail")?.value || "";
    const validation = validatePasswordResetInput({ email });
    if (!validation.valid) {
      const msg = t(validation.errorKey);
      setAuthGateAlert(msg);
      showToast(msg);
      return;
    }
    try {
      if (!state.firebaseReady) {
        const ready = await connectFirebaseCloud();
        if (!ready) {
          const msg = t("firebaseReconnectingToast");
          setAuthGateAlert(msg);
          showToast(msg);
          return;
        }
      }
      await sendPasswordReset(validation.clean.email);
      e.target.reset();
      const successMsg = t("authResetSuccessToast");
      setAuthGateAlert(successMsg, { isInfo: true });
      showToast(successMsg);
    } catch (err) {
      const code = String(err?.code || "").trim().toLowerCase();
      if (code === "auth/user-not-found") {
        e.target.reset();
        const neutralMsg = t("authResetSuccessToast");
        setAuthGateAlert(neutralMsg, { isInfo: true });
        showToast(neutralMsg);
        return;
      }
      const msg = mapFirebaseAuthError(err, { mode: "reset", t });
      setAuthGateAlert(msg);
      showToast(msg);
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
      showToast(t("authLoginSuccessToast"));
    } catch (err) {
      showToast(mapFirebaseAuthError(err, { mode: "login", t }));
    }
  });

  document.getElementById("emailRegisterBtn")?.addEventListener("click", async () => {
    try {
      await registerWithEmailPassword(
        document.getElementById("authEmail").value,
        document.getElementById("authPassword").value
      );
      closeAuthModal();
      showToast(t("authSignupSuccessToast"));
    } catch (err) {
      showToast(mapFirebaseAuthError(err, { mode: "signup", t }));
    }
  });

  document.getElementById("bookingForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const selectedServiceType = document.getElementById("bookingService").value;
    const matchedWeddingTier = findWeddingTierByBookingValue(
      getEffectiveStudioServices(),
      selectedServiceType
    );
    const weddingPricing = matchedWeddingTier
      ? calculateWeddingBookingTotal(
          matchedWeddingTier,
          state.selectedWeddingAddonIds,
          WEDDING_BOOKING_ADDONS
        )
      : null;
    const rawNotes = document.getElementById("bookingNotes").value;
    const finalNotes = weddingPricing
      ? buildWeddingBookingNotesWithPricing(rawNotes, weddingPricing)
      : rawNotes;

    const payload = {
      clientName: document.getElementById("bookingName").value,
      contactNumber: document.getElementById("bookingPhone").value,
      serviceType: selectedServiceType,
      preferredDate: document.getElementById("bookingDate").value,
      preferredTime: document.getElementById("bookingTime").value,
      location: document.getElementById("bookingLocation").value,
      notes: finalNotes
    };

    const validationError = validateOfflineBookingPayload(payload);
    if (validationError) {
      showToast(validationError);
      return;
    }

    const bookedIndex = buildBookedSlotIndex(state.studioBookedSlots, state.myBookings);
    if (isStudioSlotBooked(bookedIndex, payload.preferredDate, payload.preferredTime)) {
      showToast(
        state.lang === "hi"
          ? "यह तारीख और स्टूडियो स्लॉट पहले से बुक है। कृपया कोई अन्य उपलब्ध स्लॉट चुनें।"
          : "This studio date and time slot is already booked. Please choose another available slot."
      );
      renderBookingSlotCalendar();
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
      state.selectedWeddingAddonIds = [];
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
      state.selectedWeddingAddonIds = [];
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

  document.getElementById("modelDob")?.addEventListener("change", (e) => {
    const computedAge = calculateAgeFromDob(e.target.value);
    const ageEl = document.getElementById("modelAge");
    if (ageEl && Number.isInteger(computedAge)) {
      ageEl.value = String(computedAge);
    }
  });

  document.getElementById("modelPortfolioPhotosInput")?.addEventListener("change", async (e) => {
    const files = Array.from(e.target.files || []);
    if (!files.length) return;
    const remaining = MAX_PORTFOLIO_PHOTOS - state.modelRegPortfolioPhotos.length;
    if (remaining <= 0) {
      showToast(`Maximum ${MAX_PORTFOLIO_PHOTOS} portfolio photos allowed.`);
      e.target.value = "";
      return;
    }
    const batch = files.slice(0, remaining);
    let added = 0;
    for (const file of batch) {
      try {
        const encoded = await encodePortfolioPhotoFile(file);
        state.modelRegPortfolioPhotos.push(encoded.dataUrl);
        added += 1;
      } catch (err) {
        showToast(err.message || "Unable to optimize selected photo.");
      }
    }
    e.target.value = "";
    renderModelRegPortfolioPreview();
    if (added > 0) {
      showToast(`${added} portfolio photo(s) optimized for free cloud sync!`);
    }
  });

  document.addEventListener("change", async (e) => {
    const input = e.target.closest("[data-edit-portfolio-photos-input]");
    if (!input) return;
    const files = Array.from(input.files || []);
    if (!files.length) return;
    const remaining = MAX_PORTFOLIO_PHOTOS - state.editModelDraftPhotos.length;
    if (remaining <= 0) {
      showToast(`Maximum ${MAX_PORTFOLIO_PHOTOS} portfolio photos allowed.`);
      input.value = "";
      return;
    }
    let added = 0;
    for (const file of files.slice(0, remaining)) {
      try {
        const encoded = await encodePortfolioPhotoFile(file);
        state.editModelDraftPhotos.push(encoded.dataUrl);
        added += 1;
      } catch (err) {
        showToast(err.message || "Unable to optimize photo.");
      }
    }
    input.value = "";
    renderMyModels();
    if (added > 0) showToast(`${added} portfolio photo(s) ready to save!`);
  });

  document.addEventListener("submit", async (e) => {
    const form = e.target.closest("[data-model-edit-form]");
    if (!form) return;
    e.preventDefault();
    const modelId = form.getAttribute("data-model-edit-form");
    if (!modelId || !state.user) return;
    const fd = new FormData(form);
    const payload = {
      fullName: fd.get("fullName"),
      gender: fd.get("gender"),
      dateOfBirth: fd.get("dateOfBirth"),
      age: fd.get("age"),
      city: fd.get("city"),
      state: fd.get("state"),
      heightCm: fd.get("heightCm"),
      experienceLevel: fd.get("experienceLevel"),
      skills: fd.get("skills"),
      languages: fd.get("languages"),
      contactEmail: fd.get("contactEmail"),
      phoneNumber: fd.get("phoneNumber"),
      bio: fd.get("bio"),
      portfolioUrls: fd.get("portfolioUrls"),
      introVideoUrl: fd.get("introVideoUrl"),
      portfolioPhotos: [...state.editModelDraftPhotos],
      photoConsent: form.querySelector('input[name="photoConsent"]')?.checked !== false
    };
    const check = validateModelSelfRegistration(payload, { isEdit: true, lang: state.lang });
    if (!check.valid) {
      showToast(check.errors[0]);
      return;
    }
    try {
      await updateMyModelProfile(modelId, check.cleaned);
      state.editingModelId = null;
      state.editModelDraftPhotos = [];
      showToast(t("modelProfileUpdatedToast"));
      await refreshCloudData();
    } catch (err) {
      showToast(err.message || "Unable to update model profile.");
    }
  });

  document.getElementById("modelForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
      fullName: document.getElementById("modelName").value,
      gender: document.getElementById("modelGender").value,
      dateOfBirth: document.getElementById("modelDob")?.value || "",
      age: document.getElementById("modelAge").value,
      city: document.getElementById("modelCity").value,
      state: document.getElementById("modelState")?.value || "",
      heightCm: document.getElementById("modelHeightCm")?.value || "",
      experienceLevel: document.getElementById("modelExperience")?.value || "1-2 Years",
      languages: document.getElementById("modelLanguages")?.value || "",
      skills: document.getElementById("modelSkills").value,
      contactEmail: document.getElementById("modelContactEmail")?.value || "",
      phoneNumber: document.getElementById("modelPhone").value,
      bio: document.getElementById("modelBio")?.value || "",
      portfolioUrls: document.getElementById("modelPortfolio").value,
      introVideoUrl: document.getElementById("modelIntroVideoUrl")?.value || "",
      portfolioPhotos: [...state.modelRegPortfolioPhotos],
      photoConsent: document.getElementById("modelPhotoConsent")?.checked !== false,
      consentGiven: document.getElementById("modelConsent").checked
    };
    const check = validateModelSelfRegistration(payload, { isEdit: false, lang: state.lang });
    if (!check.valid) {
      showToast(check.errors[0]);
      return;
    }
    try {
      await submitModelRegistration(check.cleaned);
      e.target.reset();
      state.modelRegPortfolioPhotos = [];
      renderModelRegPortfolioPreview();
      const photoConsentEl = document.getElementById("modelPhotoConsent");
      if (photoConsentEl) photoConsentEl.checked = true;
      document.querySelectorAll("[data-model-skill-quick]").forEach((btn) => btn.classList.remove("active"));
      showToast("18+ Model registration submitted! Track status in your Model Dashboard.");
      await refreshCloudData();
    } catch (err) {
      showToast(err.message);
      if (!state.user && state.firebaseReady) {
        document.getElementById("authModal")?.classList.remove("hidden");
      }
    }
  });

  document.getElementById("companyRegistrationForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const validation = validateCompanyRegistration(
      {
        companyName: document.getElementById("compRegName")?.value,
        organizationType: document.getElementById("compRegType")?.value,
        contactPerson: document.getElementById("compRegContactPerson")?.value,
        mobileNumber: document.getElementById("compRegMobile")?.value,
        email: document.getElementById("compRegEmail")?.value,
        address: document.getElementById("compRegAddress")?.value
      },
      state.lang
    );
    if (!validation.valid) {
      showToast(validation.errors[0]);
      return;
    }

    if (!state.user) {
      showToast(t("authRequiredNotice"));
      if (state.firebaseReady) {
        openAuthModal({ adminNotice: false });
      } else {
        const ready = await connectFirebaseCloud();
        if (ready) openAuthModal({ adminNotice: false });
        else showToast(t("firebaseReconnectingToast"));
      }
      return;
    }

    try {
      await saveCompanyRegistration(validation.cleaned);
      state.myCompanyProfile = { id: state.user.uid, ...validation.cleaned };
      prefillCompanyFormsFromProfile(state.myCompanyProfile);
      showToast(t("companyProfileSavedToast"));
    } catch (err) {
      showToast(err.message || "Unable to save company registration.");
    }
  });

  document.getElementById("companyBookingForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const validation = validateCompanyModelBooking(
      {
        companyName: document.getElementById("compBookName")?.value,
        organizationType: document.getElementById("compBookType")?.value,
        contactPerson: document.getElementById("compBookContactPerson")?.value,
        mobileNumber: document.getElementById("compBookMobile")?.value,
        email: document.getElementById("compBookEmail")?.value,
        address: document.getElementById("compBookAddress")?.value,
        modelRequirement: document.getElementById("compBookRequirement")?.value,
        modelsCount: document.getElementById("compBookCount")?.value,
        eventType: document.getElementById("compBookEventType")?.value,
        eventDate: document.getElementById("compBookEventDate")?.value,
        eventLocation: document.getElementById("compBookLocation")?.value,
        budget: document.getElementById("compBookBudget")?.value,
        detailedRequirements: document.getElementById("compBookDetails")?.value,
        requestedModelIds: state.selectedRequestedModels.map((m) => m.id),
        requestedModelNames: state.selectedRequestedModels.map((m) => m.fullName)
      },
      state.lang
    );
    if (!validation.valid) {
      showToast(validation.errors[0]);
      return;
    }

    if (!state.user) {
      showToast(t("authRequiredNotice"));
      if (state.firebaseReady) {
        openAuthModal({ adminNotice: false });
      } else {
        const ready = await connectFirebaseCloud();
        if (ready) openAuthModal({ adminNotice: false });
        else showToast(t("firebaseReconnectingToast"));
      }
      return;
    }

    try {
      await submitCompanyModelBooking(validation.cleaned);
      state.selectedRequestedModels = [];
      e.target.reset();
      populateBookingServicesDropdown();
      if (state.myCompanyProfile) {
        prefillCompanyFormsFromProfile(state.myCompanyProfile);
      } else {
        prefillCompanyFormsFromProfile(validation.cleaned);
      }
      renderPublicModels();
      renderSelectedRequestedModels();
      showToast(t("companyBookingSubmittedToast"));
      await refreshCloudData();
    } catch (err) {
      showToast(err.message || "Unable to submit model requirement booking.");
    }
  });

  const galleryFileInput = document.getElementById("galleryImageFile");
  const galleryDropzone = document.getElementById("galleryUploadDropzone");
  const pickGalleryBtn = document.getElementById("pickGalleryImageBtn");
  const clearGalleryBtn = document.getElementById("clearGalleryImageBtn");
  const toggleEditorBtn = document.getElementById("toggleGalleryEditorBtn");
  const editorPanel = document.getElementById("galleryImageEditorPanel");
  const editorCanvas = document.getElementById("galleryEditorCanvas");
  const croppedPreviewCanvas = document.getElementById("galleryCroppedPreviewCanvas");
  const croppedOutputMeta = document.getElementById("galleryCroppedOutputMeta");
  const editorDimsBadge = document.getElementById("galleryEditorDimsBadge");
  const editorAspectGroup = document.getElementById("galleryCropAspectGroup");
  const editorZoomInput = document.getElementById("galleryCropZoom");
  const editorZoomValue = document.getElementById("galleryCropZoomValue");
  const editorBrightnessInput = document.getElementById("galleryBrightnessRange");
  const editorBrightnessValue = document.getElementById("galleryBrightnessValue");
  const resetEditorBtn = document.getElementById("resetGalleryEditorBtn");
  const applyEditorBtn = document.getElementById("applyGalleryEditsBtn");
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
  let galleryEditorSourceImg = null;
  let galleryEditorState = createDefaultEditorState();
  let galleryEditorDirty = false;
  let editorDragState = null;

  function loadEditorSourceImage(src) {
    return new Promise((resolve, reject) => {
      if (!src) {
        reject(new Error("No image source available for editor."));
        return;
      }
      const img = new Image();
      img.onload = () => resolve(img);
      img.onerror = () => reject(new Error("Could not load image into Canvas editor."));
      img.src = src;
    });
  }

  function updateGalleryPreviewMeta(encoded) {
    if (!previewMeta || !encoded) return;
    const exifParts = encoded.metadata
      ? [encoded.metadata.camera, encoded.metadata.aperture, encoded.metadata.iso, encoded.metadata.dateTaken].filter(Boolean)
      : [];
    const editSummary = encoded.edits ? formatEditorSummary(encoded.edits) : "";
    const segments = [`${encoded.width}×${encoded.height}px • ~${encoded.approxKb} KB`];
    if (editSummary) segments.push(editSummary);
    if (exifParts.length) segments.push(`EXIF: ${exifParts.join(" · ")}`);
    previewMeta.textContent = segments.join(" • ");
  }

  function syncGalleryEditorControlValues() {
    if (editorZoomInput) editorZoomInput.value = String(galleryEditorState.zoom);
    if (editorZoomValue) editorZoomValue.textContent = `${galleryEditorState.zoom.toFixed(1)}×`;
    if (editorBrightnessInput) editorBrightnessInput.value = String(galleryEditorState.brightness);
    if (editorBrightnessValue) editorBrightnessValue.textContent = `${galleryEditorState.brightness}%`;
    editorAspectGroup?.querySelectorAll("[data-crop-aspect]").forEach((btn) => {
      const isActive = btn.dataset.cropAspect === galleryEditorState.aspectRatio;
      btn.classList.toggle("active", isActive);
      btn.setAttribute("aria-pressed", String(isActive));
    });
  }

  function renderGalleryEditorPreview() {
    if (!editorCanvas || !galleryEditorSourceImg) return;
    try {
      const workspace = drawCropWorkspaceToCanvas(editorCanvas, galleryEditorSourceImg, galleryEditorState, {
        maxDim: 680
      });
      if (croppedPreviewCanvas) {
        drawEditedImageToCanvas(croppedPreviewCanvas, galleryEditorSourceImg, galleryEditorState, {
          maxDim: 320,
          showGrid: false
        });
      }
      if (editorDimsBadge) {
        const aspectLabel = galleryEditorState.aspectRatio === "original"
          ? `${workspace.crop.sw}×${workspace.crop.sh}px`
          : `${galleryEditorState.aspectRatio} • ${workspace.crop.sw}×${workspace.crop.sh}px`;
        editorDimsBadge.textContent = `${aspectLabel} • ${galleryEditorState.brightness}%`;
      }
      if (croppedOutputMeta) {
        croppedOutputMeta.textContent = `${workspace.crop.sw}×${workspace.crop.sh}px`;
      }
    } catch (err) {
      console.warn("Unable to render Canvas crop preview:", err?.message || err);
    }
  }

  function setGalleryEditorOpen(open) {
    const show = Boolean(open && galleryEditorSourceImg);
    editorPanel?.classList.toggle("hidden", !show);
    toggleEditorBtn?.setAttribute("aria-expanded", String(show));
    if (show) {
      syncGalleryEditorControlValues();
      renderGalleryEditorPreview();
    }
  }

  function commitGalleryEditorChanges({ closePanel = true, showFeedback = true } = {}) {
    if (!galleryEditorSourceImg || !state.uploadedGalleryMedia) return false;
    const prev = state.uploadedGalleryMedia;
    const exported = exportEditedCanvasPayloads({
      img: galleryEditorSourceImg,
      edits: galleryEditorState,
      originalName: prev.originalName,
      metadata: prev.metadata,
      rawDataUrl: prev.rawDataUrl || prev.mediaUrl
    });
    state.uploadedGalleryMedia = {
      ...exported,
      file: exported.file || prev.file
    };
    galleryEditorDirty = false;
    if (previewImg) previewImg.src = exported.thumbnailUrl || exported.mediaUrl;
    updateGalleryPreviewMeta(state.uploadedGalleryMedia);
    if (closePanel) setGalleryEditorOpen(false);
    if (showFeedback) showToast(t("editsAppliedToast"));
    return true;
  }

  function clearGalleryUploadSelection({ keepProgress = false } = {}) {
    state.uploadedGalleryMedia = null;
    state.isEncodingGalleryImage = false;
    galleryEditorSourceImg = null;
    galleryEditorState = createDefaultEditorState();
    galleryEditorDirty = false;
    editorDragState = null;
    setGalleryEditorOpen(false);
    syncGalleryEditorControlValues();
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
    galleryEditorState = createDefaultEditorState();
    galleryEditorDirty = false;
    setGalleryEditorOpen(false);
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
      state.uploadedGalleryMedia = {
        ...encoded,
        edits: createDefaultEditorState(),
        isEdited: false
      };
      try {
        galleryEditorSourceImg = await loadEditorSourceImage(encoded.rawDataUrl || encoded.mediaUrl);
      } catch {
        galleryEditorSourceImg = null;
      }
      if (previewImg) previewImg.src = encoded.thumbnailUrl || encoded.mediaUrl;
      if (previewName) previewName.textContent = encoded.originalName;
      updateGalleryPreviewMeta(state.uploadedGalleryMedia);
      syncGalleryEditorControlValues();
      setGalleryEditorOpen(Boolean(galleryEditorSourceImg));
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

  toggleEditorBtn?.addEventListener("click", () => {
    if (isUploadingGalleryFile || !galleryEditorSourceImg) return;
    const isHidden = editorPanel?.classList.contains("hidden");
    setGalleryEditorOpen(isHidden);
  });

  editorAspectGroup?.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-crop-aspect]");
    if (!btn || isUploadingGalleryFile || !galleryEditorSourceImg) return;
    galleryEditorState = {
      ...galleryEditorState,
      aspectRatio: normalizeAspectPreset(btn.dataset.cropAspect),
      offsetX: 0,
      offsetY: 0
    };
    galleryEditorDirty = true;
    syncGalleryEditorControlValues();
    renderGalleryEditorPreview();
  });

  editorZoomInput?.addEventListener("input", () => {
    if (isUploadingGalleryFile || !galleryEditorSourceImg) return;
    galleryEditorState = {
      ...galleryEditorState,
      zoom: clampCropZoom(editorZoomInput.value)
    };
    galleryEditorDirty = true;
    syncGalleryEditorControlValues();
    renderGalleryEditorPreview();
  });

  editorBrightnessInput?.addEventListener("input", () => {
    if (isUploadingGalleryFile || !galleryEditorSourceImg) return;
    galleryEditorState = {
      ...galleryEditorState,
      brightness: clampBrightness(editorBrightnessInput.value)
    };
    galleryEditorDirty = true;
    syncGalleryEditorControlValues();
    renderGalleryEditorPreview();
  });

  editorCanvas?.addEventListener("pointerdown", (e) => {
    if (isUploadingGalleryFile || !galleryEditorSourceImg) return;
    editorDragState = {
      pointerId: e.pointerId,
      startX: e.clientX,
      startY: e.clientY,
      startOffsetX: galleryEditorState.offsetX,
      startOffsetY: galleryEditorState.offsetY
    };
    editorCanvas.classList.add("is-dragging");
    editorCanvas.setPointerCapture?.(e.pointerId);
  });

  editorCanvas?.addEventListener("pointermove", (e) => {
    if (!editorDragState || e.pointerId !== editorDragState.pointerId || !galleryEditorSourceImg) return;
    const rect = editorCanvas.getBoundingClientRect();
    const scaleX = Math.max(80, rect.width || 300);
    const scaleY = Math.max(80, rect.height || 300);
    const dx = (e.clientX - editorDragState.startX) / (scaleX * 0.5);
    const dy = (e.clientY - editorDragState.startY) / (scaleY * 0.5);
    galleryEditorState = {
      ...galleryEditorState,
      offsetX: clampOffset(editorDragState.startOffsetX + dx),
      offsetY: clampOffset(editorDragState.startOffsetY + dy)
    };
    galleryEditorDirty = true;
    renderGalleryEditorPreview();
  });

  function stopGalleryEditorDrag(e) {
    if (!editorDragState) return;
    if (e && editorDragState.pointerId !== e.pointerId) return;
    editorCanvas?.classList.remove("is-dragging");
    editorDragState = null;
  }

  editorCanvas?.addEventListener("pointerup", stopGalleryEditorDrag);
  editorCanvas?.addEventListener("pointercancel", stopGalleryEditorDrag);
  editorCanvas?.addEventListener("keydown", (e) => {
    if (isUploadingGalleryFile || !galleryEditorSourceImg) return;
    const step = e.shiftKey ? 0.16 : 0.06;
    let dx = 0;
    let dy = 0;
    if (e.key === "ArrowLeft") dx = -step;
    else if (e.key === "ArrowRight") dx = step;
    else if (e.key === "ArrowUp") dy = -step;
    else if (e.key === "ArrowDown") dy = step;
    else return;
    e.preventDefault();
    galleryEditorState = {
      ...galleryEditorState,
      offsetX: clampOffset(galleryEditorState.offsetX + dx),
      offsetY: clampOffset(galleryEditorState.offsetY + dy)
    };
    galleryEditorDirty = true;
    renderGalleryEditorPreview();
  });

  resetEditorBtn?.addEventListener("click", () => {
    if (isUploadingGalleryFile || !galleryEditorSourceImg) return;
    galleryEditorState = createDefaultEditorState();
    syncGalleryEditorControlValues();
    renderGalleryEditorPreview();
    try {
      commitGalleryEditorChanges({ closePanel: false, showFeedback: false });
      showToast(t("editsResetToast"));
    } catch (err) {
      showToast(err.message || "Unable to reset edits.");
    }
  });

  applyEditorBtn?.addEventListener("click", () => {
    if (isUploadingGalleryFile || !galleryEditorSourceImg) return;
    try {
      commitGalleryEditorChanges({ closePanel: true, showFeedback: true });
    } catch (err) {
      showToast(err.message || "Unable to apply image edits.");
    }
  });

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

    if (galleryEditorDirty && galleryEditorSourceImg && state.uploadedGalleryMedia) {
      try {
        commitGalleryEditorChanges({ closePanel: true, showFeedback: false });
      } catch (editErr) {
        showToast(editErr.message || "Unable to apply image edits before upload.");
        return;
      }
    }

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

  document.getElementById("bookingService")?.addEventListener("change", () => {
    renderBookingWeddingSummary();
  });

  document.getElementById("bookingWeddingSummary")?.addEventListener("change", (e) => {
    const addonCheckbox = e.target.closest("input[data-wedding-addon-id]");
    if (!addonCheckbox) return;
    const summaryEl = document.getElementById("bookingWeddingSummary");
    const selectEl = document.getElementById("bookingService");
    if (!summaryEl || !selectEl) return;
    const checkedIds = Array.from(
      summaryEl.querySelectorAll("input[data-wedding-addon-id]:checked")
    ).map((el) => el.getAttribute("data-wedding-addon-id") || el.value);
    state.selectedWeddingAddonIds = checkedIds;
    const matchedTier = findWeddingTierByBookingValue(getEffectiveStudioServices(), selectEl.value);
    if (matchedTier) {
      updateBookingWeddingTotalDom(summaryEl, matchedTier, state.selectedWeddingAddonIds, {
        addons: WEDDING_BOOKING_ADDONS,
        t
      });
    }
  });

  document.addEventListener("input", (e) => {
    const priceInput = e.target.closest("[data-admin-wedding-price-input]");
    if (!priceInput) return;
    const tierId = priceInput.getAttribute("data-admin-wedding-price-input");
    const previewEl = document.querySelector(`[data-admin-wedding-price-preview="${tierId}"]`);
    if (previewEl) {
      const parsed = Number.parseInt(String(priceInput.value || "").replace(/[^\d]/g, ""), 10);
      previewEl.textContent = Number.isInteger(parsed) && parsed > 0 ? formatInrCurrency(parsed) : "₹0";
    }
  });

  document.addEventListener("click", async (e) => {
    const resetBtn = e.target.closest("[data-admin-wedding-reset]");
    if (!resetBtn) return;
    if (!hasVerifiedAdminSession()) {
      showToast(t("adminAccessDeniedToast"));
      return;
    }
    const tierId = resetBtn.getAttribute("data-admin-wedding-reset");
    if (!tierId) return;
    state.savingWeddingTierId = tierId;
    renderAdminConsole();
    try {
      await adminResetWeddingPackageConfig(tierId);
      state.weddingPackageConfigs = state.weddingPackageConfigs.filter(
        (cfg) => String(cfg.tierId || cfg.id) !== tierId
      );
      renderCatalog();
      populateBookingServicesDropdown();
      showToast(t("adminWeddingResetToast"));
    } catch (err) {
      showToast(err.message || "Unable to reset wedding package.");
    } finally {
      state.savingWeddingTierId = null;
      renderAdminConsole();
    }
  });

  document.addEventListener("submit", async (e) => {
    const weddingForm = e.target.closest("[data-admin-wedding-form]");
    if (weddingForm) {
      e.preventDefault();
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      const tierId = weddingForm.getAttribute("data-admin-wedding-form");
      if (!tierId) return;
      const fd = new FormData(weddingForm);
      state.savingWeddingTierId = tierId;
      renderAdminConsole();
      try {
        const saved = await adminSaveWeddingPackageConfig(tierId, {
          priceInr: fd.get("priceInr"),
          durationEn: fd.get("durationEn"),
          turnaroundEn: fd.get("turnaroundEn"),
          includedServices: fd.get("includedServices")
        });
        state.weddingPackageConfigs = [
          ...state.weddingPackageConfigs.filter((cfg) => String(cfg.tierId || cfg.id) !== tierId),
          saved
        ];
        renderCatalog();
        populateBookingServicesDropdown();
        showToast(t("adminWeddingSavedToast").replace("{name}", saved.tierNameEn || "Wedding Package"));
      } catch (err) {
        showToast(err.message || "Unable to save wedding package configuration.");
      } finally {
        state.savingWeddingTierId = null;
        renderAdminConsole();
      }
      return;
    }

    if (e.target.id === "adminEmailAlertSettingsForm") {
      e.preventDefault();
      if (!hasVerifiedAdminSession()) {
        showToast(t("adminAccessDeniedToast"));
        return;
      }
      if (state.isSavingEmailAlertSettings) return;
      state.isSavingEmailAlertSettings = true;
      renderAdminConsole();
      try {
        const bookingEnabled = document.getElementById("adminAlertToggleBooking")?.checked !== false;
        const modelEnabled = document.getElementById("adminAlertToggleModel")?.checked !== false;
        const browserEnabled = document.getElementById("adminAlertToggleBrowser")?.checked !== false;
        const saved = await adminSaveEmailAlertSettings({
          bookingAlertsEnabled: bookingEnabled,
          modelAlertsEnabled: modelEnabled,
          companyAlertsEnabled: true,
          browserAlertsEnabled: browserEnabled
        });
        state.adminEmailAlertSettings = normalizeAdminEmailAlertSettings(saved);
        showToast(t("adminEmailAlertsSavedToast"));
      } catch (err) {
        showToast(err.message || "Unable to save email alert settings.");
      } finally {
        state.isSavingEmailAlertSettings = false;
        renderAdminConsole();
      }
      return;
    }

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
    const authUrlRoute = extractAuthRouteFromUrl({
      pathname: window.location.pathname,
      hash: window.location.hash,
      search: window.location.search
    });
    const sharedModelId = extractSharedModelIdFromUrl({
      hash: window.location.hash,
      search: window.location.search
    });
    if (sharedModelId) {
      state.pendingSharedModelId = sharedModelId;
    }
    const requested = authUrlRoute
      ? authUrlRoute
      : sharedModelId
      ? "companies"
      : extractRequestedRoute({
          pathname: window.location.pathname,
          hash: window.location.hash,
          search: window.location.search
        });
    if (
      !isAuthenticatedMember(state.user) ||
      requested !== state.route ||
      isAdminUrlAttempt({
        pathname: window.location.pathname,
        hash: window.location.hash,
        search: window.location.search
      })
    ) {
      navigateTo(requested);
    }
    if (sharedModelId && isAuthenticatedMember(state.user)) {
      tryOpenPendingSharedModelProfile();
    }
  };
  window.addEventListener("hashchange", handleBrowserUrlChange);
  window.addEventListener("popstate", handleBrowserUrlChange);
  wireProNetworkEvents();
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

function clearProtectedStudioViews() {
  if (state.testimonialTimer) {
    clearInterval(state.testimonialTimer);
    state.testimonialTimer = null;
  }
  state.galleryItems = [];
  state.publicModels = [];
  state.weddingPackageConfigs = [];
  state.reviews = [];
  state.myBookings = [];
  state.myModels = [];
  state.myNotifications = [];
  state.myUserProfile = null;
  state.myCustomerCatalogs = [];
  state.myCustomerSlots = [];
  state.portalDeniedNotice = "";
  state.portalHighlightedBookingId = "";
  state.adminPortalData = { catalogs: [], slots: [] };
  state.studioBookedSlots = [];
  state.paymentConfig = { ...DEFAULT_PAYMENT_CONFIG };
  state.myBookingPayments = [];
  state.adminBookingPayments = [];
  state.editingModelId = null;
  state.editModelDraftPhotos = [];
  state.myCompanyProfile = null;
  state.myCompanyBookings = [];
  state.selectedRequestedModels = [];
  state.publicProfessionals = [];
  state.myProfessionals = [];
  state.myHiringRequests = [];
  state.myAssignedJobs = [];
  state.myOutsourcingRequests = [];
  state.proRegPhotos = [];
  state.hireRefPhotos = [];
  state.outRefPhotos = [];
  state.myChatThreads = [];
  state.activeUserChatId = "";
  state.activeUserChatMessages = [];
  state.userChatDraftText = "";
  state.userChatDraftImage = "";
  state.myChatBlockStatus = { isBlocked: false, reason: "" };
  state._prevUserChatUnread = null;
  inFlightReadReceiptKeys.clear();
  stopUserChatSubscriptions();

  for (const id of [
    "homeFeaturedServicesGrid",
    "homeShowcaseGrid",
    "homeTestimonialCarousel",
    "customerPortalBody",
    "bookingSlotCalendar",
    "catalogGrid",
    "galleryGrid",
    "publicModelsGrid",
    "myBookingsList",
    "myModelsList",
    "myCompanyBookingsList",
    "selectedRequestedModelsBar",
    "offlineBookingsQueueBox",
    "bookingService",
    "approvedProfessionalsDirectory",
    "proMyApplicationsBody",
    "proMyBookingsBody",
    "proMessagesBody",
    "proPortfolioPhotosPreview",
    "hireReferencePhotosPreview",
    "outRefPhotosPreview"
  ]) {
    const el = document.getElementById(id);
    if (el) el.innerHTML = "";
  }
  const desktopChatBadge = document.getElementById("desktopChatUnreadBadge");
  if (desktopChatBadge) {
    desktopChatBadge.textContent = "0";
    desktopChatBadge.classList.add("hidden");
  }
}

function renderProtectedStudioViews() {
  if (!isAuthenticatedMember(state.user)) return;
  if (!state.galleryItems.length) {
    state.galleryItems = [...SHOWCASE_GALLERY_SEEDS];
  }
  if (!state.publicModels.length) {
    state.publicModels = mergePublicModels([]);
  }
  populateBookingServicesDropdown();
  renderBookingSlotCalendar();
  renderCatalog();
  renderGallery();
  renderTestimonialCarousel();
  syncTestimonialAutoplayTimer();
  renderMyBookings();
  renderMyModels();
  renderPublicModels();
  renderSelectedRequestedModels();
  renderMyCompanyBookings();
  renderSavedNewsletterStatus();
  renderProNetworkView();
  renderCustomerPortalView();
}

// ============================================================================
// PROFESSIONAL NETWORK, HIRING, OUTSOURCING & PRIVATE CHAT CONTROLLER
// ============================================================================

let userChatsUnsubscribe = null;
let userChatMsgsUnsubscribe = null;
let adminChatsUnsubscribe = null;
let adminChatMsgsUnsubscribe = null;

function stopUserChatSubscriptions() {
  if (typeof userChatsUnsubscribe === "function") {
    try { userChatsUnsubscribe(); } catch { /* ignore */ }
    userChatsUnsubscribe = null;
  }
  if (typeof userChatMsgsUnsubscribe === "function") {
    try { userChatMsgsUnsubscribe(); } catch { /* ignore */ }
    userChatMsgsUnsubscribe = null;
  }
}

function stopAdminChatSubscriptions() {
  if (typeof adminChatsUnsubscribe === "function") {
    try { adminChatsUnsubscribe(); } catch { /* ignore */ }
    adminChatsUnsubscribe = null;
  }
  if (typeof adminChatMsgsUnsubscribe === "function") {
    try { adminChatMsgsUnsubscribe(); } catch { /* ignore */ }
    adminChatMsgsUnsubscribe = null;
  }
}

function updateChatUnreadBadgesDom() {
  const unread = countUnreadChatsForUser(state.myChatThreads);
  const desktopBadge = document.getElementById("desktopChatUnreadBadge");
  if (desktopBadge) {
    desktopBadge.textContent = String(unread);
    desktopBadge.classList.toggle("hidden", unread <= 0);
  }
  const proMsgBadge = document.querySelector('[data-pro-count="messages"]');
  if (proMsgBadge) {
    proMsgBadge.textContent = String(unread > 0 ? unread : state.myChatThreads.length);
  }
}

async function startUserPrivateChatSubscription() {
  if (!state.firebaseReady || !isAuthenticatedMember(state.user)) return;
  if (typeof userChatsUnsubscribe === "function") return;
  try {
    userChatsUnsubscribe = await subscribeMyPrivateChats(state.user.uid, {
      onThreads: (threads) => {
        const nextUnread = countUnreadChatsForUser(threads);
        if (state._prevUserChatUnread !== null && nextUnread > state._prevUserChatUnread) {
          showToast(t("proToastNewMessageAlert"));
        }
        state._prevUserChatUnread = nextUnread;
        state.myChatThreads = threads;
        if (!state.activeUserChatId && threads.length > 0) {
          selectUserChatThread(threads[0].id);
        }
        updateChatUnreadBadgesDom();
        if (state.route === "network" && state.proTab === "messages") {
          renderProMessagesSection();
        }
        if (state.route === "portal" && state.portalTab === "my-messages") {
          renderCustomerPortalView();
        }
      },
      onError: (err) => {
        console.warn("User private chat listener warning:", err?.message || err);
      }
    });
  } catch (err) {
    console.warn("Unable to start private chat listener:", err?.message || err);
  }
}

const inFlightReadReceiptKeys = new Set();

function isUserViewingPrivateChat() {
  return (
    (state.route === "network" && state.proTab === "messages") ||
    (state.route === "portal" && state.portalTab === "my-messages")
  );
}

async function syncVisibleChatReadReceipts({ asAdmin = false } = {}) {
  if (!state.firebaseReady) return;
  if (asAdmin) {
    if (!hasVerifiedAdminSession()) return;
    if (state.route !== "admin" || state.adminTab !== "private-messages" || !state.activeAdminChatId) return;
    const chatId = state.activeAdminChatId;
    const unreadIds = getUnreadIncomingMessageIds(state.activeAdminChatMessages, { viewerRole: "admin" });
    const pendingIds = unreadIds.filter((id) => !inFlightReadReceiptKeys.has(`${chatId}:${id}`));
    const activeThread = state.adminChatThreads.find((x) => x.id === chatId);
    if (activeThread && Number(activeThread.unreadByAdmin || 0) > 0) {
      markPrivateChatRead(chatId, { asAdmin: true });
    }
    if (!pendingIds.length) return;
    pendingIds.forEach((id) => inFlightReadReceiptKeys.add(`${chatId}:${id}`));
    try {
      await markPrivateChatMessagesRead({ chatId, messageIds: pendingIds, asAdmin: true, readerRole: "admin" });
    } finally {
      pendingIds.forEach((id) => inFlightReadReceiptKeys.delete(`${chatId}:${id}`));
    }
    return;
  }

  if (!isAuthenticatedMember(state.user)) return;
  if (!isUserViewingPrivateChat() || !state.activeUserChatId) return;
  const chatId = state.activeUserChatId;
  const activeThread = state.myChatThreads.find((x) => x.id === chatId);
  const readerRole = activeThread?.participantRole === "professional" ? "professional" : "customer";
  const unreadIds = getUnreadIncomingMessageIds(state.activeUserChatMessages, { viewerRole: readerRole });
  const pendingIds = unreadIds.filter((id) => !inFlightReadReceiptKeys.has(`${chatId}:${id}`));
  if (activeThread && Number(activeThread.unreadByUser || 0) > 0) {
    markPrivateChatRead(chatId, { asAdmin: false });
  }
  if (!pendingIds.length) return;
  pendingIds.forEach((id) => inFlightReadReceiptKeys.add(`${chatId}:${id}`));
  try {
    await markPrivateChatMessagesRead({ chatId, messageIds: pendingIds, asAdmin: false, readerRole });
  } finally {
    pendingIds.forEach((id) => inFlightReadReceiptKeys.delete(`${chatId}:${id}`));
  }
}

async function selectUserChatThread(chatId) {
  state.activeUserChatId = String(chatId || "");
  if (typeof userChatMsgsUnsubscribe === "function") {
    try { userChatMsgsUnsubscribe(); } catch { /* ignore */ }
    userChatMsgsUnsubscribe = null;
  }
  state.activeUserChatMessages = [];
  renderProMessagesSection();
  if (state.route === "portal" && state.portalTab === "my-messages") {
    renderCustomerPortalView();
  }
  if (!state.activeUserChatId || !state.firebaseReady || !isAuthenticatedMember(state.user)) return;
  if (isUserViewingPrivateChat()) {
    markPrivateChatRead(state.activeUserChatId, { asAdmin: false });
  }
  try {
    userChatMsgsUnsubscribe = await subscribePrivateChatMessages(state.activeUserChatId, {
      onMessages: (msgs) => {
        state.activeUserChatMessages = msgs;
        renderProMessagesSection();
        if (state.route === "portal" && state.portalTab === "my-messages") {
          renderCustomerPortalView();
        }
        syncVisibleChatReadReceipts({ asAdmin: false });
      },
      onError: (err) => {
        console.warn("User chat messages listener warning:", err?.message || err);
      }
    });
  } catch (err) {
    console.warn("Unable to subscribe to chat messages:", err?.message || err);
  }
}

async function ensureAdminChatSubscription() {
  if (!hasVerifiedAdminSession() || !state.firebaseReady) return;
  if (typeof adminChatsUnsubscribe === "function") return;
  try {
    adminChatsUnsubscribe = await adminSubscribePrivateChats({
      onThreads: (threads) => {
        if (!hasVerifiedAdminSession()) {
          stopAdminChatSubscriptions();
          return;
        }
        const nextUnread = countUnreadChatsForAdmin(threads);
        if (state._prevAdminChatUnread !== null && nextUnread > state._prevAdminChatUnread) {
          showToast(t("proToastNewMessageAlert"));
        }
        state._prevAdminChatUnread = nextUnread;
        state.adminChatThreads = threads;
        if (!state.activeAdminChatId && threads.length > 0) {
          selectAdminChatThread(threads[0].id);
        }
        if (state.route === "admin") {
          renderAdminConsole();
        }
      },
      onError: (err) => {
        console.warn("Admin private chat listener warning:", err?.message || err);
      }
    });
  } catch (err) {
    console.warn("Unable to start admin private chat listener:", err?.message || err);
  }
}

async function selectAdminChatThread(chatId) {
  state.activeAdminChatId = String(chatId || "");
  if (typeof adminChatMsgsUnsubscribe === "function") {
    try { adminChatMsgsUnsubscribe(); } catch { /* ignore */ }
    adminChatMsgsUnsubscribe = null;
  }
  state.activeAdminChatMessages = [];
  if (state.route === "admin" && state.adminTab === "private-messages") {
    renderAdminConsole();
  }
  if (!state.activeAdminChatId || !hasVerifiedAdminSession() || !state.firebaseReady) return;
  if (state.route === "admin" && state.adminTab === "private-messages") {
    markPrivateChatRead(state.activeAdminChatId, { asAdmin: true });
  }
  try {
    adminChatMsgsUnsubscribe = await subscribePrivateChatMessages(state.activeAdminChatId, {
      onMessages: (msgs) => {
        if (!hasVerifiedAdminSession()) {
          stopAdminChatSubscriptions();
          return;
        }
        state.activeAdminChatMessages = msgs;
        if (state.route === "admin" && state.adminTab === "private-messages") {
          renderAdminConsole();
        }
        syncVisibleChatReadReceipts({ asAdmin: true });
      },
      onError: (err) => {
        console.warn("Admin chat messages listener warning:", err?.message || err);
      }
    });
  } catch (err) {
    console.warn("Unable to subscribe to admin chat messages:", err?.message || err);
  }
}

function renderProPhotoStripPreview(containerId, photos, removeAttr) {
  const host = document.getElementById(containerId);
  if (!host) return;
  if (!Array.isArray(photos) || !photos.length) {
    host.innerHTML = "";
    return;
  }
  host.innerHTML = photos
    .map(
      (url, idx) => `
      <div class="portfolio-photo-thumb">
        <img src="${escapeHtml(url)}" alt="Upload preview ${idx + 1}" />
        <button type="button" class="portfolio-photo-remove" ${removeAttr}="${idx}" aria-label="${escapeHtml(t("removePhotoBtn"))}">×</button>
      </div>`
    )
    .join("");
}

function renderProMessagesSection() {
  const host = document.getElementById("proMessagesBody");
  if (state.route === "portal" && state.portalTab === "my-messages") {
    if (host) host.innerHTML = "";
    renderCustomerPortalView();
    return;
  }
  if (!host) return;
  host.innerHTML = renderUserPrivateChatHtml({
    threads: state.myChatThreads,
    activeChatId: state.activeUserChatId,
    messages: state.activeUserChatMessages,
    currentUserUid: state.user?.uid || "",
    isBlocked: Boolean(state.myChatBlockStatus?.isBlocked),
    blockReason: state.myChatBlockStatus?.reason || "",
    draftText: state.userChatDraftText,
    draftImage: state.userChatDraftImage,
    isSending: state.userChatSending,
    lang: state.lang,
    t,
    escapeHtml
  });
  const timeline = document.getElementById("userPrivateChatTimeline");
  if (timeline) timeline.scrollTop = timeline.scrollHeight;
  syncVisibleChatReadReceipts({ asAdmin: false });
}

function renderProNetworkView() {
  if (!isAuthenticatedMember(state.user)) return;

  const tabs = ["join", "hire", "outsourcing", "my-applications", "my-bookings", "messages"];
  const activeTab = tabs.includes(state.proTab) ? state.proTab : "join";
  state.proTab = activeTab;

  document.querySelectorAll("[data-pro-tab]").forEach((btn) => {
    btn.classList.toggle("active", btn.getAttribute("data-pro-tab") === activeTab);
  });
  tabs.forEach((tabKey) => {
    const panel = document.getElementById(`proPanel-${tabKey}`);
    if (!panel) return;
    const isVisible = tabKey === activeTab;
    panel.classList.toggle("hidden", !isVisible);
    panel.hidden = !isVisible;
  });

  const appsCountEl = document.querySelector('[data-pro-count="my-applications"]');
  if (appsCountEl) {
    appsCountEl.textContent = String(state.myProfessionals.length + state.myOutsourcingRequests.length);
  }
  const bookingsCountEl = document.querySelector('[data-pro-count="my-bookings"]');
  if (bookingsCountEl) {
    bookingsCountEl.textContent = String(state.myHiringRequests.length + state.myAssignedJobs.length);
  }
  updateChatUnreadBadgesDom();

  const dirHost = document.getElementById("approvedProfessionalsDirectory");
  if (dirHost) {
    dirHost.innerHTML = renderApprovedProfessionalsDirectoryHtml({
      professionals: state.publicProfessionals,
      categoryFilter: state.proDirCategory,
      searchQuery: state.proDirSearch,
      t,
      escapeHtml
    });
  }

  const appsHost = document.getElementById("proMyApplicationsBody");
  if (appsHost) {
    appsHost.innerHTML = renderMyProApplicationsHtml({
      myProfiles: state.myProfessionals,
      myOutsourcing: state.myOutsourcingRequests,
      t,
      escapeHtml
    });
  }

  const bookingsHost = document.getElementById("proMyBookingsBody");
  if (bookingsHost) {
    bookingsHost.innerHTML = renderMyHiringBookingsHtml({
      myHiringRequests: state.myHiringRequests,
      assignedToMeJobs: state.myAssignedJobs,
      t,
      escapeHtml
    });
  }

  renderProMessagesSection();
  renderProPhotoStripPreview("proPortfolioPhotosPreview", state.proRegPhotos, "data-remove-pro-photo");
  renderProPhotoStripPreview("hireReferencePhotosPreview", state.hireRefPhotos, "data-remove-hire-photo");
  renderProPhotoStripPreview("outRefPhotosPreview", state.outRefPhotos, "data-remove-out-photo");

  // Prefill email/name convenience for logged-in member if fields are empty
  const proEmail = document.getElementById("proEmail");
  if (proEmail && !proEmail.value && state.user?.email) proEmail.value = state.user.email;
  const proFullName = document.getElementById("proFullName");
  if (proFullName && !proFullName.value && state.user?.displayName) proFullName.value = state.user.displayName;
  const hireName = document.getElementById("hireCustomerName");
  if (hireName && !hireName.value && state.user?.displayName) hireName.value = state.user.displayName;
}

async function handleProMultiPhotoInput(files, targetKey, maxCount) {
  const existing = Array.isArray(state[targetKey]) ? [...state[targetKey]] : [];
  for (const file of Array.from(files || [])) {
    if (existing.length >= maxCount) break;
    try {
      const dataUrl = await encodePortfolioPhotoFile(file);
      if (dataUrl) existing.push(dataUrl);
    } catch (err) {
      showToast(err?.message || "Unable to compress photo.");
    }
  }
  state[targetKey] = existing.slice(0, maxCount);
  renderProNetworkView();
}

function wireProNetworkEvents() {
  document.getElementById("proPortfolioPhotosInput")?.addEventListener("change", async (e) => {
    await handleProMultiPhotoInput(e.target.files, "proRegPhotos", 4);
    e.target.value = "";
  });

  document.getElementById("hireReferencePhotosInput")?.addEventListener("change", async (e) => {
    await handleProMultiPhotoInput(e.target.files, "hireRefPhotos", 3);
    e.target.value = "";
  });

  document.getElementById("outRefPhotosInput")?.addEventListener("change", async (e) => {
    await handleProMultiPhotoInput(e.target.files, "outRefPhotos", 3);
    e.target.value = "";
  });

  document.getElementById("proDirCategoryFilter")?.addEventListener("change", (e) => {
    state.proDirCategory = e.target.value || "all";
    renderProNetworkView();
  });

  document.getElementById("proDirSearchInput")?.addEventListener("input", (e) => {
    state.proDirSearch = e.target.value || "";
    renderProNetworkView();
  });

  // Form 1: Join as a Professional
  document.getElementById("proRegistrationForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!isAuthenticatedMember(state.user)) {
      showToast(t("authMustSignInToast"));
      return;
    }
    const submitBtn = document.getElementById("submitProRegistrationBtn");
    if (submitBtn) submitBtn.disabled = true;
    try {
      await submitProfessionalRegistration({
        fullName: document.getElementById("proFullName")?.value || "",
        mobileNumber: document.getElementById("proMobile")?.value || "",
        email: document.getElementById("proEmail")?.value || "",
        city: document.getElementById("proCity")?.value || "",
        state: document.getElementById("proState")?.value || "",
        category: document.getElementById("proCategory")?.value || "Photographer",
        yearsExperience: document.getElementById("proYearsExp")?.value || 0,
        skills: document.getElementById("proSkills")?.value || "",
        equipmentDetails: document.getElementById("proEquipment")?.value || "",
        workingLocations: document.getElementById("proWorkingLocations")?.value || "",
        expectedCharges: document.getElementById("proExpectedCharges")?.value || "",
        portfolioPhotos: state.proRegPhotos,
        videoLinks: document.getElementById("proVideoLinks")?.value || "",
        shortIntro: document.getElementById("proShortIntro")?.value || "",
        availabilityStatus: document.getElementById("proAvailability")?.value || "Available"
      });
      e.target.reset();
      state.proRegPhotos = [];
      state.proTab = "my-applications";
      showToast(t("proToastJoinSuccess"));
      await refreshCloudData();
    } catch (err) {
      showToast(err?.message || "Unable to submit professional profile.");
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  });

  // Form 2: Hire a Photographer / Videographer
  document.getElementById("proHiringForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!isAuthenticatedMember(state.user)) {
      showToast(t("authMustSignInToast"));
      return;
    }
    const submitBtn = document.getElementById("submitProHiringBtn");
    if (submitBtn) submitBtn.disabled = true;
    try {
      await submitHiringRequest({
        customerName: document.getElementById("hireCustomerName")?.value || "",
        contactNumber: document.getElementById("hireContactNumber")?.value || "",
        eventType: document.getElementById("hireEventType")?.value || "Wedding",
        requiredService: document.getElementById("hireRequiredService")?.value || "Photography",
        eventDate: document.getElementById("hireEventDate")?.value || "",
        eventTime: document.getElementById("hireEventTime")?.value || "",
        eventLocation: document.getElementById("hireEventLocation")?.value || "",
        workingDuration: document.getElementById("hireWorkingDuration")?.value || "",
        budgetRange: document.getElementById("hireBudgetRange")?.value || "",
        specialRequirements: document.getElementById("hireSpecialRequirements")?.value || "",
        referenceLinks: document.getElementById("hireReferenceLinks")?.value || "",
        referencePhotos: state.hireRefPhotos
      });
      e.target.reset();
      state.hireRefPhotos = [];
      state.proTab = "my-bookings";
      showToast(t("proToastHireSuccess"));
      await refreshCloudData();
    } catch (err) {
      showToast(err?.message || "Unable to submit hiring request.");
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  });

  // Form 3: Outsourcing Request
  document.getElementById("proOutsourcingForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!isAuthenticatedMember(state.user)) {
      showToast(t("authMustSignInToast"));
      return;
    }
    const submitBtn = document.getElementById("submitProOutsourcingBtn");
    if (submitBtn) submitBtn.disabled = true;
    try {
      await submitOutsourcingRequest({
        studioOrName: document.getElementById("outStudioName")?.value || "",
        contactInfo: document.getElementById("outContactInfo")?.value || "",
        city: document.getElementById("outCity")?.value || "",
        workType: document.getElementById("outWorkType")?.value || "Photography",
        projectDescription: document.getElementById("outDescription")?.value || "",
        workDeadline: document.getElementById("outDeadline")?.value || "",
        budget: document.getElementById("outBudget")?.value || "",
        referenceLinks: document.getElementById("outRefLinks")?.value || "",
        referencePhotos: state.outRefPhotos,
        additionalMessage: document.getElementById("outAdditionalMsg")?.value || ""
      });
      e.target.reset();
      state.outRefPhotos = [];
      state.proTab = "my-applications";
      showToast(t("proToastOutSuccess"));
      await refreshCloudData();
    } catch (err) {
      showToast(err?.message || "Unable to submit outsourcing request.");
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  });

  // Delegated submit for User & Admin Private Chat Forms
  document.addEventListener("submit", async (e) => {
    if (e.target?.id === "userPrivateChatForm") {
      e.preventDefault();
      if (!isAuthenticatedMember(state.user) || state.myChatBlockStatus?.isBlocked) return;
      const chatId = e.target.getAttribute("data-chat-id") || state.activeUserChatId;
      const inputEl = e.target.querySelector("#userPrivateChatInput");
      const text = inputEl ? inputEl.value : state.userChatDraftText;
      const thread = state.myChatThreads.find((x) => x.id === chatId);
      const role = thread?.participantRole === "professional" ? "professional" : "customer";
      state.userChatSending = true;
      renderProMessagesSection();
      try {
        await sendPrivateChatMessage({
          chatId,
          text,
          imageAttachment: state.userChatDraftImage,
          senderRole: role
        });
        state.userChatDraftText = "";
        state.userChatDraftImage = "";
        showToast(t("proToastChatSent"));
      } catch (err) {
        showToast(err?.message || "Unable to send message.");
      } finally {
        state.userChatSending = false;
        renderProMessagesSection();
      }
      return;
    }

    if (e.target?.id === "adminPrivateChatForm") {
      e.preventDefault();
      if (!hasVerifiedAdminSession()) return;
      const chatId = e.target.getAttribute("data-chat-id") || state.activeAdminChatId;
      const inputEl = document.getElementById("adminPrivateChatInput");
      const text = inputEl ? inputEl.value : state.adminChatDraftText;
      state.adminChatSending = true;
      renderAdminConsole();
      try {
        await sendPrivateChatMessage({
          chatId,
          text,
          imageAttachment: state.adminChatDraftImage,
          senderRole: "admin"
        });
        state.adminChatDraftText = "";
        state.adminChatDraftImage = "";
        showToast(t("proToastChatSent"));
      } catch (err) {
        showToast(err?.message || "Unable to send reply.");
      } finally {
        state.adminChatSending = false;
        renderAdminConsole();
      }
    }
  });

  // Delegated input/change handlers for Chat drafts, image attachments, and Admin Pro search
  document.addEventListener("input", (e) => {
    if (e.target?.id === "userPrivateChatInput") {
      state.userChatDraftText = e.target.value || "";
      return;
    }
    if (e.target?.id === "adminPrivateChatInput") {
      state.adminChatDraftText = e.target.value || "";
      return;
    }
    if (e.target?.matches?.("[data-admin-pro-search]")) {
      state.adminProSearch = e.target.value || "";
      renderAdminConsole();
    }
  });

  document.addEventListener("change", async (e) => {
    const imgTarget = e.target?.getAttribute?.("data-chat-image-input");
    if (!imgTarget) return;
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const dataUrl = await encodePortfolioPhotoFile(file);
      if (imgTarget === "admin") {
        state.adminChatDraftImage = dataUrl;
        renderAdminConsole();
      } else {
        state.userChatDraftImage = dataUrl;
        renderProMessagesSection();
      }
    } catch (err) {
      showToast(err?.message || "Unable to attach image.");
    }
    e.target.value = "";
  });

  // Delegated click handlers for Professional Network, Customer Proposals, Private Chat & Admin Tabs
  document.addEventListener("click", async (e) => {
    const proTabBtn = e.target.closest("[data-pro-tab]");
    if (proTabBtn) {
      state.proTab = proTabBtn.getAttribute("data-pro-tab") || "join";
      renderProNetworkView();
      return;
    }

    const remProPhoto = e.target.closest("[data-remove-pro-photo]");
    if (remProPhoto) {
      const idx = Number(remProPhoto.getAttribute("data-remove-pro-photo"));
      state.proRegPhotos = state.proRegPhotos.filter((_, i) => i !== idx);
      renderProNetworkView();
      return;
    }
    const remHirePhoto = e.target.closest("[data-remove-hire-photo]");
    if (remHirePhoto) {
      const idx = Number(remHirePhoto.getAttribute("data-remove-hire-photo"));
      state.hireRefPhotos = state.hireRefPhotos.filter((_, i) => i !== idx);
      renderProNetworkView();
      return;
    }
    const remOutPhoto = e.target.closest("[data-remove-out-photo]");
    if (remOutPhoto) {
      const idx = Number(remOutPhoto.getAttribute("data-remove-out-photo"));
      state.outRefPhotos = state.outRefPhotos.filter((_, i) => i !== idx);
      renderProNetworkView();
      return;
    }

    // Customer Accept / Decline Proposed Professional (NEVER auto-confirms)
    const proposalBtn = e.target.closest("[data-customer-proposal-action]");
    if (proposalBtn) {
      if (!isAuthenticatedMember(state.user)) return;
      const action = proposalBtn.getAttribute("data-customer-proposal-action");
      const reqId = proposalBtn.getAttribute("data-hiring-id");
      const req = state.myHiringRequests.find((r) => r.id === reqId);
      if (!req) return;
      const noteEl = document.querySelector(`[data-customer-proposal-note="${CSS.escape(reqId)}"]`);
      proposalBtn.disabled = true;
      try {
        await respondToHiringProposal({ request: req, action, note: noteEl?.value || "" });
        showToast(action === "accept" ? t("proToastProposalAccepted") : t("proToastProposalDeclined"));
        await refreshCloudData();
      } catch (err) {
        showToast(err?.message || "Unable to update proposal.");
      } finally {
        proposalBtn.disabled = false;
      }
      return;
    }

    const cancelHireBtn = e.target.closest("[data-cancel-hiring-id]");
    if (cancelHireBtn) {
      const reqId = cancelHireBtn.getAttribute("data-cancel-hiring-id");
      cancelHireBtn.disabled = true;
      try {
        await cancelMyHiringRequest(reqId);
        showToast(t("proToastRequestCancelled"));
        await refreshCloudData();
      } catch (err) {
        showToast(err?.message || "Unable to cancel hiring request.");
      } finally {
        cancelHireBtn.disabled = false;
      }
      return;
    }

    const cancelOutBtn = e.target.closest("[data-cancel-outsourcing-id]");
    if (cancelOutBtn) {
      const reqId = cancelOutBtn.getAttribute("data-cancel-outsourcing-id");
      cancelOutBtn.disabled = true;
      try {
        await cancelMyOutsourcingRequest(reqId);
        showToast(t("proToastRequestCancelled"));
        await refreshCloudData();
      } catch (err) {
        showToast(err?.message || "Unable to cancel outsourcing request.");
      } finally {
        cancelOutBtn.disabled = false;
      }
      return;
    }

    // Open or start Private Chat with Admin (user side)
    const openChatBtn = e.target.closest("[data-open-private-chat]");
    if (openChatBtn) {
      if (!isAuthenticatedMember(state.user)) {
        showToast(t("authMustSignInToast"));
        return;
      }
      const contextType = openChatBtn.getAttribute("data-chat-context-type") || "general";
      const contextId = openChatBtn.getAttribute("data-chat-context-id") || "studio";
      const contextLabel = openChatBtn.getAttribute("data-chat-context-label") || t("chatGeneralThreadLabel");
      const participantRole = openChatBtn.getAttribute("data-chat-role") || "customer";
      try {
        const chatId = await openOrCreatePrivateChatThread({
          participantUid: state.user.uid,
          participantName: state.user.displayName || state.user.email || "Studio Member",
          participantEmail: state.user.email || "",
          participantRole,
          contextType,
          contextId,
          contextLabel
        });
        state.proTab = "messages";
        if (state.route !== "network") navigateTo("network");
        renderProNetworkView();
        await selectUserChatThread(chatId);
      } catch (err) {
        showToast(err?.message || "Unable to open private chat.");
      }
      return;
    }

    const selectChatBtn = e.target.closest("[data-select-chat-id]");
    if (selectChatBtn) {
      const chatId = selectChatBtn.getAttribute("data-select-chat-id");
      await selectUserChatThread(chatId);
      return;
    }

    const clearChatImgBtn = e.target.closest("[data-chat-clear-image]");
    if (clearChatImgBtn) {
      if (clearChatImgBtn.getAttribute("data-chat-clear-image") === "admin") {
        state.adminChatDraftImage = "";
        renderAdminConsole();
      } else {
        state.userChatDraftImage = "";
        renderProMessagesSection();
      }
      return;
    }

    // --- ADMIN PROFESSIONAL NETWORK & CHAT ACTIONS ---
    const adminProFilterBtn = e.target.closest("[data-admin-pro-filter]");
    if (adminProFilterBtn) {
      if (!hasVerifiedAdminSession()) return;
      state.adminProFilter = adminProFilterBtn.getAttribute("data-admin-pro-filter") || "all";
      renderAdminConsole();
      return;
    }

    const adminHireFilterBtn = e.target.closest("[data-admin-hiring-filter]");
    if (adminHireFilterBtn) {
      if (!hasVerifiedAdminSession()) return;
      state.adminHiringFilter = adminHireFilterBtn.getAttribute("data-admin-hiring-filter") || "all";
      renderAdminConsole();
      return;
    }

    const adminProStatusBtn = e.target.closest("[data-admin-pro-status]");
    if (adminProStatusBtn) {
      if (!hasVerifiedAdminSession()) return;
      const proId = adminProStatusBtn.getAttribute("data-pro-id");
      const nextStatus = adminProStatusBtn.getAttribute("data-admin-pro-status");
      const proObj = (state.adminProData?.professionals || []).find((p) => p.id === proId) || null;
      const noteEl = document.querySelector(`[data-admin-pro-note="${CSS.escape(proId)}"]`);
      adminProStatusBtn.disabled = true;
      try {
        await adminUpdateProfessionalStatus(proId, nextStatus, noteEl?.value || "", proObj);
        showToast(t("proToastStatusUpdated"));
        await loadAdminConsole();
        await refreshCloudData();
      } catch (err) {
        showToast(err?.message || "Unable to update professional status.");
      } finally {
        adminProStatusBtn.disabled = false;
      }
      return;
    }

    const proposeHiringBtn = e.target.closest("[data-admin-propose-hiring]");
    if (proposeHiringBtn) {
      if (!hasVerifiedAdminSession()) return;
      const reqId = proposeHiringBtn.getAttribute("data-admin-propose-hiring");
      const proSelect = document.querySelector(`[data-admin-hiring-pro-select="${CSS.escape(reqId)}"]`);
      const selectedProId = proSelect?.value || "";
      const proObj = (state.adminProData?.professionals || []).find(
        (p) => p.id === selectedProId && String(p.status || "").toLowerCase() === "approved"
      );
      if (!proObj) {
        showToast(t("proToastSelectProFirst"));
        return;
      }
      const msgEl = document.querySelector(`[data-admin-hiring-proposal-msg="${CSS.escape(reqId)}"]`);
      const notesEl = document.querySelector(`[data-admin-hiring-notes="${CSS.escape(reqId)}"]`);
      proposeHiringBtn.disabled = true;
      try {
        await adminProposeProfessionalForHiring({
          requestId: reqId,
          professional: proObj,
          proposalMessage: msgEl?.value || "",
          adminNotes: notesEl?.value || ""
        });
        showToast(t("proToastProposalSent"));
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to send proposal.");
      } finally {
        proposeHiringBtn.disabled = false;
      }
      return;
    }

    const quickHiringStatusBtn = e.target.closest("[data-admin-hiring-quick-status]");
    if (quickHiringStatusBtn) {
      if (!hasVerifiedAdminSession()) return;
      const reqId = quickHiringStatusBtn.getAttribute("data-hiring-id");
      const nextStatus = quickHiringStatusBtn.getAttribute("data-admin-hiring-quick-status");
      const notesEl = document.querySelector(`[data-admin-hiring-notes="${CSS.escape(reqId)}"]`);
      quickHiringStatusBtn.disabled = true;
      try {
        await adminUpdateHiringRequestStatus({
          requestId: reqId,
          status: nextStatus,
          adminNotes: notesEl?.value || ""
        });
        showToast(nextStatus === "Confirmed" ? t("proToastFinalConfirmed") : t("proToastStatusUpdated"));
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to update hiring status.");
      } finally {
        quickHiringStatusBtn.disabled = false;
      }
      return;
    }

    const saveHiringStatusBtn = e.target.closest("[data-admin-hiring-save-status]");
    if (saveHiringStatusBtn) {
      if (!hasVerifiedAdminSession()) return;
      const reqId = saveHiringStatusBtn.getAttribute("data-admin-hiring-save-status");
      const statusSel = document.querySelector(`[data-admin-hiring-status-select="${CSS.escape(reqId)}"]`);
      const notesEl = document.querySelector(`[data-admin-hiring-notes="${CSS.escape(reqId)}"]`);
      saveHiringStatusBtn.disabled = true;
      try {
        await adminUpdateHiringRequestStatus({
          requestId: reqId,
          status: statusSel?.value || "Pending Admin Review",
          adminNotes: notesEl?.value || ""
        });
        showToast(statusSel?.value === "Confirmed" ? t("proToastFinalConfirmed") : t("proToastStatusUpdated"));
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to update hiring status.");
      } finally {
        saveHiringStatusBtn.disabled = false;
      }
      return;
    }

    const quickOutStatusBtn = e.target.closest("[data-admin-out-quick-status]");
    if (quickOutStatusBtn) {
      if (!hasVerifiedAdminSession()) return;
      const reqId = quickOutStatusBtn.getAttribute("data-out-id");
      const nextStatus = quickOutStatusBtn.getAttribute("data-admin-out-quick-status");
      const notesEl = document.querySelector(`[data-admin-out-notes="${CSS.escape(reqId)}"]`);
      quickOutStatusBtn.disabled = true;
      try {
        await adminUpdateOutsourcingRequest({
          requestId: reqId,
          status: nextStatus,
          adminNotes: notesEl?.value || ""
        });
        showToast(t("proToastOutUpdated"));
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to update outsourcing request.");
      } finally {
        quickOutStatusBtn.disabled = false;
      }
      return;
    }

    const saveOutBtn = e.target.closest("[data-admin-out-save]");
    if (saveOutBtn) {
      if (!hasVerifiedAdminSession()) return;
      const reqId = saveOutBtn.getAttribute("data-admin-out-save");
      const statusSel = document.querySelector(`[data-admin-out-status-select="${CSS.escape(reqId)}"]`);
      const proSel = document.querySelector(`[data-admin-out-pro-select="${CSS.escape(reqId)}"]`);
      const notesEl = document.querySelector(`[data-admin-out-notes="${CSS.escape(reqId)}"]`);
      const proObj = (state.adminProData?.professionals || []).find(
        (p) => p.id === (proSel?.value || "") && String(p.status || "").toLowerCase() === "approved"
      ) || null;
      saveOutBtn.disabled = true;
      try {
        await adminUpdateOutsourcingRequest({
          requestId: reqId,
          status: statusSel?.value || "Pending Admin Review",
          professional: proObj,
          adminNotes: notesEl?.value || ""
        });
        showToast(t("proToastOutUpdated"));
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to update outsourcing request.");
      } finally {
        saveOutBtn.disabled = false;
      }
      return;
    }

    const adminSelectChatBtn = e.target.closest("[data-admin-select-chat-id]");
    if (adminSelectChatBtn) {
      if (!hasVerifiedAdminSession()) return;
      await selectAdminChatThread(adminSelectChatBtn.getAttribute("data-admin-select-chat-id"));
      return;
    }

    const adminBlockBtn = e.target.closest("[data-admin-toggle-chat-block-uid]");
    if (adminBlockBtn) {
      if (!hasVerifiedAdminSession()) return;
      const targetUid = adminBlockBtn.getAttribute("data-admin-toggle-chat-block-uid");
      const nextBlocked = adminBlockBtn.getAttribute("data-admin-toggle-chat-block-next") === "true";
      adminBlockBtn.disabled = true;
      try {
        await adminSetChatUserBlocked({ userId: targetUid, isBlocked: nextBlocked });
        showToast(nextBlocked ? t("proToastUserBlocked") : t("proToastUserUnblocked"));
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to update chat moderation status.");
      } finally {
        adminBlockBtn.disabled = false;
      }
      return;
    }

    const adminStartChatBtn = e.target.closest("[data-admin-start-chat-uid]");
    if (adminStartChatBtn) {
      if (!hasVerifiedAdminSession()) return;
      const participantUid = adminStartChatBtn.getAttribute("data-admin-start-chat-uid");
      if (!participantUid) return;
      try {
        const chatId = await openOrCreatePrivateChatThread({
          participantUid,
          participantName: adminStartChatBtn.getAttribute("data-admin-start-chat-name") || "Studio Member",
          participantEmail: adminStartChatBtn.getAttribute("data-admin-start-chat-email") || "",
          participantRole: adminStartChatBtn.getAttribute("data-admin-start-chat-role") || "customer",
          contextType: adminStartChatBtn.getAttribute("data-admin-start-chat-ctx-type") || "general",
          contextId: adminStartChatBtn.getAttribute("data-admin-start-chat-ctx-id") || "studio",
          contextLabel: adminStartChatBtn.getAttribute("data-admin-start-chat-ctx-label") || "Studio Support"
        });
        state.adminTab = "private-messages";
        document
          .querySelectorAll("[data-admin-tab]")
          .forEach((b) => b.classList.toggle("active", b.getAttribute("data-admin-tab") === "private-messages"));
        await selectAdminChatThread(chatId);
        renderAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to open conversation.");
      }
      return;
    }

    // --- CUSTOMER PRIVATE BOOKING PORTAL ACTIONS ---
    const portalTabBtn = e.target.closest("[data-portal-tab]");
    if (portalTabBtn) {
      const nextTab = portalTabBtn.getAttribute("data-portal-tab") || "my-catalog";
      if (CUSTOMER_PORTAL_TABS.includes(nextTab)) {
        state.portalTab = nextTab;
        state.portalDeniedNotice = "";
        renderCustomerPortalView();
        if (nextTab === "payment-history") {
          loadCustomerPaymentHistory();
        }
      }
      return;
    }

    const payHistoryFilterBtn = e.target.closest("[data-payment-history-filter]");
    if (payHistoryFilterBtn) {
      state.portalPaymentHistoryFilter =
        payHistoryFilterBtn.getAttribute("data-payment-history-filter") || "all";
      renderCustomerPortalView();
      return;
    }

    const refreshPayHistoryBtn = e.target.closest("[data-refresh-payment-history]");
    if (refreshPayHistoryBtn) {
      loadCustomerPaymentHistory({ showFeedback: true });
      return;
    }

    const copyHistoryUtrBtn = e.target.closest("[data-copy-history-utr]");
    if (copyHistoryUtrBtn) {
      const utrValue = copyHistoryUtrBtn.getAttribute("data-copy-history-utr") || "";
      if (utrValue) {
        try {
          if (navigator.clipboard?.writeText) {
            await navigator.clipboard.writeText(utrValue);
          }
          showToast(t("portalPayHistoryUtrCopiedToast"));
        } catch {
          showToast(utrValue);
        }
      }
      return;
    }

    const portalMsgAdminBtn = e.target.closest("[data-portal-message-admin]");
    if (portalMsgAdminBtn) {
      if (!isAuthenticatedMember(state.user)) {
        showToast(t("authMustSignInToast"));
        return;
      }
      try {
        const chatId = await openOrCreatePrivateChatThread({
          participantUid: state.user.uid,
          participantName: state.user.displayName || state.user.email || "Customer",
          participantEmail: state.user.email || "",
          participantRole: "customer",
          contextType: portalMsgAdminBtn.getAttribute("data-chat-context-type") || "hiring",
          contextId: portalMsgAdminBtn.getAttribute("data-chat-context-id") || "studio",
          contextLabel: portalMsgAdminBtn.getAttribute("data-chat-context-label") || t("chatGeneralThreadLabel")
        });
        state.portalTab = "my-messages";
        if (state.route !== "portal") navigateTo("portal");
        await selectUserChatThread(chatId);
        renderCustomerPortalView();
      } catch (err) {
        showToast(err?.message || "Unable to open private message.");
      }
      return;
    }

    // --- ADMIN CUSTOMER PORTAL MANAGEMENT ACTIONS ---
    const delCatalogBtn = e.target.closest("[data-admin-delete-catalog]");
    if (delCatalogBtn) {
      if (!hasVerifiedAdminSession()) return;
      const catalogId = delCatalogBtn.getAttribute("data-admin-delete-catalog");
      if (!catalogId) return;
      delCatalogBtn.disabled = true;
      try {
        await adminDeleteCustomerCatalog(catalogId);
        showToast(t("portalToastDeleted"));
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to delete catalog.");
      } finally {
        delCatalogBtn.disabled = false;
      }
      return;
    }

    const delSlotBtn = e.target.closest("[data-admin-delete-slot]");
    if (delSlotBtn) {
      if (!hasVerifiedAdminSession()) return;
      const slotId = delSlotBtn.getAttribute("data-admin-delete-slot");
      if (!slotId) return;
      delSlotBtn.disabled = true;
      try {
        await adminDeleteCustomerSlot(slotId);
        showToast(t("portalToastDeleted"));
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to delete slot.");
      } finally {
        delSlotBtn.disabled = false;
      }
    }
  });

  // Quick-pick customer autofill & slot status updates inside Admin Customer Portal tab
  document.addEventListener("change", async (e) => {
    const catPick = e.target.closest("#adminCatalogQuickPick");
    if (catPick && hasVerifiedAdminSession()) {
      const opt = catPick.selectedOptions?.[0];
      if (opt && opt.value) {
        const uidEl = document.getElementById("adminCatalogCustomerUid");
        const bkEl = document.getElementById("adminCatalogBookingId");
        const titleEl = document.getElementById("adminCatalogTitle");
        if (uidEl) uidEl.value = opt.value;
        if (bkEl) bkEl.value = opt.getAttribute("data-booking-id") || "";
        if (titleEl && !titleEl.value.trim()) {
          titleEl.value = `${opt.getAttribute("data-event-title") || "Studio"} — Private Catalog`;
        }
      }
      return;
    }

    const slotPick = e.target.closest("#adminSlotQuickPick");
    if (slotPick && hasVerifiedAdminSession()) {
      const opt = slotPick.selectedOptions?.[0];
      if (opt && opt.value) {
        const setVal = (id, v) => {
          const el = document.getElementById(id);
          if (el && v) el.value = v;
        };
        setVal("adminSlotCustomerUid", opt.value);
        setVal("adminSlotBookingId", opt.getAttribute("data-booking-id") || "");
        setVal("adminSlotEventTitle", opt.getAttribute("data-event-title") || "");
        setVal("adminSlotDate", opt.getAttribute("data-event-date") || "");
        setVal("adminSlotTime", opt.getAttribute("data-event-time") || "");
        setVal("adminSlotLocation", opt.getAttribute("data-location") || "");
      }
      return;
    }

    const slotStatusSel = e.target.closest("[data-admin-slot-status]");
    if (slotStatusSel && hasVerifiedAdminSession()) {
      const slotId = slotStatusSel.getAttribute("data-admin-slot-status");
      const nextStatus = slotStatusSel.value;
      if (!slotId) return;
      try {
        await adminUpdateCustomerSlotStatus(slotId, nextStatus);
        showToast(t("portalToastSlotStatusUpdated"));
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to update slot approval status.");
      }
    }
  });

  // Admin Assign Private Catalog & Assign Private Slot form submissions
  document.addEventListener("submit", async (e) => {
    const catForm = e.target.closest("#adminAssignCatalogForm");
    if (catForm) {
      e.preventDefault();
      if (!hasVerifiedAdminSession()) return;
      const check = validateCustomerCatalogPayload({
        customerUid: document.getElementById("adminCatalogCustomerUid")?.value || "",
        bookingId: document.getElementById("adminCatalogBookingId")?.value || "",
        title: document.getElementById("adminCatalogTitle")?.value || "",
        category: document.getElementById("adminCatalogCategory")?.value || "",
        description: document.getElementById("adminCatalogDescription")?.value || "",
        itemsText: document.getElementById("adminCatalogItems")?.value || ""
      });
      if (!check.valid) {
        showToast(check.error);
        return;
      }
      try {
        await adminAssignCustomerCatalog(check.clean);
        showToast(t("portalToastCatalogAssigned"));
        catForm.reset();
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to assign private catalog.");
      }
      return;
    }

    const slotForm = e.target.closest("#adminAssignSlotForm");
    if (slotForm) {
      e.preventDefault();
      if (!hasVerifiedAdminSession()) return;
      const check = validateCustomerSlotPayload({
        customerUid: document.getElementById("adminSlotCustomerUid")?.value || "",
        bookingId: document.getElementById("adminSlotBookingId")?.value || "",
        eventTitle: document.getElementById("adminSlotEventTitle")?.value || "",
        eventDate: document.getElementById("adminSlotDate")?.value || "",
        eventTime: document.getElementById("adminSlotTime")?.value || "",
        slotLabel: document.getElementById("adminSlotLabel")?.value || "",
        location: document.getElementById("adminSlotLocation")?.value || "",
        approvalStatus: document.getElementById("adminSlotApprovalStatus")?.value || "Pending",
        customerNote: document.getElementById("adminSlotCustomerNote")?.value || ""
      });
      if (!check.valid) {
        showToast(check.error);
        return;
      }
      try {
        await adminAssignCustomerSlot(check.clean);
        showToast(t("portalToastSlotAssigned"));
        slotForm.reset();
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to assign booking slot.");
      }
      return;
    }

    // Customer Model Advance Payment Request Form
    const modelPayForm = e.target.closest("#customerModelPaymentForm");
    if (modelPayForm) {
      e.preventDefault();
      if (!isAuthenticatedMember(state.user)) return;
      const sel = document.getElementById("payModelSelect");
      const selOpt = sel?.options?.[sel.selectedIndex];
      try {
        await createModelAdvancePaymentRequest({
          customerName: document.getElementById("payModelCustomerName")?.value || "",
          customerContact: document.getElementById("payModelContact")?.value || "",
          modelId: sel?.value || "",
          modelName: selOpt?.getAttribute("data-model-name") || selOpt?.textContent || "",
          serviceType: document.getElementById("payModelService")?.value || "",
          shootDate: document.getElementById("payModelDate")?.value || "",
          shootTime: document.getElementById("payModelTime")?.value || "",
          eventDetails: document.getElementById("payModelNotes")?.value || ""
        });
        showToast(state.lang === "hi" ? "मॉडल बुकिंग अनुरोध भेज दिया गया (Payment Pending)!" : "Model booking request submitted (Payment Pending)!");
        await refreshCloudData();
      } catch (err) {
        showToast(err?.message || "Unable to submit model booking request.");
      }
      return;
    }

    // Customer Wedding / Event Payment Request Form
    const wedPayForm = e.target.closest("#customerWeddingPaymentForm");
    if (wedPayForm) {
      e.preventDefault();
      if (!isAuthenticatedMember(state.user)) return;
      try {
        await createWeddingBookingPaymentRequest({
          customerName: document.getElementById("payWedCustomerName")?.value || "",
          customerContact: document.getElementById("payWedContact")?.value || "",
          eventType: document.getElementById("payWedEventType")?.value || "",
          shootDate: document.getElementById("payWedDate")?.value || "",
          shootTime: document.getElementById("payWedTime")?.value || "",
          eventLocation: document.getElementById("payWedLocation")?.value || "",
          eventDetails: document.getElementById("payWedDetails")?.value || ""
        });
        showToast(state.lang === "hi" ? "शादी/इवेंट बुकिंग अनुरोध एडमिन समीक्षा हेतु भेज दिया गया!" : "Wedding / event booking request submitted for Admin pricing!");
        await refreshCloudData();
      } catch (err) {
        showToast(err?.message || "Unable to submit wedding booking request.");
      }
      return;
    }

    // Customer UTR Submission Form
    const utrForm = e.target.closest("[data-customer-utr-form]");
    if (utrForm) {
      e.preventDefault();
      if (!isAuthenticatedMember(state.user)) return;
      const paymentId = utrForm.getAttribute("data-customer-utr-form") || "";
      const utrInput = utrForm.querySelector("[name='utrNumber']");
      const utrNumber = utrInput?.value || "";
      const fileInput = utrForm.querySelector("[name='screenshotFile']");
      const errBox = utrForm.querySelector("[data-utr-error-box]");
      const dupAlertBox = utrForm.querySelector("[data-duplicate-utr-alert-box]");
      const clearUtrAlerts = () => {
        if (errBox) {
          errBox.textContent = "";
          errBox.classList.add("hidden");
        }
        if (dupAlertBox) {
          dupAlertBox.innerHTML = "";
          dupAlertBox.classList.add("hidden");
        }
        if (utrInput) {
          utrInput.classList.remove("is-duplicate-utr-input");
          utrInput.removeAttribute("aria-invalid");
        }
      };
      const showUtrError = (msg) => {
        clearUtrAlerts();
        if (errBox) {
          errBox.textContent = msg;
          errBox.classList.remove("hidden");
        }
        showToast(msg);
      };
      const showDuplicateUtrAlert = (err) => {
        clearUtrAlerts();
        const alertMsg = err?.message || "This UTR has already been submitted. Please check your transaction reference number.";
        if (dupAlertBox) {
          dupAlertBox.innerHTML = renderDuplicateUtrAlertHtml({
            utrNumber: err?.utrNumber || utrNumber,
            message: alertMsg,
            title: t("payDupUtrAlertTitle"),
            hint: t("payDupUtrAlertHint"),
            escapeHtml
          });
          dupAlertBox.classList.remove("hidden");
        } else if (errBox) {
          errBox.textContent = alertMsg;
          errBox.classList.remove("hidden");
        }
        if (utrInput) {
          utrInput.classList.add("is-duplicate-utr-input");
          utrInput.setAttribute("aria-invalid", "true");
          utrInput.focus();
        }
        showToast(`${t("payDupUtrAlertTitle")}: ${alertMsg}`);
      };
      clearUtrAlerts();
      let screenshotDataUrl = "";
      if (fileInput?.files?.[0]) {
        try {
          screenshotDataUrl = await compressPaymentImageFile(fileInput.files[0]);
        } catch (imgErr) {
          showUtrError(imgErr?.message || "Could not process payment screenshot.");
          return;
        }
      }
      try {
        await submitCustomerPaymentUtr({ paymentId, utrNumber, screenshotDataUrl });
        showToast(state.lang === "hi" ? "UTR सत्यापन हेतु जमा कर दिया गया!" : "UTR submitted for Admin verification!");
        await refreshCloudData();
      } catch (err) {
        if (isDuplicateUtrError(err)) {
          showDuplicateUtrAlert(err);
        } else {
          showUtrError(err?.message || "Unable to submit UTR.");
        }
      }
      return;
    }

    // Admin UPI Config Form
    const upiCfgForm = e.target.closest("#adminUpiConfigForm");
    if (upiCfgForm) {
      e.preventDefault();
      if (!hasVerifiedAdminSession()) return;
      if (state.adminQrUploadState?.status === "uploading") {
        showToast(t("adminPayQrUploadingBadge"));
        return;
      }
      const showQrFormError = (msg) => {
        const cleanMsg = String(msg || "Unable to save UPI configuration.");
        state.adminQrUploadState = {
          ...(state.adminQrUploadState || {}),
          status: "error",
          error: cleanMsg
        };
        const errBox = document.getElementById("adminUpiQrErrorBox");
        if (errBox) {
          errBox.textContent = cleanMsg;
          errBox.classList.remove("hidden");
          errBox.scrollIntoView?.({ behavior: "smooth", block: "nearest" });
        }
        showToast(cleanMsg);
      };

      let qrImageDataUrl = String(state.paymentConfig?.qrImageDataUrl || "").trim();
      const qrFile = document.getElementById("adminUpiQrFile")?.files?.[0];
      let replacedQrInSubmit = false;
      if (qrFile) {
        const rawQrCheck = await verifyQrCode(qrFile);
        if (!rawQrCheck.valid) {
          showQrFormError(rawQrCheck.error);
          return;
        }
        try {
          const compressed = await compressPaymentImageFile(qrFile);
          if (compressed !== qrImageDataUrl) {
            qrImageDataUrl = compressed;
            replacedQrInSubmit = true;
          }
        } catch (imgErr) {
          showQrFormError(imgErr?.message || "Could not process QR image.");
          return;
        }
      }

      const hasValidSavedQr = isUploadedQrImageDataUrl(qrImageDataUrl);
      const nextUpiId = String(document.getElementById("adminUpiId")?.value || "").trim() || OFFICIAL_STUDIO_UPI_ID;
      if (hasValidSavedQr) {
        if (nextUpiId.toLowerCase() !== OFFICIAL_STUDIO_UPI_ID.toLowerCase()) {
          showQrFormError(
            `Official Studio UPI ID must remain "${OFFICIAL_STUDIO_UPI_ID}" to match the verified Studio QR code.`
          );
          return;
        }
        const savedQrCheck = await verifyQrCode(qrImageDataUrl);
        if (!savedQrCheck.valid) {
          showQrFormError(savedQrCheck.error);
          return;
        }
      }
      const upiChanged =
        Boolean(state.paymentConfig?.upiId) &&
        nextUpiId.toLowerCase() !== String(state.paymentConfig.upiId).trim().toLowerCase();
      const qrVerifiedChecked = Boolean(document.getElementById("adminUpiQrVerified")?.checked);
      const enabledChecked = Boolean(document.getElementById("adminUpiEnabled")?.checked);

      if ((qrVerifiedChecked || enabledChecked) && !hasValidSavedQr) {
        showQrFormError(t("adminPayQrNeedUploadFirst"));
        return;
      }

      // Replacing the QR image or changing the UPI ID requires saving & re-verifying the QR before activating payments
      const effectiveQrVerified = replacedQrInSubmit || upiChanged ? false : qrVerifiedChecked && hasValidSavedQr;
      const effectiveEnabled = effectiveQrVerified && hasValidSavedQr ? enabledChecked : false;
      if ((replacedQrInSubmit || upiChanged) && (qrVerifiedChecked || enabledChecked)) {
        showToast(
          state.lang === "hi"
            ? "नया QR/UPI सहेजा गया। कृपया QR स्कैन कर सत्यापित करें, फिर भुगतान चालू करें।"
            : "New QR/UPI saved in disabled state. Verify that the QR matches the official UPI ID before activating payments."
        );
      }

      try {
        const savedConfig = await adminSavePaymentSettings({
          enabled: effectiveEnabled,
          upiId: nextUpiId,
          payeeName: document.getElementById("adminUpiPayeeName")?.value || "STAR ARTIST STUDIO",
          qrImageDataUrl,
          qrVerified: effectiveQrVerified,
          instructions: document.getElementById("adminUpiInstructions")?.value || "",
          modelAdvanceMode: document.getElementById("adminModelAdvMode")?.value || "percentage",
          modelAdvanceValue: Number(document.getElementById("adminModelAdvValue")?.value || 0),
          weddingChargeMode: document.getElementById("adminWedChargeMode")?.value || "percentage",
          weddingChargeValue: Number(document.getElementById("adminWedChargeValue")?.value || 0)
        });
        const approxKb = hasValidSavedQr ? Math.max(1, Math.round((qrImageDataUrl.length * 0.75) / 1024)) : 0;
        state.paymentConfig = { ...DEFAULT_PAYMENT_CONFIG, ...savedConfig };
        state.adminQrUploadState = {
          status: isUploadedQrImageDataUrl(state.paymentConfig.qrImageDataUrl) ? "saved" : "idle",
          previewDataUrl: "",
          fileName: "",
          error: "",
          approxKb
        };
        showToast(state.lang === "hi" ? "UPI भुगतान सेटिंग्स सहेजी गईं!" : "UPI payment configuration saved!");
        renderAdminConsole();
        await loadAdminConsole();
      } catch (err) {
        showQrFormError(err?.message || "Unable to save UPI configuration.");
      }
      return;
    }

    // Admin Set Package Price & Advance Charge
    const setPriceForm = e.target.closest("[data-admin-set-price-form]");
    if (setPriceForm) {
      e.preventDefault();
      if (!hasVerifiedAdminSession()) return;
      const paymentId = setPriceForm.getAttribute("data-admin-set-price-form") || "";
      try {
        await adminSetBookingPaymentPricing({
          paymentId,
          totalPrice: Number(setPriceForm.querySelector("[name='totalPrice']")?.value || 0),
          chargeMode: setPriceForm.querySelector("[name='chargeMode']")?.value || "percentage",
          chargeValue: Number(setPriceForm.querySelector("[name='chargeValue']")?.value || 0)
        });
        showToast("Admin price & booking charge updated!");
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to update pricing.");
      }
      return;
    }

    // Admin Verify Manual UPI Payment
    const verifyPayForm = e.target.closest("[data-admin-verify-pay-form]");
    if (verifyPayForm) {
      e.preventDefault();
      if (!hasVerifiedAdminSession()) return;
      const paymentId = verifyPayForm.getAttribute("data-admin-verify-pay-form") || "";
      try {
        await adminVerifyBookingPayment({
          paymentId,
          verifiedAmount: Number(verifyPayForm.querySelector("[name='verifiedAmount']")?.value || 0),
          adminNotes: verifyPayForm.querySelector("[name='adminNotes']")?.value || "",
          confirmBooking: Boolean(verifyPayForm.querySelector("[name='confirmBooking']")?.checked)
        });
        showToast("Payment verified & balance updated!");
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to verify payment.");
      }
      return;
    }

    // Admin Reject Payment
    const rejectPayForm = e.target.closest("[data-admin-reject-pay-form]");
    if (rejectPayForm) {
      e.preventDefault();
      if (!hasVerifiedAdminSession()) return;
      const paymentId = rejectPayForm.getAttribute("data-admin-reject-pay-form") || "";
      try {
        await adminRejectBookingPayment({
          paymentId,
          rejectionReason: rejectPayForm.querySelector("[name='rejectionReason']")?.value || ""
        });
        showToast("Payment marked as Payment Rejected.");
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to reject payment.");
      }
      return;
    }

    // Admin Refund Processed
    const refundPayForm = e.target.closest("[data-admin-refund-pay-form]");
    if (refundPayForm) {
      e.preventDefault();
      if (!hasVerifiedAdminSession()) return;
      const paymentId = refundPayForm.getAttribute("data-admin-refund-pay-form") || "";
      try {
        await adminProcessBookingPaymentRefund({
          paymentId,
          refundAmount: Number(refundPayForm.querySelector("[name='refundAmount']")?.value || 0),
          refundReference: refundPayForm.querySelector("[name='refundReference']")?.value || ""
        });
        showToast("Refund recorded as Refund Processed.");
        await loadAdminConsole();
      } catch (err) {
        showToast(err?.message || "Unable to record refund.");
      }
    }
  });

  // Booking Calendar & Payment System delegated clicks
  document.addEventListener("click", async (e) => {
    const calNav = e.target.closest("[data-cal-nav]");
    if (calNav) {
      const delta = Number(calNav.getAttribute("data-cal-nav") || 0);
      const nextDate = new Date(state.bookingCalYear, state.bookingCalMonth + delta, 1);
      state.bookingCalYear = nextDate.getFullYear();
      state.bookingCalMonth = nextDate.getMonth();
      renderBookingSlotCalendar();
      return;
    }

    const calDayBtn = e.target.closest("[data-cal-date]");
    if (calDayBtn && !calDayBtn.disabled) {
      const pickedDate = calDayBtn.getAttribute("data-cal-date") || "";
      const dateEl = document.getElementById("bookingDate");
      const timeEl = document.getElementById("bookingTime");
      if (dateEl && pickedDate) {
        dateEl.value = pickedDate;
        const slotIndex = buildBookedSlotIndex(state.studioBookedSlots, state.myBookings);
        if (timeEl && isStudioSlotBooked(slotIndex, pickedDate, timeEl.value)) {
          const openTime = getFirstAvailableTimeForDate(slotIndex, pickedDate);
          if (openTime) timeEl.value = openTime;
        }
        renderBookingSlotCalendar();
      }
      return;
    }

    const calSlotBtn = e.target.closest("[data-cal-slot]");
    if (calSlotBtn && !calSlotBtn.disabled) {
      const pickedTime = calSlotBtn.getAttribute("data-cal-slot") || "";
      const timeEl = document.getElementById("bookingTime");
      if (timeEl && pickedTime) {
        timeEl.value = pickedTime;
        renderBookingSlotCalendar();
      }
      return;
    }

    const payFormTab = e.target.closest("[data-payment-form-tab]");
    if (payFormTab) {
      const tabVal = payFormTab.getAttribute("data-payment-form-tab");
      state.activePaymentForm = tabVal === "wedding" ? "wedding" : "model";
      renderCustomerPortalView();
      return;
    }

    const copyUpiBtn = e.target.closest("[data-copy-upi]");
    if (copyUpiBtn) {
      const upi = copyUpiBtn.getAttribute("data-copy-upi") || "";
      if (upi && navigator.clipboard?.writeText) {
        try {
          await navigator.clipboard.writeText(upi);
          showToast(`UPI ID copied: ${upi}`);
        } catch {
          showToast(`UPI ID: ${upi}`);
        }
      }
      return;
    }

    const openQrScanBtn = e.target.closest("[data-open-portal-qr-scanner]");
    if (openQrScanBtn) {
      if (!isAuthenticatedMember(state.user)) {
        showToast(t("portalSignInRequired"));
        return;
      }
      openPortalQrScannerModal();
      return;
    }

    const closeQrScanBtn = e.target.closest("#closePortalQrScannerBtn");
    if (closeQrScanBtn) {
      closePortalQrScannerModal();
      return;
    }

    if (e.target?.id === "portalQrScannerModal") {
      closePortalQrScannerModal();
      return;
    }

    const retryCamBtn = e.target.closest("#portalQrRetryCameraBtn");
    if (retryCamBtn) {
      openPortalQrScannerModal({ skipPermissionNotice: true });
      return;
    }

    const confirmQrCameraBtn = e.target.closest("[data-confirm-qr-camera]");
    if (confirmQrCameraBtn) {
      markQrCameraPermissionAcknowledged();
      startPortalQrLiveCamera();
      return;
    }

    const cancelQrCameraBtn = e.target.closest("[data-cancel-qr-camera]");
    if (cancelQrCameraBtn) {
      closePortalQrScannerModal();
      return;
    }

    const clearScannedBtn = e.target.closest("[data-clear-scanned-upi]");
    if (clearScannedBtn) {
      state.portalScannedUpiPayment = null;
      renderCustomerPortalView();
      return;
    }

    const jumpUtrBtn = e.target.closest("[data-jump-utr-payment-id]");
    if (jumpUtrBtn) {
      const payId = jumpUtrBtn.getAttribute("data-jump-utr-payment-id") || "";
      const card = document.querySelector(`[data-payment-id="${CSS?.escape ? CSS.escape(payId) : payId}"]`);
      const utrInput = card?.querySelector("input[name='utrNumber']");
      card?.scrollIntoView?.({ behavior: "smooth", block: "center" });
      utrInput?.focus?.();
      return;
    }

    const adminPayFilterBtn = e.target.closest("[data-admin-pay-filter]");
    if (adminPayFilterBtn && hasVerifiedAdminSession()) {
      state.adminPaymentFilter = adminPayFilterBtn.getAttribute("data-admin-pay-filter") || "all";
      renderAdminConsole();
      return;
    }

    const clearQrSelBtn = e.target.closest("#adminUpiQrClearSelectionBtn");
    if (clearQrSelBtn && hasVerifiedAdminSession()) {
      const fileEl = document.getElementById("adminUpiQrFile");
      if (fileEl) fileEl.value = "";
      const savedQr = String(state.paymentConfig?.qrImageDataUrl || "");
      const hasSaved = isUploadedQrImageDataUrl(savedQr);
      state.adminQrUploadState = {
        status: hasSaved ? "saved" : "idle",
        previewDataUrl: "",
        fileName: "",
        error: "",
        approxKb: hasSaved ? Math.max(1, Math.round((savedQr.length * 0.75) / 1024)) : 0
      };
      renderAdminConsole();
      return;
    }

    const printReceiptBtn = e.target.closest("[data-print-payment-receipt]");
    if (printReceiptBtn) {
      const pid = printReceiptBtn.getAttribute("data-print-payment-receipt") || "";
      const record =
        (state.adminBookingPayments || []).find((x) => x.id === pid) ||
        (state.myBookingPayments || []).find((x) => x.id === pid);
      if (!record) return;
      const receiptHtml = buildPrintablePaymentReceiptHtml(record, state.paymentConfig || DEFAULT_PAYMENT_CONFIG, { escapeHtml });
      const win = window.open("", "_blank", "noopener,noreferrer");
      if (win) {
        win.document.open();
        win.document.write(receiptHtml);
        win.document.close();
      } else {
        showToast("Allow pop-ups to print or download your payment receipt.");
      }
      return;
    }

    const reqRefundBtn = e.target.closest("[data-request-payment-refund]");
    if (reqRefundBtn && isAuthenticatedMember(state.user)) {
      const pid = reqRefundBtn.getAttribute("data-request-payment-refund") || "";
      const reason = window.prompt(
        state.lang === "hi" ? "रिफंड अनुरोध का कारण दर्ज करें:" : "Enter reason for refund request:"
      );
      if (!reason || !reason.trim()) return;
      try {
        await requestCustomerPaymentRefund({ paymentId: pid, refundReason: reason.trim() });
        showToast(state.lang === "hi" ? "रिफंड अनुरोध भेज दिया गया।" : "Refund request submitted.");
        await refreshCloudData();
      } catch (err) {
        showToast(err?.message || "Unable to submit refund request.");
      }
    }
  });

  document.addEventListener("change", async (e) => {
    const qrVerifiedToggle = e.target.closest("#adminUpiQrVerified");
    if (qrVerifiedToggle && hasVerifiedAdminSession()) {
      const hasSaved = isUploadedQrImageDataUrl(state.paymentConfig?.qrImageDataUrl || "");
      const qrEnabledBox = document.getElementById("adminUpiEnabled");
      const errBox = document.getElementById("adminUpiQrErrorBox");
      if (!hasSaved) {
        qrVerifiedToggle.checked = false;
        qrVerifiedToggle.disabled = true;
        if (qrEnabledBox) {
          qrEnabledBox.checked = false;
          qrEnabledBox.disabled = true;
        }
        const msg = t("adminPayQrNeedUploadFirst");
        if (errBox) {
          errBox.textContent = msg;
          errBox.classList.remove("hidden");
        }
        showToast(msg);
        return;
      }
      if (qrEnabledBox) {
        qrEnabledBox.disabled = !qrVerifiedToggle.checked;
        if (!qrVerifiedToggle.checked) {
          qrEnabledBox.checked = false;
        }
        qrEnabledBox.closest(".admin-pay-checkbox")?.classList.toggle("is-disabled", !qrVerifiedToggle.checked);
      }
      return;
    }

    const qrInput = e.target.closest("#adminUpiQrFile");
    if (!qrInput || !hasVerifiedAdminSession()) return;
    const pickedFile = qrInput.files?.[0];
    const imgWrap = document.getElementById("adminUpiQrImgWrap");
    const previewImg = document.getElementById("adminUpiQrPreviewImg");
    const emptyNote = document.getElementById("adminUpiQrEmptyNote");
    const savedNote = document.getElementById("adminUpiQrSavedNote");
    const statusBadge = document.getElementById("adminUpiQrPreviewStatus");
    const clearBtn = document.getElementById("adminUpiQrClearSelectionBtn");
    const errBox = document.getElementById("adminUpiQrErrorBox");
    const qrVerifiedBox = document.getElementById("adminUpiQrVerified");
    const qrEnabledBox = document.getElementById("adminUpiEnabled");
    if (!pickedFile) return;

    const showQrUploadError = (message) => {
      const cleanMsg = String(message || "Could not upload QR image. Please try another JPG, PNG, or WebP file.");
      qrInput.value = "";
      qrInput.disabled = false;
      state.adminQrUploadState = {
        status: "error",
        previewDataUrl: "",
        fileName: "",
        error: cleanMsg,
        approxKb: 0
      };
      if (errBox) {
        errBox.textContent = cleanMsg;
        errBox.classList.remove("hidden");
      }
      showToast(cleanMsg);
      renderAdminConsole();
      document.getElementById("adminUpiQrErrorBox")?.scrollIntoView?.({ behavior: "smooth", block: "nearest" });
    };

    try {
      if (errBox) {
        errBox.textContent = "";
        errBox.classList.add("hidden");
      }
      qrInput.disabled = true;
      if (statusBadge) {
        statusBadge.textContent = t("adminPayQrUploadingBadge");
        statusBadge.classList.remove("is-saved", "is-pending-save");
        statusBadge.classList.add("is-uploading");
      }

      // Verify original uploaded QR Blob resolves to exact mandated official UPI ID (9812890081@ybl) before compressing/saving
      const sourceQrCheck = await verifyQrCode(pickedFile);
      if (!sourceQrCheck.valid) {
        showQrUploadError(sourceQrCheck.error);
        return;
      }

      const compressedDataUrl = await compressPaymentImageFile(pickedFile);
      const compressedQrCheck = await verifyQrCode(compressedDataUrl);
      if (!compressedQrCheck.valid) {
        showQrUploadError(compressedQrCheck.error);
        return;
      }
      const approxKb = Math.max(1, Math.round((compressedDataUrl.length * 0.75) / 1024));

      // Immediately reflect uploaded preview in state & DOM so the "No QR uploaded" warning disappears immediately
      state.adminQrUploadState = {
        status: "uploading",
        previewDataUrl: compressedDataUrl,
        fileName: pickedFile.name || "",
        error: "",
        approxKb
      };
      if (previewImg) previewImg.src = compressedDataUrl;
      imgWrap?.classList.remove("hidden");
      emptyNote?.classList.add("hidden");
      savedNote?.classList.add("hidden");
      clearBtn?.classList.add("hidden");
      if (qrVerifiedBox) {
        qrVerifiedBox.checked = false;
        qrVerifiedBox.disabled = true;
      }
      if (qrEnabledBox) {
        qrEnabledBox.checked = false;
        qrEnabledBox.disabled = true;
      }

      // Auto-save compressed QR image to Firestore (paymentConfig/paymentSettings) while keeping official UPI ID & existing settings intact
      const currentCfg = state.paymentConfig || DEFAULT_PAYMENT_CONFIG;
      const preservedUpiId = OFFICIAL_STUDIO_UPI_ID;
      const savedConfig = await adminSavePaymentSettings({
        enabled: false,
        upiId: preservedUpiId,
        payeeName:
          String(document.getElementById("adminUpiPayeeName")?.value || currentCfg.payeeName || "STAR ARTIST STUDIO").trim() ||
          "STAR ARTIST STUDIO",
        qrImageDataUrl: compressedDataUrl,
        qrVerified: false,
        instructions: String(document.getElementById("adminUpiInstructions")?.value ?? currentCfg.instructions ?? ""),
        modelAdvanceMode:
          document.getElementById("adminModelAdvMode")?.value || currentCfg.modelAdvanceMode || "percentage",
        modelAdvanceValue: Number(
          document.getElementById("adminModelAdvValue")?.value ?? currentCfg.modelAdvanceValue ?? 0
        ),
        weddingChargeMode:
          document.getElementById("adminWedChargeMode")?.value || currentCfg.weddingChargeMode || "percentage",
        weddingChargeValue: Number(
          document.getElementById("adminWedChargeValue")?.value ?? currentCfg.weddingChargeValue ?? 0
        )
      });

      state.paymentConfig = { ...DEFAULT_PAYMENT_CONFIG, ...savedConfig };
      state.adminQrUploadState = {
        status: "saved",
        previewDataUrl: "",
        fileName: pickedFile.name || "",
        error: "",
        approxKb
      };
      qrInput.disabled = false;
      qrInput.value = "";
      renderAdminConsole();
      showToast(t("adminPayQrUploadSuccessToast"));
    } catch (err) {
      showQrUploadError(err?.message || "Could not upload selected QR image.");
    }
  });

  // Fallback QR Image Picker inside My Portal 'Scan QR to Pay' Modal
  document.addEventListener("change", async (e) => {
    const fallbackInput = e.target.closest("#portalQrFallbackFile");
    if (!fallbackInput) return;
    const pickedFile = fallbackInput.files?.[0];
    fallbackInput.value = "";
    if (!pickedFile) return;

    const statusEl = document.getElementById("portalQrScannerStatus");
    const errEl = document.getElementById("portalQrScannerError");
    if (errEl) {
      errEl.textContent = "";
      errEl.classList.add("hidden");
    }
    if (statusEl) statusEl.textContent = t("portalScanQrDecodingImage");

    const parsed = await decodeQrImageFileForPayment(pickedFile, {
      expectedUpiId: OFFICIAL_STUDIO_UPI_ID,
      requireOfficialUpi: true
    });
    if (!parsed.valid) {
      if (errEl) {
        errEl.textContent = parsed.error;
        errEl.classList.remove("hidden");
      }
      if (statusEl) statusEl.textContent = t("portalScanQrTryAgainHint");
      showToast(parsed.error);
      return;
    }
    applyScannedUpiPaymentDetails(parsed);
  });

  // Live sync when customer edits pre-filled Amount or Note inside Scanned UPI Card
  document.addEventListener("input", (e) => {
    const amountEl = e.target.closest("#portalScannedPayAmount");
    const noteEl = e.target.closest("#portalScannedPayNote");
    if (!amountEl && !noteEl) return;
    if (!state.portalScannedUpiPayment) return;

    const nextAmtInput = document.getElementById("portalScannedPayAmount");
    const nextNoteInput = document.getElementById("portalScannedPayNote");
    const parsedAmt = Math.max(0, Number(nextAmtInput?.value || 0) || 0);
    const nextNote = String(nextNoteInput?.value || "").trim().slice(0, 120);

    state.portalScannedUpiPayment = {
      ...state.portalScannedUpiPayment,
      amount: parsedAmt,
      note: nextNote,
      upiDeepLink: buildSafeUpiPayLink({
        upiId: state.portalScannedUpiPayment.upiId,
        payeeName: state.portalScannedUpiPayment.payeeName,
        amount: parsedAmt,
        note: nextNote,
        transactionRef: state.portalScannedUpiPayment.transactionRef
      })
    };

    const payLinkEl = document.getElementById("portalScannedUpiPayLink");
    if (payLinkEl) {
      payLinkEl.setAttribute("href", state.portalScannedUpiPayment.upiDeepLink || "#");
      const amtSuffix = parsedAmt > 0 ? ` (${formatInrAmount(parsedAmt)})` : "";
      payLinkEl.textContent = `⚡ ${t("portalScanQrOpenUpiAppBtn")}${amtSuffix}`;
    }
  });

  document.getElementById("bookingDate")?.addEventListener("change", (e) => {
    const val = e.target.value;
    if (/^\d{4}-\d{2}-\d{2}$/.test(val)) {
      const [y, m] = val.split("-").map((x) => parseInt(x, 10));
      state.bookingCalYear = y;
      state.bookingCalMonth = m - 1;
    }
    renderBookingSlotCalendar();
  });

  document.getElementById("bookingTime")?.addEventListener("change", () => {
    renderBookingSlotCalendar();
  });
}

function compressPaymentImageFile(file) {
  return new Promise((resolve, reject) => {
    const metaCheck = isAllowedPaymentImageFileMeta(file);
    if (!metaCheck.valid) {
      reject(new Error(metaCheck.error));
      return;
    }
    const reader = new FileReader();
    reader.onerror = () => reject(new Error("Unable to read image file. Please try selecting the QR image again."));
    reader.onload = () => {
      const img = new Image();
      img.onerror = () => reject(new Error("Invalid or corrupted image file. Please select a valid JPG, PNG, or WebP QR image."));
      img.onload = () => {
        const srcW = Math.max(1, img.naturalWidth || img.width || 1);
        const srcH = Math.max(1, img.naturalHeight || img.height || 1);
        const attempts = [
          { maxDim: 680, quality: 0.82 },
          { maxDim: 680, quality: 0.72 },
          { maxDim: 600, quality: 0.62 },
          { maxDim: 520, quality: 0.56 },
          { maxDim: 440, quality: 0.52 },
          { maxDim: 360, quality: 0.48 }
        ];
        let bestDataUrl = "";
        for (const step of attempts) {
          const scale = Math.min(1, step.maxDim / Math.max(srcW, srcH));
          const canvas = document.createElement("canvas");
          canvas.width = Math.max(1, Math.round(srcW * scale));
          canvas.height = Math.max(1, Math.round(srcH * scale));
          const ctx2d = canvas.getContext("2d");
          if (!ctx2d) {
            reject(new Error("Browser canvas is unavailable to process this QR image."));
            return;
          }
          ctx2d.fillStyle = "#FFFFFF";
          ctx2d.fillRect(0, 0, canvas.width, canvas.height);
          ctx2d.drawImage(img, 0, 0, canvas.width, canvas.height);
          const candidate = canvas.toDataURL("image/jpeg", step.quality);
          bestDataUrl = candidate;
          if (isUploadedQrImageDataUrl(candidate) && candidate.length <= 172000) {
            resolve(candidate);
            return;
          }
        }
        if (isUploadedQrImageDataUrl(bestDataUrl)) {
          resolve(bestDataUrl);
          return;
        }
        reject(new Error("QR image is too large even after compression (max 175 KB). Please crop closer to the QR square and retry."));
      };
      img.src = String(reader.result || "");
    };
    reader.readAsDataURL(file);
  });
}

function renderBookingSlotCalendar() {
  const host = document.getElementById("bookingSlotCalendar");
  if (!host) return;
  if (!isAuthenticatedMember(state.user)) {
    host.innerHTML = "";
    return;
  }
  const dateEl = document.getElementById("bookingDate");
  const timeEl = document.getElementById("bookingTime");
  const selectedDate = dateEl?.value || "";
  const selectedTime = timeEl?.value || "11:30 AM";
  const slotIndex = buildBookedSlotIndex(state.studioBookedSlots, state.myBookings);

  // Sync <select id="bookingTime"> option availability so booked slots are clearly disabled
  if (timeEl && selectedDate) {
    Array.from(timeEl.options).forEach((opt) => {
      const booked = isStudioSlotBooked(slotIndex, selectedDate, opt.value);
      opt.disabled = booked;
    });
    if (isStudioSlotBooked(slotIndex, selectedDate, timeEl.value)) {
      const firstOpen = getFirstAvailableTimeForDate(slotIndex, selectedDate);
      if (firstOpen) timeEl.value = firstOpen;
    }
  }

  host.innerHTML = renderBookingCalendarHtml({
    viewYear: state.bookingCalYear,
    viewMonth: state.bookingCalMonth,
    selectedDate,
    selectedTime: timeEl?.value || selectedTime,
    slotIndex,
    minDateStr: dateEl?.min || "",
    lang: state.lang,
    t,
    escapeHtml
  });
}

let portalQrScannerController = null;

function closePortalQrScannerModal() {
  if (portalQrScannerController) {
    portalQrScannerController.stop();
    portalQrScannerController = null;
  }
  const modal = document.getElementById("portalQrScannerModal");
  if (modal) {
    modal.classList.add("hidden");
  }
}

function applyScannedUpiPaymentDetails(parsedQr) {
  const prefilled = buildPrefilledUpiPaymentDetails(parsedQr, {
    customerPayments: state.myBookingPayments || [],
    fallbackConfig: state.paymentConfig || DEFAULT_PAYMENT_CONFIG
  });
  if (!prefilled) return;

  closePortalQrScannerModal();
  state.portalScannedUpiPayment = prefilled;
  state.portalTab = "my-payments";
  renderCustomerPortalView();

  const toastMsg =
    prefilled.amount > 0
      ? t("portalScanQrSuccessAmountToast").replace("{amount}", prefilled.amount.toLocaleString("en-IN"))
      : t("portalScanQrSuccessToast");
  showToast(toastMsg);

  setTimeout(() => {
    const card = document.getElementById("portalScannedUpiCard");
    card?.scrollIntoView?.({ behavior: "smooth", block: "start" });
  }, 60);
}

function startPortalQrLiveCamera() {
  const permissionEl = document.getElementById("portalQrPermissionNotice");
  const livePaneEl = document.getElementById("portalQrLiveScannerPane");
  const videoEl = document.getElementById("portalQrScannerVideo");
  const canvasEl = document.getElementById("portalQrScannerCanvas");
  const statusEl = document.getElementById("portalQrScannerStatus");
  const errEl = document.getElementById("portalQrScannerError");

  if (permissionEl) {
    permissionEl.classList.add("hidden");
    permissionEl.innerHTML = "";
  }
  if (livePaneEl) {
    livePaneEl.classList.remove("hidden");
  }
  if (errEl) {
    errEl.textContent = "";
    errEl.classList.add("hidden");
  }
  if (statusEl) {
    statusEl.textContent = t("portalScanQrStartingCamera");
  }

  if (portalQrScannerController) {
    portalQrScannerController.stop();
    portalQrScannerController = null;
  }

  portalQrScannerController = createCameraQrScannerController({
    videoEl,
    canvasEl,
    onStatus: (msg) => {
      if (statusEl) statusEl.textContent = String(msg || "");
    },
    onError: (msg) => {
      const cleanMsg = String(msg || "Unable to access camera.");
      if (errEl) {
        errEl.textContent = cleanMsg;
        errEl.classList.remove("hidden");
      }
      if (statusEl) statusEl.textContent = t("portalScanQrTryAgainHint");
    },
    onDetected: (rawPayload) => {
      const parsed = parseUpiPaymentQrDetails(rawPayload, {
        expectedUpiId: OFFICIAL_STUDIO_UPI_ID,
        requireOfficialUpi: true
      });
      if (!parsed.valid) {
        if (errEl) {
          errEl.textContent = parsed.error;
          errEl.classList.remove("hidden");
        }
        if (statusEl) statusEl.textContent = t("portalScanQrTryAgainHint");
        showToast(parsed.error);
        return;
      }
      applyScannedUpiPaymentDetails(parsed);
    }
  });

  portalQrScannerController.start();
}

function openPortalQrScannerModal({ skipPermissionNotice = false } = {}) {
  const modal = document.getElementById("portalQrScannerModal");
  const permissionEl = document.getElementById("portalQrPermissionNotice");
  const livePaneEl = document.getElementById("portalQrLiveScannerPane");
  if (!modal) return;

  if (portalQrScannerController) {
    portalQrScannerController.stop();
    portalQrScannerController = null;
  }

  const errorEl = document.getElementById("portalQrScannerError");
  if (errorEl) {
    errorEl.textContent = "";
    errorEl.classList.add("hidden");
  }

  modal.classList.remove("hidden");

  if (!skipPermissionNotice && !hasAcknowledgedQrCameraPermission() && permissionEl) {
    permissionEl.innerHTML = renderCameraPermissionNoticeHtml({ t, escapeHtml });
    permissionEl.classList.remove("hidden");
    livePaneEl?.classList.add("hidden");
    document.getElementById("portalQrConfirmCameraBtn")?.focus?.();
    return;
  }

  startPortalQrLiveCamera();
}

function renderCustomerPortalView({ checkUrlDeepLink = false } = {}) {
  const host = document.getElementById("customerPortalBody");
  if (!host) return;
  if (!isAuthenticatedMember(state.user)) {
    host.innerHTML = "";
    return;
  }

  const uid = state.user.uid;
  if (checkUrlDeepLink) {
    const qp = extractPortalQueryParams({
      hash: window.location.hash,
      search: window.location.search
    });
    if (qp.tab) {
      state.portalTab = qp.tab;
      if (qp.tab === "payment-history") {
        queueMicrotask(() => loadCustomerPaymentHistory());
      }
    }
    state.portalDeniedNotice = "";
    state.portalHighlightedBookingId = "";

    if (qp.bookingId) {
      state.portalTab = "my-bookings";
      const guard = resolvePortalDeepLinkAccess({
        requestedId: qp.bookingId,
        ownedRecords: [...state.myBookings, ...state.myHiringRequests, ...state.myCompanyBookings],
        currentUid: uid
      });
      if (guard.denied) {
        state.portalDeniedNotice = t("portalAccessDeniedDesc");
      } else {
        state.portalHighlightedBookingId = qp.bookingId;
      }
    } else if (qp.catalogId) {
      state.portalTab = "my-catalog";
      const guard = resolvePortalDeepLinkAccess({
        requestedId: qp.catalogId,
        ownedRecords: state.myCustomerCatalogs,
        currentUid: uid
      });
      if (guard.denied) {
        state.portalDeniedNotice = t("portalAccessDeniedDesc");
      }
    } else if (qp.slotId) {
      state.portalTab = "my-slot";
      const guard = resolvePortalDeepLinkAccess({
        requestedId: qp.slotId,
        ownedRecords: state.myCustomerSlots,
        currentUid: uid
      });
      if (guard.denied) {
        state.portalDeniedNotice = t("portalAccessDeniedDesc");
      }
    }
  }

  host.innerHTML = renderCustomerPortalHtml({
    user: state.user,
    userProfile: state.myUserProfile,
    catalogs: state.myCustomerCatalogs,
    slots: state.myCustomerSlots,
    bookings: state.myBookings,
    hiringRequests: state.myHiringRequests,
    models: state.myModels,
    companyBookings: state.myCompanyBookings,
    chatThreads: state.myChatThreads,
    activeChatId: state.activeUserChatId,
    chatMessages: state.activeUserChatMessages,
    isChatBlocked: Boolean(state.myChatBlockStatus?.isBlocked),
    chatBlockReason: state.myChatBlockStatus?.reason || "",
    chatDraftText: state.userChatDraftText,
    chatDraftImage: state.userChatDraftImage,
    isChatSending: state.userChatSending,
    payments: state.myBookingPayments,
    paymentConfig: state.paymentConfig || DEFAULT_PAYMENT_CONFIG,
    publicModels: state.publicModels || [],
    activePaymentForm: state.activePaymentForm || "model",
    scannedUpiPayment: state.portalScannedUpiPayment,
    paymentHistoryFilter: state.portalPaymentHistoryFilter || "all",
    isPaymentHistoryLoading: Boolean(state.portalPaymentHistoryLoading),
    paymentHistoryError: state.portalPaymentHistoryError || "",
    activeTab: state.portalTab,
    deniedResourceNotice: state.portalDeniedNotice,
    highlightedBookingId: state.portalHighlightedBookingId,
    lang: state.lang,
    t,
    escapeHtml
  });

  if (state.portalTab === "my-messages") {
    const timeline = document.getElementById("userPrivateChatTimeline");
    if (timeline) timeline.scrollTop = timeline.scrollHeight;
    syncVisibleChatReadReceipts({ asAdmin: false });
  }
}

async function loadCustomerPaymentHistory({ showFeedback = false } = {}) {
  if (!isAuthenticatedMember(state.user)) return;
  if (state.portalPaymentHistoryLoading) return;
  state.portalPaymentHistoryLoading = true;
  state.portalPaymentHistoryError = "";
  if (state.route === "portal" && state.portalTab === "payment-history") {
    renderCustomerPortalView();
  }
  try {
    const freshPayments = await fetchMyUpiPaymentHistory(state.user.uid);
    state.myBookingPayments = Array.isArray(freshPayments) ? freshPayments : [];
    state.portalPaymentHistoryError = "";
    if (showFeedback) {
      showToast(t("portalPayHistoryRefreshedToast"));
    }
  } catch (err) {
    state.portalPaymentHistoryError = err?.message || t("portalPayHistoryLoadError");
    if (showFeedback) {
      showToast(state.portalPaymentHistoryError);
    }
  } finally {
    state.portalPaymentHistoryLoading = false;
    if (state.route === "portal") {
      renderCustomerPortalView();
    }
  }
}

let authSubscriptionAttached = false;

async function connectFirebaseCloud() {
  const clientStatus = await initFirebaseClient();
  state.firebaseReady = Boolean(clientStatus.ready);
  updateFirebaseUiStatus();

  if (state.firebaseReady && !authSubscriptionAttached) {
    authSubscriptionAttached = true;
    await subscribeAuthState(async ({ user, isAdmin, firebaseReady, authError }) => {
      const authenticated = isAuthenticatedMember(user);
      state.user = authenticated ? user : null;
      state.isAdmin = Boolean(authenticated && isAdmin && isAuthorizedAdminUser(user));
      state.authResolved = true;
      state.firebaseReady = firebaseReady;

      if (!state.isAdmin) {
        stopAdminRealtimeTracker();
        adminBookingTracker.resetSessionLog();
        state.adminSelectedModelIds = [];
        state.adminBulkModelNote = "";
        state.adminData = { bookings: [], models: [], gallery: [], announcements: [], companies: [], companyBookings: [] };
      }

      if (!authenticated) {
        state.portalScannedUpiPayment = null;
        closePortalQrScannerModal();
        clearProtectedStudioViews();
        closeModelProfileModal();
        const openLightbox = document.getElementById("lightboxModal");
        if (openLightbox && !openLightbox.classList.contains("hidden")) {
          document.getElementById("closeLightboxBtn")?.click();
        }
      }

      updateFirebaseUiStatus();

      if (authError?.code === "auth/unregistered-account") {
        const msg = mapFirebaseAuthError(authError, { mode: "login", t });
        setAuthGateAlert(msg);
        showToast(msg);
        navigateTo("signup", { silentBlock: true });
        return;
      }

      if (!authenticated) {
        const targetAuthMode =
          state.authMode === "signup" ? "signup" : state.authMode === "reset" ? "reset" : "login";
        navigateTo(targetAuthMode, { silentBlock: true });
        if (state.pendingAdminRoute) {
          const adminGateHint = document.getElementById("authAdminGateHint");
          if (adminGateHint) adminGateHint.classList.remove("hidden");
        }
        return;
      }

      setAuthGateAlert("");
      closeAuthModal();
      renderProtectedStudioViews();

      if (state.pendingAdminRoute) {
        state.pendingAdminRoute = false;
        if (state.isAdmin) {
          navigateTo("admin", { silentBlock: true });
        } else {
          navigateTo("home", { silentBlock: true });
          showToast(t("adminAccessDeniedToast"));
        }
      } else {
        const destination =
          state.route === "login" || state.route === "signup" || state.route === "reset"
            ? state.postAuthRoute || "home"
            : state.route === "admin" && !state.isAdmin
            ? "home"
            : state.route;
        navigateTo(destination, { silentBlock: true });
      }

      await syncQueuedBookings();
      await refreshCloudData();
      tryOpenPendingSharedModelProfile();
    });
  } else if (!state.firebaseReady) {
    state.authResolved = true;
    syncAuthGateDom(document, {
      isAuthenticated: isAuthenticatedMember(state.user),
      authMode: state.authMode,
      authResolved: true
    });
    if (state.pendingAdminRoute) {
      state.pendingAdminRoute = false;
      showToast(t("firebaseReconnectingToast"));
    }
  }

  return state.firebaseReady;
}

async function bootstrap() {
  applyTheme();
  applyTranslations();
  clearProtectedStudioViews();
  syncAdminUiVisibility(document, { isAdmin: false });
  setupPwaInstall();
  wireEvents();

  const initialAuthRoute = extractAuthRouteFromUrl({
    pathname: window.location.pathname,
    hash: window.location.hash,
    search: window.location.search
  });
  const initialSharedModelId = extractSharedModelIdFromUrl({
    hash: window.location.hash,
    search: window.location.search
  });
  if (initialSharedModelId) {
    state.pendingSharedModelId = initialSharedModelId;
    state.postAuthRoute = "companies";
  }

  const initialRoute = initialAuthRoute
    ? initialAuthRoute
    : initialSharedModelId
    ? "companies"
    : extractRequestedRoute({
        pathname: window.location.pathname,
        hash: window.location.hash,
        search: window.location.search
      });
  navigateTo(initialRoute, { silentBlock: true });

  await connectFirebaseCloud();

  window.addEventListener("online", () => {
    if (!state.firebaseReady) {
      connectFirebaseCloud();
    }
  });
}

bootstrap();
