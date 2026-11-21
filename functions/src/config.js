export const STUDIO_NAME = "STAR ARTIST STUDIO";
export const STUDIO_TAGLINE = "Luxury Fashion • Modeling • Bridal & Editorial Production";
export const STUDIO_LOCATION = "Talwara Khurd, Sirsa, Haryana - 125102";
export const STUDIO_PHONE = "+91 9812890081";
export const FIRESTORE_DATABASE_ID = "ai-studio-android-stararti-4ffde0d6-02fd-4194-8e31-3359a6b9588b";
export const FUNCTIONS_REGION = "asia-south1";
export const DEFAULT_ADMIN_EMAIL = "chanderstudio0@gmail.com";
export const DEFAULT_DASHBOARD_URL = "https://gen-lang-client-0548683163.web.app/#admin";

const SIMPLE_EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const HEX_COLOR_RE = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i;

export function sanitizeHexColor(value, fallback) {
  const clean = String(value ?? "").trim();
  return HEX_COLOR_RE.test(clean) ? clean : fallback;
}

export function safeBrandUrl(rawUrl) {
  const clean = String(rawUrl ?? "").trim();
  if (!clean) return "";
  try {
    const parsed = new URL(clean);
    if (parsed.protocol === "https:" || parsed.protocol === "http:") {
      return parsed.href;
    }
    return "";
  } catch {
    return "";
  }
}

/**
 * Resolves customizable brand styling, template paths, and copy overrides
 * from environment variables and optional Firestore/runtime overrides.
 */
export function resolveEmailBrandConfig(env = process.env, overrides = {}) {
  const pick = (overrideKey, envKey, fallback = "") => {
    const ov = overrides && overrides[overrideKey] !== undefined ? String(overrides[overrideKey]).trim() : "";
    if (ov) return ov;
    const ev = env && env[envKey] !== undefined ? String(env[envKey]).trim() : "";
    if (ev) return ev;
    return fallback;
  };

  const brandName = pick("brandName", "EMAIL_BRAND_NAME", STUDIO_NAME).slice(0, 80);
  const brandTagline = pick("brandTagline", "EMAIL_BRAND_TAGLINE", STUDIO_TAGLINE).slice(0, 140);
  const studioLocation = pick("studioLocation", "EMAIL_STUDIO_LOCATION", STUDIO_LOCATION).slice(0, 160);
  const logoUrl = safeBrandUrl(pick("logoUrl", "EMAIL_LOGO_URL", ""));
  const dashboardUrl = safeBrandUrl(pick("dashboardUrl", "STUDIO_DASHBOARD_URL", DEFAULT_DASHBOARD_URL)) || DEFAULT_DASHBOARD_URL;

  const accentColor = sanitizeHexColor(pick("accentColor", "EMAIL_ACCENT_COLOR", "#D4AF37"), "#D4AF37");
  const accentLightColor = sanitizeHexColor(pick("accentLightColor", "EMAIL_ACCENT_LIGHT_COLOR", "#F3D87C"), "#F3D87C");
  const bgColor = sanitizeHexColor(pick("bgColor", "EMAIL_BG_COLOR", "#0C0C0C"), "#0C0C0C");
  const surfaceColor = sanitizeHexColor(pick("surfaceColor", "EMAIL_SURFACE_COLOR", "#151412"), "#151412");
  const cardColor = sanitizeHexColor(pick("cardColor", "EMAIL_CARD_COLOR", "#0F0E0C"), "#0F0E0C");
  const textColor = sanitizeHexColor(pick("textColor", "EMAIL_TEXT_COLOR", "#F5F5F4"), "#F5F5F4");
  const mutedTextColor = sanitizeHexColor(pick("mutedTextColor", "EMAIL_MUTED_TEXT_COLOR", "#C9C2B5"), "#C9C2B5");

  const footerText = pick(
    "footerText",
    "EMAIL_FOOTER_TEXT",
    "Automated Admin Alert • Restricted strictly to the authorized Studio Administrator."
  ).slice(0, 220);

  const bookingSubjectTemplate = pick(
    "bookingSubjectTemplate",
    "EMAIL_BOOKING_SUBJECT_TEMPLATE",
    "[New Booking] {{clientName}} — {{serviceType}} ({{preferredDate}})"
  ).slice(0, 160);

  const modelSubjectTemplate = pick(
    "modelSubjectTemplate",
    "EMAIL_MODEL_SUBJECT_TEMPLATE",
    "[New 18+ Model Application] {{fullName}} — {{city}}"
  ).slice(0, 160);

  const companySubjectTemplate = pick(
    "companySubjectTemplate",
    "EMAIL_COMPANY_SUBJECT_TEMPLATE",
    "[Company Booking] {{companyName}} — {{modelsCount}} {{modelRequirement}} ({{eventDate}})"
  ).slice(0, 160);

  const bookingCtaLabel = pick("bookingCtaLabel", "EMAIL_BOOKING_CTA_LABEL", "Open Admin Bookings").slice(0, 60);
  const modelCtaLabel = pick("modelCtaLabel", "EMAIL_MODEL_CTA_LABEL", "Review Model Application").slice(0, 60);
  const companyCtaLabel = pick("companyCtaLabel", "EMAIL_COMPANY_CTA_LABEL", "Manage Company Requests").slice(0, 60);

  const bookingIntroTemplate = pick(
    "bookingIntroTemplate",
    "EMAIL_BOOKING_INTRO_TEMPLATE",
    "{{clientName}} submitted a new booking request for {{serviceType}}. Open the Admin Console with your verified Studio Admin Gmail account to confirm or manage this session."
  ).slice(0, 320);

  const modelIntroTemplate = pick(
    "modelIntroTemplate",
    "EMAIL_MODEL_INTRO_TEMPLATE",
    "{{fullName}} from {{city}} applied for the Official Studio Roster. Open the Admin Console to review their 18+ verification checklist and portfolio."
  ).slice(0, 320);

  const companyIntroTemplate = pick(
    "companyIntroTemplate",
    "EMAIL_COMPANY_INTRO_TEMPLATE",
    "{{companyName}} submitted a model requirement booking for {{eventType}} ({{modelsCount}} • {{modelRequirement}}). Review and coordinate directly from the Admin Console."
  ).slice(0, 320);

  const templateDir = pick("templateDir", "EMAIL_TEMPLATE_DIR", "");

  return {
    brandName,
    brandTagline,
    studioLocation,
    logoUrl,
    dashboardUrl,
    accentColor,
    accentLightColor,
    bgColor,
    surfaceColor,
    cardColor,
    textColor,
    mutedTextColor,
    footerText,
    bookingSubjectTemplate,
    modelSubjectTemplate,
    companySubjectTemplate,
    bookingCtaLabel,
    modelCtaLabel,
    companyCtaLabel,
    bookingIntroTemplate,
    modelIntroTemplate,
    companyIntroTemplate,
    templateDir,
    baseLayoutHtml: typeof overrides?.baseLayoutHtml === "string" ? overrides.baseLayoutHtml : "",
    bookingBodyHtml: typeof overrides?.bookingBodyHtml === "string" ? overrides.bookingBodyHtml : "",
    modelBodyHtml: typeof overrides?.modelBodyHtml === "string" ? overrides.modelBodyHtml : "",
    companyBodyHtml: typeof overrides?.companyBodyHtml === "string" ? overrides.companyBodyHtml : ""
  };
}

