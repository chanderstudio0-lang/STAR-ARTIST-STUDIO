import { loadRechartsRuntime } from "./admin-model-growth.js";

const experienceChartRoots = new WeakMap();

function defaultEscapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function normalizeStatus(status) {
  const clean = String(status || "").trim().toLowerCase();
  if (clean === "approved") return "approved";
  if (clean === "rejected") return "rejected";
  return "pending";
}

/**
 * Classifies a model's experienceLevel string into canonical casting tiers:
 * "fresher" vs. "experienced".
 */
export function classifyModelExperienceTier(experienceLevel) {
  const raw = String(experienceLevel ?? "").trim().toLowerCase();
  if (!raw) return "fresher";

  if (
    raw === "0" ||
    raw.includes("no experience") ||
    /\b(fresher|fresh|beginner|entry|newcomer|none|zero)\b/.test(raw) ||
    /^0\s*(?:yr|yrs|year|years)?$/.test(raw)
  ) {
    return "fresher";
  }

  return "experienced";
}

/**
 * Maps experienced model strings into canonical sub-tier brackets for casting breakdown badges.
 */
export function classifyExperiencedSubTier(experienceLevel) {
  const raw = String(experienceLevel ?? "").trim().toLowerCase();
  if (!raw || classifyModelExperienceTier(raw) === "fresher") return null;
  if (/1\s*[-–to]+\s*2/.test(raw) || /\b[12]\s*(?:yr|yrs|year|years)\b/.test(raw)) {
    return "1-2 Years";
  }
  if (/3\s*[-–to]+\s*5/.test(raw) || /\b[34]\s*(?:yr|yrs|year|years)\b/.test(raw)) {
    return "3-5 Years";
  }
  if (/5\s*\+/.test(raw) || /\b(?:[5-9]|[1-9]\d)\s*(?:\+)?\s*(?:yr|yrs|year|years)\b/.test(raw) || /\bsenior|veteran\b/.test(raw)) {
    return "5+ Years";
  }
  return "Other";
}

/**
 * Computes Fresher vs. Experienced talent pool distribution and sub-tier breakdown.
 * Supports scope="all" (all 18+ applications) and scope="approved" (Official Studio Roster).
 */
export function buildModelExperienceDistribution(
  models = [],
  { scope = "all", t = (k) => k } = {}
) {
  const list = Array.isArray(models) ? models : [];
  const normalizedScope = scope === "approved" ? "approved" : "all";

  const approvedModels = list.filter(
    (m) => normalizeStatus(m?.status) === "approved" && m?.isDisabled !== true
  );
  const scopedModels = normalizedScope === "approved" ? approvedModels : list;

  let fresherCount = 0;
  let experiencedCount = 0;
  const breakdown = {
    "1-2 Years": 0,
    "3-5 Years": 0,
    "5+ Years": 0,
    other: 0
  };

  for (const model of scopedModels) {
    const tier = classifyModelExperienceTier(model?.experienceLevel);
    if (tier === "fresher") {
      fresherCount += 1;
    } else {
      experiencedCount += 1;
      const sub = classifyExperiencedSubTier(model?.experienceLevel);
      if (sub && Object.prototype.hasOwnProperty.call(breakdown, sub)) {
        breakdown[sub] += 1;
      } else {
        breakdown.other += 1;
      }
    }
  }

  const total = scopedModels.length;
  let fresherPercent = 0;
  let experiencedPercent = 0;
  if (total > 0) {
    if (fresherCount === total) {
      fresherPercent = 100;
      experiencedPercent = 0;
    } else if (experiencedCount === total) {
      fresherPercent = 0;
      experiencedPercent = 100;
    } else {
      fresherPercent = Math.round((fresherCount / total) * 1000) / 10;
      experiencedPercent = Math.round((100 - fresherPercent) * 10) / 10;
    }
  }

  const fresherLabel = t("adminExpFresherLabel");
  const experiencedLabel = t("adminExpExperiencedLabel");

  const slices = [
    {
      key: "fresher",
      name: fresherLabel === "adminExpFresherLabel" ? "Fresher" : fresherLabel,
      value: fresherCount,
      percent: fresherPercent,
      colorKey: "fresher"
    },
    {
      key: "experienced",
      name: experiencedLabel === "adminExpExperiencedLabel" ? "Experienced" : experiencedLabel,
      value: experiencedCount,
      percent: experiencedPercent,
      colorKey: "experienced"
    }
  ];

  return {
    scope: normalizedScope,
    total,
    allTotal: list.length,
    approvedTotal: approvedModels.length,
    fresherCount,
    experiencedCount,
    fresherPercent,
    experiencedPercent,
    breakdown,
    slices
  };
}

