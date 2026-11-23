import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

const rootDir = path.resolve(import.meta.dirname, "../..");
const webDir = path.join(rootDir, "web");

test("PWA manifest.webmanifest is valid and all referenced PNG icons exist", () => {
  const manifestPath = path.join(webDir, "manifest.webmanifest");
  assert.ok(fs.existsSync(manifestPath), "manifest.webmanifest must exist");

  const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf8"));
  assert.equal(manifest.name, "STAR ARTIST STUDIO");
  assert.equal(manifest.display, "standalone");
  assert.equal(manifest.background_color, "#0C0C0C");
  assert.equal(manifest.theme_color, "#0C0C0C");

  const sizes = manifest.icons.map((i) => i.sizes);
  assert.ok(sizes.includes("192x192"), "Must include 192x192 icon");
  assert.ok(sizes.includes("512x512"), "Must include 512x512 icon");
  assert.ok(
    manifest.icons.some((i) => String(i.purpose || "").includes("maskable")),
    "Must include maskable icon for Android adaptive Add to Home Screen"
  );

  for (const icon of manifest.icons) {
    const filePath = path.join(webDir, icon.src.replace(/^\//, ""));
    assert.ok(fs.existsSync(filePath), `Icon asset missing: ${icon.src}`);
    assert.ok(fs.statSync(filePath).size > 1000, `Icon asset empty: ${icon.src}`);
  }
});

test("Service Worker sw.js pre-caches only existing local assets and bypasses Firebase APIs", () => {
  const swText = fs.readFileSync(path.join(webDir, "sw.js"), "utf8");
  assert.ok(swText.includes("firestore.googleapis.com"), "Must bypass Firestore traffic");
  assert.ok(swText.includes("identitytoolkit.googleapis.com"), "Must bypass Firebase Auth traffic");
  assert.ok(swText.includes("/__/"), "Must bypass Firebase Hosting auto-init endpoint");

  const urlMatch = swText.match(/const APP_SHELL_URLS = \[([\s\S]*?)\];/);
  assert.ok(urlMatch, "APP_SHELL_URLS must be defined");
  const urls = [...urlMatch[1].matchAll(/"([^"]+)"/g)].map((m) => m[1]);
  assert.ok(urls.length >= 10, "App shell must cache HTML, JS, CSS, icons, and studio images");

  for (const url of urls) {
    if (url === "/") continue;
    const targetPath = path.join(webDir, url.replace(/^\//, ""));
    assert.ok(fs.existsSync(targetPath), `SW cached asset missing on disk: ${url}`);
  }
});

test("web/js/data.js preserves official Studio details, named Firestore DB, and catalog", async () => {
  const dataMod = await import(pathToFileURL(path.join(webDir, "js/data.js")).href);
  assert.equal(dataMod.STUDIO_CONTACT.studioName, "STAR ARTIST STUDIO");
  assert.equal(dataMod.STUDIO_CONTACT.mobileNumber, "9812890081");
  assert.equal(dataMod.STUDIO_CONTACT.adminEmail, "chanderstudio0@gmail.com");
  assert.equal(
    dataMod.FIREBASE_PROJECT_DEFAULTS.databaseId,
    "ai-studio-android-stararti-4ffde0d6-02fd-4194-8e31-3359a6b9588b"
  );
  assert.equal(dataMod.STUDIO_SERVICES.length, 8);
});

test("web/js/i18n.js has matching English and Hindi keys", async () => {
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);
  const enKeys = Object.keys(i18nMod.TRANSLATIONS.en).sort();
  const hiKeys = Object.keys(i18nMod.TRANSLATIONS.hi).sort();
  assert.deepEqual(enKeys, hiKeys, "EN and HI translation keys must match");
});

test("firebase.json preserves named Firestore database and configures free Hosting", () => {
  const cfg = JSON.parse(fs.readFileSync(path.join(rootDir, "firebase.json"), "utf8"));
  assert.equal(
    cfg.firestore[0].database,
    "ai-studio-android-stararti-4ffde0d6-02fd-4194-8e31-3359a6b9588b"
  );
  assert.equal(cfg.hosting.public, "web");
});

test(".firebaserc maps default project to gen-lang-client-0548683163 and web/index.html exists", () => {
  const rcPath = path.join(rootDir, ".firebaserc");
  assert.ok(fs.existsSync(rcPath), ".firebaserc must exist");
  const rc = JSON.parse(fs.readFileSync(rcPath, "utf8"));
  assert.equal(rc.projects.default, "gen-lang-client-0548683163");

  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.ok(html.includes('href="/manifest.webmanifest"'), "index.html must link manifest");
  assert.ok(html.includes('href="/styles.css"'), "index.html must link stylesheet");
  assert.ok(html.includes('src="/js/app.js"'), "index.html must load main app module");
  assert.ok(html.includes("mailto:chanderstudio0@gmail.com"), "index.html must link official studio email");
});

test("No hardcoded API keys inside web source files", () => {
  const filesToScan = [
    path.join(webDir, "index.html"),
    path.join(webDir, "sw.js"),
    path.join(webDir, "js/admin-models.js"),
    path.join(webDir, "js/app.js"),
    path.join(webDir, "js/booking-status.js"),
    path.join(webDir, "js/catalog-reviews.js"),
    path.join(webDir, "js/data.js"),
    path.join(webDir, "js/exif-reader.js"),
    path.join(webDir, "js/firebase.js"),
    path.join(webDir, "js/gallery-share.js"),
    path.join(webDir, "js/i18n.js"),
    path.join(webDir, "js/image-upload.js"),
    path.join(webDir, "js/lazy-images.js"),
    path.join(webDir, "js/newsletter.js"),
    path.join(webDir, "js/offline-sync.js"),
    path.join(webDir, "js/testimonials.js"),
    path.join(webDir, "js/tier-info.js")
  ];
  for (const file of filesToScan) {
    const content = fs.readFileSync(file, "utf8");
    assert.ok(!/AIza[0-9A-Za-z_-]{20,}/.test(content), `Found hardcoded API key in ${file}`);
  }
});

test("Booking status badges support Confirmed, Pending, Completed, and Cancelled with accessible markup & CSS", async () => {
  const statusMod = await import(pathToFileURL(path.join(webDir, "js/booking-status.js")).href);
  const sample = [
    { id: "1", status: "Confirmed" },
    { id: "2", status: "Pending" },
    { id: "3", status: "Completed" },
    { id: "4", status: "Cancelled" },
    { id: "5", status: "" }
  ];

  assert.equal(statusMod.normalizeBookingStatus("confirmed"), "Confirmed");
  assert.equal(statusMod.normalizeBookingStatus("PENDING"), "Pending");
  assert.equal(statusMod.normalizeBookingStatus("completed"), "Completed");
  assert.equal(statusMod.normalizeBookingStatus("canceled"), "Cancelled");
  assert.equal(statusMod.normalizeBookingStatus(""), "Pending");

  const summary = statusMod.summarizeBookingStatuses(sample);
  assert.deepEqual(summary, {
    total: 5,
    confirmed: 1,
    pending: 2,
    completed: 1,
    cancelled: 1
  });

  assert.equal(statusMod.filterBookingsByStatus(sample, "completed").length, 1);
  assert.equal(statusMod.filterBookingsByStatus(sample, "all").length, 5);

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.ok(html.includes('id="myBookingsStatusBar"'), "Booking view must include status filter bar");

  for (const status of ["Confirmed", "Pending", "Completed", "Cancelled"]) {
    const enMeta = statusMod.getBookingStatusMeta(status, "en");
    const hiMeta = statusMod.getBookingStatusMeta(status, "hi");
    const badgeHtml = statusMod.renderBookingStatusBadgeHtml(status, "en");
    assert.equal(enMeta.label, status);
    assert.ok(hiMeta.label.includes(status));
    assert.ok(badgeHtml.includes(`class="badge status-badge ${enMeta.cssClass}"`));
    assert.ok(badgeHtml.includes(`data-booking-status="${enMeta.key}"`));
    assert.ok(badgeHtml.includes("<svg"));
    assert.ok(css.includes(`.badge.${enMeta.cssClass}`), `CSS missing rule for .badge.${enMeta.cssClass}`);
    assert.ok(css.includes(`.booking-item-card.${enMeta.cardClass}`), `CSS missing card accent for ${enMeta.cardClass}`);
  }
});

test("Service catalog star rating & comment module validates, filters, summarizes, and renders accessible HTML + CSS", async () => {
  const reviewsMod = await import(pathToFileURL(path.join(webDir, "js/catalog-reviews.js")).href);
  const dataMod = await import(pathToFileURL(path.join(webDir, "js/data.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);

  const fashionPkg = dataMod.STUDIO_SERVICES.find((p) => p.id === "pkg_photo_fashion");
  assert.ok(fashionPkg, "pkg_photo_fashion must exist in catalog data");
  assert.equal(reviewsMod.catalogReviewBookingId(fashionPkg.id), "catalog_pkg_photo_fashion");

  // Validation rules match Firestore reviews constraints (1..5 rating, 5..1000 chars comment)
  assert.ok(reviewsMod.validateCatalogReview(0, "Great studio"), "Rating 0 must fail");
  assert.ok(reviewsMod.validateCatalogReview(5, "hi"), "Comment < 5 chars must fail");
  assert.equal(reviewsMod.validateCatalogReview(4, "Amazing editorial lighting!"), null);

  const sampleReviews = [
    {
      id: "u1_catalog_pkg_photo_fashion",
      userId: "u1",
      bookingId: "catalog_pkg_photo_fashion",
      reviewerName: "Aarav",
      serviceType: fashionPkg.recommendedServiceType,
      rating: 5,
      comment: "Crisp lighting and editorial direction!"
    },
    {
      id: "bk_99",
      userId: "u2",
      bookingId: "bk_99",
      reviewerName: "Meera",
      serviceType: `${fashionPkg.recommendedServiceType} - Pro`,
      rating: 4,
      comment: "Loved the backdrop and fast turnaround."
    },
    {
      id: "u3_catalog_pkg_video_music",
      userId: "u3",
      bookingId: "catalog_pkg_video_music",
      reviewerName: "Kabir",
      serviceType: "Music Video Production",
      rating: 5,
      comment: "Different package comment"
    }
  ];

  const matched = reviewsMod.filterReviewsForPackage(sampleReviews, fashionPkg);
  assert.equal(matched.length, 2);

  const summary = reviewsMod.summarizePackageReviews(matched);
  assert.equal(summary.count, 2);
  assert.equal(summary.formattedAverage, "4.5");

  const html = reviewsMod.renderCatalogFeedbackHtml({
    pkg: fashionPkg,
    reviews: sampleReviews,
    existingReview: matched[0],
    isExpanded: true,
    draftRating: 4,
    draftComment: "Superb studio session",
    lang: "en",
    t: (k) => i18nMod.TRANSLATIONS.en[k] || k,
    interactive: true
  });

  assert.ok(html.includes('data-toggle-catalog-reviews="pkg_photo_fashion"'));
  assert.ok(html.includes('data-rate-pkg="pkg_photo_fashion"'));
  assert.ok(html.includes('data-rate-value="4"'));
  assert.ok(html.includes('data-review-comment="pkg_photo_fashion"'));
  assert.ok(html.includes('data-submit-catalog-review="pkg_photo_fashion"'));
  assert.ok(html.includes("4.5 (2)"));
  assert.ok(html.includes("Crisp lighting and editorial direction!"));
  assert.ok(html.includes("<svg"));

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".catalog-feedback-box",
    ".catalog-rating-pill",
    ".star-picker-btn",
    ".catalog-review-item",
    ".read-only-stars"
  ]) {
    assert.ok(css.includes(selector), `CSS missing rule for ${selector}`);
  }
});

test("Admin model verification & Official Studio Roster module enforces manual check, rejection notes, and filtering", async () => {
  const adminModelsMod = await import(pathToFileURL(path.join(webDir, "js/admin-models.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);

  const sampleModels = [
    {
      id: "m_pending",
      fullName: "Anya Kapoor",
      age: 22,
      gender: "Female",
      city: "Sirsa",
      phoneNumber: "9812890081",
      skills: "Runway, Editorial",
      portfolioUrls: "https://instagram.com/anya_model",
      consentGiven: true,
      status: "pending",
      adminNotes: ""
    },
    {
      id: "m_roster",
      fullName: "Kabir Gill",
      age: 25,
      gender: "Male",
      city: "Hisar",
      phoneNumber: "9541024098",
      skills: "Commercial",
      consentGiven: true,
      status: "approved",
      adminNotes: "Official Roster"
    },
    {
      id: "m_rejected",
      fullName: "Rohan Das",
      age: 19,
      gender: "Male",
      city: "Delhi",
      phoneNumber: "9988776655",
      skills: "Print",
      consentGiven: true,
      status: "rejected",
      adminNotes: "Incomplete portfolio"
    }
  ];

  const counts = adminModelsMod.summarizeModelApplications(sampleModels);
  assert.deepEqual(counts, { total: 3, pending: 1, roster: 1, rejected: 1 });

  assert.equal(adminModelsMod.filterModelApplications(sampleModels, "pending").length, 1);
  assert.equal(adminModelsMod.filterModelApplications(sampleModels, "approved")[0].id, "m_roster");
  assert.equal(adminModelsMod.filterModelApplications(sampleModels, "rejected")[0].id, "m_rejected");
  assert.equal(adminModelsMod.filterModelApplications(sampleModels, "all", "anya").length, 1);

  // Verification gate rules
  assert.ok(
    adminModelsMod.validateManualModelVerification({
      status: "approved",
      age: 22,
      consentGiven: true,
      checklistConfirmed: false,
      adminNotes: ""
    }),
    "Must block roster verification when manual checklist is unchecked"
  );
  assert.ok(
    adminModelsMod.validateManualModelVerification({
      status: "approved",
      age: 17,
      consentGiven: true,
      checklistConfirmed: true,
      adminNotes: ""
    }),
    "Must block under-18 applicant from roster"
  );
  assert.equal(
    adminModelsMod.validateManualModelVerification({
      status: "approved",
      age: 22,
      consentGiven: true,
      checklistConfirmed: true,
      adminNotes: "Verified for roster"
    }),
    null
  );
  assert.ok(
    adminModelsMod.validateManualModelVerification({
      status: "rejected",
      age: 22,
      consentGiven: true,
      checklistConfirmed: false,
      adminNotes: ""
    }),
    "Must require brief reason before rejecting"
  );
  assert.equal(
    adminModelsMod.validateManualModelVerification({
      status: "rejected",
      age: 22,
      consentGiven: true,
      checklistConfirmed: false,
      adminNotes: "Incomplete portfolio link"
    }),
    null
  );

  const html = adminModelsMod.renderAdminModelsSectionHtml({
    models: sampleModels,
    filter: "pending",
    searchQuery: "",
    drafts: {},
    lang: "en",
    t: (k) => i18nMod.TRANSLATIONS.en[k] || k
  });

  assert.ok(html.includes('data-adm-model-filter="pending"'));
  assert.ok(html.includes('data-adm-model-filter="approved"'));
  assert.ok(html.includes('data-adm-model-check="m_pending"'));
  assert.ok(html.includes('data-adm-model-notes="m_pending"'));
  assert.ok(html.includes('data-adm-mdl-st="approved"'));
  assert.ok(html.includes('data-adm-mdl-st="rejected"'));
  assert.ok(html.includes("1 Official Roster"));

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".admin-verification-banner",
    ".admin-model-card",
    ".admin-checklist-row",
    ".roster-verified-banner"
  ]) {
    assert.ok(css.includes(selector), `CSS missing rule for ${selector}`);
  }
});

test("web/index.html <head> contains valid LocalBusiness JSON-LD structured data for Sirsa, Haryana", () => {
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  const headMatch = html.match(/<head>([\s\S]*?)<\/head>/i);
  assert.ok(headMatch, "index.html must have a <head> section");

  const ldMatch = headMatch[1].match(/<script\s+type="application\/ld\+json">([\s\S]*?)<\/script>/i);
  assert.ok(ldMatch, "<head> must include application/ld+json structured data");

  const jsonLd = JSON.parse(ldMatch[1]);
  assert.equal(jsonLd["@context"], "https://schema.org");
  assert.equal(jsonLd["@type"], "LocalBusiness");
  assert.equal(jsonLd.name, "STAR ARTIST STUDIO");
  assert.equal(jsonLd.email, "chanderstudio0@gmail.com");
  assert.equal(jsonLd.address.addressLocality, "Sirsa");
  assert.equal(jsonLd.address.addressRegion, "Haryana");
  assert.equal(jsonLd.address.postalCode, "125102");
  assert.equal(jsonLd.address.addressCountry, "IN");
});

test("Homepage Connect section includes Instagram, Facebook, and LinkedIn icons & external profile links", async () => {
  const dataMod = await import(pathToFileURL(path.join(webDir, "js/data.js")).href);
  assert.equal(dataMod.STUDIO_CONTACT.socialLinks.instagram, "https://www.instagram.com/starartiststudio");
  assert.equal(dataMod.STUDIO_CONTACT.socialLinks.facebook, "https://www.facebook.com/starartiststudio");
  assert.equal(dataMod.STUDIO_CONTACT.socialLinks.linkedin, "https://www.linkedin.com/company/star-artist-studio");

  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.ok(html.includes('id="homeConnectSection"'), "Homepage must contain Connect section");
  assert.ok(html.includes('id="connectHeading"'), "Connect section must have heading");
  assert.ok(html.includes('href="https://www.instagram.com/starartiststudio"'), "Must link Instagram profile");
  assert.ok(html.includes('href="https://www.facebook.com/starartiststudio"'), "Must link Facebook profile");
  assert.ok(html.includes('href="https://www.linkedin.com/company/star-artist-studio"'), "Must link LinkedIn profile");
  assert.ok(html.includes('aria-label="Star Artist Studio on Instagram"'));
  assert.ok(html.includes('aria-label="Star Artist Studio on Facebook"'));
  assert.ok(html.includes('aria-label="Star Artist Studio on LinkedIn"'));

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".connect-section",
    ".social-connect-grid",
    ".social-link-btn",
    ".social-icon-badge.is-instagram",
    ".social-icon-badge.is-facebook",
    ".social-icon-badge.is-linkedin",
    "@keyframes socialBtnPulse",
    "@keyframes socialIconShake"
  ]) {
    assert.ok(css.includes(selector), `CSS missing Connect rule for ${selector}`);
  }
});

test("Service catalog tier-info module renders tooltips, info badges, and Essential/Pro/Flagship guide", async () => {
  const dataMod = await import(pathToFileURL(path.join(webDir, "js/data.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);
  const tierMod = await import(pathToFileURL(path.join(webDir, "js/tier-info.js")).href);

  const pkg = dataMod.STUDIO_SERVICES[0];
  const proTier = pkg.tiers.find((t) => t.id === "pro") || pkg.tiers[0];
  const escape = (s) => String(s ?? "");

  const html = tierMod.renderCatalogTierSectionHtml({
    pkg,
    activeTier: proTier,
    isGuideExpanded: true,
    lang: "en",
    t: (k) => i18nMod.TRANSLATIONS.en[k] || k,
    escapeHtml: escape
  });

  assert.ok(html.includes('data-tooltip="'), "Tier buttons must expose descriptive data-tooltip attributes");
  assert.ok(html.includes('data-active-tier-info="pkg_photo_fashion"'), "Must render active tier info badge");
  assert.ok(html.includes('data-tier-guide-pkg="pkg_photo_fashion"'), "Must render Tier Info toggle button");
  assert.ok(html.includes('data-tier-guide-panel="pkg_photo_fashion"'), "Must render expanded tier guide panel");
  assert.ok(html.includes("CORE SESSION"), "Essential info badge must appear");
  assert.ok(html.includes("MOST POPULAR"), "Pro info badge must appear");
  assert.ok(html.includes("FULL VIP"), "Flagship info badge must appear");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".tier-guide-toggle",
    ".tier-tab.has-tier-tooltip",
    ".tier-info-badge",
    ".tier-guide-panel"
  ]) {
    assert.ok(css.includes(selector), `CSS missing tier info rule for ${selector}`);
  }
});

test("Footer Newsletter section exists in index.html and captures workshop email subscriptions cleanly", async () => {
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.ok(html.includes('<footer id="siteFooter"'), "index.html must include semantic <footer>");
  assert.ok(html.includes('id="footerNewsletterSection"'), "Footer must contain Newsletter section");
  assert.ok(html.includes('id="newsletterHeading"'), "Newsletter section must have heading");
  assert.ok(html.includes('id="newsletterForm"'), "Newsletter section must include form");
  assert.ok(html.includes('id="newsletterFormFields"'), "Newsletter form must wrap inputs in #newsletterFormFields");
  assert.ok(html.includes('id="newsletterSuccessNotice"') && html.includes("Thank you for subscribing!"), "Newsletter must include Thank you for subscribing! notice");
  assert.ok(html.includes('id="newsletterEmail"') && html.includes('type="email"'), "Newsletter must include email input");
  assert.ok(html.includes('id="newsletterErrorMsg"') && html.includes('role="alert"'), "Newsletter must include accessible error display");
  assert.ok(html.includes('id="newsletterSubscribeBtn"') && html.includes("Subscribe"), "Newsletter must include Subscribe button");

  const newsletterMod = await import(pathToFileURL(path.join(webDir, "js/newsletter.js")).href);
  assert.ok(newsletterMod.EMAIL_REGEX instanceof RegExp, "Must export EMAIL_REGEX");

  // Real-time input validation states
  assert.equal(newsletterMod.evaluateNewsletterEmailInput("", "en", { allowEmpty: true }).state, "idle");
  assert.equal(newsletterMod.evaluateNewsletterEmailInput("", "en", { allowEmpty: false }).state, "invalid");
  assert.equal(newsletterMod.evaluateNewsletterEmailInput("artist", "en").state, "invalid");
  assert.equal(newsletterMod.evaluateNewsletterEmailInput("artist@", "en").state, "invalid");
  assert.equal(newsletterMod.evaluateNewsletterEmailInput("artist@domain", "en").state, "invalid");
  assert.equal(newsletterMod.evaluateNewsletterEmailInput("a b@domain.com", "en").state, "invalid");
  assert.equal(newsletterMod.evaluateNewsletterEmailInput("a..b@domain.com", "en").state, "invalid");

  const validEval = newsletterMod.evaluateNewsletterEmailInput("  Artist.Sirsa+26@Studio.IN ", "en");
  assert.equal(validEval.state, "valid");
  assert.equal(validEval.isValid, true);
  assert.equal(validEval.normalizedEmail, "artist.sirsa+26@studio.in");
  assert.equal(validEval.error, "");

  const hiInvalid = newsletterMod.evaluateNewsletterEmailInput("invalid-email", "hi");
  assert.equal(hiInvalid.state, "invalid");
  assert.ok(hiInvalid.error.length > 0);

  const store = new Map();
  const mockStorage = {
    getItem: (k) => (store.has(k) ? store.get(k) : null),
    setItem: (k, v) => store.set(k, v)
  };

  const bad = newsletterMod.saveNewsletterSubscription({ email: "not-an-email", lang: "en", storage: mockStorage });
  assert.equal(bad.ok, false);
  assert.ok(bad.error, "Invalid email must return an error message");

  const first = newsletterMod.saveNewsletterSubscription({ email: "  Artist@Example.COM ", lang: "en", storage: mockStorage });
  assert.equal(first.ok, true);
  assert.equal(first.alreadySubscribed, false);
  assert.equal(first.entry.email, "artist@example.com");
  assert.equal(first.entry.interest, "studio_workshops");
  assert.equal(first.subscribers.length, 1);

  const duplicate = newsletterMod.saveNewsletterSubscription({ email: "artist@example.com", lang: "en", storage: mockStorage });
  assert.equal(duplicate.ok, true);
  assert.equal(duplicate.alreadySubscribed, true);
  assert.equal(duplicate.subscribers.length, 1);

  // Temporary success state replaces form inputs and restores after timer
  const makeFakeEl = (initialClasses = []) => {
    const set = new Set(initialClasses);
    return {
      textContent: "",
      classList: {
        add: (c) => set.add(c),
        remove: (c) => set.delete(c),
        contains: (c) => set.has(c)
      }
    };
  };
  const fieldsEl = makeFakeEl([]);
  const successEl = makeFakeEl(["hidden"]);
  const detailEl = makeFakeEl([]);
  let scheduledCb = null;
  let scheduledMs = 0;
  let restoredCalled = false;

  newsletterMod.showTemporaryNewsletterSuccess({
    fieldsEl,
    successEl,
    detailEl,
    detailText: "Workshop updates enabled (artist@example.com)",
    durationMs: 4000,
    scheduleTimeout: (cb, ms) => {
      scheduledCb = cb;
      scheduledMs = ms;
      return 101;
    },
    cancelTimeout: () => {},
    onRestore: () => {
      restoredCalled = true;
    }
  });

  assert.equal(fieldsEl.classList.contains("hidden"), true, "Form fields must be hidden during temporary success state");
  assert.equal(successEl.classList.contains("hidden"), false, "Success notice must be visible during temporary success state");
  assert.equal(detailEl.textContent, "Workshop updates enabled (artist@example.com)");
  assert.equal(scheduledMs, 4000);

  scheduledCb();
  assert.equal(successEl.classList.contains("hidden"), true, "Success notice must hide after timeout");
  assert.equal(fieldsEl.classList.contains("hidden"), false, "Form fields must reappear after timeout");
  assert.equal(restoredCalled, true, "onRestore callback must run after timeout");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".site-footer",
    ".newsletter-card",
    ".newsletter-input-row",
    ".newsletter-email-input.is-invalid",
    ".newsletter-email-input.is-valid",
    ".newsletter-error",
    ".newsletter-success-notice",
    ".newsletter-success-icon",
    ".newsletter-status"
  ]) {
    assert.ok(css.includes(selector), `CSS missing footer newsletter rule for ${selector}`);
  }
});

test("Lazy-load blur-up placeholder module covers all catalog and gallery images cleanly", async () => {
  const dataMod = await import(pathToFileURL(path.join(webDir, "js/data.js")).href);
  const lazyMod = await import(pathToFileURL(path.join(webDir, "js/lazy-images.js")).href);

  for (const pkg of dataMod.STUDIO_SERVICES) {
    assert.ok(pkg.imageUrl && pkg.imageUrl.startsWith("/images/"), `Service ${pkg.id} missing imageUrl`);
    const diskPath = path.join(webDir, pkg.imageUrl.replace(/^\//, ""));
    assert.ok(fs.existsSync(diskPath), `Service catalog image missing on disk: ${pkg.imageUrl}`);
  }

  const markup = lazyMod.renderLazyBlurImageHtml({
    src: "/images/gallery-fashion.jpg",
    alt: "High-Fashion Editorial",
    aspectClass: "is-catalog-cover"
  });
  assert.ok(markup.includes('class="blur-up-media is-catalog-cover"'));
  assert.ok(markup.includes('loading="lazy"'));
  assert.ok(markup.includes('decoding="async"'));
  assert.ok(markup.includes('data-src="/images/gallery-fashion.jpg"'));
  assert.ok(markup.includes('src="data:image/svg+xml;charset=utf-8,'));
  assert.ok(markup.includes("blur-up-shimmer"));

  const listeners = new Map();
  const classSet = new Set(["lazy-blur-img"]);
  const containerClassSet = new Set(["blur-up-media"]);
  const attrs = { src: "data:image/svg+xml;charset=utf-8,stub", "data-src": "/images/gallery-fashion.jpg" };
  const fakeContainer = {
    classList: { add: (c) => containerClassSet.add(c), has: (c) => containerClassSet.has(c) }
  };
  const fakeImg = {
    complete: false,
    naturalWidth: 0,
    getAttribute: (k) => attrs[k] ?? null,
    setAttribute: (k, v) => {
      attrs[k] = v;
    },
    removeAttribute: (k) => {
      delete attrs[k];
    },
    addEventListener: (ev, fn) => listeners.set(ev, fn),
    closest: () => fakeContainer,
    classList: { add: (c) => classSet.add(c), has: (c) => classSet.has(c) }
  };

  let observedImg = null;
  let unobserved = false;
  let capturedCallback = null;
  class FakeObserver {
    constructor(cb) {
      capturedCallback = cb;
    }
    observe(target) {
      observedImg = target;
    }
    unobserve() {
      unobserved = true;
    }
  }

  const count = lazyMod.initLazyBlurImages({ querySelectorAll: () => [fakeImg] }, FakeObserver);
  assert.equal(count, 1);
  assert.equal(observedImg, fakeImg);

  capturedCallback([{ isIntersecting: true, target: fakeImg }], new FakeObserver(() => {}));
  assert.equal(unobserved, true);
  assert.equal(attrs.src, "/images/gallery-fashion.jpg");
  listeners.get("load")?.();
  assert.equal(attrs["data-src"], undefined);
  assert.ok(classSet.has("is-loaded"), "Image must receive .is-loaded after load");
  assert.ok(containerClassSet.has("is-loaded"), "Container must receive .is-loaded after load");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [".blur-up-media", ".lazy-blur-img", ".lazy-blur-img.is-loaded", ".blur-up-shimmer"]) {
    assert.ok(css.includes(selector), `CSS missing blur-up rule for ${selector}`);
  }
});

test("Homepage testimonial carousel renders client and model feedback, wraps slides, and merges verified reviews", async () => {
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.ok(html.includes('id="homeTestimonialsSection"'), "Homepage must contain testimonial carousel section");
  assert.ok(html.includes('id="homeTestimonialCarousel"'), "Homepage must include testimonial carousel mount");
  assert.ok(html.includes('aria-roledescription="carousel"'), "Testimonial section must expose carousel landmark");

  const testiMod = await import(pathToFileURL(path.join(webDir, "js/testimonials.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);

  assert.ok(testiMod.STUDIO_TESTIMONIALS.length >= 4, "Must include multiple curated client & model testimonials");
  assert.ok(testiMod.STUDIO_TESTIMONIALS.some((x) => x.roleType === "model"), "Must include model testimonials");
  assert.ok(testiMod.STUDIO_TESTIMONIALS.some((x) => x.roleType === "client"), "Must include client testimonials");

  // Index wrapping both forward and backward
  assert.equal(testiMod.getNextSlideIndex(4, 5, 1), 0);
  assert.equal(testiMod.getNextSlideIndex(0, 5, -1), 4);
  assert.equal(testiMod.getNextSlideIndex(2, 5, 1), 3);

  // Merges positive 4-5 star live reviews and excludes low-rating/short noise
  const merged = testiMod.buildCarouselTestimonials(
    [
      {
        id: "rev_1",
        reviewerName: "Neha Sharma",
        serviceType: "Modeling (Portfolio & Comp-Card Shoot)",
        rating: 5,
        comment: "Incredible strobe setup and super supportive casting team!"
      },
      {
        id: "rev_low",
        reviewerName: "Skip",
        serviceType: "Portrait",
        rating: 2,
        comment: "Too short or low rating"
      }
    ],
    "en"
  );
  assert.equal(merged.length, testiMod.STUDIO_TESTIMONIALS.length + 1);
  assert.equal(merged[0].name, "Neha Sharma");
  assert.equal(merged[0].roleType, "model");

  const rendered = testiMod.renderTestimonialCarouselHtml({
    items: merged,
    activeIndex: 0,
    autoplay: true,
    t: (k) => i18nMod.TRANSLATIONS.en[k] || k
  });
  assert.ok(rendered.includes("Neha Sharma"));
  assert.ok(rendered.includes("Incredible strobe setup and super supportive casting team!"));
  assert.ok(rendered.includes("data-testimonial-prev"));
  assert.ok(rendered.includes("data-testimonial-next"));
  assert.ok(rendered.includes('data-testimonial-dot="0"'));
  assert.ok(rendered.includes("data-testimonial-autoplay"));

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".testimonial-section",
    ".testimonial-slide",
    ".testimonial-quote",
    ".testimonial-nav-btn",
    ".testimonial-dot.active"
  ]) {
    assert.ok(css.includes(selector), `CSS missing testimonial rule for ${selector}`);
  }
});

test("Service Worker Background Sync API & offline booking queue validate, register sync tag, format Firestore REST docs, and flush cleanly", async () => {
  const swText = fs.readFileSync(path.join(webDir, "sw.js"), "utf8");
  assert.ok(swText.includes('self.addEventListener("sync"'), "sw.js must listen for Background Sync 'sync' events");
  assert.ok(swText.includes('const BOOKING_SYNC_TAG = "star-artist-sync-bookings"'), "sw.js must define booking sync tag");
  assert.ok(swText.includes("star-artist-offline-sync-v1"), "sw.js must use shared IndexedDB offline sync database");
  assert.ok(swText.includes("pending_bookings"), "sw.js must read/write pending_bookings store");
  assert.ok(swText.includes("https://firestore.googleapis.com/v1/projects/"), "sw.js must push queued bookings to Firestore REST API");
  assert.ok(swText.includes("BOOKING_SYNC_COMPLETE"), "sw.js must notify clients after Background Sync completes");

  const offlineMod = await import(pathToFileURL(path.join(webDir, "js/offline-sync.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);

  // Validation mirrors firestore.rules isValidBooking()
  assert.ok(
    offlineMod.validateOfflineBookingPayload({
      clientName: "A",
      contactNumber: "9812890081",
      serviceType: "Editorial Portrait",
      preferredDate: "2026-12-01"
    }),
    "Name shorter than 2 chars must fail validation"
  );
  assert.ok(
    offlineMod.validateOfflineBookingPayload({
      clientName: "Priya Sharma",
      contactNumber: "12345",
      serviceType: "Editorial Portrait",
      preferredDate: "2026-12-01"
    }),
    "Phone shorter than 8 chars must fail validation"
  );
  assert.equal(
    offlineMod.validateOfflineBookingPayload({
      clientName: "Priya Sharma",
      contactNumber: "+91 9812890081",
      serviceType: "Cinematic Wedding Package - Pro",
      preferredDate: "2026-12-15",
      preferredTime: "11:30 AM",
      location: "Studio (Talwara Khurd)",
      notes: "Golden hour portraits"
    }),
    null
  );

  // Endpoint & Firestore REST v1 document formatting
  const endpoint = offlineMod.buildFirestoreBookingsEndpoint();
  assert.equal(
    endpoint,
    "https://firestore.googleapis.com/v1/projects/gen-lang-client-0548683163/databases/ai-studio-android-stararti-4ffde0d6-02fd-4194-8e31-3359a6b9588b/documents/bookings"
  );

  const record = offlineMod.createQueuedBookingRecord({
    payload: {
      clientName: "Priya Sharma",
      contactNumber: "+91 9812890081",
      serviceType: "Editorial Portrait - Pro",
      preferredDate: "2026-12-20",
      preferredTime: "04:30 PM",
      location: "Studio (Talwara Khurd)",
      notes: "Warm rim lighting"
    },
    userId: "uid_demo_123",
    authToken: "eyJ_mock_token"
  });

  const fsDoc = offlineMod.buildFirestoreBookingDocument(record, "2026-11-19T12:00:00.000Z");
  assert.equal(fsDoc.fields.userId.stringValue, "uid_demo_123");
  assert.equal(fsDoc.fields.status.stringValue, "Pending");
  assert.equal(fsDoc.fields.clientName.stringValue, "Priya Sharma");
  assert.equal(fsDoc.fields.createdAt.timestampValue, "2026-11-19T12:00:00.000Z");
  assert.equal(fsDoc.fields.updatedAt.timestampValue, "2026-11-19T12:00:00.000Z");

  // Offline error detection
  assert.equal(offlineMod.isLikelyOfflineError(null, false), true);
  assert.equal(offlineMod.isLikelyOfflineError(new Error("Failed to fetch"), true), true);
  assert.equal(offlineMod.isLikelyOfflineError({ code: "unavailable", message: "client is offline" }, true), true);
  assert.equal(offlineMod.isLikelyOfflineError(new Error("Permission denied"), true), false);

  // Background Sync API registration
  const registeredTags = [];
  const syncResult = await offlineMod.registerBookingBackgroundSync({
    ready: Promise.resolve({
      sync: {
        register: async (tag) => {
          registeredTags.push(tag);
        }
      }
    })
  });
  assert.deepEqual(syncResult, { registered: true, mode: "background-sync" });
  assert.deepEqual(registeredTags, [offlineMod.BOOKING_SYNC_TAG]);

  // Client fallback flush pushes queued booking and removes it from queue
  const pushed = [];
  const removed = [];
  const flushResult = await offlineMod.flushQueuedBookingsFromClient({
    queuedRecords: [record],
    isOnline: true,
    createBookingFn: async (payload) => {
      pushed.push(payload);
    },
    removeFn: async (id) => {
      removed.push(id);
    }
  });
  assert.equal(flushResult.syncedCount, 1);
  assert.equal(flushResult.remainingCount, 0);
  assert.equal(pushed.length, 1);
  assert.equal(pushed[0].clientName, "Priya Sharma");
  assert.deepEqual(removed, [record.id]);

  // Queue UI markup & CSS selectors
  const queueHtml = offlineMod.renderOfflineQueueHtml({
    queuedBookings: [record],
    isOnline: false,
    t: (k) => i18nMod.TRANSLATIONS.en[k] || k,
    escapeHtml: (v) => String(v ?? "")
  });
  assert.ok(queueHtml.includes("offline-queue-panel"));
  assert.ok(queueHtml.includes("Editorial Portrait - Pro"));
  assert.ok(queueHtml.includes("data-remove-offline-booking"));

  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.ok(html.includes('id="offlineBookingsQueueBox"'), "index.html must include offline bookings queue container");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [".offline-queue-panel", ".offline-queue-item", ".offline-sync-pill", ".offline-net-chip"]) {
    assert.ok(css.includes(selector), `CSS missing offline sync rule for ${selector}`);
  }
});

test("Filter-bar pill count badges and gallery lightbox Web Share API work cleanly", async () => {
  const dataMod = await import(pathToFileURL(path.join(webDir, "js/data.js")).href);
  const shareMod = await import(pathToFileURL(path.join(webDir, "js/gallery-share.js")).href);

  // 1. Catalog category count badges
  const allCounts = shareMod.countCatalogByCategory(dataMod.STUDIO_SERVICES, "");
  assert.equal(allCounts.all, dataMod.STUDIO_SERVICES.length);
  assert.equal(allCounts.photography + allCounts.modeling + allCounts.production, allCounts.all);
  assert.ok(allCounts.photography > 0);
  assert.ok(allCounts.modeling > 0);
  assert.ok(allCounts.production > 0);

  const bridalCounts = shareMod.countCatalogByCategory(dataMod.STUDIO_SERVICES, "bridal");
  assert.ok(bridalCounts.all >= 1 && bridalCounts.all < allCounts.all);

  // 2. Gallery category count badges
  const galCats = ["All", "Fashion", "Editorial", "Portrait", "Bridal", "Commercial", "Video Reel"];
  const galCounts = shareMod.countGalleryByCategory(dataMod.SHOWCASE_GALLERY_SEEDS, galCats);
  assert.equal(galCounts.All, dataMod.SHOWCASE_GALLERY_SEEDS.length);
  assert.ok(galCounts.Fashion >= 1);

  // 3. Lightbox Web Share API (link share, image file share, AbortError, and clipboard fallback)
  const sampleImg = dataMod.SHOWCASE_GALLERY_SEEDS[0];
  const payload = shareMod.buildGallerySharePayload(sampleImg, "https://gen-lang-client-0548683163.web.app/");
  assert.ok(payload.title.includes(sampleImg.title));
  assert.ok(payload.url.startsWith("https://gen-lang-client-0548683163.web.app/"));

  // Native Web Share link flow
  const sharedCalls = [];
  const linkRes = await shareMod.shareGalleryImage(sampleImg, {
    nav: {
      share: async (data) => {
        sharedCalls.push(data);
      }
    },
    baseUrl: "https://gen-lang-client-0548683163.web.app/"
  });
  assert.equal(linkRes.status, "shared-link");
  assert.equal(sharedCalls.length, 1);
  assert.equal(sharedCalls[0].title, payload.title);

  // Native Web Share Level 2 file attachment flow
  class MockFile {
    constructor(parts, name, opts) {
      this.parts = parts;
      this.name = name;
      this.type = opts?.type || "image/jpeg";
    }
  }
  const fileSharedCalls = [];
  const fileRes = await shareMod.shareGalleryImage(sampleImg, {
    nav: {
      canShare: (d) => Array.isArray(d?.files) && d.files.length === 1,
      share: async (data) => {
        fileSharedCalls.push(data);
      }
    },
    fetchImpl: async () => ({
      ok: true,
      blob: async () => ({ type: "image/jpeg" })
    }),
    FileCtor: MockFile,
    baseUrl: "https://gen-lang-client-0548683163.web.app/"
  });
  assert.equal(fileRes.status, "shared-file");
  assert.equal(fileSharedCalls.length, 1);
  assert.equal(fileSharedCalls[0].files.length, 1);

  // User cancels native share sheet (AbortError)
  const cancelRes = await shareMod.shareGalleryImage(sampleImg, {
    nav: {
      share: async () => {
        const err = new Error("Share canceled");
        err.name = "AbortError";
        throw err;
      }
    },
    baseUrl: "https://gen-lang-client-0548683163.web.app/"
  });
  assert.equal(cancelRes.status, "cancelled");

  // Desktop clipboard fallback when Web Share is unavailable
  let copiedText = "";
  const clipRes = await shareMod.shareGalleryImage(sampleImg, {
    nav: {
      clipboard: {
        writeText: async (txt) => {
          copiedText = txt;
        }
      }
    },
    baseUrl: "https://gen-lang-client-0548683163.web.app/"
  });
  assert.equal(clipRes.status, "copied");
  assert.ok(copiedText.includes(payload.url));

  // 4. Lightbox direct image Download (Blob Object URL & direct link fallback)
  const createdAnchors = [];
  const mockDoc = {
    body: {
      appendChild: () => {},
      removeChild: () => {}
    },
    createElement: (tag) => {
      const el = {
        tag,
        href: "",
        download: "",
        style: {},
        clicked: false,
        removed: false,
        click() {
          this.clicked = true;
        },
        remove() {
          this.removed = true;
        }
      };
      createdAnchors.push(el);
      return el;
    }
  };
  let revokedUrl = "";
  const blobDlRes = await shareMod.downloadGalleryImage(sampleImg, {
    doc: mockDoc,
    fetchImpl: async () => ({
      ok: true,
      blob: async () => ({ type: "image/jpeg" })
    }),
    urlApi: {
      createObjectURL: () => "blob:star-artist/mock-image",
      revokeObjectURL: (u) => {
        revokedUrl = u;
      }
    },
    baseUrl: "https://gen-lang-client-0548683163.web.app/"
  });
  assert.equal(blobDlRes.status, "downloaded-blob");
  assert.ok(blobDlRes.fileName.endsWith(".jpg"));
  assert.equal(createdAnchors[0].href, "blob:star-artist/mock-image");
  assert.equal(createdAnchors[0].download, blobDlRes.fileName);
  assert.equal(createdAnchors[0].clicked, true);
  assert.equal(createdAnchors[0].removed, true);

  // Fallback direct anchor download when fetch fails (e.g. third-party CORS)
  const fallbackDlRes = await shareMod.downloadGalleryImage(sampleImg, {
    doc: mockDoc,
    fetchImpl: async () => {
      throw new Error("CORS blocked");
    },
    urlApi: { createObjectURL: () => "" },
    baseUrl: "https://gen-lang-client-0548683163.web.app/"
  });
  assert.equal(fallbackDlRes.status, "downloaded-link");
  assert.equal(createdAnchors[1].clicked, true);
  assert.equal(createdAnchors[1].href, payload.imageUrl);
  assert.equal(typeof revokedUrl, "string");

  // HTML & CSS verification
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.ok(html.includes('data-cat-count="all"'), "Catalog All filter must include count pill badge");
  assert.ok(html.includes('data-cat-count="photography"'), "Catalog Photography filter must include count pill badge");
  assert.ok(html.includes('data-admin-count="bookings"'), "Admin filter bar must include count pill badge");
  assert.ok(html.includes('id="lightboxDownloadBtn"'), "Gallery lightbox must include Download button");
  assert.ok(html.includes('id="lightboxShareBtn"'), "Gallery lightbox must include Share button");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".filter-count-badge",
    ".chip-btn.active .filter-count-badge",
    ".lightbox-footer",
    ".lightbox-actions",
    ".lightbox-action-btn",
    ".lightbox-share-btn"
  ]) {
    assert.ok(css.includes(selector), `CSS missing rule for ${selector}`);
  }
});

test("Direct device gallery image upload validates files, scales dimensions, enforces Firestore limits, and exposes UI markup", async () => {
  const uploadMod = await import(pathToFileURL(path.join(webDir, "js/image-upload.js")).href);

  // 1. File validation
  assert.equal(uploadMod.validateGalleryImageFile(null).ok, false);
  assert.equal(uploadMod.validateGalleryImageFile({ name: "doc.pdf", type: "application/pdf", size: 2048 }).ok, false);
  assert.equal(uploadMod.validateGalleryImageFile({ name: "empty.jpg", type: "image/jpeg", size: 0 }).ok, false);
  assert.equal(
    uploadMod.validateGalleryImageFile({
      name: "huge.png",
      type: "image/png",
      size: uploadMod.MAX_UPLOAD_FILE_BYTES + 10
    }).ok,
    false
  );
  assert.equal(uploadMod.validateGalleryImageFile({ name: "bridal_shot.jpg", type: "image/jpeg", size: 250000 }).ok, true);
  assert.equal(uploadMod.validateGalleryImageFile({ name: "lookbook.webp", type: "", size: 120000 }).ok, true);

  // 2. Proportional scaling without upscaling
  assert.deepEqual(uploadMod.computeScaledDimensions(2560, 1280, 1280), { width: 1280, height: 640 });
  assert.deepEqual(uploadMod.computeScaledDimensions(900, 600, 1280), { width: 900, height: 600 });
  assert.deepEqual(uploadMod.computeScaledDimensions(1000, 2000, 480), { width: 240, height: 480 });

  // 3. Title suggestion helper
  assert.equal(uploadMod.suggestTitleFromFileName("royal_bridal-portrait_01.JPG"), "royal bridal portrait 01");

  // 4. Submission validation mirrors firestore.rules isValidGallery()
  assert.ok(uploadMod.validateGallerySubmission({ title: "", mediaUrl: "https://example.com/a.jpg" }));
  assert.ok(uploadMod.validateGallerySubmission({ title: "Editorial Shoot", mediaUrl: "" }));
  assert.ok(uploadMod.validateGallerySubmission({ title: "Editorial Shoot", mediaUrl: "javascript:alert(1)" }));
  assert.ok(
    uploadMod.validateGallerySubmission({
      title: "Oversized Photo",
      mediaUrl: "data:image/jpeg;base64," + "A".repeat(uploadMod.FIRESTORE_MEDIA_URL_MAX + 1)
    })
  );
  assert.ok(
    uploadMod.validateGallerySubmission({
      title: "Oversized Thumb",
      mediaUrl: "data:image/jpeg;base64,AAAA",
      thumbnailUrl: "data:image/jpeg;base64," + "B".repeat(uploadMod.FIRESTORE_THUMB_URL_MAX + 1)
    })
  );
  assert.equal(
    uploadMod.validateGallerySubmission({
      title: "Royal Bridal Shoot",
      mediaUrl: "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/",
      thumbnailUrl: "data:image/jpeg;base64,/9j/4AAQSkZJRg=="
    }),
    null
  );
  assert.equal(
    uploadMod.validateGallerySubmission({
      title: "Cinema Reel",
      mediaUrl: "https://example.com/reel.mp4"
    }),
    null
  );

  // 5. HTML & CSS verification
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.ok(html.includes('id="galleryUploadDropzone"'), "index.html must include gallery upload dropzone");
  assert.ok(html.includes('id="galleryImageFile"') && html.includes('type="file"') && html.includes('accept="image/*"'), "index.html must include image file input");
  assert.ok(html.includes('id="galleryUploadPreviewBox"'), "index.html must include live image preview container");
  assert.ok(html.includes('id="clearGalleryImageBtn"'), "index.html must include remove photo button");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".gallery-upload-dropzone",
    ".gallery-upload-dropzone.is-dragover",
    ".gallery-upload-preview",
    ".gallery-upload-preview-img",
    ".gallery-upload-meta"
  ]) {
    assert.ok(css.includes(selector), `CSS missing image upload rule for ${selector}`);
  }
});

test("Gallery lightbox CSS filter presets preview styles and bake active filters into downloads", async () => {
  const shareMod = await import(pathToFileURL(path.join(webDir, "js/gallery-share.js")).href);
  const dataMod = await import(pathToFileURL(path.join(webDir, "js/data.js")).href);

  // 1. Presets include Original, Grayscale, Sepia, Vivid, Noir, Golden
  const presetIds = shareMod.LIGHTBOX_FILTER_PRESETS.map((p) => p.id);
  for (const id of ["original", "grayscale", "sepia", "vivid", "noir", "golden"]) {
    assert.ok(presetIds.includes(id), `Missing filter preset: ${id}`);
  }

  // 2. Apply CSS filter preset to an image element
  const attrs = {};
  const fakeImg = {
    style: { filter: "" },
    setAttribute: (k, v) => {
      attrs[k] = v;
    }
  };
  const vividPreset = shareMod.applyLightboxFilterToElement(fakeImg, "vivid");
  assert.equal(vividPreset.id, "vivid");
  assert.ok(fakeImg.style.filter.includes("saturate("));
  assert.equal(attrs["data-active-filter"], "vivid");

  // Unknown preset safely falls back to original
  const fallbackPreset = shareMod.applyLightboxFilterToElement(fakeImg, "unknown_style");
  assert.equal(fallbackPreset.id, "original");
  assert.equal(fakeImg.style.filter, "none");

  // 3. Filtered filename helper appends preset slug cleanly
  assert.equal(shareMod.buildFilteredFileName("editorial.jpg", "original"), "editorial.jpg");
  assert.equal(shareMod.buildFilteredFileName("editorial.jpg", "sepia"), "editorial-sepia.jpg");
  assert.equal(shareMod.buildFilteredFileName("bridal.png", "grayscale"), "bridal-grayscale.png");

  // 4. Filtered download renders through offscreen canvas with ctx.filter applied
  let appliedCanvasFilter = "";
  let drawnDims = null;
  const createdAnchors = [];
  const mockDoc = {
    body: { appendChild: () => {}, removeChild: () => {} },
    createElement: (tag) => {
      if (tag === "canvas") {
        return {
          width: 0,
          height: 0,
          getContext: () => ({
            set filter(val) {
              appliedCanvasFilter = val;
            },
            drawImage: (_img, _x, _y, w, h) => {
              drawnDims = { w, h };
            }
          }),
          toBlob: (cb, mime) => cb({ type: mime || "image/jpeg", filtered: true })
        };
      }
      const el = {
        tag,
        href: "",
        download: "",
        style: {},
        clicked: false,
        click() {
          this.clicked = true;
        },
        remove() {}
      };
      createdAnchors.push(el);
      return el;
    }
  };

  const sampleImg = dataMod.SHOWCASE_GALLERY_SEEDS[0];
  const dlResult = await shareMod.downloadGalleryImage(sampleImg, {
    doc: mockDoc,
    fetchImpl: async () => ({
      ok: true,
      blob: async () => ({ type: "image/jpeg" })
    }),
    urlApi: {
      createObjectURL: () => "blob:star-artist/filtered-sepia",
      revokeObjectURL: () => {}
    },
    loadImageFn: async () => ({ naturalWidth: 1200, naturalHeight: 800 }),
    filterPresetId: "sepia",
    baseUrl: "https://gen-lang-client-0548683163.web.app/"
  });

  assert.equal(dlResult.status, "downloaded-filtered");
  assert.equal(dlResult.filterId, "sepia");
  assert.ok(dlResult.fileName.endsWith("-sepia.jpg"));
  assert.ok(appliedCanvasFilter.includes("sepia("), "Canvas context must receive CSS sepia filter");
  assert.deepEqual(drawnDims, { w: 1200, h: 800 });
  assert.equal(createdAnchors[0].clicked, true);
  assert.equal(createdAnchors[0].download, dlResult.fileName);

  // 5. HTML & CSS verification
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.ok(html.includes('id="lightboxFilterBar"'), "Lightbox must include #lightboxFilterBar");
  for (const id of ["original", "grayscale", "sepia", "vivid", "noir", "golden"]) {
    assert.ok(html.includes(`data-lightbox-filter="${id}"`), `Lightbox HTML missing preset button: ${id}`);
  }

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".lightbox-image",
    ".lightbox-filter-bar",
    ".lightbox-filter-btn",
    ".lightbox-filter-btn.active",
    ".lightbox-filter-swatch.is-grayscale",
    ".lightbox-filter-swatch.is-sepia",
    ".lightbox-filter-swatch.is-vivid"
  ]) {
    assert.ok(css.includes(selector), `CSS missing lightbox filter rule for ${selector}`);
  }
});

