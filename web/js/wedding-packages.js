export const WEDDING_PACKAGE_ID = "pkg_wedding_collection";

export const WEDDING_TIER_IDS = [
  "wedding_basic",
  "wedding_premium",
  "wedding_luxury"
];

export const MIN_WEDDING_PRICE_INR = 1000;
export const MAX_WEDDING_PRICE_INR = 10000000;
export const MAX_WEDDING_SERVICES_COUNT = 15;

export const DEFAULT_WEDDING_TIERS = [
  {
    id: "wedding_basic",
    tierNameEn: "Basic Wedding Package",
    tierNameHi: "बेसिक वेडिंग पैकेज",
    priceInr: 45000,
    priceFormatted: "₹45,000",
    durationEn: "1-Day Wedding Ceremony",
    turnaroundEn: "7 Days Delivery",
    isRecommended: false,
    deliverablesEn: [
      "Full wedding day Traditional Photography + HD Videography",
      "1 Candid Photographer for key rituals & family portraits",
      "150 color-graded high-resolution wedding photographs",
      "30-sheet premium printed matte wedding photo album",
      "Private cloud gallery link for family sharing"
    ]
  },
  {
    id: "wedding_premium",
    tierNameEn: "Premium Wedding Package",
    tierNameHi: "प्रीमियम वेडिंग पैकेज",
    priceInr: 85000,
    priceFormatted: "₹85,000",
    durationEn: "2-Day Events (Haldi, Mehndi & Wedding)",
    turnaroundEn: "5 Days Priority",
    isRecommended: true,
    deliverablesEn: [
      "2-day coverage across Haldi, Mehndi, Sangeet & Wedding ceremony",
      "Candid + Traditional Photography & 4K Cinematic Videography",
      "Drone aerial coverage for Baraat & venue establishing shots",
      "50-sheet luxury Karizma hardbound wedding album + couple box",
      "3-minute 4K cinematic wedding highlight film + 2 Instagram reels",
      "350 retouched high-resolution stills + digital cloud backup"
    ]
  },
  {
    id: "wedding_luxury",
    tierNameEn: "Luxury Wedding Package",
    tierNameHi: "लक्ज़री वेडिंग पैकेज",
    priceInr: 145000,
    priceFormatted: "₹1,45,000",
    durationEn: "3-Day Royal Destination Coverage",
    turnaroundEn: "48 Hours Sneak Peek",
    isRecommended: false,
    deliverablesEn: [
      "3-day complete royal destination coverage with 6-member cinema crew",
      "Complimentary Pre-Wedding editorial portrait & teaser film shoot",
      "Dual 4K cinema cameras, gimbal rig, drone aerials & LED stage lighting",
      "Same-Day Edit (SDE) highlight film screened live at Reception",
      "2 Handcrafted Italian leather luxury albums + 2 mini parent albums",
      "Full 4K master wedding feature film + 5 vertical social cutdowns",
      "600+ editorial retouched photographs + USB crystal keepsake box"
    ]
  }
];

