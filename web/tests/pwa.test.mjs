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
    path.join(webDir, "js/firebase.js"),
    path.join(webDir, "js/gallery-share.js"),
    path.join(webDir, "js/i18n.js"),
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

  // HTML & CSS verification
  const html = fs.readFileSync(path.join(webDir, "index.html"), "utf8");
  assert.ok(html.includes('data-cat-count="all"'), "Catalog All filter must include count pill badge");
  assert.ok(html.includes('data-cat-count="photography"'), "Catalog Photography filter must include count pill badge");
  assert.ok(html.includes('data-admin-count="bookings"'), "Admin filter bar must include count pill badge");
  assert.ok(html.includes('id="lightboxShareBtn"'), "Gallery lightbox must include Share button");

  const css = fs.readFileSync(path.join(webDir, "styles.css"), "utf8");
  for (const selector of [".filter-count-badge", ".chip-btn.active .filter-count-badge", ".lightbox-footer", ".lightbox-share-btn"]) {
    assert.ok(css.includes(selector), `CSS missing rule for ${selector}`);
  }
});