test("Gallery lightbox supports manual arrow cycling, keyboard shortcuts, and automatic slideshow timer", async () => {
  const shareMod = await import(pathToFileURL(path.join(webDir, "js/gallery-share.js")).href);
  const dataMod = await import(pathToFileURL(path.join(webDir, "js/data.js")).href);

  // 1. Visible gallery filtering
  const allItems = shareMod.getVisibleGalleryItems(dataMod.SHOWCASE_GALLERY_SEEDS, "All");
  assert.equal(allItems.length, dataMod.SHOWCASE_GALLERY_SEEDS.length);
  const fashionItems = shareMod.getVisibleGalleryItems(dataMod.SHOWCASE_GALLERY_SEEDS, "Fashion");
  assert.ok(fashionItems.length >= 1 && fashionItems.every((i) => i.category === "Fashion"));

  // 2. Manual slide index wrapping (forward & backward)
  assert.equal(shareMod.getNextLightboxIndex(0, 6, 1), 1);
  assert.equal(shareMod.getNextLightboxIndex(5, 6, 1), 0);
  assert.equal(shareMod.getNextLightboxIndex(0, 6, -1), 5);
  assert.equal(shareMod.getNextLightboxIndex(0, 1, 1), 0);
  assert.equal(shareMod.getNextLightboxIndex(0, 0, 1), -1);

  // 3. Slide counter formatting
  assert.equal(shareMod.formatLightboxSlideCounter(0, 6), "1 / 6");
  assert.equal(shareMod.formatLightboxSlideCounter(5, 6), "6 / 6");
  assert.equal(shareMod.formatLightboxSlideCounter(0, 0), "");

  // 4. Keyboard action mapping (ArrowLeft, ArrowRight, Space, Escape)
  assert.equal(shareMod.resolveLightboxKeyAction("ArrowLeft"), "prev");
  assert.equal(shareMod.resolveLightboxKeyAction("ArrowRight"), "next");
  assert.equal(shareMod.resolveLightboxKeyAction(" "), "toggle-play");
  assert.equal(shareMod.resolveLightboxKeyAction("Escape"), "close");
  assert.equal(shareMod.resolveLightboxKeyAction("Enter"), null);

  // 5. Automatic slideshow timer controller lifecycle (start, step, reset, toggle, stop)
  let activeCallback = null;
  let clearedCount = 0;
  let stepCount = 0;
  const stateChanges = [];
  let nextTimerId = 1;

  const ctrl = shareMod.createLightboxSlideshowController({
    intervalMs: 4000,
    onStep: () => {
      stepCount += 1;
    },
    onStateChange: (playing) => {
      stateChanges.push(playing);
    },
    setIntervalFn: (fn) => {
      activeCallback = fn;
      return nextTimerId++;
    },
    clearIntervalFn: () => {
      clearedCount += 1;
      activeCallback = null;
    }
  });

  assert.equal(ctrl.isPlaying(), false);
  assert.equal(ctrl.toggle(), true);
  assert.equal(ctrl.isPlaying(), true);
  assert.deepEqual(stateChanges, [true]);

  // Simulate two automatic ticks
  activeCallback();
  activeCallback();
  assert.equal(stepCount, 2);

  // Resetting timer after manual step clears old interval and restarts cleanly
  assert.equal(ctrl.reset(), true);
  assert.equal(clearedCount, 1);
  assert.equal(ctrl.isPlaying(), true);

  // Stopping cleans up timer and notifies state change
  assert.equal(ctrl.toggle(), false);
  assert.equal(ctrl.isPlaying(), false);
  assert.equal(clearedCount, 2);
  assert.deepEqual(stateChanges, [true, false]);

  // 6. HTML & CSS verification
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  for (const id of [
    'id="lightboxStage"',
    'id="lightboxPrevBtn"',
    'id="lightboxNextBtn"',
    'id="lightboxCounter"',
    'id="lightboxSlideshowBtn"'
  ]) {
    assert.ok(html.includes(id), `Lightbox HTML missing slideshow element: ${id}`);
  }

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".lightbox-stage",
    ".lightbox-nav-btn",
    ".lightbox-nav-btn.is-prev",
    ".lightbox-nav-btn.is-next",
    ".lightbox-counter-badge",
    ".lightbox-slideshow-bar",
    ".lightbox-slideshow-btn.is-playing"
  ]) {
    assert.ok(css.includes(selector), `CSS missing lightbox slideshow rule for ${selector}`);
  }
});

