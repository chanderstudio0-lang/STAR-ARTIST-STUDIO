import {
  loadRechartsRuntime,
  normalizeApplicationDate,
  toDateKey
} from "./admin-model-growth.js";
import { formatInrAmount } from "./payment-system.js";

export const PAYMENT_TRENDS_DEFAULT_DAYS = 30;
export const PAYMENT_TRENDS_DAY_OPTIONS = [7, 14, 30];

export const SUCCESSFUL_PAYMENT_STATUSES = new Set([
  "Payment Verified",
  "Partially Paid",
  "Fully Paid"
]);

export const REJECTED_PAYMENT_STATUSES = new Set([
  "Payment Rejected"
]);

const paymentTrendsRoots = new WeakMap();

function defaultEscapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function formatShortDayLabel(date, lang = "en", { utc = false } = {}) {
  try {
    return new Intl.DateTimeFormat(lang === "hi" ? "hi-IN" : "en-US", {
      month: "short",
      day: "numeric",
      timeZone: utc ? "UTC" : undefined
    }).format(date);
  } catch {
    return toDateKey(date, { utc }).slice(5);
  }
}

function formatFullDayLabel(date, lang = "en", { utc = false } = {}) {
  try {
    return new Intl.DateTimeFormat(lang === "hi" ? "hi-IN" : "en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
      timeZone: utc ? "UTC" : undefined
    }).format(date);
  } catch {
    return toDateKey(date, { utc });
  }
}

export function formatCompactInrAxis(value) {
  const num = Math.max(0, Math.round(Number(value) || 0));
  if (num >= 100000) {
    const lakhs = num / 100000;
    return `₹${Number.isInteger(lakhs) ? lakhs : lakhs.toFixed(1)}L`;
  }
  if (num >= 1000) {
    const k = num / 1000;
    return `₹${Number.isInteger(k) ? k : k.toFixed(1)}k`;
  }
  return `₹${num}`;
}

/**
 * Classifies a bookingPayments record into "successful", "rejected", or "other".
 */
export function classifyPaymentOutcome(payment) {
  const status = String(payment?.paymentStatus || "").trim();
  if (REJECTED_PAYMENT_STATUSES.has(status)) return "rejected";
  if (SUCCESSFUL_PAYMENT_STATUSES.has(status)) return "successful";
  return "other";
}

/**
 * Resolves the canonical event date for a payment transaction to assign it to a daily bucket.
 */
export function resolvePaymentTrendDate(payment) {
  if (!payment || typeof payment !== "object") return null;
  const outcome = classifyPaymentOutcome(payment);
  const candidates =
    outcome === "successful"
      ? [payment.verifiedAt, payment.updatedAt, payment.submittedAt, payment.createdAt]
      : outcome === "rejected"
      ? [payment.rejectedAt, payment.updatedAt, payment.submittedAt, payment.createdAt]
      : [payment.submittedAt, payment.updatedAt, payment.createdAt];

  for (const candidate of candidates) {
    const dt = normalizeApplicationDate(candidate);
    if (dt) return dt;
  }

  // Fallback to latest timestamp inside history[] if top-level timestamps are absent
  if (Array.isArray(payment.history) && payment.history.length > 0) {
    for (let i = payment.history.length - 1; i >= 0; i -= 1) {
      const dt = normalizeApplicationDate(payment.history[i]?.at);
      if (dt) return dt;
    }
  }
  return null;
}

/**
 * Aggregates booking payment records into a contiguous N-day daily series
 * tracking daily total INR collection and successful vs. rejected transaction counts.
 */
