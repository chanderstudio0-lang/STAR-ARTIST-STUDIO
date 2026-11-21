const DATE_ONLY_RE = /^(\d{4})-(\d{2})-(\d{2})$/;

function defaultEscapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function pad2(n) {
  return String(n).padStart(2, "0");
}

/**
 * Returns local calendar date key in YYYY-MM-DD format.
 */
export function getLocalDateKey(date = new Date()) {
  const d = date instanceof Date ? date : new Date(date);
  if (Number.isNaN(d.getTime())) return "";
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

/**
 * Safely parses an event date from YYYY-MM-DD strings, ISO strings,
 * Date objects, or Firestore Timestamp-like objects.
 */
export function parseEventDate(rawDate) {
  if (!rawDate) return null;

  if (typeof rawDate === "string") {
    const trimmed = rawDate.trim();
    if (!trimmed) return null;
    const match = DATE_ONLY_RE.exec(trimmed);
    if (match) {
      const year = Number(match[1]);
      const month = Number(match[2]);
      const day = Number(match[3]);
      const localDate = new Date(year, month - 1, day);
      if (
        Number.isNaN(localDate.getTime()) ||
        localDate.getFullYear() !== year ||
        localDate.getMonth() !== month - 1 ||
        localDate.getDate() !== day
      ) {
        return null;
      }
      return {
        dateKey: `${match[1]}-${match[2]}-${match[3]}`,
        isDateOnly: true,
        timestampMs: localDate.getTime()
      };
    }

    const parsed = new Date(trimmed);
    if (Number.isNaN(parsed.getTime())) return null;
    return {
      dateKey: getLocalDateKey(parsed),
      isDateOnly: false,
      timestampMs: parsed.getTime()
    };
  }

  if (rawDate instanceof Date) {
    if (Number.isNaN(rawDate.getTime())) return null;
    return {
      dateKey: getLocalDateKey(rawDate),
      isDateOnly: false,
      timestampMs: rawDate.getTime()
    };
  }

  if (typeof rawDate === "object") {
    if (typeof rawDate.toDate === "function") {
      return parseEventDate(rawDate.toDate());
    }
    if (typeof rawDate.seconds === "number") {
      return parseEventDate(new Date(rawDate.seconds * 1000));
    }
  }

  return null;
}

/**
 * Returns true strictly when an event's scheduled date has passed relative to `now`.
 * For date-only strings (YYYY-MM-DD), the event date is considered passed starting
 * the following calendar day so same-day events remain Confirmed during the event.
 */
export function hasEventDatePassed(booking, now = new Date()) {
  const rawDate = booking?.preferredDate || booking?.eventDate || "";
  const parsed = parseEventDate(rawDate);
  if (!parsed) return false;

  const refNow = now instanceof Date ? now : new Date(now);
  if (Number.isNaN(refNow.getTime())) return false;

  if (parsed.isDateOnly) {
    const todayKey = getLocalDateKey(refNow);
    return Boolean(todayKey) && parsed.dateKey < todayKey;
  }

  return parsed.timestampMs < refNow.getTime();
}

/**
 * Computes timeline state ("passed", "today", "upcoming", "unknown") and whole-day delta.
 */
export function getBookingTimelineState(booking, now = new Date()) {
  const rawDate = booking?.preferredDate || booking?.eventDate || "";
  const parsed = parseEventDate(rawDate);
  const refNow = now instanceof Date ? now : new Date(now);
  if (!parsed || Number.isNaN(refNow.getTime())) {
    return { state: "unknown", daysDelta: null, dateKey: "" };
  }

  const todayKey = getLocalDateKey(refNow);
  const todayMidnight = new Date(refNow.getFullYear(), refNow.getMonth(), refNow.getDate()).getTime();
  const [y, m, d] = parsed.dateKey.split("-").map(Number);
  const eventMidnight = new Date(y, m - 1, d).getTime();
  const daysDelta = Math.round((eventMidnight - todayMidnight) / 86400000);

  if (parsed.isDateOnly) {
    if (parsed.dateKey < todayKey) return { state: "passed", daysDelta, dateKey: parsed.dateKey };
    if (parsed.dateKey === todayKey) return { state: "today", daysDelta: 0, dateKey: parsed.dateKey };
    return { state: "upcoming", daysDelta, dateKey: parsed.dateKey };
  }

  if (parsed.timestampMs < refNow.getTime()) {
    return { state: "passed", daysDelta, dateKey: parsed.dateKey };
  }
  if (parsed.dateKey === todayKey) {
    return { state: "today", daysDelta: 0, dateKey: parsed.dateKey };
  }
  return { state: "upcoming", daysDelta, dateKey: parsed.dateKey };
}

/**
 * Finds all bookings currently in 'Confirmed' status whose event date has passed.
 */
export function findDueConfirmedBookings(bookings = [], now = new Date()) {
  if (!Array.isArray(bookings)) return [];
  return bookings.filter((b) => {
    if (!b || !b.id) return false;
    const status = String(b.status || "").trim().toLowerCase();
    return status === "confirmed" && hasEventDatePassed(b, now);
  });
}

/**
 * Computes summary counts for the Admin Real-Time Booking Status Tracker.
 */
export function summarizeRealtimeBookingTracker(bookings = [], now = new Date()) {
  const list = Array.isArray(bookings) ? bookings : [];
  const summary = {
    total: list.length,
    pending: 0,
    confirmed: 0,
    confirmedUpcoming: 0,
    confirmedToday: 0,
    confirmedPastDue: 0,
    completed: 0,
    cancelled: 0,
    dueBookings: []
  };

  for (const b of list) {
    const st = String(b?.status || "pending").trim().toLowerCase();
    if (st === "confirmed") {
      summary.confirmed += 1;
      const timeline = getBookingTimelineState(b, now);
      if (timeline.state === "passed") {
        summary.confirmedPastDue += 1;
        if (b?.id) summary.dueBookings.push(b);
      } else if (timeline.state === "today") {
        summary.confirmedToday += 1;
      } else {
        summary.confirmedUpcoming += 1;
      }
    } else if (st === "completed") {
      summary.completed += 1;
    } else if (st === "cancelled") {
      summary.cancelled += 1;
    } else {
      summary.pending += 1;
    }
  }

  return summary;
}

/**
 * Real-time status tracker controller that periodically checks Confirmed bookings
 * and automatically transitions past-date events from 'Confirmed' to 'Completed'.
 */
export function createRealtimeBookingTracker({
  getBookings = () => [],
  onAutoComplete = async () => ({ succeededIds: [], failedIds: [] }),
  onStateChange = () => {},
  intervalMs = 60000,
  nowProvider = () => new Date()
} = {}) {
  let timerId = null;
  let enabled = true;
  let running = false;
  let isSyncing = false;
  let isLiveConnected = false;
  let lastCheckedAt = null;
  let lastError = "";
  let totalAutoCompleted = 0;
  let recentTransitions = [];
  const inFlightIds = new Set();

  function getSnapshot() {
    return {
      enabled,
      running,
      isSyncing,
      isLiveConnected,
      lastCheckedAt,
      lastError,
      totalAutoCompleted,
      recentTransitions: [...recentTransitions]
    };
  }

  function emit() {
    onStateChange(getSnapshot());
  }

  async function evaluateNow({ force = false } = {}) {
    if (!enabled && !force) {
      return { transitioned: [], skipped: true };
    }
    const now = nowProvider();
    lastCheckedAt = now.toISOString();
    lastError = "";

    const bookings = getBookings() || [];
    const due = findDueConfirmedBookings(bookings, now).filter((b) => !inFlightIds.has(b.id));

    if (!due.length) {
      emit();
      return { transitioned: [], skipped: false };
    }

    due.forEach((b) => inFlightIds.add(b.id));
    isSyncing = true;
    emit();

    try {
      const result = (await onAutoComplete(due)) || {};
      const succeededSet = new Set(
        Array.isArray(result.succeededIds) ? result.succeededIds : due.map((b) => b.id)
      );
      const transitioned = due.filter((b) => succeededSet.has(b.id));

      if (transitioned.length > 0) {
        const stampedAt = nowProvider().toISOString();
        totalAutoCompleted += transitioned.length;
        const newEntries = transitioned.map((b) => ({
          id: b.id,
          clientName: b.clientName || "Studio Client",
          serviceType: b.serviceType || "Studio Shoot",
          preferredDate: b.preferredDate || b.eventDate || "",
          fromStatus: "Confirmed",
          toStatus: "Completed",
          transitionedAt: stampedAt
        }));
        recentTransitions = [...newEntries, ...recentTransitions].slice(0, 8);
      }

      if (Array.isArray(result.failedIds) && result.failedIds.length > 0) {
        lastError = `Could not auto-update ${result.failedIds.length} booking(s). Will retry on next check.`;
      }

      return { transitioned, skipped: false };
    } catch (err) {
      lastError = err?.message || "Auto-status sync failed.";
      return { transitioned: [], skipped: false, error: lastError };
    } finally {
      due.forEach((b) => inFlightIds.delete(b.id));
      isSyncing = false;
      emit();
    }
  }

  function start() {
    if (running) return;
    running = true;
    if (intervalMs > 0) {
      timerId = setInterval(() => {
        if (enabled) {
          evaluateNow();
        }
      }, intervalMs);
    }
    emit();
  }

  function stop() {
    running = false;
    isLiveConnected = false;
    if (timerId) {
      clearInterval(timerId);
      timerId = null;
    }
    inFlightIds.clear();
    emit();
  }

  function setEnabled(nextEnabled) {
    enabled = Boolean(nextEnabled);
    emit();
    if (enabled && running) {
      evaluateNow();
    }
  }

  function setLiveConnected(connected) {
    isLiveConnected = Boolean(connected);
    emit();
  }

  function resetSessionLog() {
    recentTransitions = [];
    totalAutoCompleted = 0;
    lastError = "";
    emit();
  }

  return {
    start,
    stop,
    evaluateNow,
    setEnabled,
    setLiveConnected,
    resetSessionLog,
    getSnapshot
  };
}

function formatShortTime(isoString, lang = "en") {
  if (!isoString) return "—";
  const d = new Date(isoString);
  if (Number.isNaN(d.getTime())) return "—";
  try {
    return d.toLocaleTimeString(lang === "hi" ? "hi-IN" : "en-IN", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit"
    });
  } catch {
    return d.toISOString().slice(11, 19);
  }
}

