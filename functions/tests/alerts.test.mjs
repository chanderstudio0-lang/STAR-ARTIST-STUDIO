import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {
  FIRESTORE_DATABASE_ID,
  DEFAULT_ADMIN_EMAIL,
  resolveSmtpConfig
} from "../src/config.js";
import {
  escapeHtml,
  sanitizeHeaderValue,
  safeHttpUrl,
  buildNewBookingAlertEmail,
  buildNewModelApplicationAlertEmail
} from "../src/email-templates.js";
import {
  handleNewBookingCreated,
  handleNewModelApplicationCreated
} from "../src/handlers.js";

const functionsDir = path.resolve(import.meta.dirname, "..");
const rootDir = path.resolve(functionsDir, "..");

test("resolveSmtpConfig defaults to chanderstudio0@gmail.com and reads SMTP secrets strictly from env/params", () => {
  const cfg = resolveSmtpConfig(
    { SMTP_HOST: "smtp.gmail.com", SMTP_PORT: "465" },
    { smtpUser: "chanderstudio0@gmail.com", smtpPass: "app-pass-placeholder" }
  );
  assert.equal(cfg.host, "smtp.gmail.com");
  assert.equal(cfg.port, 465);
  assert.equal(cfg.secure, true);
  assert.equal(cfg.adminEmail, DEFAULT_ADMIN_EMAIL);
  assert.equal(cfg.user, "chanderstudio0@gmail.com");
  assert.equal(cfg.pass, "app-pass-placeholder");
  assert.ok(cfg.from.includes("STAR ARTIST STUDIO Alerts"));

  // Invalid override falls back safely to DEFAULT_ADMIN_EMAIL
  const fallbackCfg = resolveSmtpConfig({ ADMIN_ALERT_EMAIL: "not-an-email" });
  assert.equal(fallbackCfg.adminEmail, DEFAULT_ADMIN_EMAIL);
});

test("buildNewBookingAlertEmail escapes untrusted HTML, strips header injection, and includes booking details", () => {
  const email = buildNewBookingAlertEmail({
    bookingId: "bk_901",
    booking: {
      clientName: "Riya\r\nBcc: attacker@evil.com <script>alert(1)</script>",
      contactNumber: "+91 9812890081",
      serviceType: "High-Fashion Editorial - Pro",
      preferredDate: "2026-12-12",
      preferredTime: "10:00 AM",
      location: "Studio (Talwara Khurd)",
      status: "Pending",
      notes: 'Bring <b>Gold</b> "backdrop"',
      createdAt: { toDate: () => new Date("2026-11-21T06:00:00.000Z") }
    }
  });

  assert.equal(/[\r\n]/.test(email.subject), false, "Subject must never contain CR/LF characters");
  assert.ok(email.subject.includes("[New Booking]"));
  assert.ok(email.html.includes("&lt;script&gt;alert(1)&lt;/script&gt;"));
  assert.equal(email.html.includes("<script>"), false, "HTML email must escape script tags");
  assert.ok(email.html.includes("&lt;b&gt;Gold&lt;/b&gt; &quot;backdrop&quot;"));
  assert.ok(email.html.includes("bk_901"));
  assert.ok(email.html.includes("+91 9812890081"));
  assert.ok(email.text.includes("High-Fashion Editorial - Pro"));
});

test("buildNewModelApplicationAlertEmail validates portfolio URLs, shows 18+ compliance, and escapes untrusted fields", () => {
  const safeEmail = buildNewModelApplicationAlertEmail({
    modelId: "mdl_404",
    model: {
      fullName: "Aarav Verma",
      age: 22,
      gender: "Male",
      city: "Sirsa",
      phoneNumber: "9876543210",
      skills: "Runway, Editorial",
      portfolioUrls: "https://instagram.com/aarav_model",
      consentGiven: true,
      status: "pending",
      createdAt: "2026-11-21T08:00:00.000Z"
    }
  });

  assert.ok(safeEmail.subject.includes("[New 18+ Model Application] Aarav Verma — Sirsa"));
  assert.ok(safeEmail.html.includes("22 yrs (18+ Eligible)"));
  assert.ok(safeEmail.html.includes("Signed &amp; Confirmed (18+)"));
  assert.ok(safeEmail.html.includes('href="https://instagram.com/aarav_model"'));

  // Blocks javascript: portfolio URLs from becoming clickable links
  const unsafeEmail = buildNewModelApplicationAlertEmail({
    modelId: "mdl_405",
    model: {
      fullName: "Malicious <img src=x onerror=alert(1)>",
      age: 19,
      city: "Hisar",
      phoneNumber: "9876543211",
      portfolioUrls: "javascript:alert(1)",
      consentGiven: true,
      status: "pending"
    }
  });
  assert.equal(unsafeEmail.html.includes('href="javascript:'), false);
  assert.equal(unsafeEmail.html.includes("<img src=x"), false);
  assert.equal(safeHttpUrl("javascript:alert(1)"), "");
  assert.equal(sanitizeHeaderValue("Hello\r\nWorld"), "Hello World");
  assert.equal(escapeHtml("<&>\"'"), "&lt;&amp;&gt;&quot;&#39;");
});

