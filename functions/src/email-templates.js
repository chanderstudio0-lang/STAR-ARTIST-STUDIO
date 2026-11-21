import {
  DEFAULT_DASHBOARD_URL,
  resolveEmailBrandConfig
} from "./config.js";
import {
  escapeHtml,
  sanitizeHeaderValue,
  loadHtmlTemplate,
  renderHtmlTemplate,
  renderSubjectTemplate
} from "./template-engine.js";

export { escapeHtml, sanitizeHeaderValue };

/**
 * Restricts dynamic links in emails strictly to http:// or https:// URLs.
 */
export function safeHttpUrl(rawUrl) {
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
 * Builds a safe tel: URI stripping non-dialable characters.
 */
export function safeTelHref(rawPhone) {
  const digits = String(rawPhone ?? "").replace(/[^+\d]/g, "").trim();
  if (digits.length < 7) return "";
  return `tel:${digits}`;
}

/**
 * Formats Firestore Timestamp / Date / ISO strings cleanly for email alerts.
 */
export function formatAlertTimestamp(value) {
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

  if (!date || Number.isNaN(date.getTime())) {
    return "Just now";
  }

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

/**
 * Renders either the custom studio logo image or the branded Black & Gold monogram badge.
 */
export function renderBrandMarkHtml(brand) {
  const safeLogo = safeHttpUrl(brand.logoUrl);
  if (safeLogo) {
    return `<table role="presentation" cellspacing="0" cellpadding="0" border="0">
      <tr>
        <td valign="middle" style="padding-right:10px;">
          <img src="${escapeHtml(safeLogo)}" alt="${escapeHtml(brand.brandName)}" width="38" height="38" style="display:block;border-radius:10px;border:1px solid ${escapeHtml(brand.accentColor)};object-fit:cover;" />
        </td>
        <td valign="middle">
          <div style="font-size:13px;font-weight:800;letter-spacing:0.12em;color:${escapeHtml(brand.accentLightColor)};text-transform:uppercase;">${escapeHtml(brand.brandName)}</div>
          <div style="font-size:11px;color:${escapeHtml(brand.mutedTextColor)};margin-top:2px;">${escapeHtml(brand.brandTagline)}</div>
        </td>
      </tr>
    </table>`;
  }

  const monogram = String(brand.brandName || "SA")
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join("") || "SA";

  return `<table role="presentation" cellspacing="0" cellpadding="0" border="0">
    <tr>
      <td valign="middle" style="padding-right:10px;">
        <div style="width:36px;height:36px;line-height:36px;text-align:center;border-radius:10px;background-color:#261f0e;border:1px solid ${escapeHtml(brand.accentColor)};color:${escapeHtml(brand.accentLightColor)};font-size:14px;font-weight:900;letter-spacing:0.06em;">
          ${escapeHtml(monogram)}
        </div>
      </td>
      <td valign="middle">
        <div style="font-size:13px;font-weight:800;letter-spacing:0.12em;color:${escapeHtml(brand.accentLightColor)};text-transform:uppercase;">${escapeHtml(brand.brandName)}</div>
        <div style="font-size:11px;color:${escapeHtml(brand.mutedTextColor)};margin-top:2px;">${escapeHtml(brand.brandTagline)}</div>
      </td>
    </tr>
  </table>`;
}

/**
 * Renders alternating detail rows for the email specification table.
 */
export function renderDetailsRowsHtml(rows = [], brand = resolveEmailBrandConfig()) {
  const visibleRows = rows.filter(
    (row) => row && row.value !== undefined && row.value !== null && String(row.value).trim() !== ""
  );
  return visibleRows
    .map((row, idx) => {
      const isLast = idx === visibleRows.length - 1;
      const borderStyle = isLast ? "" : "border-bottom:1px solid #262115;";
      const rowBg = idx % 2 === 1 ? "background-color:#13110e;" : "";
      return `
      <tr style="${rowBg}">
        <td style="padding:11px 14px;${borderStyle}color:${escapeHtml(brand.mutedTextColor)};font-size:12px;font-weight:700;letter-spacing:0.03em;width:36%;vertical-align:top;">
          ${escapeHtml(row.label)}
        </td>
        <td style="padding:11px 14px;${borderStyle}color:${escapeHtml(brand.textColor)};font-size:13.5px;line-height:1.45;vertical-align:top;word-break:break-word;">
          ${row.htmlValue || escapeHtml(row.value)}
        </td>
      </tr>`;
    })
    .join("");
}

/**
 * Renders an optional highlighted callout box for client notes, skills, or event requirements.
 */
export function renderNotesCalloutHtml({ title, content, brand }) {
  const clean = String(content ?? "").trim();
  if (!clean) return "";
  return `
  <div style="margin-top:16px;padding:14px 16px;border-radius:12px;background-color:#18140c;border:1px solid #352b14;border-left:3px solid ${escapeHtml(brand.accentColor)};">
    <div style="font-size:11px;font-weight:800;letter-spacing:0.1em;text-transform:uppercase;color:${escapeHtml(brand.accentColor)};margin-bottom:5px;">
      ${escapeHtml(title)}
    </div>
    <div style="font-size:13.5px;line-height:1.55;color:${escapeHtml(brand.textColor)};white-space:pre-line;word-break:break-word;">
      ${escapeHtml(clean)}
    </div>
  </div>`;
}

function renderSecondaryActionHtml({ href, label, brand }) {
  if (!href || !label) return "";
  return `&nbsp;&nbsp;<a href="${escapeHtml(href)}" style="display:inline-block;margin-top:8px;padding:12px 20px;border-radius:999px;background-color:transparent;border:1px solid ${escapeHtml(brand.accentColor)};color:${escapeHtml(brand.accentLightColor)};font-size:13px;font-weight:700;text-decoration:none;">
    ${escapeHtml(label)}
  </a>`;
}

function renderBrandedLayout({
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
  ctaUrl,
  ctaLabel,
  secondaryActionHtml = ""
}) {
  const safeCtaUrl = safeHttpUrl(ctaUrl) || brand.dashboardUrl || DEFAULT_DASHBOARD_URL;
  const layoutTemplate = loadHtmlTemplate("base-layout.html", {
    templateDir: brand.templateDir,
    inlineOverride: brand.baseLayoutHtml
  });

  return renderHtmlTemplate(
    layoutTemplate,
    {
      documentTitle,
      preheader,
      eyebrow,
      heading,
      intro,
      statusBadgeLabel,
      badgeBgColor,
      badgeBorderColor,
      badgeTextColor,
      ctaUrl: safeCtaUrl,
      ctaLabel,
      brandName: brand.brandName,
      studioLocation: brand.studioLocation,
      footerText: brand.footerText,
      accentColor: brand.accentColor,
      accentLightColor: brand.accentLightColor,
      bgColor: brand.bgColor,
      surfaceColor: brand.surfaceColor,
      cardColor: brand.cardColor,
      textColor: brand.textColor,
      mutedTextColor: brand.mutedTextColor
    },
    {
      brandMarkHtml: renderBrandMarkHtml(brand),
      bodyHtml,
      secondaryActionHtml
    }
  );
}

/**
 * Builds the customizable branded Admin email alert for a newly created Firestore booking request.
 */
export function buildNewBookingAlertEmail({
  bookingId = "",
  booking = {},
  dashboardUrl = "",
  env = process.env,
  templateOverrides = {}
} = {}) {
  const brand = resolveEmailBrandConfig(env, {
    ...templateOverrides,
    ...(dashboardUrl ? { dashboardUrl } : {})
  });

  const clientName = sanitizeHeaderValue(booking.clientName || "New Client", 80);
  const serviceType = sanitizeHeaderValue(booking.serviceType || "Studio Session", 80);
  const preferredDate = String(booking.preferredDate || "TBD").trim();
  const preferredTime = String(booking.preferredTime || "").trim();
  const contactNumber = String(booking.contactNumber || "Not provided").trim();
  const location = String(booking.location || "Studio").trim();
  const status = String(booking.status || "Pending").trim();
  const notes = String(booking.notes || "").trim();
  const submittedAt = formatAlertTimestamp(booking.createdAt);

  const telHref = safeTelHref(contactNumber);
  const templateVars = {
    bookingId: bookingId || "N/A",
    clientName,
    serviceType,
    preferredDate,
    preferredTime: preferredTime || "Flexible",
    preferredTimeDisplay: preferredTime || "Time TBD",
    contactNumber,
    location,
    status,
    notes: notes || "None",
    submittedAt,
    brandName: brand.brandName,
    accentColor: brand.accentColor,
    accentLightColor: brand.accentLightColor,
    cardColor: brand.cardColor,
    textColor: brand.textColor,
    mutedTextColor: brand.mutedTextColor
  };

  const subject = renderSubjectTemplate(brand.bookingSubjectTemplate, templateVars, 160);
  const intro = renderSubjectTemplate(brand.bookingIntroTemplate, templateVars, 340);

  const rows = [
    { label: "Booking ID", value: bookingId || "N/A" },
    { label: "Client Name", value: clientName },
    {
      label: "Contact Number",
      value: contactNumber,
      htmlValue: telHref
        ? `<a href="${escapeHtml(telHref)}" style="color:${escapeHtml(brand.accentLightColor)};text-decoration:none;font-weight:700;">${escapeHtml(contactNumber)}</a>`
        : escapeHtml(contactNumber)
    },
    { label: "Service Package", value: serviceType },
    { label: "Preferred Date", value: preferredDate },
    { label: "Preferred Time", value: preferredTime },
    { label: "Shoot Location", value: location },
    { label: "Status", value: status },
    { label: "Client Notes", value: notes || "None" },
    { label: "Submitted At", value: submittedAt }
  ];

  const bodyTemplate = loadHtmlTemplate("booking-alert.html", {
    templateDir: brand.templateDir,
    inlineOverride: brand.bookingBodyHtml
  });

  const bodyHtml = renderHtmlTemplate(bodyTemplate, templateVars, {
    detailsRowsHtml: renderDetailsRowsHtml(rows, brand),
    notesCalloutHtml: renderNotesCalloutHtml({
      title: "Client Notes & Shoot Brief",
      content: notes,
      brand
    })
  });

  const html = renderBrandedLayout({
    brand,
    documentTitle: subject,
    preheader: `New booking from ${clientName} for ${serviceType} on ${preferredDate}.`,
    eyebrow: `${brand.brandName} • BOOKING ALERT`,
    heading: "New Shoot Booking Request",
    intro,
    statusBadgeLabel: status.toUpperCase(),
    bodyHtml,
    ctaUrl: brand.dashboardUrl,
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

  return { subject, text, html };
}

/**
 * Builds the customizable branded Admin email alert for a newly submitted 18+ model application.
 */
export function buildNewModelApplicationAlertEmail({
  modelId = "",
  model = {},
  dashboardUrl = "",
  env = process.env,
  templateOverrides = {}
} = {}) {
  const brand = resolveEmailBrandConfig(env, {
    ...templateOverrides,
    ...(dashboardUrl ? { dashboardUrl } : {})
  });

  const fullName = sanitizeHeaderValue(model.fullName || "New Applicant", 80);
  const ageNum = Number(model.age);
  const isAdult = Number.isFinite(ageNum) && ageNum >= 18;
  const ageLabel = Number.isFinite(ageNum)
    ? `${ageNum} yrs (${isAdult ? "18+ Eligible" : "Under 18 Flag"})`
    : "Not specified";
  const gender = String(model.gender || "Not specified").trim();
  const city = sanitizeHeaderValue(model.city || "Not specified", 80);
  const phoneNumber = String(model.phoneNumber || "Not provided").trim();
  const skills = String(model.skills || "Not specified").trim();
  const hasConsent = model.consentGiven === true;
  const consentText = hasConsent ? "Signed & Confirmed (18+)" : "Missing Consent";
  const status = String(model.status || "pending").trim();
  const submittedAt = formatAlertTimestamp(model.createdAt);

  const rawPortfolio = String(model.portfolioUrls || "").trim();
  const safePortfolioHref = safeHttpUrl(rawPortfolio);
  const telHref = safeTelHref(phoneNumber);

  const templateVars = {
    modelId: modelId || "N/A",
    fullName,
    ageLabel,
    gender,
    city,
    phoneNumber,
    skills,
    consentText,
    status,
    submittedAt,
    ageHighlightColor: isAdult ? "#6EE7B7" : "#FCA5A5",
    consentHighlightColor: hasConsent ? "#6EE7B7" : "#FCA5A5",
    brandName: brand.brandName,
    accentColor: brand.accentColor,
    accentLightColor: brand.accentLightColor,
    cardColor: brand.cardColor,
    textColor: brand.textColor,
    mutedTextColor: brand.mutedTextColor
  };

  const subject = renderSubjectTemplate(brand.modelSubjectTemplate, templateVars, 160);
  const intro = renderSubjectTemplate(brand.modelIntroTemplate, templateVars, 340);

  const portfolioRow = rawPortfolio
    ? {
        label: "Portfolio Link",
        value: rawPortfolio,
        htmlValue: safePortfolioHref
          ? `<a href="${escapeHtml(safePortfolioHref)}" style="color:${escapeHtml(brand.accentLightColor)};text-decoration:underline;font-weight:700;">${escapeHtml(safePortfolioHref)}</a>`
          : escapeHtml(rawPortfolio)
      }
    : { label: "Portfolio Link", value: "None provided" };

  const rows = [
    { label: "Application ID", value: modelId || "N/A" },
    { label: "Applicant Name", value: fullName },
    { label: "Age Verification", value: ageLabel },
    { label: "Gender", value: gender },
    { label: "City", value: city },
    {
      label: "Phone Number",
      value: phoneNumber,
      htmlValue: telHref
        ? `<a href="${escapeHtml(telHref)}" style="color:${escapeHtml(brand.accentLightColor)};text-decoration:none;font-weight:700;">${escapeHtml(phoneNumber)}</a>`
        : escapeHtml(phoneNumber)
    },
    { label: "Skills & Experience", value: skills },
    portfolioRow,
    { label: "18+ Consent", value: consentText },
    { label: "Status", value: status },
    { label: "Submitted At", value: submittedAt }
  ];

  const bodyTemplate = loadHtmlTemplate("model-application-alert.html", {
    templateDir: brand.templateDir,
    inlineOverride: brand.modelBodyHtml
  });

  const bodyHtml = renderHtmlTemplate(bodyTemplate, templateVars, {
    detailsRowsHtml: renderDetailsRowsHtml(rows, brand),
    skillsCalloutHtml: renderNotesCalloutHtml({
      title: "Model Skills & Roster Notes",
      content: skills !== "Not specified" ? skills : "",
      brand
    })
  });

  const html = renderBrandedLayout({
    brand,
    documentTitle: subject,
    preheader: `New 18+ model application from ${fullName} (${city}) — ${ageLabel}.`,
    eyebrow: `${brand.brandName} • 18+ CASTING ALERT`,
    heading: "New 18+ Model Application",
    intro,
    statusBadgeLabel: isAdult && hasConsent ? "18+ VERIFIED" : "REVIEW REQUIRED",
    badgeBgColor: isAdult && hasConsent ? "#0f291e" : "#2f1515",
    badgeBorderColor: isAdult && hasConsent ? "#34d399" : "#f87171",
    badgeTextColor: isAdult && hasConsent ? "#6ee7b7" : "#fca5a5",
    bodyHtml,
    ctaUrl: brand.dashboardUrl,
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

  return { subject, text, html };
}

/**
 * Builds the customizable branded Admin email alert for a Company / Event Organizer model booking request.
 */
export function buildNewCompanyBookingAlertEmail({
  requestId = "",
  request = {},
  dashboardUrl = "",
  env = process.env,
  templateOverrides = {}
} = {}) {
  const brand = resolveEmailBrandConfig(env, {
    ...templateOverrides,
    ...(dashboardUrl ? { dashboardUrl } : {})
  });

  const companyName = sanitizeHeaderValue(request.companyName || "Company / Organizer", 100);
  const organizationType = String(request.organizationType || "Company").trim();
  const contactPerson = sanitizeHeaderValue(request.contactPerson || "Representative", 80);
  const mobileNumber = String(request.mobileNumber || "Not provided").trim();
  const email = String(request.email || "Not provided").trim();
  const address = String(request.address || "Not provided").trim();
  const modelRequirement = String(request.modelRequirement || "Multiple Models").trim();
  const modelsCount = Number(request.modelsCount) > 0 ? String(request.modelsCount) : "1";
  const eventType = sanitizeHeaderValue(request.eventType || "Event", 60);
  const eventDate = String(request.eventDate || "TBD").trim();
  const eventLocation = String(request.eventLocation || "TBD").trim();
  const budget = String(request.budget || "TBD").trim();
  const detailedRequirements = String(request.detailedRequirements || "").trim();
  const requestedModels = Array.isArray(request.requestedModelNames)
    ? request.requestedModelNames.filter(Boolean).join(", ")
    : "";
  const status = String(request.status || "Pending").trim();
  const submittedAt = formatAlertTimestamp(request.createdAt);

  const telHref = safeTelHref(mobileNumber);

  const templateVars = {
    requestId: requestId || "N/A",
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
    brandName: brand.brandName,
    accentColor: brand.accentColor,
    accentLightColor: brand.accentLightColor,
    cardColor: brand.cardColor,
    textColor: brand.textColor,
    mutedTextColor: brand.mutedTextColor
  };

  const subject = renderSubjectTemplate(brand.companySubjectTemplate, templateVars, 160);
  const intro = renderSubjectTemplate(brand.companyIntroTemplate, templateVars, 340);

  const rows = [
    { label: "Request ID", value: requestId || "N/A" },
    { label: "Company / Organizer", value: `${companyName} (${organizationType})` },
    { label: "Contact Person", value: contactPerson },
    {
      label: "Mobile Number",
      value: mobileNumber,
      htmlValue: telHref
        ? `<a href="${escapeHtml(telHref)}" style="color:${escapeHtml(brand.accentLightColor)};text-decoration:none;font-weight:700;">${escapeHtml(mobileNumber)}</a>`
        : escapeHtml(mobileNumber)
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

  const bodyTemplate = loadHtmlTemplate("company-booking-alert.html", {
    templateDir: brand.templateDir,
    inlineOverride: brand.companyBodyHtml
  });

  const bodyHtml = renderHtmlTemplate(bodyTemplate, templateVars, {
    detailsRowsHtml: renderDetailsRowsHtml(rows, brand),
    requirementsCalloutHtml: renderNotesCalloutHtml({
      title: "Detailed Event & Model Requirements",
      content: detailedRequirements,
      brand
    })
  });

  const html = renderBrandedLayout({
    brand,
    documentTitle: subject,
    preheader: `${companyName} requested ${modelsCount} ${modelRequirement} for ${eventType} (${eventDate}).`,
    eyebrow: `${brand.brandName} • CORPORATE BOOKING ALERT`,
    heading: "New Company Model Requirement",
    intro,
    statusBadgeLabel: `${modelsCount} MODELS • ${status.toUpperCase()}`,
    bodyHtml,
    ctaUrl: brand.dashboardUrl,
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

  return { subject, text, html };
}