test("Gallery lightbox displays EXIF camera metadata (camera, lens, aperture, shutter, ISO, date taken)", async () => {
  const shareMod = await import(pathToFileURL(path.join(webDir, "js/gallery-share.js")).href);
  const dataMod = await import(pathToFileURL(path.join(webDir, "js/data.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);

  // 1. All studio showcase seeds carry rich portfolio EXIF metadata
  for (const seed of dataMod.SHOWCASE_GALLERY_SEEDS) {
    assert.ok(seed.metadata?.camera, `Seed ${seed.id} missing camera metadata`);
    assert.ok(seed.metadata?.aperture, `Seed ${seed.id} missing aperture metadata`);
    assert.ok(seed.metadata?.iso, `Seed ${seed.id} missing ISO metadata`);
    assert.ok(seed.metadata?.dateTaken, `Seed ${seed.id} missing dateTaken metadata`);
  }

  // 2. Date formatting handles ISO strings and Firestore Timestamp objects
  const isoFormatted = shareMod.formatCaptureDate("2026-10-14", "en");
  assert.ok(isoFormatted.includes("2026") && isoFormatted.includes("14"));
  const tsFormatted = shareMod.formatCaptureDate({ seconds: 1791979200 }, "en");
  assert.ok(tsFormatted.includes("2026"));

  // 3. Normalization adds f/ and ISO prefixes when raw numbers are supplied
  const normalized = shareMod.extractLightboxImageMetadata(
    {
      exif: {
        camera: "Nikon Z8",
        lens: "50mm f/1.8 S",
        aperture: "2.0",
        shutterSpeed: "1/320s",
        iso: 400
      },
      createdAt: { seconds: 1791979200 }
    },
    "en"
  );
  assert.equal(normalized.camera, "Nikon Z8");
  assert.equal(normalized.aperture, "f/2.0");
  assert.equal(normalized.iso, "ISO 400");
  assert.equal(normalized.shutterSpeed, "1/320s");
  assert.ok(normalized.dateTaken.includes("2026"));

  // 4. Rendered EXIF panel HTML includes all fields & fallback archive chip when empty
  const firstHtml = shareMod.renderLightboxMetadataHtml(dataMod.SHOWCASE_GALLERY_SEEDS[0], {
    lang: "en",
    t: (k) => i18nMod.TRANSLATIONS.en[k] || k
  });
  for (const token of [
    'data-exif-field="camera"',
    'data-exif-field="aperture"',
    'data-exif-field="shutter"',
    'data-exif-field="iso"',
    'data-exif-field="date"',
    "Sony α7R V",
    "f/2.2",
    "ISO 100"
  ]) {
    assert.ok(firstHtml.includes(token), `Rendered EXIF HTML missing expected token: ${token}`);
  }

  const fallbackHtml = shareMod.renderLightboxMetadataHtml(
    { id: "user_1", title: "Client Upload", category: "Portrait" },
    {
      lang: "en",
      t: (k) => i18nMod.TRANSLATIONS.en[k] || k
    }
  );
  assert.ok(fallbackHtml.includes("Star Artist Studio Archive"));

  // 5. HTML & CSS verification
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.ok(html.includes('id="lightboxExifPanel"'), "Lightbox must include #lightboxExifPanel container");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".lightbox-exif-panel",
    ".lightbox-exif-title",
    ".lightbox-exif-grid",
    ".lightbox-exif-chip",
    ".lightbox-exif-dt",
    ".lightbox-exif-dd"
  ]) {
    assert.ok(css.includes(selector), `CSS missing EXIF metadata rule for ${selector}`);
  }
});

test("Automated binary EXIF reader extracts Camera, Lens, Aperture, Shutter, ISO, and Date Taken from JPEG, PNG, and Data URLs", async () => {
  const exifMod = await import(pathToFileURL(path.join(webDir, "js/exif-reader.js")).href);
  const shareMod = await import(pathToFileURL(path.join(webDir, "js/gallery-share.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);

  // Helper to build a real binary TIFF EXIF block (supports both Intel 'II' little-endian and Motorola 'MM' big-endian)
  function buildTestTiffBlock(littleEndian) {
    const buf = new ArrayBuffer(512);
    const view = new DataView(buf);
    const bytes = new Uint8Array(buf);

    // TIFF Header
    view.setUint16(0, littleEndian ? 0x4949 : 0x4d4d, false);
    view.setUint16(2, 0x002a, littleEndian);
    view.setUint32(4, 8, littleEndian); // IFD0 starts at offset 8

    // Helper to append ASCII string / RATIONAL value at dataCursor
    let dataCursor = 200;
    const writeAscii = (str) => {
      const start = dataCursor;
      for (let i = 0; i < str.length; i++) {
        bytes[dataCursor++] = str.charCodeAt(i);
      }
      bytes[dataCursor++] = 0;
      return { offset: start, count: str.length + 1 };
    };
    const writeRational = (num, den) => {
      const start = dataCursor;
      view.setUint32(dataCursor, num, littleEndian);
      view.setUint32(dataCursor + 4, den, littleEndian);
      dataCursor += 8;
      return start;
    };

    const makeInfo = writeAscii("Canon");
    const modelInfo = writeAscii("Canon EOS R5");
    const lensInfo = writeAscii("RF50mm F1.2 L USM");
    const dateInfo = writeAscii("2026:11:18 16:45:00");
    const apertureOff = writeRational(18, 10); // f/1.8
    const shutterOff = writeRational(1, 250); // 1/250s

    // IFD0 at offset 8: 3 entries (Make, Model, ExifIFDPointer)
    const exifIfdOffset = 80;
    view.setUint16(8, 3, littleEndian);
    const writeEntry = (pos, tag, type, count, valOrOff) => {
      view.setUint16(pos, tag, littleEndian);
      view.setUint16(pos + 2, type, littleEndian);
      view.setUint32(pos + 4, count, littleEndian);
      if (type === 3 && count === 1) {
        view.setUint16(pos + 8, valOrOff, littleEndian);
      } else {
        view.setUint32(pos + 8, valOrOff, littleEndian);
      }
    };

    writeEntry(10, 0x010f, 2, makeInfo.count, makeInfo.offset);
    writeEntry(22, 0x0110, 2, modelInfo.count, modelInfo.offset);
    writeEntry(34, 0x8769, 4, 1, exifIfdOffset);
    view.setUint32(46, 0, littleEndian);

    // Exif Sub-IFD at offset 80: 5 entries (ExposureTime, FNumber, ISO, DateTimeOriginal, LensModel)
    view.setUint16(exifIfdOffset, 5, littleEndian);
    writeEntry(exifIfdOffset + 2, 0x829a, 5, 1, shutterOff);
    writeEntry(exifIfdOffset + 14, 0x829d, 5, 1, apertureOff);
    writeEntry(exifIfdOffset + 26, 0x8827, 3, 1, 400);
    writeEntry(exifIfdOffset + 38, 0x9003, 2, dateInfo.count, dateInfo.offset);
    writeEntry(exifIfdOffset + 50, 0xa434, 2, lensInfo.count, lensInfo.offset);
    view.setUint32(exifIfdOffset + 62, 0, littleEndian);

    return bytes.slice(0, dataCursor);
  }

  function wrapAsJpeg(tiffBytes) {
    const segLen = 2 + 6 + tiffBytes.length;
    const out = new Uint8Array(2 + 2 + segLen + 2);
    out[0] = 0xff;
    out[1] = 0xd8; // SOI
    out[2] = 0xff;
    out[3] = 0xe1; // APP1
    out[4] = (segLen >> 8) & 0xff;
    out[5] = segLen & 0xff;
    // "Exif\0\0"
    out[6] = 0x45;
    out[7] = 0x78;
    out[8] = 0x69;
    out[9] = 0x66;
    out[10] = 0x00;
    out[11] = 0x00;
    out.set(tiffBytes, 12);
    out[out.length - 2] = 0xff;
    out[out.length - 1] = 0xd9; // EOI
    return out;
  }

  function wrapAsPng(tiffBytes) {
    const sig = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
    const out = new Uint8Array(sig.length + 12 + tiffBytes.length);
    out.set(sig, 0);
    const dv = new DataView(out.buffer);
    dv.setUint32(8, tiffBytes.length, false);
    // "eXIf"
    out[12] = 0x65;
    out[13] = 0x58;
    out[14] = 0x49;
    out[15] = 0x66;
    out.set(tiffBytes, 16);
    return out;
  }

  // 1. Little-Endian JPEG APP1 EXIF parsing
  const jpegLE = wrapAsJpeg(buildTestTiffBlock(true));
  const parsedLE = exifMod.parseExifFromArrayBuffer(jpegLE);
  assert.equal(parsedLE.hasExif, true);
  assert.equal(parsedLE.camera, "Canon EOS R5");
  assert.equal(parsedLE.lens, "RF50mm F1.2 L USM");
  assert.equal(parsedLE.aperture, "f/1.8");
  assert.equal(parsedLE.shutterSpeed, "1/250s");
  assert.equal(parsedLE.iso, "ISO 400");
  assert.equal(parsedLE.dateTaken, "2026-11-18");

  // 2. Big-Endian JPEG APP1 EXIF parsing
  const jpegBE = wrapAsJpeg(buildTestTiffBlock(false));
  const parsedBE = exifMod.parseExifFromArrayBuffer(jpegBE);
  assert.equal(parsedBE.hasExif, true);
  assert.equal(parsedBE.camera, "Canon EOS R5");
  assert.equal(parsedBE.aperture, "f/1.8");
  assert.equal(parsedBE.shutterSpeed, "1/250s");
  assert.equal(parsedBE.iso, "ISO 400");
  assert.equal(parsedBE.dateTaken, "2026-11-18");

  // 3. PNG eXIf chunk parsing
  const pngBytes = wrapAsPng(buildTestTiffBlock(true));
  const parsedPng = exifMod.parseExifFromArrayBuffer(pngBytes);
  assert.equal(parsedPng.hasExif, true);
  assert.equal(parsedPng.iso, "ISO 400");
  assert.equal(parsedPng.aperture, "f/1.8");

  // 4. Direct Data URL EXIF reading (used by lightbox auto-reader)
  const dataUrl = `data:image/jpeg;base64,${Buffer.from(jpegLE).toString("base64")}`;
  const parsedFromDataUrl = await exifMod.readExifFromImageUrl(dataUrl);
  assert.equal(parsedFromDataUrl.hasExif, true);
  assert.equal(parsedFromDataUrl.camera, "Canon EOS R5");
  assert.equal(parsedFromDataUrl.iso, "ISO 400");

  // 5. File reader helper (used before canvas compression during upload)
  const mockFile = {
    name: "studio_bridal.jpg",
    lastModified: Date.UTC(2026, 10, 19),
    slice: () => ({
      arrayBuffer: async () => jpegLE.buffer.slice(jpegLE.byteOffset, jpegLE.byteOffset + jpegLE.byteLength)
    })
  };
  const parsedFromFile = await exifMod.readExifFromFile(mockFile);
  assert.equal(parsedFromFile.camera, "Canon EOS R5");
  assert.equal(parsedFromFile.dateTaken, "2026-11-18");

  // 6. Non-EXIF buffer gracefully returns hasExif: false
  const nonExif = exifMod.parseExifFromArrayBuffer(new Uint8Array([0x00, 0x01, 0x02, 0x03]));
  assert.equal(nonExif.hasExif, false);

  // 7. Lightbox Auto-EXIF badge rendering & CSS verification
  const autoHtml = shareMod.renderLightboxMetadataHtml(
    { id: "uploaded_1", title: "Bridal Shot", metadata: parsedLE },
    {
      lang: "en",
      t: (k) => i18nMod.TRANSLATIONS.en[k] || k,
      autoExtracted: true
    }
  );
  assert.ok(autoHtml.includes("lightbox-exif-badge is-auto"));
  assert.ok(autoHtml.includes("Auto-EXIF"));
  assert.ok(autoHtml.includes("Canon EOS R5"));
  assert.ok(autoHtml.includes("ISO 400"));

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  assert.ok(css.includes(".lightbox-exif-badge"), "CSS missing .lightbox-exif-badge rule");
});

test("Gallery lightbox #lightboxImg supports mobile pinch-to-zoom, clamped pan, double-tap toggle, and zoom controls", async () => {
  const shareMod = await import(pathToFileURL(path.join(webDir, "js/gallery-share.js")).href);

  // 1. Touch distance & midpoint geometry
  const pA = { clientX: 100, clientY: 200 };
  const pB = { clientX: 160, clientY: 280 };
  assert.equal(shareMod.getTouchDistance(pA, pB), 100);
  assert.deepEqual(shareMod.getTouchMidpoint(pA, pB), { x: 130, y: 240 });

  // 2. Scale & pan clamping
  assert.equal(shareMod.clampZoomScale(0.4), shareMod.LIGHTBOX_MIN_ZOOM);
  assert.equal(shareMod.clampZoomScale(8.5), shareMod.LIGHTBOX_MAX_ZOOM);
  assert.equal(shareMod.clampZoomScale(2.5), 2.5);
  assert.deepEqual(shareMod.clampZoomPan(90, -50, 1.0, 400, 300), { x: 0, y: 0 });
  // At 2x in 400x300 container: maxX = 200, maxY = 150
  assert.deepEqual(shareMod.clampZoomPan(320, -240, 2.0, 400, 300), { x: 200, y: -150 });

  // 3. DOM transform application on #lightboxImg
  const attrs = {};
  const classes = new Set();
  const fakeImg = {
    style: { transform: "" },
    setAttribute: (k, v) => {
      attrs[k] = v;
    },
    classList: {
      toggle: (cls, force) => {
        if (force) classes.add(cls);
        else classes.delete(cls);
      }
    }
  };

  const zoomedSnap = shareMod.applyLightboxZoomTransform(fakeImg, { scale: 2.5, x: 42.5, y: -18 });
  assert.equal(zoomedSnap.isZoomed, true);
  assert.ok(fakeImg.style.transform.includes("translate3d(42.5px, -18.0px, 0)"));
  assert.ok(fakeImg.style.transform.includes("scale(2.500)"));
  assert.equal(attrs["data-zoom-scale"], "2.50");
  assert.equal(classes.has("is-zoomed"), true);

  const resetSnap = shareMod.applyLightboxZoomTransform(fakeImg, { scale: 1, x: 30, y: 20 });
  assert.equal(resetSnap.isZoomed, false);
  assert.equal(fakeImg.style.transform, "translate3d(0px, 0px, 0) scale(1)");
  assert.equal(attrs["data-zoom-scale"], "1.00");
  assert.equal(classes.has("is-zoomed"), false);

  // 4. Stateful pinch-to-zoom controller lifecycle (two-finger pinch, clamped pan, snap-reset, double-tap, +/- buttons)
  const history = [];
  const zoom = shareMod.createLightboxZoomController({
    onChange: (snap) => history.push(snap)
  });

  assert.equal(zoom.isZoomed(), false);
  assert.equal(zoom.startPan(100, 100), false, "Pan must remain disabled when unzoomed");

  // Start two-finger pinch at 100px separation -> spread to 250px (2.5x)
  zoom.startPinch({ clientX: 100, clientY: 200 }, { clientX: 200, clientY: 200 });
  const pinched = zoom.movePinch({ clientX: 25, clientY: 200 }, { clientX: 275, clientY: 200 }, 400, 300);
  assert.equal(pinched.scale, 2.5);
  assert.equal(pinched.isZoomed, true);

  // Single-finger pan while zoomed in, clamped to container bounds (at 2.5x in 400x300 -> maxX=300, maxY=225)
  assert.equal(zoom.startPan(150, 150), true);
  const panned = zoom.movePan(650, -300, 400, 300);
  assert.equal(panned.x, 300);
  assert.equal(panned.y, -225);
  zoom.endPan();

  // Pinching back down near 1x snaps cleanly to 1.0x centered on endPinch
  zoom.startPinch({ clientX: 0, clientY: 200 }, { clientX: 250, clientY: 200 });
  zoom.movePinch({ clientX: 75, clientY: 200 }, { clientX: 177, clientY: 200 }, 400, 300);
  const snapped = zoom.endPinch();
  assert.equal(snapped.scale, 1);
  assert.equal(snapped.x, 0);
  assert.equal(snapped.y, 0);
  assert.equal(snapped.isZoomed, false);

  // Double-tap toggles 1x -> 2.25x -> 1x
  const dtIn = zoom.toggleDoubleTap(400, 300);
  assert.equal(dtIn.scale, shareMod.LIGHTBOX_DOUBLE_TAP_ZOOM);
  assert.equal(dtIn.isZoomed, true);
  const dtOut = zoom.toggleDoubleTap(400, 300);
  assert.equal(dtOut.scale, 1);
  assert.equal(dtOut.isZoomed, false);

  // Stepper buttons zoomIn / zoomOut / reset
  assert.equal(zoom.zoomIn(400, 300).scale, 1.5);
  assert.equal(zoom.zoomIn(400, 300).scale, 2.0);
  assert.equal(zoom.zoomOut(400, 300).scale, 1.5);
  assert.equal(zoom.reset().scale, 1);
  assert.ok(history.length >= 6);

  // 5. HTML, JS wiring, and CSS verification
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  for (const token of [
    'id="lightboxImg"',
    'data-zoom-scale="1.00"',
    'id="lightboxZoomOutBtn"',
    'id="lightboxZoomResetBtn"',
    'id="lightboxZoomInBtn"'
  ]) {
    assert.ok(html.includes(token), `Lightbox HTML missing pinch-to-zoom token: ${token}`);
  }

  const appJs = fs.readFileSync(path.join(webDir, "js/app.js"), "utf8");
  for (const token of [
    "createLightboxZoomController",
    "applyLightboxZoomTransform",
    "startPinch",
    "movePinch",
    "toggleDoubleTap"
  ]) {
    assert.ok(appJs.includes(token), `app.js missing pinch-to-zoom wiring: ${token}`);
  }

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    "touch-action: none",
    ".lightbox-image.is-zoomed",
    ".lightbox-image.is-pinching",
    ".lightbox-zoom-controls",
    ".lightbox-zoom-btn",
    ".lightbox-zoom-level"
  ]) {
    assert.ok(css.includes(selector), `CSS missing pinch-to-zoom rule for ${selector}`);
  }
});

test("Gallery lightbox 'Download All' packages all active folder images into a valid single ZIP archive", async () => {
  const zipMod = await import(pathToFileURL(path.join(webDir, "js/zip-builder.js")).href);
  const shareMod = await import(pathToFileURL(path.join(webDir, "js/gallery-share.js")).href);

  // 1. Standard IEEE 802.3 CRC-32 check vector ("123456789" === 0xCBF43926)
  const checkBytes = new TextEncoder().encode("123456789");
  assert.equal(zipMod.crc32(checkBytes), 0xcbf43926);

  // 2. Path-traversal sanitization & case-insensitive deduplication
  assert.equal(zipMod.sanitizeZipEntryName("../../secret/bridal:01?.jpg"), "bridal01.jpg");
  const used = new Set();
  assert.equal(zipMod.makeUniqueZipEntryName("bridal.jpg", used), "bridal.jpg");
  assert.equal(zipMod.makeUniqueZipEntryName("Bridal.JPG", used), "Bridal-2.JPG");
  assert.equal(zipMod.makeUniqueZipEntryName("bridal.jpg", used), "bridal-3.jpg");

  // 3. Build ZIP directly and inspect PKZIP Local Headers, Central Directory, and EOCD
  const imgA = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 0x01, 0x02, 0xff, 0xd9]);
  const imgB = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  const rawZip = zipMod.buildZipArchive([
    { name: "editorial.jpg", data: imgA },
    { name: "editorial.jpg", data: imgB }
  ]);
  const dv = new DataView(rawZip.buffer, rawZip.byteOffset, rawZip.byteLength);
  // Local header #1 signature
  assert.equal(dv.getUint32(0, true), 0x04034b50);
  // EOCD record at end (last 22 bytes)
  const eocdOffset = rawZip.byteLength - 22;
  assert.equal(dv.getUint32(eocdOffset, true), 0x06054b50);
  assert.equal(dv.getUint16(eocdOffset + 8, true), 2); // 2 entries on disk
  assert.equal(dv.getUint16(eocdOffset + 10, true), 2); // 2 total entries
  const cdOffset = dv.getUint32(eocdOffset + 16, true);
  assert.equal(dv.getUint32(cdOffset, true), 0x02014b50); // Central directory signature

  // 4. Folder filename formatting & Data URL decoding
  assert.equal(shareMod.buildFolderZipFileName("Bridal"), "star-artist-studio-bridal-gallery.zip");
  assert.equal(shareMod.buildFolderZipFileName("All"), "star-artist-studio-all-gallery.zip");
  const pngDataUrl = `data:image/png;base64,${Buffer.from(imgB).toString("base64")}`;
  const decoded = shareMod.decodeDataUrlToBytes(pngDataUrl);
  assert.equal(decoded.mime, "image/png");
  assert.deepEqual(Array.from(decoded.bytes), Array.from(imgB));

  // 5. End-to-end downloadGalleryFolderAsZip (HTTP fetch + Data URL + progress callback + anchor download)
  const createdAnchors = [];
  let createdBlob = null;
  const mockDoc = {
    body: { appendChild: () => {}, removeChild: () => {} },
    createElement: (tag) => {
      const el = {
        tag,
        href: "",
        download: "",
        style: {},
        clicked: false,
        click() {
          this.clicked = true;
        },
        remove() {}
      };
      createdAnchors.push(el);
      return el;
    }
  };
  class MockBlob {
    constructor(parts, opts) {
      this.parts = parts;
      this.type = opts?.type || "";
    }
  }
  const progressEvents = [];
  const folderItems = [
    { id: "g1", title: "Royal Bridal Portrait", category: "Bridal", mediaUrl: "/images/gallery-bridal.jpg" },
    { id: "g2", title: "Royal Bridal Portrait", category: "Bridal", mediaUrl: pngDataUrl }
  ];

  const zipRes = await shareMod.downloadGalleryFolderAsZip(folderItems, {
    folderName: "Bridal",
    doc: mockDoc,
    BlobCtor: MockBlob,
    fetchImpl: async () => ({
      ok: true,
      headers: { get: () => "image/jpeg" },
      arrayBuffer: async () => imgA.buffer
    }),
    urlApi: {
      createObjectURL: (blob) => {
        createdBlob = blob;
        return "blob:star-artist/bridal-gallery-zip";
      },
      revokeObjectURL: () => {}
    },
    baseUrl: "https://gen-lang-client-0548683163.web.app/",
    onProgress: (ev) => progressEvents.push(ev)
  });

  assert.equal(zipRes.status, "downloaded-zip");
  assert.equal(zipRes.count, 2);
  assert.equal(zipRes.total, 2);
  assert.equal(zipRes.failedCount, 0);
  assert.equal(zipRes.fileName, "star-artist-studio-bridal-gallery.zip");
  assert.equal(progressEvents.length, 2);
  assert.equal(createdBlob.type, "application/zip");
  assert.equal(createdAnchors[0].href, "blob:star-artist/bridal-gallery-zip");
  assert.equal(createdAnchors[0].download, "star-artist-studio-bridal-gallery.zip");
  assert.equal(createdAnchors[0].clicked, true);

  // Verify partial success when one remote item fails CORS/network
  const partialRes = await shareMod.downloadGalleryFolderAsZip(folderItems, {
    folderName: "Bridal",
    doc: mockDoc,
    BlobCtor: MockBlob,
    fetchImpl: async () => {
      throw new Error("CORS blocked");
    },
    urlApi: {
      createObjectURL: () => "blob:star-artist/partial-zip",
      revokeObjectURL: () => {}
    },
    baseUrl: "https://gen-lang-client-0548683163.web.app/"
  });
  assert.equal(partialRes.status, "downloaded-partial");
  assert.equal(partialRes.count, 1);
  assert.equal(partialRes.failedCount, 1);

  // 6. HTML, JS wiring, and CSS verification
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  for (const token of [
    'id="lightboxDownloadAllBtn"',
    'id="lightboxDownloadAllLabel"',
    'id="lightboxDownloadAllCount"'
  ]) {
    assert.ok(html.includes(token), `Lightbox HTML missing Download All ZIP token: ${token}`);
  }

  const appJs = fs.readFileSync(path.join(webDir, "js/app.js"), "utf8");
  assert.ok(appJs.includes("downloadGalleryFolderAsZip"), "app.js must wire downloadGalleryFolderAsZip");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".lightbox-download-all-btn",
    ".lightbox-download-all-btn.is-packing",
    ".lightbox-count-pill"
  ]) {
    assert.ok(css.includes(selector), `CSS missing Download All ZIP rule for ${selector}`);
  }
});

