import { normalizeModelStatus } from "./admin-models.js";

export const MODEL_GROWTH_DAYS = 30;

export const RECHARTS_CDN_URLS = {
  react: "https://esm.sh/react@18.3.1",
  reactDomClient: "https://esm.sh/react-dom@18.3.1/client",
  recharts: "https://esm.sh/recharts@2.15.0?deps=react@18.3.1,react-dom@18.3.1"
};

let cachedRechartsRuntimePromise = null;
const chartRoots = new WeakMap();

function defaultEscapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

/**
 * Safely converts Firestore Timestamps, serialized timestamps, epoch numbers,
 * ISO strings, or Date objects into a valid JS Date (or null).
 */
export function normalizeApplicationDate(value) {
  if (!value) return null;
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value;
  }
  if (typeof value === "object") {
    if (typeof value.toDate === "function") {
      try {
        const d = value.toDate();
        return d instanceof Date && !Number.isNaN(d.getTime()) ? d : null;
      } catch {
        return null;
      }
    }
    const sec =
      typeof value.seconds === "number"
        ? value.seconds
        : typeof value._seconds === "number"
        ? value._seconds
        : null;
    if (sec !== null && Number.isFinite(sec)) {
      const nanos = Number(value.nanoseconds ?? value._nanoseconds ?? 0);
      const d = new Date(sec * 1000 + Math.floor(nanos / 1e6));
      return Number.isNaN(d.getTime()) ? null : d;
    }
  }
  if (typeof value === "number" && Number.isFinite(value)) {
    const ms = value < 1e12 ? value * 1000 : value;
    const d = new Date(ms);
    return Number.isNaN(d.getTime()) ? null : d;
  }
  if (typeof value === "string") {
    const trimmed = value.trim();
    if (!trimmed) return null;
    const d = new Date(trimmed);
    return Number.isNaN(d.getTime()) ? null : d;
  }
  return null;
}