test("handleNewBookingCreated and handleNewModelApplicationCreated dispatch emails to chanderstudio0@gmail.com and skip empty snapshots", async () => {
  const sentMessages = [];
  const fakeTransporter = {
    async sendMail(msg) {
      sentMessages.push(msg);
      return { messageId: `msg_${sentMessages.length}` };
    }
  };
  const silentLogger = { info: () => {}, warn: () => {}, error: () => {} };

  // 1. Booking trigger
  const bookingRes = await handleNewBookingCreated(
    {
      params: { bookingId: "bk_live_1" },
      data: {
        id: "bk_live_1",
        data: () => ({
          clientName: "Simran Kaur",
          contactNumber: "9812890081",
          serviceType: "Bridal Photography - Flagship",
          preferredDate: "2026-12-22",
          status: "Pending",
          createdAt: new Date("2026-11-21T05:45:00.000Z")
        })
      }
    },
    {
      env: {},
      smtpUser: "chanderstudio0@gmail.com",
      smtpPass: "secret-test-token",
      transporter: fakeTransporter,
      logger: silentLogger
    }
  );

  assert.equal(bookingRes.sent, true);
  assert.equal(bookingRes.type, "booking");
  assert.equal(bookingRes.recipient, "chanderstudio0@gmail.com");
  assert.equal(sentMessages.length, 1);
  assert.equal(sentMessages[0].to, "chanderstudio0@gmail.com");
  assert.ok(sentMessages[0].subject.includes("Simran Kaur"));

  // 2. 18+ Model trigger
  const modelRes = await handleNewModelApplicationCreated(
    {
      params: { modelId: "mdl_live_1" },
      data: {
        id: "mdl_live_1",
        data: () => ({
          fullName: "Meera Gill",
          age: 21,
          city: "Ellenabad",
          phoneNumber: "9812890082",
          skills: "Bridal & Fashion",
          consentGiven: true,
          status: "pending",
          createdAt: new Date("2026-11-21T05:50:00.000Z")
        })
      }
    },
    {
      env: {},
      smtpUser: "chanderstudio0@gmail.com",
      smtpPass: "secret-test-token",
      transporter: fakeTransporter,
      logger: silentLogger
    }
  );

  assert.equal(modelRes.sent, true);
  assert.equal(modelRes.type, "model");
  assert.equal(modelRes.recipient, "chanderstudio0@gmail.com");
  assert.equal(sentMessages.length, 2);
  assert.ok(sentMessages[1].subject.includes("Meera Gill"));

  // 3. Empty snapshot skips cleanly
  const emptyRes = await handleNewBookingCreated(
    { params: { bookingId: "bk_empty" }, data: { data: () => undefined } },
    { transporter: fakeTransporter, logger: silentLogger }
  );
  assert.equal(emptyRes.sent, false);
  assert.equal(emptyRes.reason, "empty_snapshot");
});

test("functions/index.js and firebase.json bind the exact named Firestore database and contain zero hardcoded secrets", () => {
  const indexSrc = fs.readFileSync(path.join(functionsDir, "index.js"), "utf8");
  assert.ok(indexSrc.includes('document: "bookings/{bookingId}"'));
  assert.ok(indexSrc.includes('document: "models/{modelId}"'));
  assert.ok(indexSrc.includes("database: FIRESTORE_DATABASE_ID"));
  assert.ok(indexSrc.includes('defineSecret("SMTP_USER")'));
  assert.ok(indexSrc.includes('defineSecret("SMTP_PASS")'));
  assert.equal(FIRESTORE_DATABASE_ID, "ai-studio-android-stararti-4ffde0d6-02fd-4194-8e31-3359a6b9588b");

  const firebaseJson = JSON.parse(fs.readFileSync(path.join(rootDir, "firebase.json"), "utf8"));
  assert.ok(Array.isArray(firebaseJson.functions), "firebase.json must register functions codebase");
  assert.equal(firebaseJson.functions[0].source, "functions");
  assert.equal(firebaseJson.firestore[0].database, FIRESTORE_DATABASE_ID);
  assert.equal(firebaseJson.hosting.public, "web");

  // Ensure zero hardcoded passwords or API keys in functions/
  for (const rel of [
    "index.js",
    "src/config.js",
    "src/template-engine.js",
    "src/email-templates.js",
    "src/mailer.js",
    "src/handlers.js"
  ]) {
    const src = fs.readFileSync(path.join(functionsDir, rel), "utf8");
    assert.equal(/AIza[0-9A-Za-z\-_]{30,}/.test(src), false, `API key pattern found in functions/${rel}`);
    assert.equal(/-----BEGIN PRIVATE KEY-----/.test(src), false, `Private key found in functions/${rel}`);
  }
  assert.ok(indexSrc.includes('document: "companyBookings/{requestId}"'));
});