/**
 * Resolves SMTP & recipient settings strictly from runtime secrets / environment variables.
 * Never embeds credentials in source code.
 */
export function resolveSmtpConfig(env = process.env, { smtpUser = "", smtpPass = "" } = {}) {
  const host = String(env.SMTP_HOST || "smtp.gmail.com").trim();
  const port = Number.parseInt(String(env.SMTP_PORT || "465"), 10);
  const safePort = Number.isFinite(port) && port > 0 && port <= 65535 ? port : 465;
  const secureRaw = String(env.SMTP_SECURE ?? "").trim().toLowerCase();
  const secure = secureRaw ? secureRaw === "true" || secureRaw === "1" : safePort === 465;

  const user = String(smtpUser || env.SMTP_USER || "").trim();
  const pass = String(smtpPass || env.SMTP_PASS || "").trim();

  const rawAdminEmail = String(env.ADMIN_ALERT_EMAIL || DEFAULT_ADMIN_EMAIL).trim().toLowerCase();
  const adminEmail = SIMPLE_EMAIL_RE.test(rawAdminEmail) ? rawAdminEmail : DEFAULT_ADMIN_EMAIL;

  const fromEmail = String(env.SMTP_FROM || user || adminEmail).trim();
  const brandName = String(env.EMAIL_BRAND_NAME || STUDIO_NAME).trim() || STUDIO_NAME;
  const dashboardUrl = safeBrandUrl(env.STUDIO_DASHBOARD_URL || DEFAULT_DASHBOARD_URL) || DEFAULT_DASHBOARD_URL;

  return {
    host,
    port: safePort,
    secure,
    user,
    pass,
    adminEmail,
    from: `${brandName} Alerts <${fromEmail}>`,
    dashboardUrl
  };
}
