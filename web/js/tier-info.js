const TIER_META = {
  essential: {
    badgeEn: "CORE SESSION",
    badgeHi: "कोर सत्र",
    tone: "essential",
    summaryEn: "Entry studio package with core lighting setups, key retouched deliverables & standard delivery.",
    summaryHi: "कोर लाइटिंग सेटअप, मुख्य रिटच डिलीवरी और स्टैंडर्ड टर्नअराउंड वाला शुरुआती स्टूडियो पैकेज।"
  },
  pro: {
    badgeEn: "MOST POPULAR",
    badgeHi: "सबसे लोकप्रिय",
    tone: "pro",
    summaryEn: "Extended multi-look production with signature strobe lighting, reels & faster turnaround.",
    summaryHi: "अतिरिक्त आउटफिट लुक्स, सिग्नेचर लाइटिंग, रील्स और तेज़ डिलीवरी वाला सबसे लोकप्रिय पैकेज।"
  },
  flagship: {
    badgeEn: "FULL VIP",
    badgeHi: "फुल VIP",
    tone: "flagship",
    summaryEn: "All-inclusive flagship campaign with creative direction, VIP styling, RAW/4K masters & express delivery.",
    summaryHi: "क्रिएटिव डायरेक्शन, VIP स्टाइलिंग, RAW/4K मास्टर और एक्सप्रेस डिलीवरी वाला ऑल-इनक्लूसिव पैकेज।"
  },
  wedding_basic: {
    badgeEn: "BASIC WEDDING",
    badgeHi: "बेसिक वेडिंग",
    tone: "essential",
    summaryEn: "Complete single-day wedding ceremony photography, HD video coverage & printed wedding album.",
    summaryHi: "एक-दिवसीय विवाह समारोह की पारंपरिक व कैंडिड फोटोग्राफी, HD वीडियो और प्रिंटेड एल्बम।"
  },
  wedding_premium: {
    badgeEn: "PREMIUM WEDDING",
    badgeHi: "प्रीमियम वेडिंग",
    tone: "pro",
    summaryEn: "Multi-day Haldi, Mehndi & Wedding candid stills, 4K cinematic film, drone aerials & Karizma album.",
    summaryHi: "हल्दी, मेहंदी और शादी के लिए कैंडिड स्टिल्स, 4K सिनेमाई फिल्म, ड्रोन और करिश्मा एल्बम।"
  },
  wedding_luxury: {
    badgeEn: "LUXURY ROYAL",
    badgeHi: "लक्ज़री रॉयल",
    tone: "flagship",
    summaryEn: "3-day royal destination cinema unit, pre-wedding shoot, same-day reception edit & Italian leather albums.",
    summaryHi: "3-दिवसीय रॉयल डेस्टिनेशन सिनेमा क्रू, प्री-वेडिंग शूट, सेम-डे एडिट और इटैलियन लेदर एल्बम।"
  }
};

export function getTierInfoMeta(tierId, lang = "en") {
  const key = String(tierId || "").toLowerCase();
  const entry = TIER_META[key] || {
    badgeEn: "STUDIO TIER",
    badgeHi: "स्टूडियो टियर",
    tone: "essential",
    summaryEn: "Full studio production deliverables calibrated to this tier.",
    summaryHi: "इस टियर के अनुसार संपूर्ण स्टूडियो प्रोडक्शन डिलीवरी।"
  };
  const isHi = lang === "hi";
  return {
    badge: isHi ? entry.badgeHi : entry.badgeEn,
    summary: isHi ? entry.summaryHi : entry.summaryEn,
    tone: entry.tone
  };
}

export function buildTierTooltipText(tier, lang = "en") {
  if (!tier) return "";
  const isHi = lang === "hi";
  const name = isHi ? tier.tierNameHi || tier.tierNameEn : tier.tierNameEn;
  const meta = getTierInfoMeta(tier.id, lang);
  const topDeliverables = Array.isArray(tier.deliverablesEn) ? tier.deliverablesEn.slice(0, 2).join("; ") : "";
  return `${name} (${meta.badge}): ${meta.summary}${topDeliverables ? ` Includes: ${topDeliverables}.` : ""}`;
}