test("Gallery upload dropzone progress bar updates in real-time during Firebase Storage resumable uploads", async () => {
  const uploadMod = await import(pathToFileURL(path.join(webDir, "js/image-upload.js")).href);
  const firebaseMod = await import(pathToFileURL(path.join(webDir, "js/firebase.js")).href);

  // 1. Byte formatting & progress percentage math
  assert.equal(uploadMod.formatUploadBytes(0), "0 B");
  assert.equal(uploadMod.formatUploadBytes(512), "512 B");
  assert.equal(uploadMod.formatUploadBytes(196608), "192 KB");
  assert.equal(uploadMod.formatUploadBytes(2.5 * 1024 * 1024), "2.5 MB");
  assert.equal(uploadMod.computeUploadProgress(0, 0), 0);
  assert.equal(uploadMod.computeUploadProgress(150, 300), 50);
  assert.equal(uploadMod.computeUploadProgress(450, 300), 100);

  // 2. Sanitized Firebase Storage object path (no path traversal)
  const storagePath = uploadMod.buildStorageObjectPath("uid_123", "../../Bridal Shoot #1?.jpg", 1700000000000);
  assert.equal(storagePath, "gallery_uploads/uid_123/1700000000000-Bridal-Shoot-1-.jpg");

  // 3. Progress bar DOM controller updates width, ARIA attributes, byte text, and states
  const makeClassList = () => {
    const set = new Set(["hidden"]);
    return {
      set,
      toggle(cls, force) {
        if (force) set.add(cls);
        else set.delete(cls);
      }
    };
  };
  const dropzoneClasses = makeClassList();
  dropzoneClasses.set.delete("hidden");
  const wrapClasses = makeClassList();
  const barAttrs = {};
  const fillEl = { style: { width: "0%" } };
  const labelEl = { textContent: "" };
  const pctEl = { textContent: "" };
  const bytesEl = { textContent: "" };

  const ctrl = uploadMod.createUploadProgressController({
    dropzoneEl: { classList: dropzoneClasses },
    wrapEl: { classList: wrapClasses },
    barEl: { setAttribute: (k, v) => (barAttrs[k] = v) },
    fillEl,
    labelEl,
    pctEl,
    bytesEl
  });

  ctrl.update({
    percent: 42,
    bytesTransferred: 210 * 1024,
    totalBytes: 500 * 1024,
    label: "Uploading to Firebase Storage...",
    status: "uploading"
  });
  assert.equal(wrapClasses.set.has("hidden"), false);
  assert.equal(wrapClasses.set.has("is-uploading"), true);
  assert.equal(dropzoneClasses.set.has("is-uploading"), true);
  assert.equal(fillEl.style.width, "42%");
  assert.equal(barAttrs["aria-valuenow"], "42");
  assert.equal(pctEl.textContent, "42%");
  assert.equal(bytesEl.textContent, "210 KB / 500 KB");

  ctrl.complete("Upload complete!");
  assert.equal(fillEl.style.width, "100%");
  assert.equal(barAttrs["aria-valuenow"], "100");
  assert.equal(wrapClasses.set.has("is-complete"), true);
  assert.equal(dropzoneClasses.set.has("is-uploading"), false);

  // 4. Real-time Firebase Storage resumable upload streams state_changed snapshots into the progress bar
  const streamedEvents = [];
  const fakeFile = { name: "studio_bridal.jpg", type: "image/jpeg", size: 400000 };
  const mockStorageAdapter = {
    uid: "artist_42",
    ref: (p) => ({ fullPath: p }),
    uploadBytesResumable: (refObj) => ({
      snapshot: { ref: refObj, totalBytes: 400000 },
      on(_event, nextFn, _errFn, completeFn) {
        nextFn({ bytesTransferred: 100000, totalBytes: 400000, state: "running" });
        nextFn({ bytesTransferred: 280000, totalBytes: 400000, state: "running" });
        nextFn({ bytesTransferred: 400000, totalBytes: 400000, state: "running" });
        completeFn();
      }
    }),
    getDownloadURL: async (refObj) =>
      `https://firebasestorage.googleapis.com/v0/b/gen-lang-client-0548683163.appspot.com/o/${encodeURIComponent(refObj.fullPath)}?alt=media`
  };

  const uploadRes = await firebaseMod.uploadGalleryImageToStorage(fakeFile, {
    storageAdapter: mockStorageAdapter,
    onProgress: (ev) => {
      streamedEvents.push(ev.percent);
      ctrl.update({
        percent: ev.percent,
        bytesTransferred: ev.bytesTransferred,
        totalBytes: ev.totalBytes,
        label: "Uploading to Firebase Storage...",
        status: "uploading"
      });
    }
  });

  assert.deepEqual(streamedEvents, [0, 25, 70, 100, 100]);
  assert.ok(uploadRes.downloadUrl.startsWith("https://firebasestorage.googleapis.com/"));
  assert.ok(uploadRes.storagePath.startsWith("gallery_uploads/artist_42/"));
  assert.equal(fillEl.style.width, "100%");
  assert.equal(barAttrs["aria-valuenow"], "100");

  // 5. HTML, Service Worker bypass, JS wiring, and CSS verification
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  for (const token of [
    'id="galleryUploadDropzone"',
    'id="galleryUploadProgressWrap"',
    'id="galleryUploadProgressBar"',
    'role="progressbar"',
    'id="galleryUploadProgressFill"',
    'id="galleryUploadProgressPct"',
    'id="galleryUploadProgressBytes"'
  ]) {
    assert.ok(html.includes(token), `index.html missing upload progress bar token: ${token}`);
  }

  const swJs = fs.readFileSync(path.join(webDir, "sw.js"), "utf8");
  assert.ok(swJs.includes("firebasestorage.googleapis.com"), "sw.js must bypass firebasestorage.googleapis.com");

  const appJs = fs.readFileSync(path.join(webDir, "js/app.js"), "utf8");
  for (const token of ["createUploadProgressController", "uploadGalleryImageToStorage"]) {
    assert.ok(appJs.includes(token), `app.js missing upload progress wiring: ${token}`);
  }

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".gallery-upload-dropzone.is-uploading",
    ".gallery-upload-progress",
    ".gallery-upload-progress-track",
    ".gallery-upload-progress-fill",
    ".gallery-upload-progress.is-complete",
    ".gallery-upload-progress.is-error"
  ]) {
    assert.ok(css.includes(selector), `CSS missing upload progress rule for ${selector}`);
  }
});