export function getExperienceChartPalette(theme = "dark") {
  if (theme === "light") {
    return {
      fresher: "#0284C7",
      experienced: "#9A6B00",
      emptyRing: "rgba(139, 98, 0, 0.18)",
      sliceStroke: "#FFFFFF",
      text: "#1B1B1B",
      muted: "#5F5A4F",
      tooltipBg: "#FFFFFF",
      tooltipBorder: "rgba(139, 98, 0, 0.36)"
    };
  }
  return {
    fresher: "#38BDF8",
    experienced: "#D4AF37",
    emptyRing: "rgba(212, 175, 55, 0.22)",
    sliceStroke: "#141414",
    text: "#F5F2EA",
    muted: "#A8A296",
    tooltipBg: "#141414",
    tooltipBorder: "rgba(212, 175, 55, 0.48)"
  };
}

/**
 * Builds the Recharts PieChart element tree using React.createElement (no JSX/build step required).
 */
export function createRechartsModelExperienceElement(
  React,
  Recharts,
  { distribution, theme = "dark", labels = {} } = {}
) {
  const palette = getExperienceChartPalette(theme);
  const { ResponsiveContainer, PieChart, Pie, Cell, Tooltip, Legend } = Recharts;
  const h = React.createElement;

  const total = Number(distribution?.total || 0);
  const rawSlices = Array.isArray(distribution?.slices) ? distribution.slices : [];
  const activeSlices = rawSlices.filter((s) => Number(s.value) > 0);
  const isEmpty = total === 0 || activeSlices.length === 0;

  const pieData = isEmpty
    ? [{ key: "empty", name: labels.emptyLabel || "No models yet", value: 1, percent: 0, isEmpty: true }]
    : activeSlices;

  const unitLabel = labels.modelsUnit || "models";

  const tooltipProps = {
    contentStyle: {
      backgroundColor: palette.tooltipBg,
      border: `1px solid ${palette.tooltipBorder}`,
      borderRadius: "10px",
      color: palette.text,
      fontSize: "12px",
      boxShadow: "0 10px 26px rgba(0, 0, 0, 0.28)"
    },
    itemStyle: { color: palette.text, fontWeight: 700 },
    labelStyle: { color: palette.experienced, fontWeight: 700, marginBottom: "4px" },
    formatter: (value, name, item) => {
      if (item?.payload?.isEmpty) {
        return ["0", labels.emptyLabel || "No models yet"];
      }
      const pct = item?.payload?.percent ?? 0;
      return [`${value} ${unitLabel} (${pct}%)`, name];
    }
  };

  const pieCells = pieData.map((entry) =>
    h(Cell, {
      key: entry.key,
      fill: entry.isEmpty ? palette.emptyRing : palette[entry.colorKey] || palette.experienced
    })
  );

  const pieProps = {
    data: pieData,
    dataKey: "value",
    nameKey: "name",
    cx: "50%",
    cy: "48%",
    innerRadius: 54,
    outerRadius: 88,
    paddingAngle: !isEmpty && pieData.length > 1 ? 3 : 0,
    stroke: palette.sliceStroke,
    strokeWidth: 2,
    isAnimationActive: true,
    animationDuration: 450,
    label: isEmpty
      ? false
      : ({ percent }) => `${Math.round((Number(percent) || 0) * 100)}%`,
    labelLine: !isEmpty
  };

  const chartChildren = [
    h(Pie, { key: "experience-pie", ...pieProps }, ...pieCells),
    !isEmpty ? h(Tooltip, { key: "experience-tooltip", ...tooltipProps }) : null,
    Legend && !isEmpty
      ? h(Legend, {
          key: "experience-legend",
          verticalAlign: "bottom",
          height: 28,
          iconType: "circle",
          wrapperStyle: { fontSize: "12px", color: palette.text }
        })
      : null
  ].filter(Boolean);

  return h(
    ResponsiveContainer,
    { width: "100%", height: 250 },
    h(PieChart, { margin: { top: 8, right: 12, bottom: 8, left: 12 } }, ...chartChildren)
  );
}

function describeDonutArc(cx, cy, r, startAngleDeg, endAngleDeg) {
  const toRad = (deg) => ((deg - 90) * Math.PI) / 180;
  const start = {
    x: cx + r * Math.cos(toRad(startAngleDeg)),
    y: cy + r * Math.sin(toRad(startAngleDeg))
  };
  const end = {
    x: cx + r * Math.cos(toRad(endAngleDeg)),
    y: cy + r * Math.sin(toRad(endAngleDeg))
  };
  const largeArcFlag = endAngleDeg - startAngleDeg > 180 ? 1 : 0;
  return `M ${start.x.toFixed(2)} ${start.y.toFixed(2)} A ${r} ${r} 0 ${largeArcFlag} 1 ${end.x.toFixed(2)} ${end.y.toFixed(2)}`;
}