export function buildPaymentTrendsSeries(
  payments = [],
  { days = PAYMENT_TRENDS_DEFAULT_DAYS, now = new Date(), lang = "en", utc = false } = {}
) {
  const safeDays = Math.max(1, Math.min(90, Math.floor(Number(days) || PAYMENT_TRENDS_DEFAULT_DAYS)));
  const anchor = normalizeApplicationDate(now) || new Date();
  const anchorStart = utc
    ? new Date(Date.UTC(anchor.getUTCFullYear(), anchor.getUTCMonth(), anchor.getUTCDate()))
    : new Date(anchor.getFullYear(), anchor.getMonth(), anchor.getDate());

  const buckets = [];
  const byKey = new Map();

  for (let offset = safeDays - 1; offset >= 0; offset -= 1) {
    const dayDate = utc
      ? new Date(Date.UTC(anchorStart.getUTCFullYear(), anchorStart.getUTCMonth(), anchorStart.getUTCDate() - offset))
      : new Date(anchorStart.getFullYear(), anchorStart.getMonth(), anchorStart.getDate() - offset);
    const key = toDateKey(dayDate, { utc });
    const entry = {
      dateKey: key,
      label: formatShortDayLabel(dayDate, lang, { utc }),
      fullDate: formatFullDayLabel(dayDate, lang, { utc }),
      totalCollection: 0,
      successfulCount: 0,
      rejectedCount: 0,
      transactionCount: 0
    };
    buckets.push(entry);
    byKey.set(key, entry);
  }

  for (const payment of Array.isArray(payments) ? payments : []) {
    const outcome = classifyPaymentOutcome(payment);
    if (outcome !== "successful" && outcome !== "rejected") continue;

    const dt = resolvePaymentTrendDate(payment);
    if (!dt) continue;
    const key = toDateKey(dt, { utc });
    const bucket = byKey.get(key);
    if (!bucket) continue;

    if (outcome === "successful") {
      const collected = Math.max(0, Math.round(Number(payment?.paidAmount ?? payment?.verifiedAmount ?? 0) || 0));
      bucket.totalCollection += collected;
      bucket.successfulCount += 1;
      bucket.transactionCount += 1;
    } else if (outcome === "rejected") {
      bucket.rejectedCount += 1;
      bucket.transactionCount += 1;
    }
  }

  return buckets;
}

/**
 * Computes summary KPI metrics from the daily Payment Trends series.
 */
export function summarizePaymentTrends(series = [], allPayments = []) {
  const list = Array.isArray(series) ? series : [];
  const totalCollection = list.reduce((sum, pt) => sum + Math.max(0, Number(pt?.totalCollection || 0)), 0);
  const successfulCount = list.reduce((sum, pt) => sum + Math.max(0, Number(pt?.successfulCount || 0)), 0);
  const rejectedCount = list.reduce((sum, pt) => sum + Math.max(0, Number(pt?.rejectedCount || 0)), 0);
  const decidedCount = successfulCount + rejectedCount;
  const successRate = decidedCount > 0 ? Math.round((successfulCount / decidedCount) * 100) : 0;

  const last7 = list.slice(-7);
  const prev7 = list.slice(-14, -7);
  const last7Collection = last7.reduce((sum, pt) => sum + Math.max(0, Number(pt?.totalCollection || 0)), 0);
  const prev7Collection = prev7.reduce((sum, pt) => sum + Math.max(0, Number(pt?.totalCollection || 0)), 0);

  let trendPercent = 0;
  let trendDirection = "flat";
  if (prev7Collection === 0 && last7Collection > 0) {
    trendPercent = 100;
    trendDirection = "up";
  } else if (prev7Collection > 0) {
    trendPercent = Math.round(((last7Collection - prev7Collection) / prev7Collection) * 100);
    trendDirection = trendPercent > 0 ? "up" : trendPercent < 0 ? "down" : "flat";
  }

  let peakDay = null;
  for (const pt of list) {
    if (!peakDay || pt.totalCollection > peakDay.totalCollection) {
      peakDay = {
        dateKey: pt.dateKey,
        label: pt.label,
        totalCollection: pt.totalCollection
      };
    }
  }
  if (peakDay && peakDay.totalCollection <= 0) {
    peakDay = null;
  }

  const allTimeCollection = (Array.isArray(allPayments) ? allPayments : []).reduce((sum, p) => {
    if (classifyPaymentOutcome(p) !== "successful") return sum;
    return sum + Math.max(0, Math.round(Number(p?.paidAmount ?? p?.verifiedAmount ?? 0) || 0));
  }, 0);

  return {
    days: list.length,
    totalCollection,
    successfulCount,
    rejectedCount,
    decidedCount,
    successRate,
    last7Collection,
    prev7Collection,
    trendPercent,
    trendDirection,
    peakDay,
    allTimeCollection
  };
}