export const WEDDING_BOOKING_ADDONS = [
  {
    id: "drone_shoot",
    nameEn: "Drone Shoot",
    nameHi: "ड्रोन शूट",
    descEn: "4K aerial Baraat, venue & couple cinematography",
    descHi: "बारात, वेन्यू और कपल की 4K एरियल ड्रोन शूटिंग",
    priceInr: 12000
  },
  {
    id: "cinematic_video",
    nameEn: "Cinematic Video",
    nameHi: "सिनेमाई वीडियो",
    descEn: "4K multi-camera highlight film, gimbal shots & color grade",
    descHi: "4K मल्टी-कैमरा हाइलाइट फिल्म, गिंबल शॉट्स और कलर ग्रेडिंग",
    priceInr: 18000
  },
  {
    id: "pre_wedding_shoot",
    nameEn: "Pre-Wedding Shoot",
    nameHi: "प्री-वेडिंग शूट",
    descEn: "Outdoor editorial couple portraits + 1-min teaser reel",
    descHi: "आउटडोर कपल पोर्ट्रेट शूट + 1 मिनट टीज़र रील",
    priceInr: 15000
  },
  {
    id: "same_day_edit",
    nameEn: "Same-Day Edit (SDE)",
    nameHi: "सेम-डे एडिट (SDE)",
    descEn: "Live-edited wedding highlight screened at your Reception",
    descHi: "रिसेप्शन पर स्क्रीन करने के लिए लाइव-एडिटेड हाइलाइट फिल्म",
    priceInr: 10000
  },
  {
    id: "led_wall_live",
    nameEn: "LED Wall & Live Telecast",
    nameHi: "LED वॉल व लाइव टेलीकास्ट",
    descEn: "Live stage camera feed & mixing for ceremony guests",
    descHi: "समारोह और स्टेज के लिए लाइव कैमरा फीड व मिक्सिंग",
    priceInr: 14000
  },
  {
    id: "extra_luxury_album",
    nameEn: "Extra Luxury Photo Album",
    nameHi: "अतिरिक्त लक्ज़री फोटो एल्बम",
    descEn: "40-sheet Karizma hardbound keepsake album for parents",
    descHi: "परिवार के लिए 40-शीट करिश्मा हार्डबाउंड वेडिंग एल्बम",
    priceInr: 8500
  }
];

function defaultEscapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

/**
 * Formats an integer INR price with Indian digit grouping (e.g. 145000 -> "₹1,45,000").
 */
export function formatInrCurrency(amount) {
  const num = Math.round(Number(amount) || 0);
  if (num <= 0) return "₹0";
  const str = String(num);
  if (str.length <= 3) return `₹${str}`;
  const last3 = str.slice(-3);
  const rest = str.slice(0, -3).replace(/\B(?=(\d{2})+(?!\d))/g, ",");
  return `₹${rest},${last3}`;
}

/**
 * Normalizes included services from a newline-delimited string or string array.
 */
export function normalizeIncludedServices(raw) {
  const list = Array.isArray(raw)
    ? raw
    : String(raw ?? "")
        .split(/\r?\n/)
        .map((line) => line.replace(/^[\s•\-*✓]+/, ""));
  const cleaned = [];
  for (const item of list) {
    const text = String(item ?? "").trim().slice(0, 180);
    if (!text) continue;
    cleaned.push(text);
    if (cleaned.length >= MAX_WEDDING_SERVICES_COUNT) break;
  }
  return cleaned;
}

/**
 * Validates an Admin Dashboard wedding tier configuration update before saving to Firestore.
 */
export function validateWeddingTierConfig(tierId, payload = {}, lang = "en") {
  const hi = lang === "hi";
  const errors = [];
  const cleanTierId = String(tierId || "").trim();
  const baseTier = DEFAULT_WEDDING_TIERS.find((t) => t.id === cleanTierId);

  if (!baseTier) {
    errors.push(hi ? "अमान्य वेडिंग पैकेज टियर।" : "Invalid wedding package tier.");
  }

  const rawPrice = String(payload.priceInr ?? "").replace(/[^\d]/g, "");
  const priceInr = Number.parseInt(rawPrice, 10);
  if (!Number.isInteger(priceInr) || priceInr < MIN_WEDDING_PRICE_INR || priceInr > MAX_WEDDING_PRICE_INR) {
    errors.push(
      hi
        ? "कृपया ₹1,000 से ₹1,00,00,000 के बीच मान्य मूल्य दर्ज करें।"
        : "Please enter a valid price between ₹1,000 and ₹1,00,00,000."
    );
  }

  const durationEn = String(payload.durationEn ?? baseTier?.durationEn ?? "").trim().slice(0, 100);
  if (durationEn.length < 2) {
    errors.push(hi ? "कृपया कवरेज अवधि दर्ज करें।" : "Please enter the package coverage duration.");
  }

  const turnaroundEn = String(payload.turnaroundEn ?? baseTier?.turnaroundEn ?? "").trim().slice(0, 100);
  if (turnaroundEn.length < 2) {
    errors.push(hi ? "कृपया डिलीवरी समय दर्ज करें।" : "Please enter the delivery turnaround time.");
  }

  const includedServices = normalizeIncludedServices(payload.includedServices ?? payload.deliverablesEn);
  if (!includedServices.length) {
    errors.push(
      hi
        ? "कृपया कम से कम 1 शामिल सेवा दर्ज करें।"
        : "Please enter at least 1 included service (one per line)."
    );
  }

  return {
    valid: errors.length === 0,
    errors,
    cleaned: {
      tierId: cleanTierId,
      tierNameEn: baseTier?.tierNameEn || cleanTierId,
      tierNameHi: baseTier?.tierNameHi || baseTier?.tierNameEn || cleanTierId,
      priceInr: Number.isInteger(priceInr) ? priceInr : baseTier?.priceInr || 45000,
      priceFormatted: formatInrCurrency(Number.isInteger(priceInr) ? priceInr : baseTier?.priceInr || 45000),
      durationEn,
      turnaroundEn,
      includedServices
    }
  };
}