test("Admin Dashboard is restricted exclusively to verified chanderstudio0@gmail.com, hidden from visitors, and protected against direct URL access", async () => {
  const guardMod = await import(pathToFileURL(path.join(webDir, "js/admin-guard.js")).href);

  // 1. Strict single-Gmail authorization check (case-insensitive, emailVerified required)
  assert.equal(guardMod.AUTHORIZED_ADMIN_EMAIL, "chanderstudio0@gmail.com");
  assert.equal(guardMod.isAuthorizedAdminUser(null), false);
  assert.equal(guardMod.isAuthorizedAdminUser(undefined), false);
  assert.equal(guardMod.isAuthorizedAdminUser({ email: "chanderstudio0@gmail.com", emailVerified: false }), false);
  assert.equal(guardMod.isAuthorizedAdminUser({ email: "visitor@gmail.com", emailVerified: true }), false);
  assert.equal(guardMod.isAuthorizedAdminUser({ email: "chanderstudio0@gmail.com.evil.com", emailVerified: true }), false);
  assert.equal(guardMod.isAuthorizedAdminUser({ email: "chanderstudio0@gmail.com", emailVerified: true }), true);
  assert.equal(guardMod.isAuthorizedAdminUser({ email: "  CHANDERSTUDIO0@GMAIL.COM  ", emailVerified: true }), true);

  // 2. Direct admin URL detection and route resolution for unauthorized vs authorized users
  const adminUrlAttempts = [
    { pathname: "/admin", hash: "", search: "" },
    { pathname: "/admin/", hash: "", search: "" },
    { pathname: "/admin/dashboard", hash: "", search: "" },
    { pathname: "/admin.html", hash: "", search: "" },
    { pathname: "/", hash: "#admin", search: "" },
    { pathname: "/", hash: "#/admin", search: "" },
    { pathname: "/", hash: "#admin/bookings", search: "" },
    { pathname: "/", hash: "", search: "?route=admin" },
    { pathname: "/", hash: "", search: "?page=admin" },
    { pathname: "/", hash: "", search: "?admin=1" }
  ];

  for (const loc of adminUrlAttempts) {
    assert.equal(guardMod.isAdminUrlAttempt(loc), true, `Expected admin attempt for ${JSON.stringify(loc)}`);
    assert.equal(guardMod.extractRequestedRoute(loc), "admin");

    const blocked = guardMod.resolveSafeRoute({ ...loc, isAdmin: false });
    assert.equal(blocked.route, "home");
    assert.equal(blocked.blockedAdminAttempt, true);
    assert.equal(blocked.shouldNormalizeUrl, true);

    const allowed = guardMod.resolveSafeRoute({ ...loc, isAdmin: true });
    assert.equal(allowed.route, "admin");
    assert.equal(allowed.blockedAdminAttempt, false);
  }

  // Non-admin public routes remain unaffected
  assert.equal(guardMod.isAdminUrlAttempt({ pathname: "/", hash: "#gallery", search: "" }), false);
  assert.deepEqual(guardMod.resolveSafeRoute({ requestedRoute: "gallery", isAdmin: false }), {
    route: "gallery",
    blockedAdminAttempt: false,
    shouldNormalizeUrl: false
  });

  // 3. DOM visibility sync hides all admin buttons/views and wipes sensitive DOM nodes when unauthorized
  const makeFakeEl = (id, initialText = "") => {
    const classes = new Set();
    const attrs = {};
    return {
      id,
      hidden: false,
      innerHTML: initialText,
      textContent: initialText,
      classList: {
        toggle: (cls, force) => {
          if (force) classes.add(cls);
          else classes.delete(cls);
        },
        has: (cls) => classes.has(cls)
      },
      setAttribute: (k, v) => {
        attrs[k] = v;
      },
      removeAttribute: (k) => {
        delete attrs[k];
      },
      attrs
    };
  };

  const els = {
    desktopAdminNavBtn: makeFakeEl("desktopAdminNavBtn"),
    openAdminConsoleQuickBtn: makeFakeEl("openAdminConsoleQuickBtn"),
    mobileAdminNavBtn: makeFakeEl("mobileAdminNavBtn"),
    "view-admin": makeFakeEl("view-admin"),
    adminLockedPanel: makeFakeEl("adminLockedPanel"),
    adminConsoleContent: makeFakeEl("adminConsoleContent"),
    adminStatsGrid: makeFakeEl("adminStatsGrid", "<div>Secret Stats</div>"),
    adminTabBody: makeFakeEl("adminTabBody", "<div>Client Phone Numbers</div>"),
    adminVerifiedEmailBadge: makeFakeEl("adminVerifiedEmailBadge", "chanderstudio0@gmail.com")
  };
  const fakeDoc = { getElementById: (id) => els[id] || null };

  // Unauthorized visitor: everything hidden & wiped
  guardMod.syncAdminUiVisibility(fakeDoc, { isAdmin: false, adminEmail: "" });
  for (const id of [
    "desktopAdminNavBtn",
    "openAdminConsoleQuickBtn",
    "mobileAdminNavBtn",
    "view-admin",
    "adminConsoleContent"
  ]) {
    assert.equal(els[id].hidden, true, `${id} must be hidden for visitors`);
    assert.equal(els[id].classList.has("hidden"), true, `${id} must have .hidden class for visitors`);
    assert.equal(els[id].attrs["aria-hidden"], "true", `${id} must have aria-hidden="true"`);
  }
  assert.equal(els.adminStatsGrid.innerHTML, "", "adminStatsGrid must be wiped on unauthorized state");
  assert.equal(els.adminTabBody.innerHTML, "", "adminTabBody must be wiped on unauthorized state");
  assert.equal(els.adminVerifiedEmailBadge.textContent, "");

  // Verified admin: admin navigation links and console content become visible (view-admin shown when navigated)
  guardMod.syncAdminUiVisibility(fakeDoc, { isAdmin: true, adminEmail: "chanderstudio0@gmail.com" });
  for (const id of [
    "desktopAdminNavBtn",
    "openAdminConsoleQuickBtn",
    "mobileAdminNavBtn",
    "adminConsoleContent"
  ]) {
    assert.equal(els[id].hidden, false, `${id} must be unhidden for verified admin`);
    assert.equal(els[id].classList.has("hidden"), false, `${id} must remove .hidden for verified admin`);
  }
  assert.equal(els.adminLockedPanel.hidden, true);
  assert.equal(els.adminVerifiedEmailBadge.textContent, "chanderstudio0@gmail.com");

  // 4. Static HTML ships with all admin controls & view hidden by default
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  for (const id of [
    "desktopAdminNavBtn",
    "openAdminConsoleQuickBtn",
    "mobileAdminNavBtn",
    "view-admin",
    "adminConsoleContent"
  ]) {
    const tagMatch = html.match(new RegExp(`<[^>]+id="${id}"[^>]*>`));
    assert.ok(tagMatch, `index.html missing #${id}`);
    assert.ok(/\bhidden\b/.test(tagMatch[0]), `#${id} must include HTML hidden attribute by default`);
    assert.ok(tagMatch[0].includes('aria-hidden="true"'), `#${id} must include aria-hidden="true" by default`);
  }

  // 5. Firebase client layer guards every admin action with requireAuthorizedAdmin
  const firebaseJs = fs.readFileSync(path.join(webDir, "js/firebase.js"), "utf8");
  assert.ok(firebaseJs.includes("isAuthorizedAdminUser"), "firebase.js must enforce isAuthorizedAdminUser");
  assert.ok(firebaseJs.includes("requireAuthorizedAdmin"), "firebase.js must guard admin operations");
  assert.ok(!firebaseJs.includes('doc(ctx.db, "admins"'), "firebase.js must not allow /admins doc fallback");

  // 6. Firestore Security Rules strictly enforce verified chanderstudio0@gmail.com only
  const rulesPath = path.resolve(webDir, "..", "firestore.rules");
  const rules = fs.readFileSync(rulesPath, "utf8");
  assert.ok(
    rules.includes("request.auth.token.email.lower() == 'chanderstudio0@gmail.com'"),
    "firestore.rules must enforce chanderstudio0@gmail.com"
  );
  assert.ok(
    rules.includes("request.auth.token.email_verified == true"),
    "firestore.rules must require email_verified == true"
  );
  assert.ok(
    !rules.includes("exists(/databases/$(database)/documents/admins/$(request.auth.uid))"),
    "firestore.rules must not permit secondary /admins UID bypass"
  );

  // 7. Zero hardcoded admin passwords or secret keys in web frontend files
  const jsDir = path.join(webDir, "js");
  for (const fname of fs.readdirSync(jsDir).filter((f) => f.endsWith(".js"))) {
    const src = fs.readFileSync(path.join(jsDir, fname), "utf8");
    assert.equal(/adminPassword\s*[:=]\s*["'][^"']+["']/i.test(src), false, `Hardcoded admin password in ${fname}`);
    assert.equal(/-----BEGIN PRIVATE KEY-----/.test(src), false, `Private key found in ${fname}`);
  }
});