export function getPaymentTrendsPalette(theme = "dark") {
  if (theme === "light") {
    return {
      gold: "#9A6E00",
      goldLight: "#D4AF37",
      emerald: "#15803D",
      crimson: "#DC2626",
      grid: "rgba(139, 98, 0, 0.16)",
      axis: "#57534E",
      tooltipBg: "#FFFFFF",
      tooltipBorder: "rgba(154, 110, 0, 0.38)",
      tooltipText: "#1C1917",
      tooltipMuted: "#57534E"
    };
  }
  return {
    gold: "#D4AF37",
    goldLight: "#F3D87C",
    emerald: "#4ADE80",
    crimson: "#F87171",
    grid: "rgba(212, 175, 55, 0.18)",
    axis: "#B8B0A0",
    tooltipBg: "#141414",
    tooltipBorder: "rgba(212, 175, 55, 0.5)",
    tooltipText: "#F5F5F4",
    tooltipMuted: "#B8B0A0"
  };
}

/**
 * Builds the dual Recharts visualization (Daily Total Collection + Successful vs. Rejected Transactions)
 * using React.createElement (no JSX build step required).
 */
export function createRechartsPaymentTrendsElement(
  React,
  Recharts,
  { series = [], theme = "dark", labels = {} } = {}
) {
  const h = React.createElement;
  const {
    ResponsiveContainer,
    AreaChart,
    Area,
    BarChart,
    Bar,
    CartesianGrid,
    XAxis,
    YAxis,
    Tooltip,
    Legend
  } = Recharts;

  const palette = getPaymentTrendsPalette(theme);
  const collectionTitle = labels.collectionChartTitle || "Daily Total Collection (₹)";
  const countsTitle = labels.countsChartTitle || "Successful vs. Rejected Transactions";
  const collectionSeriesLabel = labels.collectionSeries || "Daily Collection";
  const successSeriesLabel = labels.successSeries || "Successful";
  const rejectedSeriesLabel = labels.rejectedSeries || "Rejected";

  const tooltipStyle = {
    backgroundColor: palette.tooltipBg,
    border: `1px solid ${palette.tooltipBorder}`,
    borderRadius: "12px",
    color: palette.tooltipText,
    fontSize: "12px",
    boxShadow: "0 10px 28px rgba(0, 0, 0, 0.35)"
  };

  const collectionChart = h(
    ResponsiveContainer,
    { width: "100%", height: 245, minWidth: 200 },
    h(
      AreaChart,
      {
        data: series,
        margin: { top: 12, right: 12, left: 4, bottom: 4 }
      },
      h(
        "defs",
        { key: "defs" },
        h(
          "linearGradient",
          { id: "paymentTrendsGoldFill", x1: "0", y1: "0", x2: "0", y2: "1" },
          h("stop", { offset: "5%", stopColor: palette.gold, stopOpacity: 0.45 }),
          h("stop", { offset: "95%", stopColor: palette.gold, stopOpacity: 0.03 })
        )
      ),
      h(CartesianGrid, {
        key: "grid-col",
        strokeDasharray: "3 3",
        vertical: false,
        stroke: palette.grid
      }),
      h(XAxis, {
        key: "xaxis-col",
        dataKey: "label",
        tick: { fill: palette.axis, fontSize: 11 },
        axisLine: { stroke: palette.grid },
        tickLine: false,
        minTickGap: 20
      }),
      h(YAxis, {
        key: "yaxis-col",
        width: 52,
        tickFormatter: formatCompactInrAxis,
        tick: { fill: palette.axis, fontSize: 11 },
        axisLine: false,
        tickLine: false
      }),
      h(Tooltip, {
        key: "tooltip-col",
        contentStyle: tooltipStyle,
        labelStyle: { color: palette.gold, fontWeight: 700, marginBottom: "4px" },
        formatter: (val) => [formatInrAmount(val), collectionSeriesLabel],
        labelFormatter: (_label, payload) => payload?.[0]?.payload?.fullDate || _label
      }),
      h(Area, {
        key: "area-collection",
        type: "monotone",
        dataKey: "totalCollection",
        name: collectionSeriesLabel,
        stroke: palette.gold,
        strokeWidth: 2.5,
        fill: "url(#paymentTrendsGoldFill)",
        dot: false,
        activeDot: { r: 5, fill: palette.goldLight, stroke: palette.gold, strokeWidth: 2 }
      })
    )
  );

  const countsChartChildren = [
    h(CartesianGrid, {
      key: "grid-cnt",
      strokeDasharray: "3 3",
      vertical: false,
      stroke: palette.grid
    }),
    h(XAxis, {
      key: "xaxis-cnt",
      dataKey: "label",
      tick: { fill: palette.axis, fontSize: 11 },
      axisLine: { stroke: palette.grid },
      tickLine: false,
      minTickGap: 20
    }),
    h(YAxis, {
      key: "yaxis-cnt",
      allowDecimals: false,
      width: 30,
      tick: { fill: palette.axis, fontSize: 11 },
      axisLine: false,
      tickLine: false
    }),
    h(Tooltip, {
      key: "tooltip-cnt",
      contentStyle: tooltipStyle,
      labelStyle: { color: palette.gold, fontWeight: 700, marginBottom: "4px" },
      itemStyle: { padding: "1px 0" },
      labelFormatter: (_label, payload) => payload?.[0]?.payload?.fullDate || _label
    }),
    Legend
      ? h(Legend, {
          key: "legend-cnt",
          verticalAlign: "top",
          height: 26,
          iconType: "circle",
          wrapperStyle: { fontSize: "12px", color: palette.axis }
        })
      : null,
    h(Bar, {
      key: "bar-success",
      dataKey: "successfulCount",
      name: successSeriesLabel,
      fill: palette.emerald,
      radius: [6, 6, 0, 0],
      maxBarSize: 18
    }),
    h(Bar, {
      key: "bar-rejected",
      dataKey: "rejectedCount",
      name: rejectedSeriesLabel,
      fill: palette.crimson,
      radius: [6, 6, 0, 0],
      maxBarSize: 18
    })
  ].filter(Boolean);

  const countsChart = h(
    ResponsiveContainer,
    { width: "100%", height: 245, minWidth: 200 },
    h(
      BarChart,
      {
        data: series,
        margin: { top: 8, right: 12, left: -6, bottom: 4 }
      },
      ...countsChartChildren
    )
  );

  return h(
    "div",
    { className: "payment-trends-charts-grid" },
    h(
      "div",
      { className: "payment-trends-chart-panel", "data-payment-trend-chart": "collection" },
      h("div", { className: "payment-trends-panel-title" }, collectionTitle),
      collectionChart
    ),
    h(
      "div",
      { className: "payment-trends-chart-panel", "data-payment-trend-chart": "outcomes" },
      h("div", { className: "payment-trends-panel-title" }, countsTitle),
      countsChart
    )
  );
}