/**
 * Returns the canonical booking dropdown value for any service package + tier.
 * Wedding tiers use their exact tier names ("Basic Wedding Package", "Premium Wedding Package",
 * "Luxury Wedding Package") so clients and admins can identify them directly.
 */
export function getServiceTierBookingValue(pkg, tier) {
  if (!pkg || !tier) return "";
  if (pkg.id === WEDDING_PACKAGE_ID || pkg.isWeddingCollection) {
    return String(tier.tierNameEn || "").trim().slice(0, 100);
  }
  return `${pkg.recommendedServiceType} - ${tier.tierNameEn}`.slice(0, 100);
}

/**
 * Merges live Admin-configured Firestore overrides into STUDIO_SERVICES without mutating defaults.
 */
export function mergeWeddingTierConfigs(baseServices = [], cloudConfigs = []) {
  const overrideMap = new Map();
  if (Array.isArray(cloudConfigs)) {
    for (const cfg of cloudConfigs) {
      const id = String(cfg?.tierId || cfg?.id || "").trim();
      if (!WEDDING_TIER_IDS.includes(id)) continue;
      overrideMap.set(id, cfg);
    }
  }

  return (Array.isArray(baseServices) ? baseServices : []).map((pkg) => {
    if (pkg.id !== WEDDING_PACKAGE_ID) return pkg;
    const mergedTiers = (pkg.tiers || DEFAULT_WEDDING_TIERS).map((tier) => {
      const ov = overrideMap.get(tier.id);
      if (!ov) return { ...tier, deliverablesEn: [...(tier.deliverablesEn || [])], isCustomized: false };
      const parsedPrice = Number.parseInt(String(ov.priceInr ?? ""), 10);
      const safePrice =
        Number.isInteger(parsedPrice) && parsedPrice >= MIN_WEDDING_PRICE_INR && parsedPrice <= MAX_WEDDING_PRICE_INR
          ? parsedPrice
          : tier.priceInr;
      const services = normalizeIncludedServices(ov.includedServices ?? ov.deliverablesEn);
      return {
        ...tier,
        priceInr: safePrice,
        priceFormatted: formatInrCurrency(safePrice),
        durationEn: String(ov.durationEn || "").trim().slice(0, 100) || tier.durationEn,
        turnaroundEn: String(ov.turnaroundEn || "").trim().slice(0, 100) || tier.turnaroundEn,
        deliverablesEn: services.length ? services : [...(tier.deliverablesEn || [])],
        isCustomized: true
      };
    });

    const minPrice = Math.min(...mergedTiers.map((t) => Number(t.priceInr) || 45000));
    return {
      ...pkg,
      startingPriceInr: minPrice,
      priceFormatted: formatInrCurrency(minPrice),
      tiers: mergedTiers
    };
  });
}

/**
 * Finds the active wedding tier matching a booking serviceType value (e.g. "Premium Wedding Package").
 */