/**
 * Accessible SVG donut/pie chart fallback rendered automatically if offline or Recharts CDN cannot load.
 */
export function renderModelExperienceFallbackSvg(
  distribution,
  { theme = "dark", labels = {} } = {}
) {
  const palette = getExperienceChartPalette(theme);
  const total = Number(distribution?.total || 0);
  const fresherCount = Number(distribution?.fresherCount || 0);
  const experiencedCount = Number(distribution?.experiencedCount || 0);
  const fresherPct = Number(distribution?.fresherPercent || 0);
  const experiencedPct = Number(distribution?.experiencedPercent || 0);

  const cx = 130;
  const cy = 115;
  const radius = 72;
  const strokeWidth = 30;
  const ariaTitle = labels.chartTitle || "Model Experience Distribution (Fresher vs Experienced)";
  const totalCaption = labels.totalCaption || "Models";

  let segmentsSvg = "";
  if (total === 0) {
    segmentsSvg = `<circle cx="${cx}" cy="${cy}" r="${radius}" fill="none" stroke="${palette.emptyRing}" stroke-width="${strokeWidth}" />`;
  } else if (fresherCount === total) {
    segmentsSvg = `<circle cx="${cx}" cy="${cy}" r="${radius}" fill="none" stroke="${palette.fresher}" stroke-width="${strokeWidth}" />`;
  } else if (experiencedCount === total) {
    segmentsSvg = `<circle cx="${cx}" cy="${cy}" r="${radius}" fill="none" stroke="${palette.experienced}" stroke-width="${strokeWidth}" />`;
  } else {
    const fresherAngle = (fresherCount / total) * 360;
    const fresherPath = describeDonutArc(cx, cy, radius, 0, fresherAngle);
    const experiencedPath = describeDonutArc(cx, cy, radius, fresherAngle, 360);
    segmentsSvg = `
      <path d="${fresherPath}" fill="none" stroke="${palette.fresher}" stroke-width="${strokeWidth}" stroke-linecap="butt" />
      <path d="${experiencedPath}" fill="none" stroke="${palette.experienced}" stroke-width="${strokeWidth}" stroke-linecap="butt" />`;
  }

  return `
    <svg viewBox="0 0 260 230" class="model-experience-fallback-svg" role="img" aria-label="${defaultEscapeHtml(ariaTitle)}">
      <title>${defaultEscapeHtml(ariaTitle)}</title>
      ${segmentsSvg}
      <text x="${cx}" y="${cy - 4}" text-anchor="middle" fill="${palette.text}" font-size="24" font-weight="800">${total}</text>
      <text x="${cx}" y="${cy + 15}" text-anchor="middle" fill="${palette.muted}" font-size="11" font-weight="600">${defaultEscapeHtml(totalCaption)}</text>
      <g transform="translate(22, 214)">
        <circle cx="6" cy="-4" r="5" fill="${palette.fresher}" />
        <text x="16" y="0" fill="${palette.text}" font-size="11" font-weight="600">${defaultEscapeHtml(labels.fresher || "Fresher")}: ${fresherPct}%</text>
        <circle cx="126" cy="-4" r="5" fill="${palette.experienced}" />
        <text x="136" y="0" fill="${palette.text}" font-size="11" font-weight="600">${defaultEscapeHtml(labels.experienced || "Experienced")}: ${experiencedPct}%</text>
      </g>
    </svg>`;
}

/**
 * Renders the Talent Pool Experience KPI cards + experienced bracket breakdown pills.
 */
