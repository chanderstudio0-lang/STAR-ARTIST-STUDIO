import { STUDIO_CONTACT } from "./data.js";

export const EMAIL_PREVIEW_TYPES = Object.freeze(["booking", "model", "company"]);

export const DEFAULT_EMAIL_PREVIEW_BRAND = Object.freeze({
  brandName: STUDIO_CONTACT.studioName || "STAR ARTIST STUDIO",
  brandTagline: "Luxury Fashion • Modeling • Bridal & Editorial Production",
  studioLocation: "Talwara Khurd, Sirsa, Haryana - 125102",
  dashboardUrl: "https://gen-lang-client-0548683163.web.app/#admin",
  adminEmail: STUDIO_CONTACT.adminEmail || "chanderstudio0@gmail.com",
  accentColor: "#D4AF37",
  accentLightColor: "#F3D87C",
  bgColor: "#0C0C0C",
  surfaceColor: "#151412",
  cardColor: "#0F0E0C",
  textColor: "#F5F5F4",
  mutedTextColor: "#C9C2B5",
  footerText: "Automated Admin Alert • Restricted strictly to the authorized Studio Administrator.",
  bookingSubjectTemplate: "[New Booking] {{clientName}} — {{serviceType}} ({{preferredDate}})",
  modelSubjectTemplate: "[New 18+ Model Application] {{fullName}} — {{city}}",
  companySubjectTemplate: "[Company Booking] {{companyName}} — {{modelsCount}} {{modelRequirement}} ({{eventDate}})",
  bookingIntroTemplate:
    "{{clientName}} submitted a new booking request for {{serviceType}}. Open the Admin Console with your verified Studio Admin Gmail account to confirm or manage this session.",
  modelIntroTemplate:
    "{{fullName}} from {{city}} applied for the Official Studio Roster. Open the Admin Console to review their 18+ verification checklist and portfolio.",
  companyIntroTemplate:
    "{{companyName}} submitted a model requirement booking for {{eventType}} ({{modelsCount}} • {{modelRequirement}}). Review and coordinate directly from the Admin Console.",
  bookingCtaLabel: "Open Admin Bookings",
  modelCtaLabel: "Review Model Application",
  companyCtaLabel: "Manage Company Requests"
});

export const SAMPLE_EMAIL_PREVIEW_RECORDS = Object.freeze({
  booking: Object.freeze({
    id: "bk_preview_sample_01",
    clientName: "Simranpreet Kaur",
    contactNumber: "+91 9812890081",
    serviceType: "High-Fashion Editorial — Pro Package",
    preferredDate: "2026-12-14",
    preferredTime: "11:00 AM",
    location: "Star Artist Studio Floor (Talwara Khurd, Sirsa)",
    status: "Pending",
    notes: "3 couture wardrobe changes, warm golden rim lighting, and 4K behind-the-scenes portrait reel.",
    createdAt: "2026-11-21T11:30:00.000Z"
  }),
  model: Object.freeze({
    id: "mdl_preview_sample_01",
    fullName: "Aarav Sharma",
    age: 22,
    gender: "Male",
    city: "Sirsa, Haryana",
    phoneNumber: "+91 9812890081",
    skills: "Editorial Print, Ramp Walk, Ethnic & Bridal Couture, Commercial Music Video",
    portfolioUrls: "https://gen-lang-client-0548683163.web.app/#gallery",
    consentGiven: true,
    status: "pending",
    createdAt: "2026-11-21T12:15:00.000Z"
  }),
  company: Object.freeze({
    id: "cmp_preview_sample_01",
    companyName: "Royal Heritage Couture Events",
    organizationType: "Event Organizer",
    contactPerson: "Vikram Malhotra",
    mobileNumber: "+91 9812890081",
    email: "casting@royalheritageevents.in",
    address: "Hissar Road Commercial Complex, Sirsa, Haryana",
    modelRequirement: "Both (Male & Female)",
    modelsCount: 6,
    eventType: "Fashion Show",
    eventDate: "2026-12-22",
    eventLocation: "Grand Courtyard Banquet, Sirsa",
    budget: "₹85,000",
    requestedModelNames: ["Ananya Verma", "Kabir Gill"],
    detailedRequirements:
      "6 verified 18+ runway models (3 female, 3 male) for Winter Bridal Couture Showcase. Call time 4:00 PM; hair, makeup & refreshments included.",
    status: "Pending",
    createdAt: "2026-11-21T13:00:00.000Z"
  })
});

const HEX_COLOR_RE = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i;

export function escapeEmailHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function sanitizeEmailHeader(value, maxLen = 140) {
  return String(value ?? "")
    .replace(/[\r\n\t]+/g, " ")
    .replace(/\s{2,}/g, " ")
    .trim()
    .slice(0, maxLen);
}

function sanitizeHexColor(value, fallback) {
  const clean = String(value ?? "").trim();
  return HEX_COLOR_RE.test(clean) ? clean : fallback;
}

function safeHttpUrl(rawUrl) {
  const clean = String(rawUrl ?? "").trim();
  if (!clean) return "";
  try {
    const parsed = new URL(clean);
    return parsed.protocol === "https:" || parsed.protocol === "http:" ? parsed.href : "";
  } catch {
    return "";
  }
}

function safeTelHref(rawPhone) {
  const digits = String(rawPhone ?? "").replace(/[^+\d]/g, "").trim();
  return digits.length >= 7 ? `tel:${digits}` : "";
}