export function findWeddingTierByBookingValue(services = [], bookingValue = "") {
  const clean = String(bookingValue || "").trim().toLowerCase();
  if (!clean) return null;
  const weddingPkg = (Array.isArray(services) ? services : []).find((p) => p.id === WEDDING_PACKAGE_ID);
  if (!weddingPkg || !Array.isArray(weddingPkg.tiers)) return null;
  return (
    weddingPkg.tiers.find((tier) => String(tier.tierNameEn || "").trim().toLowerCase() === clean) ||
    null
  );
}

/**
 * Looks up a wedding add-on by ID.
 */
export function getWeddingAddonById(addonId, addons = WEDDING_BOOKING_ADDONS) {
  const cleanId = String(addonId || "").trim();
  if (!cleanId || !Array.isArray(addons)) return null;
  return addons.find((addon) => addon.id === cleanId) || null;
}

/**
 * Dynamically calculates the total wedding booking price by adding user-selected
 * add-ons (e.g., Drone Shoot, Cinematic Video) to the chosen wedding package price.
 */
export function calculateWeddingBookingTotal(
  tier,
  selectedAddonIds = [],
  addons = WEDDING_BOOKING_ADDONS
) {
  const basePriceInr = Math.max(0, Math.round(Number(tier?.priceInr) || 0));
  const seen = new Set();
  const selectedAddons = [];

  if (Array.isArray(selectedAddonIds)) {
    for (const rawId of selectedAddonIds) {
      const id = String(rawId || "").trim();
      if (!id || seen.has(id)) continue;
      const matched = getWeddingAddonById(id, addons);
      if (!matched) continue;
      seen.add(id);
      const addonPrice = Math.max(0, Math.round(Number(matched.priceInr) || 0));
      selectedAddons.push({
        ...matched,
        priceInr: addonPrice,
        priceFormatted: formatInrCurrency(addonPrice)
      });
    }
  }

  const addonsTotalInr = selectedAddons.reduce((sum, item) => sum + item.priceInr, 0);
  const totalPriceInr = basePriceInr + addonsTotalInr;

  return {
    tierId: String(tier?.id || ""),
    tierNameEn: String(tier?.tierNameEn || ""),
    basePriceInr,
    basePriceFormatted: formatInrCurrency(basePriceInr),
    selectedAddons,
    selectedAddonIds: selectedAddons.map((a) => a.id),
    addonsCount: selectedAddons.length,
    addonsTotalInr,
    addonsTotalFormatted: formatInrCurrency(addonsTotalInr),
    totalPriceInr,
    totalPriceFormatted: formatInrCurrency(totalPriceInr)
  };
}

/**
 * Appends the calculated wedding package + add-on pricing summary to booking notes
 * while strictly staying within Firestore's 1,000-character notes limit.
 */
export function buildWeddingBookingNotesWithPricing(rawNotes = "", pricingSummary = null) {
  const userNotes = String(rawNotes || "").trim();
  if (!pricingSummary || !pricingSummary.tierNameEn) {
    return userNotes.slice(0, 1000);
  }
  const addonsPart = pricingSummary.selectedAddons.length
    ? pricingSummary.selectedAddons
        .map((a) => `${a.nameEn} (+${a.priceFormatted})`)
        .join(", ")
    : "None";
  const quoteLine = `[Wedding Quote] Package: ${pricingSummary.tierNameEn} (${pricingSummary.basePriceFormatted}) | Add-ons: ${addonsPart} | Estimated Total: ${pricingSummary.totalPriceFormatted}`;

  if (!userNotes) return quoteLine.slice(0, 1000);
  if (userNotes.includes("[Wedding Quote]")) return userNotes.slice(0, 1000);

  const separator = "\n";
  const maxUserLen = Math.max(0, 1000 - quoteLine.length - separator.length);
  const trimmedUser = userNotes.slice(0, maxUserLen).trim();
  return trimmedUser ? `${trimmedUser}${separator}${quoteLine}` : quoteLine.slice(0, 1000);
}