test("Admin Dashboard 'Model Growth' Recharts chart aggregates 30-day applications, computes outreach KPIs, mounts Area/Bar charts, and handles offline fallback", async () => {
  const growthMod = await import(pathToFileURL(path.join(webDir, "js/admin-model-growth.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);

  // 1. Date normalization across Firestore Timestamp, serialized seconds, ISO strings, and epoch ms
  const anchor = new Date("2026-11-21T12:00:00.000Z");
  assert.equal(growthMod.normalizeApplicationDate({ toDate: () => anchor }).toISOString(), anchor.toISOString());
  assert.equal(
    growthMod.normalizeApplicationDate({ seconds: Math.floor(anchor.getTime() / 1000), nanoseconds: 0 }).toISOString(),
    anchor.toISOString()
  );
  assert.equal(growthMod.normalizeApplicationDate("2026-11-20T09:30:00.000Z").getUTCDate(), 20);
  assert.equal(growthMod.normalizeApplicationDate("invalid-date"), null);

  // 2. 30-day daily series aggregation (ignores records older than 30 days)
  const sampleModels = [
    { id: "m1", status: "approved", createdAt: { toDate: () => new Date("2026-11-21T08:00:00.000Z") } },
    { id: "m2", status: "pending", createdAt: "2026-11-21T14:00:00.000Z" },
    { id: "m3", status: "approved", createdAt: "2026-11-21T19:00:00.000Z" },
    { id: "m4", status: "pending", createdAt: "2026-11-18T10:00:00.000Z" },
    { id: "m5", status: "rejected", createdAt: "2026-11-10T10:00:00.000Z" },
    { id: "m6", status: "approved", createdAt: "2026-11-09T10:00:00.000Z" },
    { id: "m_old", status: "approved", createdAt: "2026-09-01T10:00:00.000Z" }
  ];

  const series = growthMod.buildModelGrowthSeries(sampleModels, {
    days: 30,
    now: anchor,
    lang: "en",
    utc: true
  });
  assert.equal(series.length, 30, "Model Growth series must cover exactly 30 consecutive days");
  assert.equal(series[0].dateKey, "2026-10-23");
  assert.equal(series[29].dateKey, "2026-11-21");
  assert.equal(series[29].applications, 3);
  assert.equal(series[29].approved, 2);
  assert.equal(series[29].pending, 1);
  assert.equal(series[29].cumulative, 6, "30-day cumulative must exclude older model m_old");

  // 3. Outreach KPI summary (last 7 days = 4 vs prev 7 days = 2 => +100% up trend)
  const summary = growthMod.summarizeModelGrowth(series, sampleModels);
  assert.equal(summary.total30Days, 6);
  assert.equal(summary.approved30Days, 3);
  assert.equal(summary.last7Days, 4);
  assert.equal(summary.prev7Days, 2);
  assert.equal(summary.trendPercent, 100);
  assert.equal(summary.trendDirection, "up");
  assert.equal(summary.peakDay.dateKey, "2026-11-21");
  assert.equal(summary.peakDay.applications, 3);
  assert.equal(summary.dailyAverage, 0.2);
  assert.equal(summary.approvalRate30Days, 50);
  assert.equal(summary.allTimeTotal, 7);

  // 4. Recharts runtime mounting (AreaChart + BarChart), root reuse, and clean unmount
  const renderedTrees = [];
  let rootCreateCount = 0;
  let unmountCount = 0;
  const fakeReact = {
    createElement: (type, props, ...children) => ({
      type: typeof type === "function" ? type.name : type,
      props: props || {},
      children
    })
  };
  const fakeRecharts = {
    ResponsiveContainer: function ResponsiveContainer() {},
    AreaChart: function AreaChart() {},
    Area: function Area() {},
    BarChart: function BarChart() {},
    Bar: function Bar() {},
    CartesianGrid: function CartesianGrid() {},
    XAxis: function XAxis() {},
    YAxis: function YAxis() {},
    Tooltip: function Tooltip() {}
  };
  const fakeLoaders = {
    react: async () => fakeReact,
    reactDomClient: async () => ({
      createRoot: () => {
        rootCreateCount += 1;
        return {
          render: (tree) => renderedTrees.push(tree),
          unmount: () => {
            unmountCount += 1;
          }
        };
      }
    }),
    recharts: async () => fakeRecharts
  };

  const chartHost = { innerHTML: "" };
  const metricsEl = { innerHTML: "" };
  const statusEl = { textContent: "" };
  const tEn = (k) => i18nMod.TRANSLATIONS.en[k] || k;

  const areaResult = await growthMod.mountModelGrowthChart(chartHost, {
    models: sampleModels,
    metricsEl,
    statusEl,
    chartType: "area",
    theme: "dark",
    lang: "en",
    now: anchor,
    t: tEn,
    loaders: fakeLoaders
  });
  assert.equal(areaResult.engine, "recharts");
  assert.equal(rootCreateCount, 1);
  assert.equal(renderedTrees[0].type, "ResponsiveContainer");
  assert.equal(renderedTrees[0].children[0].type, "AreaChart");
  assert.equal(renderedTrees[0].children[0].props.data.length, 30);
  assert.ok(metricsEl.innerHTML.includes("+100%"));
  assert.ok(statusEl.textContent.includes("Recharts"));

  // Switch to BarChart reuses the existing React root without remount leak
  const barResult = await growthMod.mountModelGrowthChart(chartHost, {
    models: sampleModels,
    metricsEl,
    statusEl,
    chartType: "bar",
    theme: "light",
    lang: "en",
    now: anchor,
    t: tEn,
    loaders: fakeLoaders
  });
  assert.equal(barResult.engine, "recharts");
  assert.equal(rootCreateCount, 1, "Existing React root must be reused on chart updates");
  assert.equal(renderedTrees[1].children[0].type, "BarChart");

  // Unmount on admin sign-out clears chart host cleanly
  growthMod.unmountModelGrowthChart(chartHost);
  assert.equal(unmountCount, 1);
  assert.equal(chartHost.innerHTML, "");

  // 5. Offline fallback renders accessible vector SVG when Recharts CDN is unreachable
  const offlineHost = { innerHTML: "" };
  const offlineStatus = { textContent: "" };
  const fallbackRes = await growthMod.mountModelGrowthChart(offlineHost, {
    models: sampleModels,
    statusEl: offlineStatus,
    now: anchor,
    t: tEn,
    loaders: {
      react: async () => {
        throw new Error("Offline");
      },
      reactDomClient: async () => {
        throw new Error("Offline");
      },
      recharts: async () => {
        throw new Error("Offline");
      }
    }
  });
  assert.equal(fallbackRes.engine, "svg-fallback");
  assert.ok(offlineHost.innerHTML.includes("<svg"));
  assert.ok(offlineHost.innerHTML.includes("model-growth-fallback-svg"));

  // 6. HTML, SW, JS wiring, and CSS checks
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  for (const token of [
    'id="adminModelGrowthCard"',
    'id="adminModelGrowthMetrics"',
    'id="adminModelGrowthChartMount"',
    'data-growth-chart-type="area"',
    'data-growth-chart-type="bar"'
  ]) {
    assert.ok(html.includes(token), `index.html missing Model Growth token: ${token}`);
  }
  assert.ok(
    html.indexOf('id="adminConsoleContent"') < html.indexOf('id="adminModelGrowthCard"'),
    "Model Growth card must live strictly inside the gated #adminConsoleContent container"
  );

  const swJs = fs.readFileSync(path.join(webDir, "sw.js"), "utf8");
  assert.ok(swJs.includes("/js/admin-model-growth.js"), "sw.js must pre-cache admin-model-growth.js");
  assert.ok(swJs.includes("esm.sh"), "sw.js must bypass esm.sh for Recharts ESM");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".model-growth-card",
    ".model-growth-kpis",
    ".model-growth-trend.is-up",
    ".model-growth-chart-host",
    ".model-growth-fallback-svg"
  ]) {
    assert.ok(css.includes(selector), `styles.css missing Model Growth rule for ${selector}`);
  }
});

test("Admin Dashboard bookings CSV export formats RFC 4180 UTF-8 CSV, neutralizes spreadsheet formulas, and downloads cleanly", async () => {
  const csvMod = await import(pathToFileURL(path.join(webDir, "js/admin-bookings-export.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);

  // 1. RFC 4180 quoting & formula-injection neutralization
  assert.equal(csvMod.sanitizeCsvCell("Simple Name"), "Simple Name");
  assert.equal(csvMod.sanitizeCsvCell('Sirsa, "Haryana"'), '"Sirsa, ""Haryana"""');
  assert.equal(csvMod.sanitizeCsvCell("Line 1\nLine 2"), '"Line 1\nLine 2"');
  assert.equal(csvMod.sanitizeCsvCell("=CMD|' /C calc'!A0"), "'=CMD|' /C calc'!A0");
  assert.equal(csvMod.sanitizeCsvCell("+91 9812890081"), "'+91 9812890081");
  assert.equal(csvMod.sanitizeCsvCell("@SUM(A1:A2)"), "'@SUM(A1:A2)");

  // 2. Date formatting & CSV build with UTF-8 BOM
  const sampleBookings = [
    {
      id: "bk_101",
      clientName: "Riya Sharma, Model",
      contactNumber: "+91 9812890081",
      serviceType: "High-Fashion Editorial - Pro",
      preferredDate: "2026-12-05",
      preferredTime: "10:00 AM",
      location: "Studio (Talwara Khurd, Sirsa)",
      status: "Confirmed",
      notes: 'Bring "Gold" backdrop\nTwo lighting setups',
      createdAt: { toDate: () => new Date("2026-11-21T09:15:00.000Z") }
    },
    {
      id: "bk_102",
      clientName: "=HYPERLINK(\"http://evil\",\"Click\")",
      contactNumber: "9876543210",
      serviceType: "Bridal Story",
      preferredDate: "2026-12-10",
      preferredTime: "04:00 PM",
      location: "Sirsa Palace",
      status: "Pending",
      notes: "",
      createdAt: "2026-11-20T14:30:00.000Z"
    }
  ];

  const csv = csvMod.buildBookingsCsv(sampleBookings);
  assert.ok(csv.startsWith("\uFEFFBooking ID,Client Name,Contact Number,Service Type,Preferred Date,Preferred Time,Location,Status,Notes,Created At\r\n"));
  assert.ok(csv.includes('"Riya Sharma, Model"'));
  assert.ok(csv.includes("'+91 9812890081"));
  assert.ok(csv.includes('"Bring ""Gold"" backdrop\nTwo lighting setups"'));
  assert.ok(csv.includes("2026-11-21 09:15 UTC"));
  assert.ok(csv.includes("\"'=HYPERLINK("), "Formula-starting cells must be neutralized with leading apostrophe");

  // 3. Empty bookings returns empty status without triggering download
  assert.deepEqual(csvMod.exportBookingsToCsv([]), {
    status: "empty",
    count: 0,
    fileName: "",
    csv: ""
  });

  // 4. End-to-end anchor download flow with Blob + URL.createObjectURL
  const anchors = [];
  let capturedBlob = null;
  const mockDoc = {
    body: { appendChild: () => {}, removeChild: () => {} },
    createElement: (tag) => {
      const el = {
        tag,
        href: "",
        download: "",
        style: {},
        clicked: false,
        click() {
          this.clicked = true;
        },
        remove() {}
      };
      anchors.push(el);
      return el;
    }
  };
  class MockBlob {
    constructor(parts, opts) {
      this.parts = parts;
      this.type = opts?.type || "";
    }
  }

  const res = csvMod.exportBookingsToCsv(sampleBookings, {
    doc: mockDoc,
    BlobCtor: MockBlob,
    urlApi: {
      createObjectURL: (blob) => {
        capturedBlob = blob;
        return "blob:star-artist/bookings-csv";
      },
      revokeObjectURL: () => {}
    },
    now: new Date("2026-11-21T05:16:00.000Z")
  });

  assert.equal(res.status, "exported");
  assert.equal(res.count, 2);
  assert.equal(res.fileName, "star-artist-studio-bookings-2026-11-21.csv");
  assert.equal(capturedBlob.type, "text/csv;charset=utf-8;");
  assert.equal(anchors[0].href, "blob:star-artist/bookings-csv");
  assert.equal(anchors[0].download, "star-artist-studio-bookings-2026-11-21.csv");
  assert.equal(anchors[0].clicked, true);

  // 5. Toolbar rendering (enabled with count > 0, disabled with count === 0)
  const tEn = (k) => i18nMod.TRANSLATIONS.en[k] || k;
  const enabledToolbar = csvMod.renderAdminBookingsToolbarHtml({ count: 2, t: tEn });
  assert.ok(enabledToolbar.includes('id="adminExportBookingsCsvBtn"'));
  assert.ok(enabledToolbar.includes("data-adm-export-bookings-csv"));
  assert.ok(enabledToolbar.includes("2 current booking(s)"));
  assert.equal(/\bdisabled\b/.test(enabledToolbar), false);

  const disabledToolbar = csvMod.renderAdminBookingsToolbarHtml({ count: 0, t: tEn });
  assert.ok(/\bdisabled\b/.test(disabledToolbar), "Export CSV button should be disabled when bookings list is empty");

  // 6. Wiring, SW pre-cache, and CSS verification
  const appJs = fs.readFileSync(path.join(webDir, "js/app.js"), "utf8");
  assert.ok(appJs.includes("exportBookingsToCsv"), "app.js must wire exportBookingsToCsv");
  assert.ok(appJs.includes("renderAdminBookingsToolbarHtml"), "app.js must render Admin Bookings CSV toolbar");
  assert.ok(appJs.includes("data-adm-export-bookings-csv"), "app.js must handle CSV export button clicks");

  const swJs = fs.readFileSync(path.join(webDir, "sw.js"), "utf8");
  assert.ok(swJs.includes("/js/admin-bookings-export.js"), "sw.js must pre-cache admin-bookings-export.js");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [".admin-bookings-toolbar", ".admin-export-csv-btn"]) {
    assert.ok(css.includes(selector), `styles.css missing CSV export rule for ${selector}`);
  }
});

test("Company / Event Organizer Registration & Model Requirement Booking System protects model privacy and integrates with Admin Dashboard", async () => {
  const companyMod = await import(pathToFileURL(path.join(webDir, "js/company-booking.js")).href);
  const guardMod = await import(pathToFileURL(path.join(webDir, "js/admin-guard.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);
  const tEn = (k) => i18nMod.TRANSLATIONS.en[k] || k;

  // 1. Public route registration
  assert.ok(guardMod.PUBLIC_ROUTES.includes("companies"), "companies route must be publicly accessible");

  // 2. Model privacy sanitization strictly strips personal contact details & unsafe URLs
  const rawSensitiveModel = {
    id: "m_live_01",
    userId: "uid_secret_owner",
    fullName: "Kavya Arora",
    gender: "Female",
    city: "Sirsa, Haryana",
    phoneNumber: "+91 9812899999",
    mobileNumber: "9812899999",
    email: "kavya.private@gmail.com",
    address: "Private Home Street 42",
    adminNotes: "Internal casting note",
    skills: "Bridal, Runway, Print",
    portfolioUrls: "javascript:alert(1)",
    avatarUrl: "/images/model-portrait.jpg"
  };
  const sanitized = companyMod.sanitizePublicModelProfile(rawSensitiveModel);
  assert.ok(sanitized);
  assert.equal(sanitized.fullName, "Kavya Arora");
  assert.equal(sanitized.portfolioUrl, "", "Unsafe javascript: portfolio URL must be stripped");
  assert.equal("phoneNumber" in sanitized, false);
  assert.equal("mobileNumber" in sanitized, false);
  assert.equal("email" in sanitized, false);
  assert.equal("address" in sanitized, false);
  assert.equal("userId" in sanitized, false);
  assert.equal("adminNotes" in sanitized, false);

  // Merged roster + gender/search filtering
  const merged = companyMod.mergePublicModels([rawSensitiveModel]);
  assert.ok(merged.length >= 6);
  const females = companyMod.filterPublicModels(merged, { gender: "female" });
  assert.ok(females.every((m) => m.gender.toLowerCase() === "female"));
  const searchHits = companyMod.filterPublicModels(merged, { search: "Kavya" });
  assert.equal(searchHits.length, 1);

  // Rendered public cards NEVER expose private phone/email
  const publicCardsHtml = companyMod.renderPublicModelPortfolioCardsHtml({
    models: [rawSensitiveModel],
    selectedModelIds: ["m_live_01"],
    t: tEn
  });
  assert.ok(publicCardsHtml.includes("Kavya Arora"));
  assert.ok(publicCardsHtml.includes("Selected for Booking"));
  assert.equal(publicCardsHtml.includes("9812899999"), false, "Model phone number must NEVER appear in public portfolio HTML");
  assert.equal(publicCardsHtml.includes("kavya.private@gmail.com"), false, "Model email must NEVER appear in public portfolio HTML");

  // 3. Company registration & model requirement booking validation
  const badReg = companyMod.validateCompanyRegistration({
    companyName: "A",
    contactPerson: "",
    mobileNumber: "123",
    email: "bad-email",
    address: "No"
  });
  assert.equal(badReg.valid, false);
  assert.ok(badReg.errors.length >= 5);

  const goodReg = companyMod.validateCompanyRegistration({
    companyName: "Royal Heritage Events",
    organizationType: "Event Organizer",
    contactPerson: "Vikram Mehta",
    mobileNumber: "+91 9812890081",
    email: "Events@RoyalHeritage.in",
    address: "Sector 17, Chandigarh, India"
  });
  assert.equal(goodReg.valid, true);
  assert.equal(goodReg.cleaned.email, "events@royalheritage.in");

  // Verify all required dropdown options exist
  assert.deepEqual(companyMod.MODEL_REQUIREMENT_OPTIONS, [
    "Male Models",
    "Female Models",
    "Both",
    "Multiple Models"
  ]);
  assert.deepEqual(companyMod.EVENT_TYPE_OPTIONS, [
    "Fashion Show",
    "Wedding",
    "Music Video",
    "Advertisement",
    "Brand Promotion",
    "Photoshoot",
    "Other"
  ]);

  const goodBooking = companyMod.validateCompanyModelBooking({
    ...goodReg.cleaned,
    modelRequirement: "Both",
    modelsCount: "6",
    eventType: "Fashion Show",
    eventDate: "2026-12-28",
    eventLocation: "Sirsa Grand Palace",
    budget: "₹80,000",
    detailedRequirements: "Need 3 male and 3 female models for festive runway show.",
    requestedModelIds: ["m_live_01", "m_live_01"],
    requestedModelNames: ["Kavya Arora"]
  });
  assert.equal(goodBooking.valid, true);
  assert.equal(goodBooking.cleaned.modelsCount, 6);
  assert.deepEqual(goodBooking.cleaned.requestedModelIds, ["m_live_01"]);

  const badBooking = companyMod.validateCompanyModelBooking({
    ...goodReg.cleaned,
    modelRequirement: "InvalidOption",
    modelsCount: "0",
    eventType: "UnknownEvent",
    eventDate: "",
    eventLocation: "",
    budget: "",
    detailedRequirements: "short"
  });
  assert.equal(badBooking.valid, false);
  assert.ok(badBooking.errors.length >= 6);

  // 4. Admin Dashboard company request management rendering & XSS escaping
  const sampleReq = {
    id: "req_101",
    ...goodBooking.cleaned,
    companyName: "Royal <script>alert(1)</script> Events",
    status: "Pending",
    adminNotes: ""
  };
  const adminHtml = companyMod.renderAdminCompanyRequestsSectionHtml({
    requests: [sampleReq],
    companies: [goodReg.cleaned],
    filter: "all",
    searchQuery: "",
    drafts: {},
    t: tEn
  });
  assert.equal(adminHtml.includes("<script>"), false, "Admin company cards must escape HTML");
  assert.ok(adminHtml.includes("data-adm-comp-id=\"req_101\""));
  assert.ok(adminHtml.includes("data-adm-comp-st=\"Approved\""));
  assert.ok(adminHtml.includes("data-adm-comp-st=\"Contacted\""));
  assert.ok(adminHtml.includes("data-adm-comp-st=\"Rejected\""));
  assert.ok(adminHtml.includes("tel:+919812890081"));
  assert.ok(adminHtml.includes("mailto:events@royalheritage.in"));
  assert.ok(adminHtml.includes("Kavya Arora"));

  // 5. HTML, JS, SW, CSS, and Firestore Rules integration verification
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  for (const token of [
    'id="view-companies"',
    'id="companyRegistrationForm"',
    'id="publicModelsGrid"',
    'id="companyBookingForm"',
    'id="compBookRequirement"',
    'id="compBookCount"',
    'id="compBookEventType"',
    'id="selectedRequestedModelsBar"',
    'id="myCompanyBookingsList"',
    'data-admin-tab="companies"',
    'data-route="companies"'
  ]) {
    assert.ok(html.includes(token), `index.html missing Company Booking token: ${token}`);
  }

  const appJs = fs.readFileSync(path.join(webDir, "js/app.js"), "utf8");
  for (const token of [
    "renderPublicModels",
    "submitCompanyModelBooking",
    "saveCompanyRegistration",
    "adminUpdateCompanyBookingStatus",
    "renderAdminCompanyRequestsSectionHtml"
  ]) {
    assert.ok(appJs.includes(token), `app.js missing Company Booking wiring: ${token}`);
  }

  const firebaseJs = fs.readFileSync(path.join(webDir, "js/firebase.js"), "utf8");
  assert.ok(firebaseJs.includes('collection(db, "publicModels")'), "firebase.js must read publicModels");
  assert.ok(firebaseJs.includes('doc(db, "publicModels", modelId)'), "firebase.js must sync publicModels on model approval");
  assert.ok(firebaseJs.includes('collection(db, "companyBookings")'), "firebase.js must persist companyBookings");

  const swJs = fs.readFileSync(path.join(webDir, "sw.js"), "utf8");
  assert.ok(swJs.includes("/js/company-booking.js"), "sw.js must pre-cache company-booking.js");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".public-model-card",
    ".selected-model-chip",
    ".admin-company-meta-grid",
    ".company-privacy-banner"
  ]) {
    assert.ok(css.includes(selector), `styles.css missing Company Booking rule: ${selector}`);
  }

  const rules = fs.readFileSync(path.join(webDir, "../firestore.rules"), "utf8");
  for (const token of [
    "function isValidPublicModel(data)",
    "data.keys().hasOnly([",
    "match /publicModels/{modelId}",
    "match /companies/{companyId}",
    "match /companyBookings/{requestId}"
  ]) {
    assert.ok(rules.includes(token), `firestore.rules missing rule: ${token}`);
  }
});

test("Admin Dashboard real-time booking status tracker auto-transitions past-date Confirmed bookings to Completed", async () => {
  const trackerMod = await import(pathToFileURL(path.join(webDir, "js/admin-booking-tracker.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);
  const tEn = (k) => i18nMod.TRANSLATIONS.en[k] || k;

  // Reference clock: 2026-11-21 15:00 local time
  const now = new Date(2026, 10, 21, 15, 0, 0);
  assert.equal(trackerMod.getLocalDateKey(now), "2026-11-21");

  // 1. Date parsing & event date passing detection
  assert.equal(trackerMod.parseEventDate("2026-11-20").isDateOnly, true);
  assert.equal(trackerMod.parseEventDate("2026-02-31"), null, "Invalid calendar dates must return null");
  assert.equal(trackerMod.parseEventDate("not-a-date"), null);

  assert.equal(
    trackerMod.hasEventDatePassed({ preferredDate: "2026-11-20" }, now),
    true,
    "Yesterday's date-only event must be marked passed"
  );
  assert.equal(
    trackerMod.hasEventDatePassed({ preferredDate: "2026-11-21" }, now),
    false,
    "Today's date-only event must stay Confirmed until the day finishes"
  );
  assert.equal(
    trackerMod.hasEventDatePassed({ preferredDate: "2026-11-22" }, now),
    false,
    "Tomorrow's event must not be marked passed"
  );
  assert.equal(
    trackerMod.hasEventDatePassed({ preferredDate: new Date(2026, 10, 21, 10, 0, 0).toISOString() }, now),
    true,
    "Past ISO timestamp on same day must be marked passed"
  );
  assert.equal(
    trackerMod.hasEventDatePassed({ preferredDate: { seconds: Math.floor(new Date(2026, 10, 19, 12, 0, 0).getTime() / 1000) } }, now),
    true,
    "Past Firestore Timestamp object must be marked passed"
  );
  assert.equal(trackerMod.hasEventDatePassed({ preferredDate: "invalid" }, now), false);

  // 2. findDueConfirmedBookings & summarizeRealtimeBookingTracker
  const sampleBookings = [
    { id: "b_past_conf_1", clientName: "Aarav Kapoor", serviceType: "Bridal Shoot", preferredDate: "2026-11-18", status: "Confirmed" },
    { id: "b_past_conf_2", clientName: "Meera <script>alert(1)</script>", serviceType: "Runway Lookbook", preferredDate: "2026-11-20", status: "Confirmed" },
    { id: "b_today_conf", clientName: "Riya Sharma", serviceType: "Editorial", preferredDate: "2026-11-21", status: "Confirmed" },
    { id: "b_future_conf", clientName: "Kabir Gill", serviceType: "Music Video", preferredDate: "2026-11-26", status: "Confirmed" },
    { id: "b_past_pending", clientName: "Unconfirmed Client", serviceType: "Portfolio", preferredDate: "2026-11-15", status: "Pending" },
    { id: "b_past_cancelled", clientName: "Cancelled Client", serviceType: "Portfolio", preferredDate: "2026-11-12", status: "Cancelled" },
    { id: "b_done", clientName: "Completed Client", serviceType: "Commercial", preferredDate: "2026-11-10", status: "Completed" }
  ];

  const due = trackerMod.findDueConfirmedBookings(sampleBookings, now);
  assert.deepEqual(
    due.map((b) => b.id),
    ["b_past_conf_1", "b_past_conf_2"],
    "Only past-date Confirmed bookings should be selected for auto-completion"
  );

  const summary = trackerMod.summarizeRealtimeBookingTracker(sampleBookings, now);
  assert.equal(summary.confirmedUpcoming, 1);
  assert.equal(summary.confirmedToday, 1);
  assert.equal(summary.confirmedPastDue, 2);
  assert.equal(summary.completed, 1);
  assert.equal(summary.pending, 1);
  assert.equal(summary.cancelled, 1);

  // 3. Stateful controller: auto-complete, in-flight deduplication, pause/resume, clean stop()
  let liveBookings = sampleBookings.map((b) => ({ ...b }));
  let autoCompleteCalls = 0;
  let releasePendingWrite;
  const pendingWritePromise = new Promise((resolve) => {
    releasePendingWrite = resolve;
  });

  const controller = trackerMod.createRealtimeBookingTracker({
    getBookings: () => liveBookings,
    intervalMs: 0,
    nowProvider: () => now,
    onAutoComplete: async (dueList) => {
      autoCompleteCalls += 1;
      await pendingWritePromise;
      const ids = dueList.map((b) => b.id);
      liveBookings = liveBookings.map((b) => (ids.includes(b.id) ? { ...b, status: "Completed" } : b));
      return { succeededIds: ids, failedIds: [] };
    }
  });

  controller.start();
  controller.setLiveConnected(true);

  // Fire two concurrent evaluations while first write is still in flight
  const eval1 = controller.evaluateNow();
  const eval2 = controller.evaluateNow();
  releasePendingWrite();
  const [res1, res2] = await Promise.all([eval1, eval2]);

  assert.equal(autoCompleteCalls, 1, "Concurrent evaluations must deduplicate in-flight booking IDs");
  assert.deepEqual(
    res1.transitioned.map((b) => b.id),
    ["b_past_conf_1", "b_past_conf_2"]
  );
  assert.equal(res2.transitioned.length, 0);

  const snapAfter = controller.getSnapshot();
  assert.equal(snapAfter.totalAutoCompleted, 2);
  assert.equal(snapAfter.isLiveConnected, true);
  assert.equal(snapAfter.recentTransitions.length, 2);
  assert.equal(snapAfter.recentTransitions[0].fromStatus, "Confirmed");
  assert.equal(snapAfter.recentTransitions[0].toStatus, "Completed");

  // Pause auto-complete: automatic evaluateNow() is skipped unless force: true
  controller.setEnabled(false);
  liveBookings.push({
    id: "b_past_conf_3",
    clientName: "Late Sync Client",
    serviceType: "Fashion",
    preferredDate: "2026-11-19",
    status: "Confirmed"
  });
  const skippedRes = await controller.evaluateNow();
  assert.equal(skippedRes.skipped, true);
  assert.equal(autoCompleteCalls, 1);

  const forcedRes = await controller.evaluateNow({ force: true });
  assert.equal(forcedRes.skipped, false);
  assert.deepEqual(forcedRes.transitioned.map((b) => b.id), ["b_past_conf_3"]);
  assert.equal(autoCompleteCalls, 2);

  controller.stop();
  assert.equal(controller.getSnapshot().running, false);
  assert.equal(controller.getSnapshot().isLiveConnected, false);

  // 4. HTML rendering & XSS escaping
  const cardHtml = trackerMod.renderAdminBookingTrackerHtml({
    bookings: liveBookings,
    trackerState: { ...snapAfter, enabled: true, isLiveConnected: true },
    now,
    lang: "en",
    t: tEn
  });
  assert.equal(cardHtml.includes("<script>"), false, "Tracker feed must escape HTML/XSS payloads");
  assert.ok(cardHtml.includes("Confirmed → Completed"));
  assert.ok(cardHtml.includes("data-tracker-toggle"));
  assert.ok(cardHtml.includes("data-tracker-check-now"));
  assert.ok(cardHtml.includes("booking-tracker-live-pill is-live"));

  const chipDue = trackerMod.renderBookingTimelineChipHtml(sampleBookings[0], { now, lang: "en", t: tEn });
  const chipToday = trackerMod.renderBookingTimelineChipHtml(sampleBookings[2], { now, lang: "en", t: tEn });
  const chipUpcoming = trackerMod.renderBookingTimelineChipHtml(sampleBookings[3], { now, lang: "en", t: tEn });
  const chipCompleted = trackerMod.renderBookingTimelineChipHtml(sampleBookings[6], { now, lang: "en", t: tEn });
  assert.ok(chipDue.includes("tracker-timeline-chip is-passed"));
  assert.ok(chipToday.includes("tracker-timeline-chip is-today"));
  assert.ok(chipUpcoming.includes("In 5d"));
  assert.ok(chipCompleted.includes("tracker-timeline-chip is-completed"));

  // 5. Integration verification across HTML, Firebase, App, SW, and CSS
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.ok(html.includes('id="adminBookingTrackerCard"'), "index.html missing #adminBookingTrackerCard");
  assert.ok(
    html.indexOf('id="adminConsoleContent"') < html.indexOf('id="adminBookingTrackerCard"'),
    "#adminBookingTrackerCard must remain gated inside #adminConsoleContent"
  );

  const firebaseJs = fs.readFileSync(path.join(webDir, "js/firebase.js"), "utf8");
  assert.ok(firebaseJs.includes("export async function adminAutoCompleteExpiredBookings"), "firebase.js missing adminAutoCompleteExpiredBookings");
  assert.ok(firebaseJs.includes("export async function adminSubscribeBookingsRealtime"), "firebase.js missing adminSubscribeBookingsRealtime");
  assert.ok(firebaseJs.includes("onSnapshot"), "firebase.js must subscribe to Firestore onSnapshot for real-time tracking");

  const appJs = fs.readFileSync(path.join(webDir, "js/app.js"), "utf8");
  for (const token of [
    "createRealtimeBookingTracker",
    "adminSubscribeBookingsRealtime",
    "adminAutoCompleteExpiredBookings",
    "renderAdminBookingTracker",
    "renderBookingTimelineChipHtml"
  ]) {
    assert.ok(appJs.includes(token), `app.js missing real-time tracker wiring: ${token}`);
  }

  const swJs = fs.readFileSync(path.join(webDir, "sw.js"), "utf8");
  assert.ok(swJs.includes("/js/admin-booking-tracker.js"), "sw.js must pre-cache /js/admin-booking-tracker.js");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [
    ".booking-tracker-card",
    ".booking-tracker-live-pill",
    ".booking-tracker-pulse-dot",
    ".tracker-timeline-chip"
  ]) {
    assert.ok(css.includes(selector), `styles.css missing tracker rule: ${selector}`);
  }
});

test("Permanent shared Firebase Web configuration loads automatically on all mobile devices with zero localStorage setup prompts or private keys", async () => {
  const firebaseMod = await import(pathToFileURL(path.join(webDir, "js/firebase.js")).href);

  // 1. Central shared web/firebase-config.json exists, matches the studio project & named DB, and contains zero private service account keys
  const sharedCfgPath = path.join(webDir, "firebase-config.json");
  assert.ok(fs.existsSync(sharedCfgPath), "web/firebase-config.json must exist");
  const sharedCfgRaw = fs.readFileSync(sharedCfgPath, "utf8");
  assert.equal(sharedCfgRaw.includes("BEGIN PRIVATE KEY"), false, "Must never expose private service account keys");
  assert.equal(sharedCfgRaw.includes("private_key"), false, "Must never contain service account private_key");
  assert.equal(sharedCfgRaw.includes("client_email"), false, "Must never contain service account client_email");

  const sharedCfg = JSON.parse(sharedCfgRaw);
  assert.equal(sharedCfg.projectId, "gen-lang-client-0548683163");
  assert.equal(sharedCfg.databaseId, "ai-studio-android-stararti-4ffde0d6-02fd-4194-8e31-3359a6b9588b");
  assert.ok(String(sharedCfg.appId || "").startsWith("1:634443186065:web:"), "Shared config must specify Firebase Web App ID");
  assert.ok(String(sharedCfg.apiKey || "").length >= 20, "Shared config must specify public Web API Key");

  // 2. On Firebase Hosting, /__/firebase/init.json is preferred automatically and merges the studio's named Firestore databaseId
  const hostedRes = await firebaseMod.resolveFirebaseWebConfig({
    fetchImpl: async (url) => {
      if (url === "/__/firebase/init.json") {
        return {
          ok: true,
          headers: { get: () => "application/json" },
          json: async () => ({
            apiKey: sharedCfg.apiKey,
            appId: sharedCfg.appId,
            projectId: sharedCfg.projectId,
            authDomain: sharedCfg.authDomain,
            storageBucket: sharedCfg.storageBucket,
            messagingSenderId: sharedCfg.messagingSenderId
          })
        };
      }
      return { ok: false, headers: { get: () => "text/html" } };
    }
  });
  assert.equal(hostedRes.configSource, "firebase-hosting");
  assert.equal(hostedRes.projectId, "gen-lang-client-0548683163");
  assert.equal(hostedRes.databaseId, "ai-studio-android-stararti-4ffde0d6-02fd-4194-8e31-3359a6b9588b");

  // 3. Outside Firebase Hosting (when /__/firebase/init.json returns SPA HTML or 404), seamlessly falls back to /firebase-config.json
  const fallbackRes = await firebaseMod.resolveFirebaseWebConfig({
    fetchImpl: async (url) => {
      if (url === "/__/firebase/init.json") {
        return {
          ok: true,
          headers: { get: () => "text/html; charset=utf-8" },
          json: async () => {
            throw new Error("Unexpected HTML");
          }
        };
      }
      if (url === "/firebase-config.json") {
        return {
          ok: true,
          headers: { get: () => "application/json" },
          json: async () => sharedCfg
        };
      }
      return { ok: false, headers: { get: () => "" } };
    }
  });
  assert.equal(fallbackRes.configSource, "shared-config");
  assert.equal(fallbackRes.projectId, "gen-lang-client-0548683163");
  assert.equal(fallbackRes.appId, sharedCfg.appId);
  assert.equal(fallbackRes.databaseId, "ai-studio-android-stararti-4ffde0d6-02fd-4194-8e31-3359a6b9588b");

  // 4. Manual Firebase Setup UI & localStorage prompts are completely removed for normal visitors
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.equal(html.includes('id="openFirebaseSetupBtn"'), false, "Visitors must not see a Firebase Setup button");
  assert.equal(html.includes('id="firebaseSetupModal"'), false, "Visitors must not see a manual Firebase API key modal");
  assert.equal(html.includes('id="firebaseConfigForm"'), false, "Manual Firebase config form must be removed");

  const appJs = fs.readFileSync(path.join(webDir, "js/app.js"), "utf8");
  assert.equal(appJs.includes("firebaseSetupModal"), false, "app.js must not open firebaseSetupModal");
  assert.equal(appJs.includes("saveManualWebConfig"), false, "app.js must not prompt visitors to save config to localStorage");

  const firebaseJs = fs.readFileSync(path.join(webDir, "js/firebase.js"), "utf8");
  assert.equal(firebaseJs.includes("localStorage.setItem"), false, "firebase.js must not store Firebase config in localStorage");
  assert.ok(firebaseJs.includes("/firebase-config.json"), "firebase.js must load centralized /firebase-config.json");
  assert.ok(firebaseJs.includes("/__/firebase/init.json"), "firebase.js must auto-load Firebase Hosting init.json");
  assert.ok(firebaseJs.includes("signInWithRedirect"), "firebase.js must include mobile redirect sign-in fallback");

  const swJs = fs.readFileSync(path.join(webDir, "sw.js"), "utf8");
  assert.ok(swJs.includes('"/firebase-config.json"'), "sw.js must pre-cache /firebase-config.json for offline PWA support");
});

test("Production Firebase Hosting (.web.app / .firebaseapp.com) auto-loads /__/firebase/init.json, never shows setup popup, and prevents stale mobile caches", async () => {
  const firebaseMod = await import(pathToFileURL(path.join(webDir, "js/firebase.js")).href);

  // 1. Production domain classification (.web.app and .firebaseapp.com) vs local development preview
  assert.equal(firebaseMod.isFirebaseHostingProductionHost("gen-lang-client-0548683163.web.app"), true);
  assert.equal(firebaseMod.isFirebaseHostingProductionHost("gen-lang-client-0548683163.firebaseapp.com"), true);
  assert.equal(firebaseMod.isFirebaseHostingProductionHost("GEN-LANG-CLIENT-0548683163.WEB.APP"), true);
  assert.equal(firebaseMod.isFirebaseHostingProductionHost("localhost"), false);
  assert.equal(firebaseMod.isFirebaseHostingProductionHost("127.0.0.1"), false);

  assert.equal(firebaseMod.isLocalDevelopmentHost("localhost"), true);
  assert.equal(firebaseMod.isLocalDevelopmentHost("127.0.0.1"), true);
  assert.equal(firebaseMod.isLocalDevelopmentHost("::1"), true);
  assert.equal(firebaseMod.isLocalDevelopmentHost("gen-lang-client-0548683163.web.app"), false);
  assert.equal(firebaseMod.isLocalDevelopmentHost("gen-lang-client-0548683163.firebaseapp.com"), false);

  // 2. Manual setup is NEVER shown on production Firebase Hosting domains (even if offline/unready) or once connected
  assert.equal(
    firebaseMod.shouldShowManualFirebaseSetup({ hostname: "gen-lang-client-0548683163.web.app", firebaseReady: false }),
    false,
    "Production .web.app must NEVER show manual Firebase setup"
  );
  assert.equal(
    firebaseMod.shouldShowManualFirebaseSetup({ hostname: "gen-lang-client-0548683163.firebaseapp.com", firebaseReady: false }),
    false,
    "Production .firebaseapp.com must NEVER show manual Firebase setup"
  );
  assert.equal(
    firebaseMod.shouldShowManualFirebaseSetup({ hostname: "starartiststudio.in", firebaseReady: false }),
    false,
    "Custom visitor domains must NEVER show manual Firebase setup"
  );
  assert.equal(
    firebaseMod.shouldShowManualFirebaseSetup({ hostname: "localhost", firebaseReady: true }),
    false,
    "Local dev preview must hide manual setup once Firebase is connected"
  );
  assert.equal(
    firebaseMod.shouldShowManualFirebaseSetup({ hostname: "localhost", firebaseReady: false }),
    true,
    "Local dev preview may offer fallback manual setup ONLY when auto-config is unavailable"
  );

  // 3. saveLocalDevFirebaseConfig is a strict no-op on production domains
  const blockedProdSave = firebaseMod.saveLocalDevFirebaseConfig(
    { apiKey: "test-key-12345678901234567890", appId: "1:634443186065:web:1fed1386efeed679c12430" },
    { hostname: "gen-lang-client-0548683163.web.app" }
  );
  assert.equal(blockedProdSave, null, "Production domains must ignore manual local-dev config saves");

  // 4. Production .web.app retries /__/firebase/init.json once on transient mobile network blip and resolves automatically on a fresh session
  const sharedCfg = JSON.parse(fs.readFileSync(path.join(webDir, "firebase-config.json"), "utf8"));
  let initAttempts = 0;
  const prodRes = await firebaseMod.resolveFirebaseWebConfig({
    hostname: "gen-lang-client-0548683163.web.app",
    fetchImpl: async (url) => {
      if (url === "/__/firebase/init.json") {
        initAttempts += 1;
        if (initAttempts === 1) {
          throw new Error("Transient mobile network drop");
        }
        return {
          ok: true,
          headers: { get: () => "application/json; charset=utf-8" },
          json: async () => ({
            apiKey: sharedCfg.apiKey,
            appId: sharedCfg.appId,
            projectId: sharedCfg.projectId,
            authDomain: sharedCfg.authDomain,
            storageBucket: sharedCfg.storageBucket,
            messagingSenderId: sharedCfg.messagingSenderId
          })
        };
      }
      return { ok: false, headers: { get: () => "" } };
    }
  });
  assert.equal(initAttempts, 2, "Production Hosting should retry /__/firebase/init.json once on transient mobile drop");
  assert.equal(prodRes.configSource, "firebase-hosting");
  assert.equal(prodRes.projectId, "gen-lang-client-0548683163");
  assert.equal(prodRes.databaseId, "ai-studio-android-stararti-4ffde0d6-02fd-4194-8e31-3359a6b9588b");

  // 5. No stale "Firebase Web App is not configured" error messages remain in firebase.js
  const firebaseJs = fs.readFileSync(path.join(webDir, "js/firebase.js"), "utf8");
  assert.equal(
    firebaseJs.includes("Firebase Web App is not configured"),
    false,
    "firebase.js must not show legacy unconfigured setup errors to visitors"
  );

  // 6. firebase.json prevents stale 1-hour caching of /, /index.html, /sw.js, /firebase-config.json, and /js/**
  const firebaseJson = JSON.parse(fs.readFileSync(path.join(rootDir, "firebase.json"), "utf8"));
  const headerMap = new Map(
    (firebaseJson.hosting?.headers || []).map((entry) => [
      entry.source,
      Object.fromEntries((entry.headers || []).map((h) => [h.key.toLowerCase(), h.value]))
    ])
  );
  for (const src of ["/", "/index.html", "/sw.js", "/firebase-config.json", "/js/**", "/styles.css"]) {
    const cc = headerMap.get(src)?.["cache-control"] || "";
    assert.ok(cc.includes("no-cache"), `firebase.json must set no-cache header on ${src}`);
  }

  // 7. Service Worker v32+ uses network-first for JS/CSS/HTML/config and app.js forces SW updateViaCache: none + controllerchange reload
  const swJs = fs.readFileSync(path.join(webDir, "sw.js"), "utf8");
  assert.match(swJs, /CACHE_VERSION = "star-artist-pwa-v3[1-9]"/, "sw.js must use v31+ cache version");
  assert.ok(swJs.includes("isFreshCodeAsset"), "sw.js must serve code & config network-first with cache fallback");

  const appJs = fs.readFileSync(path.join(webDir, "js/app.js"), "utf8");
  assert.ok(appJs.includes('updateViaCache: "none"'), "app.js must register sw.js with updateViaCache: none");
  assert.ok(appJs.includes("controllerchange"), "app.js must refresh once when an updated Service Worker takes control");
  assert.ok(appJs.includes("shouldShowManualFirebaseSetup"), "app.js must gate any local-dev setup behind shouldShowManualFirebaseSetup");
});

test("Admin Dashboard Email Preview mode renders branded booking, 18+ model, and company alerts safely inside a sandboxed preview", async () => {
  const previewMod = await import(pathToFileURL(path.join(webDir, "js/admin-email-preview.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);
  const t = (k) => i18nMod.TRANSLATIONS.en[k] || k;

  // 1. Booking preview with live record + XSS injection attempt safely escaped
  const bookingPreview = previewMod.buildAdminEmailPreview({
    type: "booking",
    recordId: "bk_live_77",
    record: {
      id: "bk_live_77",
      clientName: "Rohan <img src=x onerror=alert(1)>",
      serviceType: "4K Music Video",
      preferredDate: "2026-12-25",
      preferredTime: "02:00 PM",
      contactNumber: "+91 9812890081",
      location: "Studio Floor Sirsa",
      status: "Pending",
      notes: "Need crane shot & <script>evil()</script>"
    },
    brandOverrides: {
      brandName: "STAR ARTIST LUXURY",
      accentColor: "#E5B842",
      bookingCtaLabel: "Approve Shoot Now"
    }
  });
  assert.equal(bookingPreview.type, "booking");
  assert.equal(bookingPreview.isSample, false);
  assert.equal(bookingPreview.to, "chanderstudio0@gmail.com");
  assert.ok(bookingPreview.subject.includes("4K Music Video"));
  assert.equal(bookingPreview.html.includes("<script>evil()</script>"), false, "HTML preview must escape script tags");
  assert.ok(bookingPreview.html.includes("&lt;script&gt;evil()&lt;/script&gt;"));
  assert.ok(bookingPreview.html.includes("STAR ARTIST LUXURY"));
  assert.ok(bookingPreview.html.includes("Approve Shoot Now"));
  assert.ok(bookingPreview.html.includes("#E5B842"));

  // 2. 18+ Model Application preview (sample + javascript: URL rejection)
  const modelPreview = previewMod.buildAdminEmailPreview({
    type: "model",
    record: {
      id: "mdl_test_1",
      fullName: "Kavya Verma",
      age: 23,
      gender: "Female",
      city: "Sirsa",
      phoneNumber: "+91 9812890081",
      skills: "Fashion Editorial, Bridal",
      portfolioUrls: "javascript:alert(1)",
      consentGiven: true,
      status: "pending"
    }
  });
  assert.equal(modelPreview.type, "model");
  assert.equal(modelPreview.statusBadgeLabel, "18+ VERIFIED");
  assert.ok(modelPreview.subject.includes("Kavya Verma"));
  assert.equal(modelPreview.html.includes('href="javascript:'), false, "Must strip dangerous javascript: portfolio URLs");

  // 3. Company Request preview (sample fallback)
  const companyPreview = previewMod.buildAdminEmailPreview({ type: "company" });
  assert.equal(companyPreview.type, "company");
  assert.equal(companyPreview.isSample, true);
  assert.ok(companyPreview.subject.includes("Royal Heritage Couture Events"));
  assert.ok(companyPreview.html.includes("CORPORATE BOOKING ALERT"));
  assert.ok(companyPreview.text.includes("Grand Courtyard Banquet, Sirsa"));

  // 4. Rendered Admin Email Preview section uses strictly sandboxed iframe (sandbox="")
  const renderedHtml = previewMod.renderAdminEmailPreviewSectionHtml({
    previewState: { type: "company", recordId: "__sample__", viewMode: "html", viewport: "mobile", overrides: {} },
    adminData: { bookings: [], models: [], companyBookings: [] },
    t
  });
  assert.ok(renderedHtml.includes('id="adminEmailPreviewPanel"'));
  assert.ok(renderedHtml.includes('id="adminEmailPreviewFrame"'));
  assert.ok(renderedHtml.includes('sandbox=""'), "Preview iframe must enforce empty sandbox attribute");
  assert.ok(renderedHtml.includes("is-mobile"), "Mobile viewport toggle must apply is-mobile class");

  // 5. Email Preview tab exists strictly inside #adminConsoleContent and sw.js pre-caches the module
  const indexHtml = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  const adminContentIdx = indexHtml.indexOf('id="adminConsoleContent"');
  const emailTabIdx = indexHtml.indexOf('data-admin-tab="email-preview"');
  assert.ok(adminContentIdx > 0 && emailTabIdx > adminContentIdx, "Email Preview tab must live inside admin-only console");

  const swJs = fs.readFileSync(path.join(webDir, "sw.js"), "utf8");
  assert.ok(swJs.includes('"/js/admin-email-preview.js"'), "sw.js must pre-cache /js/admin-email-preview.js");
});

test("Gallery lightbox full-screen toggle uses browser Fullscreen API with vendor-prefix and viewport fallback support", async () => {
  const shareMod = await import(pathToFileURL(path.join(webDir, "js/gallery-share.js")).href);

  // 1. Keyboard shortcut F / f maps to toggle-fullscreen
  assert.equal(shareMod.resolveLightboxKeyAction("f"), "toggle-fullscreen");
  assert.equal(shareMod.resolveLightboxKeyAction("F"), "toggle-fullscreen");

  // 2. Native Fullscreen API enter, fullscreenchange sync, and exit flow
  const events = [];
  const fakeDoc = {
    fullscreenEnabled: true,
    fullscreenElement: null,
    async exitFullscreen() {
      this.fullscreenElement = null;
    }
  };
  let requestCalls = 0;
  const fakeTarget = {
    async requestFullscreen() {
      requestCalls += 1;
      fakeDoc.fullscreenElement = fakeTarget;
    }
  };

  assert.equal(shareMod.isFullscreenApiSupported(fakeTarget, fakeDoc), true);
  assert.equal(shareMod.getFullscreenElement(fakeDoc), null);

  const ctrl = shareMod.createLightboxFullscreenController({
    getTargetEl: () => fakeTarget,
    doc: fakeDoc,
    onStateChange: (snap) => events.push(snap)
  });

  const enterResult = await ctrl.toggle();
  assert.equal(requestCalls, 1, "toggle() must invoke element.requestFullscreen()");
  assert.equal(enterResult.active, true);
  assert.equal(enterResult.mode, "native");
  assert.equal(ctrl.isFullscreen(), true);
  assert.equal(ctrl.isNativeFullscreen(), true);

  // Simulate user exiting native fullscreen via browser Escape key (fullscreenchange)
  fakeDoc.fullscreenElement = null;
  const synced = ctrl.syncFromDocument();
  assert.equal(synced.active, false);
  assert.equal(ctrl.isFullscreen(), false);
  assert.equal(events[events.length - 1].isFullscreen, false);

  // Re-enter and exit programmatically via toggle()
  await ctrl.enter();
  assert.equal(ctrl.isNativeFullscreen(), true);
  const exitResult = await ctrl.toggle();
  assert.equal(exitResult.active, false);
  assert.equal(fakeDoc.fullscreenElement, null);

  // 3. Safari webkit vendor-prefix support
  const webkitDoc = {
    webkitFullscreenEnabled: true,
    webkitFullscreenElement: null,
    webkitExitFullscreen() {
      this.webkitFullscreenElement = null;
    }
  };
  const webkitTarget = {
    webkitRequestFullscreen() {
      webkitDoc.webkitFullscreenElement = webkitTarget;
    }
  };
  const webkitCtrl = shareMod.createLightboxFullscreenController({
    getTargetEl: () => webkitTarget,
    doc: webkitDoc
  });
  const webkitEnter = await webkitCtrl.enter();
  assert.equal(webkitEnter.active, true);
  assert.equal(webkitEnter.mode, "native");
  assert.equal(webkitCtrl.isNativeFullscreen(), true);
  await webkitCtrl.exit();
  assert.equal(webkitCtrl.isFullscreen(), false);

  // 4. Graceful CSS viewport fallback when Fullscreen API is unavailable (e.g., iOS Safari)
  const fallbackEvents = [];
  const fallbackCtrl = shareMod.createLightboxFullscreenController({
    getTargetEl: () => ({}),
    doc: { fullscreenEnabled: false, fullscreenElement: null },
    onStateChange: (snap) => fallbackEvents.push(snap)
  });
  const fbEnter = await fallbackCtrl.toggle();
  assert.equal(fbEnter.active, true);
  assert.equal(fbEnter.mode, "fallback");
  assert.equal(fallbackCtrl.isFullscreen(), true);
  assert.equal(fallbackCtrl.isNativeFullscreen(), false);
  const fbExit = await fallbackCtrl.toggle();
  assert.equal(fbExit.active, false);
  assert.equal(fallbackEvents.length, 2);

  // 5. HTML, CSS, and app.js integration assertions
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.ok(html.includes('id="lightboxFullscreenBtn"'), "Lightbox must render #lightboxFullscreenBtn");
  assert.ok(html.includes('id="lightboxFullscreenEnterIcon"'), "Lightbox must include expand fullscreen icon");
  assert.ok(html.includes('id="lightboxFullscreenExitIcon"'), "Lightbox must include exit fullscreen icon");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const token of [
    ".lightbox-fullscreen-btn",
    "#lightboxModal.is-fullscreen",
    "#lightboxModal:fullscreen",
    "#lightboxModal:-webkit-full-screen",
    "#lightboxModal:fullscreen .lightbox-image"
  ]) {
    assert.ok(css.includes(token), `styles.css missing fullscreen lightbox rule: ${token}`);
  }

  const appJs = fs.readFileSync(path.join(webDir, "js/app.js"), "utf8");
  assert.ok(appJs.includes("createLightboxFullscreenController"), "app.js must wire createLightboxFullscreenController");
  assert.ok(appJs.includes('"fullscreenchange"'), "app.js must listen for fullscreenchange events");
  assert.ok(appJs.includes("lightboxFullscreen.exit()"), "app.js must exit fullscreen cleanly when closing lightbox");
});

test("Admin Dashboard CSV export button displays temporary loading animation and status text during download generation", async () => {
  const csvMod = await import(pathToFileURL(path.join(webDir, "js/admin-bookings-export.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);
  const t = (k) => i18nMod.TRANSLATIONS.en[k] || k;

  // 1. Toolbar renders idle, processing (spinner + disabled + aria-busy), and done states
  const processingHtml = csvMod.renderAdminBookingsToolbarHtml({ count: 5, status: "processing", t });
  assert.ok(processingHtml.includes("is-loading"), "Processing state must apply is-loading class");
  assert.ok(processingHtml.includes('aria-busy="true"'), "Processing state must set aria-busy=true");
  assert.ok(processingHtml.includes("admin-export-csv-spinner"), "Processing state must render spinner element");
  assert.ok(processingHtml.includes("Generating CSV..."), "Processing state must show localized status text");
  assert.ok(processingHtml.includes("disabled"), "Button must be disabled while processing");

  const doneHtml = csvMod.renderAdminBookingsToolbarHtml({ count: 5, status: "done", t });
  assert.ok(doneHtml.includes("is-exported"), "Done state must apply is-exported class");
  assert.ok(doneHtml.includes("CSV Downloaded"), "Done state must show downloaded confirmation label");

  // 2. In-place button updater mutates attributes, classes, disabled state, and innerHTML cleanly
  const classes = new Set();
  const attrs = {};
  const fakeBtn = {
    disabled: false,
    innerHTML: "",
    setAttribute(k, v) {
      attrs[k] = v;
    },
    classList: {
      toggle(cls, force) {
        if (force) classes.add(cls);
        else classes.delete(cls);
      }
    }
  };
  csvMod.applyAdminCsvButtonState(fakeBtn, "processing", { count: 3, t });
  assert.equal(fakeBtn.disabled, true);
  assert.equal(attrs["aria-busy"], "true");
  assert.equal(attrs["data-export-status"], "processing");
  assert.equal(classes.has("is-loading"), true);
  assert.ok(fakeBtn.innerHTML.includes("admin-export-csv-spinner"));

  csvMod.applyAdminCsvButtonState(fakeBtn, "idle", { count: 3, t });
  assert.equal(fakeBtn.disabled, false);
  assert.equal(attrs["aria-busy"], "false");
  assert.equal(classes.has("is-loading"), false);
  assert.ok(fakeBtn.innerHTML.includes("Export CSV"));

  // 3. Async export runner transitions cleanly through processing -> done -> idle
  const transitions = [];
  const fakeLink = { href: "", download: "", style: {}, clicked: false, click() { this.clicked = true; }, remove() {} };
  const fakeDoc = { createElement: () => fakeLink, body: { appendChild() {}, removeChild() {} } };
  const result = await csvMod.runAdminBookingsCsvExport(
    [
      {
        id: "bk_1",
        clientName: "Aarav Sharma",
        contactNumber: "+91 9812890081",
        serviceType: "Royal Wedding Film",
        preferredDate: "2026-12-20",
        status: "Confirmed"
      }
    ],
    {
      onStateChange: (status) => transitions.push(status),
      minProcessingMs: 10,
      exportedHoldMs: 10,
      delayFn: async () => {},
      exportOptions: {
        doc: fakeDoc,
        BlobCtor: class FakeBlob { constructor(parts, opts) { this.parts = parts; this.opts = opts; } },
        urlApi: { createObjectURL: () => "blob:test-csv", revokeObjectURL: () => {} }
      }
    }
  );
  assert.equal(result.status, "exported");
  assert.equal(result.count, 1);
  assert.equal(fakeLink.clicked, true);
  assert.deepEqual(transitions, ["processing", "done", "idle"]);

  // 4. Empty booking list immediately reports empty without hanging in loading state
  const emptyTransitions = [];
  const emptyRes = await csvMod.runAdminBookingsCsvExport([], {
    onStateChange: (status) => emptyTransitions.push(status),
    delayFn: async () => {}
  });
  assert.equal(emptyRes.status, "empty");
  assert.deepEqual(emptyTransitions, ["idle"]);

  // 5. CSS animation & app.js wiring checks
  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const token of [
    ".admin-export-csv-btn.is-loading",
    ".admin-export-csv-btn.is-exported",
    ".admin-export-csv-spinner",
    "@keyframes adminCsvSpin"
  ]) {
    assert.ok(css.includes(token), `styles.css missing CSV loading animation rule: ${token}`);
  }

  const appJs = fs.readFileSync(path.join(webDir, "js/app.js"), "utf8");
  assert.ok(appJs.includes("runAdminBookingsCsvExport"), "app.js must invoke runAdminBookingsCsvExport");
  assert.ok(appJs.includes("applyAdminCsvButtonState"), "app.js must update CSV button state in-place");
});

test("Admin Dashboard Email Preview 'Test Email Notification' builds admin-locked payload, renders live SMTP status banners, and enforces Firestore rules", async () => {
  const previewMod = await import(pathToFileURL(path.join(webDir, "js/admin-email-preview.js")).href);
  const i18nMod = await import(pathToFileURL(path.join(webDir, "js/i18n.js")).href);
  const t = (k) => i18nMod.TRANSLATIONS.en[k] || k;

  // 1. Test email request payload locks recipient to chanderstudio0@gmail.com and prefixes [TEST]
  const payload = previewMod.buildAdminTestEmailRequestPayload({
    type: "booking",
    record: {
      id: "bk_test_99",
      clientName: "Riya Malhotra",
      serviceType: "Luxury Bridal Shoot",
      preferredDate: "2026-12-20",
      createdAt: { seconds: 1791979200 }
    },
    brandOverrides: {
      brandName: "STAR ARTIST LUXURY",
      accentColor: "#E5C158",
      ignoredUnknownField: "should_not_pass"
    }
  });
  assert.equal(payload.templateType, "booking");
  assert.equal(payload.recipientEmail, "chanderstudio0@gmail.com");
  assert.equal(payload.isSample, false);
  assert.ok(payload.subject.startsWith("[TEST] "));
  assert.equal(payload.recordSnapshot.clientName, "Riya Malhotra");
  assert.equal("createdAt" in payload.recordSnapshot, false, "Non-serializable Firestore timestamps stripped from snapshot");
  assert.equal(payload.templateOverrides.brandName, "STAR ARTIST LUXURY");
  assert.equal("ignoredUnknownField" in payload.templateOverrides, false);

  // 2. Rendered Email Preview panel includes Send Test Email button and live status states
  const idleHtml = previewMod.renderAdminEmailPreviewSectionHtml({
    previewState: { type: "model", recordId: "__sample__", viewMode: "html", viewport: "desktop", overrides: {} },
    adminData: {},
    t
  });
  assert.ok(idleHtml.includes('id="emailPreviewSendTestBtn"'));
  assert.ok(idleHtml.includes("data-email-preview-send-test"));
  assert.ok(idleHtml.includes("Send Test Email"));

  const sendingHtml = previewMod.renderAdminEmailPreviewSectionHtml({
    previewState: {
      type: "booking",
      testSend: { status: "queued", recipient: "chanderstudio0@gmail.com" }
    },
    adminData: {},
    t
  });
  assert.ok(sendingHtml.includes('aria-busy="true"'));
  assert.ok(sendingHtml.includes('data-test-email-status="queued"'));
  assert.ok(sendingHtml.includes("Sending Test..."));

  const sentBanner = previewMod.renderAdminTestEmailStatusHtml(
    { status: "sent", recipient: "chanderstudio0@gmail.com", smtpHost: "smtp.gmail.com", smtpPort: 465, messageId: "msg_42" },
    { t }
  );
  assert.ok(sentBanner.includes('data-test-email-status="sent"'));
  assert.ok(sentBanner.includes("SMTP: smtp.gmail.com:465"));
  assert.ok(sentBanner.includes("ID: msg_42"));

  const failBanner = previewMod.renderAdminTestEmailStatusHtml(
    { status: "failed", error: "535 Authentication failed" },
    { t }
  );
  assert.ok(failBanner.includes('data-test-email-status="failed"'));
  assert.ok(failBanner.includes("535 Authentication failed"));

  const awaitingBanner = previewMod.renderAdminTestEmailStatusHtml(
    { status: "awaiting_worker", recipient: "chanderstudio0@gmail.com" },
    { t }
  );
  assert.ok(awaitingBanner.includes('data-test-email-status="awaiting_worker"'));

  // 3. Firestore security rules strictly lock adminEmailTests/{testId} to isAdmin() & chanderstudio0@gmail.com
  const rules = fs.readFileSync(path.join(rootDir, "firestore.rules"), "utf8");
  assert.ok(rules.includes("function isValidAdminEmailTest(data)"));
  assert.ok(rules.includes("match /adminEmailTests/{testId}"));
  assert.ok(rules.includes("data.recipientEmail.lower() == 'chanderstudio0@gmail.com'"));

  // 4. firebase.js & app.js integration checks
  const fbJs = fs.readFileSync(path.join(webDir, "js/firebase.js"), "utf8");
  assert.ok(fbJs.includes("export async function adminRequestTestEmail"));
  assert.ok(fbJs.includes("export async function adminSubscribeTestEmailStatus"));

  const appJs = fs.readFileSync(path.join(webDir, "js/app.js"), "utf8");
  assert.ok(appJs.includes("data-email-preview-send-test"));
  assert.ok(appJs.includes("adminRequestTestEmail"));
  assert.ok(appJs.includes("adminSubscribeTestEmailStatus"));
});