function renderSubjectTemplate(template, vars, maxLen = 160) {
  const rendered = String(template || "").replace(/\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g, (_, key) =>
    vars[key] !== undefined && vars[key] !== null ? String(vars[key]) : ""
  );
  return sanitizeEmailHeader(rendered, maxLen);
}

function formatPreviewTimestamp(value) {
  if (!value) return "Just now";
  let date = null;
  if (value instanceof Date) {
    date = value;
  } else if (typeof value === "object" && typeof value.toDate === "function") {
    try {
      date = value.toDate();
    } catch {
      date = null;
    }
  } else if (typeof value === "object" && typeof value.seconds === "number") {
    date = new Date(value.seconds * 1000);
  } else if (typeof value === "string" || typeof value === "number") {
    date = new Date(value);
  }
  if (!date || Number.isNaN(date.getTime())) return "Just now";
  try {
    return (
      new Intl.DateTimeFormat("en-IN", {
        dateStyle: "medium",
        timeStyle: "short",
        timeZone: "Asia/Kolkata"
      }).format(date) + " IST"
    );
  } catch {
    return date.toISOString();
  }
}

function resolveBrand(overrides = {}) {
  const pickText = (key, maxLen) => {
    const raw = overrides && overrides[key] !== undefined ? String(overrides[key]).trim() : "";
    return (raw || DEFAULT_EMAIL_PREVIEW_BRAND[key] || "").slice(0, maxLen);
  };
  return {
    ...DEFAULT_EMAIL_PREVIEW_BRAND,
    brandName: pickText("brandName", 80),
    brandTagline: pickText("brandTagline", 140),
    studioLocation: pickText("studioLocation", 160),
    footerText: pickText("footerText", 220),
    accentColor: sanitizeHexColor(overrides?.accentColor, DEFAULT_EMAIL_PREVIEW_BRAND.accentColor),
    accentLightColor: sanitizeHexColor(overrides?.accentLightColor, DEFAULT_EMAIL_PREVIEW_BRAND.accentLightColor),
    bookingSubjectTemplate: pickText("bookingSubjectTemplate", 160),
    modelSubjectTemplate: pickText("modelSubjectTemplate", 160),
    companySubjectTemplate: pickText("companySubjectTemplate", 160),
    bookingCtaLabel: pickText("bookingCtaLabel", 60),
    modelCtaLabel: pickText("modelCtaLabel", 60),
    companyCtaLabel: pickText("companyCtaLabel", 60)
  };
}

function renderBrandMarkHtml(brand) {
  const monogram =
    String(brand.brandName || "SA")
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((w) => w[0].toUpperCase())
      .join("") || "SA";

  return `<table role="presentation" cellspacing="0" cellpadding="0" border="0">
    <tr>
      <td valign="middle" style="padding-right:10px;">
        <div style="width:36px;height:36px;line-height:36px;text-align:center;border-radius:10px;background-color:#261f0e;border:1px solid ${escapeEmailHtml(brand.accentColor)};color:${escapeEmailHtml(brand.accentLightColor)};font-size:14px;font-weight:900;letter-spacing:0.06em;">
          ${escapeEmailHtml(monogram)}
        </div>
      </td>
      <td valign="middle">
        <div style="font-size:13px;font-weight:800;letter-spacing:0.12em;color:${escapeEmailHtml(brand.accentLightColor)};text-transform:uppercase;">${escapeEmailHtml(brand.brandName)}</div>
        <div style="font-size:11px;color:${escapeEmailHtml(brand.mutedTextColor)};margin-top:2px;">${escapeEmailHtml(brand.brandTagline)}</div>
      </td>
    </tr>
  </table>`;
}

function renderDetailsRowsHtml(rows, brand) {
  const visible = rows.filter((r) => r && r.value !== undefined && r.value !== null && String(r.value).trim() !== "");
  return visible
    .map((row, idx) => {
      const isLast = idx === visible.length - 1;
      const borderStyle = isLast ? "" : "border-bottom:1px solid #262115;";
      const rowBg = idx % 2 === 1 ? "background-color:#13110e;" : "";
      return `
      <tr style="${rowBg}">
        <td style="padding:11px 14px;${borderStyle}color:${escapeEmailHtml(brand.mutedTextColor)};font-size:12px;font-weight:700;letter-spacing:0.03em;width:36%;vertical-align:top;">
          ${escapeEmailHtml(row.label)}
        </td>
        <td style="padding:11px 14px;${borderStyle}color:${escapeEmailHtml(brand.textColor)};font-size:13.5px;line-height:1.45;vertical-align:top;word-break:break-word;">
          ${row.htmlValue || escapeEmailHtml(row.value)}
        </td>
      </tr>`;
    })
    .join("");
}

function renderNotesCalloutHtml({ title, content, brand }) {
  const clean = String(content ?? "").trim();
  if (!clean) return "";
  return `
  <div style="margin-top:16px;padding:14px 16px;border-radius:12px;background-color:#18140c;border:1px solid #352b14;border-left:3px solid ${escapeEmailHtml(brand.accentColor)};">
    <div style="font-size:11px;font-weight:800;letter-spacing:0.1em;text-transform:uppercase;color:${escapeEmailHtml(brand.accentColor)};margin-bottom:5px;">
      ${escapeEmailHtml(title)}
    </div>
    <div style="font-size:13.5px;line-height:1.55;color:${escapeEmailHtml(brand.textColor)};white-space:pre-line;word-break:break-word;">
      ${escapeEmailHtml(clean)}
    </div>
  </div>`;
}