/**
 * Renders the per-booking timeline indicator chip on Admin Booking cards.
 */
export function renderBookingTimelineChipHtml(
  booking,
  { now = new Date(), lang = "en", t = (k) => k, escapeHtml = defaultEscapeHtml } = {}
) {
  const status = String(booking?.status || "").trim().toLowerCase();
  if (status === "completed") {
    return `<span class="tracker-timeline-chip is-completed">✓ ${escapeHtml(t("adminTrackerChipCompleted"))}</span>`;
  }
  if (status === "cancelled") {
    return "";
  }
  const timeline = getBookingTimelineState(booking, now);
  if (timeline.state === "passed") {
    const label =
      status === "confirmed"
        ? t("adminTrackerChipDueComplete")
        : t("adminTrackerChipDatePassed");
    return `<span class="tracker-timeline-chip is-passed">⏱ ${escapeHtml(label)}</span>`;
  }
  if (timeline.state === "today") {
    return `<span class="tracker-timeline-chip is-today">● ${escapeHtml(t("adminTrackerChipToday"))}</span>`;
  }
  if (timeline.state === "upcoming" && Number.isInteger(timeline.daysDelta)) {
    const label = lang === "hi"
      ? `${timeline.daysDelta} दिन शेष`
      : `In ${timeline.daysDelta}d`;
    return `<span class="tracker-timeline-chip is-upcoming">◷ ${escapeHtml(label)}</span>`;
  }
  return "";
}

