/**
 * STAR ARTIST STUDIO — Interactive Booking Date-Picker Calendar & Studio Slot Availability
 *
 * Highlights available, partially booked, and fully booked studio dates and prevents
 * double-booking using privacy-safe studio slot occupancy records (zero customer PII).
 */

export const STUDIO_TIME_SLOTS = [
  { code: "morning_gold", value: "10:00 AM", labelEn: "10:00 AM (Morning Gold)", labelHi: "सुबह 10:00 (मॉर्निंग गोल्ड)" },
  { code: "main_bay", value: "11:30 AM", labelEn: "11:30 AM (Main Bay)", labelHi: "सुबह 11:30 (मेन बे)" },
  { code: "afternoon_cinema", value: "02:00 PM", labelEn: "02:00 PM (Afternoon Cinema)", labelHi: "दोपहर 02:00 (आफ्टरनून सिनेमा)" },
  { code: "golden_hour", value: "04:30 PM", labelEn: "04:30 PM (Golden Hour)", labelHi: "शाम 04:30 (गोल्डन ऑवर)" },
  { code: "gel_night", value: "06:30 PM", labelEn: "06:30 PM (Studio Gel Night)", labelHi: "शाम 06:30 (स्टूडियो जेल नाइट)" }
];

const NON_BLOCKING_STATUSES = new Set(["cancelled", "rejected"]);