function renderSecondaryActionHtml({ href, label, brand }) {
  if (!href || !label) return "";
  return `&nbsp;&nbsp;<a href="${escapeEmailHtml(href)}" style="display:inline-block;margin-top:8px;padding:12px 20px;border-radius:999px;background-color:transparent;border:1px solid ${escapeEmailHtml(brand.accentColor)};color:${escapeEmailHtml(brand.accentLightColor)};font-size:13px;font-weight:700;text-decoration:none;">
    ${escapeEmailHtml(label)}
  </a>`;
}

function renderBrandedEmailDocument({
  brand,
  documentTitle,
  preheader,
  eyebrow,
  heading,
  intro,
  statusBadgeLabel,
  badgeBgColor = "#2a2210",
  badgeBorderColor = brand.accentColor,
  badgeTextColor = brand.accentLightColor,
  bodyHtml,
  ctaLabel,
  secondaryActionHtml = ""
}) {
  const safeCtaUrl = safeHttpUrl(brand.dashboardUrl) || DEFAULT_EMAIL_PREVIEW_BRAND.dashboardUrl;
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${escapeEmailHtml(documentTitle)}</title>
</head>
<body style="margin:0;padding:0;background-color:${escapeEmailHtml(brand.bgColor)};color:${escapeEmailHtml(brand.textColor)};font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;-webkit-font-smoothing:antialiased;">
  <div style="display:none;max-height:0;overflow:hidden;font-size:1px;line-height:1px;color:${escapeEmailHtml(brand.bgColor)};opacity:0;">
    ${escapeEmailHtml(preheader)}
  </div>
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color:${escapeEmailHtml(brand.bgColor)};padding:24px 12px;">
    <tr>
      <td align="center">
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="max-width:620px;margin:0 auto;background-color:${escapeEmailHtml(brand.surfaceColor)};border:1px solid ${escapeEmailHtml(brand.accentColor)};border-radius:18px;overflow:hidden;box-shadow:0 16px 40px rgba(0,0,0,0.45);">
          <tr>
            <td style="height:4px;background-color:${escapeEmailHtml(brand.accentColor)};background:linear-gradient(90deg,${escapeEmailHtml(brand.accentColor)},${escapeEmailHtml(brand.accentLightColor)},${escapeEmailHtml(brand.accentColor)});font-size:0;line-height:0;">&nbsp;</td>
          </tr>
          <tr>
            <td style="padding:24px 24px 20px;background-color:#1a160e;background:linear-gradient(135deg,#211b0f 0%,${escapeEmailHtml(brand.surfaceColor)} 100%);border-bottom:1px solid #302714;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
                <tr>
                  <td valign="middle" style="padding-bottom:14px;">
                    ${renderBrandMarkHtml(brand)}
                  </td>
                  <td align="right" valign="middle" style="padding-bottom:14px;">
                    <span style="display:inline-block;padding:5px 12px;border-radius:999px;background-color:${escapeEmailHtml(badgeBgColor)};border:1px solid ${escapeEmailHtml(badgeBorderColor)};color:${escapeEmailHtml(badgeTextColor)};font-size:11px;font-weight:800;letter-spacing:0.08em;text-transform:uppercase;">
                      ${escapeEmailHtml(statusBadgeLabel)}
                    </span>
                  </td>
                </tr>
              </table>
              <div style="font-size:11px;font-weight:800;letter-spacing:0.16em;text-transform:uppercase;color:${escapeEmailHtml(brand.accentColor)};margin-bottom:6px;">
                ${escapeEmailHtml(eyebrow)}
              </div>
              <h1 style="margin:0 0 8px;font-size:22px;font-weight:800;line-height:1.25;color:${escapeEmailHtml(brand.accentLightColor)};">
                ${escapeEmailHtml(heading)}
              </h1>
              <p style="margin:0;font-size:14px;line-height:1.6;color:${escapeEmailHtml(brand.mutedTextColor)};">
                ${escapeEmailHtml(intro)}
              </p>
            </td>
          </tr>
          <tr>
            <td style="padding:20px 24px 24px;">
              ${bodyHtml}
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="margin-top:22px;">
                <tr>
                  <td align="center">
                    <a href="${escapeEmailHtml(safeCtaUrl)}" style="display:inline-block;padding:12px 26px;border-radius:999px;background-color:${escapeEmailHtml(brand.accentColor)};color:#141002;font-size:14px;font-weight:800;letter-spacing:0.03em;text-decoration:none;">
                      ${escapeEmailHtml(ctaLabel)}
                    </a>
                    ${secondaryActionHtml}
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="padding:16px 24px;background-color:#100f0d;border-top:1px solid #282216;text-align:center;">
              <div style="font-size:12px;font-weight:700;letter-spacing:0.08em;color:${escapeEmailHtml(brand.accentColor)};margin-bottom:4px;">
                ${escapeEmailHtml(brand.brandName)}
              </div>
              <div style="font-size:12px;color:${escapeEmailHtml(brand.mutedTextColor)};line-height:1.5;">
                ${escapeEmailHtml(brand.studioLocation)}
              </div>
              <div style="font-size:11px;color:#8a8172;margin-top:6px;line-height:1.5;">
                ${escapeEmailHtml(brand.footerText)}
              </div>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>`;
}

function renderHighlightCardsHtml(cards, brand) {
  const cells = cards
    .map((card, index) => {
      const pad = index === 0 ? "0 5px 0 0" : index === cards.length - 1 ? "0 0 0 5px" : "0 5px";
      const valColor = card.valueColor || brand.textColor;
      const subHtml = card.sub
        ? `<div style="font-size:11px;color:${escapeEmailHtml(brand.mutedTextColor)};margin-top:2px;">${escapeEmailHtml(card.sub)}</div>`
        : "";
      return `
      <td width="33.33%" valign="top" style="padding:${pad};">
        <div style="background-color:${escapeEmailHtml(brand.cardColor)};border:1px solid #2e2718;border-radius:12px;padding:11px;">
          <div style="font-size:10px;font-weight:700;letter-spacing:0.1em;text-transform:uppercase;color:${escapeEmailHtml(brand.accentColor)};margin-bottom:4px;">${escapeEmailHtml(card.label)}</div>
          <div style="font-size:13px;font-weight:800;color:${escapeEmailHtml(valColor)};line-height:1.3;word-break:break-word;">${escapeEmailHtml(card.value)}</div>
          ${subHtml}
        </div>
      </td>`;
    })
    .join("");
  return `<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="margin-bottom:16px;"><tr>${cells}</tr></table>`;
}

/**
 * Generates an accurate preview of the automated Cloud Functions notification email
 * for booking, 18+ model application, or company model requirement alerts.
 */
export function buildAdminEmailPreview({
  type = "booking",
  record = null,
  recordId = "",
  brandOverrides = {}
} = {}) {
  const normalizedType = EMAIL_PREVIEW_TYPES.includes(type) ? type : "booking";
  const isSample = !record;
  const source = record || SAMPLE_EMAIL_PREVIEW_RECORDS[normalizedType];
  const resolvedId = String(recordId || source.id || `preview_${normalizedType}`).trim();
  const brand = resolveBrand(brandOverrides);
  const from = `${brand.brandName} Alerts <${brand.adminEmail}>`;
  const to = brand.adminEmail;

  if (normalizedType === "model") {
    const fullName = sanitizeEmailHeader(source.fullName || "New Applicant", 80);
    const ageNum = Number(source.age);
    const isAdult = Number.isFinite(ageNum) && ageNum >= 18;
    const ageLabel = Number.isFinite(ageNum)
      ? `${ageNum} yrs (${isAdult ? "18+ Eligible" : "Under 18 Flag"})`
      : "Not specified";
    const gender = String(source.gender || "Not specified").trim();
    const city = sanitizeEmailHeader(source.city || "Not specified", 80);
    const phoneNumber = String(source.phoneNumber || "Not provided").trim();
    const skills = String(source.skills || "Not specified").trim();
    const hasConsent = source.consentGiven === true;
    const consentText = hasConsent ? "Signed & Confirmed (18+)" : "Missing Consent";
    const status = String(source.status || "pending").trim();
    const submittedAt = formatPreviewTimestamp(source.createdAt);
    const rawPortfolio = String(source.portfolioUrls || "").trim();
    const safePortfolioHref = safeHttpUrl(rawPortfolio);
    const telHref = safeTelHref(phoneNumber);

    const vars = {
      modelId: resolvedId,
      fullName,
      ageLabel,
      gender,
      city,
      phoneNumber,
      skills,
      consentText,
      status,
      submittedAt,
      brandName: brand.brandName
    };

    const subject = renderSubjectTemplate(brand.modelSubjectTemplate, vars, 160);
    const intro = renderSubjectTemplate(brand.modelIntroTemplate, vars, 340);
    const preheader = `New 18+ model application from ${fullName} (${city}) — ${ageLabel}.`;
    const statusBadgeLabel = isAdult && hasConsent ? "18+ VERIFIED" : "REVIEW REQUIRED";

    const portfolioRow = rawPortfolio
      ? {
          label: "Portfolio Link",
          value: rawPortfolio,
          htmlValue: safePortfolioHref
            ? `<a href="${escapeEmailHtml(safePortfolioHref)}" style="color:${escapeEmailHtml(brand.accentLightColor)};text-decoration:underline;font-weight:700;">${escapeEmailHtml(safePortfolioHref)}</a>`
            : escapeEmailHtml(rawPortfolio)
        }
      : { label: "Portfolio Link", value: "None provided" };

    const rows = [
      { label: "Application ID", value: resolvedId },
      { label: "Applicant Name", value: fullName },
      { label: "Age Verification", value: ageLabel },
      { label: "Gender", value: gender },
      { label: "City", value: city },
      {
        label: "Phone Number",
        value: phoneNumber,
        htmlValue: telHref
          ? `<a href="${escapeEmailHtml(telHref)}" style="color:${escapeEmailHtml(brand.accentLightColor)};text-decoration:none;font-weight:700;">${escapeEmailHtml(phoneNumber)}</a>`
          : escapeEmailHtml(phoneNumber)
      },
      { label: "Skills & Experience", value: skills },
      portfolioRow,
      { label: "18+ Consent", value: consentText },
      { label: "Status", value: status },
      { label: "Submitted At", value: submittedAt }
    ];

    const bodyHtml = `
      ${renderHighlightCardsHtml(
        [
          { label: "Applicant", value: fullName, sub: city },
          { label: "18+ Check", value: ageLabel, valueColor: isAdult ? "#6EE7B7" : "#FCA5A5" },
          { label: "Consent", value: consentText, valueColor: hasConsent ? "#6EE7B7" : "#FCA5A5" }
        ],
        brand
      )}
      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;background-color:${escapeEmailHtml(brand.cardColor)};border:1px solid #2e2718;border-radius:12px;overflow:hidden;">
        ${renderDetailsRowsHtml(rows, brand)}
      </table>
      ${renderNotesCalloutHtml({
        title: "Model Skills & Roster Notes",
        content: skills !== "Not specified" ? skills : "",
        brand
      })}`;

    const html = renderBrandedEmailDocument({
      brand,
      documentTitle: subject,
      preheader,
      eyebrow: `${brand.brandName} • 18+ CASTING ALERT`,
      heading: "New 18+ Model Application",
      intro,
      statusBadgeLabel,
      badgeBgColor: isAdult && hasConsent ? "#0f291e" : "#2f1515",
      badgeBorderColor: isAdult && hasConsent ? "#34d399" : "#f87171",
      badgeTextColor: isAdult && hasConsent ? "#6ee7b7" : "#fca5a5",
      bodyHtml,
      ctaLabel: brand.modelCtaLabel,
      secondaryActionHtml: safePortfolioHref
        ? renderSecondaryActionHtml({ href: safePortfolioHref, label: "View Portfolio", brand })
        : ""
    });

    const text = [
      `${brand.brandName} — New 18+ Model Application`,
      "",
      ...rows.filter((r) => r.value).map((r) => `${r.label}: ${r.value}`),
      "",
      `Verify in Admin Dashboard: ${brand.dashboardUrl}`
    ].join("\n");

    return {
      type: "model",
      from,
      to,
      subject,
      preheader,
      html,
      text,
      statusBadgeLabel,
      recordId: resolvedId,
      recordLabel: `${fullName} (${city})`,
      isSample
    };
  }

  if (normalizedType === "company") {
    const companyName = sanitizeEmailHeader(source.companyName || "Company / Organizer", 100);
    const organizationType = String(source.organizationType || "Company").trim();
    const contactPerson = sanitizeEmailHeader(source.contactPerson || "Representative", 80);
    const mobileNumber = String(source.mobileNumber || "Not provided").trim();
    const email = String(source.email || "Not provided").trim();
    const address = String(source.address || "Not provided").trim();
    const modelRequirement = String(source.modelRequirement || "Multiple Models").trim();
    const modelsCount = Number(source.modelsCount) > 0 ? String(source.modelsCount) : "1";
    const eventType = sanitizeEmailHeader(source.eventType || "Event", 60);
    const eventDate = String(source.eventDate || "TBD").trim();
    const eventLocation = String(source.eventLocation || "TBD").trim();
    const budget = String(source.budget || "TBD").trim();
    const detailedRequirements = String(source.detailedRequirements || "").trim();
    const requestedModels = Array.isArray(source.requestedModelNames)
      ? source.requestedModelNames.filter(Boolean).join(", ")
      : "";
    const status = String(source.status || "Pending").trim();
    const submittedAt = formatPreviewTimestamp(source.createdAt);
    const telHref = safeTelHref(mobileNumber);

    const vars = {
      requestId: resolvedId,
      companyName,
      organizationType,
      contactPerson,
      mobileNumber,
      email,
      address,
      modelRequirement,
      modelsCount,
      eventType,
      eventDate,
      eventLocation,
      budget,
      status,
      submittedAt,
      brandName: brand.brandName
    };

    const subject = renderSubjectTemplate(brand.companySubjectTemplate, vars, 160);
    const intro = renderSubjectTemplate(brand.companyIntroTemplate, vars, 340);
    const preheader = `${companyName} requested ${modelsCount} ${modelRequirement} for ${eventType} (${eventDate}).`;
    const statusBadgeLabel = `${modelsCount} MODELS • ${status.toUpperCase()}`;

    const rows = [
      { label: "Request ID", value: resolvedId },
      { label: "Company / Organizer", value: `${companyName} (${organizationType})` },
      { label: "Contact Person", value: contactPerson },
      {
        label: "Mobile Number",
        value: mobileNumber,
        htmlValue: telHref
          ? `<a href="${escapeEmailHtml(telHref)}" style="color:${escapeEmailHtml(brand.accentLightColor)};text-decoration:none;font-weight:700;">${escapeEmailHtml(mobileNumber)}</a>`
          : escapeEmailHtml(mobileNumber)
      },
      { label: "Business Email", value: email },
      { label: "Company Address", value: address },
      { label: "Requirement", value: `${modelsCount} × ${modelRequirement}` },
      { label: "Event Type", value: eventType },
      { label: "Event Date", value: eventDate },
      { label: "Event Location", value: eventLocation },
      { label: "Estimated Budget", value: budget },
      ...(requestedModels ? [{ label: "Requested Roster Models", value: requestedModels }] : []),
      { label: "Status", value: status },
      { label: "Submitted At", value: submittedAt }
    ];

    const bodyHtml = `
      ${renderHighlightCardsHtml(
        [
          { label: "Organization", value: companyName, sub: eventType },
          { label: "Models Needed", value: `${modelsCount} • ${modelRequirement}`, valueColor: brand.accentLightColor },
          { label: "Date & Budget", value: eventDate, sub: budget }
        ],
        brand
      )}
      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;background-color:${escapeEmailHtml(brand.cardColor)};border:1px solid #2e2718;border-radius:12px;overflow:hidden;">
        ${renderDetailsRowsHtml(rows, brand)}
      </table>
      ${renderNotesCalloutHtml({
        title: "Detailed Event & Model Requirements",
        content: detailedRequirements,
        brand
      })}`;

    const html = renderBrandedEmailDocument({
      brand,
      documentTitle: subject,
      preheader,
      eyebrow: `${brand.brandName} • CORPORATE BOOKING ALERT`,
      heading: "New Company Model Requirement",
      intro,
      statusBadgeLabel,
      bodyHtml,
      ctaLabel: brand.companyCtaLabel,
      secondaryActionHtml: telHref
        ? renderSecondaryActionHtml({ href: telHref, label: "Call Organizer", brand })
        : ""
    });

    const text = [
      `${brand.brandName} — New Company Model Requirement Booking`,
      "",
      ...rows.filter((r) => r.value).map((r) => `${r.label}: ${r.value}`),
      ...(detailedRequirements ? ["", `Detailed Requirements: ${detailedRequirements}`] : []),
      "",
      `Manage in Admin Dashboard: ${brand.dashboardUrl}`
    ].join("\n");

    return {
      type: "company",
      from,
      to,
      subject,
      preheader,
      html,
      text,
      statusBadgeLabel,
      recordId: resolvedId,
      recordLabel: `${companyName} — ${eventType}`,
      isSample
    };
  }

  // Default: Shoot Booking Alert
  const clientName = sanitizeEmailHeader(source.clientName || "New Client", 80);
  const serviceType = sanitizeEmailHeader(source.serviceType || "Studio Session", 80);
  const preferredDate = String(source.preferredDate || "TBD").trim();
  const preferredTime = String(source.preferredTime || "").trim();
  const contactNumber = String(source.contactNumber || "Not provided").trim();
  const location = String(source.location || "Studio").trim();
  const status = String(source.status || "Pending").trim();
  const notes = String(source.notes || "").trim();
  const submittedAt = formatPreviewTimestamp(source.createdAt);
  const telHref = safeTelHref(contactNumber);

  const vars = {
    bookingId: resolvedId,
    clientName,
    serviceType,
    preferredDate,
    preferredTime: preferredTime || "Flexible",
    contactNumber,
    location,
    status,
    notes: notes || "None",
    submittedAt,
    brandName: brand.brandName
  };

  const subject = renderSubjectTemplate(brand.bookingSubjectTemplate, vars, 160);
  const intro = renderSubjectTemplate(brand.bookingIntroTemplate, vars, 340);
  const preheader = `New booking from ${clientName} for ${serviceType} on ${preferredDate}.`;
  const statusBadgeLabel = status.toUpperCase();

  const rows = [
    { label: "Booking ID", value: resolvedId },
    { label: "Client Name", value: clientName },
    {
      label: "Contact Number",
      value: contactNumber,
      htmlValue: telHref
        ? `<a href="${escapeEmailHtml(telHref)}" style="color:${escapeEmailHtml(brand.accentLightColor)};text-decoration:none;font-weight:700;">${escapeEmailHtml(contactNumber)}</a>`
        : escapeEmailHtml(contactNumber)
    },
    { label: "Service Package", value: serviceType },
    { label: "Preferred Date", value: preferredDate },
    { label: "Preferred Time", value: preferredTime },
    { label: "Shoot Location", value: location },
    { label: "Status", value: status },
    { label: "Client Notes", value: notes || "None" },
    { label: "Submitted At", value: submittedAt }
  ];

  const bodyHtml = `
    ${renderHighlightCardsHtml(
      [
        { label: "Event Date", value: preferredDate, sub: preferredTime || "Time TBD" },
        { label: "Package", value: serviceType },
        { label: "Client", value: clientName }
      ],
      brand
    )}
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;background-color:${escapeEmailHtml(brand.cardColor)};border:1px solid #2e2718;border-radius:12px;overflow:hidden;">
      ${renderDetailsRowsHtml(rows, brand)}
    </table>
    ${renderNotesCalloutHtml({
      title: "Client Notes & Shoot Brief",
      content: notes,
      brand
    })}`;

  const html = renderBrandedEmailDocument({
    brand,
    documentTitle: subject,
    preheader,
    eyebrow: `${brand.brandName} • BOOKING ALERT`,
    heading: "New Shoot Booking Request",
    intro,
    statusBadgeLabel,
    bodyHtml,
    ctaLabel: brand.bookingCtaLabel,
    secondaryActionHtml: telHref
      ? renderSecondaryActionHtml({ href: telHref, label: "Call Client", brand })
      : ""
  });

  const text = [
    `${brand.brandName} — New Shoot Booking Request`,
    "",
    ...rows.filter((r) => r.value).map((r) => `${r.label}: ${r.value}`),
    "",
    `Review in Admin Dashboard: ${brand.dashboardUrl}`
  ].join("\n");

  return {
    type: "booking",
    from,
    to,
    subject,
    preheader,
    html,
    text,
    statusBadgeLabel,
    recordId: resolvedId,
    recordLabel: `${clientName} — ${serviceType}`,
    isSample
  };
}

export function getPreviewRecordsForType(type, adminData = {}) {
  if (type === "model") {
    return Array.isArray(adminData.models) ? adminData.models : [];
  }
  if (type === "company") {
    return Array.isArray(adminData.companyBookings) ? adminData.companyBookings : [];
  }
  return Array.isArray(adminData.bookings) ? adminData.bookings : [];
}

function formatRecordOptionLabel(type, item) {
  if (type === "model") {
    return `${item.fullName || "Model"} • ${item.city || "Studio"} (${item.status || "pending"})`;
  }
  if (type === "company") {
    return `${item.companyName || "Company"} • ${item.eventType || "Event"} (${item.eventDate || "TBD"})`;
  }
  return `${item.clientName || "Client"} • ${item.serviceType || "Session"} (${item.preferredDate || "TBD"})`;
}

/**
 * Renders the complete Admin Email Preview studio section HTML.
 * Uses a strictly sandboxed iframe (sandbox="") so previewed HTML can never execute scripts.
 */
export function renderAdminEmailPreviewSectionHtml({
  previewState = {},
  adminData = {},
  t = (k) => k,
  escapeHtml = escapeEmailHtml
} = {}) {
  const activeType = EMAIL_PREVIEW_TYPES.includes(previewState.type) ? previewState.type : "booking";
  const viewMode = ["html", "text", "source"].includes(previewState.viewMode) ? previewState.viewMode : "html";
  const viewport = previewState.viewport === "mobile" ? "mobile" : "desktop";
  const overrides = previewState.overrides || {};

  const liveRecords = getPreviewRecordsForType(activeType, adminData);
  const selectedRecordId = String(previewState.recordId || "__sample__");
  const matchedRecord =
    selectedRecordId !== "__sample__" ? liveRecords.find((item) => item.id === selectedRecordId) || null : null;

  const preview = buildAdminEmailPreview({
    type: activeType,
    record: matchedRecord,
    recordId: matchedRecord?.id || "",
    brandOverrides: overrides
  });

  const subjectKey =
    activeType === "model"
      ? "modelSubjectTemplate"
      : activeType === "company"
        ? "companySubjectTemplate"
        : "bookingSubjectTemplate";
  const ctaKey =
    activeType === "model"
      ? "modelCtaLabel"
      : activeType === "company"
        ? "companyCtaLabel"
        : "bookingCtaLabel";

  const recordOptionsHtml = [
    `<option value="__sample__"${!matchedRecord ? " selected" : ""}>${escapeHtml(t("adminEmailPreviewSampleOption"))}</option>`,
    ...liveRecords.slice(0, 25).map(
      (item) =>
        `<option value="${escapeHtml(item.id)}"${matchedRecord?.id === item.id ? " selected" : ""}>${escapeHtml(formatRecordOptionLabel(activeType, item))}</option>`
    )
  ].join("");

  const activeAccent = sanitizeHexColor(overrides.accentColor, DEFAULT_EMAIL_PREVIEW_BRAND.accentColor);
  const activeBrandName = overrides.brandName ?? DEFAULT_EMAIL_PREVIEW_BRAND.brandName;
  const activeSubjectTemplate = overrides[subjectKey] ?? DEFAULT_EMAIL_PREVIEW_BRAND[subjectKey];
  const activeCtaLabel = overrides[ctaKey] ?? DEFAULT_EMAIL_PREVIEW_BRAND[ctaKey];

  let bodyStageHtml = "";
  if (viewMode === "text") {
    bodyStageHtml = `<pre id="adminEmailPreviewTextBody" class="email-preview-code">${escapeHtml(preview.text)}</pre>`;
  } else if (viewMode === "source") {
    bodyStageHtml = `<pre id="adminEmailPreviewSourceBody" class="email-preview-code">${escapeHtml(preview.html)}</pre>`;
  } else {
    bodyStageHtml = `
      <div class="email-preview-device-stage is-${viewport}">
        <iframe
          id="adminEmailPreviewFrame"
          class="email-preview-iframe"
          sandbox=""
          title="${escapeHtml(t("adminEmailPreviewFrameTitle"))}"
          srcdoc="${escapeHtml(preview.html)}"
        ></iframe>
      </div>`;
  }

  return `
  <section id="adminEmailPreviewPanel" class="studio-card email-preview-panel" aria-labelledby="adminEmailPreviewHeading">
    <div class="email-preview-header">
      <div>
        <span class="eyebrow">${escapeHtml(t("adminEmailPreviewEyebrow"))}</span>
        <h3 id="adminEmailPreviewHeading" class="email-preview-title">${escapeHtml(t("adminEmailPreviewTitle"))}</h3>
        <p class="email-preview-subtitle">${escapeHtml(t("adminEmailPreviewSubtitle"))}</p>
      </div>
      <span class="email-preview-safe-pill">${escapeHtml(t("adminEmailPreviewSafeBadge"))}</span>
    </div>

    <div class="email-preview-type-bar" role="group" aria-label="${escapeHtml(t("adminEmailPreviewTypeLabel"))}">
      <button type="button" class="chip-btn${activeType === "booking" ? " active" : ""}" data-email-preview-type="booking" aria-pressed="${activeType === "booking"}">
        ${escapeHtml(t("adminEmailTypeBooking"))}
      </button>
      <button type="button" class="chip-btn${activeType === "model" ? " active" : ""}" data-email-preview-type="model" aria-pressed="${activeType === "model"}">
        ${escapeHtml(t("adminEmailTypeModel"))}
      </button>
      <button type="button" class="chip-btn${activeType === "company" ? " active" : ""}" data-email-preview-type="company" aria-pressed="${activeType === "company"}">
        ${escapeHtml(t("adminEmailTypeCompany"))}
      </button>
    </div>

    <div class="email-preview-controls-grid">
      <div class="field">
        <label for="adminEmailPreviewRecordSelect">${escapeHtml(t("adminEmailPreviewRecordLabel"))}</label>
        <select id="adminEmailPreviewRecordSelect" class="input" data-email-preview-record>
          ${recordOptionsHtml}
        </select>
      </div>
      <div class="field">
        <label>${escapeHtml(t("adminEmailPreviewViewModeLabel"))}</label>
        <div class="email-preview-toggle-row" role="group" aria-label="Preview format">
          <button type="button" class="chip-btn${viewMode === "html" ? " active" : ""}" data-email-preview-mode="html" aria-pressed="${viewMode === "html"}">${escapeHtml(t("adminEmailModeHtml"))}</button>
          <button type="button" class="chip-btn${viewMode === "text" ? " active" : ""}" data-email-preview-mode="text" aria-pressed="${viewMode === "text"}">${escapeHtml(t("adminEmailModeText"))}</button>
          <button type="button" class="chip-btn${viewMode === "source" ? " active" : ""}" data-email-preview-mode="source" aria-pressed="${viewMode === "source"}">${escapeHtml(t("adminEmailModeSource"))}</button>
        </div>
      </div>
      <div class="field">
        <label>${escapeHtml(t("adminEmailPreviewViewportLabel"))}</label>
        <div class="email-preview-toggle-row" role="group" aria-label="Email viewport width">
          <button type="button" class="chip-btn${viewport === "desktop" ? " active" : ""}" data-email-preview-viewport="desktop" aria-pressed="${viewport === "desktop"}">${escapeHtml(t("adminEmailViewportDesktop"))}</button>
          <button type="button" class="chip-btn${viewport === "mobile" ? " active" : ""}" data-email-preview-viewport="mobile" aria-pressed="${viewport === "mobile"}">${escapeHtml(t("adminEmailViewportMobile"))}</button>
        </div>
      </div>
    </div>

    <details class="email-preview-customizer">
      <summary class="email-preview-customizer-summary">${escapeHtml(t("adminEmailCustomizeSummary"))}</summary>
      <div class="email-preview-customizer-grid">
        <div class="field">
          <label for="emailPreviewBrandNameInput">${escapeHtml(t("adminEmailBrandNameLabel"))}</label>
          <input id="emailPreviewBrandNameInput" class="input" type="text" maxlength="80" value="${escapeHtml(activeBrandName)}" data-email-override-key="brandName" />
        </div>
        <div class="field">
          <label for="emailPreviewAccentInput">${escapeHtml(t("adminEmailAccentLabel"))}</label>
          <div style="display:flex;gap:8px;align-items:center;">
            <input id="emailPreviewAccentInput" class="input" type="text" maxlength="7" value="${escapeHtml(activeAccent)}" data-email-override-key="accentColor" />
            <span class="email-preview-swatch" style="background:${escapeHtml(activeAccent)};" aria-hidden="true"></span>
          </div>
        </div>
        <div class="field">
          <label for="emailPreviewCtaInput">${escapeHtml(t("adminEmailCtaLabel"))}</label>
          <input id="emailPreviewCtaInput" class="input" type="text" maxlength="60" value="${escapeHtml(activeCtaLabel)}" data-email-override-key="${escapeHtml(ctaKey)}" />
        </div>
        <div class="field full">
          <label for="emailPreviewSubjectInput">${escapeHtml(t("adminEmailSubjectTemplateLabel"))}</label>
          <input id="emailPreviewSubjectInput" class="input" type="text" maxlength="160" value="${escapeHtml(activeSubjectTemplate)}" data-email-override-key="${escapeHtml(subjectKey)}" />
        </div>
        <div class="field full" style="display:flex;justify-content:flex-end;">
          <button type="button" class="btn-outline btn-sm" id="emailPreviewResetBtn" data-email-preview-reset>${escapeHtml(t("adminEmailResetDefaultsBtn"))}</button>
        </div>
      </div>
    </details>

    <div class="email-preview-envelope">
      <div class="email-preview-envelope-row">
        <span class="email-preview-meta-key">${escapeHtml(t("adminEmailFromLabel"))}</span>
        <span class="email-preview-meta-val">${escapeHtml(preview.from)}</span>
      </div>
      <div class="email-preview-envelope-row">
        <span class="email-preview-meta-key">${escapeHtml(t("adminEmailToLabel"))}</span>
        <span class="email-preview-meta-val">${escapeHtml(preview.to)}</span>
      </div>
      <div class="email-preview-envelope-row">
        <span class="email-preview-meta-key">${escapeHtml(t("adminEmailSubjectLabel"))}</span>
        <strong id="adminEmailPreviewSubjectText" class="email-preview-meta-val is-gold">${escapeHtml(preview.subject)}</strong>
      </div>
      <div class="email-preview-envelope-row">
        <span class="email-preview-meta-key">${escapeHtml(t("adminEmailPreheaderLabel"))}</span>
        <span class="email-preview-meta-val is-muted">${escapeHtml(preview.preheader)}</span>
      </div>
      <div class="email-preview-action-bar">
        <span class="badge">${escapeHtml(preview.isSample ? t("adminEmailSourceSampleBadge") : t("adminEmailSourceLiveBadge"))} • ${escapeHtml(preview.recordLabel)}</span>
        <div style="display:flex;gap:8px;flex-wrap:wrap;">
          <button type="button" class="btn-outline btn-sm" id="emailPreviewCopySubjectBtn" data-email-preview-copy="subject">${escapeHtml(t("adminEmailCopySubjectBtn"))}</button>
          <button type="button" class="btn-gold btn-sm" id="emailPreviewCopyHtmlBtn" data-email-preview-copy="html">${escapeHtml(t("adminEmailCopyHtmlBtn"))}</button>
        </div>
      </div>
    </div>

    ${bodyStageHtml}
  </section>`;
}
