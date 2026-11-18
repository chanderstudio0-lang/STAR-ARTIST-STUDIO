/**
 * Lightweight LQIP blur-up placeholder & IntersectionObserver lazy image loader
 * for STAR ARTIST STUDIO gallery and service catalog images.
 */

export function buildLqipSvgDataUri(accentHex = "#d4af37", baseHex = "#181510") {
  const safeAccent = /^#[0-9a-fA-F]{3,8}$/.test(String(accentHex)) ? accentHex : "#d4af37";
  const safeBase = /^#[0-9a-fA-F]{3,8}$/.test(String(baseHex)) ? baseHex : "#181510";
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 20" preserveAspectRatio="none">` +
    `<defs>` +
    `<radialGradient id="g" cx="50%" cy="38%" r="68%">` +
    `<stop offset="0%" stop-color="${safeAccent}" stop-opacity="0.42"/>` +
    `<stop offset="65%" stop-color="#2c240e" stop-opacity="0.88"/>` +
    `<stop offset="100%" stop-color="${safeBase}" stop-opacity="1"/>` +
    `</radialGradient>` +
    `</defs>` +
    `<rect width="32" height="20" fill="url(#g)"/>` +
    `</svg>`;
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

const DEFAULT_ESCAPE = (value) =>
  String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");

export function renderLazyBlurImageHtml({
  src,
  alt = "",
  aspectClass = "is-gallery-cover",
  badgeText = "",
  accent = "#d4af37",
  escapeHtml = DEFAULT_ESCAPE
} = {}) {
  const cleanSrc = String(src || "").trim();
  const lqipUri = buildLqipSvgDataUri(accent);
  const classes = ["blur-up-media", aspectClass].filter(Boolean).join(" ");

  return `
    <div class="${escapeHtml(classes)}" style="background-image:url('${lqipUri}')" data-lazy-container>
      <img
        src="${lqipUri}"
        data-src="${escapeHtml(cleanSrc)}"
        alt="${escapeHtml(alt)}"
        class="lazy-blur-img"
        loading="lazy"
        decoding="async"
        fetchpriority="low"
      />
      <span class="blur-up-shimmer" aria-hidden="true"></span>
      ${badgeText ? `<span class="blur-up-caption-badge">${escapeHtml(badgeText)}</span>` : ""}
    </div>
  `;
}

function markImageLoaded(img) {
  if (!img) return;
  img.classList.add("is-loaded");
  img.removeAttribute("data-src");
  const container = typeof img.closest === "function" ? img.closest("[data-lazy-container]") : null;
  if (container && container.classList) {
    container.classList.add("is-loaded");
  }
}

export function promoteLazyImage(img) {
  if (!img) return;
  const targetSrc = img.getAttribute ? img.getAttribute("data-src") : img.dataset?.src;
  if (!targetSrc) {
    markImageLoaded(img);
    return;
  }

  const onLoad = () => markImageLoaded(img);
  const onError = () => {
    img.classList.add("is-error");
    markImageLoaded(img);
  };

  if (typeof img.addEventListener === "function") {
    img.addEventListener("load", onLoad, { once: true });
    img.addEventListener("error", onError, { once: true });
  }

  if (typeof img.setAttribute === "function") {
    img.setAttribute("src", targetSrc);
  } else {
    img.src = targetSrc;
  }

  // Handle immediately cached images (already complete in memory/SW cache)
  if (img.complete && Number(img.naturalWidth) > 0) {
    markImageLoaded(img);
  }
}

let activeObserver = null;

export function initLazyBlurImages(
  root = typeof document !== "undefined" ? document : null,
  ObserverCtor = typeof IntersectionObserver !== "undefined" ? IntersectionObserver : null
) {
  if (!root || typeof root.querySelectorAll !== "function") return 0;
  const images = Array.from(root.querySelectorAll("img.lazy-blur-img[data-src]"));
  if (!images.length) return 0;

  if (typeof ObserverCtor === "function") {
    if (!activeObserver || activeObserver._ctor !== ObserverCtor) {
      const obs = new ObserverCtor(
        (entries, observerInstance) => {
          for (const entry of entries) {
            if (entry.isIntersecting || (entry.intersectionRatio && entry.intersectionRatio > 0)) {
              const target = entry.target;
              if (observerInstance && typeof observerInstance.unobserve === "function") {
                observerInstance.unobserve(target);
              }
              promoteLazyImage(target);
            }
          }
        },
        {
          rootMargin: "180px 0px",
          threshold: 0.01
        }
      );
      activeObserver = { instance: obs, _ctor: ObserverCtor };
    }

    for (const img of images) {
      activeObserver.instance.observe(img);
    }
    return images.length;
  }

  // Fallback for environments without IntersectionObserver: native loading="lazy" still defers fetch
  for (const img of images) {
    promoteLazyImage(img);
  }
  return images.length;
}