test("Customizable HTML template engine loads external templates, prevents path traversal, and applies brand & subject overrides safely", async () => {
  const { resolveEmailBrandConfig, sanitizeHexColor } = await import("../src/config.js");
  const {
    loadHtmlTemplate,
    renderHtmlTemplate,
    renderSubjectTemplate,
    clearTemplateCache
  } = await import("../src/template-engine.js");
  const {
    buildNewCompanyBookingAlertEmail,
    safeTelHref
  } = await import("../src/email-templates.js");
  const { handleNewCompanyBookingCreated } = await import("../src/handlers.js");

  clearTemplateCache();

  // 1. All customizable HTML template files exist on disk
  for (const tpl of [
    "base-layout.html",
    "booking-alert.html",
    "model-application-alert.html",
    "company-booking-alert.html"
  ]) {
    const content = loadHtmlTemplate(tpl);
    assert.ok(content.length > 100, `Template ${tpl} should be non-empty HTML`);
  }

  // 2. Path traversal is strictly blocked
  assert.throws(() => loadHtmlTemplate("../package.json"), /Invalid template filename/);
  assert.throws(() => loadHtmlTemplate("../../etc/passwd"), /Invalid template filename/);

  // 3. Auto-escaping {{token}} vs trusted {{{partial}}}
  const rendered = renderHtmlTemplate(
    "<div>{{untrusted}} | {{{trustedBadge}}}</div>",
    { untrusted: '<script>alert("xss")</script>' },
    { trustedBadge: '<span class="gold">VIP</span>' }
  );
  assert.equal(
    rendered,
    '<div>&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt; | <span class="gold">VIP</span></div>'
  );
  assert.equal(
    renderSubjectTemplate("[VIP] {{name}}\r\nBcc: evil@test.com", { name: "Kabir\nSingh" }),
    "[VIP] Kabir Singh Bcc: evil@test.com"
  );

  // 4. Brand config sanitizes hex colors & URLs and honors env/override customization
  assert.equal(sanitizeHexColor("#C59B27", "#D4AF37"), "#C59B27");
  assert.equal(sanitizeHexColor("red; background:url(evil)", "#D4AF37"), "#D4AF37");
  assert.equal(safeTelHref("+91 98128-90081"), "tel:+919812890081");
  assert.equal(safeTelHref("abc"), "");

  const customBrand = resolveEmailBrandConfig(
    {
      EMAIL_BRAND_NAME: "STAR ARTIST LUXURY",
      EMAIL_ACCENT_COLOR: "#E5C158",
      EMAIL_LOGO_URL: "https://gen-lang-client-0548683163.web.app/images/icon-192.png",
      EMAIL_FOOTER_TEXT: "Custom Sirsa Studio Footer"
    },
    {
      accentColor: "not-a-hex", // ignored -> falls back to default hex safely
      bookingCtaLabel: "Review VIP Booking"
    }
  );
  assert.equal(customBrand.brandName, "STAR ARTIST LUXURY");
  assert.equal(customBrand.accentColor, "#D4AF37");
  assert.equal(customBrand.logoUrl, "https://gen-lang-client-0548683163.web.app/images/icon-192.png");
  assert.equal(customBrand.bookingCtaLabel, "Review VIP Booking");

  // 5. Booking email applies custom branding, logo <img>, custom subject template, and clickable tel: CTA
  const customBookingEmail = buildNewBookingAlertEmail({
    bookingId: "bk_custom_1",
    booking: {
      clientName: "Ananya Verma",
      contactNumber: "+91 9812890081",
      serviceType: "Bridal Royal Shoot",
      preferredDate: "2026-12-25",
      preferredTime: "11:00 AM",
      location: "Sirsa Heritage Palace",
      status: "Pending",
      notes: "Golden hour portraits required"
    },
    templateOverrides: {
      brandName: "STAR ARTIST VIP",
      logoUrl: "https://gen-lang-client-0548683163.web.app/images/icon-192.png",
      accentColor: "#E5C158",
      bookingSubjectTemplate: "VIP ALERT: {{clientName}} booked {{serviceType}}",
      bookingCtaLabel: "Open VIP Console",
      footerText: "VIP Automated Studio Dispatch"
    }
  });
  assert.equal(customBookingEmail.subject, "VIP ALERT: Ananya Verma booked Bridal Royal Shoot");
  assert.ok(customBookingEmail.html.includes("STAR ARTIST VIP"));
  assert.ok(customBookingEmail.html.includes('src="https://gen-lang-client-0548683163.web.app/images/icon-192.png"'));
  assert.ok(customBookingEmail.html.includes("#E5C158"));
  assert.ok(customBookingEmail.html.includes("Open VIP Console"));
  assert.ok(customBookingEmail.html.includes("VIP Automated Studio Dispatch"));
  assert.ok(customBookingEmail.html.includes('href="tel:+919812890081"'));
  assert.ok(customBookingEmail.html.includes("Golden hour portraits required"));

  // 6. Company model requirement email + runtime Firestore override loader & fallback
  const companyEmail = buildNewCompanyBookingAlertEmail({
    requestId: "comp_77",
    request: {
      companyName: "Royal Heritage <script>x</script> Events",
      organizationType: "Event Organizer",
      contactPerson: "Vikram Singh",
      mobileNumber: "+91 9812890081",
      email: "events@royalheritage.in",
      address: "Mall Road, Sirsa",
      modelRequirement: "Both",
      modelsCount: 6,
      eventType: "Fashion Show",
      eventDate: "2026-12-29",
      eventLocation: "Grand Ballroom Sirsa",
      budget: "₹90,000",
      detailedRequirements: "Need 3 male & 3 female runway models for festive showcase.",
      requestedModelNames: ["Kavya Arora", "Aarav Kapoor"],
      status: "Pending"
    }
  });
  assert.ok(companyEmail.subject.includes("[Company Booking]"));
  assert.ok(companyEmail.subject.includes("6 Both"));
  assert.equal(companyEmail.html.includes("<script>"), false);
  assert.ok(companyEmail.html.includes("Kavya Arora, Aarav Kapoor"));
  assert.ok(companyEmail.html.includes("Manage Company Requests"));

  const sent = [];
  const res = await handleNewCompanyBookingCreated(
    {
      params: { requestId: "comp_live_1" },
      data: {
        id: "comp_live_1",
        data: () => ({
          companyName: "Glamour Productions",
          contactPerson: "Neha Sethi",
          mobileNumber: "+91 9812890081",
          email: "neha@glamour.in",
          address: "Sirsa",
          modelRequirement: "Female Models",
          modelsCount: 4,
          eventType: "Brand Promotion",
          eventDate: "2026-12-30",
          eventLocation: "Hisar Plaza",
          budget: "₹55,000",
          detailedRequirements: "4 female models for luxury jewelry launch.",
          status: "Pending"
        })
      }
    },
    {
      smtpUser: "chanderstudio0@gmail.com",
      smtpPass: "secret-test-token",
      loadTemplateOverrides: async () => {
        throw new Error("Simulated Firestore read timeout");
      },
      transporter: {
        async sendMail(msg) {
          sent.push(msg);
          return { messageId: "msg_comp_1" };
        }
      },
      logger: { info: () => {}, warn: () => {}, error: () => {} }
    }
  );
  assert.equal(res.sent, true);
  assert.equal(res.type, "companyBooking");
  assert.equal(sent.length, 1);
  assert.ok(sent[0].html.includes("Glamour Productions"));
});