export function renderModelExperienceSummaryHtml(
  distribution,
  { t = (k) => k, escapeHtml = defaultEscapeHtml } = {}
) {
  const b = distribution?.breakdown || {};
  const breakdownItems = [
    { label: "1-2 Years", count: b["1-2 Years"] || 0 },
    { label: "3-5 Years", count: b["3-5 Years"] || 0 },
    { label: "5+ Years", count: b["5+ Years"] || 0 }
  ];
  if (b.other > 0) {
    breakdownItems.push({ label: t("adminExpOtherBracket"), count: b.other });
  }

  return `
    <div class="model-experience-stats-grid">
      <div class="model-experience-stat">
        <span class="model-experience-stat-label">${escapeHtml(t("adminExpTotalPool"))}</span>
        <strong class="model-experience-stat-value">${escapeHtml(distribution.total)}</strong>
        <span class="model-experience-stat-sub">${escapeHtml(
          distribution.scope === "approved" ? t("adminExpScopeApproved") : t("adminExpScopeAll")
        )}</span>
      </div>
      <div class="model-experience-stat is-fresher">
        <span class="model-experience-stat-label">
          <span class="model-experience-dot is-fresher"></span>
          ${escapeHtml(t("adminExpFresherLabel"))}
        </span>
        <div class="model-experience-stat-row">
          <strong class="model-experience-stat-value">${escapeHtml(distribution.fresherCount)}</strong>
          <span class="model-experience-pct is-fresher">${escapeHtml(distribution.fresherPercent)}%</span>
        </div>
        <span class="model-experience-stat-sub">${escapeHtml(t("adminExpFresherHint"))}</span>
      </div>
      <div class="model-experience-stat is-experienced">
        <span class="model-experience-stat-label">
          <span class="model-experience-dot is-experienced"></span>
          ${escapeHtml(t("adminExpExperiencedLabel"))}
        </span>
        <div class="model-experience-stat-row">
          <strong class="model-experience-stat-value">${escapeHtml(distribution.experiencedCount)}</strong>
          <span class="model-experience-pct is-experienced">${escapeHtml(distribution.experiencedPercent)}%</span>
        </div>
        <span class="model-experience-stat-sub">${escapeHtml(t("adminExpExperiencedHint"))}</span>
      </div>
    </div>
    <div class="model-experience-breakdown">
      <span class="model-experience-breakdown-label">${escapeHtml(t("adminExpBreakdownTitle"))}</span>
      <div class="model-experience-breakdown-chips">
        ${breakdownItems
          .map(
            (item) =>
              `<span class="badge ${item.count > 0 ? "status-confirmed" : ""}">${escapeHtml(item.label)}: <strong>${escapeHtml(item.count)}</strong></span>`
          )
          .join("")}
      </div>
    </div>`;
}

/**
 * Renders the quick-oversight administrative summary card displayed directly above
 * the Model Experience pie chart (Total Approved Models, Fresher Models, Experienced Models).
 */
export function renderModelExperienceOverviewCardHtml(
  models = [],
  { t = (k) => k, escapeHtml = defaultEscapeHtml } = {}
) {
  const allDist = buildModelExperienceDistribution(models, { scope: "all", t });
  const approvedDist = buildModelExperienceDistribution(models, { scope: "approved", t });

  const approvedSub = t("adminExpOverviewApprovedSub").replace("{total}", String(allDist.allTotal));
  const fresherApprovedSub = t("adminExpOverviewApprovedCountSub").replace(
    "{count}",
    String(approvedDist.fresherCount)
  );
  const experiencedApprovedSub = t("adminExpOverviewApprovedCountSub").replace(
    "{count}",
    String(approvedDist.experiencedCount)
  );

  return `
    <section
      id="adminModelExperienceOverviewCard"
      class="studio-card model-experience-overview-card"
      aria-label="${escapeHtml(t("adminExpOverviewTitle"))}"
    >
      <div class="model-experience-overview-head">
        <div>
          <strong class="model-experience-overview-title">${escapeHtml(t("adminExpOverviewTitle"))}</strong>
          <span class="model-experience-overview-subtitle">${escapeHtml(t("adminExpOverviewSubtitle"))}</span>
        </div>
        <span class="badge status-approved">${escapeHtml(approvedDist.total)} ${escapeHtml(t("adminExpScopeApproved"))}</span>
      </div>
      <div class="model-experience-overview-grid">
        <div class="model-experience-overview-tile is-approved" data-exp-overview-metric="approved">
          <span class="model-experience-overview-label">
            <span class="model-experience-dot is-approved"></span>
            ${escapeHtml(t("adminExpOverviewApprovedLabel"))}
          </span>
          <div class="model-experience-overview-value-row">
            <strong class="model-experience-overview-value" data-exp-overview-count="approved">${escapeHtml(approvedDist.total)}</strong>
          </div>
          <span class="model-experience-overview-sub">${escapeHtml(approvedSub)}</span>
        </div>
        <div class="model-experience-overview-tile is-fresher" data-exp-overview-metric="fresher">
          <span class="model-experience-overview-label">
            <span class="model-experience-dot is-fresher"></span>
            ${escapeHtml(t("adminExpOverviewFresherLabel"))}
          </span>
          <div class="model-experience-overview-value-row">
            <strong class="model-experience-overview-value" data-exp-overview-count="fresher">${escapeHtml(allDist.fresherCount)}</strong>
            <span class="model-experience-pct is-fresher">${escapeHtml(allDist.fresherPercent)}%</span>
          </div>
          <span class="model-experience-overview-sub">${escapeHtml(fresherApprovedSub)}</span>
        </div>
        <div class="model-experience-overview-tile is-experienced" data-exp-overview-metric="experienced">
          <span class="model-experience-overview-label">
            <span class="model-experience-dot is-experienced"></span>
            ${escapeHtml(t("adminExpOverviewExperiencedLabel"))}
          </span>
          <div class="model-experience-overview-value-row">
            <strong class="model-experience-overview-value" data-exp-overview-count="experienced">${escapeHtml(allDist.experiencedCount)}</strong>
            <span class="model-experience-pct is-experienced">${escapeHtml(allDist.experiencedPercent)}%</span>
          </div>
          <span class="model-experience-overview-sub">${escapeHtml(experiencedApprovedSub)}</span>
        </div>
      </div>
    </section>`;
}