/**
 * Updates the live pricing breakdown inside #bookingWeddingSummary in place
 * without recreating checkbox DOM nodes or interrupting keyboard focus.
 */
export function updateBookingWeddingTotalDom(
  containerEl,
  tier,
  selectedAddonIds = [],
  { addons = WEDDING_BOOKING_ADDONS, t = (k) => k } = {}
) {
  const pricing = calculateWeddingBookingTotal(tier, selectedAddonIds, addons);
  if (!containerEl) return pricing;

  const baseEl = containerEl.querySelector?.("[data-wedding-base-price]");
  if (baseEl) baseEl.textContent = pricing.basePriceFormatted;

  const addonsTotalEl = containerEl.querySelector?.("[data-wedding-addons-total]");
  if (addonsTotalEl) {
    addonsTotalEl.textContent =
      pricing.addonsTotalInr > 0 ? `+${pricing.addonsTotalFormatted}` : "₹0";
  }

  const addonsCountEl = containerEl.querySelector?.("[data-wedding-addons-count]");
  if (addonsCountEl) {
    addonsCountEl.textContent =
      pricing.addonsCount > 0
        ? String(t("weddingAddonsSelectedCount")).replace("{count}", String(pricing.addonsCount))
        : t("weddingNoAddonsSelected");
  }

  const grandTotalEl = containerEl.querySelector?.("[data-wedding-grand-total]");
  if (grandTotalEl) {
    grandTotalEl.textContent = pricing.totalPriceFormatted;
    grandTotalEl.setAttribute?.("data-wedding-grand-total", String(pricing.totalPriceInr));
  }

  const selectedSet = new Set(pricing.selectedAddonIds);
  const itemEls = containerEl.querySelectorAll?.("[data-wedding-addon-card]");
  if (itemEls) {
    for (const cardEl of itemEls) {
      const id = cardEl.getAttribute?.("data-wedding-addon-card") || "";
      cardEl.classList?.toggle("is-selected", selectedSet.has(id));
    }
  }

  return pricing;
}

/**
 * Renders the live selected Wedding Package summary card, interactive add-on checkboxes,
 * and dynamic total price calculator inside the Shoot Booking Form.
 */