test("handleAdminTestEmailRequested verifies SMTP, sends branded [TEST] HTML email strictly to chanderstudio0@gmail.com, blocks unauthorized callers, and redacts secrets on error", async () => {
  const { handleAdminTestEmailRequested } = await import("../src/handlers.js");

  // 1. Authorized admin triggers test email -> verify() called, [TEST] email dispatched, status written back
  const sentMessages = [];
  const statusUpdates = [];
  let verifyCalls = 0;
  const fakeTransporter = {
    async verify() {
      verifyCalls += 1;
      return true;
    },
    async sendMail(msg) {
      sentMessages.push(msg);
      return { messageId: "smtp_test_msg_001" };
    }
  };
  const silentLogger = { info: () => {}, warn: () => {}, error: () => {} };

  const okRes = await handleAdminTestEmailRequested(
    {
      params: { testId: "emailtest_001" },
      data: {
        id: "emailtest_001",
        data: () => ({
          requestedByEmail: "chanderstudio0@gmail.com",
          recipientEmail: "attacker@example.com", // Ignored: server forces chanderstudio0@gmail.com
          templateType: "booking",
          recordId: "bk_sample_01",
          recordSnapshot: {
            clientName: "Priya Kapoor",
            contactNumber: "+91 9812890081",
            serviceType: "Royal Bridal Film",
            preferredDate: "2026-12-28",
            status: "Pending"
          },
          templateOverrides: {
            brandName: "STAR ARTIST TEST BRAND",
            accentColor: "#E5B842"
          },
          status: "queued"
        })
      }
    },
    {
      smtpUser: "chanderstudio0@gmail.com",
      smtpPass: "super-secret-app-password",
      transporter: fakeTransporter,
      updateStatusDoc: async (patch) => statusUpdates.push(patch),
      logger: silentLogger
    }
  );

  assert.equal(okRes.sent, true);
  assert.equal(okRes.smtpVerified, true);
  assert.equal(verifyCalls, 1, "Must verify SMTP transport before dispatching test email");
  assert.equal(sentMessages.length, 1);
  assert.equal(sentMessages[0].to, "chanderstudio0@gmail.com", "Must strictly send to authorized admin email");
  assert.ok(sentMessages[0].subject.startsWith("[TEST] "), "Test subject must include [TEST] prefix");
  assert.ok(sentMessages[0].html.includes("STAR ARTIST TEST BRAND"));
  assert.ok(sentMessages[0].html.includes("Priya Kapoor"));
  assert.equal(statusUpdates.length, 1);
  assert.equal(statusUpdates[0].status, "sent");
  assert.equal(statusUpdates[0].smtpVerified, true);
  assert.equal(statusUpdates[0].messageId, "smtp_test_msg_001");

  // 2. Unauthorized requester is strictly rejected
  const rejectedPatches = [];
  const rejectedRes = await handleAdminTestEmailRequested(
    {
      params: { testId: "emailtest_unauth" },
      data: {
        id: "emailtest_unauth",
        data: () => ({
          requestedByEmail: "intruder@example.com",
          templateType: "model",
          status: "queued"
        })
      }
    },
    {
      smtpUser: "chanderstudio0@gmail.com",
      smtpPass: "super-secret-app-password",
      transporter: fakeTransporter,
      updateStatusDoc: async (patch) => rejectedPatches.push(patch),
      logger: silentLogger
    }
  );
  assert.equal(rejectedRes.sent, false);
  assert.equal(rejectedRes.reason, "unauthorized_requester");
  assert.equal(sentMessages.length, 1, "Unauthorized caller must never dispatch mail");
  assert.equal(rejectedPatches[0].status, "failed");

  // 3. SMTP failure writes sanitized error status and redacts SMTP_PASS
  const failedPatches = [];
  const failingTransporter = {
    async verify() {
      throw new Error("Invalid login for super-secret-app-password on smtp.gmail.com");
    },
    async sendMail() {
      throw new Error("Should not call sendMail after verify fails");
    }
  };
  const failRes = await handleAdminTestEmailRequested(
    {
      params: { testId: "emailtest_fail" },
      data: {
        id: "emailtest_fail",
        data: () => ({
          requestedByEmail: "chanderstudio0@gmail.com",
          templateType: "company",
          status: "queued"
        })
      }
    },
    {
      smtpUser: "chanderstudio0@gmail.com",
      smtpPass: "super-secret-app-password",
      transporter: failingTransporter,
      updateStatusDoc: async (patch) => failedPatches.push(patch),
      logger: silentLogger
    }
  );
  assert.equal(failRes.sent, false);
  assert.equal(failRes.smtpVerified, false);
  assert.equal(failedPatches[0].status, "failed");
  assert.equal(failedPatches[0].errorMessage.includes("super-secret-app-password"), false, "Must redact SMTP password");
  assert.ok(failedPatches[0].errorMessage.includes("[REDACTED]"));

  // 4. functions/index.js exports sendAdminTestNotificationEmail bound to adminEmailTests/{testId}
  const indexSrc = fs.readFileSync(path.join(functionsDir, "index.js"), "utf8");
  assert.ok(indexSrc.includes("export const sendAdminTestNotificationEmail"));
  assert.ok(indexSrc.includes('document: "adminEmailTests/{testId}"'));
});