/**
 * Offline-safe SVG fallback renderer displaying both Daily Total Collection and
 * Successful vs. Rejected transaction counts when offline or CDN scripts cannot load.
 */
export function renderPaymentTrendsFallbackSvg(series = [], { theme = "dark", labels = {} } = {}) {
  const palette = getPaymentTrendsPalette(theme);
  const width = 560;
  const height = 215;
  const pad = { top: 18, right: 16, bottom: 30, left: 48 };
  const plotW = width - pad.left - pad.right;
  const plotH = height - pad.top - pad.bottom;
  const count = Math.max(1, series.length);

  // 1. Daily Collection SVG
  const maxCol = Math.max(1, ...series.map((p) => Number(p?.totalCollection || 0)));
  const xAt = (idx) => (count === 1 ? pad.left + plotW / 2 : pad.left + (idx / (count - 1)) * plotW);
  const yCol = (val) => pad.top + plotH - (Number(val || 0) / maxCol) * plotH;
  const linePoints = series.map((p, i) => `${xAt(i).toFixed(1)},${yCol(p.totalCollection).toFixed(1)}`).join(" ");
  const areaPath = series.length
    ? `M ${xAt(0).toFixed(1)},${(pad.top + plotH).toFixed(1)} ` +
      series.map((p, i) => `L ${xAt(i).toFixed(1)},${yCol(p.totalCollection).toFixed(1)}`).join(" ") +
      ` L ${xAt(series.length - 1).toFixed(1)},${(pad.top + plotH).toFixed(1)} Z`
    : "";

  const tickIndices = [0, Math.floor(count / 3), Math.floor((2 * count) / 3), count - 1];
  const uniqueTicks = [...new Set(tickIndices.filter((i) => i >= 0 && i < series.length))];

  // 2. Successful vs Rejected Bar SVG
  const maxCnt = Math.max(1, ...series.map((p) => Math.max(Number(p?.successfulCount || 0), Number(p?.rejectedCount || 0))));
  const slotW = plotW / count;
  const barW = Math.max(2, Math.min(12, slotW * 0.36));
  const barsSvg = series
    .map((p, i) => {
      const baseX = pad.left + i * slotW + slotW / 2;
      const sH = (Number(p?.successfulCount || 0) / maxCnt) * plotH;
      const rH = (Number(p?.rejectedCount || 0) / maxCnt) * plotH;
      const sRect = sH > 0
        ? `<rect x="${(baseX - barW - 1).toFixed(1)}" y="${(pad.top + plotH - sH).toFixed(1)}" width="${barW.toFixed(1)}" height="${sH.toFixed(1)}" rx="2" fill="${palette.emerald}" />`
        : "";
      const rRect = rH > 0
        ? `<rect x="${(baseX + 1).toFixed(1)}" y="${(pad.top + plotH - rH).toFixed(1)}" width="${barW.toFixed(1)}" height="${rH.toFixed(1)}" rx="2" fill="${palette.crimson}" />`
        : "";
      return `${sRect}${rRect}`;
    })
    .join("");

  const collectionTitle = defaultEscapeHtml(labels.collectionChartTitle || "Daily Total Collection (₹)");
  const countsTitle = defaultEscapeHtml(labels.countsChartTitle || "Successful vs. Rejected Transactions");

  return `
    <div class="payment-trends-charts-grid">
      <div class="payment-trends-chart-panel" data-payment-trend-chart="collection">
        <div class="payment-trends-panel-title">${collectionTitle}</div>
        <svg class="payment-trends-fallback-svg" viewBox="0 0 ${width} ${height}" role="img" aria-label="${collectionTitle}">
          <defs>
            <linearGradient id="ptFallbackGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="${palette.gold}" stop-opacity="0.38" />
              <stop offset="100%" stop-color="${palette.gold}" stop-opacity="0.02" />
            </linearGradient>
          </defs>
          <line x1="${pad.left}" y1="${pad.top}" x2="${width - pad.right}" y2="${pad.top}" stroke="${palette.grid}" stroke-dasharray="3 3" />
          <line x1="${pad.left}" y1="${(pad.top + plotH / 2).toFixed(1)}" x2="${width - pad.right}" y2="${(pad.top + plotH / 2).toFixed(1)}" stroke="${palette.grid}" stroke-dasharray="3 3" />
          <line x1="${pad.left}" y1="${pad.top + plotH}" x2="${width - pad.right}" y2="${pad.top + plotH}" stroke="${palette.grid}" />
          ${areaPath ? `<path d="${areaPath}" fill="url(#ptFallbackGrad)" />` : ""}
          ${linePoints ? `<polyline points="${linePoints}" fill="none" stroke="${palette.gold}" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round" />` : ""}
          <text x="${pad.left - 8}" y="${pad.top + 4}" text-anchor="end" font-size="11" fill="${palette.axis}">${defaultEscapeHtml(formatCompactInrAxis(maxCol))}</text>
          <text x="${pad.left - 8}" y="${pad.top + plotH + 4}" text-anchor="end" font-size="11" fill="${palette.axis}">₹0</text>
          ${uniqueTicks
            .map(
              (idx) =>
                `<text x="${xAt(idx).toFixed(1)}" y="${height - 8}" text-anchor="middle" font-size="11" fill="${palette.axis}">${defaultEscapeHtml(series[idx].label)}</text>`
            )
            .join("")}
        </svg>
      </div>
      <div class="payment-trends-chart-panel" data-payment-trend-chart="outcomes">
        <div class="payment-trends-panel-title">${countsTitle}</div>
        <svg class="payment-trends-fallback-svg" viewBox="0 0 ${width} ${height}" role="img" aria-label="${countsTitle}">
          <line x1="${pad.left}" y1="${pad.top}" x2="${width - pad.right}" y2="${pad.top}" stroke="${palette.grid}" stroke-dasharray="3 3" />
          <line x1="${pad.left}" y1="${(pad.top + plotH / 2).toFixed(1)}" x2="${width - pad.right}" y2="${(pad.top + plotH / 2).toFixed(1)}" stroke="${palette.grid}" stroke-dasharray="3 3" />
          <line x1="${pad.left}" y1="${pad.top + plotH}" x2="${width - pad.right}" y2="${pad.top + plotH}" stroke="${palette.grid}" />
          ${barsSvg}
          <text x="${pad.left - 8}" y="${pad.top + 4}" text-anchor="end" font-size="11" fill="${palette.axis}">${maxCnt}</text>
          <text x="${pad.left - 8}" y="${pad.top + plotH + 4}" text-anchor="end" font-size="11" fill="${palette.axis}">0</text>
          ${uniqueTicks
            .map(
              (idx) =>
                `<text x="${(pad.left + idx * slotW + slotW / 2).toFixed(1)}" y="${height - 8}" text-anchor="middle" font-size="11" fill="${palette.axis}">${defaultEscapeHtml(series[idx].label)}</text>`
            )
            .join("")}
        </svg>
      </div>
    </div>`;
}

export function renderPaymentTrendsMetricsHtml(
  summary,
  { t = (k) => k, escapeHtml = defaultEscapeHtml } = {}
) {
  const trendSign = summary.trendDirection === "up" ? "+" : "";
  const trendLabel = summary.trendDirection === "flat" ? "0%" : `${trendSign}${summary.trendPercent}%`;
  const trendCls =
    summary.trendDirection === "up"
      ? "is-up"
      : summary.trendDirection === "down"
      ? "is-down"
      : "is-flat";
  const peakText = summary.peakDay
    ? `${formatInrAmount(summary.peakDay.totalCollection)} (${summary.peakDay.label})`
    : "₹0";

  return `
    <div class="payment-trends-kpi is-gold">
      <span class="payment-trends-kpi-label">${escapeHtml(t("adminPayTrendsTotalCollection"))}</span>
      <div class="payment-trends-kpi-row">
        <strong class="payment-trends-kpi-value">${escapeHtml(formatInrAmount(summary.totalCollection))}</strong>
        <span class="model-growth-trend ${trendCls}" title="${escapeHtml(t("adminGrowthVsPrev7d"))}">${escapeHtml(trendLabel)}</span>
      </div>
    </div>
    <div class="payment-trends-kpi is-success">
      <span class="payment-trends-kpi-label">${escapeHtml(t("adminPayTrendsSuccessful"))}</span>
      <div class="payment-trends-kpi-row">
        <strong class="payment-trends-kpi-value">${escapeHtml(summary.successfulCount)}</strong>
        <span class="payment-trends-rate">${escapeHtml(summary.successRate)}%</span>
      </div>
    </div>
    <div class="payment-trends-kpi is-rejected">
      <span class="payment-trends-kpi-label">${escapeHtml(t("adminPayTrendsRejected"))}</span>
      <strong class="payment-trends-kpi-value">${escapeHtml(summary.rejectedCount)}</strong>
    </div>
    <div class="payment-trends-kpi">
      <span class="payment-trends-kpi-label">${escapeHtml(t("adminPayTrendsPeakDay"))}</span>
      <strong class="payment-trends-kpi-value">${escapeHtml(peakText)}</strong>
    </div>`;
}

export function unmountPaymentTrendsChart(chartHostEl) {
  if (!chartHostEl) return;
  const existingRoot = paymentTrendsRoots.get(chartHostEl);
  if (existingRoot && typeof existingRoot.unmount === "function") {
    try {
      existingRoot.unmount();
    } catch {
      // Ignore unmount errors on detached nodes
    }
    paymentTrendsRoots.delete(chartHostEl);
  }
  chartHostEl.innerHTML = "";
}

/**
 * Mounts or updates the Admin Payment Trends Recharts section (Daily Total Collection +
 * Successful vs. Rejected transaction counts).
 */
export async function mountPaymentTrendsChart(
  chartHostEl,
  {
    payments = [],
    metricsEl = null,
    statusEl = null,
    days = PAYMENT_TRENDS_DEFAULT_DAYS,
    theme = "dark",
    lang = "en",
    now = new Date(),
    t = (k) => k,
    escapeHtml = defaultEscapeHtml,
    loaders = null
  } = {}
) {
  const series = buildPaymentTrendsSeries(payments, { days, now, lang });
  const summary = summarizePaymentTrends(series, payments);

  if (metricsEl) {
    metricsEl.innerHTML = renderPaymentTrendsMetricsHtml(summary, { t, escapeHtml });
  }

  const labels = {
    collectionChartTitle: t("adminPayTrendsCollectionTitle"),
    countsChartTitle: t("adminPayTrendsCountsTitle"),
    collectionSeries: t("adminPayTrendsSeriesCollection"),
    successSeries: t("adminPayTrendsSeriesSuccess"),
    rejectedSeries: t("adminPayTrendsSeriesRejected")
  };

  if (!chartHostEl) {
    return { engine: "none", series, summary };
  }

  try {
    const { React, createRoot, Recharts } = await loadRechartsRuntime(loaders);
    if (!Recharts?.AreaChart || !Recharts?.BarChart) {
      throw new Error("Recharts AreaChart/BarChart exports missing");
    }
    let root = paymentTrendsRoots.get(chartHostEl);
    if (!root) {
      chartHostEl.innerHTML = "";
      root = createRoot(chartHostEl);
      paymentTrendsRoots.set(chartHostEl, root);
    }
    const element = createRechartsPaymentTrendsElement(React, Recharts, {
      series,
      theme,
      labels
    });
    root.render(element);
    if (statusEl) {
      statusEl.textContent =
        summary.decidedCount > 0 || summary.totalCollection > 0
          ? t("adminPayTrendsPoweredBy")
          : t("adminPayTrendsEmptyHint");
    }
    return { engine: "recharts", series, summary };
  } catch {
    unmountPaymentTrendsChart(chartHostEl);
    chartHostEl.innerHTML = renderPaymentTrendsFallbackSvg(series, { theme, labels });
    if (statusEl) {
      statusEl.textContent = t("adminPayTrendsOfflineFallback");
    }
    return { engine: "svg-fallback", series, summary };
  }
}