export function renderBookingWeddingSummaryHtml(
  tier,
  {
    selectedAddonIds = [],
    addons = WEDDING_BOOKING_ADDONS,
    lang = "en",
    t = (k) => k,
    escapeHtml = defaultEscapeHtml
  } = {}
) {
  if (!tier) return "";
  const title = lang === "hi" ? tier.tierNameHi || tier.tierNameEn : tier.tierNameEn;
  const items = Array.isArray(tier.deliverablesEn) ? tier.deliverablesEn : [];
  const pricing = calculateWeddingBookingTotal(tier, selectedAddonIds, addons);
  const selectedSet = new Set(pricing.selectedAddonIds);

  const addonsHtml = (Array.isArray(addons) ? addons : [])
    .map((addon) => {
      const isChecked = selectedSet.has(addon.id);
      const addonName = lang === "hi" ? addon.nameHi || addon.nameEn : addon.nameEn;
      const addonDesc = lang === "hi" ? addon.descHi || addon.descEn : addon.descEn;
      const addonPriceText = `+${formatInrCurrency(addon.priceInr)}`;
      return `
        <label class="booking-wedding-addon-item ${isChecked ? "is-selected" : ""}" data-wedding-addon-card="${escapeHtml(addon.id)}">
          <input
            type="checkbox"
            class="booking-wedding-addon-checkbox"
            value="${escapeHtml(addon.id)}"
            data-wedding-addon-id="${escapeHtml(addon.id)}"
            data-addon-price="${escapeHtml(addon.priceInr)}"
            ${isChecked ? "checked" : ""}
          />
          <span class="booking-wedding-addon-copy">
            <strong>${escapeHtml(addonName)}</strong>
            <span>${escapeHtml(addonDesc)}</span>
          </span>
          <span class="booking-wedding-addon-price">${escapeHtml(addonPriceText)}</span>
        </label>`;
    })
    .join("");

  const addonsCountText =
    pricing.addonsCount > 0
      ? String(t("weddingAddonsSelectedCount")).replace("{count}", String(pricing.addonsCount))
      : t("weddingNoAddonsSelected");

  return `
    <div class="booking-wedding-summary-card" data-booking-wedding-tier="${escapeHtml(tier.id)}">
      <div class="booking-wedding-summary-head">
        <div>
          <span class="badge">${escapeHtml(t("weddingSelectedBadge"))}</span>
          <strong style="display:block;margin-top:4px;color:var(--gold-light);font-size:1rem;">${escapeHtml(title)}</strong>
          <span style="font-size:0.8rem;color:var(--text-secondary);">${escapeHtml(tier.durationEn)} • ${escapeHtml(tier.turnaroundEn)}</span>
        </div>
        <strong class="booking-wedding-price">${escapeHtml(pricing.basePriceFormatted)}</strong>
      </div>
      <div style="font-size:0.78rem;font-weight:700;color:var(--gold);margin-top:8px;">${escapeHtml(t("weddingIncludedServicesLabel"))}:</div>
      <ul class="booking-wedding-list">
        ${items.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}
      </ul>

      <div class="booking-wedding-addons-section">
        <div class="booking-wedding-addons-head">
          <strong>${escapeHtml(t("weddingAddonsHeading"))}</strong>
          <span>${escapeHtml(t("weddingAddonsHint"))}</span>
        </div>
        <div class="booking-wedding-addons-grid">
          ${addonsHtml}
        </div>
      </div>

      <div id="bookingWeddingTotalBox" class="booking-wedding-total-box" aria-live="polite">
        <div class="booking-wedding-total-row">
          <span>${escapeHtml(t("weddingBasePackageLabel"))}</span>
          <strong data-wedding-base-price>${escapeHtml(pricing.basePriceFormatted)}</strong>
        </div>
        <div class="booking-wedding-total-row">
          <span>
            ${escapeHtml(t("weddingAddonsSubtotalLabel"))}
            <small class="booking-wedding-addons-count" data-wedding-addons-count>${escapeHtml(addonsCountText)}</small>
          </span>
          <strong data-wedding-addons-total>${escapeHtml(pricing.addonsTotalInr > 0 ? `+${pricing.addonsTotalFormatted}` : "₹0")}</strong>
        </div>
        <div class="booking-wedding-total-row is-grand">
          <span>${escapeHtml(t("weddingEstimatedTotalLabel"))}</span>
          <strong
            id="bookingWeddingGrandTotal"
            class="booking-wedding-grand-total"
            data-wedding-grand-total="${escapeHtml(pricing.totalPriceInr)}"
          >${escapeHtml(pricing.totalPriceFormatted)}</strong>
        </div>
      </div>
    </div>`;
}

/**
 * Renders the Admin Dashboard "Wedding Packages" tab where Admin configures Price and Included Services
 * for Basic Wedding Package, Premium Wedding Package, and Luxury Wedding Package.
 */