async function loadPieRechartsRuntime(customLoaders = null) {
  if (customLoaders) {
    const [reactMod, reactDomMod, rechartsMod] = await Promise.all([
      customLoaders.react(),
      customLoaders.reactDomClient(),
      customLoaders.recharts()
    ]);
    const React = reactMod?.default || reactMod;
    const createRoot = reactDomMod?.createRoot || reactDomMod?.default?.createRoot;
    const Recharts = rechartsMod?.PieChart ? rechartsMod : rechartsMod?.default || rechartsMod;
    if (!React?.createElement || typeof createRoot !== "function" || !Recharts?.PieChart || !Recharts?.Pie) {
      throw new Error("Recharts PieChart runtime exports incomplete");
    }
    return { React, createRoot, Recharts };
  }

  const runtime = await loadRechartsRuntime();
  if (!runtime?.Recharts?.PieChart || !runtime?.Recharts?.Pie) {
    throw new Error("Recharts PieChart exports missing");
  }
  return runtime;
}

/**
 * Unmounts any active Recharts PieChart React root and clears its DOM container.
 */
export function unmountModelExperienceChart(chartHostEl) {
  if (!chartHostEl) return;
  const existingRoot = experienceChartRoots.get(chartHostEl);
  if (existingRoot && typeof existingRoot.unmount === "function") {
    try {
      existingRoot.unmount();
    } catch {
      // Ignore unmount errors on detached nodes
    }
    experienceChartRoots.delete(chartHostEl);
  }
  chartHostEl.innerHTML = "";
}

/**
 * Mounts or updates the Fresher vs. Experienced Recharts PieChart under the Admin 'Models' tab.
 */
export async function mountModelExperienceChart(
  chartHostEl,
  {
    models = [],
    summaryEl = null,
    statusEl = null,
    scope = "all",
    theme = "dark",
    t = (k) => k,
    escapeHtml = defaultEscapeHtml,
    loaders = null
  } = {}
) {
  const distribution = buildModelExperienceDistribution(models, { scope, t });

  if (summaryEl) {
    summaryEl.innerHTML = renderModelExperienceSummaryHtml(distribution, { t, escapeHtml });
  }

  const labels = {
    chartTitle: t("adminExpChartTitle"),
    fresher: t("adminExpFresherLabel"),
    experienced: t("adminExpExperiencedLabel"),
    totalCaption: t("adminExpTotalCaption"),
    modelsUnit: t("adminExpModelsUnit"),
    emptyLabel: t("adminExpEmptyLabel")
  };

  if (!chartHostEl) {
    return { engine: "none", distribution };
  }

  try {
    const { React, createRoot, Recharts } = await loadPieRechartsRuntime(loaders);
    let root = experienceChartRoots.get(chartHostEl);
    if (!root) {
      chartHostEl.innerHTML = "";
      root = createRoot(chartHostEl);
      experienceChartRoots.set(chartHostEl, root);
    }
    const element = createRechartsModelExperienceElement(React, Recharts, {
      distribution,
      theme,
      labels
    });
    root.render(element);
    if (statusEl) {
      statusEl.textContent =
        distribution.total > 0 ? t("adminExpPoweredBy") : t("adminExpEmptyHint");
    }
    return { engine: "recharts", distribution };
  } catch {
    unmountModelExperienceChart(chartHostEl);
    chartHostEl.innerHTML = renderModelExperienceFallbackSvg(distribution, { theme, labels });
    if (statusEl) {
      statusEl.textContent = t("adminExpOfflineFallback");
    }
    return { engine: "svg-fallback", distribution };
  }
}