export function toDateKey(date, { utc = false } = {}) {
  const y = utc ? date.getUTCFullYear() : date.getFullYear();
  const m = (utc ? date.getUTCMonth() : date.getMonth()) + 1;
  const d = utc ? date.getUTCDate() : date.getDate();
  return `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
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

/**
 * Aggregates model applications into a contiguous N-day (default 30-day) daily
 * time series ordered chronologically from oldest day to today.
 */
export function buildModelGrowthSeries(
  models = [],
  { days = MODEL_GROWTH_DAYS, now = new Date(), lang = "en", utc = false } = {}
) {
  const safeDays = Math.max(1, Math.min(90, Math.floor(Number(days) || MODEL_GROWTH_DAYS)));
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
      applications: 0,
      approved: 0,
      pending: 0,
      rejected: 0,
      cumulative: 0
    };
    buckets.push(entry);
    byKey.set(key, entry);
  }

  for (const model of Array.isArray(models) ? models : []) {
    const dt = normalizeApplicationDate(model?.createdAt ?? model?.submittedAt ?? model?.timestamp);
    if (!dt) continue;
    const key = toDateKey(dt, { utc });
    const bucket = byKey.get(key);
    if (!bucket) continue;

    bucket.applications += 1;
    const st = normalizeModelStatus(model?.status);
    if (st === "approved") bucket.approved += 1;
    else if (st === "rejected") bucket.rejected += 1;
    else bucket.pending += 1;
  }

  let running = 0;
  for (const point of buckets) {
    running += point.applications;
    point.cumulative = running;
  }

  return buckets;
}

/**
 * Computes outreach performance KPIs from the 30-day model growth series.
 */
export function summarizeModelGrowth(series = [], allModels = []) {
  const list = Array.isArray(series) ? series : [];
  const total30Days = list.reduce((sum, pt) => sum + Number(pt?.applications || 0), 0);
  const approved30Days = list.reduce((sum, pt) => sum + Number(pt?.approved || 0), 0);
  const last7 = list.slice(-7);
  const prev7 = list.slice(-14, -7);
  const last7Days = last7.reduce((sum, pt) => sum + Number(pt?.applications || 0), 0);
  const prev7Days = prev7.reduce((sum, pt) => sum + Number(pt?.applications || 0), 0);

  let trendPercent = 0;
  let trendDirection = "flat";
  if (prev7Days === 0 && last7Days > 0) {
    trendPercent = 100;
    trendDirection = "up";
  } else if (prev7Days > 0) {
    trendPercent = Math.round(((last7Days - prev7Days) / prev7Days) * 100);
    trendDirection = trendPercent > 0 ? "up" : trendPercent < 0 ? "down" : "flat";
  }

  let peakDay = null;
  for (const pt of list) {
    if (!peakDay || pt.applications > peakDay.applications) {
      peakDay = {
        dateKey: pt.dateKey,
        label: pt.label,
        applications: pt.applications
      };
    }
  }
  if (peakDay && peakDay.applications === 0) {
    peakDay = null;
  }

  const daysCount = Math.max(1, list.length);
  const dailyAverage = Number((total30Days / daysCount).toFixed(1));
  const approvalRate30Days = total30Days > 0 ? Math.round((approved30Days / total30Days) * 100) : 0;

  return {
    total30Days,
    approved30Days,
    last7Days,
    prev7Days,
    trendPercent,
    trendDirection,
    peakDay,
    dailyAverage,
    approvalRate30Days,
    allTimeTotal: Array.isArray(allModels) ? allModels.length : total30Days
  };
}

export function getChartThemePalette(theme = "dark") {
  if (theme === "light") {
    return {
      gold: "#9A6E00",
      goldLight: "#D4AF37",
      emerald: "#15803D",
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
    grid: "rgba(212, 175, 55, 0.18)",
    axis: "#B8B0A0",
    tooltipBg: "#141414",
    tooltipBorder: "rgba(212, 175, 55, 0.5)",
    tooltipText: "#F5F5F4",
    tooltipMuted: "#B8B0A0"
  };
}

/**
 * Builds the Recharts element tree using React.createElement (no JSX build step needed).
 */
export function createRechartsModelGrowthElement(
  React,
  Recharts,
  { series = [], chartType = "area", theme = "dark", labels = {} } = {}
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
    Tooltip
  } = Recharts;

  const palette = getChartThemePalette(theme);
  const appsLabel = labels.newApplications || "New Applications";
  const approvedLabel = labels.approvedRoster || "Roster Approved";

  const tooltipStyle = {
    backgroundColor: palette.tooltipBg,
    border: `1px solid ${palette.tooltipBorder}`,
    borderRadius: "12px",
    color: palette.tooltipText,
    fontSize: "12px",
    boxShadow: "0 10px 28px rgba(0, 0, 0, 0.35)"
  };

  const sharedChildren = [
    h(CartesianGrid, {
      key: "grid",
      strokeDasharray: "3 3",
      vertical: false,
      stroke: palette.grid
    }),
    h(XAxis, {
      key: "xaxis",
      dataKey: "label",
      tick: { fill: palette.axis, fontSize: 11 },
      axisLine: { stroke: palette.grid },
      tickLine: false,
      minTickGap: 22
    }),
    h(YAxis, {
      key: "yaxis",
      allowDecimals: false,
      width: 30,
      tick: { fill: palette.axis, fontSize: 11 },
      axisLine: false,
      tickLine: false
    }),
    h(Tooltip, {
      key: "tooltip",
      contentStyle: tooltipStyle,
      labelStyle: { color: palette.gold, fontWeight: 700, marginBottom: "4px" },
      itemStyle: { padding: "1px 0" },
      labelFormatter: (_label, payload) => payload?.[0]?.payload?.fullDate || _label
    })
  ];

  const chartNode =
    chartType === "bar"
      ? h(
          BarChart,
          {
            data: series,
            margin: { top: 12, right: 12, left: -6, bottom: 4 }
          },
          ...sharedChildren,
          h(Bar, {
            key: "bar-apps",
            dataKey: "applications",
            name: appsLabel,
            fill: palette.gold,
            radius: [6, 6, 0, 0],
            maxBarSize: 22
          }),
          h(Bar, {
            key: "bar-approved",
            dataKey: "approved",
            name: approvedLabel,
            fill: palette.emerald,
            radius: [6, 6, 0, 0],
            maxBarSize: 22
          })
        )
      : h(
          AreaChart,
          {
            data: series,
            margin: { top: 12, right: 12, left: -6, bottom: 4 }
          },
          h(
            "defs",
            { key: "defs" },
            h(
              "linearGradient",
              { id: "modelGrowthGoldFill", x1: "0", y1: "0", x2: "0", y2: "1" },
              h("stop", { offset: "5%", stopColor: palette.gold, stopOpacity: 0.45 }),
              h("stop", { offset: "95%", stopColor: palette.gold, stopOpacity: 0.02 })
            ),
            h(
              "linearGradient",
              { id: "modelGrowthEmeraldFill", x1: "0", y1: "0", x2: "0", y2: "1" },
              h("stop", { offset: "5%", stopColor: palette.emerald, stopOpacity: 0.3 }),
              h("stop", { offset: "95%", stopColor: palette.emerald, stopOpacity: 0.02 })
            )
          ),
          ...sharedChildren,
          h(Area, {
            key: "area-apps",
            type: "monotone",
            dataKey: "applications",
            name: appsLabel,
            stroke: palette.gold,
            strokeWidth: 2.5,
            fill: "url(#modelGrowthGoldFill)",
            dot: false,
            activeDot: { r: 5, fill: palette.goldLight, stroke: palette.gold, strokeWidth: 2 }
          }),
          h(Area, {
            key: "area-approved",
            type: "monotone",
            dataKey: "approved",
            name: approvedLabel,
            stroke: palette.emerald,
            strokeWidth: 2,
            fill: "url(#modelGrowthEmeraldFill)",
            dot: false,
            activeDot: { r: 4, fill: palette.emerald }
          })
        );

  return h(
    ResponsiveContainer,
    { width: "100%", height: 270, minWidth: 220 },
    chartNode
  );
}

/**
 * Offline-safe SVG fallback renderer so the 30-day Model Growth trend remains
 * visible even if the admin is offline and CDN scripts cannot be loaded.
 */
export function renderModelGrowthFallbackSvg(series = [], { theme = "dark", labels = {} } = {}) {
  const palette = getChartThemePalette(theme);
  const width = 640;
  const height = 240;
  const pad = { top: 18, right: 16, bottom: 32, left: 34 };
  const plotW = width - pad.left - pad.right;
  const plotH = height - pad.top - pad.bottom;
  const count = Math.max(1, series.length);
  const maxVal = Math.max(1, ...series.map((p) => Number(p?.applications || 0)));

  const xAt = (idx) => (count === 1 ? pad.left + plotW / 2 : pad.left + (idx / (count - 1)) * plotW);
  const yAt = (val) => pad.top + plotH - (Number(val || 0) / maxVal) * plotH;

  const linePoints = series.map((p, i) => `${xAt(i).toFixed(1)},${yAt(p.applications).toFixed(1)}`).join(" ");
  const areaPath = series.length
    ? `M ${xAt(0).toFixed(1)},${(pad.top + plotH).toFixed(1)} ` +
      series.map((p, i) => `L ${xAt(i).toFixed(1)},${yAt(p.applications).toFixed(1)}`).join(" ") +
      ` L ${xAt(series.length - 1).toFixed(1)},${(pad.top + plotH).toFixed(1)} Z`
    : "";

  const tickIndices = [0, Math.floor(count / 3), Math.floor((2 * count) / 3), count - 1];
  const uniqueTicks = [...new Set(tickIndices.filter((i) => i >= 0 && i < series.length))];
  const ariaTitle = defaultEscapeHtml(labels.chartTitle || "Model Growth — Last 30 Days");

  return `
    <svg class="model-growth-fallback-svg" viewBox="0 0 ${width} ${height}" role="img" aria-label="${ariaTitle}">
      <defs>
        <linearGradient id="mgFallbackGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="${palette.gold}" stop-opacity="0.38" />
          <stop offset="100%" stop-color="${palette.gold}" stop-opacity="0.02" />
        </linearGradient>
      </defs>
      <line x1="${pad.left}" y1="${pad.top}" x2="${width - pad.right}" y2="${pad.top}" stroke="${palette.grid}" stroke-dasharray="3 3" />
      <line x1="${pad.left}" y1="${(pad.top + plotH / 2).toFixed(1)}" x2="${width - pad.right}" y2="${(pad.top + plotH / 2).toFixed(1)}" stroke="${palette.grid}" stroke-dasharray="3 3" />
      <line x1="${pad.left}" y1="${pad.top + plotH}" x2="${width - pad.right}" y2="${pad.top + plotH}" stroke="${palette.grid}" />
      ${areaPath ? `<path d="${areaPath}" fill="url(#mgFallbackGrad)" />` : ""}
      ${linePoints ? `<polyline points="${linePoints}" fill="none" stroke="${palette.gold}" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round" />` : ""}
      <text x="${pad.left - 8}" y="${pad.top + 4}" text-anchor="end" font-size="11" fill="${palette.axis}">${maxVal}</text>
      <text x="${pad.left - 8}" y="${pad.top + plotH + 4}" text-anchor="end" font-size="11" fill="${palette.axis}">0</text>
      ${uniqueTicks
        .map(
          (idx) =>
            `<text x="${xAt(idx).toFixed(1)}" y="${height - 10}" text-anchor="middle" font-size="11" fill="${palette.axis}">${defaultEscapeHtml(series[idx].label)}</text>`
        )
        .join("")}
    </svg>`;
}

export function renderModelGrowthMetricsHtml(summary, { t = (k) => k, escapeHtml = defaultEscapeHtml } = {}) {
  const trendSign = summary.trendDirection === "up" ? "+" : "";
  const trendLabel =
    summary.trendDirection === "flat"
      ? "0%"
      : `${trendSign}${summary.trendPercent}%`;
  const trendCls =
    summary.trendDirection === "up"
      ? "is-up"
      : summary.trendDirection === "down"
      ? "is-down"
      : "is-flat";
  const peakText = summary.peakDay
    ? `${summary.peakDay.applications} (${summary.peakDay.label})`
    : "0";

  return `
    <div class="model-growth-kpi">
      <span class="model-growth-kpi-label">${escapeHtml(t("adminGrowth30dTotal"))}</span>
      <strong class="model-growth-kpi-value">${escapeHtml(summary.total30Days)}</strong>
    </div>
    <div class="model-growth-kpi">
      <span class="model-growth-kpi-label">${escapeHtml(t("adminGrowth7dTotal"))}</span>
      <div class="model-growth-kpi-row">
        <strong class="model-growth-kpi-value">${escapeHtml(summary.last7Days)}</strong>
        <span class="model-growth-trend ${trendCls}" title="${escapeHtml(t("adminGrowthVsPrev7d"))}">${escapeHtml(trendLabel)}</span>
      </div>
    </div>
    <div class="model-growth-kpi">
      <span class="model-growth-kpi-label">${escapeHtml(t("adminGrowthPeakDay"))}</span>
      <strong class="model-growth-kpi-value">${escapeHtml(peakText)}</strong>
    </div>
    <div class="model-growth-kpi">
      <span class="model-growth-kpi-label">${escapeHtml(t("adminGrowthDailyAvg"))}</span>
      <strong class="model-growth-kpi-value">${escapeHtml(summary.dailyAverage)}/day</strong>
    </div>`;
}

export async function loadRechartsRuntime(customLoaders = null) {
  if (customLoaders) {
    const [reactMod, reactDomMod, rechartsMod] = await Promise.all([
      customLoaders.react(),
      customLoaders.reactDomClient(),
      customLoaders.recharts()
    ]);
    return {
      React: reactMod?.default || reactMod,
      createRoot: reactDomMod?.createRoot || reactDomMod?.default?.createRoot,
      Recharts: rechartsMod?.default?.AreaChart ? rechartsMod.default : rechartsMod
    };
  }

  if (!cachedRechartsRuntimePromise) {
    cachedRechartsRuntimePromise = Promise.all([
      import(/* @vite-ignore */ RECHARTS_CDN_URLS.react),
      import(/* @vite-ignore */ RECHARTS_CDN_URLS.reactDomClient),
      import(/* @vite-ignore */ RECHARTS_CDN_URLS.recharts)
    ])
      .then(([reactMod, reactDomMod, rechartsMod]) => {
        const React = reactMod?.default || reactMod;
        const createRoot = reactDomMod?.createRoot || reactDomMod?.default?.createRoot;
        const Recharts = rechartsMod?.AreaChart ? rechartsMod : rechartsMod?.default || rechartsMod;
        if (!React?.createElement || typeof createRoot !== "function" || !Recharts?.AreaChart) {
          throw new Error("Recharts runtime exports incomplete");
        }
        return { React, createRoot, Recharts };
      })
      .catch((err) => {
        cachedRechartsRuntimePromise = null;
        throw err;
      });
  }
  return cachedRechartsRuntimePromise;
}

/**
 * Unmounts any active Recharts React root and clears chart DOM (used on sign-out / unauthorized access).
 */
export function unmountModelGrowthChart(chartHostEl) {
  if (!chartHostEl) return;
  const existingRoot = chartRoots.get(chartHostEl);
  if (existingRoot && typeof existingRoot.unmount === "function") {
    try {
      existingRoot.unmount();
    } catch {
      // Ignore unmount errors on detached nodes
    }
    chartRoots.delete(chartHostEl);
  }
  chartHostEl.innerHTML = "";
}

/**
 * Mounts or updates the 30-day Model Growth Recharts visualization inside the Admin Dashboard.
 */
export async function mountModelGrowthChart(
  chartHostEl,
  {
    models = [],
    metricsEl = null,
    statusEl = null,
    chartType = "area",
    theme = "dark",
    lang = "en",
    now = new Date(),
    t = (k) => k,
    escapeHtml = defaultEscapeHtml,
    loaders = null
  } = {}
) {
  const series = buildModelGrowthSeries(models, { days: MODEL_GROWTH_DAYS, now, lang });
  const summary = summarizeModelGrowth(series, models);

  if (metricsEl) {
    metricsEl.innerHTML = renderModelGrowthMetricsHtml(summary, { t, escapeHtml });
  }

  const labels = {
    chartTitle: t("adminGrowthTitle"),
    newApplications: t("adminGrowthSeriesApps"),
    approvedRoster: t("adminGrowthSeriesApproved")
  };

  if (!chartHostEl) {
    return { engine: "none", series, summary };
  }

  try {
    const { React, createRoot, Recharts } = await loadRechartsRuntime(loaders);
    let root = chartRoots.get(chartHostEl);
    if (!root) {
      chartHostEl.innerHTML = "";
      root = createRoot(chartHostEl);
      chartRoots.set(chartHostEl, root);
    }
    const element = createRechartsModelGrowthElement(React, Recharts, {
      series,
      chartType,
      theme,
      labels
    });
    root.render(element);
    if (statusEl) {
      statusEl.textContent =
        summary.total30Days > 0
          ? t("adminGrowthPoweredBy")
          : t("adminGrowthEmptyHint");
    }
    return { engine: "recharts", series, summary };
  } catch {
    unmountModelGrowthChart(chartHostEl);
    chartHostEl.innerHTML = renderModelGrowthFallbackSvg(series, { theme, labels });
    if (statusEl) {
      statusEl.textContent = t("adminGrowthOfflineFallback");
    }
    return { engine: "svg-fallback", series, summary };
  }
}