export function renderAdminWeddingPackagesSectionHtml({
  services = [],
  savingTierId = null,
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const weddingPkg = (Array.isArray(services) ? services : []).find((p) => p.id === WEDDING_PACKAGE_ID);
  const tiers = weddingPkg?.tiers || DEFAULT_WEDDING_TIERS;

  const cardsHtml = tiers
    .map((tier) => {
      const isSaving = savingTierId === tier.id;
      const servicesText = (tier.deliverablesEn || []).join("\n");
      return `
      <form class="studio-card admin-wedding-tier-card" data-admin-wedding-form="${escapeHtml(tier.id)}" novalidate>
        <div class="admin-wedding-tier-head">
          <div>
            <span class="badge ${tier.isCustomized ? "status-approved" : ""}">
              ${escapeHtml(tier.isCustomized ? t("adminWeddingCustomBadge") : t("adminWeddingDefaultBadge"))}
            </span>
            <h3 style="margin:6px 0 2px;color:var(--gold-light);font-size:1.1rem;">${escapeHtml(tier.tierNameEn)}</h3>
            <span style="font-size:0.8rem;color:var(--text-secondary);">${escapeHtml(tier.tierNameHi)} • ID: ${escapeHtml(tier.id)}</span>
          </div>
          <div class="admin-wedding-live-price" data-admin-wedding-price-preview="${escapeHtml(tier.id)}">
            ${escapeHtml(tier.priceFormatted)}
          </div>
        </div>

        <div class="form-grid two-col">
          <div class="field">
            <label for="admWedPrice_${escapeHtml(tier.id)}">${escapeHtml(t("adminWeddingPriceLabel"))}</label>
            <input
              id="admWedPrice_${escapeHtml(tier.id)}"
              name="priceInr"
              type="number"
              class="input"
              min="${MIN_WEDDING_PRICE_INR}"
              max="${MAX_WEDDING_PRICE_INR}"
              step="500"
              required
              value="${escapeHtml(tier.priceInr)}"
              data-admin-wedding-price-input="${escapeHtml(tier.id)}"
            />
          </div>
          <div class="field">
            <label for="admWedDuration_${escapeHtml(tier.id)}">${escapeHtml(t("adminWeddingDurationLabel"))}</label>
            <input
              id="admWedDuration_${escapeHtml(tier.id)}"
              name="durationEn"
              type="text"
              class="input"
              required
              maxlength="100"
              value="${escapeHtml(tier.durationEn)}"
            />
          </div>
          <div class="field full">
            <label for="admWedTurnaround_${escapeHtml(tier.id)}">${escapeHtml(t("adminWeddingTurnaroundLabel"))}</label>
            <input
              id="admWedTurnaround_${escapeHtml(tier.id)}"
              name="turnaroundEn"
              type="text"
              class="input"
              required
              maxlength="100"
              value="${escapeHtml(tier.turnaroundEn)}"
            />
          </div>
          <div class="field full">
            <label for="admWedServices_${escapeHtml(tier.id)}">
              ${escapeHtml(t("adminWeddingServicesLabel"))} (${(tier.deliverablesEn || []).length}/${MAX_WEDDING_SERVICES_COUNT})
            </label>
            <textarea
              id="admWedServices_${escapeHtml(tier.id)}"
              name="includedServices"
              class="input"
              rows="5"
              required
              placeholder="${escapeHtml(t("adminWeddingServicesPlaceholder"))}"
            >${escapeHtml(servicesText)}</textarea>
            <span style="font-size:0.76rem;color:var(--text-muted);">${escapeHtml(t("adminWeddingServicesHint"))}</span>
          </div>
        </div>

        <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
          <button
            type="submit"
            class="btn-gold btn-sm"
            ${isSaving ? "disabled" : ""}
            data-admin-wedding-save="${escapeHtml(tier.id)}"
          >${escapeHtml(isSaving ? t("adminWeddingSavingBtn") : t("adminWeddingSaveBtn"))}</button>
          <button
            type="button"
            class="btn-outline btn-sm"
            ${isSaving ? "disabled" : ""}
            data-admin-wedding-reset="${escapeHtml(tier.id)}"
          >${escapeHtml(t("adminWeddingResetBtn"))}</button>
        </div>
      </form>`;
    })
    .join("");

  return `
    <div class="studio-card admin-verification-banner">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
        <div>
          <strong style="color:var(--gold);display:block;">${escapeHtml(t("adminWeddingBannerTitle"))}</strong>
          <span style="font-size:0.84rem;color:var(--text-secondary);">${escapeHtml(t("adminWeddingBannerDesc"))}</span>
        </div>
        <span class="badge status-approved">3 Wedding Tiers</span>
      </div>
    </div>
    <div class="admin-wedding-tiers-grid">
      ${cardsHtml}
    </div>`;
}