function defaultEscapeHtml(str) {
  return String(str ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function isActiveBlockingBookingStatus(status = "") {
  const norm = String(status || "").trim().toLowerCase();
  if (!norm) return true;
  return !NON_BLOCKING_STATUSES.has(norm);
}

/**
 * Normalizes any preferredTime string (e.g. "10:00 AM", "10:00 AM (Morning Gold)")
 * into a deterministic studio slot code for Firestore slot locking.
 */
export function normalizeStudioSlotCode(preferredTime = "") {
  const raw = String(preferredTime || "").trim().toLowerCase();
  if (!raw) return "main_bay";

  for (const slot of STUDIO_TIME_SLOTS) {
    if (raw === slot.code || raw.startsWith(slot.value.toLowerCase())) {
      return slot.code;
    }
  }
  if (raw.includes("10:00") || raw.includes("morning")) return "morning_gold";
  if (raw.includes("11:00") || raw.includes("11:30") || raw.includes("main")) return "main_bay";
  if (raw.includes("02:00") || raw.includes("2:00") || raw.includes("14:00") || raw.includes("afternoon")) {
    return "afternoon_cinema";
  }
  if (raw.includes("04:30") || raw.includes("4:30") || raw.includes("16:30") || raw.includes("golden")) {
    return "golden_hour";
  }
  if (raw.includes("06:30") || raw.includes("6:30") || raw.includes("18:30") || raw.includes("night")) {
    return "gel_night";
  }
  const safeSlug = raw.replace(/[^a-z0-9]+/g, "_").replace(/^_+|_+$/g, "").slice(0, 32);
  return safeSlug || "main_bay";
}

export function isValidIsoDateString(dateStr = "") {
  return /^\d{4}-\d{2}-\d{2}$/.test(String(dateStr || "").trim());
}

export function buildStudioSlotKey(dateStr = "", preferredTime = "") {
  const cleanDate = String(dateStr || "").trim();
  if (!isValidIsoDateString(cleanDate)) return "";
  const code = normalizeStudioSlotCode(preferredTime);
  return `${cleanDate}__${code}`;
}

/**
 * Builds a privacy-safe occupancy index: Map<YYYY-MM-DD, Set<slotCode>>.
 * Combines studio-wide privacy-safe `bookedSlots` records with the signed-in user's own active bookings.
 */
export function buildBookedSlotIndex(bookedSlots = [], ownBookings = []) {
  const index = new Map();
  const mark = (dateStr, timeOrCode) => {
    const cleanDate = String(dateStr || "").trim();
    if (!isValidIsoDateString(cleanDate)) return;
    const code = normalizeStudioSlotCode(timeOrCode);
    if (!index.has(cleanDate)) index.set(cleanDate, new Set());
    index.get(cleanDate).add(code);
  };

  for (const slot of Array.isArray(bookedSlots) ? bookedSlots : []) {
    if (!slot || typeof slot !== "object") continue;
    if (!isActiveBlockingBookingStatus(slot.status || "reserved")) continue;
    mark(slot.date, slot.slotCode || slot.preferredTime);
  }

  for (const bk of Array.isArray(ownBookings) ? ownBookings : []) {
    if (!bk || typeof bk !== "object") continue;
    if (!isActiveBlockingBookingStatus(bk.status)) continue;
    mark(bk.preferredDate, bk.preferredTime);
  }

  return index;
}

export function isStudioSlotBooked(slotIndex, dateStr, preferredTime) {
  if (!(slotIndex instanceof Map) || !isValidIsoDateString(dateStr)) return false;
  const bookedSet = slotIndex.get(String(dateStr).trim());
  if (!bookedSet || !bookedSet.size) return false;
  return bookedSet.has(normalizeStudioSlotCode(preferredTime));
}

export function getDateSlotAvailability(slotIndex, dateStr) {
  const total = STUDIO_TIME_SLOTS.length;
  const bookedSet =
    slotIndex instanceof Map && isValidIsoDateString(dateStr)
      ? slotIndex.get(String(dateStr).trim()) || new Set()
      : new Set();
  const bookedCodes = STUDIO_TIME_SLOTS.filter((s) => bookedSet.has(s.code)).map((s) => s.code);
  const bookedCount = bookedCodes.length;
  const availableCount = Math.max(0, total - bookedCount);
  const state =
    availableCount === 0 ? "full" : bookedCount > 0 ? "partial" : "available";
  return {
    date: String(dateStr || ""),
    total,
    bookedCount,
    availableCount,
    bookedCodes,
    state
  };
}

export function getFirstAvailableTimeForDate(slotIndex, dateStr) {
  const bookedSet =
    slotIndex instanceof Map && isValidIsoDateString(dateStr)
      ? slotIndex.get(String(dateStr).trim()) || new Set()
      : new Set();
  const openSlot = STUDIO_TIME_SLOTS.find((s) => !bookedSet.has(s.code));
  return openSlot ? openSlot.value : "";
}

function pad2(n) {
  return String(n).padStart(2, "0");
}

export function formatIsoDateLocal(year, monthIndex, day) {
  return `${year}-${pad2(monthIndex + 1)}-${pad2(day)}`;
}

export function renderBookingCalendarHtml({
  viewYear,
  viewMonth,
  selectedDate = "",
  selectedTime = "",
  slotIndex = new Map(),
  minDateStr = "",
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const now = new Date();
  const fallbackMin = formatIsoDateLocal(now.getFullYear(), now.getMonth(), now.getDate() + 1);
  const effectiveMin = isValidIsoDateString(minDateStr) ? minDateStr : fallbackMin;

  let y = Number.isInteger(viewYear) ? viewYear : now.getFullYear();
  let m = Number.isInteger(viewMonth) ? viewMonth : now.getMonth();
  if (!Number.isInteger(viewYear) && isValidIsoDateString(selectedDate)) {
    const parts = selectedDate.split("-").map((x) => parseInt(x, 10));
    if (parts.length === 3 && !Number.isNaN(parts[0]) && !Number.isNaN(parts[1])) {
      y = parts[0];
      m = parts[1] - 1;
    }
  }

  const monthNamesEn = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  ];
  const monthNamesHi = [
    "जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून",
    "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
  ];
  const weekdays = lang === "hi"
    ? ["रवि", "सोम", "मंगल", "बुध", "गुरु", "शुक्र", "शनि"]
    : ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

  const monthTitle = `${(lang === "hi" ? monthNamesHi : monthNamesEn)[m] || monthNamesEn[0]} ${y}`;
  const firstDayOfWeek = new Date(y, m, 1).getDay();
  const daysInMonth = new Date(y, m + 1, 0).getDate();

  const cells = [];
  for (let i = 0; i < firstDayOfWeek; i += 1) {
    cells.push(`<span class="booking-cal-blank" aria-hidden="true"></span>`);
  }

  for (let day = 1; day <= daysInMonth; day += 1) {
    const iso = formatIsoDateLocal(y, m, day);
    const isPast = iso < effectiveMin;
    const avail = getDateSlotAvailability(slotIndex, iso);
    const isSelected = iso === selectedDate;
    const isFull = avail.state === "full";
    const disabled = isPast || isFull;
    const stateCls = isPast
      ? "is-past"
      : isFull
      ? "is-full"
      : avail.state === "partial"
      ? "is-partial"
      : "is-available";
    const titleText = isPast
      ? t("calPastDate")
      : isFull
      ? t("calFullyBooked")
      : `${avail.availableCount}/${avail.total} ${t("calSlotsOpen")}`;

    cells.push(`
      <button
        type="button"
        class="booking-cal-day ${stateCls} ${isSelected ? "is-selected" : ""}"
        data-cal-date="${iso}"
        ${disabled ? "disabled" : ""}
        aria-pressed="${isSelected ? "true" : "false"}"
        title="${escapeHtml(titleText)}"
      >
        <span class="booking-cal-num">${day}</span>
        ${!isPast ? `<span class="booking-cal-dot" aria-hidden="true"></span>` : ""}
      </button>
    `);
  }

  const activeDate = isValidIsoDateString(selectedDate) ? selectedDate : effectiveMin;
  const activeAvail = getDateSlotAvailability(slotIndex, activeDate);
  const selectedCode = normalizeStudioSlotCode(selectedTime);

  const slotChipsHtml = STUDIO_TIME_SLOTS.map((slot) => {
    const booked = isStudioSlotBooked(slotIndex, activeDate, slot.value);
    const isSelected = !booked && slot.code === selectedCode;
    const label = lang === "hi" ? slot.labelHi : slot.labelEn;
    return `
      <button
        type="button"
        class="booking-slot-chip ${booked ? "is-booked" : "is-open"} ${isSelected ? "is-selected" : ""}"
        data-cal-slot="${escapeHtml(slot.value)}"
        ${booked ? "disabled" : ""}
        aria-pressed="${isSelected ? "true" : "false"}"
      >
        <span>${escapeHtml(label)}</span>
        <span class="booking-slot-tag">${escapeHtml(booked ? t("calSlotBooked") : t("calSlotAvailable"))}</span>
      </button>
    `;
  }).join("");

  return `
    <div class="booking-calendar-card" data-booking-calendar="true">
      <div class="booking-cal-header">
        <div>
          <div class="booking-cal-eyebrow">${escapeHtml(t("calAvailabilityTitle"))}</div>
          <div class="booking-cal-month">${escapeHtml(monthTitle)}</div>
        </div>
        <div class="booking-cal-nav">
          <button type="button" class="btn-outline booking-cal-nav-btn" data-cal-nav="-1" aria-label="${escapeHtml(t("calPrevMonth"))}">‹</button>
          <button type="button" class="btn-outline booking-cal-nav-btn" data-cal-nav="1" aria-label="${escapeHtml(t("calNextMonth"))}">›</button>
        </div>
      </div>

      <div class="booking-cal-legend">
        <span><i class="legend-dot is-available"></i> ${escapeHtml(t("calLegendAvailable"))}</span>
        <span><i class="legend-dot is-partial"></i> ${escapeHtml(t("calLegendLimited"))}</span>
        <span><i class="legend-dot is-full"></i> ${escapeHtml(t("calLegendFull"))}</span>
      </div>

      <div class="booking-cal-weekdays">
        ${weekdays.map((w) => `<span>${escapeHtml(w)}</span>`).join("")}
      </div>
      <div class="booking-cal-grid">${cells.join("")}</div>

      <div class="booking-slot-picker">
        <div class="booking-slot-picker-head">
          <strong>${escapeHtml(t("calSlotsForDate"))} ${escapeHtml(activeDate)}</strong>
          <span class="booking-slot-count-badge ${activeAvail.state === "full" ? "is-full" : ""}">
            ${escapeHtml(`${activeAvail.availableCount}/${activeAvail.total} ${t("calSlotsOpen")}`)}
          </span>
        </div>
        <div class="booking-slot-grid">${slotChipsHtml}</div>
      </div>
    </div>
  `;
}