/**
 * Renders the Real-Time Booking Status Tracker card for the Admin Dashboard.
 */
export function renderAdminBookingTrackerHtml({
  bookings = [],
  trackerState = {},
  now = new Date(),
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const summary = summarizeRealtimeBookingTracker(bookings, now);
  const enabled = trackerState.enabled !== false;
  const isSyncing = Boolean(trackerState.isSyncing);
  const isLive = Boolean(trackerState.isLiveConnected);
  const recent = Array.isArray(trackerState.recentTransitions) ? trackerState.recentTransitions : [];
  const lastChecked = formatShortTime(trackerState.lastCheckedAt, lang);

  const statusPillClass = !enabled
    ? "is-paused"
    : isSyncing
    ? "is-syncing"
    : "is-live";
  const statusPillText = !enabled
    ? t("adminTrackerPausedLabel")
    : isSyncing
    ? t("adminTrackerSyncingLabel")
    : isLive
    ? t("adminTrackerLiveLabel")
    : t("adminTrackerActiveLabel");

  const transitionsHtml = recent.length
    ? `
      <div class="booking-tracker-feed" aria-live="polite">
        <div class="booking-tracker-feed-title">${escapeHtml(t("adminTrackerRecentTitle"))}</div>
        <ul class="booking-tracker-feed-list">
          ${recent
            .map(
              (item) => `
            <li class="booking-tracker-feed-item">
              <div class="booking-tracker-feed-main">
                <strong>${escapeHtml(item.clientName)}</strong>
                <span>${escapeHtml(item.serviceType)} • ${escapeHtml(item.preferredDate)}</span>
              </div>
              <span class="booking-tracker-transition-pill">
                ${escapeHtml(item.fromStatus)} → ${escapeHtml(item.toStatus)}
              </span>
            </li>`
            )
            .join("")}
        </ul>
      </div>`
    : `<div class="booking-tracker-empty">${escapeHtml(
        summary.confirmedPastDue > 0
          ? t("adminTrackerPendingSyncHint").replace("{count}", String(summary.confirmedPastDue))
          : t("adminTrackerAllUpToDate")
      )}</div>`;

  const errorHtml = trackerState.lastError
    ? `<div class="booking-tracker-error" role="alert">${escapeHtml(trackerState.lastError)}</div>`
    : "";

  return `
    <div class="booking-tracker-header">
      <div>
        <span class="eyebrow">${escapeHtml(t("adminTrackerEyebrow"))}</span>
        <h3 id="adminBookingTrackerHeading" class="booking-tracker-title">${escapeHtml(t("adminTrackerTitle"))}</h3>
        <p class="booking-tracker-subtitle">${escapeHtml(t("adminTrackerSubtitle"))}</p>
      </div>
      <div class="booking-tracker-actions">
        <span class="booking-tracker-live-pill ${statusPillClass}">
          <span class="booking-tracker-pulse-dot" aria-hidden="true"></span>
          <span>${escapeHtml(statusPillText)}</span>
        </span>
        <button
          type="button"
          id="adminTrackerToggleBtn"
          class="chip-btn ${enabled ? "active" : ""}"
          data-tracker-toggle
          aria-pressed="${enabled ? "true" : "false"}"
        >${escapeHtml(enabled ? t("adminTrackerAutoOnBtn") : t("adminTrackerAutoOffBtn"))}</button>
        <button
          type="button"
          id="adminTrackerCheckNowBtn"
          class="btn-outline btn-sm"
          data-tracker-check-now
          ${isSyncing ? "disabled" : ""}
        >${escapeHtml(t("adminTrackerCheckNowBtn"))}</button>
      </div>
    </div>

    <div class="booking-tracker-kpis">
      <div class="booking-tracker-kpi">
        <span class="booking-tracker-kpi-label">${escapeHtml(t("adminTrackerKpiUpcoming"))}</span>
        <strong class="booking-tracker-kpi-value">${summary.confirmedUpcoming}</strong>
      </div>
      <div class="booking-tracker-kpi">
        <span class="booking-tracker-kpi-label">${escapeHtml(t("adminTrackerKpiToday"))}</span>
        <strong class="booking-tracker-kpi-value is-gold">${summary.confirmedToday}</strong>
      </div>
      <div class="booking-tracker-kpi">
        <span class="booking-tracker-kpi-label">${escapeHtml(t("adminTrackerKpiDue"))}</span>
        <strong class="booking-tracker-kpi-value ${summary.confirmedPastDue > 0 ? "is-alert" : ""}">${summary.confirmedPastDue}</strong>
      </div>
      <div class="booking-tracker-kpi">
        <span class="booking-tracker-kpi-label">${escapeHtml(t("adminTrackerKpiCompleted"))}</span>
        <strong class="booking-tracker-kpi-value is-emerald">${summary.completed}</strong>
      </div>
    </div>

    ${errorHtml}
    ${transitionsHtml}

    <div class="booking-tracker-footer">
      <span>${escapeHtml(t("adminTrackerRuleSummary"))}</span>
      <span>${escapeHtml(t("adminTrackerLastChecked"))}: <strong>${escapeHtml(lastChecked)}</strong></span>
    </div>
  `;
}