export function renderCatalogTierSectionHtml({ pkg, activeTier, isGuideExpanded, lang, t, escapeHtml }) {
  const safePkgId = escapeHtml(pkg.id);
  const activeMeta = getTierInfoMeta(activeTier?.id, lang);
  const activeTierName = escapeHtml(lang === "hi" ? activeTier?.tierNameHi || activeTier?.tierNameEn : activeTier?.tierNameEn);

  const tabsHtml = (pkg.tiers || [])
    .map((tr) => {
      const tierLabel = lang === "hi" ? tr.tierNameHi || tr.tierNameEn : tr.tierNameEn;
      const meta = getTierInfoMeta(tr.id, lang);
      const tooltipText = buildTierTooltipText(tr, lang);
      const isSelected = tr.id === activeTier?.id;
      return `
        <button
          type="button"
          class="tier-tab has-tier-tooltip ${isSelected ? "active" : ""}"
          data-pkg-id="${safePkgId}"
          data-tier-id="${escapeHtml(tr.id)}"
          data-tooltip="${escapeHtml(tooltipText)}"
          title="${escapeHtml(tooltipText)}"
          aria-pressed="${isSelected ? "true" : "false"}"
        >
          <span class="tier-tab-badge tone-${escapeHtml(meta.tone)}">${escapeHtml(meta.badge)}</span>
          <span class="tier-tab-name">${escapeHtml(tierLabel)}</span>
        </button>`;
    })
    .join("");

  const guideRowsHtml = (pkg.tiers || [])
    .map((tr) => {
      const tierLabel = lang === "hi" ? tr.tierNameHi || tr.tierNameEn : tr.tierNameEn;
      const meta = getTierInfoMeta(tr.id, lang);
      const deliverables = Array.isArray(tr.deliverablesEn) ? tr.deliverablesEn : [];
      return `
        <div class="tier-guide-row tone-${escapeHtml(meta.tone)}" data-tier-guide-row="${escapeHtml(tr.id)}">
          <div class="tier-guide-row-head">
            <span class="tier-tab-badge tone-${escapeHtml(meta.tone)}">${escapeHtml(meta.badge)}</span>
            <strong>${escapeHtml(tierLabel)} • ${escapeHtml(tr.priceFormatted)}</strong>
            <span class="tier-guide-timing">${escapeHtml(tr.durationEn)} • ${escapeHtml(tr.turnaroundEn)}</span>
          </div>
          <p class="tier-guide-summary">${escapeHtml(meta.summary)}</p>
          <ul class="tier-guide-deliverables">
            ${deliverables.map((d) => `<li>${escapeHtml(d)}</li>`).join("")}
          </ul>
        </div>`;
    })
    .join("");

  return `
    <div class="tier-controls-header">
      <span class="tier-controls-label">${escapeHtml(t("tierOptionsLabel"))}</span>
      <button
        type="button"
        class="tier-guide-toggle ${isGuideExpanded ? "active" : ""}"
        data-tier-guide-pkg="${safePkgId}"
        aria-expanded="${isGuideExpanded ? "true" : "false"}"
        title="${escapeHtml(t("tierGuideTooltip"))}"
      >
        <span aria-hidden="true">ⓘ</span>
        <span>${escapeHtml(isGuideExpanded ? t("hideTierInfoBtn") : t("tierInfoBtn"))}</span>
      </button>
    </div>
    <div class="tier-tabs">${tabsHtml}</div>
    <div class="tier-info-badge tone-${escapeHtml(activeMeta.tone)}" role="status" data-active-tier-info="${safePkgId}">
      <span class="tier-info-icon" aria-hidden="true">ⓘ</span>
      <div class="tier-info-copy">
        <strong>${activeTierName} — ${escapeHtml(activeMeta.badge)}</strong>
        <span>${escapeHtml(activeMeta.summary)}</span>
      </div>
    </div>
    ${
      isGuideExpanded
        ? `<div class="tier-guide-panel" data-tier-guide-panel="${safePkgId}" role="region" aria-label="${escapeHtml(t("tierInfoBtn"))}">${guideRowsHtml}</div>`
        : ""
    }
  `;
}
